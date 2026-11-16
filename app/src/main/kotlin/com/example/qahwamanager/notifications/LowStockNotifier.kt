package com.example.qahwamanager.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.qahwamanager.MainActivity
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.ui.common.AppLocaleManager
import com.example.qahwamanager.util.formatQuantity
import java.util.Locale

object LowStockNotifier {
    const val CHANNEL_ID = "low_stock_restock_channel"
    const val NOTIFICATION_ID = 2001
    const val TWO_HOURS_MS = 2 * 60 * 60 * 1000L

    private const val PREFS_NAME = "qahwa_low_stock_notification_prefs"
    private const val KEY_LAST_NOTIFIED_MS = "last_notified_timestamp_ms"

    /**
     * Formats the restock notification summary for a non-empty list of low-stock [items].
     */
    fun formatItemSummary(items: List<InventoryItem>, locale: Locale): String {
        return items.joinToString(", ") { item ->
            "${item.name} (${formatQuantity(item.quantity, locale)} ${item.unit})"
        }
    }

    /**
     * Returns true if at least [intervalMs] have passed since [lastNotifiedMs].
     */
    fun isDueForNotification(
        nowMs: Long,
        lastNotifiedMs: Long,
        intervalMs: Long = TWO_HOURS_MS,
    ): Boolean = lastNotifiedMs <= 0L || (nowMs - lastNotifiedMs) >= intervalMs

    fun notifyIfLowStock(
        context: Context,
        lowStockItems: List<InventoryItem>,
        force: Boolean = false,
        nowMs: Long = System.currentTimeMillis(),
    ): Boolean {
        if (lowStockItems.isEmpty()) return false

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getLong(KEY_LAST_NOTIFIED_MS, 0L)
        if (!force && !isDueForNotification(nowMs, lastNotified)) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return false
        }

        val localizedContext = createLocalizedContext(context)
        val locale = Locale.forLanguageTag(AppLocaleManager.getSavedLanguage(context))
        ensureChannel(localizedContext)

        val summary = formatItemSummary(lowStockItems, locale)
        val title = localizedContext.getString(R.string.low_stock_notification_title)
        val body = localizedContext.getString(
            R.string.low_stock_notification_body,
            lowStockItems.size,
            summary,
        )

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(localizedContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_qahwa_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        prefs.edit().putLong(KEY_LAST_NOTIFIED_MS, nowMs).apply()
        return true
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.low_stock_notification_channel)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createLocalizedContext(context: Context): Context {
        val lang = AppLocaleManager.getSavedLanguage(context)
        val locale = Locale.forLanguageTag(lang)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
