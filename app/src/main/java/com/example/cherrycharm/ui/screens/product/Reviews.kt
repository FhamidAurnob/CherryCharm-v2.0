package com.example.cherrycharm.ui.screens.product

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.timeAgo
import com.example.cherrycharm.data.model.Review
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.theme.shop

@Composable
fun ReviewItem(review: Review, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (review.userImage.isNotBlank()) {
                    AsyncImage(review.userImage, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        review.userName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(review.userName, style = MaterialTheme.typography.titleSmall)
                    if (review.verifiedPurchase) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.Verified, null, Modifier.size(14.dp), tint = MaterialTheme.shop.success)
                        Text(" Verified", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.shop.success)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingStars(review.rating, size = 13.dp)
                    Spacer(Modifier.width(6.dp))
                    Text(review.createdAt.timeAgo(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (review.comment.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(review.comment, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun WriteReviewDialog(
    initialRating: Int,
    initialComment: String,
    state: Resource<Unit>,
    onSubmit: (Int, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(initialRating) }
    var comment by remember { mutableStateOf(initialComment) }
    LaunchedEffect(state) { if (state is Resource.Success) onDismiss() }
    val labels = listOf("Tap a star", "Poor", "Fair", "Good", "Very good", "Excellent")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate this bag") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row {
                    (1..5).forEach { i ->
                        IconButton(onClick = { rating = i }) {
                            Icon(
                                if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$i stars",
                                tint = MaterialTheme.shop.star,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }
                Text(labels[rating], style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { if (it.length <= 500) comment = it },
                    label = { Text("Tell others what you think (optional)") },
                    minLines = 3,
                    supportingText = { Text("${comment.length}/500") },
                    modifier = Modifier.fillMaxWidth(),
                )
                (state as? Resource.Error)?.let {
                    Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }, enabled = rating > 0 && state !is Resource.Loading) {
                if (state is Resource.Loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Submit")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Full-screen photo with pinch-to-zoom and pan. */
@Composable
fun ZoomableImageDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) { offsetX += pan.x; offsetY += pan.y } else { offsetX = 0f; offsetY = 0f }
                        }
                    }
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            ) { Icon(Icons.Filled.Close, "Close", tint = Color.White) }
        }
    }
}

// ── All reviews ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(navController: NavController, viewModel: ProductViewModel = hiltViewModel()) {
    val productRes by viewModel.product.collectAsStateWithLifecycle()
    val reviewsRes by viewModel.reviews.collectAsStateWithLifecycle()
    val myReview by viewModel.myReview.collectAsStateWithLifecycle()
    val reviewState by viewModel.reviewState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showDialog by remember { mutableStateOf(false) }
    var starFilter by remember { mutableIntStateOf(0) }
    ShowMessages(viewModel.messages.flow, snackbar)

    Scaffold(
        topBar = { AppTopBar("Reviews", onBack = { navController.popBackStack() }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDialog = true },
                icon = { Icon(Icons.Filled.Star, null) },
                text = { Text(if (myReview != null) "Edit review" else "Write review") },
            )
        },
    ) { padding ->
        when (val r = reviewsRes) {
            is Resource.Success -> {
                val all = r.data
                val shown = if (starFilter == 0) all else all.filter { it.rating.toInt() == starFilter }
                val avg = if (all.isEmpty()) 0.0 else all.sumOf { it.rating } / all.size
                LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp)) {
                    item {
                        Text(
                            (productRes as? Resource.Success)?.data?.name.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                    item { Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(110.dp)) {
                                Text("%.1f".format(avg), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                RatingStars(avg, size = 16.dp)
                                Text("${all.size} reviews", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(Modifier.weight(1f)) {
                                (5 downTo 1).forEach { star ->
                                    val count = all.count { it.rating.toInt() == star }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                            .clickable { starFilter = if (starFilter == star) 0 else star }
                                            .padding(vertical = 2.dp),
                                    ) {
                                        Text("$star", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(12.dp))
                                        Icon(Icons.Filled.Star, null, Modifier.size(12.dp), tint = MaterialTheme.shop.star)
                                        Spacer(Modifier.width(6.dp))
                                        LinearProgressIndicator(
                                            progress = { if (all.isEmpty()) 0f else count / all.size.toFloat() },
                                            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.shop.star,
                                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        )
                                        Text("$count", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                    }
                                }
                            }
                        }
                        if (starFilter != 0) {
                            Spacer(Modifier.height(8.dp))
                            AssistChip(onClick = { starFilter = 0 }, label = { Text("$starFilter-star reviews · clear") })
                        }
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    } }
                    if (shown.isEmpty()) {
                        item {
                            Text(
                                if (all.isEmpty()) "No reviews yet — be the first to share your thoughts." else "No $starFilter-star reviews.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 24.dp),
                            )
                        }
                    }
                    items(shown, key = { it.id }) { review ->
                        Column {
                            ReviewItem(review, Modifier.padding(vertical = 10.dp))
                            HorizontalDivider()
                        }
                    }
                }
            }
            is Resource.Error -> ErrorState(r.message, onRetry = null, modifier = Modifier.padding(padding))
            else -> LoadingBox(Modifier.padding(padding))
        }
    }

    if (showDialog) {
        WriteReviewDialog(
            initialRating = myReview?.rating?.toInt() ?: 0,
            initialComment = myReview?.comment.orEmpty(),
            state = reviewState,
            onSubmit = viewModel::submitReview,
            onDismiss = { showDialog = false; viewModel.resetReviewState() },
        )
    }
}
