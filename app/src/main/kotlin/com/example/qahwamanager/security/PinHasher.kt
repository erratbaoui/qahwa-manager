package com.example.qahwamanager.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 for the owner's PIN and recovery code. The SHA-1 variant is the one available on
 * API 24; the iteration count is what makes a 4-digit PIN slow to brute-force offline.
 * Hashing takes tens of milliseconds, so callers run it off the main thread.
 */
object PinHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATIONS = 20_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun newSalt(): String = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }.toHex()

    fun hash(secret: String, saltHex: String): String {
        val spec = PBEKeySpec(secret.toCharArray(), saltHex.fromHex(), ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded.toHex()
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison, so a wrong guess doesn't leak how much of it was right. */
    fun matches(secret: String, saltHex: String, expectedHex: String): Boolean =
        MessageDigest.isEqual(hash(secret, saltHex).fromHex(), expectedHex.fromHex())

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray = ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

/** A PIN is exactly four ASCII digits. */
fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it in '0'..'9' }
