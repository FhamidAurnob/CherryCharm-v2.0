package com.example.cherrycharm.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.toTaka
import com.example.cherrycharm.data.local.PreferencesRepository
import com.example.cherrycharm.data.model.Banner
import com.example.cherrycharm.data.model.Category
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.data.repository.NotificationRepository
import com.example.cherrycharm.data.repository.ProductRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.ErrorState
import com.example.cherrycharm.ui.components.MessageQueue
import com.example.cherrycharm.ui.components.ProductCard
import com.example.cherrycharm.ui.components.ProductCardSkeleton
import com.example.cherrycharm.ui.components.SectionHeader
import com.example.cherrycharm.ui.components.ShimmerBox
import com.example.cherrycharm.ui.components.ShowMessages
import com.example.cherrycharm.ui.components.safeTopPadding
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.screens.catalog.categoryEmoji
import com.example.cherrycharm.ui.screens.catalog.fullWidthItem
import com.example.cherrycharm.ui.theme.shop
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeState(
    val loading: Boolean = true,
    val error: String? = null,
    val firstName: String = "",
    val banners: List<Banner> = emptyList(),
    val categories: List<Category> = emptyList(),
    val flashSale: List<Product> = emptyList(),
    val newArrivals: List<Product> = emptyList(),
    val bestSellers: List<Product> = emptyList(),
    val recentlyViewed: List<Product> = emptyList(),
    val forYou: List<Product> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val products: ProductRepository,
    private val users: UserRepository,
    notifications: NotificationRepository,
    prefs: PreferencesRepository,
) : ViewModel() {
    val messages = MessageQueue()
    val wishlist: StateFlow<Set<String>> = users.wishlistIds
    val unread: StateFlow<Int> = notifications.unreadCount

    val state: StateFlow<HomeState> = combine(
        products.products,
        products.categories,
        products.banners,
        prefs.recentlyViewed,
        users.profile,
    ) { res, cats, banners, recent, profile ->
        val name = profile?.firstName.orEmpty()
        when (res) {
            is Resource.Success -> {
                val all = res.data
                val inStock = all.filter { it.inStock }
                val byId = all.associateBy { it.id }
                HomeState(
                    loading = false,
                    firstName = name,
                    banners = banners,
                    categories = cats,
                    flashSale = inStock.filter { it.hasDiscount }.sortedByDescending { it.discountPercent }.take(12),
                    newArrivals = all.sortedByDescending { it.createdAt }.take(10),
                    bestSellers = inStock.sortedByDescending { it.soldCount * 2 + it.reviewCount }.take(10),
                    recentlyViewed = recent.mapNotNull { byId[it] }.take(10),
                    forYou = rankForYou(all, recent.mapNotNull { byId[it] }),
                )
            }
            is Resource.Error -> HomeState(loading = false, error = res.message, firstName = name)
            else -> HomeState(loading = true, firstName = name)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** Simple personalization: favour categories the shopper has been browsing. */
    private fun rankForYou(all: List<Product>, recent: List<Product>): List<Product> {
        val affinity = recent.groupingBy { it.category }.eachCount()
        return all.sortedWith(
            compareByDescending<Product> { it.inStock }
                .thenByDescending { affinity[it.category] ?: 0 }
                .thenByDescending { it.isFeatured }
                .thenByDescending { it.rating }
        )
    }

    fun refresh() = products.retry()

    fun toggleWishlist(p: Product) {
        viewModelScope.launch {
            when (val r = users.toggleWishlist(p.id)) {
                is Resource.Success -> messages.send(if (r.data) "Saved to wishlist ❤️" else "Removed from wishlist")
                is Resource.Error -> messages.send(r.message)
                else -> Unit
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val unread by viewModel.unread.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var refreshing by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)
    LaunchedEffect(refreshing) { if (refreshing) { delay(900); refreshing = false } }

    val openProduct: (Product) -> Unit = { navController.navigate(Routes.product(it.id)) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { refreshing = true; viewModel.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            ) {
                fullWidthItem("header") {
                    HomeHeader(
                        firstName = state.firstName,
                        unread = unread,
                        onSearch = { navController.navigate(Routes.SEARCH) },
                        onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    )
                }

                if (state.error != null && state.flashSale.isEmpty()) {
                    fullWidthItem("error") {
                        ErrorState(state.error!!, onRetry = viewModel::refresh, modifier = Modifier.height(360.dp))
                    }
                    return@LazyVerticalGrid
                }

                fullWidthItem("banners") {
                    BannerCarousel(state.banners) { target ->
                        when {
                            target.startsWith("product:") -> navController.navigate(Routes.product(target.removePrefix("product:")))
                            target in setOf(Routes.COLLECTION_SALE, Routes.COLLECTION_NEW, Routes.COLLECTION_BEST, Routes.COLLECTION_FEATURED) ->
                                navController.navigate(Routes.catalog(collection = target))
                            target.isNotBlank() -> navController.navigate(Routes.catalog(category = target))
                            else -> navController.navigate(Routes.catalog(collection = Routes.COLLECTION_SALE))
                        }
                    }
                }

                fullWidthItem("perks") { PerksStrip() }

                if (state.categories.isNotEmpty()) {
                    fullWidthItem("categories") {
                        CategoryRow(state.categories) { navController.navigate(Routes.catalog(category = it.name)) }
                    }
                }

                if (state.loading) {
                    fullWidthItem("loading") { RowSkeleton() }
                } else {
                    if (state.flashSale.isNotEmpty()) {
                        fullWidthItem("flash") {
                            Column {
                                SectionHeader(
                                    title = "Flash sale",
                                    actionLabel = "See all",
                                    onAction = { navController.navigate(Routes.catalog(collection = Routes.COLLECTION_SALE)) },
                                    trailing = { Countdown() },
                                )
                                ProductRow(state.flashSale, wishlist, openProduct, viewModel::toggleWishlist)
                            }
                        }
                    }
                    if (state.newArrivals.isNotEmpty()) {
                        fullWidthItem("new") {
                            Column {
                                SectionHeader(
                                    "New arrivals", subtitle = "Fresh in this week",
                                    actionLabel = "See all",
                                    onAction = { navController.navigate(Routes.catalog(collection = Routes.COLLECTION_NEW)) },
                                )
                                ProductRow(state.newArrivals, wishlist, openProduct, viewModel::toggleWishlist)
                            }
                        }
                    }
                    if (state.bestSellers.isNotEmpty()) {
                        fullWidthItem("best") {
                            Column {
                                SectionHeader(
                                    "Best sellers", subtitle = "Loved by our customers",
                                    actionLabel = "See all",
                                    onAction = { navController.navigate(Routes.catalog(collection = Routes.COLLECTION_BEST)) },
                                )
                                ProductRow(state.bestSellers, wishlist, openProduct, viewModel::toggleWishlist)
                            }
                        }
                    }
                    if (state.recentlyViewed.isNotEmpty()) {
                        fullWidthItem("recent") {
                            Column {
                                SectionHeader(
                                    "Recently viewed",
                                    actionLabel = "See all",
                                    onAction = { navController.navigate(Routes.catalog(collection = Routes.COLLECTION_RECENT)) },
                                )
                                ProductRow(state.recentlyViewed, wishlist, openProduct, viewModel::toggleWishlist)
                            }
                        }
                    }
                    if (state.forYou.isNotEmpty()) {
                        fullWidthItem("foryou") { SectionHeader("Just for you", Modifier.padding(top = 8.dp)) }
                        items(state.forYou, key = { "fy_" + it.id }) { p ->
                            ProductCard(
                                product = p,
                                isWishlisted = p.id in wishlist,
                                onClick = { openProduct(p) },
                                onToggleWishlist = { viewModel.toggleWishlist(p) },
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(firstName: String, unread: Int, onSearch: () -> Unit, onNotifications: () -> Unit) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    Column(Modifier.safeTopPadding().padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (firstName.isNotBlank()) "$greeting, $firstName" else greeting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Find your perfect bag", style = MaterialTheme.typography.headlineSmall)
            }
            BadgedBox(badge = { if (unread > 0) Badge { Text(if (unread > 9) "9+" else "$unread") } }) {
                FilledTonalIconButton(onClick = onNotifications) {
                    Icon(Icons.Outlined.Notifications, "Notifications")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            onClick = onSearch,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text("Search totes, clutches, colours…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class FallbackBanner(val title: String, val subtitle: String, val cta: String, val target: String, val colors: List<Color>)

private val fallbackBanners = listOf(
    FallbackBanner("Season Sale", "Up to 40% off selected bags", "Shop the sale", Routes.COLLECTION_SALE,
        listOf(Color(0xFFB5274E), Color(0xFF7D0D30))),
    FallbackBanner("New Arrivals", "This week's freshest drops", "Discover", Routes.COLLECTION_NEW,
        listOf(Color(0xFF3A1C71), Color(0xFFD76D77))),
    FallbackBanner("Free delivery", "On orders over ৳${Constants.FREE_DELIVERY_THRESHOLD.toInt()}", "Start shopping", Routes.COLLECTION_BEST,
        listOf(Color(0xFF134E5E), Color(0xFF2E8B57))),
)

@Composable
private fun BannerCarousel(banners: List<Banner>, onClick: (String) -> Unit) {
    val count = if (banners.isNotEmpty()) banners.size else fallbackBanners.size
    val pager = rememberPagerState(pageCount = { count })
    LaunchedEffect(count) {
        while (count > 1) {
            delay(4500)
            pager.animateScrollToPage((pager.currentPage + 1) % count)
        }
    }
    Column(Modifier.padding(vertical = 8.dp)) {
        HorizontalPager(
            state = pager,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth().height(168.dp),
        ) { page ->
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .clickable {
                        onClick(if (banners.isNotEmpty()) banners[page].target else fallbackBanners[page].target)
                    },
            ) {
                if (banners.isNotEmpty()) {
                    val b = banners[page]
                    AsyncImage(
                        model = b.image, contentDescription = b.title, contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxSize(),
                    )
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))))
                    BannerText(b.title, b.subtitle, "Shop now")
                } else {
                    val b = fallbackBanners[page]
                    Box(Modifier.fillMaxSize().background(Brush.linearGradient(b.colors)))
                    Text(
                        "🍒", style = MaterialTheme.typography.displayLarge,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp),
                    )
                    BannerText(b.title, b.subtitle, b.cta)
                }
            }
        }
        if (count > 1) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(count) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (i == pager.currentPage) 18.dp else 6.dp, height = 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (i == pager.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.BannerText(title: String, subtitle: String, cta: String) {
    Column(Modifier.align(Alignment.CenterStart).padding(20.dp).fillMaxWidth(0.65f)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        if (subtitle.isNotBlank()) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        }
        Spacer(Modifier.height(10.dp))
        Surface(shape = RoundedCornerShape(50), color = Color.White) {
            Text(
                cta, color = Color.Black, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun PerksStrip() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.LocalShipping, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Free delivery over ${Constants.FREE_DELIVERY_THRESHOLD.toTaka()} · Cash on delivery · 7-day easy returns",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
private fun CategoryRow(categories: List<Category>, onClick: (Category) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(categories, key = { it.id }) { c ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(72.dp).clip(MaterialTheme.shapes.medium).clickable { onClick(c) },
            ) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (c.image.isNotBlank()) {
                        AsyncImage(
                            model = c.image, contentDescription = c.name, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(categoryEmoji(c.name), style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(c.name, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProductRow(
    products: List<Product>,
    wishlist: Set<String>,
    onClick: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(products, key = { it.id }) { p ->
            ProductCard(
                product = p,
                isWishlisted = p.id in wishlist,
                onClick = { onClick(p) },
                onToggleWishlist = { onToggleWishlist(p) },
                modifier = Modifier.width(156.dp),
            )
        }
    }
}

@Composable
private fun RowSkeleton() {
    Column(Modifier.padding(16.dp)) {
        ShimmerBox(Modifier.width(160.dp).height(22.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) { ProductCardSkeleton(Modifier.width(156.dp)) }
        }
    }
}

/** Counts down to midnight — today's flash sale window. */
@Composable
private fun Countdown() {
    var remaining by remember { mutableLongStateOf(msToMidnight()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            remaining = msToMidnight()
        }
    }
    val h = remaining / 3_600_000
    val m = (remaining / 60_000) % 60
    val s = (remaining / 1000) % 60
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.shop.sale, contentColor = Color.White) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, null, Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                "%02d:%02d:%02d".format(h, m, s),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun msToMidnight(): Long {
    val now = Calendar.getInstance()
    val midnight = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return midnight.timeInMillis - now.timeInMillis
}

