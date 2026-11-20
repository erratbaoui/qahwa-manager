package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.qahwamanager.data.model.Staff
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    /** People who can start a shift: the owner first, then waiters by name. */
    @Query("SELECT * FROM staff WHERE isActive = 1 ORDER BY isOwner DESC, name")
    fun observeActive(): Flow<List<Staff>>

    @Query("SELECT * FROM staff ORDER BY isOwner DESC, name")
    fun observeAll(): Flow<List<Staff>>

    @Query("SELECT * FROM staff WHERE id = :id")
    suspend fun getById(id: Long): Staff?

    @Insert
    suspend fun insert(staff: Staff): Long

    @Update
    suspend fun update(staff: Staff)
}
