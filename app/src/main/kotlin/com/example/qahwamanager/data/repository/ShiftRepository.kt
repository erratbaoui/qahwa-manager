package com.example.qahwamanager.data.repository

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
    val leftOpen: List<ShiftLeftOpen>
)

@Singleton
class ShiftRepository @Inject constructor(
    private val shiftDao: ShiftDao,
    private val orderDao: OrderDao,
    private val clock: Clock
) {
    fun observeActive(): Flow<ShiftWithStaff?> = shiftDao.observeActive()

    /** Cash the shift has collected so far: the sum of orders marked paid in it. */
    fun observeCollectedCentimes(shiftId: Long): Flow<Long> = shiftDao.observeExpectedCentimes(shiftId)

    suspend fun activeShiftId(): Long? = shiftDao.getActive()?.id

    /** Starts a shift for [staffId]. Throws [com.example.qahwamanager.data.db.ShiftAlreadyActiveException] if one is running. */
    suspend fun start(staffId: Long): Long = shiftDao.startShift(staffId, clock.nowMillis())

    /** Ends the active shift, recording its open orders as left open. Returns its id, or null if none was active. */
    suspend fun endActive(): Long? = shiftDao.endActiveShift(clock.nowMillis(), takeover = false)

    /** Ends the running shift as a takeover and starts one for [newStaffId]. */
    suspend fun takeOver(newStaffId: Long): Long = shiftDao.takeOver(newStaffId, clock.nowMillis())

    /** Settles an ENDED shift with the cash the owner counted. False if it isn't ENDED (already settled, or still active). */
    suspend fun settle(shiftId: Long, countedCentimes: Long): Boolean =
        shiftDao.settle(shiftId, countedCentimes, clock.nowMillis())

    fun observeToSettle(): Flow<List<ShiftWithStaff>> = shiftDao.observeToSettle()

    fun observeHistory(staffId: Long?): Flow<List<ShiftWithStaff>> = shiftDao.observeSettled(staffId)

    fun observeReport(shiftId: Long): Flow<ShiftReport?> = combine(
        shiftDao.observeById(shiftId),
        shiftDao.observeExpectedCentimes(shiftId),
        orderDao.observePaidInShift(shiftId),
        orderDao.observeCancelledForShift(shiftId),
        shiftDao.observeLeftOpen(shiftId),
    ) { shift, liveExpected, paid, cancelled, leftOpen ->
        shift ?: return@combine null
        val expected = if (shift.shift.status == ShiftStatus.SETTLED) shift.shift.expectedCentimes ?: liveExpected else liveExpected
        ShiftReport(shift, expected, paid, cancelled, leftOpen)
    }
}
