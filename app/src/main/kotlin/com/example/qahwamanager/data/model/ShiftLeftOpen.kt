package com.example.qahwamanager.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * An order that was still OPEN when its shift ended, with its total at that moment. Recorded once,
 * so a settled report never changes when the order is paid or cancelled later.
 */
@Entity(
    tableName = "shift_left_open",
    primaryKeys = ["shiftId", "orderId"],
    foreignKeys = [
        ForeignKey(entity = Shift::class, parentColumns = ["id"], childColumns = ["shiftId"]),
        ForeignKey(entity = Order::class, parentColumns = ["id"], childColumns = ["orderId"])
    ],
    indices = [Index("orderId")]
)
data class ShiftLeftOpen(
    val shiftId: Long,
    val orderId: Long,
    val totalCentimes: Long
)
