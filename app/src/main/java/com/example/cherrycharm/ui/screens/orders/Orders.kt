package com.example.cherrycharm.ui.screens.orders

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.shortId
import com.example.cherrycharm.core.toDate
import com.example.cherrycharm.core.toDateTime
import com.example.cherrycharm.core.toTaka
import com.example.cherrycharm.data.model.Order
import com.example.cherrycharm.data.model.OrderStatus
import com.example.cherrycharm.data.model.PaymentStatus
import com.example.cherrycharm.data.repository.CartRepository
import com.example.cherrycharm.data.repository.OrderRepository
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.navigation.navigateToTab
import com.example.cherrycharm.ui.screens.checkout.AddressSummary
import com.example.cherrycharm.ui.screens.checkout.paymentIcon
import com.example.cherrycharm.ui.theme.shop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OrderTab(val label: String) { ALL("All"), ACTIVE("Active"), DELIVERED("Delivered"), CANCELLED("Cancelled") }

@HiltViewModel
class OrdersViewModel @Inject constructor(orders: OrderRepository) : ViewModel() {
    val orders: StateFlow<Resource<List<Order>>> = orders.orders
    private val _tab = MutableStateFlow(OrderTab.ALL)
    val tab = _tab.asStateFlow()
    fun setTab(t: OrderTab) { _tab.value = t }
}

@Composable
fun statusColors(s: OrderStatus): Pair<Color, Color> = when (s) {
    OrderStatus.PENDING -> MaterialTheme.shop.warningContainer to MaterialTheme.shop.warning
    OrderStatus.CONFIRMED, OrderStatus.PROCESSING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    OrderStatus.SHIPPED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    OrderStatus.DELIVERED -> MaterialTheme.shop.successContainer to MaterialTheme.shop.onSuccessContainer
    OrderStatus.CANCELLED, OrderStatus.REFUNDED -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
}

@Composable
fun StatusPill(s: OrderStatus) {
    val (bg, fg) = statusColors(s)
    Pill(s.label, bg, fg)
}

@Composable
fun OrdersScreen(navController: NavController, viewModel: OrdersViewModel = hiltViewModel()) {
    val res by viewModel.orders.collectAsStateWithLifecycle()
    val tab by viewModel.tab.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar("My orders", onBack = { navController.popBackStack() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(OrderTab.entries) { t ->
                    FilterChip(selected = t == tab, onClick = { viewModel.setTab(t) }, label = { Text(t.label) })
                }
            }
            when (val r = res) {
                is Resource.Success -> {
                    val list = r.data.filter {
                        when (tab) {
                            OrderTab.ALL -> true
                            OrderTab.ACTIVE -> it.status.isActive
                            OrderTab.DELIVERED -> it.status == OrderStatus.DELIVERED
                            OrderTab.CANCELLED -> it.status == OrderStatus.CANCELLED || it.status == OrderStatus.REFUNDED
                        }
                    }
                    if (list.isEmpty()) {
                        EmptyState(
                            "📦",
                            if (tab == OrderTab.ALL) "No orders yet" else "No ${tab.label.lowercase()} orders",
                            "When you place an order it'll show up here so you can track it.",
                            actionLabel = "Start shopping",
                            onAction = { navController.navigateToTab(Routes.HOME) },
                        )
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(list, key = { it.id }) { o ->
                                OrderCard(o) { navController.navigate(Routes.order(o.id)) }
                            }
                        }
                    }
                }
                is Resource.Error -> ErrorState(r.message, onRetry = null)
                else -> LoadingBox()
            }
        }
    }
}

