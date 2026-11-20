package com.example.cherrycharm.ui.screens.catalog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cherrycharm.core.toTaka
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.ui.components.ProductCard
import com.example.cherrycharm.ui.components.ProductCardSkeleton
import com.example.cherrycharm.ui.components.RatingStars
import kotlin.math.ceil

enum class SortOption(val label: String) {
    RELEVANCE("Recommended"),
    NEWEST("Newest"),
    POPULAR("Best selling"),
    PRICE_LOW("Price: low to high"),
    PRICE_HIGH("Price: high to low"),
    RATING("Top rated"),
    DISCOUNT("Biggest discount"),
}

data class ProductFilters(
    val categories: Set<String> = emptySet(),
    val colors: Set<String> = emptySet(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val minRating: Double = 0.0,
    val inStockOnly: Boolean = false,
    val onSaleOnly: Boolean = false,
    val sort: SortOption = SortOption.RELEVANCE,
) {
    /** Number of active filters (sort doesn't count) — shown as a badge on the filter button. */
    val activeCount: Int
        get() = listOf(
            categories.isNotEmpty(), colors.isNotEmpty(), minPrice != null || maxPrice != null,
            minRating > 0, inStockOnly, onSaleOnly,
        ).count { it }

    fun apply(products: List<Product>): List<Product> {
        val filtered = products.filter { p ->
            (categories.isEmpty() || p.category in categories) &&
                (colors.isEmpty() || p.colors.any { c -> colors.any { it.equals(c, true) } }) &&
                (minPrice == null || p.finalPrice >= minPrice) &&
                (maxPrice == null || p.finalPrice <= maxPrice) &&
                p.rating >= minRating &&
                (!inStockOnly || p.inStock) &&
                (!onSaleOnly || p.hasDiscount)
        }
        return when (sort) {
            SortOption.RELEVANCE -> filtered.sortedWith(
                compareByDescending<Product> { it.inStock }.thenByDescending { it.isFeatured }.thenByDescending { it.soldCount }
            )
            SortOption.NEWEST -> filtered.sortedByDescending { it.createdAt }
            SortOption.POPULAR -> filtered.sortedByDescending { it.soldCount * 2 + it.reviewCount }
            SortOption.PRICE_LOW -> filtered.sortedBy { it.finalPrice }
            SortOption.PRICE_HIGH -> filtered.sortedByDescending { it.finalPrice }
            SortOption.RATING -> filtered.sortedWith(compareByDescending<Product> { it.rating }.thenByDescending { it.reviewCount })
            SortOption.DISCOUNT -> filtered.sortedByDescending { it.discountPercent }
        }
    }
}

// ── Product grid used by Catalog, Search and Wishlist ─────────────────────────

@Composable
fun ProductGrid(
    products: List<Product>,
    wishlist: Set<String>,
    onProductClick: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    header: (LazyGridScope.() -> Unit)? = null,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        header?.invoke(this)
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                isWishlisted = product.id in wishlist,
                onClick = { onProductClick(product) },
                onToggleWishlist = { onToggleWishlist(product) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
fun ProductGridSkeleton(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
    ) {
        items(6) { ProductCardSkeleton() }
    }
}

fun LazyGridScope.fullWidthItem(key: Any? = null, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

// ── Sort + filter bottom sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    current: ProductFilters,
    allCategories: List<String>,
    allColors: List<String>,
    priceCeiling: Double,
    resultCount: (ProductFilters) -> Int,
    onApply: (ProductFilters) -> Unit,
    onDismiss: () -> Unit,
    showCategories: Boolean = true,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember { mutableStateOf(current) }
    val ceiling = ceil(priceCeiling.coerceAtLeast(1000.0) / 500.0) * 500.0
    var range by remember {
        mutableStateOf((draft.minPrice ?: 0.0).toFloat()..(draft.maxPrice ?: ceiling).toFloat())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sort & filter", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    draft = ProductFilters(sort = draft.sort)
                    range = 0f..ceiling.toFloat()
                }) { Text("Reset") }
            }

            FilterGroup("Sort by") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SortOption.entries.forEach { opt ->
                        FilterChip(
                            selected = draft.sort == opt,
                            onClick = { draft = draft.copy(sort = opt) },
                            label = { Text(opt.label) },
                            leadingIcon = if (draft.sort == opt) { { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) } } else null,
                        )
                    }
                }
            }

            if (showCategories && allCategories.isNotEmpty()) {
                FilterGroup("Category") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allCategories.forEach { c ->
                            val on = c in draft.categories
                            FilterChip(
                                selected = on,
                                onClick = { draft = draft.copy(categories = if (on) draft.categories - c else draft.categories + c) },
                                label = { Text(c) },
                            )
                        }
                    }
                }
            }

            FilterGroup("Price") {
                Text(
                    "${range.start.toDouble().toTaka()} – ${range.endInclusive.toDouble().toTaka()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                RangeSlider(
                    value = range,
                    onValueChange = { range = it },
                    valueRange = 0f..ceiling.toFloat(),
                    onValueChangeFinished = {
                        draft = draft.copy(
                            minPrice = range.start.toDouble().takeIf { it > 0 },
                            maxPrice = range.endInclusive.toDouble().takeIf { it < ceiling },
                        )
                    },
                )
            }

            FilterGroup("Customer rating") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.0, 3.0, 4.0, 4.5).forEach { r ->
                        FilterChip(
                            selected = draft.minRating == r,
                            onClick = { draft = draft.copy(minRating = r) },
                            label = { Text(if (r == 0.0) "Any" else "$r+") },
                            leadingIcon = if (r > 0) { { RatingStars(1.0, size = 12.dp) } } else null,
                        )
                    }
                }
            }

            if (allColors.isNotEmpty()) {
                FilterGroup("Colour") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allColors.forEach { c ->
                            val on = c in draft.colors
                            FilterChip(
                                selected = on,
                                onClick = { draft = draft.copy(colors = if (on) draft.colors - c else draft.colors + c) },
                                label = { Text(c) },
                            )
                        }
                    }
                }
            }

            FilterGroup("More") {
                SwitchRow("In stock only", draft.inStockOnly) { draft = draft.copy(inStockOnly = it) }
                SwitchRow("On sale", draft.onSaleOnly) { draft = draft.copy(onSaleOnly = it) }
            }

            val count = resultCount(draft)
            Button(
                onClick = { onApply(draft); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.large,
            ) { Text(if (count == 0) "No matches" else "Show $count item${if (count == 1) "" else "s"}") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FilterGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
