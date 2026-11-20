package com.example.cherrycharm.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.timeAgo
import com.example.cherrycharm.data.model.AppNotification
import com.example.cherrycharm.data.repository.NotificationRepository
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(private val repo: NotificationRepository) : ViewModel() {
    val notifications: StateFlow<Resource<List<AppNotification>>> = repo.notifications
    val unread: StateFlow<Int> = repo.unreadCount
    fun open(n: AppNotification) = viewModelScope.launch { if (!n.isRead) repo.markRead(n.id) }
    fun markAllRead() = viewModelScope.launch { repo.markAllRead() }
    fun delete(n: AppNotification) = viewModelScope.launch { repo.delete(n.id) }
    fun clearAll() = viewModelScope.launch { repo.clearAll() }
}

private fun emojiFor(type: String) = when (type) {
    "order" -> "📦"
    "promo" -> "🎁"
    "price_drop" -> "💰"
    "new_arrival" -> "✨"
    "review" -> "⭐"
    else -> "🔔"
}

private fun groupLabel(ts: Long): String {
    val start = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return when {
        ts >= start -> "Today"
        ts >= start - 86_400_000L -> "Yesterday"
        ts >= start - 7 * 86_400_000L -> "This week"
        else -> "Earlier"
    }
}

@Composable
fun NotificationsScreen(navController: NavController, viewModel: NotificationsViewModel = hiltViewModel()) {
    val res by viewModel.notifications.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    var menu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar("Notifications", onBack = { navController.popBackStack() }) {
                if (unread > 0) {
                    IconButton(onClick = { viewModel.markAllRead() }) { Icon(Icons.Filled.DoneAll, "Mark all read") }
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Clear all") }, onClick = { menu = false; confirmClear = true })
                    }
                }
            }
        },
    ) { padding ->
        when (val r = res) {
            is Resource.Success -> if (r.data.isEmpty()) {
                EmptyState("🔔", "You're all caught up", "Order updates, deals and price drops will appear here.", Modifier.padding(padding))
            } else {
                val groups = r.data.groupBy { groupLabel(it.createdAt) }
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
                    groups.forEach { (label, list) ->
                        item(key = "h_$label") {
                            Text(
                                label, style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        items(list, key = { it.id }) { n ->
                            NotificationRow(
                                n,
                                onClick = {
                                    viewModel.open(n)
                                    Routes.fromDeepLink(n.deepLink)?.let { navController.navigate(it) }
                                },
                                onDelete = { viewModel.delete(n) },
                                modifier = Modifier.animateItem(),
                            )
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
            title = { Text("Clear all notifications?") },
            confirmButton = { TextButton(onClick = { confirmClear = false; viewModel.clearAll() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NotificationRow(n: AppNotification, onClick: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        color = if (n.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) { Text(emojiFor(n.type), style = MaterialTheme.typography.titleLarge) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (n.isRead) FontWeight.Medium else FontWeight.Bold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (!n.isRead) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
                if (n.body.isNotBlank()) {
                    Text(n.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(n.createdAt.timeAgo(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, "Dismiss", Modifier.size(18.dp)) }
        }
    }
}
