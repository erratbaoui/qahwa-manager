package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.security.PinCheck
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeOwnerDao
import com.example.qahwamanager.testing.FakeShiftDao
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerAuthRepositoryTest {

    private val staffDao = FakeStaffDao()
    private val orderDao = FakeOrderDao()
    private val shiftDao = FakeShiftDao(orderDao, staffDao)
    private val ownerDao = FakeOwnerDao(staffDao)
    private val start = 1_700_000_000_000L

    private fun TestScope.repo() = OwnerAuthRepository(ownerDao, shiftDao, testClock(start))

    /** The clock the repository sees: the fakes charge virtual time for every write. */
    private fun TestScope.now() = start + testScheduler.currentTime

    private suspend fun OwnerAuthRepository.failTimes(n: Int): List<PinCheck> = (1..n).map { checkPin("0000") }

    @Test
    fun setUpOwner_createsOwnerStaffAndStoresNoPlainPin() = runTest {
        val repo = repo()
        assertFalse(repo.observeOwnerExists().first())

        val code = repo.setUpOwner("Amina", "1234")

        assertTrue(repo.observeOwnerExists().first())
        val owner = staffDao.staff.value.single()
        assertEquals("Amina", owner.name)
        assertTrue(owner.isOwner)
        assertTrue(owner.isActive)
        assertEquals(8, code.length)
        val stored = ownerDao.credentials.value!!
        assertFalse(stored.toString().contains("1234"))
        assertFalse(stored.toString().contains(code))
    }

    @Test
    fun setUpOwner_rejectsBlankNameInvalidPinAndSecondOwner() = runTest {
        val repo = repo()
        assertTrue(runCatching { repo.setUpOwner("  ", "1234") }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { repo.setUpOwner("Amina", "12") }.exceptionOrNull() is IllegalArgumentException)
        repo.setUpOwner("Amina", "1234")
        assertTrue(runCatching { repo.setUpOwner("Other", "1234") }.exceptionOrNull() is IllegalStateException)
        assertEquals(1, staffDao.staff.value.size)
    }

    @Test
    fun checkPin_correct_isOkAndResetsFailures() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")
        repo.failTimes(3)

        assertEquals(PinCheck.Ok, repo.checkPin("1234"))

        assertEquals(0, ownerDao.credentials.value!!.failedAttemptsInRow)
        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 4), repo.checkPin("0000"))
    }

    @Test
    fun checkPin_wrongFiveTimes_locksForOneMinute() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")

        val firstFour = repo.failTimes(4)
        val lockedAt = now()
        val fifth = repo.checkPin("0000")

        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 1), firstFour[3])
        assertEquals(PinCheck.LockedOut(untilMillis = lockedAt + 60_000), fifth)
    }

    @Test
    fun correctPinDuringLockout_isRefused() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")
        val locked = repo.failTimes(5).last()
        assertTrue(locked is PinCheck.LockedOut)

        assertEquals(locked, repo.checkPin("1234"))
    }

    @Test
    fun lockout_persistsAcrossRepositoryInstances() = runTest {
        val locked = repo().run { setUpOwner("Amina", "1234"); failTimes(5).last() }
        assertTrue(locked is PinCheck.LockedOut)

        assertEquals(locked, repo().checkPin("1234"))
    }

    @Test
    fun lockout_endsAfterDuration() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")
        repo.failTimes(5)

        advanceTimeBy(60_000)
        assertEquals(PinCheck.Ok, repo.checkPin("1234"))
        assertEquals(0, ownerDao.credentials.value!!.failedAttemptsInRow)
    }

    @Test
    fun wrongPinAfterLockout_locksTwiceAsLong() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")
        repo.failTimes(5)
        advanceTimeBy(60_000)

        val lockedAt = now()
        assertEquals(PinCheck.LockedOut(untilMillis = lockedAt + 120_000), repo.checkPin("0000"))
    }

    @Test
    fun wrongPin_recordsAttemptOnActiveShift() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")
        val karim = staffDao.insert(Staff(name = "Karim", createdAt = 1))
        val shiftId = shiftDao.startShift(karim, now = start)

        repo.checkPin("0000")
        assertEquals(1, shiftDao.shifts.value.single { it.id == shiftId }.failedOwnerPinAttempts)

        shiftDao.endActiveShift(now = start + 1, takeover = false)
        repo.checkPin("0000")
        assertEquals(1, shiftDao.shifts.value.single { it.id == shiftId }.failedOwnerPinAttempts)
        assertEquals(2, ownerDao.credentials.value!!.failedAttemptsInRow)
        assertEquals(ShiftStatus.ENDED, shiftDao.shifts.value.single().status)
    }

    @Test
    fun recoveryCode_resetsPinAndRotatesCode() = runTest {
        val repo = repo()
        val oldCode = repo.setUpOwner("Amina", "1234")

        val newCode = repo.resetPinWithRecoveryCode(oldCode.lowercase(), "9876")

        assertNotNull(newCode)
        assertNotEquals(oldCode, newCode)
        assertEquals(PinCheck.Ok, repo.checkPin("9876"))
        assertTrue(repo.checkPin("1234") is PinCheck.Wrong)
        assertNull(repo.resetPinWithRecoveryCode(oldCode, "1111"))
        assertNotNull(repo.resetPinWithRecoveryCode(newCode!!, "2222"))
        assertEquals(PinCheck.Ok, repo.checkPin("2222"))
    }

    @Test
    fun wrongRecoveryCode_countsAsFailure() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")
        val karim = staffDao.insert(Staff(name = "Karim", createdAt = 1))
        shiftDao.startShift(karim, now = start)

        assertNull(repo.resetPinWithRecoveryCode("ZZZZ-ZZZZ", "9876"))

        assertEquals(1, ownerDao.credentials.value!!.failedAttemptsInRow)
        assertEquals(1, shiftDao.shifts.value.single().failedOwnerPinAttempts)
        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 3), repo.checkPin("0000"))
    }

    @Test
    fun changePin_requiresCurrentPin() = runTest {
        val repo = repo()
        val code = repo.setUpOwner("Amina", "1234")

        assertEquals(PinCheck.Wrong(attemptsBeforeLock = 4), repo.changePin("0000", "5555"))
        assertEquals(PinCheck.Ok, repo.changePin("1234", "5555"))

        assertEquals(PinCheck.Ok, repo.checkPin("5555"))
        assertTrue(repo.checkPin("1234") is PinCheck.Wrong)
        assertNotNull("the recovery code survives a PIN change", repo.resetPinWithRecoveryCode(code, "7777"))
    }

    @Test
    fun changePin_rejectsInvalidNewPin() = runTest {
        val repo = repo()
        repo.setUpOwner("Amina", "1234")

        assertTrue(runCatching { repo.changePin("1234", "12") }.exceptionOrNull() is IllegalArgumentException)
        assertEquals(PinCheck.Ok, repo.checkPin("1234"))
    }
}
