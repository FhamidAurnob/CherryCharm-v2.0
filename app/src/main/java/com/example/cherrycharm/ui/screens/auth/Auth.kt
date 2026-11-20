package com.example.cherrycharm.ui.screens.auth

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.isValidBdPhone
import com.example.cherrycharm.core.isValidEmail
import com.example.cherrycharm.data.repository.AuthRepository
import com.example.cherrycharm.ui.components.AppTextField
import com.example.cherrycharm.ui.components.AppTopBar
import com.example.cherrycharm.ui.components.PrimaryButton
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val auth: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val state = _state.asStateFlow()

    private val _resetSent = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val resetSent = _resetSent.asStateFlow()

    fun login(email: String, password: String) = launchAuth { auth.login(email, password) }

    fun register(name: String, email: String, phone: String, password: String) =
        launchAuth { auth.register(name, email, phone, password) }

    fun sendReset(email: String) {
        viewModelScope.launch {
            _resetSent.value = Resource.Loading
            _resetSent.value = auth.resetPassword(email)
        }
    }

    fun clearError() {
        if (_state.value is Resource.Error) _state.value = Resource.Idle
    }

    private fun launchAuth(block: suspend () -> Resource<Unit>) {
        if (_state.value is Resource.Loading) return
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = block()
        }
    }
}

// ── Shared pieces ─────────────────────────────────────────────────────────────

@Composable
private fun BrandHeader(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background))
            )
            .statusBarsPadding()
            .padding(top = 40.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🍒", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    imeAction: ImeAction,
    onIme: () -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Lock, null) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onIme() }, onNext = { onIme() }),
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ErrorBanner(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(14.dp))
    }
}

/** Test account for debug builds — create it once in Firebase Console → Authentication → Users. */
private const val TEST_EMAIL = "test@cherrycharm.com"
private const val TEST_PASSWORD = "Test@123"

