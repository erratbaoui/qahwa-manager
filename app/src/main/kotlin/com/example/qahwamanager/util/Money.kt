package com.example.qahwamanager.util

import java.math.BigDecimal

private val MONEY_INPUT = Regex("""^(\d+([.,]\d{0,2})?|[.,]\d{1,2})$""")

/**
 * Parses a user-typed MAD amount ("12", "12.5", "12,50") into centimes.
 * Returns null for blank, malformed, negative input or more than two decimals.
 */
fun parseMadToCentimes(text: String): Long? {
    val trimmed = text.trim()
    if (!MONEY_INPUT.matches(trimmed)) return null
    return BigDecimal(trimmed.replace(',', '.')).movePointRight(2).longValueExact()
}

/** Exact decimal MAD amount for display, e.g. 1250 -> 12.50. */
fun centimesToAmount(centimes: Long): BigDecimal = BigDecimal.valueOf(centimes, 2)
