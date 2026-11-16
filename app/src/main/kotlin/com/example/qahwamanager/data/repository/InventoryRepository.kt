package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.model.InventoryItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepository @Inject constructor(
    private val inventoryDao: InventoryDao
) {
    fun getAllItems(): Flow<List<InventoryItem>> = inventoryDao.getAllItems()

    fun getLowStockItems(): Flow<List<InventoryItem>> = inventoryDao.getLowStockItems()

    suspend fun addItem(item: InventoryItem): Long = inventoryDao.insert(item)

    suspend fun updateItem(item: InventoryItem) = inventoryDao.update(item)

    suspend fun adjustQuantity(id: Long, delta: Double) = inventoryDao.adjustQuantity(id, delta)

    suspend fun deleteItem(item: InventoryItem) = inventoryDao.delete(item)

    suspend fun getItemById(id: Long): InventoryItem? = inventoryDao.getById(id)
}

