package com.example.qahwamanager.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Salted PBKDF2 for the owner's PIN and recovery code.
 * Uses PBKDF2WithHmacSHA256 at 210,000 iterations (`v2:` prefix) when supported by the runtime
 * (Android API 26+ / JVM 17), with automatic fallback to legacy PBKDF2WithHmacSHA1 on API 24–25
 * and seamless verification of previously stored unprefixed hashes.
 */
object PinHasher {
    private const val LEGACY_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val LEGACY_ITERATIONS = 20_000
    private const val V2_PREFIX = "v2:"
    private const val V2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val V2_ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    private val secureRandom = SecureRandom()

    private val supportsSha256: Boolean by lazy {
        try {
            SecretKeyFactory.getInstance(V2_ALGORITHM)
            true
        } catch (_: NoSuchAlgorithmException) {
            false
        }
    }

    fun newSalt(): String = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }.toHex()

    fun hash(secret: String, saltHex: String): String {
        val chars = secret.toCharArray()
        return try {
            hashChars(chars, saltHex)
        } finally {
            chars.fill('\u0000')
        }
    }

    fun hashChars(secret: CharArray, saltHex: String): String {
        val salt = saltHex.fromHex()
        return if (supportsSha256) {
            V2_PREFIX + derive(secret, salt, V2_ALGORITHM, V2_ITERATIONS).toHex()
        } else {
            derive(secret, salt, LEGACY_ALGORITHM, LEGACY_ITERATIONS).toHex()
        }
    }

    /** Constant-time comparison supporting both `v2:` SHA-256 and legacy SHA-1 stored hashes. */
    fun matches(secret: String, saltHex: String, expectedHex: String): Boolean {
        val chars = secret.toCharArray()
        return try {
            val isV2 = expectedHex.startsWith(V2_PREFIX)
            val expectedBytes = expectedHex.removePrefix(V2_PREFIX).fromHex()
            val actualBytes = derive(
                chars = chars,
                salt = saltHex.fromHex(),
                algorithm = if (isV2) V2_ALGORITHM else LEGACY_ALGORITHM,
                iterations = if (isV2) V2_ITERATIONS else LEGACY_ITERATIONS,
            )
            MessageDigest.isEqual(actualBytes, expectedBytes)
        } finally {
            chars.fill('\u0000')
        }
    }

    private fun derive(chars: CharArray, salt: ByteArray, algorithm: String, iterations: Int): ByteArray {
        val spec = PBEKeySpec(chars, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray = ByteArray(length / 2) { i ->
        substring(i * 2, i * 2 + 2).toInt(16).toByte()
    }
}

/** A PIN is exactly four ASCII digits. */
fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all { it in '0'..'9' }
