package com.example.cherrycharm.ui.screens.product

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.data.local.PreferencesRepository
import com.example.cherrycharm.data.model.OrderStatus
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.data.model.Review
import com.example.cherrycharm.data.repository.AuthRepository
import com.example.cherrycharm.data.repository.CartRepository
import com.example.cherrycharm.data.repository.OrderRepository
import com.example.cherrycharm.data.repository.ProductRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.MessageQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shared by the product page and the full reviews page (both have an "id" nav argument). */
@HiltViewModel
class ProductViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val products: ProductRepository,
    private val cart: CartRepository,
    private val users: UserRepository,
    private val auth: AuthRepository,
    orders: OrderRepository,
    private val prefs: PreferencesRepository,
) : ViewModel() {
    val productId: String = savedState.get<String>("id").orEmpty()
    val messages = MessageQueue()

    private val _color = MutableStateFlow("")
    val color = _color.asStateFlow()
    private val _quantity = MutableStateFlow(1)
    val quantity = _quantity.asStateFlow()
    private val _adding = MutableStateFlow(false)
    val adding = _adding.asStateFlow()
    private val _reviewState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val reviewState = _reviewState.asStateFlow()

    val product: StateFlow<Resource<Product>> = products.product(productId)
        .onEach { r ->
            // Pick the first colour automatically so "Add to bag" works in one tap.
            if (r is Resource.Success && _color.value.isBlank()) _color.value = r.data.colors.firstOrNull().orEmpty()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    val reviews: StateFlow<Resource<List<Review>>> = products.reviews(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    val related: StateFlow<List<Product>> = combine(products.productList(), product) { all, p ->
        val current = (p as? Resource.Success)?.data ?: return@combine emptyList()
        all.filter { it.id != current.id }
            .sortedWith(
                compareByDescending<Product> { it.category == current.category }
                    .thenByDescending { it.tags.intersect(current.tags.toSet()).size }
                    .thenByDescending { it.inStock }
            )
            .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isWishlisted: StateFlow<Boolean> = users.wishlistIds.map { productId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val cartCount: StateFlow<Int> = cart.count
    val wishlistIds: StateFlow<Set<String>> = users.wishlistIds

    /** Shoppers who received this product get a "Verified purchase" badge on their review. */
    val hasPurchased: StateFlow<Boolean> = orders.orders.map { r ->
        (r as? Resource.Success)?.data.orEmpty().any { o ->
            o.status == OrderStatus.DELIVERED && o.items.any { it.productId == productId }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val myReview: StateFlow<Review?> = reviews.map { r ->
        (r as? Resource.Success)?.data?.firstOrNull { it.userId == auth.currentUser?.uid }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        if (productId.isNotBlank()) viewModelScope.launch { prefs.addRecentlyViewed(productId) }
    }

    fun selectColor(c: String) { _color.value = c }

    fun changeQuantity(delta: Int) {
        val stock = (product.value as? Resource.Success)?.data?.stock ?: Constants.MAX_QTY_PER_ITEM
        _quantity.value = (_quantity.value + delta).coerceIn(1, minOf(stock, Constants.MAX_QTY_PER_ITEM).coerceAtLeast(1))
    }

    /** Adds to bag; [then] runs on success (e.g. go to checkout for "Buy now"). */
    fun addToCart(onViewBag: () -> Unit, then: (() -> Unit)? = null) {
        val p = (product.value as? Resource.Success)?.data ?: return
        if (p.colors.isNotEmpty() && _color.value.isBlank()) {
            messages.send("Please choose a colour")
            return
        }
        if (_adding.value) return
        viewModelScope.launch {
            _adding.value = true
            when (val r = cart.add(p, _color.value, _quantity.value)) {
                is Resource.Success -> {
                    if (then != null) then()
                    else messages.send("Added to your bag", actionLabel = "View bag", onAction = onViewBag)
                }
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
            _adding.value = false
        }
    }

    fun toggleWishlist() {
        viewModelScope.launch {
            when (val r = users.toggleWishlist(productId)) {
                is Resource.Success -> messages.send(if (r.data) "Saved to wishlist ❤️" else "Removed from wishlist")
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
        }
    }

    fun toggleRelatedWishlist(p: Product) {
        viewModelScope.launch {
            (users.toggleWishlist(p.id) as? Resource.Error)?.let { messages.send(it.message) }
        }
    }

    fun submitReview(rating: Int, comment: String) {
        val user = auth.currentUser ?: run { messages.send("Please sign in to review"); return }
        viewModelScope.launch {
            _reviewState.value = Resource.Loading
            val profileName = users.profile.value?.name
            val review = Review(
                userId = user.uid,
                userName = profileName?.takeIf { it.isNotBlank() } ?: user.displayName ?: "Customer",
                userImage = users.profile.value?.profileImage?.takeIf { it.isNotBlank() } ?: user.photoUrl?.toString().orEmpty(),
                rating = rating.toDouble(),
                comment = comment.trim(),
                verifiedPurchase = hasPurchased.value,
                createdAt = System.currentTimeMillis(),
            )
            val r = products.submitReview(productId, review)
            _reviewState.value = r
            if (r is Resource.Success) messages.send("Thanks for your review! ⭐")
            else if (r is Resource.Error) messages.send(r.message)
        }
    }

    fun resetReviewState() { _reviewState.value = Resource.Idle }
}
