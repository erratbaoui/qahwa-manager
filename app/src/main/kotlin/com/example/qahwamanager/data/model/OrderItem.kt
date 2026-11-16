package com.example.qahwamanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = Order::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val menuItemId: Long,
    // Names are snapshotted so past orders keep showing what was sold, in every language.
    val menuItemName: String,
    val menuItemNameAr: String = "",
    val menuItemNameFr: String = "",
    val quantity: Int,
    val unitPriceCentimes: Long // MAD × 100
)
