package com.example.cherrycharm.ui.screens.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.data.local.PreferencesRepository
import com.example.cherrycharm.data.model.Category
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.data.repository.ProductRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.AppTopBar
import com.example.cherrycharm.ui.components.EmptyState
import com.example.cherrycharm.ui.components.ErrorState
import com.example.cherrycharm.ui.components.MessageQueue
import com.example.cherrycharm.ui.components.ShowMessages
import com.example.cherrycharm.ui.components.safeTopPadding
import com.example.cherrycharm.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ══ Catalog (one category or collection) ══════════════════════════════════════

data class CatalogState(
    val title: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val base: List<Product> = emptyList(),
    val visible: List<Product> = emptyList(),
)

@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val products: ProductRepository,
    private val users: UserRepository,
    prefs: PreferencesRepository,
) : ViewModel() {
    val category: String = savedState.get<String>("category").orEmpty()
    val collection: String = savedState.get<String>("collection").orEmpty()
    val messages = MessageQueue()

    private val _filters = MutableStateFlow(
        ProductFilters(
            sort = when (collection) {
                Routes.COLLECTION_NEW -> SortOption.NEWEST
                Routes.COLLECTION_BEST -> SortOption.POPULAR
                Routes.COLLECTION_SALE -> SortOption.DISCOUNT
                else -> SortOption.RELEVANCE
            }
        )
    )
    val filters = _filters.asStateFlow()
    val wishlist: StateFlow<Set<String>> = users.wishlistIds

    val state: StateFlow<CatalogState> = combine(products.products, _filters, prefs.recentlyViewed) { res, f, recent ->
        val title = when {
            category.isNotBlank() -> category
            collection == Routes.COLLECTION_SALE -> "On sale"
            collection == Routes.COLLECTION_NEW -> "New arrivals"
            collection == Routes.COLLECTION_BEST -> "Best sellers"
            collection == Routes.COLLECTION_FEATURED -> "Featured"
            collection == Routes.COLLECTION_RECENT -> "Recently viewed"
            else -> "All bags"
        }
        when (res) {
            is Resource.Success -> {
                val all = res.data
                val base = when {
                    category.isNotBlank() -> all.filter { it.category.equals(category, ignoreCase = true) }
                    collection == Routes.COLLECTION_SALE -> all.filter { it.hasDiscount }
                    collection == Routes.COLLECTION_NEW -> all.sortedByDescending { it.createdAt }.take(40)
                    collection == Routes.COLLECTION_BEST -> all.sortedByDescending { it.soldCount }.take(40)
                    collection == Routes.COLLECTION_FEATURED -> all.filter { it.isFeatured }
                    collection == Routes.COLLECTION_RECENT -> recent.mapNotNull { id -> all.firstOrNull { it.id == id } }
                    else -> all
                }
                val visible = if (collection == Routes.COLLECTION_RECENT && f.activeCount == 0 && f.sort == SortOption.RELEVANCE) base else f.apply(base)
                CatalogState(title, loading = false, base = base, visible = visible)
            }
            is Resource.Error -> CatalogState(title, loading = false, error = res.message)
            else -> CatalogState(title, loading = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CatalogState())

    fun setFilters(f: ProductFilters) { _filters.value = f }
    fun setSort(s: SortOption) { _filters.value = _filters.value.copy(sort = s) }
    fun retry() = products.retry()

    fun toggleWishlist(p: Product) {
        viewModelScope.launch {
            when (val r = users.toggleWishlist(p.id)) {
                is Resource.Success -> messages.send(if (r.data) "Saved to wishlist" else "Removed from wishlist")
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(navController: NavController, viewModel: CatalogViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)

    Scaffold(
        topBar = {
            AppTopBar(state.title.ifBlank { "Shop" }, onBack = { navController.popBackStack() }) {
                IconButton(onClick = { navController.navigate(Routes.SEARCH) }) { Icon(Icons.Filled.Search, "Search") }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Toolbar: count · sort · filter
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (state.loading) "Loading…" else "${state.visible.size} item${if (state.visible.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box {
                    TextButton(onClick = { showSort = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(filters.sort.label)
                    }
                    DropdownMenu(expanded = showSort, onDismissRequest = { showSort = false }) {
                        SortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label) },
                                onClick = { viewModel.setSort(opt); showSort = false },
                            )
                        }
                    }
                }
                BadgedBox(badge = { if (filters.activeCount > 0) Badge { Text("${filters.activeCount}") } }) {
                    FilledTonalIconButton(onClick = { showFilters = true }) { Icon(Icons.Filled.Tune, "Filter") }
                }
            }

            when {
                state.loading -> ProductGridSkeleton()
                state.error != null -> ErrorState(state.error!!, onRetry = viewModel::retry)
                state.base.isEmpty() -> EmptyState(
                    "👜", "Nothing here yet",
                    "New pieces are added often — check back soon or explore other categories.",
                    actionLabel = "Browse all", onAction = { navController.navigate(Routes.catalog()) },
                )
                state.visible.isEmpty() -> EmptyState(
                    "🔍", "No matches", "Try removing a filter or two.",
                    actionLabel = "Clear filters", onAction = { viewModel.setFilters(ProductFilters(sort = filters.sort)) },
                )
                else -> ProductGrid(
                    products = state.visible,
                    wishlist = wishlist,
                    onProductClick = { navController.navigate(Routes.product(it.id)) },
                    onToggleWishlist = viewModel::toggleWishlist,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                )
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            current = filters,
            allCategories = state.base.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
            allColors = state.base.flatMap { it.colors }.map { it.trim() }.filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }.sorted(),
            priceCeiling = state.base.maxOfOrNull { it.finalPrice } ?: 10_000.0,
            resultCount = { it.apply(state.base).size },
            onApply = viewModel::setFilters,
            onDismiss = { showFilters = false },
            showCategories = viewModel.category.isBlank(),
        )
    }
}

// ══ Explore tab (categories + collections) ════════════════════════════════════

@HiltViewModel
class ExploreViewModel @Inject constructor(products: ProductRepository) : ViewModel() {
    val categories: StateFlow<List<Category>> =
        products.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val counts: StateFlow<Map<String, Int>> = combine(products.productList(), categories) { list, _ ->
        list.groupingBy { it.category }.eachCount()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

private data class ShopCollection(val title: String, val key: String, val icon: ImageVector, val colors: List<Color>)

@Composable
fun ExploreScreen(navController: NavController, viewModel: ExploreViewModel = hiltViewModel()) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val collections = listOf(
        ShopCollection("Sale", Routes.COLLECTION_SALE, Icons.Filled.Bolt, listOf(Color(0xFFD7263D), Color(0xFFFF6B6B))),
        ShopCollection("New in", Routes.COLLECTION_NEW, Icons.Filled.AutoAwesome, listOf(Color(0xFF6A3093), Color(0xFFA044FF))),
        ShopCollection("Best sellers", Routes.COLLECTION_BEST, Icons.Filled.LocalFireDepartment, listOf(Color(0xFFE65C00), Color(0xFFF9A825))),
        ShopCollection("Featured", Routes.COLLECTION_FEATURED, Icons.Filled.Star, listOf(Color(0xFF7D0D30), Color(0xFFB5274E))),
    )

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeTopPadding()) {
        Text(
            "Explore",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 16.dp),
        )
        Surface(
            onClick = { navController.navigate(Routes.SEARCH) },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
        ) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text("Search bags, colours, styles", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(collections) { c ->
                Box(
                    Modifier
                        .size(width = 132.dp, height = 72.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(Brush.linearGradient(c.colors))
                        .clickable { navController.navigate(Routes.catalog(collection = c.key)) }
                        .padding(12.dp),
                ) {
                    Icon(c.icon, null, tint = Color.White, modifier = Modifier.align(Alignment.TopStart))
                    Text(
                        c.title, color = Color.White, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                }
            }
        }
        Text(
            "Shop by category",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                CategoryTile("All bags", "", counts.values.sum(), onClick = { navController.navigate(Routes.catalog()) })
            }
            items(categories, key = { it.id }) { cat ->
                CategoryTile(cat.name, cat.image, counts[cat.name] ?: 0) {
                    navController.navigate(Routes.catalog(category = cat.name))
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(name: String, image: String, count: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick),
    ) {
        if (image.isNotBlank()) {
            AsyncImage(
                model = image, contentDescription = name, contentScale = ContentScale.Crop,
                placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                categoryEmoji(name),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.align(Alignment.Center).padding(bottom = 24.dp),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.6f)))
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(name, color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (count > 0) Text("$count items", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun categoryEmoji(name: String): String = when {
    name.contains("tote", true) -> "👜"
    name.contains("clutch", true) -> "👛"
    name.contains("back", true) -> "🎒"
    name.contains("wallet", true) -> "💳"
    name.contains("cross", true) || name.contains("sling", true) -> "👝"
    name.contains("travel", true) || name.contains("luggage", true) -> "🧳"
    name.contains("office", true) || name.contains("laptop", true) -> "💼"
    else -> "🛍️"
}
