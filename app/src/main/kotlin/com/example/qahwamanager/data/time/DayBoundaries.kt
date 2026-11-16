package com.example.qahwamanager.data.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import java.util.TimeZone

interface Clock {
    fun nowMillis(): Long
    fun timeZone(): TimeZone = TimeZone.getDefault()
}

object SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

fun startOfDay(epochMillis: Long, zone: TimeZone): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = epochMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

fun startOfWeek(
    epochMillis: Long,
    zone: TimeZone,
    firstDayOfWeek: Int = Calendar.getInstance().firstDayOfWeek,
): Long {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = startOfDay(epochMillis, zone) }
    val daysSinceWeekStart = (cal.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    cal.add(Calendar.DAY_OF_YEAR, -daysSinceWeekStart)
    return cal.timeInMillis
}

private fun startOfNextDay(epochMillis: Long, zone: TimeZone): Long =
    Calendar.getInstance(zone).apply {
        timeInMillis = startOfDay(epochMillis, zone)
        add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis

/**
 * Emits the start of the current day now, and again each time the clock passes midnight,
 * so "today" figures roll over on a till that is left running overnight.
 */
fun dayStarts(clock: Clock): Flow<Long> = flow {
    while (true) {
        val now = clock.nowMillis()
        val zone = clock.timeZone()
        emit(startOfDay(now, zone))
        delay(startOfNextDay(now, zone) - now)
    }
}.distinctUntilChanged()
