package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.StaffRole
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffRepositoryTest {

    private val staffDao = FakeStaffDao()

    @Test
    fun addWaiter_trimsName() = runTest {
        val repo = StaffRepository(staffDao, testClock(5_000))

        val id = repo.addWaiter("  Karim ")

        val karim = staffDao.getById(id)!!
        assertEquals("Karim", karim.name)
        assertEquals(false, karim.isOwner)
        assertEquals(true, karim.isActive)
        assertEquals(5_000L, karim.createdAt)
    }

    @Test
    fun addWaiter_blank_throws() = runTest {
        val repo = StaffRepository(staffDao, testClock(0))

        assertTrue(runCatching { repo.addWaiter("   ") }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { repo.rename(1, "") }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(staffDao.staff.value.isEmpty())
    }

    @Test
    fun deactivateOwner_throws() = runTest {
        val repo = StaffRepository(staffDao, testClock(0))
        val owner = staffDao.insert(Staff(name = "Amina", isOwner = true, createdAt = 0))

        assertTrue(runCatching { repo.setActive(owner, false) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(staffDao.getById(owner)!!.isActive)
    }

    @Test
    fun deactivatedWaiter_leavesActiveList_keepsInAll() = runTest {
        val repo = StaffRepository(staffDao, testClock(0))
        staffDao.insert(Staff(name = "Amina", isOwner = true, createdAt = 0))
        val karim = repo.addWaiter("Karim")
        repo.addWaiter("Youssef")

        repo.setActive(karim, false)

        assertEquals(listOf("Amina", "Youssef"), repo.observeActive().first().map { it.name })
        assertEquals(listOf("Amina", "Karim", "Youssef"), repo.observeAll().first().map { it.name })

        repo.rename(karim, " Karim B. ")
        assertEquals("Karim B.", staffDao.getById(karim)!!.name)
    }

    @Test
    fun addStaff_andUpdateStaff_persistsAllRolesAndProtectsPrimaryOwnerRole() = runTest {
        val repo = StaffRepository(staffDao, testClock(1_000))
        val owner = staffDao.insert(Staff(name = "Amina", role = StaffRole.OWNER, isOwner = true, createdAt = 0))
        val barman = repo.addStaff("Hassan", StaffRole.BARMAN)
        val security = repo.addStaff("Omar", StaffRole.SECURITY)
        val coOwner = repo.addStaff("Nadia", StaffRole.OWNER)

        assertEquals(StaffRole.BARMAN, staffDao.getById(barman)!!.role)
        assertEquals(StaffRole.SECURITY, staffDao.getById(security)!!.role)
        assertEquals(StaffRole.OWNER, staffDao.getById(coOwner)!!.role)

        repo.updateStaff(barman, "Hassan M.", StaffRole.WAITER)
        assertEquals("Hassan M.", staffDao.getById(barman)!!.name)
        assertEquals(StaffRole.WAITER, staffDao.getById(barman)!!.role)

        // Primary owner keeps OWNER role even if another role is passed
        repo.updateStaff(owner, "Amina K.", StaffRole.WAITER)
        assertEquals("Amina K.", staffDao.getById(owner)!!.name)
        assertEquals(StaffRole.OWNER, staffDao.getById(owner)!!.role)
    }
}
