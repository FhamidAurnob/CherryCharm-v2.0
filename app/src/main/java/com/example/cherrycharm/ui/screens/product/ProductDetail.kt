package com.example.cherrycharm.ui.screens.product

import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.toDate
import com.example.cherrycharm.core.toTaka
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.data.model.Review
import com.example.cherrycharm.ui.components.*
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.navigation.navigateToTab
import com.example.cherrycharm.ui.theme.shop
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(navController: NavController, viewModel: ProductViewModel = hiltViewModel()) {
    val productRes by viewModel.product.collectAsStateWithLifecycle()
    val reviews by viewModel.reviews.collectAsStateWithLifecycle()
    val related by viewModel.related.collectAsStateWithLifecycle()
    val wishlisted by viewModel.isWishlisted.collectAsStateWithLifecycle()
    val color by viewModel.color.collectAsStateWithLifecycle()
    val quantity by viewModel.quantity.collectAsStateWithLifecycle()
    val adding by viewModel.adding.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()
    val wishlistIds by viewModel.wishlistIds.collectAsStateWithLifecycle()
    val reviewState by viewModel.reviewState.collectAsStateWithLifecycle()
    val myReview by viewModel.myReview.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showReviewDialog by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)

    val goToBag: () -> Unit = { navController.navigateToTab(Routes.CART) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            val p = (productRes as? Resource.Success)?.data
            if (p != null) {
                BuyBar(
                    product = p,
                    adding = adding,
                    onAdd = { viewModel.addToCart(onViewBag = goToBag) },
                    onBuyNow = { viewModel.addToCart(onViewBag = goToBag, then = { navController.navigate(Routes.CHECKOUT) }) },
                )
            }
        },
    ) { padding ->
        when (val res = productRes) {
            is Resource.Loading, Resource.Idle -> LoadingBox()
            is Resource.Error -> Column(Modifier.fillMaxSize().statusBarsPadding()) {
                AppTopBar("", onBack = { navController.popBackStack() })
                ErrorState(res.message, onRetry = null)
            }
            is Resource.Success -> {
                val p = res.data
                Box(Modifier.fillMaxSize().padding(padding)) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        item { Gallery(p) }
                        item { InfoSection(p, reviews, onReviews = { navController.navigate(Routes.reviews(p.id)) }) }
                        if (p.colors.isNotEmpty()) {
                            item { ColorSection(p.colors, color, viewModel::selectColor) }
                        }
                        if (p.inStock) {
                            item {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Quantity", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    QuantityStepper(
                                        quantity = quantity,
                                        onDecrease = { viewModel.changeQuantity(-1) },
                                        onIncrease = { viewModel.changeQuantity(+1) },
                                        canIncrease = quantity < minOf(p.stock, Constants.MAX_QTY_PER_ITEM),
                                    )
                                }
                            }
                        }
                        item { DeliveryCard() }
                        item { DescriptionSection(p) }
                        item {
                            ReviewsPreview(
                                product = p,
                                reviews = (reviews as? Resource.Success)?.data.orEmpty(),
                                hasMyReview = myReview != null,
                                onSeeAll = { navController.navigate(Routes.reviews(p.id)) },
                                onWrite = { showReviewDialog = true },
                            )
                        }
                        if (related.isNotEmpty()) {
                            item { Column {
                                SectionHeader("You may also like", Modifier.padding(top = 16.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    items(related, key = { it.id }) { r ->
                                        ProductCard(
                                            product = r,
                                            isWishlisted = r.id in wishlistIds,
                                            onClick = { navController.navigate(Routes.product(r.id)) },
                                            onToggleWishlist = { viewModel.toggleRelatedWishlist(r) },
                                            modifier = Modifier.width(150.dp),
                                        )
                                    }
                                }
                            } }
                        }
                    }

                    // Floating actions over the gallery
                    Row(
                        Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RoundAction(Icons.AutoMirrored.Filled.ArrowBack, "Back") { navController.popBackStack() }
                        Spacer(Modifier.weight(1f))
                        RoundAction(Icons.Filled.Share, "Share") {
                            val text = "Check out ${p.name} for ${p.finalPrice.toTaka()} on CherryCharm 🍒\nbagsapp://product/${p.id}"
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text),
                                    "Share",
                                )
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        WishlistHeart(wishlisted, viewModel::toggleWishlist, Modifier.size(40.dp))
                        Spacer(Modifier.width(8.dp))
                        BadgedBox(badge = { if (cartCount > 0) Badge { Text("$cartCount") } }) {
                            RoundAction(Icons.Outlined.ShoppingBag, "Bag", onClick = goToBag)
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        WriteReviewDialog(
            initialRating = myReview?.rating?.toInt() ?: 0,
            initialComment = myReview?.comment.orEmpty(),
            state = reviewState,
            onSubmit = viewModel::submitReview,
            onDismiss = { showReviewDialog = false; viewModel.resetReviewState() },
        )
    }
}

