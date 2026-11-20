package com.example.cherrycharm.data.repository

import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.friendlyMessage
import com.example.cherrycharm.core.resourceOf
import com.example.cherrycharm.data.model.CartItem
import com.example.cherrycharm.data.model.CartLine
import com.example.cherrycharm.data.model.OrderItem
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.di.ApplicationScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class CartRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
    products: ProductRepository,
    @ApplicationScope scope: CoroutineScope,
) {
    private fun cartRef(uid: String) =
        firestore.collection(Constants.CARTS).document(uid).collection(Constants.ITEMS)

    val items: StateFlow<Resource<List<CartItem>>> = auth.userId.flatMapLatest { uid ->
        if (uid == null) flowOf<Resource<List<CartItem>>>(Resource.Success(emptyList()))
        else cartRef(uid).snapshotFlow()
            .map<QuerySnapshot, Resource<List<CartItem>>> { snap ->
                Resource.Success(snap.documents.mapNotNull(CartItem::from).sortedByDescending { it.addedAt })
            }
            .onStart { emit(Resource.Loading) }
            .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load your bag"))) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(10_000), Resource.Loading)

    /** Cart lines joined with live product data so prices and stock are always current. */
    val lines: StateFlow<Resource<List<CartLine>>> = combine(items, products.products) { cart, catalog ->
        val result: Resource<List<CartLine>> = when (cart) {
            is Resource.Success -> {
                val byId = (catalog as? Resource.Success)?.data?.associateBy { it.id } ?: emptyMap()
                Resource.Success(cart.data.map { CartLine(it, byId[it.productId]) })
            }
            is Resource.Error -> cart
            else -> Resource.Loading
        }
        result
    }.stateIn(scope, SharingStarted.WhileSubscribed(10_000), Resource.Loading)

    val count: StateFlow<Int> = items
        .map { r -> (r as? Resource.Success)?.data?.sumOf { it.quantity } ?: 0 }
        .stateIn(scope, SharingStarted.WhileSubscribed(10_000), 0)

    suspend fun add(product: Product, color: String, quantity: Int): Resource<Unit> = resourceOf("Couldn't add to bag") {
        val uid = auth.requireUid()
        require(product.inStock) { "Sorry, this item is out of stock." }
        val ref = cartRef(uid).document(CartItem.keyFor(product.id, color))
        firestore.runTransaction<Unit> { tx ->
            val snap = tx.get(ref)
            val existing = (snap.getLong("quantity") ?: 0L).toInt()
            val newQty = (existing + quantity)
                .coerceAtMost(minOf(Constants.MAX_QTY_PER_ITEM, product.stock))
                .coerceAtLeast(1)
            val item = CartItem(
                id = ref.id,
                productId = product.id,
                productName = product.name,
                productImage = product.mainImage.orEmpty(),
                price = product.finalPrice,
                quantity = newQty,
                color = color,
                addedAt = if (snap.exists()) (snap.getLong("addedAt") ?: System.currentTimeMillis()) else System.currentTimeMillis(),
            )
            tx.set(ref, item.toMap())
            Unit
        }.await()
        Unit
    }

    suspend fun updateQuantity(itemId: String, quantity: Int): Resource<Unit> = resourceOf("Couldn't update quantity") {
        val ref = cartRef(auth.requireUid()).document(itemId)
        if (quantity <= 0) ref.delete().await()
        else ref.update("quantity", quantity.coerceAtMost(Constants.MAX_QTY_PER_ITEM)).await()
        Unit
    }

    suspend fun remove(itemId: String): Resource<Unit> = resourceOf("Couldn't remove item") {
        cartRef(auth.requireUid()).document(itemId).delete().await()
        Unit
    }

    /** Puts a removed line back (used by the "Undo" snackbar). */
    suspend fun restore(item: CartItem): Resource<Unit> = resourceOf("Couldn't restore item") {
        cartRef(auth.requireUid()).document(item.id).set(item.toMap()).await()
        Unit
    }

    suspend fun clear(): Resource<Unit> = resourceOf("Couldn't clear bag") {
        val ref = cartRef(auth.requireUid())
        val docs = ref.get().await().documents
        if (docs.isNotEmpty()) {
            val batch = firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        Unit
    }

    /** "Buy again": puts a past order's items back in the bag. */
    suspend fun addAll(items: List<OrderItem>): Resource<Unit> = resourceOf("Couldn't add items") {
        val ref = cartRef(auth.requireUid())
        val batch = firestore.batch()
        val now = System.currentTimeMillis()
        items.forEachIndexed { i, it ->
            val key = CartItem.keyFor(it.productId, it.color)
            batch.set(
                ref.document(key),
                CartItem(
                    id = key, productId = it.productId, productName = it.productName,
                    productImage = it.productImage, price = it.price,
                    quantity = it.quantity.coerceAtMost(Constants.MAX_QTY_PER_ITEM),
                    color = it.color, addedAt = now + i,
                ).toMap(),
            )
        }
        batch.commit().await()
        Unit
    }
}
