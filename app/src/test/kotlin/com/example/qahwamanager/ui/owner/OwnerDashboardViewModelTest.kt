package com.example.qahwamanager.ui.owner

import com.example.qahwamanager.data.model.Shift
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.model.StaffRole
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.data.repository.StaffRepository
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.testing.FakeOrderDao
import com.example.qahwamanager.testing.FakeShiftDao
import com.example.qahwamanager.testing.FakeStaffDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class OwnerDashboardViewModelTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int): Long {
        return Calendar.getInstance(utc).apply {
            set(year, month - 1, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun classifyShiftSlot_distinguishesMorningAndAfternoon() {
        assertEquals(ShiftSlot.MORNING, classifyShiftSlot(utcMillis(2026, 11, 20, 8), utc))
        assertEquals(ShiftSlot.MORNING, classifyShiftSlot(utcMillis(2026, 11, 20, 13), utc))
        assertEquals(ShiftSlot.AFTERNOON, classifyShiftSlot(utcMillis(2026, 11, 20, 14), utc))
        assertEquals(ShiftSlot.AFTERNOON, classifyShiftSlot(utcMillis(2026, 11, 20, 21), utc))
    }

    @Test
    fun computeOwnerDashboardState_ranksTopStaffAndComparesMorningVsAfternoon() {
        val now = utcMillis(2026, 11, 20, 20)
        val shifts = listOf(
            // Morning shift by Karim: 400.00 MAD handed over (ticket bowl)
            ShiftWithStaff(
                shift = Shift(id = 1, staffId = 10, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 19, 8), countedCentimes = 40_000L),
                staffName = "Karim",
            ),
            // Afternoon shift by Youssef: 650.00 MAD handed over
            ShiftWithStaff(
                shift = Shift(id = 2, staffId = 20, status = ShiftStatus.SETTLED, startedAt = utcMillis(2026, 11, 19, 15), expectedCentimes = 60_000L, countedCentimes = 65_000L),
                staffName = "Youssef",
            ),
            // Morning shift by Karim: 350.00 MAD handed over
            ShiftWithStaff(
                shift = Shift(id = 3, staffId = 10, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 20, 9), countedCentimes = 35_000L),
                staffName = "Karim",
            ),
        )

        val state = computeOwnerDashboardState(shifts, DashboardPeriod.THIS_WEEK, now, utc)

        assertEquals(140_000L, state.totalEarningsCentimes)
        assertEquals(3, state.totalShifts)
        assertEquals(75_000L, state.morningStats.totalCentimes)
        assertEquals(2, state.morningStats.shiftCount)
        assertEquals(37_500L, state.morningStats.averageCentimes)
        assertEquals(65_000L, state.afternoonStats.totalCentimes)
        assertEquals(1, state.afternoonStats.shiftCount)
        assertEquals(65_000L, state.afternoonStats.averageCentimes)

        // Karim earned 750 MAD total across 2 shifts; Youssef earned 650 MAD across 1 shift
        assertEquals(listOf("Karim", "Youssef"), state.topStaff.map { it.staffName })
        assertEquals(75_000L, state.topStaff[0].totalCentimes)
        assertEquals(2, state.topStaff[0].shiftCount)
        assertEquals(65_000L, state.topStaff[1].totalCentimes)

        // Two distinct days in daily earnings chart
        assertEquals(2, state.dailyEarnings.size)
        assertEquals(105_000L, state.dailyEarnings[0].totalCentimes)
        assertEquals(35_000L, state.dailyEarnings[1].totalCentimes)
    }

    @Test
    fun computeOwnerDashboardState_todayFilterExcludesYesterday() {
        val now = utcMillis(2026, 11, 20, 18)
        val shifts = listOf(
            ShiftWithStaff(
                shift = Shift(id = 1, staffId = 10, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 19, 10), countedCentimes = 50_000L),
                staffName = "Karim",
            ),
            ShiftWithStaff(
                shift = Shift(id = 2, staffId = 20, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 20, 10), countedCentimes = 25_000L),
                staffName = "Youssef",
            ),
        )

        val todayState = computeOwnerDashboardState(shifts, DashboardPeriod.TODAY, now, utc)

        assertEquals(25_000L, todayState.totalEarningsCentimes)
        assertEquals(1, todayState.totalShifts)
        assertEquals(listOf("Youssef"), todayState.topStaff.map { it.staffName })
    }

    @Test
    fun computeOwnerDashboardState_excludesNonWaiterShifts() {
        val now = utcMillis(2026, 11, 20, 20)
        val shifts = listOf(
            ShiftWithStaff(
                shift = Shift(id = 1, staffId = 1, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 20, 9), countedCentimes = 30_000L),
                staffName = "Karim",
                staffRole = StaffRole.WAITER,
            ),
            ShiftWithStaff(
                shift = Shift(id = 2, staffId = 2, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 20, 9), countedCentimes = 99_000L),
                staffName = "Boss",
                staffRole = StaffRole.OWNER,
            ),
            ShiftWithStaff(
                shift = Shift(id = 3, staffId = 3, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 20, 15), countedCentimes = 50_000L),
                staffName = "Omar",
                staffRole = StaffRole.BARMAN,
            ),
            ShiftWithStaff(
                shift = Shift(id = 4, staffId = 4, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 20, 16), countedCentimes = 20_000L),
                staffName = "Hassan",
                staffRole = StaffRole.SECURITY,
            ),
            ShiftWithStaff(
                shift = Shift(id = 5, staffId = 5, status = ShiftStatus.ENDED, startedAt = utcMillis(2026, 11, 20, 17), countedCentimes = 10_000L),
                staffName = "Fatima",
                staffRole = StaffRole.CLEANING,
            ),
        )

        val state = computeOwnerDashboardState(shifts, DashboardPeriod.ALL_TIME, now, utc)

        assertEquals(30_000L, state.totalEarningsCentimes)
        assertEquals(1, state.totalShifts)
        assertEquals(listOf("Karim"), state.topStaff.map { it.staffName })
    }

    @Test
    fun pastShiftBounds_mapsMorningAndAfternoonSlotsOnChosenDate() {
        val chosenDateUtc = utcMillis(2026, 11, 15, 0)
        val (morningStart, morningEnd) = pastShiftBounds(chosenDateUtc, ShiftSlot.MORNING, utc)
        val (afternoonStart, afternoonEnd) = pastShiftBounds(chosenDateUtc, ShiftSlot.AFTERNOON, utc)

        assertEquals(ShiftSlot.MORNING, classifyShiftSlot(morningStart, utc))
        assertEquals(utcMillis(2026, 11, 15, 9), morningStart)
        assertEquals(utcMillis(2026, 11, 15, 14), morningEnd)

        assertEquals(ShiftSlot.AFTERNOON, classifyShiftSlot(afternoonStart, utc))
        assertEquals(utcMillis(2026, 11, 15, 15), afternoonStart)
        assertEquals(utcMillis(2026, 11, 15, 22), afternoonEnd)
    }

    @Test
    fun recordPastShift_persistsSettledBackdatedWaiterEarnings() = runTest {
        val now = utcMillis(2026, 11, 20, 18)
        val clock = object : Clock {
            override fun nowMillis(): Long = now
        }
        val staffDao = FakeStaffDao()
        val orderDao = FakeOrderDao()
        val shiftDao = FakeShiftDao(orderDao, staffDao)
        val staffRepo = StaffRepository(staffDao, clock)
        val shiftRepo = ShiftRepository(shiftDao, orderDao, clock)

        val waiterId = staffRepo.addStaff("Karim", StaffRole.WAITER)
        val (startedAt, endedAt) = pastShiftBounds(utcMillis(2026, 11, 14, 0), ShiftSlot.AFTERNOON, utc)

        shiftRepo.recordPastShift(
            staffId = waiterId,
            startedAt = startedAt,
            endedAt = endedAt,
            amountCentimes = 45_000L,
        )

        val closed = shiftRepo.observeClosedShifts().first()
        assertEquals(1, closed.size)
        val entry = closed.first()
        assertEquals(ShiftStatus.SETTLED, entry.shift.status)
        assertEquals(StaffRole.WAITER, entry.staffRole)
        assertEquals(45_000L, entry.earningsCentimes())
        assertEquals(startedAt, entry.shift.startedAt)
        assertEquals(endedAt, entry.shift.endedAt)
    }
}
