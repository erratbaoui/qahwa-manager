package com.example.qahwamanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Roles a café staff member can hold. */
enum class StaffRole {
    OWNER,
    WAITER,
    BARMAN,
    SECURITY,
    CLEANING,
}

/**
 * A person who can run a shift. The primary owner has [isOwner] set and cannot be deactivated.
 * Nobody is ever deleted.
 */
@Entity(
    tableName = "staff",
    indices = [Index("remoteId", unique = true)]
)
data class Staff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(defaultValue = "WAITER") val role: StaffRole = StaffRole.WAITER,
    val isOwner: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)
