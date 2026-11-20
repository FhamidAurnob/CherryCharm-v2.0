package com.example.cherrycharm.ui.screens.checkout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.toTaka
import com.example.cherrycharm.data.model.Address
import com.example.cherrycharm.data.model.PaymentMethod
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.theme.shop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(navController: NavController, viewModel: CheckoutViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val promoInput by viewModel.promoInput.collectAsStateWithLifecycle()
    val promoChecking by viewModel.promoChecking.collectAsStateWithLifecycle()
    val promoError by viewModel.promoError.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val placing by viewModel.placing.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAddresses by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)

    // If the bag empties (e.g. order placed in another tab), leave checkout.
    LaunchedEffect(state.loading, state.lines.isEmpty(), placing) {
        if (!state.loading && state.lines.isEmpty() && !placing &&
            navController.currentDestination?.route == Routes.CHECKOUT
        ) navController.popBackStack()
    }

    Scaffold(
        topBar = { AppTopBar("Checkout", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            PlaceOrderBar(
                total = state.total,
                method = state.payment,
                enabled = state.canPlace,
                loading = placing,
                onPlace = {
                    viewModel.placeOrder { order ->
                        val route = if (order.paymentMethod.isOnline) Routes.payment(order.id) else Routes.orderSuccess(order.id)
                        navController.navigate(route) { popUpTo(Routes.CART) }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) { LoadingBox(Modifier.padding(padding)); return@Scaffold }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepCard(1, "Delivery address") {
                val a = state.address
                if (a == null) {
                    OutlinedButton(
                        onClick = { navController.navigate(Routes.addressEdit()) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add a delivery address")
                    }
                } else {
                    AddressSummary(a, onChange = { showAddresses = true })
                }
            }

            StepCard(2, "Items (${state.itemCount})") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.lines, key = { it.item.id }) { line ->
                        Box {
                            AsyncImage(
                                model = line.product?.mainImage ?: line.item.productImage,
                                contentDescription = line.item.productName,
                                contentScale = ContentScale.Crop,
                                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                                modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.medium),
                            )
                            if (line.item.quantity > 1) {
                                Badge(Modifier.align(Alignment.TopEnd).padding(4.dp)) { Text("×${line.item.quantity}") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Estimated delivery: ${if (state.address?.isInsideDhaka != false) "1–2 days" else "3–5 days"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            StepCard(3, "Payment") {
                PaymentMethod.entries.forEach { m ->
                    PaymentOption(m, selected = state.payment == m) { viewModel.selectPayment(m) }
                }
            }

            StepCard(4, "Promo code") {
                AnimatedVisibility(visible = state.promo != null) {
                    state.promo?.let { p ->
                        Surface(color = MaterialTheme.shop.successContainer, shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.shop.success)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.code, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.shop.onSuccessContainer)
                                    Text(
                                        state.promoWarning ?: "${p.label} · saving ${state.discount.toTaka()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (state.promoWarning != null) MaterialTheme.shop.warning else MaterialTheme.shop.onSuccessContainer,
                                    )
                                }
                                IconButton(onClick = viewModel::removePromo) { Icon(Icons.Filled.Close, "Remove code") }
                            }
                        }
                    }
                }
                if (state.promo == null) {
                    Row(verticalAlignment = Alignment.Top) {
                        OutlinedTextField(
                            value = promoInput,
                            onValueChange = viewModel::setPromoInput,
                            placeholder = { Text("Enter code") },
                            leadingIcon = { Icon(Icons.Filled.LocalOffer, null) },
                            isError = promoError != null,
                            supportingText = promoError?.let { { Text(it) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { viewModel.applyPromo() }),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = viewModel::applyPromo,
                            enabled = promoInput.isNotBlank() && !promoChecking,
                            modifier = Modifier.height(56.dp),
                        ) {
                            if (promoChecking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            else Text("Apply")
                        }
                    }
                }
            }

            StepCard(5, "Note for delivery (optional)") {
                OutlinedTextField(
                    value = note,
                    onValueChange = viewModel::setNote,
                    placeholder = { Text("e.g. Call before arriving, gate code…") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order summary", style = MaterialTheme.typography.titleMedium)
                    SummaryRow("Subtotal (${state.itemCount} items)", state.subtotal.toTaka())
                    SummaryRow(
                        "Delivery" + if (state.address != null) (if (state.address!!.isInsideDhaka) " · Inside Dhaka" else " · Outside Dhaka") else "",
                        if (state.deliveryFee == 0.0) "FREE" else state.deliveryFee.toTaka(),
                        valueColor = if (state.deliveryFee == 0.0) MaterialTheme.shop.success else null,
                    )
                    if (state.discount > 0) {
                        SummaryRow("Promo (${state.promo?.code})", "−${state.discount.toTaka()}", valueColor = MaterialTheme.shop.success)
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(state.total.toTaka(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                    if (state.deliveryFee > 0 && state.subtotal < Constants.FREE_DELIVERY_THRESHOLD) {
                        Text(
                            "Add ${(Constants.FREE_DELIVERY_THRESHOLD - state.subtotal).toTaka()} more for free delivery",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAddresses) {
        AddressPickerSheet(
            addresses = state.addresses,
            selectedId = state.address?.id,
            onSelect = { viewModel.selectAddress(it.id); showAddresses = false },
            onAddNew = { showAddresses = false; navController.navigate(Routes.addressEdit()) },
            onDismiss = { showAddresses = false },
        )
    }
}

@Composable
private fun StepCard(step: Int, title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("$step", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

@Composable
fun AddressSummary(a: Address, onChange: (() -> Unit)?) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().then(if (onChange != null) Modifier.clickable(onClick = onChange) else Modifier),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.fullName, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(8.dp))
                    Pill(a.label, MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Text(a.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(a.singleLine, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (onChange != null) {
                Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

fun paymentIcon(m: PaymentMethod): ImageVector = when (m) {
    PaymentMethod.COD -> Icons.Filled.Payments
    PaymentMethod.BKASH, PaymentMethod.NAGAD -> Icons.Filled.PhoneAndroid
    PaymentMethod.CARD -> Icons.Filled.CreditCard
}

@Composable
private fun PaymentOption(m: PaymentMethod, selected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).selectable(selected = selected, onClick = onSelect),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(paymentIcon(m), null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(m.title, style = MaterialTheme.typography.titleSmall)
                Text(m.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = onSelect)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = valueColor ?: MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PlaceOrderBar(total: Double, method: PaymentMethod, enabled: Boolean, loading: Boolean, onPlace: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(total.toTaka(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            PrimaryButton(
                text = if (method.isOnline) "Pay ${total.toTaka()}" else "Place order",
                onClick = onPlace,
                enabled = enabled,
                loading = loading,
                icon = Icons.Filled.Lock,
                modifier = Modifier.widthIn(min = 180.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressPickerSheet(
    addresses: List<Address>,
    selectedId: String?,
    onSelect: (Address) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Deliver to", style = MaterialTheme.typography.titleLarge)
            addresses.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = a.id == selectedId, onClick = { onSelect(a) })
                    Box(Modifier.weight(1f).clip(MaterialTheme.shapes.large).clickable { onSelect(a) }) { AddressSummary(a, onChange = null) }
                }
            }
            OutlinedButton(onClick = onAddNew, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add new address")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
