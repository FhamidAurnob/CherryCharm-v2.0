package com.example.cherrycharm.ui.screens.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.toTaka
import com.example.cherrycharm.data.model.CartLine
import com.example.cherrycharm.data.repository.CartRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.navigation.navigateToTab
import com.example.cherrycharm.ui.theme.shop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartSummary(
    val itemCount: Int = 0,
    val subtotal: Double = 0.0,
    val savings: Double = 0.0,
    val hasProblems: Boolean = false,
) {
    val toFreeDelivery: Double get() = (Constants.FREE_DELIVERY_THRESHOLD - subtotal).coerceAtLeast(0.0)
    val freeDeliveryProgress: Float get() = (subtotal / Constants.FREE_DELIVERY_THRESHOLD).toFloat().coerceIn(0f, 1f)
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cart: CartRepository,
    private val users: UserRepository,
) : ViewModel() {
    val messages = MessageQueue()
    val lines: StateFlow<Resource<List<CartLine>>> = cart.lines

    val summary: StateFlow<CartSummary> = cart.lines.map { r ->
        val list = (r as? Resource.Success)?.data.orEmpty()
        CartSummary(
            itemCount = list.sumOf { it.item.quantity },
            subtotal = list.filter { it.available }.sumOf { it.total },
            savings = list.sumOf { l ->
                val p = l.product
                if (p != null && p.hasDiscount) (p.price - p.finalPrice) * l.item.quantity else 0.0
            },
            hasProblems = list.any { !it.available || it.exceedsStock },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CartSummary())

    fun changeQty(line: CartLine, delta: Int) {
        val max = minOf(Constants.MAX_QTY_PER_ITEM, line.product?.stock ?: Constants.MAX_QTY_PER_ITEM)
        val newQty = (line.item.quantity + delta).coerceAtMost(max)
        if (newQty == line.item.quantity) {
            messages.send("Only $max available")
            return
        }
        if (newQty <= 0) { remove(line); return }
        viewModelScope.launch { (cart.updateQuantity(line.item.id, newQty) as? Resource.Error)?.let { messages.send(it.message) } }
    }

    fun fixQuantity(line: CartLine) {
        val stock = line.product?.stock ?: return
        viewModelScope.launch {
            if (stock <= 0) cart.remove(line.item.id) else cart.updateQuantity(line.item.id, stock)
        }
    }

    fun remove(line: CartLine) {
        viewModelScope.launch {
            when (val r = cart.remove(line.item.id)) {
                is Resource.Success -> messages.send("Removed ${line.item.productName}", "Undo") {
                    viewModelScope.launch { cart.restore(line.item) }
                }
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
        }
    }

    fun moveToWishlist(line: CartLine) {
        viewModelScope.launch {
            users.setWishlisted(line.item.productId, true)
            cart.remove(line.item.id)
            messages.send("Moved to wishlist ❤️")
        }
    }

    fun clear() {
        viewModelScope.launch { cart.clear() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController, viewModel: CartViewModel = hiltViewModel()) {
    val linesRes by viewModel.lines.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmClear by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)

    Scaffold(
        topBar = {
            AppTopBar(if (summary.itemCount > 0) "My bag (${summary.itemCount})" else "My bag") {
                if (summary.itemCount > 0) TextButton(onClick = { confirmClear = true }) { Text("Clear") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            val lines = (linesRes as? Resource.Success)?.data.orEmpty()
            if (lines.isNotEmpty()) {
                CheckoutBar(summary) { navController.navigate(Routes.CHECKOUT) }
            }
        },
    ) { padding ->
        when (val r = linesRes) {
            is Resource.Success -> {
                if (r.data.isEmpty()) {
                    EmptyState(
                        "🛍️", "Your bag is empty",
                        "Looks like you haven't added anything yet. Let's find something you'll love.",
                        modifier = Modifier.padding(padding),
                        actionLabel = "Start shopping",
                        onAction = { navController.navigateToTab(Routes.HOME) },
                    )
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item { FreeDeliveryBanner(summary) }
                        items(r.data, key = { it.item.id }) { line ->
                            CartLineCard(
                                line = line,
                                onOpen = { navController.navigate(Routes.product(line.item.productId)) },
                                onInc = { viewModel.changeQty(line, +1) },
                                onDec = { viewModel.changeQty(line, -1) },
                                onRemove = { viewModel.remove(line) },
                                onSave = { viewModel.moveToWishlist(line) },
                                onFix = { viewModel.fixQuantity(line) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Secure checkout · Cash on delivery available",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            is Resource.Error -> ErrorState(r.message, onRetry = null, modifier = Modifier.padding(padding))
            else -> LoadingBox(Modifier.padding(padding))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Empty your bag?") },
            text = { Text("All items will be removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clear(); confirmClear = false }) {
                    Text("Empty bag", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep") } },
        )
    }
}

@Composable
private fun FreeDeliveryBanner(summary: CartSummary) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalShipping, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (summary.toFreeDelivery <= 0) "🎉 You've unlocked FREE delivery!"
                    else "Add ${summary.toFreeDelivery.toTaka()} more for FREE delivery",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { summary.freeDeliveryProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    onOpen: () -> Unit,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemove: () -> Unit,
    onSave: () -> Unit,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = line.item
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row {
                AsyncImage(
                    model = line.product?.mainImage ?: item.productImage,
                    contentDescription = item.productName,
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.size(92.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onOpen),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        line.product?.name ?: item.productName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onOpen),
                    )
                    if (item.color.isNotBlank()) {
                        Text("Colour: ${item.color}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    PriceRow(line.unitPrice, line.product?.takeIf { it.hasDiscount }?.price)
                    if (line.priceChanged) {
                        Text(
                            "Price updated from ${item.price.toTaka()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.shop.warning,
                        )
                    }
                }
            }
            if (!line.available || line.exceedsStock) {
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.shop.warningContainer, shape = MaterialTheme.shapes.small) {
                    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WarningAmber, null, tint = MaterialTheme.shop.warning, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (!line.available) "Sold out — remove to continue" else "Only ${line.product?.stock} left in stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.shop.warning,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onFix) { Text(if (!line.available) "Remove" else "Update") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSave, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.FavoriteBorder, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save for later")
                }
                IconButton(onClick = onRemove) { Icon(Icons.Filled.DeleteOutline, "Remove") }
                Spacer(Modifier.weight(1f))
                QuantityStepper(
                    quantity = item.quantity,
                    onDecrease = onDec,
                    onIncrease = onInc,
                    compact = true,
                    canIncrease = line.available && item.quantity < minOf(Constants.MAX_QTY_PER_ITEM, line.product?.stock ?: Constants.MAX_QTY_PER_ITEM),
                )
            }
        }
    }
}

@Composable
private fun CheckoutBar(summary: CartSummary, onCheckout: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(summary.subtotal.toTaka(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (summary.savings > 0) {
                        Text("You save ${summary.savings.toTaka()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.shop.success)
                    }
                }
                PrimaryButton(
                    "Checkout",
                    onClick = onCheckout,
                    enabled = !summary.hasProblems && summary.subtotal > 0,
                    modifier = Modifier.widthIn(min = 170.dp),
                )
            }
            if (summary.hasProblems) {
                Text(
                    "Resolve the highlighted items to continue",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

