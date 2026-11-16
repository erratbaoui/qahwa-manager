package com.example.qahwamanager.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats a quantity or count for display in [locale]: that locale's digits and decimal
 * separator, at most two decimals, no trailing zeros and no grouping ("2,5", "5", "1200").
 */
fun formatQuantity(value: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        maximumFractionDigits = 2
        isGroupingUsed = false
    }.format(value)