@Composable
private fun OrderCard(o: Order, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Order #${o.id.shortId()}", style = MaterialTheme.typography.titleSmall)
                    Text(o.createdAt.toDateTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(o.status)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                o.items.take(3).forEach { item ->
                    AsyncImage(
                        model = item.productImage, contentDescription = null, contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier.padding(end = 8.dp).size(54.dp).clip(MaterialTheme.shapes.small),
                    )
                }
                if (o.items.size > 3) {
                    Box(
                        Modifier.size(54.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) { Text("+${o.items.size - 3}", style = MaterialTheme.typography.labelLarge) }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${o.itemCount} item${if (o.itemCount == 1) "" else "s"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(o.totalAmount.toTaka(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            if (o.paymentMethod.isOnline && o.paymentStatus != PaymentStatus.SUCCESS && o.status == OrderStatus.PENDING) {
                Spacer(Modifier.height(10.dp))
                Text("⚠ Payment pending — tap to complete", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.shop.warning)
            }
        }
    }
}

// ══ Order detail ═════════════════════════════════════════════════════════════

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orders: OrderRepository,
    private val cart: CartRepository,
) : ViewModel() {
    val id: String = savedState.get<String>("id").orEmpty()
    val messages = MessageQueue()
    val order: StateFlow<Resource<Order>> =
        orders.order(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    fun cancel() {
        viewModelScope.launch {
            _busy.value = true
            when (val r = orders.cancel(id)) {
                is Resource.Success -> messages.send("Order cancelled")
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
            _busy.value = false
        }
    }

    fun buyAgain(onDone: () -> Unit) {
        val o = (order.value as? Resource.Success)?.data ?: return
        viewModelScope.launch {
            _busy.value = true
            when (val r = cart.addAll(o.items)) {
                is Resource.Success -> onDone()
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
            _busy.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(navController: NavController, viewModel: OrderDetailViewModel = hiltViewModel()) {
    val res by viewModel.order.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmCancel by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)

    Scaffold(
        topBar = {
            AppTopBar("Order #${viewModel.id.shortId()}", onBack = { navController.popBackStack() }) {
                val o = (res as? Resource.Success)?.data
                if (o != null) {
                    IconButton(onClick = {
                        context.startActivity(Intent.createChooser(
                            Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, receiptText(o)),
                            "Share receipt",
                        ))
                    }) { Icon(Icons.Filled.Share, "Share receipt") }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (val r = res) {
            is Resource.Success -> {
                val o = r.data
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Placed ${o.createdAt.toDateTime()}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                                    StatusPill(o.status)
                                }
                                Spacer(Modifier.height(16.dp))
                                Timeline(o)
                            }
                        }
                    }
                    if (o.paymentMethod.isOnline && o.paymentStatus != PaymentStatus.SUCCESS && o.status == OrderStatus.PENDING) {
                        item {
                            Surface(color = MaterialTheme.shop.warningContainer, shape = MaterialTheme.shapes.large) {
                                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Payment not completed yet", Modifier.weight(1f), color = MaterialTheme.shop.warning,
                                        style = MaterialTheme.typography.titleSmall)
                                    Button(onClick = { navController.navigate(Routes.payment(o.id)) }) { Text("Pay now") }
                                }
                            }
                        }
                    }
                    item { Text("Items", style = MaterialTheme.typography.titleMedium) }
                    items(o.items) { item ->
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                                .clickable { navController.navigate(Routes.product(item.productId)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AsyncImage(
                                model = item.productImage, contentDescription = null, contentScale = ContentScale.Crop,
                                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.size(64.dp).clip(MaterialTheme.shapes.medium),
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.productName, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    listOfNotNull(item.color.takeIf { it.isNotBlank() }, "Qty ${item.quantity}").joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (o.status == OrderStatus.DELIVERED) {
                                    Text("Rate this item", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { navController.navigate(Routes.reviews(item.productId)) })
                                }
                            }
                            Text(item.total.toTaka(), style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Delivery address", style = MaterialTheme.typography.titleMedium)
                            AddressSummary(o.address, onChange = null)
                        }
                    }
                    item {
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(paymentIcon(o.paymentMethod), null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(10.dp))
                                    Text(o.paymentMethod.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    Text(
                                        when (o.paymentStatus) {
                                            PaymentStatus.SUCCESS -> "Paid"
                                            PaymentStatus.FAILED -> "Failed"
                                            PaymentStatus.REFUNDED -> "Refunded"
                                            PaymentStatus.PENDING -> if (o.paymentMethod.isOnline) "Unpaid" else "Pay on delivery"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (o.paymentStatus == PaymentStatus.SUCCESS) MaterialTheme.shop.success else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                                Line("Subtotal", o.subtotal.toTaka())
                                Line("Delivery", if (o.deliveryFee == 0.0) "FREE" else o.deliveryFee.toTaka())
                                if (o.discountAmount > 0) Line("Discount${if (o.promoCode.isNotBlank()) " (${o.promoCode})" else ""}", "−${o.discountAmount.toTaka()}")
                                Row {
                                    Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    Text(o.totalAmount.toTaka(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                if (o.note.isNotBlank()) {
                                    Text("Note: ${o.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (o.status in setOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.REFUNDED)) {
                                PrimaryButton(
                                    "Buy again", icon = Icons.Filled.Replay, loading = busy,
                                    onClick = { viewModel.buyAgain { navController.navigateToTab(Routes.CART) } },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            if (o.status.canCancel) {
                                OutlinedButton(
                                    onClick = { confirmCancel = true }, enabled = !busy,
                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) { Text("Cancel order") }
                            }
                            TextButton(onClick = { navController.navigate(Routes.HELP) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.SupportAgent, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Need help with this order?")
                            }
                        }
                    }
                }
            }
            is Resource.Error -> ErrorState(r.message, onRetry = null, modifier = Modifier.padding(padding))
            else -> LoadingBox(Modifier.padding(padding))
        }
    }

    if (confirmCancel) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            icon = { Icon(Icons.Filled.Cancel, null) },
            title = { Text("Cancel this order?") },
            text = { Text("This can't be undone. If you paid online, your refund will be processed within 5–7 working days.") },
            confirmButton = {
                TextButton(onClick = { confirmCancel = false; viewModel.cancel() }) {
                    Text("Cancel order", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Keep order") } },
        )
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun stepIcon(s: OrderStatus): ImageVector = when (s) {
    OrderStatus.PENDING -> Icons.Filled.Receipt
    OrderStatus.CONFIRMED -> Icons.Filled.ThumbUp
    OrderStatus.PROCESSING -> Icons.Filled.Inventory2
    OrderStatus.SHIPPED -> Icons.Filled.LocalShipping
    OrderStatus.DELIVERED -> Icons.Filled.TaskAlt
    else -> Icons.Filled.ShoppingCartCheckout
}

@Composable
private fun Timeline(o: Order) {
    if (o.status == OrderStatus.CANCELLED || o.status == OrderStatus.REFUNDED) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Cancel, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Order ${o.status.label.lowercase()}", style = MaterialTheme.typography.titleSmall)
                Text(
                    (o.timeOf(o.status) ?: o.updatedAt).toDateTime(),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }
    val steps = OrderStatus.timeline
    val current = steps.indexOf(o.status).coerceAtLeast(0)
    val descriptions = mapOf(
        OrderStatus.PENDING to "We've received your order",
        OrderStatus.CONFIRMED to "Your order is confirmed",
        OrderStatus.PROCESSING to "We're packing your bag with care",
        OrderStatus.SHIPPED to "On its way to you",
        OrderStatus.DELIVERED to "Delivered — enjoy! 🍒",
    )
    steps.forEachIndexed { i, s ->
        val done = i <= current
        val active = i == current
        Row {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                active -> MaterialTheme.colorScheme.primary
                                done -> MaterialTheme.shop.success
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (done && !active) Icons.Filled.Check else stepIcon(s), null,
                        Modifier.size(16.dp),
                        tint = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (i < steps.lastIndex) {
                    Box(
                        Modifier.width(2.dp).height(30.dp)
                            .background(if (i < current) MaterialTheme.shop.success else MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.padding(top = 4.dp)) {
                Text(
                    s.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val time = o.timeOf(s)
                Text(
                    if (done && time != null) "${descriptions[s]} · ${time.toDate("dd MMM, hh:mm a")}" else descriptions[s].orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun receiptText(o: Order): String = buildString {
    appendLine("CherryCharm — Order #${o.id.shortId()}")
    appendLine("Placed: ${o.createdAt.toDateTime()}")
    appendLine("Status: ${o.status.label}")
    appendLine()
    o.items.forEach { appendLine("• ${it.productName}${if (it.color.isNotBlank()) " (${it.color})" else ""} ×${it.quantity} — ${it.total.toTaka()}") }
    appendLine()
    appendLine("Subtotal: ${o.subtotal.toTaka()}")
    appendLine("Delivery: ${if (o.deliveryFee == 0.0) "FREE" else o.deliveryFee.toTaka()}")
    if (o.discountAmount > 0) appendLine("Discount: −${o.discountAmount.toTaka()}")
    appendLine("Total: ${o.totalAmount.toTaka()} (${o.paymentMethod.title})")
}

