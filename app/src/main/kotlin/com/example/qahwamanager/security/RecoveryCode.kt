package com.example.qahwamanager.security

import java.security.SecureRandom

/**
 * The owner's way back in after a forgotten PIN: 8 characters from an alphabet without the
 * look-alikes (no 0/O, 1/I/L), shown as `XXXX-XXXX` and accepted in any case, with or without
 * the dash or spaces.
 */
object RecoveryCode {
    const val ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
    const val LENGTH = 8

    fun generate(random: SecureRandom = SecureRandom()): String =
        String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })

    fun normalize(input: String): String =
        input.filterNot { it == ' ' || it == '-' }.uppercase()

    fun format(code: String): String =
        if (code.length == LENGTH) code.substring(0, 4) + "-" + code.substring(4) else code
}
