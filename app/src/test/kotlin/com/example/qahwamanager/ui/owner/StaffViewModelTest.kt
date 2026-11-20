package com.example.qahwamanager.ui.owner

import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.StaffRole
import com.example.qahwamanager.data.repository.StaffRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StaffViewModelTest {

    private val staffDao = FakeStaffDao()
    private lateinit var session: OwnerSession
    private var owner = 0L

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun TestScope.build(): StaffViewModel {
        val clock = testClock(0)
        owner = staffDao.insert(Staff(name = "Amina", isOwner = true, createdAt = 0))
        session = OwnerSession(clock)
        val vm = StaffViewModel(StaffRepository(staffDao, clock), session)
        backgroundScope.launch { vm.staff.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun add_locked_doesNothing() = runTest {
        val vm = build()

        vm.add("Karim")
        advanceUntilIdle()

        assertEquals(listOf("Amina"), vm.staff.value.map { it.name })
        assertNull(vm.error.value)
    }

    @Test
    fun add_ownerMode_addsTrimmed() = runTest {
        val vm = build()
        session.unlock()

        vm.add("  Karim ")
        advanceUntilIdle()

        assertEquals(listOf("Amina", "Karim"), vm.staff.value.map { it.name })
    }

    @Test
    fun add_blank_setsError() = runTest {
        val vm = build()
        session.unlock()

        vm.add("   ")
        advanceUntilIdle()

        assertEquals(StaffError.BLANK_NAME, vm.error.value)
        assertEquals(1, staffDao.staff.value.size)

        vm.clearError()
        assertNull(vm.error.value)
    }

    @Test
    fun deactivateOwner_setsError() = runTest {
        val vm = build()
        session.unlock()

        vm.setActive(owner, false)
        advanceUntilIdle()

        assertEquals(StaffError.OWNER_ALWAYS_ACTIVE, vm.error.value)
        assertEquals(true, staffDao.getById(owner)!!.isActive)
    }

    @Test
    fun renameAndDeactivate_ownerMode_apply() = runTest {
        val vm = build()
        session.unlock()
        val karim = staffDao.insert(Staff(name = "Karim", createdAt = 0))

        vm.rename(karim, "Karim B.")
        advanceUntilIdle()
        vm.setActive(karim, false)
        advanceUntilIdle()

        val stored = staffDao.getById(karim)!!
        assertEquals("Karim B.", stored.name)
        assertEquals(false, stored.isActive)
        assertEquals(listOf("Amina", "Karim B."), vm.staff.value.map { it.name })
    }

    @Test
    fun addAndUpdateRole_ownerMode_persistsRoles() = runTest {
        val vm = build()
        session.unlock()

        vm.add("Hassan", StaffRole.BARMAN)
        vm.add("Omar", StaffRole.SECURITY)
        advanceUntilIdle()

        val hassan = vm.staff.value.first { it.name == "Hassan" }
        val omar = vm.staff.value.first { it.name == "Omar" }
        assertEquals(StaffRole.BARMAN, hassan.role)
        assertEquals(StaffRole.SECURITY, omar.role)

        vm.update(omar.id, "Omar S.", StaffRole.OWNER)
        advanceUntilIdle()

        val updatedOmar = staffDao.getById(omar.id)!!
        assertEquals("Omar S.", updatedOmar.name)
        assertEquals(StaffRole.OWNER, updatedOmar.role)
    }
}
