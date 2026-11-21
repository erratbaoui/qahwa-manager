package com.example.qahwamanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MoneyTest {

    @Test
    fun parsesWholeDirhams() {
        assertEquals(1200L, parseMadToCentimes("12"))
    }

    @Test
    fun parsesDotDecimals() {
        assertEquals(1250L, parseMadToCentimes("12.5"))
        assertEquals(1205L, parseMadToCentimes("12.05"))
    }

    @Test
    fun parsesCommaDecimals() {
        assertEquals(1250L, parseMadToCentimes("12,50"))
    }

    @Test
    fun parsesValuesThatAreInexactAsDouble() {
        // 0.29 * 100 = 28.999999999999996 in floating point.
        assertEquals(29L, parseMadToCentimes("0.29"))
    }

    @Test
    fun rejectsMoreThanTwoDecimals() {
        assertNull(parseMadToCentimes("12.555"))
    }

    @Test
    fun rejectsInvalidInput() {
        assertNull(parseMadToCentimes(""))
        assertNull(parseMadToCentimes("abc"))
        assertNull(parseMadToCentimes("-5"))
    }

    @Test
    fun rejectsOverflowingInputWithoutThrowing() {
        assertNull(parseMadToCentimes("99999999999999999999"))
        assertNull(parseMadToCentimes("10000001"))
        assertEquals(1_000_000_000L, parseMadToCentimes("10000000"))
    }

    @Test
    fun convertsCentimesToExactDecimalAmount() {
        assertEquals(BigDecimal("12.50"), centimesToAmount(1250L))
        assertEquals(BigDecimal("0.05"), centimesToAmount(5L))
    }
}
