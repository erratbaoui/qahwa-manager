package com.example.qahwamanager.ui.orders

import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.repository.MenuRepository
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.testing.FakeMenuItemDao
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeOrderItemDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val tea = MenuItem(
        id = 7, name = "Mint tea", nameAr = "أتاي", nameFr = "Thé à la menthe",
        category = "tea", priceCentimes = 850,
    )
    private lateinit var orderDao: FakeOrderDao
    private lateinit var viewModel: OrdersViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        orderDao = FakeOrderDao()
        viewModel = OrdersViewModel(
            OrderRepository(orderDao, FakeOrderItemDao(orderDao)),
            MenuRepository(FakeMenuItemDao(listOf(tea))),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun placeOrder_ignoresASecondTapWhileTheFirstIsSaving() = runTest {
        viewModel.addToCart(tea)

        viewModel.placeOrder("4") {}
        viewModel.placeOrder("4") {}
        advanceUntilIdle()

        assertEquals(1, orderDao.orders.value.size)
    }

    @Test
    fun placeOrder_storesCentimesAndLocalizedNameSnapshots() = runTest {
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
    fun markAsPaid_reportsDoneOnlyAfterTheStatusIsSaved() = runTest {
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))
        var statusWhenDone: OrderStatus? = null

        viewModel.markAsPaid(3) { statusWhenDone = orderDao.statusOf(3) }
        advanceUntilIdle()

        assertEquals(OrderStatus.PAID, statusWhenDone)
    }

    @Test
    fun cancelOrder_reportsDoneOnlyAfterTheStatusIsSaved() = runTest {
        orderDao.seed(Order(id = 3, tableNumber = "2", totalCentimes = 850))
        var statusWhenDone: OrderStatus? = null

        viewModel.cancelOrder(3) { statusWhenDone = orderDao.statusOf(3) }
        advanceUntilIdle()

        assertEquals(OrderStatus.CANCELLED, statusWhenDone)
    }
}
