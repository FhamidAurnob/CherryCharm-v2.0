package com.example.cherrycharm.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.data.model.Address
import com.example.cherrycharm.data.model.CartLine
import com.example.cherrycharm.data.model.Order
import com.example.cherrycharm.data.model.PaymentMethod
import com.example.cherrycharm.data.model.PromoCode
import com.example.cherrycharm.data.repository.AddressRepository
import com.example.cherrycharm.data.repository.CartRepository
import com.example.cherrycharm.data.repository.OrderDraft
import com.example.cherrycharm.data.repository.OrderRepository
import com.example.cherrycharm.ui.components.MessageQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutState(
    val loading: Boolean = true,
    val lines: List<CartLine> = emptyList(),
    val addresses: List<Address> = emptyList(),
    val address: Address? = null,
    val payment: PaymentMethod = PaymentMethod.COD,
    val promo: PromoCode? = null,
    val promoWarning: String? = null,
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
) {
    val itemCount: Int get() = lines.sumOf { it.item.quantity }
    val hasProblems: Boolean get() = lines.any { !it.available || it.exceedsStock }
    val canPlace: Boolean get() = !loading && lines.isNotEmpty() && address != null && !hasProblems
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    cart: CartRepository,
    addresses: AddressRepository,
    private val orders: OrderRepository,
) : ViewModel() {
    val messages = MessageQueue()

    private val selectedAddressId = MutableStateFlow<String?>(null)
    private val payment = MutableStateFlow(PaymentMethod.COD)
    private val promo = MutableStateFlow<PromoCode?>(null)

    private val _promoInput = MutableStateFlow("")
    val promoInput = _promoInput.asStateFlow()
    private val _promoChecking = MutableStateFlow(false)
    val promoChecking = _promoChecking.asStateFlow()
    private val _promoError = MutableStateFlow<String?>(null)
    val promoError = _promoError.asStateFlow()
    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()
    private val _placing = MutableStateFlow(false)
    val placing = _placing.asStateFlow()

    val state: StateFlow<CheckoutState> = combine(
        cart.lines, addresses.addresses, selectedAddressId, payment, promo,
    ) { linesRes, addrRes, selId, pay, code ->
        val lines = (linesRes as? Resource.Success)?.data.orEmpty()
        val addrs = (addrRes as? Resource.Success)?.data.orEmpty()
        val address = addrs.firstOrNull { it.id == selId } ?: addrs.firstOrNull { it.isDefault } ?: addrs.firstOrNull()
        val subtotal = lines.filter { it.available }.sumOf { it.total }
        val delivery = when {
            subtotal >= Constants.FREE_DELIVERY_THRESHOLD -> 0.0
            address == null || address.isInsideDhaka -> Constants.DELIVERY_INSIDE_DHAKA
            else -> Constants.DELIVERY_OUTSIDE_DHAKA
        }
        val promoValid = code != null && subtotal >= code.minOrderAmount
        val discount = if (promoValid) code!!.discountFor(subtotal) else 0.0
        CheckoutState(
            loading = linesRes is Resource.Loading || addrRes is Resource.Loading,
            lines = lines,
            addresses = addrs,
            address = address,
            payment = pay,
            promo = code,
            promoWarning = if (code != null && !promoValid)
                "Add ${(code.minOrderAmount - subtotal).toInt()} more to use ${code.code}" else null,
            subtotal = subtotal,
            deliveryFee = delivery,
            discount = discount,
            total = (subtotal + delivery - discount).coerceAtLeast(0.0),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckoutState())

    fun selectAddress(id: String) { selectedAddressId.value = id }
    fun selectPayment(m: PaymentMethod) { payment.value = m }
    fun setNote(n: String) { if (n.length <= 200) _note.value = n }
    fun setPromoInput(s: String) { _promoInput.value = s.uppercase().take(20); _promoError.value = null }

    fun applyPromo() {
        val code = _promoInput.value.trim()
        if (code.isEmpty()) return
        viewModelScope.launch {
            _promoChecking.value = true
            when (val r = orders.validatePromo(code, state.value.subtotal)) {
                is Resource.Success -> {
                    promo.value = r.data
                    _promoError.value = null
                    messages.send("🎉 ${r.data.code} applied — ${r.data.label}")
                }
                is Resource.Error -> _promoError.value = r.message
                else -> Unit
            }
            _promoChecking.value = false
        }
    }

    fun removePromo() {
        promo.value = null
        _promoInput.value = ""
    }

    /** Places the order; [onPlaced] receives the order so the screen can route to payment or success. */
    fun placeOrder(onPlaced: (Order) -> Unit) {
        val s = state.value
        val address = s.address ?: run { messages.send("Add a delivery address first"); return }
        if (!s.canPlace || _placing.value) return
        viewModelScope.launch {
            _placing.value = true
            val result = orders.placeOrder(
                OrderDraft(
                    lines = s.lines.filter { it.available },
                    address = address,
                    paymentMethod = s.payment,
                    subtotal = s.subtotal,
                    deliveryFee = s.deliveryFee,
                    discount = s.discount,
                    total = s.total,
                    promo = s.promo?.takeIf { s.discount > 0 },
                    note = _note.value,
                )
            )
            _placing.value = false
            when (result) {
                is Resource.Success -> onPlaced(result.data)
                is Resource.Error -> messages.send(result.message)
                else -> Unit
            }
        }
    }
}
