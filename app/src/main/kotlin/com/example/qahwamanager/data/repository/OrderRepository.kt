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

    fun getOrdersForDay(startOfDay: Long, endOfDay: Long): Flow<List<Order>> =
        orderDao.getOrdersForDay(startOfDay, endOfDay)

    fun getOrderItems(orderId: Long): Flow<List<OrderItem>> =
        orderItemDao.getItemsForOrder(orderId)

    suspend fun createOrder(tableNumber: String, items: List<OrderItem>): Long {
        val total = items.sumOf { it.unitPrice * it.quantity }
        val order = Order(tableNumber = tableNumber, totalAmount = total)
        val orderId = orderDao.insert(order)
        val orderItems = items.map { it.copy(orderId = orderId) }
        orderItemDao.insertAll(orderItems)
        return orderId
    }

    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        val order = orderDao.getById(orderId) ?: return
        orderDao.update(order.copy(status = status))
    }

    suspend fun getOrderById(id: Long): Order? = orderDao.getById(id)
}

