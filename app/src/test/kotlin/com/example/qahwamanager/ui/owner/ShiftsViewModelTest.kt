package com.example.qahwamanager.ui.owner

import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.data.repository.StaffRepository
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeShiftDao
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShiftsViewModelTest {

    private val staffDao = FakeStaffDao()
    private val orderDao = FakeOrderDao()
    private val shiftDao = FakeShiftDao(orderDao, staffDao)
    private var karim = 0L
    private var youssef = 0L

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Karim: ended at 2000 then settled −3000; Youssef: ended at 4000 then settled +500; Karim again: ended at 6000, unsettled. */
    private suspend fun TestScope.seedAndBuild(): ShiftsViewModel {
        val clock = testClock(0)
        karim = staffDao.insert(Staff(name = "Karim", createdAt = 0))
        youssef = staffDao.insert(Staff(name = "Youssef", createdAt = 0))
        val a = shiftDao.startShift(karim, now = 1000)
        orderDao.seed(Order(id = 1, tableNumber = "1", status = OrderStatus.PAID, createdAt = 1500, totalCentimes = 64_000, paidShiftId = a, paidAt = 1500))
        shiftDao.endActiveShift(now = 2000, takeover = false)
        val b = shiftDao.startShift(youssef, now = 3000)
        orderDao.seed(Order(id = 2, tableNumber = "1", status = OrderStatus.PAID, createdAt = 3500, totalCentimes = 10_000, paidShiftId = b, paidAt = 3500))
        shiftDao.endActiveShift(now = 4000, takeover = false)
        shiftDao.startShift(karim, now = 5000)
        shiftDao.endActiveShift(now = 6000, takeover = false)
        shiftDao.settle(a, countedCentimes = 61_000, now = 7000)
        shiftDao.settle(b, countedCentimes = 10_500, now = 8000)

        val vm = ShiftsViewModel(ShiftRepository(shiftDao, orderDao, clock), StaffRepository(staffDao, clock))
        backgroundScope.launch { vm.toSettle.collect {} }
        backgroundScope.launch { vm.history.collect {} }
        backgroundScope.launch { vm.staff.collect {} }
        backgroundScope.launch { vm.totalDifferenceCentimes.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun toSettle_oldestFirst() = runTest {
        val vm = seedAndBuild()
        shiftDao.startShift(youssef, now = 9000)
        shiftDao.endActiveShift(now = 9500, takeover = false)
        runCurrent()

        assertEquals(listOf("Karim", "Youssef"), vm.toSettle.value.map { it.staffName })
        assertEquals(listOf(6000L, 9500L), vm.toSettle.value.map { it.shift.endedAt })
    }

    @Test
    fun history_filterByStaff() = runTest {
        val vm = seedAndBuild()
        assertEquals(listOf("Youssef", "Karim"), vm.history.value.map { it.staffName })
        assertEquals(listOf("Karim", "Youssef"), vm.staff.value.map { it.name })

        vm.selectStaff(karim)
        runCurrent()

        assertEquals(karim, vm.selectedStaffId.value)
        assertEquals(listOf("Karim"), vm.history.value.map { it.staffName })

        vm.selectStaff(null)
        runCurrent()
        assertEquals(2, vm.history.value.size)
    }

    @Test
    fun totalDifference_sumsFilteredHistory() = runTest {
        val vm = seedAndBuild()
        assertEquals(-2500L, vm.totalDifferenceCentimes.value)

        vm.selectStaff(karim)
        runCurrent()
        assertEquals(-3000L, vm.totalDifferenceCentimes.value)

        vm.selectStaff(youssef)
        runCurrent()
        assertEquals(500L, vm.totalDifferenceCentimes.value)
    }
}
