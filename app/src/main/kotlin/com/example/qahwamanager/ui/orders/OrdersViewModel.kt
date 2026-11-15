package com.example.qahwamanager.ui.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.repository.MenuRepository
import com.example.qahwamanager.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val menuRepository: MenuRepository
) : ViewModel() {

    val openOrders: StateFlow<List<Order>> = orderRepository.getOpenOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableMenuItems: StateFlow<List<MenuItem>> = menuRepository.getAvailableItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val cartTotal: Double get() = _cart.value.sumOf { it.menuItem.price * it.quantity }

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

    fun placeOrder(tableNumber: String, onSuccess: (Long) -> Unit) = viewModelScope.launch {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) return@launch
        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                orderId = 0,
                menuItemId = cartItem.menuItem.id,
                menuItemName = cartItem.menuItem.name,
                quantity = cartItem.quantity,
                unitPrice = cartItem.menuItem.price
            )
        }
        val orderId = orderRepository.createOrder(tableNumber, orderItems)
        clearCart()
        onSuccess(orderId)
    }

    fun markAsPaid(orderId: Long) = viewModelScope.launch {
        orderRepository.updateOrderStatus(orderId, OrderStatus.PAID)
    }

    fun cancelOrder(orderId: Long) = viewModelScope.launch {
        orderRepository.updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }

    fun getOrderItems(orderId: Long): StateFlow<List<OrderItem>> =
        orderRepository.getOrderItems(orderId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getOrderById(id: Long): Order? = orderRepository.getOrderById(id)
}

