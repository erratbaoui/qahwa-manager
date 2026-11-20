package com.example.qahwamanager.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.repository.MenuRepository
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CartItem(val menuItem: MenuItem, val quantity: Int)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val menuRepository: MenuRepository,
    private val shiftRepository: ShiftRepository,
    private val ownerSession: OwnerSession
) : ViewModel() {

    /** The running shift; null means orders can't be placed or paid until someone starts one. */
    val activeShift: StateFlow<ShiftWithStaff?> = shiftRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val openOrders: StateFlow<List<Order>> = orderRepository.getOpenOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableMenuItems: StateFlow<List<MenuItem>> = menuRepository.getAvailableItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    /** True while an order write is in flight; the UI disables its buttons meanwhile. */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val cartTotalCentimes: Long get() = _cart.value.sumOf { it.menuItem.priceCentimes * it.quantity }

    fun addToCart(item: MenuItem) {
        _cart.update { current ->
            val existing = current.find { it.menuItem.id == item.id }
            if (existing != null) {
                current.map { if (it.menuItem.id == item.id) it.copy(quantity = it.quantity + 1) else it }
            } else {
                current + CartItem(item, 1)
            }
        }
    }

    fun removeFromCart(item: MenuItem) {
        _cart.update { current ->
            val existing = current.find { it.menuItem.id == item.id }
            if (existing != null && existing.quantity > 1) {
                current.map { if (it.menuItem.id == item.id) it.copy(quantity = it.quantity - 1) else it }
            } else {
                current.filter { it.menuItem.id != item.id }
            }
        }
    }

    fun clearCart() { _cart.value = emptyList() }

    fun placeOrder(tableNumber: String, onSuccess: (Long) -> Unit) {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return
        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                orderId = 0,
                menuItemId = cartItem.menuItem.id,
                menuItemName = cartItem.menuItem.name,
                menuItemNameAr = cartItem.menuItem.nameAr,
                menuItemNameFr = cartItem.menuItem.nameFr,
                quantity = cartItem.quantity,
                unitPriceCentimes = cartItem.menuItem.priceCentimes
            )
        }
        save {
            val shiftId = shiftRepository.activeShiftId() ?: return@save
            val orderId = orderRepository.createOrder(tableNumber, orderItems, shiftId)
            clearCart()
            onSuccess(orderId)
        }
    }

    /** Marks the order paid in the active shift, then calls [onDone]. Does nothing without a shift. */
    fun markAsPaid(orderId: Long, onDone: () -> Unit = {}) = save {
        val shiftId = shiftRepository.activeShiftId() ?: return@save
        orderRepository.markPaid(orderId, shiftId)
        onDone()
    }

    /** Cancels the order, then calls [onDone]. Owner only: the UI asks for the PIN first. */
    fun cancelOrder(orderId: Long, onDone: () -> Unit = {}) {
        if (!ownerSession.isOwnerMode.value) return
        save {
            orderRepository.cancel(orderId, shiftRepository.activeShiftId())
            onDone()
        }
    }

    fun observeOrder(orderId: Long): Flow<Order?> = orderRepository.observeOrder(orderId)

    fun observeOrderItems(orderId: Long): Flow<List<OrderItem>> = orderRepository.getOrderItems(orderId)

    /** Runs one write at a time; taps that arrive while a write is in flight are ignored. */
    private fun save(block: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
