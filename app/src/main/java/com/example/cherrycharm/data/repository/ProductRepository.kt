package com.example.cherrycharm.data.repository

import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.friendlyMessage
import com.example.cherrycharm.core.resourceOf
import com.example.cherrycharm.data.model.Banner
import com.example.cherrycharm.data.model.Category
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.data.model.Review
import com.example.cherrycharm.data.model.asString
import com.example.cherrycharm.di.ApplicationScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One live listener on the catalog, shared by every screen. Boutique catalogs are
 * small, so we keep it in memory and do search/filter/sort on-device — instant and
 * far more flexible than Firestore queries (no composite indexes to maintain).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ProductRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    @ApplicationScope scope: CoroutineScope,
) {
    private val productsRef = firestore.collection(Constants.PRODUCTS)

    private val refreshTick = MutableStateFlow(0)

    val products: StateFlow<Resource<List<Product>>> = refreshTick
        .flatMapLatest {
            productsRef.snapshotFlow()
                .map<QuerySnapshot, Resource<List<Product>>> { snap ->
                    Resource.Success(
                        snap.documents.mapNotNull(Product::from)
                            .filter { it.isActive && it.name.isNotBlank() }
                            .sortedByDescending { it.createdAt }
                    )
                }
                .onStart { emit(Resource.Loading) }
                .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load products"))) }
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(10_000), Resource.Loading)

    fun retry() { refreshTick.value++ }

    fun productList(): Flow<List<Product>> =
        products.map { (it as? Resource.Success)?.data ?: emptyList() }

    fun product(id: String): Flow<Resource<Product>> =
        productsRef.document(id).snapshotFlow()
            .map<DocumentSnapshot, Resource<Product>> { doc ->
                Product.from(doc)?.let { Resource.Success(it) } ?: Resource.Error("This product is no longer available")
            }
            .onStart { emit(Resource.Loading) }
            .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load product"))) }

    /** Categories from Firestore if the admin set them up, otherwise derived from products. */
    val categories: Flow<List<Category>> = combine(
        firestore.collection(Constants.CATEGORIES).snapshotFlow()
            .map { snap ->
                snap.documents.map { d ->
                    Category(id = d.id, name = d.get("name").asString(d.id), image = d.get("image").asString())
                }
            }
            .catch { emit(emptyList()) }
            .onStart { emit(emptyList()) },
        productList(),
    ) { remote, products ->
        if (remote.isNotEmpty()) remote
        else {
            val names = (Constants.DEFAULT_CATEGORIES + products.map { it.category })
                .filter { it.isNotBlank() }.distinct()
            names.map { name ->
                Category(id = name, name = name, image = products.firstOrNull { it.category == name }?.mainImage.orEmpty())
            }
        }
    }

    val banners: Flow<List<Banner>> = firestore.collection(Constants.BANNERS).snapshotFlow()
        .map { snap ->
            snap.documents.map { d ->
                Banner(
                    id = d.id,
                    title = d.get("title").asString(),
                    subtitle = d.get("subtitle").asString(),
                    image = d.get("image").asString(),
                    target = d.get("target").asString(),
                )
            }.filter { it.title.isNotBlank() || it.image.isNotBlank() }
        }
        .catch { emit(emptyList()) }
        .onStart { emit(emptyList()) }

    // ── Reviews ───────────────────────────────────────────────────────────

    fun reviews(productId: String): Flow<Resource<List<Review>>> =
        productsRef.document(productId).collection(Constants.REVIEWS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshotFlow()
            .map<QuerySnapshot, Resource<List<Review>>> { snap -> Resource.Success(snap.documents.mapNotNull(Review::from)) }
            .onStart { emit(Resource.Loading) }
            .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load reviews"))) }

    /** One review per user per product (doc id = uid), so editing replaces it. */
    suspend fun submitReview(productId: String, review: Review): Resource<Unit> = resourceOf("Couldn't post review") {
        val reviewsRef = productsRef.document(productId).collection(Constants.REVIEWS)
        reviewsRef.document(review.userId).set(review.copy(id = review.userId).toMap()).await()

        // Best effort: refresh the product's aggregate rating (rules may restrict this to admins).
        runCatching {
            val all = reviewsRef.get().await().documents.mapNotNull(Review::from)
            val avg = if (all.isEmpty()) 0.0 else all.sumOf { it.rating } / all.size
            productsRef.document(productId).set(
                mapOf("rating" to (Math.round(avg * 10) / 10.0), "reviewCount" to all.size),
                SetOptions.merge(),
            ).await()
        }
        Unit
    }

    /** Best effort after an order: lower stock and raise sold count. */
    suspend fun recordSale(items: List<Pair<String, Int>>) {
        runCatching {
            val batch = firestore.batch()
            items.forEach { (id, qty) ->
                batch.update(
                    productsRef.document(id),
                    mapOf("stock" to FieldValue.increment(-qty.toLong()), "soldCount" to FieldValue.increment(qty.toLong())),
                )
            }
            batch.commit().await()
        }
    }
}
