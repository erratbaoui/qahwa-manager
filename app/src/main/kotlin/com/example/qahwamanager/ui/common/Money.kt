package com.example.qahwamanager.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.qahwamanager.R
import com.example.qahwamanager.util.centimesToAmount

/** Formats centimes with the localized currency pattern, e.g. "12.50 MAD" / "12,50 MAD" / "12.50 درهم". */
@Composable
fun formatMad(centimes: Long): String =
    stringResource(R.string.currency_format, centimesToAmount(centimes))
