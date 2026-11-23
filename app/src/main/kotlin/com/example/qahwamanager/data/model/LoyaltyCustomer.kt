package com.example.qahwamanager.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

data class LoyaltyConfig(
    val stampsRequired: Int = 8,
    val rewardTitle: String = "1 Free Coffee",
    val googleReviewUrl: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val updatedAt: Long = 0L,
)

/**
 * A café loyalty program member (Tappy-style digital stamp card).
 */
@Entity(
    tableName = "loyalty_customers",
    indices = [
        Index("phone"),
        Index("lastVisitAt"),
        Index("remoteId", unique = true),
    ]
)
data class LoyaltyCustomer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val currentStamps: Int = 0,
    val lifetimeStamps: Int = 0,
    val rewardsRedeemed: Int = 0,
    val lastVisitAt: Long,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "") val remoteId: String = UUID.randomUUID().toString(),
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false,
)
