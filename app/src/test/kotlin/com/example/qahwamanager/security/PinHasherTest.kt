package com.example.qahwamanager.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun sameSecretAndSalt_sameHash() {
        val salt = PinHasher.newSalt()
        assertEquals(PinHasher.hash("1234", salt), PinHasher.hash("1234", salt))
    }

    @Test
    fun differentSalt_differentHash() {
        assertNotEquals(PinHasher.hash("1234", PinHasher.newSalt()), PinHasher.hash("1234", PinHasher.newSalt()))
    }

    @Test
    fun matches_rejectsWrongPin() {
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash("1234", salt)
        assertTrue(PinHasher.matches("1234", salt, hash))
        assertFalse(PinHasher.matches("1235", salt, hash))
    }

    @Test
    fun hash_isHex64Chars() {
        val hash = PinHasher.hash("1234", PinHasher.newSalt())
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(32, PinHasher.newSalt().length)
    }

    @Test
    fun isValidPin_exactlyFourAsciiDigits() {
        assertTrue(isValidPin("0000"))
        assertTrue(isValidPin("1234"))
        assertFalse(isValidPin("123"))
        assertFalse(isValidPin("12345"))
        assertFalse(isValidPin("12a4"))
        assertFalse(isValidPin("١٢٣٤"))
        assertFalse(isValidPin(""))
    }
}
