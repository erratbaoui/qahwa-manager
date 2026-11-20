package com.example.qahwamanager.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The owner's PIN and recovery code as salted PBKDF2 hashes (never the secrets themselves), plus
 * the lockout state, which has to survive the app being killed. A single row with id 1.
 */
@Entity(tableName = "owner_credentials")
data class OwnerCredentials(
    @PrimaryKey val id: Int = 1,
    val pinHash: String,
    val pinSalt: String,
    val recoveryHash: String,
    val recoverySalt: String,
    val failedAttemptsInRow: Int = 0,
    val lockedUntil: Long = 0
)