// ── Login ─────────────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Debug builds pre-fill a test account so you can sign in with one tap. Release builds start empty.
    val context = LocalContext.current
    val isDebug = remember { (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0 }
    var email by rememberSaveable { mutableStateOf(if (isDebug) TEST_EMAIL else "") }
    var password by rememberSaveable { mutableStateOf(if (isDebug) TEST_PASSWORD else "") }
    var submitted by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current

    val emailError = if (submitted && !email.isValidEmail()) "Enter a valid email" else null
    val passwordError = if (submitted && password.isEmpty()) "Enter your password" else null

    LaunchedEffect(state) { if (state is Resource.Success) onLoggedIn() }

    val submit = {
        submitted = true
        focus.clearFocus()
        if (email.isValidEmail() && password.isNotEmpty()) viewModel.login(email, password)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
    ) {
        BrandHeader("Welcome back", "Sign in to continue shopping")
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (state as? Resource.Error)?.let { ErrorBanner(it.message) }
            AppTextField(
                value = email,
                onValueChange = { email = it; viewModel.clearError() },
                label = "Email",
                leadingIcon = Icons.Filled.Email,
                error = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            PasswordField(
                value = password,
                onValueChange = { password = it; viewModel.clearError() },
                label = "Password",
                error = passwordError,
                imeAction = ImeAction.Done,
                onIme = submit,
            )
            TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot password?")
            }
            PrimaryButton(
                text = "Sign in",
                onClick = submit,
                loading = state is Resource.Loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("New to CherryCharm?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRegister) { Text("Create account") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Register ──────────────────────────────────────────────────────────────────

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var agreed by rememberSaveable { mutableStateOf(true) }
    var submitted by remember { mutableStateOf(false) }

    val nameError = if (submitted && name.trim().length < 2) "Enter your name" else null
    val emailError = if (submitted && !email.isValidEmail()) "Enter a valid email" else null
    val phoneError = if (submitted && phone.isNotBlank() && !phone.isValidBdPhone()) "Use an 11-digit number like 01XXXXXXXXX" else null
    val passwordError = if (submitted && password.length < 6) "At least 6 characters" else null
    val confirmError = if (submitted && confirm != password) "Passwords don't match" else null
    val strength = passwordStrength(password)

    LaunchedEffect(state) { if (state is Resource.Success) onRegistered() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding(),
    ) {
        AppTopBar(title = "", onBack = onBack)
        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Create your account", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Save your favourites, check out faster and track orders.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            (state as? Resource.Error)?.let { ErrorBanner(it.message) }
            AppTextField(name, { name = it; viewModel.clearError() }, "Full name", leadingIcon = Icons.Filled.Person, error = nameError)
            AppTextField(email, { email = it; viewModel.clearError() }, "Email", leadingIcon = Icons.Filled.Email, error = emailError, keyboardType = KeyboardType.Email)
            AppTextField(
                phone, { if (it.length <= 14) phone = it }, "Mobile number (optional)",
                leadingIcon = Icons.Filled.Phone, error = phoneError, keyboardType = KeyboardType.Phone,
                placeholder = "01XXXXXXXXX",
            )
            PasswordField(password, { password = it }, "Password", passwordError, ImeAction.Next)
            if (password.isNotEmpty()) {
                LinearProgressIndicator(
                    progress = { strength.first },
                    color = strength.third,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
                Text(strength.second, style = MaterialTheme.typography.labelSmall, color = strength.third)
            }
            PasswordField(confirm, { confirm = it }, "Confirm password", confirmError, ImeAction.Done)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = agreed, onCheckedChange = { agreed = it })
                Text(
                    "I agree to the Terms & Privacy Policy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PrimaryButton(
                text = "Create account",
                onClick = {
                    submitted = true
                    val ok = name.trim().length >= 2 && email.isValidEmail() && password.length >= 6 &&
                        confirm == password && (phone.isBlank() || phone.isValidBdPhone())
                    if (ok) viewModel.register(name, email, phone, password)
                },
                enabled = agreed,
                loading = state is Resource.Loading,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onBack) { Text("Sign in") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun passwordStrength(p: String): Triple<Float, String, androidx.compose.ui.graphics.Color> {
    var score = 0
    if (p.length >= 6) score++
    if (p.length >= 10) score++
    if (p.any { it.isDigit() } && p.any { it.isLetter() }) score++
    if (p.any { it.isUpperCase() } && p.any { !it.isLetterOrDigit() }) score++
    return when (score) {
        0, 1 -> Triple(0.25f, "Weak", MaterialTheme.colorScheme.error)
        2 -> Triple(0.5f, "Fair", androidx.compose.ui.graphics.Color(0xFFE69500))
        3 -> Triple(0.75f, "Good", androidx.compose.ui.graphics.Color(0xFF43A047))
        else -> Triple(1f, "Strong", androidx.compose.ui.graphics.Color(0xFF2E7D32))
    }
}

// ── Forgot password ───────────────────────────────────────────────────────────

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit, viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.resetSent.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar("Reset password", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state is Resource.Success) {
                Spacer(Modifier.height(32.dp))
                Icon(
                    Icons.Filled.MarkEmailRead, null,
                    Modifier.size(72.dp).align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Check your inbox",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text(
                    "We sent a reset link to $email. It may take a minute — check spam too.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                PrimaryButton("Back to sign in", onClick = onBack, modifier = Modifier.fillMaxWidth())
            } else {
                Text(
                    "Enter the email you signed up with and we'll send you a link to set a new password.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                (state as? Resource.Error)?.let { ErrorBanner(it.message) }
                AppTextField(
                    email, { email = it }, "Email",
                    leadingIcon = Icons.Filled.Email,
                    error = if (submitted && !email.isValidEmail()) "Enter a valid email" else null,
                    keyboardType = KeyboardType.Email,
                )
                PrimaryButton(
                    "Send reset link",
                    onClick = { submitted = true; if (email.isValidEmail()) viewModel.sendReset(email) },
                    loading = state is Resource.Loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
