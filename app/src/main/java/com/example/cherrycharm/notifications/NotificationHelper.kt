package com.example.cherrycharm.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.cherrycharm.MainActivity
import com.example.cherrycharm.R
import com.example.cherrycharm.core.Constants

object NotificationHelper {
    const val CHANNEL_ORDERS = "bag_app_orders"
    const val CHANNEL_PROMOTIONS = "bag_app_promotions"
    const val CHANNEL_PRICE_DROPS = "bag_app_price_drops"
    const val CHANNEL_GENERAL = "bag_app_default"

    private const val BRAND_COLOR = 0xFFB5274E.toInt()

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            NotificationChannel(CHANNEL_ORDERS, "Order updates", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Confirmations, shipping and delivery" },
            NotificationChannel(CHANNEL_PROMOTIONS, "Offers & deals", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Sales, promo codes and new arrivals" },
            NotificationChannel(CHANNEL_PRICE_DROPS, "Price drops", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "When something in your wishlist gets cheaper" },
            NotificationChannel(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_LOW),
        ).forEach(manager::createNotificationChannel)
    }

    fun channelFor(type: String): String = when (type) {
        "order" -> CHANNEL_ORDERS
        "promo", "new_arrival" -> CHANNEL_PROMOTIONS
        "price_drop" -> CHANNEL_PRICE_DROPS
        else -> CHANNEL_GENERAL
    }

    fun show(context: Context, title: String, body: String, type: String = "general", deepLink: String? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (!deepLink.isNullOrBlank()) putExtra(Constants.DEEP_LINK_EXTRA, deepLink)
        }
        val id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val pending = PendingIntent.getActivity(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channelFor(type))
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(if (type == "order") NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
