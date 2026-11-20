package com.example.qahwamanager.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.example.qahwamanager.R
import com.example.qahwamanager.security.PinCheck
import com.example.qahwamanager.ui.theme.QahwaTheme
import kotlinx.coroutines.delay

/**
 * The message for a [PinUiState]: tries left after a wrong PIN or code, a live countdown during a
 * lockout, or a problem with the typed input. [onLockoutOver] fires when the countdown reaches zero.
 */
@Composable
fun PinErrorText(
    state: PinUiState,
    modifier: Modifier = Modifier,
    forRecoveryCode: Boolean = false,
    onLockoutOver: () -> Unit = {},
) {
    val qColors = QahwaTheme.colors
    val error = state.error
    val text: String? = when {
        state.inputError == PinInputError.INVALID_PIN -> stringResource(R.string.pin_must_be_4_digits)
        state.inputError == PinInputError.PIN_MISMATCH -> stringResource(R.string.pins_dont_match)
        error is PinCheck.Wrong && error.attemptsBeforeLock <= 0 ->
            stringResource(if (forRecoveryCode) R.string.next_wrong_code_locks else R.string.next_wrong_pin_locks)
        error is PinCheck.Wrong -> {
            val plural = if (forRecoveryCode) R.plurals.wrong_code_tries_left else R.plurals.wrong_pin_tries_left
            pluralStringResource(plural, error.attemptsBeforeLock, error.attemptsBeforeLock)
        }
        error is PinCheck.LockedOut -> {
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(error.untilMillis) {
                while (System.currentTimeMillis() < error.untilMillis) {
                    delay(1_000)
                    now = System.currentTimeMillis()
                }
                onLockoutOver()
            }
            stringResource(R.string.pin_locked_until, formatCountdown(error.untilMillis - now))
        }
        else -> null
    }
    if (text != null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = qColors.error,
            textAlign = TextAlign.Center,
            modifier = modifier,
        )
    }
}
