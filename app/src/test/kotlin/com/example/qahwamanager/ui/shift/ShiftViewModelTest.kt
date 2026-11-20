package com.example.qahwamanager.ui.shift

import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.data.repository.StaffRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeOrderItemDao
import com.example.qahwamanager.testing.FakeShiftDao
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftViewModelTest {

    private val start = 1_700_000_000_000L
    private val staffDao = FakeStaffDao()
    private val orderDao = FakeOrderDao()
    private val shiftDao = FakeShiftDao(orderDao, staffDao)
    private var karim = 0L
    private var youssef = 0L

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private suspend fun TestScope.viewModel(): ShiftViewModel {
        val clock = testClock(start)
        karim = staffDao.insert(Staff(name = "Karim", createdAt = 0))
        youssef = staffDao.insert(Staff(name = "Youssef", createdAt = 0))
        val vm = ShiftViewModel(
            ShiftRepository(shiftDao, orderDao, clock),
            StaffRepository(staffDao, clock),
            OrderRepository(orderDao, FakeOrderItemDao(orderDao), clock),
            OwnerSession(clock),
        )
        backgroundScope.launch { vm.activeShift.collect {} }
        backgroundScope.launch { vm.activeStaff.collect {} }
        backgroundScope.launch { vm.openOrders.collect {} }
        backgroundScope.launch { vm.collectedCentimes.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun start_thenActiveShiftShowsStaffName() = runTest {
        val vm = viewModel()
        assertNull(vm.activeShift.value)
        assertEquals(listOf("Karim", "Youssef"), vm.activeStaff.value.map { it.name })

        vm.start(karim)
        advanceUntilIdle()

        assertEquals("Karim", vm.activeShift.value?.staffName)
        assertEquals(start, vm.activeShift.value?.shift?.startedAt)
    }

    @Test
    fun start_doubleTap_createsOneShift() = runTest {
        val vm = viewModel()

        vm.start(karim)
        vm.start(youssef)
        advanceUntilIdle()

        assertEquals(1, shiftDao.shifts.value.size)
        assertEquals("Karim", vm.activeShift.value?.staffName)
    }

    @Test
    fun collected_tracksPaidOrdersInShift() = runTest {
        val vm = viewModel()
        vm.start(karim)
        advanceUntilIdle()
        orderDao.seed(Order(id = 1, tableNumber = "1", totalCentimes = 800), Order(id = 2, tableNumber = "2", totalCentimes = 1200))
        runCurrent()
        assertEquals(0L, vm.collectedCentimes.value)
        assertEquals(2, vm.openOrders.value.size)

        vm.markPaid(1)
        advanceUntilIdle()
        vm.markPaid(2)
        advanceUntilIdle()

        assertEquals(2000L, vm.collectedCentimes.value)
        assertTrue(vm.openOrders.value.isEmpty())
    }

    @Test
    fun endShift_withOpenOrders_recordsLeftOpenAndCallsDone() = runTest {
        val vm = viewModel()
        vm.start(karim)
        advanceUntilIdle()
        val shiftId = vm.activeShift.value!!.shift.id
        orderDao.seed(Order(id = 5, tableNumber = "4", totalCentimes = 500))
        var done = 0

        vm.endShift { done++ }
        advanceUntilIdle()

        assertEquals(1, done)
        assertNull(vm.activeShift.value)
        assertEquals(0L, vm.collectedCentimes.value)
        assertEquals(listOf(5L), shiftDao.leftOpen.value.filter { it.shiftId == shiftId }.map { it.orderId })
        assertEquals(OrderStatus.OPEN, orderDao.statusOf(5))
    }

    @Test
    fun takeOver_endsPreviousAndStartsNew() = runTest {
        val vm = viewModel()
        vm.start(karim)
        advanceUntilIdle()
        val old = vm.activeShift.value!!.shift.id

        vm.takeOver(youssef)
        advanceUntilIdle()

        val oldShift = shiftDao.shifts.value.single { it.id == old }
        assertEquals(ShiftStatus.ENDED, oldShift.status)
        assertTrue(oldShift.endedByTakeover)
        assertEquals("Youssef", vm.activeShift.value?.staffName)
    }
}
