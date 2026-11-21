package com.example.qahwamanager.util

import java.math.BigDecimal

private val MONEY_INPUT = Regex("""^(\d{1,9}([.,]\d{0,2})?|[.,]\d{1,2})$""")
private const val MAX_CENTIMES = 1_000_000_000L // 10,000,000.00 MAD (aligned with firestore.rules)

/**
 * Parses a user-typed MAD amount ("12", "12.5", "12,50") into centimes.
 * Returns null for blank, malformed, negative, overflowing input or more than two decimals.
 */
fun parseMadToCentimes(text: String): Long? {
    val trimmed = text.trim()
    if (!MONEY_INPUT.matches(trimmed)) return null
    val centimes = runCatching {
        BigDecimal(trimmed.replace(',', '.')).movePointRight(2).longValueExact()
    }.getOrNull() ?: return null
    return centimes.takeIf { it in 0L..MAX_CENTIMES }
}

/** Exact decimal MAD amount for display, e.g. 1250 -> 12.50. */
fun centimesToAmount(centimes: Long): BigDecimal = BigDecimal.valueOf(centimes, 2)
