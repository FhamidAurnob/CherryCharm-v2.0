package com.example.cherrycharm.core

object Constants {
    // ── Firestore collections ─────────────────────────────────────────────
    const val PRODUCTS = "products"
    const val USERS = "users"
    const val ORDERS = "orders"
    const val CARTS = "carts"
    const val ITEMS = "items"
    const val ADDRESSES = "addresses"
    const val REVIEWS = "reviews"
    const val NOTIFICATIONS = "notifications"
    const val PROMO_CODES = "promoCodes"
    const val CATEGORIES = "categories"
    const val BANNERS = "banners"

    // ── Delivery (BDT) ────────────────────────────────────────────────────
    const val DELIVERY_INSIDE_DHAKA = 60.0
    const val DELIVERY_OUTSIDE_DHAKA = 120.0
    const val FREE_DELIVERY_THRESHOLD = 2000.0
    const val MAX_QTY_PER_ITEM = 10
    const val LOW_STOCK_THRESHOLD = 5

    // ── Payment gateway return URLs (must match functions/index.js) ───────
    const val PAYMENT_RETURN_HOST = "your-app.web.app"

    // ── Support ───────────────────────────────────────────────────────────
    const val SUPPORT_PHONE = "+8801521204516"
    const val SUPPORT_EMAIL = "support@cherrycharm.com"

    val DEFAULT_CATEGORIES = listOf("Tote Bags", "Clutches", "Crossbody", "Backpacks", "Wallets")

    const val DEEP_LINK_EXTRA = "deep_link"
}
