package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.model.MenuItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val menuItemDao: MenuItemDao
) {
    fun getAllItems(): Flow<List<MenuItem>> = menuItemDao.getAllItems()

    fun getItemsByCategory(category: String): Flow<List<MenuItem>> =
        menuItemDao.getItemsByCategory(category)

    fun getAvailableItems(): Flow<List<MenuItem>> = menuItemDao.getAvailableItems()

    suspend fun addItem(item: MenuItem): Long = menuItemDao.insert(item)

    suspend fun updateItem(item: MenuItem) = menuItemDao.update(item)

    suspend fun deleteItem(item: MenuItem) = menuItemDao.delete(item)
}

