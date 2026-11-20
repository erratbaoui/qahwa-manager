package com.example.qahwamanager.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderDaoTest {

    private lateinit var db: QahwaDatabase
    private lateinit var dao: OrderDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QahwaDatabase::class.java,
        ).build()
        dao = db.orderDao()
    }

    @After
    fun tearDown() = db.close()

    private fun item(id: Long = 0, priceCentimes: Long = 800) = OrderItem(
        id = id, orderId = 0, menuItemId = 1, menuItemName = "Tea", quantity = 1, unitPriceCentimes = priceCentimes,
    )

    @Test
    fun insertOrderWithItems_storesItemsUnderTheNewOrder() = runTest {
        val orderId = dao.insertOrderWithItems(Order(tableNumber = "3", totalCentimes = 1600), listOf(item(), item()))

        val items = db.orderItemDao().getItemsForOrder(orderId).first()
        assertEquals(2, items.size)
        assertTrue(items.all { it.orderId == orderId })
    }

    @Test
    fun insertOrderWithItems_leavesNoOrderBehindWhenItemsFail() = runTest {
        // Two items with the same primary key make the second insert fail.
        val result = runCatching {
            dao.insertOrderWithItems(Order(tableNumber = "3", totalCentimes = 1600), listOf(item(id = 5), item(id = 5)))
        }

        assertTrue(result.isFailure)
        assertEquals(emptyList<Order>(), dao.getAllOrders().first())
    }

    @Test
    fun updateStatusIfOpen_doesNotChangeAClosedOrder() = runTest {
        val orderId = dao.insertOrderWithItems(Order(tableNumber = "3", totalCentimes = 800), listOf(item()))

        assertEquals(1, dao.updateStatusIfOpen(orderId, OrderStatus.CANCELLED))
        assertEquals(0, dao.updateStatusIfOpen(orderId, OrderStatus.PAID))

        assertEquals(OrderStatus.CANCELLED, dao.observeById(orderId).first()?.status)
        assertEquals(0L, dao.getRevenueSince(0).first())
    }

    @Test
    fun cancelledForShift_fallsBackToCancelledShiftId() = runTest {
        val legacy = dao.insertOrder(Order(tableNumber = "1", status = OrderStatus.OPEN, createdAt = 100, totalCentimes = 300))
        val opened = dao.insertOrder(Order(tableNumber = "2", status = OrderStatus.OPEN, createdAt = 200, totalCentimes = 400, openedShiftId = 2))
        val other = dao.insertOrder(Order(tableNumber = "3", status = OrderStatus.OPEN, createdAt = 300, totalCentimes = 500, openedShiftId = 3))

        assertEquals(1, dao.cancelIfOpen(legacy, shiftId = 3, now = 900))
        assertEquals(1, dao.cancelIfOpen(opened, shiftId = 3, now = 901))
        assertEquals(1, dao.cancelIfOpen(other, shiftId = 3, now = 902))

        assertEquals(listOf(legacy, other), dao.observeCancelledForShift(3).first().map { it.id }.sorted())
        assertEquals(listOf(opened), dao.observeCancelledForShift(2).first().map { it.id })
    }

    @Test
    fun getRevenueSince_sumsPaidOrdersFromStart() = runTest {
        dao.insertOrder(Order(tableNumber = "1", status = OrderStatus.PAID, createdAt = 500, totalCentimes = 999))
        dao.insertOrder(Order(tableNumber = "2", status = OrderStatus.PAID, createdAt = 1500, totalCentimes = 1250))
        dao.insertOrder(Order(tableNumber = "3", status = OrderStatus.OPEN, createdAt = 1500, totalCentimes = 700))

        assertEquals(1250L, dao.getRevenueSince(1000).first())
    }
}
