package com.example.qahwamanager.data.time

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class DayBoundariesTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    // 2026-11-16T00:00:00Z and neighbours, computed by hand.
    private val nov16 = 1_794_787_200_000L
    private val nov17 = nov16 + 86_400_000L
    private val nov16At2359 = nov17 - 60_000L

    @Test
    fun startOfDayTruncatesToMidnight() {
        assertEquals(nov16, startOfDay(nov16 + 13 * 3_600_000L, utc))
        assertEquals(nov16, startOfDay(nov16, utc))
    }

    @Test
    fun startOfWeekUsesGivenFirstDay() {
        // 2026-11-16 is a Monday.
        assertEquals(nov16, startOfWeek(nov16 + 2 * 86_400_000L, utc, Calendar.MONDAY))
        assertEquals(nov16 - 86_400_000L, startOfWeek(nov16 + 2 * 86_400_000L, utc, Calendar.SUNDAY))
    }

    @Test
    fun dayStartsEmitsAgainAfterMidnight() = runTest {
        val clock = object : Clock {
            override fun nowMillis() = nov16At2359 + testScheduler.currentTime
            override fun timeZone() = utc
        }

        val emitted = dayStarts(clock).take(2).toList()

        assertEquals(listOf(nov16, nov17), emitted)
    }
}