@Composable
private fun RoundAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, label, Modifier.size(20.dp)) }
    }
}

@Composable
private fun Gallery(p: Product) {
    val images = p.images.ifEmpty { listOf("") }
    val pager = rememberPagerState(pageCount = { images.size })
    var zoomed by remember { mutableStateOf<String?>(null) }
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.9f)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = images[page].ifBlank { null },
                    contentDescription = "${p.name} photo ${page + 1}",
                    contentScale = ContentScale.Crop,
                    placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                    error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.fillMaxSize().clickable { if (images[page].isNotBlank()) zoomed = images[page] },
                )
            }
            if (images.size > 1) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                ) {
                    Text(
                        "${pager.currentPage + 1}/${images.size}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            if (p.hasDiscount) DiscountBadge(p.discountPercent, Modifier.align(Alignment.BottomStart).padding(14.dp))
        }
        if (images.size > 1) {
            val scope = rememberCoroutineScope()
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(images) { i, url ->
                    val selected = i == pager.currentPage
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier
                            .size(58.dp)
                            .clip(MaterialTheme.shapes.small)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = MaterialTheme.shapes.small,
                            )
                            .clickable { scope.launch { pager.animateScrollToPage(i) } },
                    )
                }
            }
        }
    }
    zoomed?.let { url -> ZoomableImageDialog(url) { zoomed = null } }
}

