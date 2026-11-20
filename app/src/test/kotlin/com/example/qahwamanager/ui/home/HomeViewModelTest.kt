package com.example.qahwamanager.ui.home

import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.repository.InventoryRepository
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.data.repository.SalesRepository
import com.example.qahwamanager.testing.FakeInventoryDao
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeOrderItemDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
class HomeViewModelTest {

    // 2026-11-16T00:00:00Z
    private val nov16 = 1_794_787_200_000L
    private val nov17 = nov16 + 86_400_000L

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun todayRevenue_rollsOverAtMidnight() = runTest {
        val orderDao = FakeOrderDao()
        orderDao.seed(Order(id = 1, tableNumber = "1", status = OrderStatus.PAID, createdAt = nov16 + 36_000_000L, totalCentimes = 1000))
        val clock = testClock(startMillis = nov17 - 60_000L)
        val viewModel = HomeViewModel(
            SalesRepository(orderDao, clock),
            OrderRepository(orderDao, FakeOrderItemDao(orderDao), clock),
            InventoryRepository(FakeInventoryDao()),
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(1000L, viewModel.uiState.value.todayRevenueCentimes)

        advanceTimeBy(120_000L) // 00:01 on Nov 17
        orderDao.seed(Order(id = 2, tableNumber = "2", status = OrderStatus.PAID, createdAt = nov17 + 30_000L, totalCentimes = 500))
        runCurrent()

        assertEquals(500L, viewModel.uiState.value.todayRevenueCentimes)
    }
}
