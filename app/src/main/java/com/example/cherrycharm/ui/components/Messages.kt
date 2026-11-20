package com.example.cherrycharm.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** A one-off message for a snackbar, optionally with an action such as "Undo" or "View bag". */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/** One-shot event pipe for ViewModels: survives recomposition, delivered exactly once. */
class MessageQueue {
    private val channel = Channel<UiMessage>(Channel.BUFFERED)
    val flow: Flow<UiMessage> = channel.receiveAsFlow()
    fun send(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        channel.trySend(UiMessage(text, actionLabel, onAction))
    }
}

@Composable
fun ShowMessages(messages: Flow<UiMessage>, host: SnackbarHostState) {
    LaunchedEffect(messages, host) {
        messages.collect { msg ->
            host.currentSnackbarData?.dismiss()
            val result = host.showSnackbar(
                message = msg.text,
                actionLabel = msg.actionLabel,
                withDismissAction = msg.actionLabel == null,
                duration = if (msg.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) msg.onAction?.invoke()
        }
    }
}
