package com.example.cherrycharm.ui.screens.profile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.data.local.PreferencesRepository
import com.example.cherrycharm.data.local.ThemeMode
import com.example.cherrycharm.data.repository.AuthRepository
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    val messages = MessageQueue()
    val theme: StateFlow<ThemeMode> = prefs.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    private val _busy = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val busy = _busy.asStateFlow()

    fun setTheme(m: ThemeMode) = viewModelScope.launch { prefs.setThemeMode(m) }

    fun changePassword(current: String, new: String, onDone: () -> Unit) = viewModelScope.launch {
        _busy.value = Resource.Loading
        val r = auth.changePassword(current, new)
        _busy.value = r
        if (r is Resource.Success) { messages.send("Password updated"); onDone() }
    }

    fun deleteAccount(password: String, onDeleted: () -> Unit) = viewModelScope.launch {
        _busy.value = Resource.Loading
        val r = auth.deleteAccount(password)
        _busy.value = r
        if (r is Resource.Success) onDeleted()
    }

    fun resetBusy() { _busy.value = Resource.Idle }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val theme by viewModel.theme.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showPassword by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)

    Scaffold(
        topBar = { AppTopBar("Settings", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            MenuGroup("Appearance") {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DarkMode, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text("Theme", style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(10.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { i, m ->
                            SegmentedButton(
                                selected = theme == m,
                                onClick = { viewModel.setTheme(m) },
                                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            ) { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        }
                    }
                }
            }
            MenuGroup("Notifications") {
                MenuRow(Icons.Filled.NotificationsActive, "Notification preferences", "Order updates, offers, price drops") {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    }
                    context.startActivity(intent)
                }
            }
            MenuGroup("Security") {
                MenuRow(Icons.Filled.Lock, "Change password") { showPassword = true }
            }
            MenuGroup("About") {
                MenuRow(Icons.Filled.Policy, "Privacy & terms") { navController.navigate(Routes.HELP) }
            }
            MenuGroup("Danger zone") {
                ClickableRow(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(16.dp))
                    Text("Delete account", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPassword) {
        PasswordDialog(
            title = "Change password",
            fields = listOf("Current password", "New password", "Confirm new password"),
            confirmLabel = "Update",
            state = busy,
            validate = { v ->
                when {
                    v[0].isBlank() -> "Enter your current password"
                    v[1].length < 6 -> "New password must be at least 6 characters"
                    v[1] != v[2] -> "New passwords don't match"
                    else -> null
                }
            },
            onConfirm = { v -> viewModel.changePassword(v[0], v[1]) { showPassword = false } },
            onDismiss = { showPassword = false; viewModel.resetBusy() },
        )
    }
    if (showDelete) {
        PasswordDialog(
            title = "Delete account?",
            message = "This permanently deletes your profile, saved addresses and wishlist. Orders stay on record for accounting.",
            fields = listOf("Password"),
            confirmLabel = "Delete forever",
            destructive = true,
            state = busy,
            validate = { v -> if (v[0].isBlank()) "Enter your password" else null },
            onConfirm = { v ->
                viewModel.deleteAccount(v[0]) {
                    showDelete = false
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            },
            onDismiss = { showDelete = false; viewModel.resetBusy() },
        )
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    fields: List<String>,
    confirmLabel: String,
    state: Resource<Unit>,
    validate: (List<String>) -> String?,
    onConfirm: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    destructive: Boolean = false,
) {
    val values = remember { mutableStateListOf(*Array(fields.size) { "" }) }
    var localError by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (message != null) Text(message, style = MaterialTheme.typography.bodyMedium)
                fields.forEachIndexed { i, label ->
                    OutlinedTextField(
                        value = values[i],
                        onValueChange = { values[i] = it; localError = null },
                        label = { Text(label) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                val err = localError ?: (state as? Resource.Error)?.message
                if (err != null) Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = state !is Resource.Loading,
                onClick = {
                    val e = validate(values.toList())
                    if (e != null) localError = e else onConfirm(values.toList())
                },
            ) {
                if (state is Resource.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── Help & FAQ ────────────────────────────────────────────────────────────────

private val faqs = listOf(
    "How long does delivery take?" to "Inside Dhaka: 1–2 working days. Outside Dhaka: 3–5 working days. You'll get a notification at every step.",
    "How much is delivery?" to "৳60 inside Dhaka, ৳120 elsewhere — and free on orders over ৳2,000.",
    "Which payment methods do you accept?" to "Cash on delivery, bKash, Nagad, and Visa/Mastercard/Amex through SSLCommerz.",
    "Can I cancel my order?" to "Yes — while it's Pending or Confirmed, open the order in My Orders and tap Cancel order.",
    "What is your return policy?" to "Return unused items in their original packaging within 7 days of delivery. Contact us and we'll arrange a pickup.",
    "My online payment failed — was I charged?" to "If money left your account but the order shows unpaid, it's automatically reversed by the gateway within 3–7 days. Contact us with your order number and we'll check.",
    "How do promo codes work?" to "Enter the code at checkout. Some codes need a minimum order amount; the discount is shown before you pay.",
    "Privacy" to "We only use your details to deliver orders and improve your experience. We never sell your data. Payments are processed by the gateway — we never see your card number.",
)

@Composable
fun HelpScreen(navController: NavController) {
    val context = LocalContext.current
    Scaffold(topBar = { AppTopBar("Help & FAQ", onBack = { navController.popBackStack() }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            MenuGroup("Contact us") {
                MenuRow(Icons.Filled.Call, "Call us", Constants.SUPPORT_PHONE) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Constants.SUPPORT_PHONE}")))
                }
                MenuRow(Icons.AutoMirrored.Filled.Chat, "WhatsApp", "Usually replies within an hour") {
                    val number = Constants.SUPPORT_PHONE.filter { it.isDigit() }
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number"))) }
                }
                MenuRow(Icons.Filled.Email, "Email", Constants.SUPPORT_EMAIL) {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Constants.SUPPORT_EMAIL}")))
                    }
                }
            }
            MenuGroup("Frequently asked") {
                faqs.forEachIndexed { i, (q, a) ->
                    FaqItem(q, a)
                    if (i < faqs.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FaqItem(q: String, a: String) {
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable { open = !open }.padding(16.dp).animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text(a, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
