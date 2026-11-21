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
        assertTrue(hash.startsWith("v2:"))
        val hexPart = hash.removePrefix("v2:")
        assertEquals(64, hexPart.length)
        assertTrue(hexPart.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(32, PinHasher.newSalt().length)
    }

    @Test
    fun matches_supportsLegacySha1Hash() {
        val salt = "0123456789abcdef0123456789abcdef"
        val spec = javax.crypto.spec.PBEKeySpec(
            "1234".toCharArray(),
            ByteArray(16) { i -> salt.substring(i * 2, i * 2 + 2).toInt(16).toByte() },
            20_000,
            256,
        )
        val legacyHash = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
        assertTrue(PinHasher.matches("1234", salt, legacyHash))
        assertFalse(PinHasher.matches("9999", salt, legacyHash))
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
