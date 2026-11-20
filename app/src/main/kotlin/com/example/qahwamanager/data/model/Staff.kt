package com.example.qahwamanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A person who can run a shift. The owner is a staff row too, with [isOwner] set. Nobody is ever deleted. */
@Entity(tableName = "staff")
data class Staff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isOwner: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long
)
