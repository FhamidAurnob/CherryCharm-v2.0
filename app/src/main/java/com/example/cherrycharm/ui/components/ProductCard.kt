package com.example.cherrycharm.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.ui.theme.shop

@Composable
fun ProductCard(
    product: Product,
    isWishlisted: Boolean,
    onClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            AsyncImage(
                model = product.mainImage,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxSize(),
            )
            if (product.hasDiscount) {
                DiscountBadge(product.discountPercent, Modifier.align(Alignment.TopStart).padding(10.dp))
            }
            WishlistHeart(
                isWishlisted = isWishlisted,
                onToggle = onToggleWishlist,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
            if (!product.inStock) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = RoundedCornerShape(50), color = Color.White) {
                        Text(
                            "Sold out",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            } else if (product.isLowStock) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.shop.warningContainer,
                    contentColor = MaterialTheme.shop.warning,
                ) {
                    Text(
                        "Only ${product.stock} left",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
            Text(
                product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            if (product.reviewCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, Modifier.size(13.dp), tint = MaterialTheme.shop.star)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${"%.1f".format(product.rating)} (${product.reviewCount})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
            PriceRow(product.finalPrice, if (product.hasDiscount) product.price else null)
        }
    }
}

@Composable
fun WishlistHeart(isWishlisted: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    var bump by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (bump) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        finishedListener = { bump = false },
        label = "heart",
    )
    val tint by animateColorAsState(
        if (isWishlisted) MaterialTheme.shop.sale else MaterialTheme.colorScheme.onSurface,
        label = "heartTint",
    )
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        onClick = { bump = true; onToggle() },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (isWishlisted) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = if (isWishlisted) "Remove from wishlist" else "Add to wishlist",
                tint = tint,
                modifier = Modifier.size(19.dp).scale(scale),
            )
        }
    }
}

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier) {
        ShimmerBox(Modifier.fillMaxWidth().aspectRatio(0.8f), MaterialTheme.shapes.large)
        Spacer(Modifier.height(10.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.9f).height(14.dp), RoundedCornerShape(4.dp))
        Spacer(Modifier.height(6.dp))
        ShimmerBox(Modifier.fillMaxWidth(0.5f).height(14.dp), RoundedCornerShape(4.dp))
    }
}
