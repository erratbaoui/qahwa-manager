package com.example.qahwamanager.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class QuantityFormatTest {

    @Test
    fun wholeQuantity_hasNoDecimals() {
        assertEquals("5", formatQuantity(5.0, Locale.ENGLISH))
    }

    @Test
    fun french_usesDecimalComma() {
        assertEquals("2,5", formatQuantity(2.5, Locale.FRENCH))
    }

    @Test
    fun keepsUpToTwoDecimals() {
        assertEquals("0.25", formatQuantity(0.25, Locale.ENGLISH))
        assertEquals("0.33", formatQuantity(1.0 / 3, Locale.ENGLISH))
    }

    @Test
    fun largeQuantity_isNotGrouped() {
        assertEquals("1200", formatQuantity(1200.0, Locale.ENGLISH))
    }
}
