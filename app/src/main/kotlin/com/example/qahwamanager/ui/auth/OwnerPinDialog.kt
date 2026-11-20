package com.example.qahwamanager.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.security.PinCheck
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.theme.QahwaTheme

private enum class RecoveryStep { CODE, NEW_PIN, CONFIRM }

/**
 * The owner PIN pad. Four digits submit on their own. After a wrong PIN it says how many tries are
 * left; during a lockout it counts down. "Forgot PIN?" switches to the recovery-code form, which
 * ends by showing the new code before [onVerified].
 */
@Composable
fun OwnerPinDialog(
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
    viewModel: OwnerPinViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var recovering by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.onOpened() }

    val locked = state.error is PinCheck.LockedOut
    val enabled = !locked && !state.busy

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = qColors.background,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .resetsOwnerIdleTimer(),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when {
                    // A successful reset unlocks owner mode; the new code is shown by RecoveryCodeDialog
                    // from the view model's state, so a rotation can't lose it.
                    recovering -> RecoveryForm(
                        state = state,
                        enabled = enabled,
                        onSubmit = { recoveryCode, pin, confirm ->
                            viewModel.resetWithCode(recoveryCode, pin, confirm) { onVerified() }
                        },
                        onLockoutOver = viewModel::onOpened,
                        onBack = { recovering = false },
                    )
                    else -> {
                        PinEntry(
                            title = stringResource(R.string.enter_owner_pin),
                            enabled = enabled,
                            isError = state.error is PinCheck.Wrong,
                            onComplete = { pin -> viewModel.submit(pin, onVerified) },
                            footer = { PinErrorText(state = state, onLockoutOver = viewModel::onOpened) },
                        )
                        TextButton(onClick = { recovering = true }) {
                            Text(stringResource(R.string.forgot_pin), color = qColors.textSecondary)
                        }
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryForm(
    state: PinUiState,
    enabled: Boolean,
    onSubmit: (code: String, pin: String, confirm: String) -> Unit,
    onLockoutOver: () -> Unit,
    onBack: () -> Unit,
) {
    val qColors = QahwaTheme.colors
    var step by remember { mutableStateOf(RecoveryStep.CODE) }
    var code by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }

    // A wrong code or a mismatched PIN sends the owner back to the start of the form.
    LaunchedEffect(state.error, state.inputError) {
        if (state.error != null && step != RecoveryStep.CODE) step = RecoveryStep.CODE
        if (state.inputError != null) step = RecoveryStep.NEW_PIN
    }

    Text(
        text = stringResource(R.string.reset_pin),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = qColors.textPrimary,
    )
    when (step) {
        RecoveryStep.CODE -> {
            QahwaTextField(
                value = code,
                onValueChange = { code = it.uppercase() },
                label = stringResource(R.string.recovery_code),
                enabled = enabled,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            PinErrorText(state = state, forRecoveryCode = true, onLockoutOver = onLockoutOver)
            QahwaPrimaryButton(
                text = stringResource(R.string.continue_action),
                onClick = { step = RecoveryStep.NEW_PIN },
                enabled = enabled && code.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RecoveryStep.NEW_PIN -> PinEntry(
            title = stringResource(R.string.new_pin),
            enabled = enabled,
            onComplete = { pin = it; step = RecoveryStep.CONFIRM },
            footer = { PinErrorText(state = state) },
        )
        RecoveryStep.CONFIRM -> PinEntry(
            title = stringResource(R.string.confirm_pin),
            enabled = enabled,
            onComplete = { confirm -> onSubmit(code, pin, confirm) },
        )
    }
    TextButton(onClick = onBack) {
        Text(stringResource(R.string.back), color = qColors.textSecondary)
    }
}
