package com.example.qahwamanager.ui.owner

import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeShiftDao
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class ShiftReportViewModelTest {

    private val staffDao = FakeStaffDao()
    private val orderDao = FakeOrderDao()
    private val shiftDao = FakeShiftDao(orderDao, staffDao)
    private lateinit var session: OwnerSession
    private var shiftId = 0L

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** One ENDED shift with 64 000 expected. */
    private suspend fun TestScope.build(): ShiftReportViewModel {
        val clock = testClock(0)
        val karim = staffDao.insert(Staff(name = "Karim", createdAt = 0))
        shiftId = shiftDao.startShift(karim, now = 1000)
        orderDao.seed(Order(id = 1, tableNumber = "1", status = OrderStatus.PAID, createdAt = 1500, totalCentimes = 64_000, paidShiftId = shiftId, paidAt = 1500))
        shiftDao.endActiveShift(now = 2000, takeover = false)
        session = OwnerSession(clock)
        val vm = ShiftReportViewModel(ShiftRepository(shiftDao, orderDao, clock), session)
        backgroundScope.launch { vm.countedCentimes.collect {} }
        runCurrent()
        return vm
    }

    private fun status() = shiftDao.shifts.value.single { it.id == shiftId }.status

    @Test
    fun countedInput_frenchCommaParses() = runTest {
        val vm = build()

        vm.setCountedInput("610,50")
        runCurrent()

        assertEquals("610,50", vm.countedInput.value)
        assertEquals(61_050L, vm.countedCentimes.value)
        assertEquals(64_000L, vm.report(shiftId).first()!!.expectedCentimes)
    }

    @Test
    fun settle_refusesInvalidInput() = runTest {
        val vm = build()
        session.unlock()
        var done = 0

        for (input in listOf("", "abc", "12.345", "-5")) {
            vm.setCountedInput(input)
            runCurrent()
            assertNull("'$input' must not parse", vm.countedCentimes.value)
            vm.settle(shiftId) { done++ }
            advanceUntilIdle()
        }

        assertEquals(0, done)
        assertEquals(ShiftStatus.ENDED, status())
    }

    @Test
    fun settle_locked_doesNothing() = runTest {
        val vm = build()
        vm.setCountedInput("610")
        runCurrent()
        var done = 0

        vm.settle(shiftId) { done++ }
        advanceUntilIdle()

        assertEquals(0, done)
        assertEquals(ShiftStatus.ENDED, status())
    }

    @Test
    fun settle_ownerMode_settlesAndCallsDone() = runTest {
        val vm = build()
        session.unlock()
        vm.setCountedInput("610")
        runCurrent()
        var done = 0

        vm.settle(shiftId) { done++ }
        advanceUntilIdle()

        assertEquals(1, done)
        val shift = shiftDao.shifts.value.single()
        assertEquals(ShiftStatus.SETTLED, shift.status)
        assertEquals(64_000L, shift.expectedCentimes)
        assertEquals(61_000L, shift.countedCentimes)
        assertEquals(false, vm.isSaving.value)
    }

    @Test
    fun settle_ignoresSecondTapWhileSaving() = runTest {
        val vm = build()
        session.unlock()
        vm.setCountedInput("610")
        runCurrent()
        var done = 0

        vm.settle(shiftId) { done++ }
        vm.settle(shiftId) { done++ }
        advanceUntilIdle()

        assertEquals(1, done)
        assertEquals(61_000L, shiftDao.shifts.value.single().countedCentimes)
    }
}
