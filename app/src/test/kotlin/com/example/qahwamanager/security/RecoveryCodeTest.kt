package com.example.qahwamanager.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryCodeTest {
    @Test
    fun generate_is8CharsFromAlphabet() {
        repeat(20) {
            val code = RecoveryCode.generate()
            assertEquals(8, code.length)
            assertTrue(code, code.all { it in RecoveryCode.ALPHABET })
        }
    }

    @Test
    fun normalize_acceptsLowercaseAndDash() {
        assertEquals("ABCDEFGH", RecoveryCode.normalize("abcd-efgh "))
        assertEquals("ABCDEFGH", RecoveryCode.normalize(" ab cd ef gh"))
    }

    @Test
    fun format_insertsDash() {
        assertEquals("ABCD-EFGH", RecoveryCode.format("ABCDEFGH"))
    }
}
