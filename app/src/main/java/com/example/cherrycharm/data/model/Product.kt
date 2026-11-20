package com.example.cherrycharm.data.model

import com.example.cherrycharm.core.Constants
import com.google.firebase.firestore.DocumentSnapshot

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val salePrice: Double? = null,
    val category: String = "",
    val subcategory: String = "",
    val images: List<String> = emptyList(),
    val stock: Int = 0,
    val sku: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val tags: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val isFeatured: Boolean = false,
    val isActive: Boolean = true,
    val soldCount: Int = 0,
    val createdAt: Long = 0L,
) {
    val hasDiscount: Boolean get() = salePrice != null && salePrice > 0 && salePrice < price
    val finalPrice: Double get() = if (hasDiscount) salePrice!! else price
    val discountPercent: Int get() = if (hasDiscount) (((price - finalPrice) / price) * 100).toInt() else 0
    val inStock: Boolean get() = stock > 0
    val isLowStock: Boolean get() = stock in 1..Constants.LOW_STOCK_THRESHOLD
    val mainImage: String? get() = images.firstOrNull()

    /** Loose, typo-tolerant-ish match used by search. */
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        val haystack = buildString {
            append(name).append(' ').append(category).append(' ').append(subcategory).append(' ')
            append(description).append(' ').append(tags.joinToString(" ")).append(' ')
            append(colors.joinToString(" "))
        }.lowercase()
        return q.split(" ").filter { it.isNotBlank() }.all { token ->
            haystack.contains(token) || haystack.contains(token.trimEnd('s'))
        }
    }

    companion object {
        fun from(doc: DocumentSnapshot): Product? {
            val d = doc.data ?: return null
            return Product(
                id = doc.id,
                name = d["name"].asString(),
                description = d["description"].asString(),
                price = d["price"].asDouble(),
                salePrice = d["salePrice"].asDoubleOrNull()?.takeIf { it > 0 },
                category = d["category"].asString(),
                subcategory = d["subcategory"].asString(),
                images = d["images"].asStringList(),
                stock = d["stock"].asInt(),
                sku = d["sku"].asString(),
                rating = d["rating"].asDouble(),
                reviewCount = d["reviewCount"].asInt(),
                tags = d["tags"].asStringList(),
                colors = d["colors"].asStringList(),
                isFeatured = d["isFeatured"].asBool(),
                isActive = d["isActive"].asBool(default = true),
                soldCount = d["soldCount"].asInt(),
                createdAt = d["createdAt"].asLong(),
            )
        }
    }
}

data class Category(
    val id: String = "",
    val name: String = "",
    val image: String = "",
)

data class Banner(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val image: String = "",
    /** A category name, "sale", "new", or a product id prefixed with "product:". */
    val target: String = "",
)
