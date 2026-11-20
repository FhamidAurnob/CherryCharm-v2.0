package com.example.cherrycharm.ui.screens.checkout

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.shortId
import com.example.cherrycharm.core.toTaka
import com.example.cherrycharm.data.model.Order
import com.example.cherrycharm.data.model.PaymentMethod
import com.example.cherrycharm.data.repository.OrderRepository
import com.example.cherrycharm.data.repository.PaymentRepository
import com.example.cherrycharm.ui.components.AppTopBar
import com.example.cherrycharm.ui.components.PrimaryButton
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.theme.shop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PaymentUi {
    data object Starting : PaymentUi
    data class Gateway(val url: String) : PaymentUi
    data object Verifying : PaymentUi
    data object Paid : PaymentUi
    data class Failed(val message: String) : PaymentUi
}

@HiltViewModel
class PaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orders: OrderRepository,
    private val payments: PaymentRepository,
) : ViewModel() {
    val orderId: String = savedState.get<String>("orderId").orEmpty()
    private val _ui = MutableStateFlow<PaymentUi>(PaymentUi.Starting)
    val ui = _ui.asStateFlow()
    private val _order = MutableStateFlow<Order?>(null)
    val order = _order.asStateFlow()
    private var gatewayPaymentId = ""
    private var handled = false

    init { start() }

    fun start() {
        handled = false
        viewModelScope.launch {
            _ui.value = PaymentUi.Starting
            val res = orders.order(orderId).first { it !is Resource.Loading }
            val order = (res as? Resource.Success)?.data
            if (order == null) {
                _ui.value = PaymentUi.Failed((res as? Resource.Error)?.message ?: "Order not found")
                return@launch
            }
            _order.value = order
            when (val session = payments.startSession(order)) {
                is Resource.Success -> {
                    gatewayPaymentId = session.data.gatewayPaymentId
                    _ui.value = PaymentUi.Gateway(session.data.url)
                }
                is Resource.Error -> _ui.value = PaymentUi.Failed(session.message)
                else -> Unit
            }
        }
    }

    /** True if the URL is our return URL (so the WebView should stop loading it). */
    fun onUrl(url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.host != Constants.PAYMENT_RETURN_HOST) return false
        if (handled) return true
        handled = true
        val path = uri.path.orEmpty().lowercase()
        val status = uri.getQueryParameter("status")?.lowercase()
        val method = _order.value?.paymentMethod
        val success = when {
            "fail" in path || "cancel" in path -> false
            status != null -> status == "success"
            else -> "success" in path
        }
        viewModelScope.launch {
            if (!success) {
                orders.markPaymentFailed(orderId)
                _ui.value = PaymentUi.Failed(if ("cancel" in path || status == "cancel") "Payment was cancelled" else "Payment didn't go through")
                return@launch
            }
            _ui.value = PaymentUi.Verifying
            val reference = if (method == PaymentMethod.BKASH) {
                val pid = uri.getQueryParameter("paymentID") ?: gatewayPaymentId
                when (val exec = payments.executeBkash(pid, orderId)) {
                    is Resource.Success -> exec.data
                    is Resource.Error -> { _ui.value = PaymentUi.Failed(exec.message); return@launch }
                    else -> pid
                }
            } else {
                uri.getQueryParameter("val_id") ?: uri.getQueryParameter("payment_ref_id") ?: gatewayPaymentId.ifBlank { orderId }
            }
            when (val r = orders.markPaid(orderId, reference)) {
                is Resource.Success -> _ui.value = PaymentUi.Paid
                is Resource.Error -> _ui.value = PaymentUi.Failed(r.message)
                else -> Unit
            }
        }
        return true
    }

    fun cancel() {
        viewModelScope.launch {
            orders.markPaymentFailed(orderId)
            _ui.value = PaymentUi.Failed("Payment was cancelled")
        }
    }

    fun payOnDelivery() {
        viewModelScope.launch {
            _ui.value = PaymentUi.Verifying
            when (val r = orders.switchToCashOnDelivery(orderId)) {
                is Resource.Success -> _ui.value = PaymentUi.Paid
                is Resource.Error -> _ui.value = PaymentUi.Failed(r.message)
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(navController: NavController, viewModel: PaymentViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val order by viewModel.order.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }

    LaunchedEffect(ui) {
        if (ui is PaymentUi.Paid) {
            navController.navigate(Routes.orderSuccess(viewModel.orderId)) {
                popUpTo(Routes.PAYMENT) { inclusive = true }
            }
        }
    }
    BackHandler(enabled = ui is PaymentUi.Gateway) { confirmLeave = true }

    Scaffold(
        topBar = {
            AppTopBar(
                title = order?.paymentMethod?.title?.let { "Pay with $it" } ?: "Secure payment",
                onBack = { if (ui is PaymentUi.Gateway) confirmLeave = true else navController.popBackStack() },
            ) {
                Row(Modifier.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, Modifier.size(16.dp), tint = MaterialTheme.shop.success)
                    Spacer(Modifier.width(4.dp))
                    Text(order?.totalAmount?.toTaka().orEmpty(), style = MaterialTheme.typography.titleSmall)
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = ui) {
                PaymentUi.Starting, PaymentUi.Verifying, PaymentUi.Paid -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text(if (s == PaymentUi.Starting) "Connecting to secure gateway…" else "Confirming your payment…")
                }
                is PaymentUi.Gateway -> {
                    PaymentWebView(url = s.url, onUrl = viewModel::onUrl, onProgress = { progress = it })
                    if (progress in 1..99) {
                        LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                }
                is PaymentUi.Failed -> Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.ErrorOutline, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text("Payment not completed", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${s.message}. Your order #${viewModel.orderId.shortId()} is saved — you can try again or pay in cash when it arrives.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                    PrimaryButton("Try again", onClick = viewModel::start, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = viewModel::payOnDelivery, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Pay with cash on delivery instead")
                    }
                    TextButton(onClick = { navController.navigate(Routes.order(viewModel.orderId)) { popUpTo(Routes.HOME) } }) {
                        Text("View order")
                    }
                }
            }
        }
    }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text("Cancel payment?") },
            text = { Text("Your order is saved. You can pay later from My Orders or switch to cash on delivery.") },
            confirmButton = { TextButton(onClick = { confirmLeave = false; viewModel.cancel() }) { Text("Cancel payment") } },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Continue paying") } },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PaymentWebView(url: String, onUrl: (String) -> Boolean, onProgress: (Int) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                        onUrl(request.url.toString())

                    // POST redirects (e.g. SSLCommerz success_url) don't hit shouldOverrideUrlLoading.
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        if (url != null && onUrl(url)) view.stopLoading() else super.onPageStarted(view, url, favicon)
                    }
                }
                webChromeClient = object : android.webkit.WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) { onProgress(newProgress) }
                }
                loadUrl(url)
            }
        },
        onRelease = { it.destroy() },
    )
}

// ── Order success ─────────────────────────────────────────────────────────────

@Composable
fun OrderSuccessScreen(orderId: String, onTrack: (String) -> Unit, onContinue: () -> Unit) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    BackHandler { onContinue() }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(120.dp).scale(scale.value).clip(CircleShape).background(MaterialTheme.shop.successContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, null, Modifier.size(64.dp), tint = MaterialTheme.shop.success)
        }
        Spacer(Modifier.height(28.dp))
        Text("Order placed! 🍒", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Thank you for shopping with CherryCharm. We'll send you updates as your order moves.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text("Order #${orderId.shortId()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(32.dp))
        PrimaryButton("Track my order", onClick = { onTrack(orderId) }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Continue shopping") }
    }
}
