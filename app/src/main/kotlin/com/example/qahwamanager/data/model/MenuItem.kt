package com.example.qahwamanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "menu_items",
    indices = [Index("remoteId", unique = true)]
)
data class MenuItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameAr: String,
    val nameFr: String,
    val category: String, // "coffee", "tea", "food", "juice"
    val priceCentimes: Long, // MAD × 100
    val description: String = "",
    val isAvailable: Boolean = true,
    @ColumnInfo(defaultValue = "") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false
)
