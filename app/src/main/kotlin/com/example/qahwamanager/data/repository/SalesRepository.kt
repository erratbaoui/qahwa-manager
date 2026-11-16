package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.data.time.dayStarts
import com.example.qahwamanager.data.time.startOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepository @Inject constructor(
    private val orderDao: OrderDao,
    private val clock: Clock,
) {
    /** Start of the current day; re-emits at every midnight. */
    fun dayStarts(): Flow<Long> = dayStarts(clock)

    fun startOfWeek(dayStart: Long): Long = startOfWeek(dayStart, clock.timeZone())

    /** Paid revenue for the current day in centimes; rolls over at midnight. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayRevenue(): Flow<Long> =
        dayStarts().flatMapLatest { start -> orderDao.getRevenueSince(start) }

    fun getPaidOrdersSince(startMs: Long): Flow<List<Order>> =
        orderDao.getPaidOrdersSince(startMs)
}
