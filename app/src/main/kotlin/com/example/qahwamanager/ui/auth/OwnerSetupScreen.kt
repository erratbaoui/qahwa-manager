package com.example.qahwamanager.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.common.QahwaBrandEmblem
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.root.RootViewModel
import com.example.qahwamanager.ui.root.SetupError
import com.example.qahwamanager.ui.theme.QahwaTheme

private enum class SetupStep { NAME, PIN, CONFIRM }

/** First run: the owner's name, a PIN typed twice, then the recovery code shown once. */
@Composable
fun OwnerSetupScreen(viewModel: RootViewModel) {
    val recoveryCode by viewModel.recoveryCodeToShow.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var step by remember { mutableStateOf(SetupStep.NAME) }
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<SetupError?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(qColors.background)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                QahwaTopBarActions()
            }
            QahwaBrandEmblem(size = 64.dp, concentricRingStyle = true)

            val code = recoveryCode
            if (code != null) {
                RecoveryCodeCard(code = code, onDone = viewModel::acknowledgeRecoveryCode)
                return@Column
            }

            Text(
                text = stringResource(R.string.owner_setup_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = qColors.textPrimary,
                textAlign = TextAlign.Center,
            )

            when (step) {
                SetupStep.NAME -> {
                    QahwaTextField(
                        value = name,
                        onValueChange = { name = it; error = null },
                        label = stringResource(R.string.owner_name),
                        isError = error == SetupError.BLANK_NAME,
                        errorMessage = if (error == SetupError.BLANK_NAME) stringResource(R.string.name_required) else null,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    QahwaPrimaryButton(
                        text = stringResource(R.string.continue_action),
                        onClick = {
                            if (name.isBlank()) error = SetupError.BLANK_NAME else step = SetupStep.PIN
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                SetupStep.PIN -> PinEntry(
                    title = stringResource(R.string.create_pin),
                    onComplete = { typed ->
                        pin = typed
                        error = null
                        step = SetupStep.CONFIRM
                    },
                )
                SetupStep.CONFIRM -> PinEntry(
                    title = stringResource(R.string.confirm_pin),
                    isError = error == SetupError.PIN_MISMATCH,
                    resetKey = error,
                    onComplete = { typed ->
                        val problem = viewModel.setUp(name, pin, typed)
                        error = problem
                        if (problem == SetupError.PIN_MISMATCH || problem == SetupError.INVALID_PIN) step = SetupStep.PIN
                        if (problem == SetupError.BLANK_NAME) step = SetupStep.NAME
                    },
                    footer = {
                        if (error == SetupError.PIN_MISMATCH) {
                            Text(stringResource(R.string.pins_dont_match), color = qColors.error, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                )
            }
            if (step != SetupStep.NAME) {
                TextButton(onClick = { step = SetupStep.NAME; pin = "" }) {
                    Text(stringResource(R.string.back), color = qColors.textSecondary)
                }
            }
            if (error == SetupError.PIN_MISMATCH && step == SetupStep.PIN) {
                Text(stringResource(R.string.pins_dont_match), color = qColors.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
