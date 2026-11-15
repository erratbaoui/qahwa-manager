package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.qahwamanager.data.model.MenuItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MenuItemDao {
    @Query("SELECT * FROM menu_items ORDER BY category, name")
    fun getAllItems(): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE category = :category ORDER BY name")
    fun getItemsByCategory(category: String): Flow<List<MenuItem>>

    @Query("SELECT * FROM menu_items WHERE isAvailable = 1 ORDER BY category, name")
    fun getAvailableItems(): Flow<List<MenuItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MenuItem): Long

    @Update
    suspend fun update(item: MenuItem)

    @Delete
    suspend fun delete(item: MenuItem)

    @Query("SELECT * FROM menu_items WHERE id = :id")
    suspend fun getById(id: Long): MenuItem?
}

