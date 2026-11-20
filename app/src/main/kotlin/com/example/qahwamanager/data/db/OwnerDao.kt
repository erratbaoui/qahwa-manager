package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.qahwamanager.data.model.OwnerCredentials
import com.example.qahwamanager.data.model.Staff
import kotlinx.coroutines.flow.Flow

@Dao
abstract class OwnerDao {
    @Query("SELECT COUNT(*) > 0 FROM owner_credentials")
    abstract fun observeExists(): Flow<Boolean>

    @Query("SELECT * FROM owner_credentials WHERE id = 1")
    abstract suspend fun getCredentials(): OwnerCredentials?

    @Upsert
    abstract suspend fun upsertCredentials(c: OwnerCredentials)

    @Insert
    abstract suspend fun insertStaff(staff: Staff): Long

    /** First-run setup: the owner's staff row and credentials land together or not at all. */
    @Transaction
    open suspend fun createOwner(owner: Staff, credentials: OwnerCredentials): Long {
        val id = insertStaff(owner)
        upsertCredentials(credentials)
        return id
    }
}
