package com.example.qahwamanager.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object LowStockScheduler {
    private const val REQUEST_CODE = 2002

    /**
     * Schedules a repeating bi-hourly (every 2 hours) background alarm via [AlarmManager]
     * to check inventory levels and post a restock reminder notification when items are low.
     */
    fun scheduleBiHourlyCheck(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(appContext, LowStockAlarmReceiver::class.java).apply {
            action = LowStockAlarmReceiver.ACTION_CHECK_LOW_STOCK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val intervalMs = LowStockNotifier.TWO_HOURS_MS
        val firstTriggerAt = SystemClock.elapsedRealtime() + intervalMs

        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            firstTriggerAt,
            intervalMs,
            pendingIntent,
        )
    }

    /**
     * Triggers an immediate low-stock check (respecting the 2-hour notification cooldown)
     * and ensures the bi-hourly repeating alarm is active.
     */
    fun ensureScheduledAndCheckIfDue(context: Context) {
        scheduleBiHourlyCheck(context)
        val checkIntent = Intent(context.applicationContext, LowStockAlarmReceiver::class.java)
        context.applicationContext.sendBroadcast(checkIntent)
    }
}
