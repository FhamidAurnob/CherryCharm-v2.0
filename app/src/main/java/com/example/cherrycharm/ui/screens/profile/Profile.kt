package com.example.cherrycharm.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.isValidBdPhone
import com.example.cherrycharm.core.toDate
import com.example.cherrycharm.data.model.OrderStatus
import com.example.cherrycharm.data.model.UserProfile
import com.example.cherrycharm.data.repository.AddressRepository
import com.example.cherrycharm.data.repository.AuthRepository
import com.example.cherrycharm.data.repository.NotificationRepository
import com.example.cherrycharm.data.repository.OrderRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.navigation.navigateToTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileStats(
    val toPay: Int = 0,
    val toShip: Int = 0,
    val shipped: Int = 0,
    val toReview: Int = 0,
    val orders: Int = 0,
    val wishlist: Int = 0,
    val addresses: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val users: UserRepository,
    private val auth: AuthRepository,
    orders: OrderRepository,
    addresses: AddressRepository,
    notifications: NotificationRepository,
) : ViewModel() {
    val messages = MessageQueue()
    val profile: StateFlow<UserProfile?> = users.profile
    val unread: StateFlow<Int> = notifications.unreadCount

    val stats: StateFlow<ProfileStats> = combine(orders.orders, users.wishlistIds, addresses.addresses) { o, w, a ->
        val list = (o as? Resource.Success)?.data.orEmpty()
        ProfileStats(
            toPay = list.count { it.paymentMethod.isOnline && it.status == OrderStatus.PENDING },
            toShip = list.count { it.status == OrderStatus.CONFIRMED || it.status == OrderStatus.PROCESSING },
            shipped = list.count { it.status == OrderStatus.SHIPPED },
            toReview = list.count { it.status == OrderStatus.DELIVERED },
            orders = list.size,
            wishlist = w.size,
            addresses = (a as? Resource.Success)?.data?.size ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileStats())

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _uploading = MutableStateFlow(false)
    val uploading = _uploading.asStateFlow()

    fun save(name: String, phone: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            when (val r = users.updateProfile(name, phone)) {
                is Resource.Success -> onDone()
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
            _saving.value = false
        }
    }

    fun uploadPhoto(uri: Uri) {
        viewModelScope.launch {
            _uploading.value = true
            when (val r = users.uploadAvatar(uri)) {
                is Resource.Success -> messages.send("Photo updated")
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
            _uploading.value = false
        }
    }

    fun logout() = auth.logout()
}

@Composable
fun Avatar(profile: UserProfile?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!profile?.profileImage.isNullOrBlank()) {
            AsyncImage(profile?.profileImage, "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                profile?.name?.trim()?.firstOrNull()?.uppercase() ?: "🍒",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
fun ProfileScreen(navController: NavController, viewModel: ProfileViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()),
    ) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background)))
                .safeTopPadding()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(profile, 72.dp, Modifier.border(3.dp, MaterialTheme.colorScheme.surface, CircleShape))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile?.name?.ifBlank { null } ?: "Welcome!", style = MaterialTheme.typography.headlineSmall)
                    Text(profile?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if ((profile?.createdAt ?: 0L) > 0) {
                        Text("Member since ${profile!!.createdAt.toDate("MMM yyyy")}", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                FilledTonalIconButton(onClick = { navController.navigate(Routes.SETTINGS) }) { Icon(Icons.Filled.Settings, "Settings") }
            }
        }

        // Orders quick status
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        ) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("My orders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { navController.navigate(Routes.ORDERS) }) { Text("View all") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OrderShortcut(Icons.Filled.Receipt, "To pay", stats.toPay) { navController.navigate(Routes.ORDERS) }
                    OrderShortcut(Icons.Filled.ShoppingBag, "To ship", stats.toShip) { navController.navigate(Routes.ORDERS) }
                    OrderShortcut(Icons.Filled.LocalShipping, "Shipped", stats.shipped) { navController.navigate(Routes.ORDERS) }
                    OrderShortcut(Icons.Filled.ThumbUp, "Delivered", stats.toReview) { navController.navigate(Routes.ORDERS) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        MenuGroup("Account") {
            MenuRow(Icons.Filled.Person, "Edit profile") { navController.navigate(Routes.EDIT_PROFILE) }
            MenuRow(Icons.Filled.LocationOn, "Addresses", badge = stats.addresses.takeIf { it > 0 }?.toString()) {
                navController.navigate(Routes.ADDRESSES)
            }
            MenuRow(Icons.Filled.Favorite, "Wishlist", badge = stats.wishlist.takeIf { it > 0 }?.toString()) {
                navController.navigateToTab(Routes.WISHLIST)
            }
            MenuRow(Icons.Filled.Notifications, "Notifications", badge = unread.takeIf { it > 0 }?.toString()) {
                navController.navigate(Routes.NOTIFICATIONS)
            }
        }
        MenuGroup("Support") {
            MenuRow(Icons.AutoMirrored.Filled.Help, "Help & FAQ") { navController.navigate(Routes.HELP) }
            MenuRow(Icons.Filled.Settings, "Settings") { navController.navigate(Routes.SETTINGS) }
        }
        MenuGroup(null) {
            ClickableRow(onClick = { confirmLogout = true }) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(16.dp))
                Text("Sign out", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Text(
            "CherryCharm v2.0 · Made with 🍒 in Dhaka",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp),
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Sign out?") },
            text = { Text("Your bag and wishlist are saved to your account.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }) { Text("Sign out") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun OrderShortcut(icon: ImageVector, label: String, count: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(8.dp),
    ) {
        BadgedBox(badge = { if (count > 0) Badge { Text("$count") } }) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun MenuGroup(title: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(content = content)
        }
    }
}

@Composable
fun MenuRow(icon: ImageVector, title: String, subtitle: String? = null, badge: String? = null, onClick: () -> Unit) {
    ClickableRow(onClick = onClick) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (badge != null) {
            Text(badge, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Edit profile ──────────────────────────────────────────────────────────────

@Composable
fun EditProfileScreen(navController: NavController, viewModel: ProfileViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()
    val uploading by viewModel.uploading.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    ShowMessages(viewModel.messages.flow, snackbar)

    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var phone by rememberSaveable(profile?.id) { mutableStateOf(profile?.phone.orEmpty()) }
    var submitted by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadPhoto(uri)
    }

    Scaffold(
        topBar = { AppTopBar("Edit profile", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box {
                Avatar(profile, 112.dp)
                if (uploading) CircularProgressIndicator(Modifier.size(112.dp))
                SmallFloatingActionButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.align(Alignment.BottomEnd),
                ) { Icon(Icons.Filled.CameraAlt, "Change photo") }
            }
            AppTextField(
                name, { name = it }, "Full name", leadingIcon = Icons.Filled.Person,
                error = if (submitted && name.trim().length < 2) "Enter your name" else null,
            )
            AppTextField(
                phone, { if (it.length <= 14) phone = it }, "Mobile number", leadingIcon = Icons.Filled.Phone,
                keyboardType = KeyboardType.Phone, placeholder = "01XXXXXXXXX",
                error = if (submitted && phone.isNotBlank() && !phone.isValidBdPhone()) "Use an 11-digit number like 01XXXXXXXXX" else null,
            )
            AppTextField(profile?.email.orEmpty(), {}, "Email", leadingIcon = Icons.Filled.Email, enabled = false)
            Text(
                "To change your email or password, go to Settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(
                "Save changes",
                loading = saving,
                onClick = {
                    submitted = true
                    if (name.trim().length >= 2 && (phone.isBlank() || phone.isValidBdPhone())) {
                        viewModel.save(name, phone) { navController.popBackStack() }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
