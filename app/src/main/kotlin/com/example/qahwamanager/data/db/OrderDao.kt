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

    /** Marks an OPEN order paid in [shiftId] at [now]; closed orders are left alone. Returns rows changed. */
    @Query("UPDATE orders SET status = 'PAID', paidShiftId = :shiftId, paidAt = :now, updatedAt = :now WHERE id = :id AND status = 'OPEN'")
    abstract suspend fun markPaidIfOpen(id: Long, shiftId: Long, now: Long): Int

    /** Cancels an OPEN order at [now], noting the shift that was active (if any). Returns rows changed. */
    @Query("UPDATE orders SET status = 'CANCELLED', cancelledAt = :now, cancelledShiftId = :shiftId, updatedAt = :now WHERE id = :id AND status = 'OPEN'")
    abstract suspend fun cancelIfOpen(id: Long, shiftId: Long?, now: Long): Int

    @Query("SELECT * FROM orders")
    abstract suspend fun getAllSnapshot(): List<Order>

    @Query("SELECT * FROM orders WHERE id = :id")
    abstract suspend fun getById(id: Long): Order?

    @Query("SELECT * FROM orders WHERE remoteId = :remoteId")
    abstract suspend fun getByRemoteId(remoteId: String): Order?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    abstract suspend fun getItemsSnapshot(orderId: Long): List<OrderItem>

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    abstract suspend fun deleteItemsForOrder(orderId: Long)

    @androidx.room.Update
    abstract suspend fun updateOrder(order: Order)

    @Transaction
    open suspend fun upsertRemoteOrderWithItems(order: Order, items: List<OrderItem>): Long {
        val existing = getByRemoteId(order.remoteId)
        val localId = if (existing == null) {
            insertOrder(order.copy(id = 0))
        } else {
            updateOrder(order.copy(id = existing.id))
            deleteItemsForOrder(existing.id)
            existing.id
        }
        if (items.isNotEmpty()) {
            insertOrderItems(items.map { it.copy(id = 0, orderId = localId) })
        }
        return localId
    }

    @Query("SELECT * FROM orders WHERE paidShiftId = :shiftId AND status = 'PAID' ORDER BY paidAt DESC, id DESC")
    abstract fun observePaidInShift(shiftId: Long): Flow<List<Order>>

    /**
     * Cancelled orders for a shift's report: the shift they were opened in, or, for orders from
     * before shifts existed, the shift that was active when they were cancelled.
     */
    @Query("SELECT * FROM orders WHERE status = 'CANCELLED' AND COALESCE(openedShiftId, cancelledShiftId) = :shiftId ORDER BY cancelledAt DESC, id DESC")
    abstract fun observeCancelledForShift(shiftId: Long): Flow<List<Order>>

    /** The orders a shift left open (their current state; the frozen totals live in shift_left_open). */
    @Query("SELECT orders.* FROM orders JOIN shift_left_open ON shift_left_open.orderId = orders.id WHERE shift_left_open.shiftId = :shiftId ORDER BY orders.id")
    abstract fun observeLeftOpenOrders(shiftId: Long): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE id = :id")
    abstract fun observeById(id: Long): Flow<Order?>
}
