package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.OrderItemDao
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao
) {
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()

    fun getOpenOrders(): Flow<List<Order>> = orderDao.getOrdersByStatus(OrderStatus.OPEN)

    fun observeOrder(orderId: Long): Flow<Order?> = orderDao.observeById(orderId)

    fun getOrderItems(orderId: Long): Flow<List<OrderItem>> =
        orderItemDao.getItemsForOrder(orderId)

    /** Creates the order and its items atomically; returns the new order id. */
    suspend fun createOrder(tableNumber: String, items: List<OrderItem>): Long {
        val total = items.sumOf { it.unitPriceCentimes * it.quantity }
        val order = Order(tableNumber = tableNumber, totalCentimes = total)
        return orderDao.insertOrderWithItems(order, items)
    }

    /** Closes an open order as paid or cancelled. Orders that are already closed are left alone. */
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        orderDao.updateStatusIfOpen(orderId, status)
    }
}
