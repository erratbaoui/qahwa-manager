package com.example.qahwamanager.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.example.qahwamanager.util.formatQuantity

/** [value] in the UI language's digits, matching how [formatMad] shows money. */
@Composable
fun formatQuantity(value: Double): String = formatQuantity(value, LocalConfiguration.current.locales[0])

@Composable
fun formatQuantity(value: Int): String = formatQuantity(value.toDouble())
