package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.cloud.NoOpSyncPublisher
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.ShiftDao
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.ShiftLeftOpen
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the owner sees about one shift. [expectedCentimes] is the live sum of orders paid in
 * the shift until it's SETTLED; from then on it's the value copied onto the shift at settlement.
 */
data class ShiftReport(
    val shift: ShiftWithStaff,
    val expectedCentimes: Long,
    val paidOrders: List<Order>,
    val cancelledOrders: List<Order>,
    val leftOpen: List<ShiftLeftOpen>,
    val leftOpenOrders: List<Order> = emptyList()
)

@Singleton
class ShiftRepository @Inject constructor(
    private val shiftDao: ShiftDao,
    private val orderDao: OrderDao,
    private val clock: Clock,
    private val syncPublisher: SyncPublisher = NoOpSyncPublisher,
) {
    fun observeActive(): Flow<ShiftWithStaff?> = shiftDao.observeActive()

    /** Cash the shift has collected so far: the sum of orders marked paid in it. */
    fun observeCollectedCentimes(shiftId: Long): Flow<Long> = shiftDao.observeExpectedCentimes(shiftId)

    suspend fun activeShiftId(): Long? = shiftDao.getActive()?.id

    /** Starts a shift for [staffId]. Throws [com.example.qahwamanager.data.db.ShiftAlreadyActiveException] if one is running. */
    suspend fun start(staffId: Long): Long {
        val id = shiftDao.startShift(staffId, clock.nowMillis())
        syncPublisher.pushShift(id)
        return id
    }

    /** Ends the active shift, optionally recording the cash handed over ([countedCentimes]). Returns its id, or null if none was active. */
    suspend fun endActive(countedCentimes: Long? = null): Long? {
        val id = shiftDao.endActiveShift(clock.nowMillis(), takeover = false, countedCentimes = countedCentimes)
        if (id != null) syncPublisher.pushShift(id)
        return id
    }

    /** Ends the running shift as a takeover and starts one for [newStaffId]. */
    suspend fun takeOver(newStaffId: Long): Long {
        val previousId = shiftDao.getActive()?.id
        val newId = shiftDao.takeOver(newStaffId, clock.nowMillis())
        if (previousId != null) syncPublisher.pushShift(previousId)
        syncPublisher.pushShift(newId)
        return newId
    }

    /** Settles an ENDED shift with the cash the owner counted. False if it isn't ENDED (already settled, or still active). */
    suspend fun settle(shiftId: Long, countedCentimes: Long): Boolean {
        val settled = shiftDao.settle(shiftId, countedCentimes, clock.nowMillis())
        if (settled) syncPublisher.pushShift(shiftId)
        return settled
    }

    fun observeToSettle(): Flow<List<ShiftWithStaff>> = shiftDao.observeToSettle()

    fun observeHistory(staffId: Long?): Flow<List<ShiftWithStaff>> = shiftDao.observeSettled(staffId)

    fun observeClosedShifts(): Flow<List<ShiftWithStaff>> = shiftDao.observeClosedShifts()

    /** Records historical earnings for a waiter on a past date/shift slot. */
    suspend fun recordPastShift(
        staffId: Long,
        startedAt: Long,
        endedAt: Long,
        amountCentimes: Long,
    ): Long {
        require(amountCentimes >= 0L) { "Amount cannot be negative" }
        val id = shiftDao.recordPastShift(
            staffId = staffId,
            startedAt = startedAt,
            endedAt = endedAt,
            amountCentimes = amountCentimes,
            settledAt = clock.nowMillis(),
        )
        syncPublisher.pushShift(id)
        return id
    }

    fun observeReport(shiftId: Long): Flow<ShiftReport?> = combine(
        combine(shiftDao.observeById(shiftId), shiftDao.observeExpectedCentimes(shiftId)) { shift, expected -> shift to expected },
        orderDao.observePaidInShift(shiftId),
        orderDao.observeCancelledForShift(shiftId),
        shiftDao.observeLeftOpen(shiftId),
        orderDao.observeLeftOpenOrders(shiftId),
    ) { (shift, liveExpected), paid, cancelled, leftOpen, leftOpenOrders ->
        shift ?: return@combine null
        val expected = if (shift.shift.status == ShiftStatus.SETTLED) shift.shift.expectedCentimes ?: liveExpected else liveExpected
        ShiftReport(shift, expected, paid, cancelled, leftOpen, leftOpenOrders)
    }
}
