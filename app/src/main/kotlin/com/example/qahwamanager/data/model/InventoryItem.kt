package com.example.qahwamanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val unit: String,       // "kg", "L", "pcs"
    val lowStockThreshold: Double = 1.0
)

