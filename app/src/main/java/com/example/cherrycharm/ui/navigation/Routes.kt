package com.example.cherrycharm.ui.navigation

import android.net.Uri

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot_password"

    // Bottom tabs
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val WISHLIST = "wishlist"
    const val CART = "cart"
    const val PROFILE = "profile"

    const val SEARCH = "search"
    const val CATALOG = "catalog?category={category}&collection={collection}"
    const val PRODUCT = "product/{id}"
    const val REVIEWS = "reviews/{id}"
    const val CHECKOUT = "checkout"
    const val PAYMENT = "payment/{orderId}"
    const val ORDER_SUCCESS = "order_success/{orderId}"
    const val ORDERS = "orders"
    const val ORDER = "order/{id}"
    const val EDIT_PROFILE = "edit_profile"
    const val ADDRESSES = "addresses"
    const val ADDRESS_EDIT = "address_edit?id={id}"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    const val HELP = "help"

    fun catalog(category: String? = null, collection: String? = null) =
        "catalog?category=${Uri.encode(category.orEmpty())}&collection=${Uri.encode(collection.orEmpty())}"

    fun product(id: String) = "product/$id"
    fun reviews(id: String) = "reviews/$id"
    fun payment(orderId: String) = "payment/$orderId"
    fun orderSuccess(orderId: String) = "order_success/$orderId"
    fun order(id: String) = "order/$id"
    fun addressEdit(id: String? = null) = "address_edit?id=${id.orEmpty()}"

    val topLevel = setOf(HOME, EXPLORE, WISHLIST, CART, PROFILE)

    /** Collections used by the catalog screen. */
    const val COLLECTION_SALE = "sale"
    const val COLLECTION_NEW = "new"
    const val COLLECTION_FEATURED = "featured"
    const val COLLECTION_BEST = "best"
    const val COLLECTION_RECENT = "recent"

    /**
     * Turns notification deep links (as written by the admin dashboard, e.g.
     * "bagsapp://orders/abc") into an in-app route.
     */
    fun fromDeepLink(link: String?): String? {
        if (link.isNullOrBlank()) return null
        val path = link.substringAfter("://", link).trim('/')
        val parts = path.split('/')
        return when (parts.firstOrNull()) {
            "orders", "order" -> parts.getOrNull(1)?.let { order(it) } ?: ORDERS
            "product" -> parts.getOrNull(1)?.let { product(it) }
            "wishlist" -> WISHLIST
            "cart" -> CART
            "promo", "sale" -> catalog(collection = COLLECTION_SALE)
            "category" -> parts.getOrNull(1)?.let { catalog(category = Uri.decode(it)) }
            "notifications" -> NOTIFICATIONS
            "home" -> HOME
            else -> null
        }
    }
}
