package com.example.qahwamanager.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.StaffRole
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.data.repository.StaffRepository
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

enum class DashboardPeriod { TODAY, THIS_WEEK, THIS_MONTH, ALL_TIME }

enum class ShiftSlot { MORNING, AFTERNOON }

data class StaffPerformance(
    val staffId: Long,
    val staffName: String,
    val totalCentimes: Long,
    val shiftCount: Int,
) {
    val averageCentimes: Long
        get() = if (shiftCount == 0) 0L else totalCentimes / shiftCount
}

data class SlotStats(
    val totalCentimes: Long = 0L,
    val shiftCount: Int = 0,
) {
    val averageCentimes: Long
        get() = if (shiftCount == 0) 0L else totalCentimes / shiftCount
}

data class DailyEarning(
    val dayStartMillis: Long,
    val totalCentimes: Long,
)

data class OwnerDashboardUiState(
    val period: DashboardPeriod = DashboardPeriod.THIS_WEEK,
    val totalEarningsCentimes: Long = 0L,
    val totalShifts: Int = 0,
    val morningStats: SlotStats = SlotStats(),
    val afternoonStats: SlotStats = SlotStats(),
    val topStaff: List<StaffPerformance> = emptyList(),
    val dailyEarnings: List<DailyEarning> = emptyList(),
) {
    val averagePerShiftCentimes: Long
        get() = if (totalShifts == 0) 0L else totalEarningsCentimes / totalShifts
}

/**
 * Classifies a shift into [ShiftSlot.MORNING] (05:00–13:59) or [ShiftSlot.AFTERNOON] (14:00–04:59)
 * using the shift's start time in [timeZone].
 */
fun classifyShiftSlot(startedAtMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): ShiftSlot {
    val cal = Calendar.getInstance(timeZone).apply { timeInMillis = startedAtMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return if (hour in 5..13) ShiftSlot.MORNING else ShiftSlot.AFTERNOON
}

/**
 * Earnings attributed to a closed shift: prefers the cash handed over ([countedCentimes]) for
 * traditional ticket-bowl workflows, falling back to POS [expectedCentimes] if no cash was entered.
 */
fun ShiftWithStaff.earningsCentimes(): Long =
    shift.countedCentimes ?: shift.expectedCentimes ?: 0L

fun computeOwnerDashboardState(
    closedShifts: List<ShiftWithStaff>,
    period: DashboardPeriod,
    nowMillis: Long,
    timeZone: TimeZone = TimeZone.getDefault(),
): OwnerDashboardUiState {
    val minStart = when (period) {
        DashboardPeriod.TODAY -> startOfDay(nowMillis, timeZone)
        DashboardPeriod.THIS_WEEK -> startOfWeek(nowMillis, timeZone)
        DashboardPeriod.THIS_MONTH -> startOfMonth(nowMillis, timeZone)
        DashboardPeriod.ALL_TIME -> Long.MIN_VALUE
    }

    val filtered = closedShifts.filter {
        it.staffRole == StaffRole.WAITER && it.shift.startedAt >= minStart
    }
    if (filtered.isEmpty()) return OwnerDashboardUiState(period = period)

    var totalEarnings = 0L
    var morningTotal = 0L
    var morningCount = 0
    var afternoonTotal = 0L
    var afternoonCount = 0

    val byStaff = linkedMapOf<Long, Triple<String, Long, Int>>()
    val byDay = sortedMapOf<Long, Long>()

    for (entry in filtered) {
        val earnings = entry.earningsCentimes()
        totalEarnings += earnings

        when (classifyShiftSlot(entry.shift.startedAt, timeZone)) {
            ShiftSlot.MORNING -> {
                morningTotal += earnings
                morningCount++
            }
            ShiftSlot.AFTERNOON -> {
                afternoonTotal += earnings
                afternoonCount++
            }
        }

        val currentStaff = byStaff[entry.shift.staffId]
        byStaff[entry.shift.staffId] = if (currentStaff == null) {
            Triple(entry.staffName, earnings, 1)
        } else {
            Triple(currentStaff.first, currentStaff.second + earnings, currentStaff.third + 1)
        }

        val dayStart = startOfDay(entry.shift.startedAt, timeZone)
        byDay[dayStart] = (byDay[dayStart] ?: 0L) + earnings
    }

    val topStaff = byStaff.entries
        .map { (id, triple) ->
            StaffPerformance(
                staffId = id,
                staffName = triple.first,
                totalCentimes = triple.second,
                shiftCount = triple.third,
            )
        }
        .sortedWith(compareByDescending<StaffPerformance> { it.totalCentimes }.thenBy { it.staffName })

    val dailyEarnings = byDay.entries
        .toList()
        .takeLast(7)
        .map { DailyEarning(dayStartMillis = it.key, totalCentimes = it.value) }

    return OwnerDashboardUiState(
        period = period,
        totalEarningsCentimes = totalEarnings,
        totalShifts = filtered.size,
        morningStats = SlotStats(totalCentimes = morningTotal, shiftCount = morningCount),
        afternoonStats = SlotStats(totalCentimes = afternoonTotal, shiftCount = afternoonCount),
        topStaff = topStaff,
        dailyEarnings = dailyEarnings,
    )
}

private fun startOfDay(nowMillis: Long, timeZone: TimeZone): Long {
    val cal = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun startOfWeek(nowMillis: Long, timeZone: TimeZone): Long {
    val cal = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis > nowMillis) add(Calendar.WEEK_OF_YEAR, -1)
    }
    return cal.timeInMillis
}

