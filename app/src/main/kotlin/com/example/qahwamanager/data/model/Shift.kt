package com.example.qahwamanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class ShiftStatus { ACTIVE, ENDED, SETTLED }

/**
 * One waiter's shift: ACTIVE while they work, ENDED once they (or a takeover) close it, SETTLED
 * once the owner has counted the cash. [expectedCentimes] and [countedCentimes] are copied onto
 * the row at settlement so the history never moves afterwards.
 */
@Entity(
    tableName = "shifts",
    foreignKeys = [
        ForeignKey(entity = Staff::class, parentColumns = ["id"], childColumns = ["staffId"])
    ],
    indices = [Index("staffId"), Index("status"), Index("remoteId", unique = true)]
)
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val status: ShiftStatus = ShiftStatus.ACTIVE,
    val startedAt: Long,
    val endedAt: Long? = null,
    val endedByTakeover: Boolean = false,
    val settledAt: Long? = null,
    val expectedCentimes: Long? = null,
    val countedCentimes: Long? = null,
    val failedOwnerPinAttempts: Int = 0,
    @ColumnInfo(defaultValue = "") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L
)

/** Counted minus expected once both are known (negative = short, positive = over), otherwise null. */
val Shift.differenceCentimes: Long?
    get() {
        val expected = expectedCentimes ?: return null
        val counted = countedCentimes ?: return null
        return counted - expected
    }