@Composable
private fun InfoSection(p: Product, reviews: Resource<List<Review>>, onReviews: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            p.category.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.2.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(p.name, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        if (p.reviewCount > 0) {
            Row(
                Modifier.clip(MaterialTheme.shapes.small).clickable(onClick = onReviews).padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RatingStars(p.rating, size = 16.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${"%.1f".format(p.rating)} · ${p.reviewCount} review${if (p.reviewCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (p.soldCount > 0) {
                    Text(
                        " · ${p.soldCount}+ sold",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        PriceRow(p.finalPrice, if (p.hasDiscount) p.price else null, big = true)
        if (p.hasDiscount) {
            Text(
                "You save ${(p.price - p.finalPrice).toTaka()} (${p.discountPercent}%)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.shop.success,
            )
        }
        Spacer(Modifier.height(10.dp))
        when {
            !p.inStock -> Pill("Out of stock", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
            p.isLowStock -> Pill(
                "Hurry, only ${p.stock} left", MaterialTheme.shop.warningContainer, MaterialTheme.shop.warning,
                icon = Icons.Filled.Inventory2,
            )
            else -> Pill(
                "In stock", MaterialTheme.shop.successContainer, MaterialTheme.shop.onSuccessContainer,
                icon = Icons.Filled.Check,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSection(colors: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row {
            Text("Colour: ", style = MaterialTheme.typography.titleSmall)
            Text(selected, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            colors.forEach { c ->
                val isSel = c == selected
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(c) }) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .border(2.dp, if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(colorSwatch(c))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSel) Icon(Icons.Filled.Check, null, Modifier.size(16.dp), tint = if (isDark(colorSwatch(c))) Color.White else Color.Black)
                    }
                    Text(c, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

fun colorSwatch(name: String): Color = when (name.trim().lowercase()) {
    "black" -> Color(0xFF1A1A1A)
    "white", "ivory", "off white", "off-white" -> Color(0xFFF8F6F0)
    "brown", "chocolate" -> Color(0xFF6B3E26)
    "tan", "camel" -> Color(0xFFC19A6B)
    "beige", "nude", "cream" -> Color(0xFFE8D9C0)
    "navy", "navy blue" -> Color(0xFF1F2A44)
    "blue" -> Color(0xFF2F6FD6)
    "red" -> Color(0xFFC62828)
    "maroon", "burgundy", "wine" -> Color(0xFF7B1E33)
    "pink", "blush" -> Color(0xFFF2A7B9)
    "green", "olive" -> Color(0xFF556B2F)
    "grey", "gray" -> Color(0xFF9E9E9E)
    "gold" -> Color(0xFFD4AF37)
    "silver" -> Color(0xFFC0C0C0)
    "purple", "lavender" -> Color(0xFF8E6BBF)
    "orange" -> Color(0xFFE67E22)
    "yellow", "mustard" -> Color(0xFFE1B530)
    else -> Color(0xFFBDBDBD)
}

private fun isDark(c: Color) = (0.299 * c.red + 0.587 * c.green + 0.114 * c.blue) < 0.55

@Composable
private fun DeliveryCard() {
    val eta = remember {
        val from = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 2) }.timeInMillis
        val to = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 5) }.timeInMillis
        "${from.toDate("dd MMM")} – ${to.toDate("dd MMM")}"
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Perk(Icons.Filled.LocalShipping, "Delivery $eta", "Free over ${Constants.FREE_DELIVERY_THRESHOLD.toTaka()} · Dhaka ${Constants.DELIVERY_INSIDE_DHAKA.toTaka()} · Elsewhere ${Constants.DELIVERY_OUTSIDE_DHAKA.toTaka()}")
            Perk(Icons.Filled.Payments, "Cash on delivery available", "Or pay with bKash, Nagad or card")
            Perk(Icons.AutoMirrored.Filled.AssignmentReturn, "7-day easy returns", "Unused items in original packaging")
            Perk(Icons.Filled.Verified, "100% authentic", "Quality-checked before it ships")
        }
    }
}

@Composable
private fun Perk(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DescriptionSection(p: Product) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp).animateContentSize()) {
        Text("About this bag", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            p.description.ifBlank { "A CherryCharm favourite — thoughtfully designed for everyday carry." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
        )
        if (p.description.length > 180) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "Show less" else "Read more")
            }
        }
        if (expanded || p.description.length <= 180) {
            Spacer(Modifier.height(8.dp))
            SpecRow("Category", listOf(p.category, p.subcategory).filter { it.isNotBlank() }.joinToString(" · "))
            if (p.colors.isNotEmpty()) SpecRow("Colours", p.colors.joinToString(", "))
            if (p.sku.isNotBlank()) SpecRow("SKU", p.sku)
        }
        if (p.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                p.tags.forEach { t ->
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text("#$t", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ReviewsPreview(
    product: Product,
    reviews: List<Review>,
    hasMyReview: Boolean,
    onSeeAll: () -> Unit,
    onWrite: () -> Unit,
) {
    Column(Modifier.padding(top = 20.dp)) {
        SectionHeader(
            "Reviews",
            subtitle = if (reviews.isEmpty()) "No reviews yet" else "${"%.1f".format(product.rating)} out of 5 · ${reviews.size} reviews",
            actionLabel = if (reviews.size > 2) "See all" else null,
            onAction = onSeeAll,
        )
        reviews.take(2).forEach { ReviewItem(it, Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        OutlinedButton(
            onClick = onWrite,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) { Text(if (hasMyReview) "Edit your review" else "Write a review") }
    }
}

@Composable
private fun BuyBar(product: Product, adding: Boolean, onAdd: () -> Unit, onBuyNow: () -> Unit) {
    Surface(shadowElevation = 12.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!product.inStock) {
                Button(onClick = {}, enabled = false, modifier = Modifier.weight(1f).height(52.dp)) { Text("Sold out") }
            } else {
                OutlinedButton(
                    onClick = onAdd,
                    enabled = !adding,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Filled.ShoppingBag, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add to bag")
                }
                PrimaryButton("Buy now", onClick = onBuyNow, loading = adding, modifier = Modifier.weight(1f))
            }
        }
    }
}

