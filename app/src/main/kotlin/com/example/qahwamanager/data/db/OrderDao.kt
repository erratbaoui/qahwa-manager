package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>>

    @Query("""
        SELECT * FROM orders 
        WHERE createdAt >= :startOfDay AND createdAt < :endOfDay 
        ORDER BY createdAt DESC
    """)
    fun getOrdersForDay(startOfDay: Long, endOfDay: Long): Flow<List<Order>>

    @Query("""
        SELECT * FROM orders 
        WHERE createdAt >= :startMs AND status = 'PAID'
        ORDER BY createdAt DESC
    """)
    fun getPaidOrdersSince(startMs: Long): Flow<List<Order>>

    @Query("SELECT SUM(totalAmount) FROM orders WHERE status = 'PAID' AND createdAt >= :startMs")
    fun getTotalRevenueSince(startMs: Long): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: Order): Long

    @Update
    suspend fun update(order: Order)

    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: Long): Order?
}

