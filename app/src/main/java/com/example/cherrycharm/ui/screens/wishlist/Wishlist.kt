package com.example.cherrycharm.ui.screens.wishlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.data.repository.CartRepository
import com.example.cherrycharm.data.repository.ProductRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.navigation.navigateToTab
import com.example.cherrycharm.ui.screens.catalog.ProductGridSkeleton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(
    products: ProductRepository,
    private val users: UserRepository,
    private val cart: CartRepository,
) : ViewModel() {
    val messages = MessageQueue()

    /** Null while loading. Keeps the order items were saved in (newest last in Firestore → reversed). */
    val items: StateFlow<List<Product>?> = combine(products.products, users.wishlistIds, users.profile) { res, ids, profile ->
        val all = (res as? Resource.Success)?.data ?: return@combine null
        val order = profile?.wishlist?.reversed() ?: ids.toList()
        val byId = all.associateBy { it.id }
        order.mapNotNull { byId[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun remove(p: Product) {
        viewModelScope.launch {
            when (val r = users.setWishlisted(p.id, false)) {
                is Resource.Success -> messages.send("Removed ${p.name}", "Undo") {
                    viewModelScope.launch { users.setWishlisted(p.id, true) }
                }
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
        }
    }

    fun moveToBag(p: Product, onViewBag: () -> Unit) {
        viewModelScope.launch {
            when (val r = cart.add(p, p.colors.firstOrNull().orEmpty(), 1)) {
                is Resource.Success -> {
                    users.setWishlisted(p.id, false)
                    messages.send("Moved to your bag", "View bag", onViewBag)
                }
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
        }
    }
}

@Composable
fun WishlistScreen(navController: NavController, viewModel: WishlistViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ShowMessages(viewModel.messages.flow, snackbar)

    Scaffold(
        topBar = { AppTopBar(items?.size?.takeIf { it > 0 }?.let { "Wishlist ($it)" } ?: "Wishlist") },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = items
        when {
            list == null -> ProductGridSkeleton(Modifier.padding(padding))
            list.isEmpty() -> EmptyState(
                "🤍", "Nothing saved yet",
                "Tap the heart on any bag to save it here. We'll let you know if the price drops.",
                modifier = Modifier.padding(padding),
                actionLabel = "Discover bags",
                onAction = { navController.navigateToTab(Routes.HOME) },
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(list, key = { it.id }) { p ->
                    Column(Modifier.animateItem()) {
                        ProductCard(
                            product = p,
                            isWishlisted = true,
                            onClick = { navController.navigate(Routes.product(p.id)) },
                            onToggleWishlist = { viewModel.remove(p) },
                        )
                        FilledTonalButton(
                            onClick = { viewModel.moveToBag(p) { navController.navigateToTab(Routes.CART) } },
                            enabled = p.inStock,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Icon(Icons.Filled.ShoppingBag, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (p.inStock) "Move to bag" else "Sold out")
                        }
                    }
                }
            }
        }
    }
}
