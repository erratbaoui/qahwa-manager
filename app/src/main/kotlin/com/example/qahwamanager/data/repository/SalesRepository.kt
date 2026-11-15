package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.model.Order
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SalesRepository @Inject constructor(
    private val orderDao: OrderDao
) {
    fun getTodayRevenue(): Flow<Double?> {
        val (start, end) = getTodayRange()
        return orderDao.getTotalRevenueSince(start)
    }

    fun getWeekRevenue(): Flow<Double?> {
        val start = getStartOfWeek()
        return orderDao.getTotalRevenueSince(start)
    }

    fun getAllTimeRevenue(): Flow<Double?> = orderDao.getTotalRevenueSince(0L)

    fun getPaidOrdersSince(startMs: Long): Flow<List<Order>> =
        orderDao.getPaidOrdersSince(startMs)

    fun getTodayOrders(): Flow<List<Order>> {
        val (start, end) = getTodayRange()
        return orderDao.getOrdersForDay(start, end)
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return start to cal.timeInMillis
    }

    private fun getStartOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

