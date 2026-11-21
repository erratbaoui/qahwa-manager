package com.example.qahwamanager.data.cloud

import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Staff

enum class SyncStatus { IDLE, SYNCING, SYNCED, OFFLINE, ERROR }

/**
 * Non-blocking outbound hook called by repositories after local Room mutations.
 * Default [NoOpSyncPublisher] keeps unit tests completely offline and fast.
 */
interface SyncPublisher {
    fun pushMenuItem(item: MenuItem)
    fun pushInventoryItem(item: InventoryItem)
    fun pushStaff(staff: Staff)
    fun pushShift(shiftId: Long)
    fun pushOrder(orderId: Long)
}

object NoOpSyncPublisher : SyncPublisher {
    override fun pushMenuItem(item: MenuItem) = Unit
    override fun pushInventoryItem(item: InventoryItem) = Unit
    override fun pushStaff(staff: Staff) = Unit
    override fun pushShift(shiftId: Long) = Unit
    override fun pushOrder(orderId: Long) = Unit
}
