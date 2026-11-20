package com.example.qahwamanager.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.theme.QahwaTheme

const val PIN_LENGTH = 4

/** Four dots, filled from the start as digits are typed. */
@Composable
fun PinDots(filled: Int, modifier: Modifier = Modifier, isError: Boolean = false) {
    val qColors = QahwaTheme.colors
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(PIN_LENGTH) { index ->
            val on = index < filled
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isError -> qColors.error
                            on -> qColors.primaryAccent
                            else -> qColors.divider
                        }
                    ),
            )
        }
    }
}

/** Digits 0–9 and backspace, laid out like a phone keypad (digits stay left-to-right in Arabic, as on the dialer). */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'), listOf(null, '0', '⌫'))
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { key ->
                    when (key) {
                        null -> Box(Modifier.size(KEY_SIZE))
                        '⌫' -> PinKey(enabled = enabled, onClick = onBackspace) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = stringResource(R.string.backspace),
                                tint = QahwaTheme.colors.textPrimary,
                            )
                        }
                        else -> PinKey(enabled = enabled, onClick = { onDigit(key) }) {
                            Text(
                                text = key.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = QahwaTheme.colors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

private val KEY_SIZE = 68.dp

@Composable
private fun PinKey(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val qColors = QahwaTheme.colors
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = qColors.cardSurface,
        border = BorderStroke(1.dp, qColors.divider),
        modifier = Modifier.size(KEY_SIZE),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * One PIN entry: a title, the dots and the keypad. Calls [onComplete] as soon as four digits are
 * in, then clears itself. [resetKey] changes clear the entry too (e.g. after a wrong PIN).
 */
@Composable
fun PinEntry(
    title: String,
    onComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    resetKey: Any? = null,
    footer: @Composable (() -> Unit)? = null,
) {
    var pin by remember(resetKey) { mutableStateOf("") }
    val qColors = QahwaTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = qColors.textPrimary,
        )
        PinDots(filled = pin.length, isError = isError)
        footer?.invoke()
        PinKeypad(
            enabled = enabled,
            onDigit = { digit ->
                if (pin.length < PIN_LENGTH) {
                    pin += digit
                    if (pin.length == PIN_LENGTH) {
                        val complete = pin
                        pin = ""
                        onComplete(complete)
                    }
                }
            },
            onBackspace = { pin = pin.dropLast(1) },
        )
    }
}

/** "m:ss" for a lockout countdown. */
fun formatCountdown(millis: Long): String {
    val totalSeconds = (maxOf(millis, 0L) + 999) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
