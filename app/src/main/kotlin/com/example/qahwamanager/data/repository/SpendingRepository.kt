package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.cloud.NoOpSyncPublisher
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.example.qahwamanager.data.db.SpendingDao
import com.example.qahwamanager.data.model.Spending
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpendingRepository @Inject constructor(
    private val spendingDao: SpendingDao,
    private val clock: Clock,
    private val syncPublisher: SyncPublisher = NoOpSyncPublisher,
) {
    fun observeAll(): Flow<List<Spending>> = spendingDao.observeAll()

    fun observeForShift(shiftId: Long): Flow<List<Spending>> = spendingDao.observeForShift(shiftId)

    fun observeTotalForShift(shiftId: Long): Flow<Long> = spendingDao.observeTotalForShift(shiftId)

    suspend fun addSpending(
        title: String,
        amountCentimes: Long,
        shiftId: Long? = null,
        spentAt: Long = clock.nowMillis(),
    ): Long {
        val cleaned = title.trim()
        require(cleaned.isNotEmpty()) { "Spending title is blank" }
        require(amountCentimes > 0L) { "Spending amount must be positive" }
        val now = clock.nowMillis()
        val spending = Spending(
            shiftId = shiftId,
            title = cleaned,
            amountCentimes = amountCentimes,
            spentAt = spentAt,
            updatedAt = now,
        )
        val id = spendingDao.insert(spending)
        syncPublisher.pushSpending(id)
        return id
    }

    suspend fun deleteSpending(id: Long) {
        val existing = spendingDao.getById(id) ?: return
        val tombstoned = existing.copy(isDeleted = true, updatedAt = clock.nowMillis())
        spendingDao.update(tombstoned)
        syncPublisher.pushSpending(id)
    }
}
