package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.qahwamanager.data.model.Spending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Dao
interface SpendingDao {
    @Query("SELECT * FROM spendings WHERE isDeleted = 0 ORDER BY spentAt DESC, id DESC")
    fun observeAll(): Flow<List<Spending>>

    @Query("SELECT * FROM spendings WHERE isDeleted = 0 AND shiftId = :shiftId ORDER BY spentAt DESC, id DESC")
    fun observeForShift(shiftId: Long): Flow<List<Spending>>

    @Query("SELECT COALESCE(SUM(amountCentimes), 0) FROM spendings WHERE isDeleted = 0 AND shiftId = :shiftId")
    fun observeTotalForShift(shiftId: Long): Flow<Long>

    @Query("SELECT * FROM spendings")
    suspend fun getAllSnapshot(): List<Spending>

    @Query("SELECT * FROM spendings WHERE id = :id")
    suspend fun getById(id: Long): Spending?

    @Query("SELECT * FROM spendings WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): Spending?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(spending: Spending): Long

    @Update
    suspend fun update(spending: Spending)
}

object EmptySpendingDao : SpendingDao {
    override fun observeAll(): Flow<List<Spending>> = flowOf(emptyList())
    override fun observeForShift(shiftId: Long): Flow<List<Spending>> = flowOf(emptyList())
    override fun observeTotalForShift(shiftId: Long): Flow<Long> = flowOf(0L)
    override suspend fun getAllSnapshot(): List<Spending> = emptyList()
    override suspend fun getById(id: Long): Spending? = null
    override suspend fun getByRemoteId(remoteId: String): Spending? = null
    override suspend fun insert(spending: Spending): Long = 0L
    override suspend fun update(spending: Spending) = Unit
}
