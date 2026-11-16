package com.example.qahwamanager.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.isTakeaway
import java.text.SimpleDateFormat

/** "Table 4" or "Takeaway", in the UI language. */
@Composable
fun tableLabel(order: Order): String =
    if (order.isTakeaway) stringResource(R.string.takeaway)
    else stringResource(R.string.table_label, order.tableNumber)

@Composable
fun statusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.OPEN -> R.string.status_open
        OrderStatus.PAID -> R.string.status_paid
        OrderStatus.CANCELLED -> R.string.status_cancelled
    }
)

/**
 * A date format for [skeleton] (e.g. "yyyyMMMdHm") in the UI language, with that language's
 * field order and month names. Uses the Compose configuration rather than Locale.getDefault(),
 * so it follows the in-app language choice.
 */
@Composable
fun rememberDateFormat(skeleton: String): SimpleDateFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(skeleton, locale) {
        SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }
}
