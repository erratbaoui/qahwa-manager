package com.example.qahwamanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "inventory_items",
    indices = [Index("remoteId", unique = true)]
)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,       // "kg", "L", "pcs"
    val lowStockThreshold: Double = 1.0,
    @ColumnInfo(defaultValue = "") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false
)
