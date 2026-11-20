package com.example.cherrycharm.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.data.local.PreferencesRepository
import com.example.cherrycharm.data.model.Product
import com.example.cherrycharm.data.repository.ProductRepository
import com.example.cherrycharm.data.repository.UserRepository
import com.example.cherrycharm.ui.components.EmptyState
import com.example.cherrycharm.ui.components.MessageQueue
import com.example.cherrycharm.ui.components.ShowMessages
import com.example.cherrycharm.ui.navigation.Routes
import com.example.cherrycharm.ui.screens.catalog.FilterSheet
import com.example.cherrycharm.ui.screens.catalog.ProductFilters
import com.example.cherrycharm.ui.screens.catalog.ProductGrid
import com.example.cherrycharm.ui.screens.catalog.ProductGridSkeleton
import com.example.cherrycharm.ui.screens.catalog.SortOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val loading: Boolean = true,
    val query: String = "",
    val matches: List<Product> = emptyList(),
    val results: List<Product> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val trending: List<String> = emptyList(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val products: ProductRepository,
    private val users: UserRepository,
    private val prefs: PreferencesRepository,
) : ViewModel() {
    val messages = MessageQueue()
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()
    private val _filters = MutableStateFlow(ProductFilters())
    val filters = _filters.asStateFlow()

    val wishlist: StateFlow<Set<String>> = users.wishlistIds
    val recent: StateFlow<List<String>> =
        prefs.recentSearches.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<SearchState> = combine(
        products.products,
        _query.debounce(250).map { it.trim() }.distinctUntilChanged(),
        _filters,
    ) { res, q, f ->
        val all = (res as? Resource.Success)?.data
        if (all == null) return@combine SearchState(loading = res !is Resource.Error, query = q)
        val matches = if (q.isBlank()) emptyList() else all.filter { it.matches(q) }
        val suggestions = if (q.length < 2) emptyList() else
            (all.map { it.name } + all.map { it.category } + all.flatMap { it.tags })
                .filter { it.contains(q, ignoreCase = true) && !it.equals(q, true) }
                .distinctBy { it.lowercase() }
                .take(6)
        val trending = (all.sortedByDescending { it.soldCount }.flatMap { it.tags } + all.map { it.category })
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .take(10)
        SearchState(
            loading = false,
            query = q,
            matches = matches,
            results = f.apply(matches),
            suggestions = suggestions,
            trending = trending,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    fun onQueryChange(q: String) { _query.value = q }
    fun submit(q: String = _query.value) {
        _query.value = q
        viewModelScope.launch { prefs.addRecentSearch(q) }
    }
    fun removeRecent(q: String) = viewModelScope.launch { prefs.removeRecentSearch(q) }
    fun clearRecent() = viewModelScope.launch { prefs.clearRecentSearches() }
    fun setFilters(f: ProductFilters) { _filters.value = f }
    fun setSort(s: SortOption) { _filters.value = _filters.value.copy(sort = s) }

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(navController: NavController, viewModel: SearchViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val snackbar = remember { SnackbarHostState() }
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    ShowMessages(viewModel.messages.flow, snackbar)
    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }

    val search: (String) -> Unit = { q ->
        viewModel.submit(q)
        keyboard?.hide()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
                Row(
                    Modifier.statusBarsPadding().padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    TextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = { Text("Search CherryCharm") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange(""); focus.requestFocus() }) {
                                    Icon(Icons.Filled.Close, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search(query) }),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                        modifier = Modifier.weight(1f).focusRequester(focus),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                // ── Before typing: history + trending ─────────────────────
                state.query.isBlank() && query.isBlank() -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    if (recent.isNotEmpty()) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Recent searches", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { viewModel.clearRecent() }) { Text("Clear") }
                            }
                        }
                        items(recent) { r ->
                            ListItem(
                                headlineContent = { Text(r) },
                                leadingContent = { Icon(Icons.Filled.History, null) },
                                trailingContent = {
                                    IconButton(onClick = { viewModel.removeRecent(r) }) { Icon(Icons.Filled.Close, "Remove") }
                                },
                                modifier = Modifier.clickable { search(r) },
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                    if (state.trending.isNotEmpty()) {
                        item { Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Popular right now", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                state.trending.forEach { t ->
                                    SuggestionChip(onClick = { search(t) }, label = { Text(t) })
                                }
                            }
                        } }
                    }
                }

                // ── Typing: suggestions while the debounce settles ────────
                state.loading || state.query.isBlank() -> ProductGridSkeleton()

                state.matches.isEmpty() -> Column {
                    Suggestions(state.suggestions, onPick = search, onFill = viewModel::onQueryChange)
                    EmptyState(
                        "🔎", "No results for \"${state.query}\"",
                        "Check the spelling or try a broader word like \"tote\" or \"black\".",
                    )
                }

                else -> Column {
                    Suggestions(state.suggestions, onPick = search, onFill = viewModel::onQueryChange)
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${state.results.size} results",
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
                                    DropdownMenuItem(text = { Text(opt.label) }, onClick = { viewModel.setSort(opt); showSort = false })
                                }
                            }
                        }
                        BadgedBox(badge = { if (filters.activeCount > 0) Badge { Text("${filters.activeCount}") } }) {
                            FilledTonalIconButton(onClick = { showFilters = true }) { Icon(Icons.Filled.Tune, "Filter") }
                        }
                    }
                    if (state.results.isEmpty()) {
                        EmptyState(
                            "🧺", "No matches with these filters", "Loosen a filter to see more.",
                            actionLabel = "Clear filters", onAction = { viewModel.setFilters(ProductFilters(sort = filters.sort)) },
                        )
                    } else {
                        ProductGrid(
                            products = state.results,
                            wishlist = wishlist,
                            onProductClick = {
                                viewModel.submit(query)
                                navController.navigate(Routes.product(it.id))
                            },
                            onToggleWishlist = viewModel::toggleWishlist,
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        FilterSheet(
            current = filters,
            allCategories = state.matches.map { it.category }.filter { it.isNotBlank() }.distinct().sorted(),
            allColors = state.matches.flatMap { it.colors }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.sorted(),
            priceCeiling = state.matches.maxOfOrNull { it.finalPrice } ?: 10_000.0,
            resultCount = { it.apply(state.matches).size },
            onApply = viewModel::setFilters,
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun Suggestions(items: List<String>, onPick: (String) -> Unit, onFill: (String) -> Unit) {
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        items.forEach { s ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(s) }.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                Text(s, modifier = Modifier.weight(1f).padding(vertical = 12.dp), maxLines = 1)
                IconButton(onClick = { onFill(s) }) {
                    Icon(Icons.Filled.NorthWest, "Use", Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider()
    }
}
