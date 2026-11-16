package com.example.qahwamanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalInputTest {

    @Test
    fun acceptsDotAsDecimalSeparator() {
        assertEquals(2.5, parseDecimalInput("2.5")!!, 0.0)
    }

    @Test
    fun acceptsCommaAsDecimalSeparator() {
        // French/Moroccan users type "2,5"; this used to be silently saved as 0.
        assertEquals(2.5, parseDecimalInput("2,5")!!, 0.0)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        assertEquals(3.0, parseDecimalInput("  3 ")!!, 0.0)
    }

    @Test
    fun acceptsLeadingSeparator() {
        assertEquals(0.5, parseDecimalInput(",5")!!, 0.0)
    }

    @Test
    fun rejectsBlank() {
        assertNull(parseDecimalInput(""))
        assertNull(parseDecimalInput("   "))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(parseDecimalInput("abc"))
        assertNull(parseDecimalInput("1,2,3"))
        assertNull(parseDecimalInput("1.2.3"))
        assertNull(parseDecimalInput("NaN"))
        assertNull(parseDecimalInput("Infinity"))
    }

    @Test
    fun rejectsNegative() {
        assertNull(parseDecimalInput("-1"))
    }
}
