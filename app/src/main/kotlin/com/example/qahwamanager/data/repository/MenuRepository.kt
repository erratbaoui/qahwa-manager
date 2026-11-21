package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.cloud.NoOpSyncPublisher
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.data.time.SystemClock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val menuItemDao: MenuItemDao,
    private val clock: Clock = SystemClock,
    private val syncPublisher: SyncPublisher = NoOpSyncPublisher,
) {
    fun getAllItems(): Flow<List<MenuItem>> = menuItemDao.getAllItems()

    fun getItemsByCategory(category: String): Flow<List<MenuItem>> =
        menuItemDao.getItemsByCategory(category)

    fun getAvailableItems(): Flow<List<MenuItem>> = menuItemDao.getAvailableItems()

    suspend fun addItem(item: MenuItem): Long {
        val stamped = item.copy(updatedAt = clock.nowMillis(), isDeleted = false)
        val id = menuItemDao.insert(stamped)
        syncPublisher.pushMenuItem(stamped.copy(id = id))
        return id
    }

    suspend fun updateItem(item: MenuItem) {
        val stamped = item.copy(updatedAt = clock.nowMillis())
        menuItemDao.update(stamped)
        syncPublisher.pushMenuItem(stamped)
    }

    suspend fun deleteItem(item: MenuItem) {
        val tombstoned = item.copy(isDeleted = true, updatedAt = clock.nowMillis())
        menuItemDao.update(tombstoned)
        syncPublisher.pushMenuItem(tombstoned)
    }
}
