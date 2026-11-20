package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.model.Staff
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
}
