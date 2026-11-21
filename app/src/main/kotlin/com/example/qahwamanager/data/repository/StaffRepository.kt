package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.cloud.NoOpSyncPublisher
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.example.qahwamanager.data.db.StaffDao
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.StaffRole
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Café staff (owner, waiters, barmen, security, cleaning). People are deactivated, never deleted, so their history stays. */
@Singleton
class StaffRepository @Inject constructor(
    private val staffDao: StaffDao,
    private val clock: Clock,
    private val syncPublisher: SyncPublisher = NoOpSyncPublisher,
) {
    fun observeActive(): Flow<List<Staff>> = staffDao.observeActive()

    fun observeAll(): Flow<List<Staff>> = staffDao.observeAll()

    suspend fun addStaff(name: String, role: StaffRole = StaffRole.WAITER): Long {
        val now = clock.nowMillis()
        val staff = Staff(name = cleanName(name), role = role, createdAt = now, updatedAt = now)
        val id = staffDao.insert(staff)
        syncPublisher.pushStaff(staff.copy(id = id))
        return id
    }

    suspend fun addWaiter(name: String): Long = addStaff(name, StaffRole.WAITER)

    suspend fun rename(id: Long, name: String) {
        val cleaned = cleanName(name)
        val staff = staffDao.getById(id) ?: return
        val updated = staff.copy(name = cleaned, updatedAt = clock.nowMillis())
        staffDao.update(updated)
        syncPublisher.pushStaff(updated)
    }

    /** Updates a staff member's name and role. The primary owner keeps the [StaffRole.OWNER] role. */
    suspend fun updateStaff(id: Long, name: String, role: StaffRole) {
        val cleaned = cleanName(name)
        val staff = staffDao.getById(id) ?: return
        val effectiveRole = if (staff.isOwner) StaffRole.OWNER else role
        val updated = staff.copy(name = cleaned, role = effectiveRole, updatedAt = clock.nowMillis())
        staffDao.update(updated)
        syncPublisher.pushStaff(updated)
    }

    /** Hides or shows a person in the start-shift list. The owner can't be deactivated. */
    suspend fun setActive(id: Long, active: Boolean) {
        val staff = staffDao.getById(id) ?: return
        require(active || !staff.isOwner) { "The owner can't be deactivated" }
        val updated = staff.copy(isActive = active, updatedAt = clock.nowMillis())
        staffDao.update(updated)
        syncPublisher.pushStaff(updated)
    }

    private fun cleanName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "Name is blank" } }
}
