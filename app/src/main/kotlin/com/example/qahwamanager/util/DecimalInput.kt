package com.example.qahwamanager.util

private val DECIMAL_INPUT = Regex("""^(\d+([.,]\d*)?|[.,]\d+)$""")

/**
 * Parses a non-negative decimal typed by a user, accepting either "." or "," as the
 * decimal separator (French and Moroccan keyboards produce "2,5").
 *
 * Returns null for blank, malformed or negative input so callers can show an error
 * instead of silently saving a default.
 */
fun parseDecimalInput(text: String): Double? {
    val trimmed = text.trim()
    if (!DECIMAL_INPUT.matches(trimmed)) return null
    return trimmed.replace(',', '.').toDoubleOrNull()
}
