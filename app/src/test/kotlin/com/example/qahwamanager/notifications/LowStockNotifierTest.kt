package com.example.qahwamanager.notifications

import com.example.qahwamanager.data.model.InventoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LowStockNotifierTest {

    @Test
    fun isDueForNotification_firstTime_returnsTrue() {
        assertTrue(LowStockNotifier.isDueForNotification(nowMs = 10_000L, lastNotifiedMs = 0L))
    }

    @Test
    fun isDueForNotification_beforeTwoHours_returnsFalse() {
        val last = 1_000_000L
        val oneHourLater = last + 60 * 60 * 1000L
        assertFalse(LowStockNotifier.isDueForNotification(nowMs = oneHourLater, lastNotifiedMs = last))
    }

    @Test
    fun isDueForNotification_afterTwoHours_returnsTrue() {
        val last = 1_000_000L
        val twoHoursLater = last + LowStockNotifier.TWO_HOURS_MS
        assertTrue(LowStockNotifier.isDueForNotification(nowMs = twoHoursLater, lastNotifiedMs = last))
    }

    @Test
    fun formatItemSummary_listsLowStockItemsAndQuantities() {
        val items = listOf(
            InventoryItem(id = 1, name = "Mint", quantity = 0.5, unit = "kg", lowStockThreshold = 1.0),
            InventoryItem(id = 2, name = "Milk", quantity = 2.0, unit = "L", lowStockThreshold = 5.0),
        )
        val summary = LowStockNotifier.formatItemSummary(items, Locale.ENGLISH)
        assertEquals("Mint (0.5 kg), Milk (2 L)", summary)
    }
}
