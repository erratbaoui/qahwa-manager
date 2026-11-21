package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.cloud.NoOpSyncPublisher
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.data.time.SystemClock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val clock: Clock = SystemClock,
    private val syncPublisher: SyncPublisher = NoOpSyncPublisher,
) {
    fun getAllItems(): Flow<List<InventoryItem>> = inventoryDao.getAllItems()

    fun getLowStockItems(): Flow<List<InventoryItem>> = inventoryDao.getLowStockItems()

    suspend fun addItem(item: InventoryItem): Long {
        val stamped = item.copy(updatedAt = clock.nowMillis(), isDeleted = false)
        val id = inventoryDao.insert(stamped)
        syncPublisher.pushInventoryItem(stamped.copy(id = id))
        return id
    }

    suspend fun updateItem(item: InventoryItem) {
        val stamped = item.copy(updatedAt = clock.nowMillis())
        inventoryDao.update(stamped)
        syncPublisher.pushInventoryItem(stamped)
    }

    suspend fun adjustQuantity(id: Long, delta: Double) {
        inventoryDao.adjustQuantity(id, delta, clock.nowMillis())
        inventoryDao.getById(id)?.let(syncPublisher::pushInventoryItem)
    }

    suspend fun deleteItem(item: InventoryItem) {
        val tombstoned = item.copy(isDeleted = true, updatedAt = clock.nowMillis())
        inventoryDao.update(tombstoned)
        syncPublisher.pushInventoryItem(tombstoned)
    }
}
