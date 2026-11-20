package com.example.cherrycharm.data.repository

import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.friendlyMessage
import com.example.cherrycharm.core.resourceOf
import com.example.cherrycharm.data.model.Address
import com.example.cherrycharm.data.model.CartLine
import com.example.cherrycharm.data.model.Order
import com.example.cherrycharm.data.model.OrderItem
import com.example.cherrycharm.data.model.OrderStatus
import com.example.cherrycharm.data.model.PaymentMethod
import com.example.cherrycharm.data.model.PaymentStatus
import com.example.cherrycharm.data.model.PromoCode
import com.example.cherrycharm.data.model.StatusEvent
import com.example.cherrycharm.di.ApplicationScope
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/** Everything a checkout needs to turn a bag into an order. */
data class OrderDraft(
    val lines: List<CartLine>,
    val address: Address,
    val paymentMethod: PaymentMethod,
    val subtotal: Double,
    val deliveryFee: Double,
    val discount: Double,
    val total: Double,
    val promo: PromoCode?,
    val note: String,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class OrderRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val products: ProductRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val ordersRef = firestore.collection(Constants.ORDERS)

    /** The user's orders, newest first (sorted on-device, so no composite index is needed). */
    val orders: StateFlow<Resource<List<Order>>> = auth.userId.flatMapLatest { uid ->
        if (uid == null) flowOf<Resource<List<Order>>>(Resource.Success(emptyList()))
        else ordersRef.whereEqualTo("userId", uid).snapshotFlow()
            .map<QuerySnapshot, Resource<List<Order>>> { snap ->
                Resource.Success(snap.documents.mapNotNull(Order::from).sortedByDescending { it.createdAt })
            }
            .onStart { emit(Resource.Loading) }
            .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load orders"))) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(10_000), Resource.Loading)

    fun order(id: String): Flow<Resource<Order>> = ordersRef.document(id).snapshotFlow()
        .map<DocumentSnapshot, Resource<Order>> { doc -> Order.from(doc)?.let { Resource.Success(it) } ?: Resource.Error("Order not found") }
        .onStart { emit(Resource.Loading) }
        .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load order"))) }

    suspend fun validatePromo(code: String, subtotal: Double): Resource<PromoCode> {
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) return Resource.Error("Enter a promo code")
        return try {
            val doc = firestore.collection(Constants.PROMO_CODES).document(clean).get().await()
            val promo = PromoCode.from(doc) ?: return Resource.Error("That code isn't valid")
            when {
                !promo.isActive -> Resource.Error("This code is no longer active")
                promo.expiryDate in 1 until System.currentTimeMillis() -> Resource.Error("This code has expired")
                promo.usageLimit > 0 && promo.usedCount >= promo.usageLimit -> Resource.Error("This code has reached its limit")
                subtotal < promo.minOrderAmount ->
                    Resource.Error("Add ৳${(promo.minOrderAmount - subtotal).toInt()} more to use this code")
                else -> Resource.Success(promo)
            }
        } catch (e: Exception) {
            Resource.Error(e.friendlyMessage("Couldn't check that code"))
        }
    }

    /**
     * Creates the order and empties the bag in one atomic batch.
     * COD orders are confirmed immediately; online payments stay PENDING until the gateway returns.
     */
    suspend fun placeOrder(draft: OrderDraft): Resource<Order> = resourceOf("Couldn't place your order") {
        val uid = auth.requireUid()
        require(draft.lines.isNotEmpty()) { "Your bag is empty" }
        draft.lines.firstOrNull { !it.available }?.let { error("${it.item.productName} is out of stock") }
        draft.lines.firstOrNull { it.exceedsStock }?.let {
            error("Only ${it.product?.stock} left of ${it.item.productName}")
        }

        val now = System.currentTimeMillis()
        val status = if (draft.paymentMethod.isOnline) OrderStatus.PENDING else OrderStatus.CONFIRMED
        val doc = ordersRef.document()
        val order = Order(
            id = doc.id,
            userId = uid,
            items = draft.lines.map {
                OrderItem(
                    productId = it.item.productId,
                    productName = it.product?.name ?: it.item.productName,
                    productImage = it.product?.mainImage ?: it.item.productImage,
                    price = it.unitPrice,
                    quantity = it.item.quantity,
                    color = it.item.color,
                )
            },
            subtotal = draft.subtotal,
            discountAmount = draft.discount,
            deliveryFee = draft.deliveryFee,
            totalAmount = draft.total,
            status = status,
            address = draft.address,
            paymentMethod = draft.paymentMethod,
            paymentStatus = PaymentStatus.PENDING,
            promoCode = draft.promo?.code.orEmpty(),
            note = draft.note.trim(),
            statusHistory = buildList {
                add(StatusEvent(OrderStatus.PENDING, now))
                if (status == OrderStatus.CONFIRMED) add(StatusEvent(OrderStatus.CONFIRMED, now))
            },
            createdAt = now,
            updatedAt = now,
        )

        val batch = firestore.batch()
        batch.set(doc, order.toMap())
        if (!draft.paymentMethod.isOnline) {
            val cart = firestore.collection(Constants.CARTS).document(uid).collection(Constants.ITEMS)
            draft.lines.forEach { batch.delete(cart.document(it.item.id)) }
        }
        batch.commit().await()

        if (!draft.paymentMethod.isOnline) afterOrderConfirmed(order)
        order
    }

    /** Called when an online payment succeeds (also safe to call for COD). */
    suspend fun markPaid(orderId: String, paymentId: String): Resource<Unit> = resourceOf("Couldn't confirm payment") {
        val uid = auth.requireUid()
        val now = System.currentTimeMillis()
        ordersRef.document(orderId).update(
            mapOf(
                "status" to OrderStatus.CONFIRMED.name,
                "paymentStatus" to PaymentStatus.SUCCESS.name,
                "paymentId" to paymentId,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(mapOf("status" to OrderStatus.CONFIRMED.name, "at" to now)),
            )
        ).await()
        // Payment cleared — now it's safe to empty the bag.
        runCatching {
            val cart = firestore.collection(Constants.CARTS).document(uid).collection(Constants.ITEMS)
            val docs = cart.get().await().documents
            if (docs.isNotEmpty()) {
                val batch = firestore.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        Order.from(ordersRef.document(orderId).get().await())?.let { afterOrderConfirmed(it) }
        Unit
    }

    /** Customer gave up on online payment and chose cash on delivery instead. */
    suspend fun switchToCashOnDelivery(orderId: String): Resource<Unit> = resourceOf("Couldn't update payment method") {
        val uid = auth.requireUid()
        val now = System.currentTimeMillis()
        ordersRef.document(orderId).update(
            mapOf(
                "paymentMethod" to PaymentMethod.COD.name,
                "paymentStatus" to PaymentStatus.PENDING.name,
                "status" to OrderStatus.CONFIRMED.name,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(mapOf("status" to OrderStatus.CONFIRMED.name, "at" to now)),
            )
        ).await()
        runCatching {
            val cart = firestore.collection(Constants.CARTS).document(uid).collection(Constants.ITEMS)
            val docs = cart.get().await().documents
            if (docs.isNotEmpty()) {
                val batch = firestore.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
        Order.from(ordersRef.document(orderId).get().await())?.let { afterOrderConfirmed(it) }
        Unit
    }

    suspend fun markPaymentFailed(orderId: String): Resource<Unit> = resourceOf("Couldn't update order") {
        ordersRef.document(orderId).update(
            mapOf("paymentStatus" to PaymentStatus.FAILED.name, "updatedAt" to System.currentTimeMillis())
        ).await()
        Unit
    }

    suspend fun cancel(orderId: String): Resource<Unit> = resourceOf("Couldn't cancel order") {
        val now = System.currentTimeMillis()
        ordersRef.document(orderId).update(
            mapOf(
                "status" to OrderStatus.CANCELLED.name,
                "updatedAt" to now,
                "statusHistory" to FieldValue.arrayUnion(mapOf("status" to OrderStatus.CANCELLED.name, "at" to now)),
            )
        ).await()
        Unit
    }

    /** Best-effort bookkeeping that may be restricted by security rules; never blocks the customer. */
    private fun afterOrderConfirmed(order: Order) {
        scope.launch {
            products.recordSale(order.items.map { it.productId to it.quantity })
            if (order.promoCode.isNotBlank()) {
                runCatching {
                    firestore.collection(Constants.PROMO_CODES).document(order.promoCode)
                        .update("usedCount", FieldValue.increment(1)).await()
                }
            }
        }
    }
}
