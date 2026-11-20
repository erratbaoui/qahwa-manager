package com.example.qahwamanager.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.security.PinCheck
import com.example.qahwamanager.ui.theme.QahwaTheme

private enum class ChangeStep { CURRENT, NEW, CONFIRM }

/** Current PIN, new PIN, new PIN again. The recovery code is unchanged. */
@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    viewModel: OwnerPinViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var step by remember { mutableStateOf(ChangeStep.CURRENT) }
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.onOpened() }
    LaunchedEffect(state.error, state.inputError) {
        if (state.error != null) step = ChangeStep.CURRENT
        if (state.inputError != null) step = ChangeStep.NEW
    }
    val enabled = state.error !is PinCheck.LockedOut && !state.busy

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = qColors.background,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.resetsOwnerIdleTimer().widthIn(max = 400.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.change_pin),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                when (step) {
                    ChangeStep.CURRENT -> PinEntry(
                        title = stringResource(R.string.current_pin),
                        enabled = enabled,
                        isError = state.error is PinCheck.Wrong,
                        onComplete = { current = it; step = ChangeStep.NEW },
                        footer = { PinErrorText(state = state, onLockoutOver = viewModel::onOpened) },
                    )
                    ChangeStep.NEW -> PinEntry(
                        title = stringResource(R.string.new_pin),
                        enabled = enabled,
                        onComplete = { newPin = it; step = ChangeStep.CONFIRM },
                        footer = { PinErrorText(state = state) },
                    )
                    ChangeStep.CONFIRM -> PinEntry(
                        title = stringResource(R.string.confirm_pin),
                        enabled = enabled,
                        onComplete = { confirm -> viewModel.changePin(current, newPin, confirm, onDismiss) },
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                }
            }
        }
    }
}
