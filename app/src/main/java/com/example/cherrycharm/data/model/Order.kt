package com.example.cherrycharm.data.model

import com.google.firebase.firestore.DocumentSnapshot

enum class OrderStatus(val label: String) {
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled"),
    REFUNDED("Refunded");

    val isActive: Boolean get() = this in setOf(PENDING, CONFIRMED, PROCESSING, SHIPPED)
    val canCancel: Boolean get() = this == PENDING || this == CONFIRMED

    companion object {
        fun from(value: String?): OrderStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING

        /** The happy path shown on the tracking timeline. */
        val timeline = listOf(PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED)
    }
}

enum class PaymentMethod(val title: String, val subtitle: String, val isOnline: Boolean) {
    COD("Cash on Delivery", "Pay when your bag arrives", false),
    BKASH("bKash", "Pay with your bKash wallet", true),
    NAGAD("Nagad", "Pay with your Nagad wallet", true),
    CARD("Card / Net banking", "Visa, Mastercard, Amex via SSLCommerz", true);

    companion object {
        fun from(value: String?): PaymentMethod =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: if (value.equals("CASH_ON_DELIVERY", true)) COD else COD
    }
}

enum class PaymentStatus { PENDING, SUCCESS, FAILED, REFUNDED;
    companion object {
        fun from(value: String?): PaymentStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}

data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productImage: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val color: String = "",
) {
    val total: Double get() = price * quantity

    fun toMap(): Map<String, Any> = mapOf(
        "productId" to productId, "productName" to productName, "productImage" to productImage,
        "price" to price, "quantity" to quantity, "color" to color,
    )

    companion object {
        fun from(m: Map<String, Any?>) = OrderItem(
            productId = m["productId"].asString(),
            productName = m["productName"].asString(),
            productImage = m["productImage"].asString(),
            price = m["price"].asDouble(),
            quantity = m["quantity"].asInt(1),
            color = m["color"].asString(),
        )
    }
}

data class StatusEvent(val status: OrderStatus, val at: Long)

data class Order(
    val id: String = "",
    val userId: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: OrderStatus = OrderStatus.PENDING,
    val address: Address = Address(),
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val paymentId: String = "",
    val promoCode: String = "",
    val note: String = "",
    val statusHistory: List<StatusEvent> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    val itemCount: Int get() = items.sumOf { it.quantity }

    /** Time a given status was reached, if the history records it. */
    fun timeOf(s: OrderStatus): Long? = statusHistory.lastOrNull { it.status == s }?.at
        ?: if (s == OrderStatus.PENDING) createdAt else null

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "userId" to userId,
        "items" to items.map { it.toMap() },
        "subtotal" to subtotal,
        "discountAmount" to discountAmount,
        "deliveryFee" to deliveryFee,
        "totalAmount" to totalAmount,
        "status" to status.name,
        "address" to address.toMap(),
        "paymentMethod" to paymentMethod.name,
        "paymentStatus" to paymentStatus.name,
        "paymentId" to paymentId,
        "promoCode" to promoCode,
        "note" to note,
        "statusHistory" to statusHistory.map { mapOf("status" to it.status.name, "at" to it.at) },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )

    companion object {
        fun from(doc: DocumentSnapshot): Order? {
            val d = doc.data ?: return null
            val items = d["items"].asMapList().map(OrderItem::from)
            val subtotal = d["subtotal"].asDoubleOrNull() ?: items.sumOf { it.total }
            return Order(
                id = doc.id,
                userId = d["userId"].asString(),
                items = items,
                subtotal = subtotal,
                discountAmount = d["discountAmount"].asDouble(),
                deliveryFee = d["deliveryFee"].asDouble(),
                totalAmount = d["totalAmount"].asDouble(),
                status = OrderStatus.from(d["status"] as? String),
                address = Address.from(d["address"].asMap()),
                paymentMethod = PaymentMethod.from(d["paymentMethod"] as? String),
                paymentStatus = PaymentStatus.from(d["paymentStatus"] as? String),
                paymentId = d["paymentId"].asString(),
                promoCode = d["promoCode"].asString(),
                note = d["note"].asString(),
                statusHistory = d["statusHistory"].asMapList().map {
                    StatusEvent(OrderStatus.from(it["status"] as? String), it["at"].asLong())
                },
                createdAt = d["createdAt"].asLong(),
                updatedAt = d["updatedAt"].asLong(),
            )
        }
    }
}