private fun startOfMonth(nowMillis: Long, timeZone: TimeZone): Long {
    val cal = Calendar.getInstance(timeZone).apply {
        timeInMillis = nowMillis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

/**
 * Computes representative local shift start and end timestamps for a backdated earnings entry
 * on the calendar date identified by [utcDateMillis] (as returned by Material 3 DatePicker).
 */
fun pastShiftBounds(
    utcDateMillis: Long,
    slot: ShiftSlot,
    timeZone: TimeZone = TimeZone.getDefault(),
): Pair<Long, Long> {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    val year = utcCal.get(Calendar.YEAR)
    val month = utcCal.get(Calendar.MONTH)
    val day = utcCal.get(Calendar.DAY_OF_MONTH)

    val (startHour, endHour) = when (slot) {
        ShiftSlot.MORNING -> 9 to 14
        ShiftSlot.AFTERNOON -> 15 to 22
    }
    val startCal = Calendar.getInstance(timeZone).apply {
        set(year, month, day, startHour, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance(timeZone).apply {
        set(year, month, day, endHour, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return startCal.timeInMillis to endCal.timeInMillis
}

@HiltViewModel
class OwnerDashboardViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    staffRepository: StaffRepository,
    private val clock: Clock,
    private val ownerSession: OwnerSession,
) : ViewModel() {

    private val _period = MutableStateFlow(DashboardPeriod.THIS_WEEK)
    val period: StateFlow<DashboardPeriod> = _period.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    val waiters: StateFlow<List<Staff>> = staffRepository.observeAll()
        .map { list ->
            list.filter { it.role == StaffRole.WAITER }
                .sortedWith(compareByDescending<Staff> { it.isActive }.thenBy { it.name.lowercase() })
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val uiState: StateFlow<OwnerDashboardUiState> = combine(
        shiftRepository.observeClosedShifts(),
        _period,
    ) { shifts, selectedPeriod ->
        computeOwnerDashboardState(shifts, selectedPeriod, clock.nowMillis())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OwnerDashboardUiState(),
    )

    fun selectPeriod(period: DashboardPeriod) {
        _period.value = period
    }

    fun addPastEarnings(
        staffId: Long,
        utcDateMillis: Long,
        slot: ShiftSlot,
        amountCentimes: Long,
        onDone: () -> Unit = {},
    ) {
        if (!ownerSession.isOwnerMode.value || _isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                val (startedAt, endedAt) = pastShiftBounds(utcDateMillis, slot)
                shiftRepository.recordPastShift(
                    staffId = staffId,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    amountCentimes = amountCentimes,
                )
                onDone()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
