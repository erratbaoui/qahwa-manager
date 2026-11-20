package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.db.StaffDao
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Waiters and the owner. People are deactivated, never deleted, so their history stays. */
@Singleton
class StaffRepository @Inject constructor(
    private val staffDao: StaffDao,
    private val clock: Clock
) {
    fun observeActive(): Flow<List<Staff>> = staffDao.observeActive()

    fun observeAll(): Flow<List<Staff>> = staffDao.observeAll()

    suspend fun addWaiter(name: String): Long =
        staffDao.insert(Staff(name = cleanName(name), createdAt = clock.nowMillis()))

    suspend fun rename(id: Long, name: String) {
        val cleaned = cleanName(name)
        val staff = staffDao.getById(id) ?: return
        staffDao.update(staff.copy(name = cleaned))
    }

    /** Hides or shows a person in the start-shift list. The owner can't be deactivated. */
    suspend fun setActive(id: Long, active: Boolean) {
        val staff = staffDao.getById(id) ?: return
        require(active || !staff.isOwner) { "The owner can't be deactivated" }
        staffDao.update(staff.copy(isActive = active))
    }

    private fun cleanName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Name is blank" } }
}
