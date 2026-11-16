package com.example.qahwamanager.ui.sales

import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.repository.SalesRepository
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SalesViewModelTest {

    // 2026-11-16T00:00:00Z is a Monday.
    private val nov16 = 1_794_787_200_000L
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val wednesday10am = nov16 + 2 * day + 10 * hour

    private lateinit var orderDao: FakeOrderDao

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        orderDao = FakeOrderDao()
        orderDao.seed(
            Order(id = 1, tableNumber = "1", status = OrderStatus.PAID, createdAt = nov16 - 6 * day, totalCentimes = 300),
            Order(id = 2, tableNumber = "2", status = OrderStatus.PAID, createdAt = nov16 + 9 * hour, totalCentimes = 1000),
            Order(id = 3, tableNumber = "3", status = OrderStatus.PAID, createdAt = nov16 + 2 * day + 8 * hour, totalCentimes = 500),
            Order(id = 4, tableNumber = "4", status = OrderStatus.OPEN, createdAt = nov16 + 2 * day + 9 * hour, totalCentimes = 700),
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): SalesViewModel {
        val vm = SalesViewModel(SalesRepository(orderDao, testClock(startMillis = wednesday10am)))
        backgroundScope.launch { vm.totalRevenue.collect {} }
        backgroundScope.launch { vm.paidOrders.collect {} }
        runCurrent()
        return vm
    }

    @Test
    fun todayFilter_countsOnlyTodaysPaidOrders() = runTest {
        assertEquals(500L, viewModel().totalRevenue.value)
    }

    @Test
    fun thisWeekFilter_countsPaidOrdersSinceTheStartOfTheWeek() = runTest {
        val vm = viewModel()
        vm.setFilter(SalesFilter.THIS_WEEK)
        runCurrent()

        assertEquals(1500L, vm.totalRevenue.value)
    }

    @Test
    fun allTimeFilter_countsEveryPaidOrder() = runTest {
        val vm = viewModel()
        vm.setFilter(SalesFilter.ALL_TIME)
        runCurrent()

        assertEquals(1800L, vm.totalRevenue.value)
    }

    @Test
    fun todayFilter_rollsOverAtMidnightWhileOnScreen() = runTest {
        val vm = viewModel()

        advanceTimeBy(15 * hour) // Thursday 01:00
        runCurrent()

        assertEquals(0L, vm.totalRevenue.value)
    }
}
