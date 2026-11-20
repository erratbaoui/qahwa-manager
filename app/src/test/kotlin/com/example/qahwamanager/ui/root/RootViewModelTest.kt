package com.example.qahwamanager.ui.root

import com.example.qahwamanager.data.repository.OwnerAuthRepository
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeOwnerDao
import com.example.qahwamanager.testing.FakeShiftDao
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootViewModelTest {

    private val staffDao = FakeStaffDao()
    private val ownerDao = FakeOwnerDao(staffDao)
    private val orderDao = FakeOrderDao()
    private val shiftDao = FakeShiftDao(orderDao, staffDao)

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.repo() =
        OwnerAuthRepository(ownerDao, shiftDao, testClock(0), UnconfinedTestDispatcher(testScheduler))

    private fun TestScope.viewModel(): RootViewModel {
        val vm = RootViewModel(repo())
        backgroundScope.launch { vm.screen.collect {} }
        backgroundScope.launch { vm.recoveryCodeToShow.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun noOwner_showsSetup() = runTest {
        assertEquals(RootScreen.SETUP, viewModel().screen.value)
    }

    @Test
    fun existingOwner_showsApp() = runTest {
        repo().setUpOwner("Amina", "1234")

        assertEquals(RootScreen.APP, viewModel().screen.value)
    }

    @Test
    fun setUp_pinMismatch_returnsError() = runTest {
        val vm = viewModel()

        assertEquals(SetupError.PIN_MISMATCH, vm.setUp("Amina", "1234", "1243"))
        assertEquals(SetupError.INVALID_PIN, vm.setUp("Amina", "12", "12"))
        assertEquals(SetupError.BLANK_NAME, vm.setUp("  ", "1234", "1234"))
        advanceUntilIdle()

        assertNull(ownerDao.credentials.value)
        assertEquals(RootScreen.SETUP, vm.screen.value)
    }

    @Test
    fun setUp_success_keepsSetupUntilCodeAcknowledged() = runTest {
        val vm = viewModel()

        assertNull(vm.setUp(" Amina ", "1234", "1234"))
        advanceUntilIdle()

        assertNotNull(ownerDao.credentials.value)
        assertEquals("Amina", staffDao.staff.value.single().name)
        val code = vm.recoveryCodeToShow.value
        assertNotNull(code)
        assertEquals(9, code!!.length) // XXXX-XXXX
        assertEquals(RootScreen.SETUP, vm.screen.value)

        vm.acknowledgeRecoveryCode()
        runCurrent()

        assertNull(vm.recoveryCodeToShow.value)
        assertEquals(RootScreen.APP, vm.screen.value)
    }
}
