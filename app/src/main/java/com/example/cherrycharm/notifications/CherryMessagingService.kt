package com.example.cherrycharm.notifications

import com.example.cherrycharm.core.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Receives FCM pushes sent by the admin dashboard / Cloud Functions. */
class CherryMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: "general"
        val title = message.notification?.title ?: data["title"] ?: titleFor(type, data)
        val body = message.notification?.body ?: data["body"] ?: ""
        val deepLink = data["deepLink"] ?: when (type) {
            "order" -> data["orderId"]?.let { "bagsapp://orders/$it" }
            "price_drop" -> data["productId"]?.let { "bagsapp://product/$it" }
            else -> null
        }
        NotificationHelper.show(this, title, body, type, deepLink)
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection(Constants.USERS).document(uid)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
    }

    private fun titleFor(type: String, data: Map<String, String>): String = when (type) {
        "order" -> when (data["status"]?.uppercase()) {
            "CONFIRMED" -> "Order confirmed ✅"
            "PROCESSING" -> "We're packing your order 📦"
            "SHIPPED" -> "Your order is on the way 🚚"
            "DELIVERED" -> "Delivered! Enjoy your new bag 🍒"
            "CANCELLED" -> "Order cancelled"
            else -> "Order update"
        }
        "price_drop" -> "Price drop on your wishlist 💰"
        "promo" -> "A treat for you 🎁"
        else -> "CherryCharm"
    }
}
