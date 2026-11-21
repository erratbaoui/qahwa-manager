package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.qahwamanager.data.model.InventoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 ORDER BY name")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE isDeleted = 0 AND quantity <= lowStockThreshold ORDER BY name")
    fun getLowStockItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items")
    suspend fun getAllSnapshot(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItem): Long

    @Update
    suspend fun update(item: InventoryItem)

    @Delete
    suspend fun delete(item: InventoryItem)

    /** Applies [delta] to the stored quantity in one statement, so rapid taps can't overwrite each other. */
    @Query("UPDATE inventory_items SET quantity = MAX(0, quantity + :delta), updatedAt = :updatedAt WHERE id = :id")
    suspend fun adjustQuantity(id: Long, delta: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: Long): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): InventoryItem?
}
