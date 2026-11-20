package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.differenceCentimes
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeShiftDao
import com.example.qahwamanager.testing.FakeStaffDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShiftRepositoryTest {

    private val staffDao = FakeStaffDao()
    private val orderDao = FakeOrderDao()
    private val shiftDao = FakeShiftDao(orderDao, staffDao)
    // 2026-11-17T23:00:00Z
    private val elevenPm = 1_794_956_400_000L
    private var karim = 0L
    private var youssef = 0L

    @Before
    fun seedStaff() = runTest {
        karim = staffDao.insert(Staff(name = "Karim", createdAt = 0))
        youssef = staffDao.insert(Staff(name = "Youssef", createdAt = 0))
    }

    private fun TestScope.repo(start: Long = elevenPm) = ShiftRepository(shiftDao, orderDao, testClock(start))

    private fun paid(total: Long, shiftId: Long, paidAt: Long) =
        Order(tableNumber = "1", status = OrderStatus.PAID, createdAt = paidAt - 1, totalCentimes = total, openedShiftId = shiftId, paidShiftId = shiftId, paidAt = paidAt)

    @Test
    fun start_concurrentCallsCreateOneShift() = runTest {
        val repo = repo()

        val results = listOf(
            async { runCatching { repo.start(karim) } },
            async { runCatching { repo.start(youssef) } },
        ).awaitAll()

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, shiftDao.shifts.value.count { it.status == ShiftStatus.ACTIVE })
        assertEquals(1, shiftDao.shifts.value.size)
    }

    @Test
    fun activeShift_survivesNewRepositoryInstance() = runTest {
        val id = repo().start(karim)

        val fresh = repo()
        assertEquals(id, fresh.activeShiftId())
        assertEquals("Karim", fresh.observeActive().first()!!.staffName)
    }

    @Test
    fun expected_countsOrdersPaidAfterMidnightInSameShift() = runTest {
        val repo = repo()
        val shift = repo.start(karim)
        orderDao.seed(paid(800, shift, paidAt = elevenPm + 30 * 60_000L))
        orderDao.seed(paid(1200, shift, paidAt = elevenPm + 90 * 60_000L))

        assertEquals(2000L, repo.observeCollectedCentimes(shift).first())
        assertEquals(2000L, repo.observeReport(shift).first()!!.expectedCentimes)
    }

    @Test
    fun endActive_leavesOpenOrdersOutOfExpected() = runTest {
        val repo = repo()
        val shift = repo.start(karim)
        orderDao.seed(paid(800, shift, paidAt = elevenPm + 1))
        orderDao.seed(Order(id = 50, tableNumber = "2", status = OrderStatus.OPEN, createdAt = elevenPm, totalCentimes = 500, openedShiftId = shift))

        assertEquals(shift, repo.endActive())

        val report = repo.observeReport(shift).first()!!
        assertEquals(800L, report.expectedCentimes)
        assertEquals(listOf(500L), report.leftOpen.map { it.totalCentimes })
        assertEquals(listOf(50L), report.leftOpen.map { it.orderId })
        assertEquals(ShiftStatus.ENDED, report.shift.shift.status)
        assertNull(repo.activeShiftId())
    }

    @Test
    fun leftOpenOrderPaidLater_countsForTheShiftThatCollectsIt() = runTest {
        val repo = repo()
        val karimShift = repo.start(karim)
        orderDao.seed(Order(id = 50, tableNumber = "2", status = OrderStatus.OPEN, createdAt = elevenPm, totalCentimes = 500, openedShiftId = karimShift))
        repo.endActive()
        val youssefShift = repo.start(youssef)

        orderDao.markPaidIfOpen(50, youssefShift, now = elevenPm + 10)

        assertEquals(500L, repo.observeCollectedCentimes(youssefShift).first())
        val karimReport = repo.observeReport(karimShift).first()!!
        assertEquals(0L, karimReport.expectedCentimes)
        assertEquals(listOf(50L), karimReport.leftOpen.map { it.orderId })
        assertTrue(karimReport.paidOrders.isEmpty())
        assertEquals(listOf(50L), repo.observeReport(youssefShift).first()!!.paidOrders.map { it.id })
    }

    @Test
    fun takeOver_flagsOldShiftAndStartsNew() = runTest {
        val repo = repo()
        val old = repo.start(karim)
        advanceTimeBy(60_000)

        val new = repo.takeOver(youssef)

        val oldShift = repo.observeReport(old).first()!!.shift.shift
        assertEquals(ShiftStatus.ENDED, oldShift.status)
        assertTrue(oldShift.endedByTakeover)
        val active = repo.observeActive().first()
        assertNotNull(active)
        assertEquals(new, active!!.shift.id)
        assertEquals("Youssef", active.staffName)
        assertEquals(oldShift.endedAt, active.shift.startedAt)
    }

    @Test
    fun settle_snapshotsAndComputesDifference() = runTest {
        val repo = repo()
        val shift = repo.start(karim)
        orderDao.seed(paid(64_000, shift, paidAt = elevenPm + 1))
        repo.endActive()

        assertTrue(repo.settle(shift, countedCentimes = 61_000))

        val settled = repo.observeReport(shift).first()!!.shift.shift
        assertEquals(ShiftStatus.SETTLED, settled.status)
        assertEquals(64_000L, settled.expectedCentimes)
        assertEquals(61_000L, settled.countedCentimes)
        assertEquals(-3000L, settled.differenceCentimes)
        assertEquals(listOf(shift), repo.observeHistory(null).first().map { it.shift.id })
        assertTrue(repo.observeToSettle().first().isEmpty())
    }

    @Test
    fun settle_secondCallIsRefused() = runTest {
        val repo = repo()
        val shift = repo.start(karim)
        repo.endActive()
        assertTrue(repo.settle(shift, 100))

        assertFalse(repo.settle(shift, 999))
        assertEquals(100L, repo.observeReport(shift).first()!!.shift.shift.countedCentimes)
    }

    @Test
    fun settle_activeShift_isRefused() = runTest {
        val repo = repo()
        val shift = repo.start(karim)

        assertFalse(repo.settle(shift, 100))
        assertEquals(ShiftStatus.ACTIVE, repo.observeActive().first()!!.shift.status)
    }

    @Test
    fun report_settledShiftKeepsStoredExpected() = runTest {
        val repo = repo()
        val shift = repo.start(karim)
        orderDao.seed(paid(2000, shift, paidAt = elevenPm + 1))
        repo.endActive()
        repo.settle(shift, 2000)

        orderDao.seed(paid(999, shift, paidAt = elevenPm + 2))

        val report = repo.observeReport(shift).first()!!
        assertEquals(2000L, report.expectedCentimes)
        assertEquals(2, report.paidOrders.size)
    }

    @Test
    fun report_listsCancelledOrdersOfTheShiftTheyWereOpenedIn() = runTest {
        val repo = repo()
        val shift = repo.start(karim)
        orderDao.seed(Order(id = 60, tableNumber = "3", status = OrderStatus.OPEN, createdAt = elevenPm, totalCentimes = 3600, openedShiftId = shift))
        repo.endActive()
        val later = repo.start(youssef)
        orderDao.cancelIfOpen(60, later, now = elevenPm + 5)

        assertEquals(listOf(60L), repo.observeReport(shift).first()!!.cancelledOrders.map { it.id })
        assertTrue(repo.observeReport(later).first()!!.cancelledOrders.isEmpty())
    }
}
