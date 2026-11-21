package com.example.qahwamanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class OrderStatus { OPEN, PAID, CANCELLED }

// No foreign keys on the shift columns: they were added to an existing table with ALTER TABLE.
@Entity(
    tableName = "orders",
    indices = [Index("openedShiftId"), Index("paidShiftId"), Index("remoteId", unique = true)]
)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableNumber: String,
    val status: OrderStatus = OrderStatus.OPEN,
    val createdAt: Long = System.currentTimeMillis(),
    val totalCentimes: Long = 0, // MAD × 100
    /** The shift this order was placed in; null for orders from before shifts existed. */
    val openedShiftId: Long? = null,
    /** The shift that collected the money. Expected cash for a shift sums orders paid in it. */
    val paidShiftId: Long? = null,
    val paidAt: Long? = null,
    val cancelledAt: Long? = null,
    val cancelledShiftId: Long? = null,
    @ColumnInfo(defaultValue = "") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
) {
    companion object {
        /** Stored in [tableNumber] for takeaway orders. Existing rows rely on this exact value. */
        const val TAKEAWAY = "takeaway"
    }
}

val Order.isTakeaway: Boolean get() = tableNumber == Order.TAKEAWAY
