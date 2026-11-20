package com.example.qahwamanager.ui.auth

import com.example.qahwamanager.data.repository.OwnerAuthRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.security.PinCheck
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeOwnerDao
import com.example.qahwamanager.testing.FakeShiftDao
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerPinViewModelTest {

    private val staffDao = FakeStaffDao()
    private val ownerDao = FakeOwnerDao(staffDao)
    private val orderDao = FakeOrderDao()
    private val shiftDao = FakeShiftDao(orderDao, staffDao)
    private lateinit var session: OwnerSession
    private lateinit var repo: OwnerAuthRepository
    private var recoveryCode = ""

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun TestScope.viewModel(): OwnerPinViewModel {
        val clock = testClock(0)
        repo = OwnerAuthRepository(ownerDao, shiftDao, clock, UnconfinedTestDispatcher(testScheduler))
        recoveryCode = repo.setUpOwner("Amina", "1234")
        session = OwnerSession(clock)
        return OwnerPinViewModel(repo, session)
    }

    @Test
    fun submit_correct_unlocksSessionAndCallsOk() = runTest {
        val vm = viewModel()
        var ok = 0

        vm.submit("1234") { ok++ }
        advanceUntilIdle()

        assertEquals(1, ok)
        assertTrue(session.isOwnerMode.value)
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.busy)
    }

    @Test
    fun submit_wrong_setsWrongError_keepsLocked() = runTest {
        val vm = viewModel()
        var ok = 0

        vm.submit("0000") { ok++ }
        advanceUntilIdle()

        assertEquals(0, ok)
        assertFalse(session.isOwnerMode.value)
        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 4), vm.state.value.error)
    }

    @Test
    fun submit_ignoresSecondTapWhileBusy() = runTest {
        val vm = viewModel()
        var ok = 0

        vm.submit("1234") { ok++ }
        vm.submit("1234") { ok++ }
        advanceUntilIdle()

        assertEquals(1, ok)
    }

    @Test
    fun resetWithCode_mismatch_doesNotCallRepo() = runTest {
        val vm = viewModel()
        val before = ownerDao.credentials.value
        var newCode: String? = null

        vm.resetWithCode(recoveryCode, "9876", "9867") { newCode = it }
        advanceUntilIdle()

        assertNull(newCode)
        assertEquals(before, ownerDao.credentials.value)
        assertEquals(PinInputError.PIN_MISMATCH, vm.state.value.inputError)
        assertFalse(session.isOwnerMode.value)
    }

    @Test
    fun resetWithCode_valid_unlocksAndReportsNewCode() = runTest {
        val vm = viewModel()
        var newCode: String? = null

        vm.resetWithCode(recoveryCode.lowercase(), "9876", "9876") { newCode = it }
        advanceUntilIdle()

        assertEquals(9, newCode!!.length)
        assertTrue(session.isOwnerMode.value)
        assertEquals(PinCheck.Ok, repo.checkPin("9876"))
    }

    @Test
    fun resetWithCode_wrongCode_setsWrongError() = runTest {
        val vm = viewModel()

        vm.resetWithCode("ZZZZ-ZZZZ", "9876", "9876") {}
        advanceUntilIdle()

        assertTrue(vm.state.value.error is PinCheck.Wrong)
        assertFalse(session.isOwnerMode.value)
    }

    @Test
    fun resetWithCode_keepsNewCodeInStateUntilAcknowledged() = runTest {
        val vm = viewModel()

        vm.resetWithCode(recoveryCode, "9876", "9876") {}
        advanceUntilIdle()

        val shown = vm.state.value.newRecoveryCode
        assertEquals(9, shown!!.length)

        vm.acknowledgeRecoveryCode()
        assertNull(vm.state.value.newRecoveryCode)
    }

    @Test
    fun issueNewRecoveryCode_locked_doesNothing() = runTest {
        val vm = viewModel()
        val before = ownerDao.credentials.value

        vm.issueNewRecoveryCode()
        advanceUntilIdle()

        assertNull(vm.state.value.newRecoveryCode)
        assertEquals(before, ownerDao.credentials.value)
    }

    @Test
    fun issueNewRecoveryCode_ownerMode_rotatesAndShowsCode() = runTest {
        val vm = viewModel()
        session.unlock()

        vm.issueNewRecoveryCode()
        advanceUntilIdle()

        val shown = vm.state.value.newRecoveryCode!!
        assertEquals(9, shown.length)
        assertNull(repo.resetPinWithRecoveryCode(recoveryCode, "9876"))
        assertEquals(PinCheck.Ok, repo.checkPin("1234"))
    }

    @Test
    fun changePin_requiresMatchingNewPinAndCurrentPin() = runTest {
        val vm = viewModel()
        var done = 0

        vm.changePin("1234", "5555", "5556") { done++ }
        advanceUntilIdle()
        assertEquals(PinInputError.PIN_MISMATCH, vm.state.value.inputError)

        vm.changePin("0000", "5555", "5555") { done++ }
        advanceUntilIdle()
        assertTrue(vm.state.value.error is PinCheck.Wrong)

        vm.changePin("1234", "5555", "5555") { done++ }
        advanceUntilIdle()
        assertEquals(1, done)
        assertEquals(PinCheck.Ok, repo.checkPin("5555"))
    }
}
