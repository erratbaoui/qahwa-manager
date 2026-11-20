package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.qahwamanager.data.model.Shift
import com.example.qahwamanager.data.model.ShiftLeftOpen
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.ShiftWithStaff
import kotlinx.coroutines.flow.Flow

/** Thrown by [ShiftDao.startShift] when a shift is already ACTIVE. Only one can be. */
class ShiftAlreadyActiveException : IllegalStateException("A shift is already active")

/**
 * The multi-row shift rules live here as `@Transaction` methods, so they hold no matter which
 * screen (or how many taps) triggers them: one active shift, the left-open snapshot taken at the
 * moment a shift ends, and a settlement that copies the expected cash once and never again.
 */
@Dao
abstract class ShiftDao {

    @Query("SELECT * FROM shifts WHERE status = 'ACTIVE' LIMIT 1")
    abstract suspend fun getActive(): Shift?

    @Query("SELECT shifts.*, staff.name AS staffName, staff.role AS staffRole FROM shifts JOIN staff ON staff.id = shifts.staffId WHERE shifts.status = 'ACTIVE' LIMIT 1")
    abstract fun observeActive(): Flow<ShiftWithStaff?>

    @Query("SELECT shifts.*, staff.name AS staffName, staff.role AS staffRole FROM shifts JOIN staff ON staff.id = shifts.staffId WHERE shifts.id = :id")
    abstract fun observeById(id: Long): Flow<ShiftWithStaff?>

    /** Ended shifts waiting for the owner, oldest first. */
    @Query("SELECT shifts.*, staff.name AS staffName, staff.role AS staffRole FROM shifts JOIN staff ON staff.id = shifts.staffId WHERE shifts.status = 'ENDED' ORDER BY shifts.endedAt ASC")
    abstract fun observeToSettle(): Flow<List<ShiftWithStaff>>

    /** Settled shifts, newest first; [staffId] null means everyone. */
    @Query("SELECT shifts.*, staff.name AS staffName, staff.role AS staffRole FROM shifts JOIN staff ON staff.id = shifts.staffId WHERE shifts.status = 'SETTLED' AND (:staffId IS NULL OR shifts.staffId = :staffId) ORDER BY shifts.settledAt DESC")
    abstract fun observeSettled(staffId: Long?): Flow<List<ShiftWithStaff>>

    /** All ended or settled shifts, newest first, for owner analytics. */
    @Query("SELECT shifts.*, staff.name AS staffName, staff.role AS staffRole FROM shifts JOIN staff ON staff.id = shifts.staffId WHERE shifts.status IN ('ENDED', 'SETTLED') ORDER BY shifts.startedAt DESC")
    abstract fun observeClosedShifts(): Flow<List<ShiftWithStaff>>

    /** Expected cash: everything marked paid during the shift. Whoever collects the money owes it. */
    @Query("SELECT COALESCE(SUM(totalCentimes), 0) FROM orders WHERE paidShiftId = :shiftId AND status = 'PAID'")
    abstract fun observeExpectedCentimes(shiftId: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(totalCentimes), 0) FROM orders WHERE paidShiftId = :shiftId AND status = 'PAID'")
    abstract suspend fun expectedCentimes(shiftId: Long): Long

    @Query("SELECT * FROM shift_left_open WHERE shiftId = :shiftId ORDER BY orderId")
    abstract fun observeLeftOpen(shiftId: Long): Flow<List<ShiftLeftOpen>>

    /** Counts a failed owner-PIN attempt on the active shift. Returns rows changed (0 with no shift). */
    @Query("UPDATE shifts SET failedOwnerPinAttempts = failedOwnerPinAttempts + 1 WHERE status = 'ACTIVE'")
    abstract suspend fun recordFailedOwnerPinOnActive(): Int

    @Insert
    protected abstract suspend fun insertShift(shift: Shift): Long

    @Insert
    protected abstract suspend fun insertLeftOpen(rows: List<ShiftLeftOpen>)

    @Query("INSERT INTO shift_left_open (shiftId, orderId, totalCentimes) SELECT :shiftId, id, totalCentimes FROM orders WHERE status = 'OPEN'")
    protected abstract suspend fun snapshotOpenOrders(shiftId: Long)

    @Query("UPDATE shifts SET status = 'ENDED', endedAt = :now, endedByTakeover = :takeover, expectedCentimes = (SELECT COALESCE(SUM(totalCentimes), 0) FROM orders WHERE paidShiftId = :id AND status = 'PAID'), countedCentimes = :countedCentimes WHERE id = :id AND status = 'ACTIVE'")
    protected abstract suspend fun markEnded(id: Long, now: Long, takeover: Boolean, countedCentimes: Long? = null): Int

    @Query("UPDATE shifts SET status = 'SETTLED', expectedCentimes = :expected, countedCentimes = :counted, settledAt = :now WHERE id = :id AND status = 'ENDED'")
    protected abstract suspend fun markSettled(id: Long, expected: Long, counted: Long, now: Long): Int

    /** Starts a shift for [staffId]. Throws [ShiftAlreadyActiveException] if one is already running. */
    @Transaction
    open suspend fun startShift(staffId: Long, now: Long): Long {
        if (getActive() != null) throw ShiftAlreadyActiveException()
        return insertShift(Shift(staffId = staffId, status = ShiftStatus.ACTIVE, startedAt = now))
    }

    /**
     * Ends the active shift: every OPEN order is snapshotted into `shift_left_open` with its total
     * right now, then the shift is marked ENDED. Returns the shift id, or null with no active shift.
     */
    @Transaction
    open suspend fun endActiveShift(now: Long, takeover: Boolean, countedCentimes: Long? = null): Long? {
        val active = getActive() ?: return null
        snapshotOpenOrders(active.id)
        markEnded(active.id, now, takeover, countedCentimes)
        return active.id
    }

    /** Ends whoever's shift is running (flagged as a takeover) and starts one for [newStaffId]. */
    @Transaction
    open suspend fun takeOver(newStaffId: Long, now: Long): Long {
        endActiveShift(now, takeover = true)
        return startShift(newStaffId, now)
    }

    /**
     * Settles an ENDED shift: copies the expected cash onto the row with [countedCentimes] and
     * [now], and marks it SETTLED. Returns false (and changes nothing) unless the shift is ENDED.
     */
    @Transaction
    open suspend fun settle(shiftId: Long, countedCentimes: Long, now: Long): Boolean {
        val expected = expectedCentimes(shiftId)
        return markSettled(shiftId, expected, countedCentimes, now) == 1
    }

    /** Records a backdated, already-settled shift for historical earnings entry. */
    open suspend fun recordPastShift(
        staffId: Long,
        startedAt: Long,
        endedAt: Long,
        amountCentimes: Long,
        settledAt: Long,
    ): Long = insertShift(
        Shift(
            staffId = staffId,
            status = ShiftStatus.SETTLED,
            startedAt = startedAt,
            endedAt = endedAt,
            settledAt = settledAt,
            expectedCentimes = amountCentimes,
            countedCentimes = amountCentimes,
        )
    )
}
