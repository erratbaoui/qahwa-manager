package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
abstract class OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    abstract fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    abstract fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>>

    @Query("""
        SELECT * FROM orders
        WHERE createdAt >= :startMs AND status = 'PAID'
        ORDER BY createdAt DESC
    """)
    abstract fun getPaidOrdersSince(startMs: Long): Flow<List<Order>>

    @Query("SELECT COALESCE(SUM(totalCentimes), 0) FROM orders WHERE status = 'PAID' AND createdAt >= :startMs")
    abstract fun getRevenueSince(startMs: Long): Flow<Long>

    @Insert
    abstract suspend fun insertOrder(order: Order): Long

    @Insert
    abstract suspend fun insertOrderItems(items: List<OrderItem>)

    /** Inserts the order and its items atomically; returns the new order id. */
    @Transaction
    open suspend fun insertOrderWithItems(order: Order, items: List<OrderItem>): Long {
        val orderId = insertOrder(order)
        insertOrderItems(items.map { it.copy(orderId = orderId) })
        return orderId
    }

    /** Moves an OPEN order to [status]; closed orders are left alone. Returns rows changed. */
    @Query("UPDATE orders SET status = :status WHERE id = :id AND status = 'OPEN'")
    abstract suspend fun updateStatusIfOpen(id: Long, status: OrderStatus): Int

    @Query("SELECT * FROM orders WHERE id = :id")
    abstract fun observeById(id: Long): Flow<Order?>
}
