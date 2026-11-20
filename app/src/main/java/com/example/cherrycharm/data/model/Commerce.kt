package com.example.cherrycharm.data.model

import com.google.firebase.firestore.DocumentSnapshot

// ── Cart ──────────────────────────────────────────────────────────────────────

data class CartItem(
    val id: String = "",
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val color: String = "",
    val addedAt: Long = 0L,
) {
    val lineTotal: Double get() = price * quantity

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "productId" to productId, "productName" to productName,
        "productImage" to productImage, "price" to price, "quantity" to quantity,
        "color" to color, "addedAt" to addedAt,
    )

    companion object {
        fun from(doc: DocumentSnapshot): CartItem? {
            val d = doc.data ?: return null
            return CartItem(
                id = doc.id,
                productId = d["productId"].asString(),
                productName = d["productName"].asString(),
                productImage = d["productImage"].asString(),
                price = d["price"].asDouble(),
                quantity = d["quantity"].asInt(1),
                color = d["color"].asString(),
                addedAt = d["addedAt"].asLong(),
            )
        }

        /** Stable doc id so the same product+color merges into one line. */
        fun keyFor(productId: String, color: String) =
            if (color.isBlank()) productId else "${productId}_${color.lowercase().replace(" ", "-")}"
    }
}

/** Cart line enriched with live product data (current price/stock). */
data class CartLine(val item: CartItem, val product: Product?) {
    val unitPrice: Double get() = product?.finalPrice ?: item.price
    val total: Double get() = unitPrice * item.quantity
    val available: Boolean get() = product == null || (product.isActive && product.stock > 0)
    val exceedsStock: Boolean get() = product != null && item.quantity > product.stock
    val priceChanged: Boolean get() = product != null && product.finalPrice != item.price
}

// ── Address ───────────────────────────────────────────────────────────────────

data class Address(
    val id: String = "",
    val label: String = "Home",
    val fullName: String = "",
    val phone: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    /** District / division. Field name kept as "state" for the admin dashboard. */
    val state: String = "",
    val pincode: String = "",
    val isDefault: Boolean = false,
) {
    val isInsideDhaka: Boolean get() = city.trim().equals("Dhaka", ignoreCase = true)

    val singleLine: String
        get() = listOf(line1, line2, city, state, pincode).filter { it.isNotBlank() }.joinToString(", ")

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "label" to label, "fullName" to fullName, "phone" to phone,
        "line1" to line1, "line2" to line2, "city" to city, "state" to state,
        "pincode" to pincode, "isDefault" to isDefault,
    )

    companion object {
        fun from(map: Map<String, Any?>, id: String = map["id"].asString()) = Address(
            id = id,
            label = map["label"].asString("Home"),
            fullName = map["fullName"].asString(),
            phone = map["phone"].asString(),
            line1 = map["line1"].asString(),
            line2 = map["line2"].asString(),
            city = map["city"].asString(),
            state = map["state"].asString(),
            pincode = map["pincode"].asString(),
            isDefault = map["isDefault"].asBool(),
        )

        fun from(doc: DocumentSnapshot): Address? = doc.data?.let { from(it, doc.id) }
    }
}

// ── Promo ─────────────────────────────────────────────────────────────────────

data class PromoCode(
    val code: String = "",
    val discountType: String = "percentage",
    val discountValue: Double = 0.0,
    val minOrderAmount: Double = 0.0,
    val maxDiscount: Double = 0.0,
    val usageLimit: Int = 0,
    val usedCount: Int = 0,
    val expiryDate: Long = 0L,
    val isActive: Boolean = true,
) {
    val isPercentage: Boolean get() = discountType == "percentage"

    fun discountFor(subtotal: Double): Double {
        val raw = if (isPercentage) subtotal * discountValue / 100.0 else discountValue
        val capped = if (isPercentage && maxDiscount > 0) minOf(raw, maxDiscount) else raw
        return capped.coerceIn(0.0, subtotal)
    }

    val label: String
        get() = if (isPercentage) "${discountValue.toInt()}% off" else "৳${discountValue.toInt()} off"

    companion object {
        fun from(doc: DocumentSnapshot): PromoCode? {
            val d = doc.data ?: return null
            return PromoCode(
                code = d["code"].asString(doc.id),
                discountType = d["discountType"].asString("percentage"),
                discountValue = d["discountValue"].asDouble(),
                minOrderAmount = d["minOrderAmount"].asDouble(),
                maxDiscount = d["maxDiscount"].asDouble(),
                usageLimit = d["usageLimit"].asInt(),
                usedCount = d["usedCount"].asInt(),
                expiryDate = d["expiryDate"].asLong(),
                isActive = d["isActive"].asBool(true),
            )
        }
    }
}

// ── Review ────────────────────────────────────────────────────────────────────

data class Review(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userImage: String = "",
    val rating: Double = 0.0,
    val comment: String = "",
    val verifiedPurchase: Boolean = false,
    val createdAt: Long = 0L,
) {
    fun toMap(): Map<String, Any> = mapOf(
        "id" to id, "userId" to userId, "userName" to userName, "userImage" to userImage,
        "rating" to rating, "comment" to comment, "verifiedPurchase" to verifiedPurchase,
        "createdAt" to createdAt,
    )

    companion object {
        fun from(doc: DocumentSnapshot): Review? {
            val d = doc.data ?: return null
            return Review(
                id = doc.id,
                userId = d["userId"].asString(),
                userName = d["userName"].asString("Customer"),
                userImage = d["userImage"].asString(),
                rating = d["rating"].asDouble(),
                comment = d["comment"].asString(),
                verifiedPurchase = d["verifiedPurchase"].asBool(),
                createdAt = d["createdAt"].asLong(),
            )
        }
    }
}

// ── User ──────────────────────────────────────────────────────────────────────

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImage: String = "",
    val wishlist: List<String> = emptyList(),
    val createdAt: Long = 0L,
) {
    val firstName: String get() = name.trim().substringBefore(' ').ifBlank { "there" }

    companion object {
        fun from(doc: DocumentSnapshot): UserProfile? {
            val d = doc.data ?: return null
            return UserProfile(
                id = doc.id,
                name = d["name"].asString(),
                email = d["email"].asString(),
                phone = d["phone"].asString(),
                profileImage = d["profileImage"].asString(),
                wishlist = d["wishlist"].asStringList(),
                createdAt = d["createdAt"].asLong(),
            )
        }
    }
}

// ── In-app notification ───────────────────────────────────────────────────────

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "general",
    val isRead: Boolean = false,
    val deepLink: String = "",
    val createdAt: Long = 0L,
) {
    companion object {
        fun from(doc: DocumentSnapshot): AppNotification? {
            val d = doc.data ?: return null
            return AppNotification(
                id = doc.id,
                title = d["title"].asString(),
                body = d["body"].asString(),
                type = d["type"].asString("general"),
                isRead = d["isRead"].asBool(),
                deepLink = d["deepLink"].asString(),
                createdAt = d["createdAt"].asLong(),
            )
        }
    }
}
