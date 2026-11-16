package com.example.qahwamanager.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTest {

    @Test
    fun takeawayMarker_keepsTheValueAlreadyStoredInTheDatabase() {
        // Existing rows hold this exact text; changing it would turn them into "Table takeaway".
        assertEquals("takeaway", Order.TAKEAWAY)
    }

    @Test
    fun orderWithTakeawayMarker_isTakeaway() {
        assertTrue(Order(tableNumber = Order.TAKEAWAY).isTakeaway)
    }

    @Test
    fun orderWithTableNumber_isNotTakeaway() {
        assertFalse(Order(tableNumber = "4").isTakeaway)
    }
}
