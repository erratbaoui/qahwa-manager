package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Query
import com.example.qahwamanager.data.model.OrderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {
    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getItemsForOrder(orderId: Long): Flow<List<OrderItem>>
}
