package com.example.cherrycharm.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cherrycharm.ui.MainViewModel
import com.example.cherrycharm.ui.screens.auth.ForgotPasswordScreen
import com.example.cherrycharm.ui.screens.auth.LoginScreen
import com.example.cherrycharm.ui.screens.auth.RegisterScreen
import com.example.cherrycharm.ui.screens.cart.CartScreen
import com.example.cherrycharm.ui.screens.catalog.CatalogScreen
import com.example.cherrycharm.ui.screens.catalog.ExploreScreen
import com.example.cherrycharm.ui.screens.checkout.CheckoutScreen
import com.example.cherrycharm.ui.screens.checkout.OrderSuccessScreen
import com.example.cherrycharm.ui.screens.checkout.PaymentScreen
import com.example.cherrycharm.ui.screens.home.HomeScreen
import com.example.cherrycharm.ui.screens.notifications.NotificationsScreen
import com.example.cherrycharm.ui.screens.onboarding.OnboardingScreen
import com.example.cherrycharm.ui.screens.onboarding.SplashScreen
import com.example.cherrycharm.ui.screens.orders.OrderDetailScreen
import com.example.cherrycharm.ui.screens.orders.OrdersScreen
import com.example.cherrycharm.ui.screens.product.ProductDetailScreen
import com.example.cherrycharm.ui.screens.product.ReviewsScreen
import com.example.cherrycharm.ui.screens.profile.AddressEditScreen
import com.example.cherrycharm.ui.screens.profile.AddressesScreen
import com.example.cherrycharm.ui.screens.profile.EditProfileScreen
import com.example.cherrycharm.ui.screens.profile.HelpScreen
import com.example.cherrycharm.ui.screens.profile.ProfileScreen
import com.example.cherrycharm.ui.screens.profile.SettingsScreen
import com.example.cherrycharm.ui.screens.search.SearchScreen
import com.example.cherrycharm.ui.screens.wishlist.WishlistScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Tab(Routes.EXPLORE, "Explore", Icons.Outlined.GridView, Icons.Filled.GridView),
    Tab(Routes.WISHLIST, "Wishlist", Icons.Outlined.FavoriteBorder, Icons.Filled.Favorite),
    Tab(Routes.CART, "Bag", Icons.Outlined.ShoppingBag, Icons.Filled.ShoppingBag),
    Tab(Routes.PROFILE, "Me", Icons.Outlined.Person, Icons.Filled.Person),
)

/** Navigate to a bottom-tab destination the standard way (single copy, state saved). */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Leave the auth flow and land on Home with a clean back stack. */
fun NavController.goHomeClearingStack() {
    navigate(Routes.HOME) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppNavHost(
    pendingDeepLink: String?,
    onDeepLinkHandled: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val cartCount by mainViewModel.cartCount.collectAsStateWithLifecycle()
    val wishlistCount by mainViewModel.wishlistCount.collectAsStateWithLifecycle()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val showBottomBar = currentRoute in Routes.topLevel

    // Open notification deep links once the user is inside the app.
    LaunchedEffect(pendingDeepLink, isLoggedIn, currentRoute) {
        val target = Routes.fromDeepLink(pendingDeepLink)
        val inApp = currentRoute != null && currentRoute !in setOf(Routes.SPLASH, Routes.ONBOARDING, Routes.LOGIN, Routes.REGISTER, Routes.FORGOT)
        if (pendingDeepLink != null && isLoggedIn && inApp) {
            if (target != null) navController.navigate(target) { launchSingleTop = true }
            onDeepLinkHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            label = { Text(tab.label) },
                            icon = {
                                val badge = when (tab.route) {
                                    Routes.CART -> cartCount
                                    Routes.WISHLIST -> wishlistCount
                                    else -> 0
                                }
                                BadgedBox(badge = { if (badge > 0) Badge { Text(if (badge > 99) "99+" else "$badge") } }) {
                                    Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = tab.label)
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 12 } },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(180)) + slideOutHorizontally(tween(260)) { it / 12 } },
        ) {
            // ── Entry ─────────────────────────────────────────────────────
            composable(Routes.SPLASH) {
                SplashScreen(onRoute = { route ->
                    navController.navigate(route) { popUpTo(Routes.SPLASH) { inclusive = true } }
                })
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = { navController.goHomeClearingStack() },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onForgotPassword = { navController.navigate(Routes.FORGOT) },
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = { navController.goHomeClearingStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.FORGOT) { ForgotPasswordScreen(onBack = { navController.popBackStack() }) }

            // ── Tabs ──────────────────────────────────────────────────────
            composable(Routes.HOME) { HomeScreen(navController) }
            composable(Routes.EXPLORE) { ExploreScreen(navController) }
            composable(Routes.WISHLIST) { WishlistScreen(navController) }
            composable(Routes.CART) { CartScreen(navController) }
            composable(Routes.PROFILE) { ProfileScreen(navController) }

            // ── Browse ────────────────────────────────────────────────────
            composable(Routes.SEARCH) { SearchScreen(navController) }
            composable(
                Routes.CATALOG,
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType; defaultValue = "" },
                    navArgument("collection") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { CatalogScreen(navController) }
            composable(Routes.PRODUCT, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ProductDetailScreen(navController)
            }
            composable(Routes.REVIEWS, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ReviewsScreen(navController)
            }

            // ── Buy ───────────────────────────────────────────────────────
            composable(Routes.CHECKOUT) { CheckoutScreen(navController) }
            composable(Routes.PAYMENT, arguments = listOf(navArgument("orderId") { type = NavType.StringType })) {
                PaymentScreen(navController)
            }
            composable(Routes.ORDER_SUCCESS, arguments = listOf(navArgument("orderId") { type = NavType.StringType })) { entry ->
                OrderSuccessScreen(
                    orderId = entry.arguments?.getString("orderId").orEmpty(),
                    onTrack = { id ->
                        navController.navigate(Routes.order(id)) { popUpTo(Routes.HOME) }
                    },
                    onContinue = { navController.navigateToTab(Routes.HOME) },
                )
            }
            composable(Routes.ORDERS) { OrdersScreen(navController) }
            composable(Routes.ORDER, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                OrderDetailScreen(navController)
            }

            // ── Account ───────────────────────────────────────────────────
            composable(Routes.EDIT_PROFILE) { EditProfileScreen(navController) }
            composable(Routes.ADDRESSES) { AddressesScreen(navController) }
            composable(
                Routes.ADDRESS_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.StringType; defaultValue = "" }),
            ) { AddressEditScreen(navController) }
            composable(Routes.SETTINGS) { SettingsScreen(navController) }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(navController) }
            composable(Routes.HELP) { HelpScreen(navController) }
        }
    }
}
