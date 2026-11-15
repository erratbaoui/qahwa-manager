package com.example.qahwamanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class OrderStatus { OPEN, PAID, CANCELLED }

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableNumber: String,
    val status: OrderStatus = OrderStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val totalAmount: Double = 0.0
)

