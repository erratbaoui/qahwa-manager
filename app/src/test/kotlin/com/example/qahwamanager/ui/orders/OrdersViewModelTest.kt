package com.example.qahwamanager.ui.orders

import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.repository.MenuRepository
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.testing.FakeMenuItemDao
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val tea = MenuItem(
        id = 7, name = "Mint tea", nameAr = "أتاي", nameFr = "Thé à la menthe",
        category = "tea", priceCentimes = 850,
    )
    private val start = 1_700_000_000_000L
    private lateinit var orderDao: FakeOrderDao
    private lateinit var shiftDao: FakeShiftDao
    private lateinit var ownerSession: OwnerSession
    private lateinit var viewModel: OrdersViewModel
    private var karim = 0L

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        orderDao = FakeOrderDao()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Builds the view model over the fakes; by default Karim's shift is already running. */
    private suspend fun TestScope.build(withActiveShift: Boolean = true) {
        val clock = testClock(start)
        val staffDao = FakeStaffDao()
        shiftDao = FakeShiftDao(orderDao, staffDao)
        karim = staffDao.insert(Staff(name = "Karim", createdAt = 0))
        if (withActiveShift) shiftDao.startShift(karim, now = start)
        ownerSession = OwnerSession(clock)
        viewModel = OrdersViewModel(
            OrderRepository(orderDao, FakeOrderItemDao(orderDao), clock),
            MenuRepository(FakeMenuItemDao(listOf(tea))),
            ShiftRepository(shiftDao, orderDao, clock),
            ownerSession,
        )
        backgroundScope.launch { viewModel.activeShift.collect {} }
        runCurrent()
    }

    @Test
    fun placeOrder_ignoresASecondTapWhileTheFirstIsSaving() = runTest {
        build()
        viewModel.addToCart(tea)

        viewModel.placeOrder("4") {}
        viewModel.placeOrder("4") {}
        advanceUntilIdle()

        assertEquals(1, orderDao.orders.value.size)
    }

    @Test
    fun placeOrder_storesCentimesAndLocalizedNameSnapshots() = runTest {
        build()
        viewModel.addToCart(tea)
        viewModel.addToCart(tea)

        viewModel.placeOrder("4") {}
        advanceUntilIdle()

        assertEquals(1700L, orderDao.orders.value.single().totalCentimes)
        val item = orderDao.items.value.single()
        assertEquals(listOf("Mint tea", "أتاي", "Thé à la menthe"), listOf(item.menuItemName, item.menuItemNameAr, item.menuItemNameFr))
        assertEquals(2, item.quantity)
        assertEquals(850L, item.unitPriceCentimes)
        assertTrue(viewModel.cart.value.isEmpty())
    }

    @Test
    fun placeOrder_stampsOpenedShift() = runTest {
        build()
        viewModel.addToCart(tea)

        viewModel.placeOrder("4") {}
        advanceUntilIdle()

        val order = orderDao.orders.value.single()
        assertEquals(shiftDao.getActive()!!.id, order.openedShiftId)
        assertNull(order.paidShiftId)
        assertEquals("Karim", viewModel.activeShift.value?.staffName)
    }

    @Test
    fun placeOrder_withoutActiveShift_doesNothing() = runTest {
        build(withActiveShift = false)
        viewModel.addToCart(tea)
        var called = false

        viewModel.placeOrder("4") { called = true }
        advanceUntilIdle()

        assertTrue(orderDao.orders.value.isEmpty())
        assertEquals(1, viewModel.cart.value.size)
        assertTrue(!called)
        assertNull(viewModel.activeShift.value)
    }

    @Test
    fun markAsPaid_reportsDoneOnlyAfterTheStatusIsSaved() = runTest {
        build()
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))
        var statusWhenDone: OrderStatus? = null

        viewModel.markAsPaid(3) { statusWhenDone = orderDao.statusOf(3) }
        advanceUntilIdle()

        assertEquals(OrderStatus.PAID, statusWhenDone)
    }

    @Test
    fun markAsPaid_stampsPaidShiftAndTime() = runTest {
        build()
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))

        viewModel.markAsPaid(3)
        advanceUntilIdle()

        val order = orderDao.orders.value.single()
        assertEquals(shiftDao.getActive()!!.id, order.paidShiftId)
        assertNotNull(order.paidAt)
        assertTrue(order.paidAt!! >= start)
    }

    @Test
    fun markAsPaid_withoutActiveShift_doesNothing() = runTest {
        build(withActiveShift = false)
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))
        var called = false

        viewModel.markAsPaid(3) { called = true }
        advanceUntilIdle()

        assertEquals(OrderStatus.OPEN, orderDao.statusOf(3))
        assertTrue(!called)
    }

    @Test
    fun cancelOrder_locked_doesNothing() = runTest {
        build()
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))
        var called = false

        viewModel.cancelOrder(3) { called = true }
        advanceUntilIdle()

        assertEquals(OrderStatus.OPEN, orderDao.statusOf(3))
        assertTrue(!called)
    }

    @Test
    fun cancelOrder_ownerMode_cancelsAndStamps() = runTest {
        build()
        ownerSession.unlock()
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))
        var statusWhenDone: OrderStatus? = null

        viewModel.cancelOrder(3) { statusWhenDone = orderDao.statusOf(3) }
        advanceUntilIdle()

        assertEquals(OrderStatus.CANCELLED, statusWhenDone)
        val order = orderDao.orders.value.single()
        assertEquals(shiftDao.getActive()!!.id, order.cancelledShiftId)
        assertNotNull(order.cancelledAt)
    }

    @Test
    fun cancelOrder_ownerMode_withoutShift_stillCancels() = runTest {
        build(withActiveShift = false)
        ownerSession.unlock()
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))

        viewModel.cancelOrder(3)
        advanceUntilIdle()

        val order = orderDao.orders.value.single()
        assertEquals(OrderStatus.CANCELLED, order.status)
        assertNull(order.cancelledShiftId)
    }
}
