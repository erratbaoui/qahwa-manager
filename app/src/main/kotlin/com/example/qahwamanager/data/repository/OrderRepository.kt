package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.cloud.NoOpSyncPublisher
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.OrderItemDao
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val orderItemDao: OrderItemDao,
    private val clock: Clock,
    private val syncPublisher: SyncPublisher = NoOpSyncPublisher,
) {
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()

    fun getOpenOrders(): Flow<List<Order>> = orderDao.getOrdersByStatus(OrderStatus.OPEN)

    fun observeOrder(orderId: Long): Flow<Order?> = orderDao.observeById(orderId)

    fun getOrderItems(orderId: Long): Flow<List<OrderItem>> =
        orderItemDao.getItemsForOrder(orderId)

    /** Creates the order in [shiftId] with its items, atomically; returns the new order id. */
    suspend fun createOrder(tableNumber: String, items: List<OrderItem>, shiftId: Long): Long {
        val total = items.sumOf { it.unitPriceCentimes * it.quantity }
        val now = clock.nowMillis()
        val order = Order(
            tableNumber = tableNumber,
            totalCentimes = total,
            createdAt = now,
            openedShiftId = shiftId,
            updatedAt = now,
        )
        val id = orderDao.insertOrderWithItems(order, items)
        syncPublisher.pushOrder(id)
        return id
    }

    /** Marks an open order paid in [shiftId]. Returns false if it was already closed. */
    suspend fun markPaid(orderId: Long, shiftId: Long): Boolean {
        val updated = orderDao.markPaidIfOpen(orderId, shiftId, clock.nowMillis()) == 1
        if (updated) syncPublisher.pushOrder(orderId)
        return updated
    }

    /** Cancels an open order, noting the shift that was active. Returns false if it was already closed. */
    suspend fun cancel(orderId: Long, activeShiftId: Long?): Boolean {
        val updated = orderDao.cancelIfOpen(orderId, activeShiftId, clock.nowMillis()) == 1
        if (updated) syncPublisher.pushOrder(orderId)
        return updated
    }
}
