package com.example.qahwamanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menu_items")
data class MenuItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameAr: String,
    val nameFr: String,
    val category: String, // "coffee", "tea", "food", "juice"
    val price: Double,    // in MAD
    val description: String = "",
    val isAvailable: Boolean = true
)

