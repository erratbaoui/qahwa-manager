package com.example.qahwamanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A cash spending/expense recorded during a shift or for a specific date/slot
 * (e.g. buying milk, mint, sugar, ice, cleaning supplies).
 */
@Entity(
    tableName = "spendings",
    indices = [
        Index("shiftId"),
        Index("spentAt"),
        Index("remoteId", unique = true),
    ]
)
data class Spending(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shiftId: Long? = null,
    val title: String,
    val amountCentimes: Long,
    val spentAt: Long,
    @ColumnInfo(defaultValue = "") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false,
)
