package com.example.qahwamanager.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.Staff
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShiftDaoTest {

    private lateinit var db: QahwaDatabase
    private lateinit var shiftDao: ShiftDao
    private lateinit var orderDao: OrderDao
    private var karim = 0L
    private var youssef = 0L

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QahwaDatabase::class.java,
        ).build()
        shiftDao = db.shiftDao()
        orderDao = db.orderDao()
        karim = db.staffDao().insert(Staff(name = "Karim", createdAt = 1))
        youssef = db.staffDao().insert(Staff(name = "Youssef", createdAt = 2))
    }

    @After
    fun tearDown() = db.close()

    private fun order(status: OrderStatus, total: Long, openedShiftId: Long? = null, paidShiftId: Long? = null) =
        Order(tableNumber = "1", status = status, createdAt = 1000, totalCentimes = total,
            openedShiftId = openedShiftId, paidShiftId = paidShiftId)

    @Test
    fun startShift_whileActive_throws() = runTest {
        shiftDao.startShift(karim, now = 1000)

        val result = runCatching { shiftDao.startShift(youssef, now = 2000) }

        assertTrue(result.exceptionOrNull() is ShiftAlreadyActiveException)
        assertEquals(karim, shiftDao.getActive()!!.staffId)
    }

    @Test
    fun endActiveShift_snapshotsOpenOrders() = runTest {
        val shiftId = shiftDao.startShift(karim, now = 1000)
        val open1 = orderDao.insertOrder(order(OrderStatus.OPEN, 500, openedShiftId = shiftId))
        val open2 = orderDao.insertOrder(order(OrderStatus.OPEN, 700, openedShiftId = shiftId))
        orderDao.insertOrder(order(OrderStatus.PAID, 900, openedShiftId = shiftId, paidShiftId = shiftId))

        val ended = shiftDao.endActiveShift(now = 5000, takeover = false)

        assertEquals(shiftId, ended)
        val leftOpen = shiftDao.observeLeftOpen(shiftId).first()
        assertEquals(mapOf(open1 to 500L, open2 to 700L), leftOpen.associate { it.orderId to it.totalCentimes })
        assertEquals(listOf(open1, open2), orderDao.observeLeftOpenOrders(shiftId).first().map { it.id }.sorted())
        val shift = shiftDao.observeById(shiftId).first()!!.shift
        assertEquals(ShiftStatus.ENDED, shift.status)
        assertEquals(5000L, shift.endedAt)
        assertFalse(shift.endedByTakeover)
        assertEquals(900L, shift.expectedCentimes)
        assertNull(shiftDao.getActive())
    }

    @Test
    fun endActiveShift_withNoActiveShift_returnsNull() = runTest {
        assertNull(shiftDao.endActiveShift(now = 5000, takeover = false))
    }

    @Test
    fun expectedCentimes_sumsOnlyOrdersPaidInThatShift() = runTest {
        val shift1 = shiftDao.startShift(karim, now = 1000)
        shiftDao.endActiveShift(now = 2000, takeover = false)
        val shift2 = shiftDao.startShift(youssef, now = 3000)
        orderDao.insertOrder(order(OrderStatus.PAID, 800, openedShiftId = shift1, paidShiftId = shift1))
        orderDao.insertOrder(order(OrderStatus.PAID, 1200, openedShiftId = shift1, paidShiftId = shift1))
        orderDao.insertOrder(order(OrderStatus.PAID, 500, openedShiftId = shift2, paidShiftId = shift2))
        orderDao.insertOrder(order(OrderStatus.CANCELLED, 300, openedShiftId = shift1))

        assertEquals(2000L, shiftDao.expectedCentimes(shift1))
        assertEquals(2000L, shiftDao.observeExpectedCentimes(shift1).first())
        assertEquals(500L, shiftDao.expectedCentimes(shift2))
    }

    @Test
    fun settle_copiesExpectedAndRefusesSecondCall() = runTest {
        val shiftId = shiftDao.startShift(karim, now = 1000)
        orderDao.insertOrder(order(OrderStatus.PAID, 2000, openedShiftId = shiftId, paidShiftId = shiftId))
        shiftDao.endActiveShift(now = 2000, takeover = false)

        assertTrue(shiftDao.settle(shiftId, countedCentimes = 1700, now = 3000))

        val settled = shiftDao.observeById(shiftId).first()!!.shift
        assertEquals(ShiftStatus.SETTLED, settled.status)
        assertEquals(2000L, settled.expectedCentimes)
        assertEquals(1700L, settled.countedCentimes)
        assertEquals(3000L, settled.settledAt)

        orderDao.insertOrder(order(OrderStatus.PAID, 999, paidShiftId = shiftId))
        assertFalse(shiftDao.settle(shiftId, countedCentimes = 1, now = 4000))
        assertEquals(2000L, shiftDao.observeById(shiftId).first()!!.shift.expectedCentimes)
    }

    @Test
    fun settle_activeShift_isRefused() = runTest {
        val shiftId = shiftDao.startShift(karim, now = 1000)

        assertFalse(shiftDao.settle(shiftId, countedCentimes = 0, now = 2000))
        assertEquals(ShiftStatus.ACTIVE, shiftDao.getActive()!!.status)
    }

    @Test
    fun takeOver_endsOldShiftFlaggedAndStartsNew() = runTest {
        val old = shiftDao.startShift(karim, now = 1000)
        orderDao.insertOrder(order(OrderStatus.OPEN, 400, openedShiftId = old))

        val new = shiftDao.takeOver(youssef, now = 5000)

        val oldShift = shiftDao.observeById(old).first()!!
        assertEquals(ShiftStatus.ENDED, oldShift.shift.status)
        assertTrue(oldShift.shift.endedByTakeover)
        assertEquals(5000L, oldShift.shift.endedAt)
        assertEquals("Karim", oldShift.staffName)
        assertEquals(1, shiftDao.observeLeftOpen(old).first().size)
        val active = shiftDao.observeActive().first()
        assertNotNull(active)
        assertEquals(new, active!!.shift.id)
        assertEquals(youssef, active.shift.staffId)
        assertEquals("Youssef", active.staffName)
        assertEquals(5000L, active.shift.startedAt)
    }

    @Test
    fun observeToSettle_listsEndedOldestFirst_andSettledNewestFirst() = runTest {
        val a = shiftDao.startShift(karim, now = 1000); shiftDao.endActiveShift(now = 2000, takeover = false)
        val b = shiftDao.startShift(youssef, now = 3000); shiftDao.endActiveShift(now = 4000, takeover = false)
        val c = shiftDao.startShift(karim, now = 5000); shiftDao.endActiveShift(now = 6000, takeover = false)
        shiftDao.settle(a, 0, now = 7000)
        shiftDao.settle(b, 0, now = 8000)

        assertEquals(listOf(c), shiftDao.observeToSettle().first().map { it.shift.id })
        assertEquals(listOf(b, a), shiftDao.observeSettled(null).first().map { it.shift.id })
        assertEquals(listOf(a), shiftDao.observeSettled(karim).first().map { it.shift.id })
    }

    @Test
    fun recordFailedOwnerPinOnActive_countsOnTheActiveShiftOnly() = runTest {
        assertEquals(0, shiftDao.recordFailedOwnerPinOnActive())
        val shiftId = shiftDao.startShift(karim, now = 1000)

        assertEquals(1, shiftDao.recordFailedOwnerPinOnActive())
        assertEquals(1, shiftDao.recordFailedOwnerPinOnActive())

        assertEquals(2, shiftDao.observeById(shiftId).first()!!.shift.failedOwnerPinAttempts)
    }
}
