package com.example.cherrycharm.data.repository

import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.resourceOf
import com.example.cherrycharm.data.model.Order
import com.example.cherrycharm.data.model.PaymentMethod
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** A gateway page to open in the in-app browser, plus the id the gateway gave us. */
data class GatewaySession(val url: String, val gatewayPaymentId: String = "")

/**
 * Talks to the Cloud Functions in functions/index.js. Secrets (store passwords, app keys)
 * never live in the app — the functions hold them and hand back a hosted payment page.
 */
@Singleton
class PaymentRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) {
    suspend fun startSession(order: Order): Resource<GatewaySession> = resourceOf("Couldn't reach the payment gateway") {
        when (order.paymentMethod) {
            PaymentMethod.BKASH -> {
                val data = call("initiateBkashPayment", mapOf("amount" to String.format(Locale.US, "%.2f", order.totalAmount), "orderId" to order.id))
                val url = data["bkashURL"] as? String ?: error(data["statusMessage"] as? String ?: "bKash didn't return a payment page")
                GatewaySession(url, data["paymentID"] as? String ?: "")
            }
            PaymentMethod.NAGAD -> {
                val data = call("initiateNagadPayment", mapOf("amount" to String.format(Locale.US, "%.2f", order.totalAmount), "orderId" to order.id))
                val url = (data["callBackUrl"] ?: data["redirectUrl"] ?: data["callbackUrl"]) as? String
                    ?: error("Nagad didn't return a payment page")
                GatewaySession(url)
            }
            PaymentMethod.CARD -> {
                val a = order.address
                val data = call(
                    "initiateSSLCommerzPayment",
                    mapOf(
                        "amount" to order.totalAmount,
                        "orderId" to order.id,
                        "customerName" to a.fullName,
                        "customerEmail" to (auth.currentUser?.email ?: "customer@cherrycharm.com"),
                        "customerPhone" to a.phone,
                        "customerAddress" to a.singleLine,
                    ),
                )
                val url = data["GatewayPageURL"] as? String
                    ?: error(data["failedreason"] as? String ?: "Card gateway didn't return a payment page")
                GatewaySession(url, data["sessionkey"] as? String ?: "")
            }
            PaymentMethod.COD -> error("Cash on delivery doesn't need a gateway")
        }
    }

    /** bKash needs a server-side "execute" after the customer approves. */
    suspend fun executeBkash(paymentId: String, orderId: String): Resource<String> = resourceOf("bKash payment failed") {
        val data = call("executeBkashPayment", mapOf("paymentId" to paymentId, "orderId" to orderId))
        val status = data["transactionStatus"] as? String
        if (status != "Completed") error(data["statusMessage"] as? String ?: "bKash payment was not completed")
        data["trxID"] as? String ?: paymentId
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun call(name: String, payload: Map<String, Any>): Map<String, Any?> {
        val result = functions.getHttpsCallable(name).call(payload).await()
        return (result.getData() as? Map<String, Any?>) ?: emptyMap()
    }
}
