package com.example.qahwamanager.ui.shift

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSecondaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.common.roleLabel
import com.example.qahwamanager.ui.theme.QahwaTheme
import com.example.qahwamanager.util.parseMadToCentimes
import java.util.Date

/**
 * The shift card: with no shift running, a name per active staff member to start one; with a
 * shift, who's on, since when, cash collected so far, spendings this shift, *Add spending*,
 * *End shift*, and a *Take over* link.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShiftCard(
    onEndShift: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShiftViewModel = hiltViewModel(),
) {
    val shift by viewModel.activeShift.collectAsStateWithLifecycle()
    val staff by viewModel.activeStaff.collectAsStateWithLifecycle()
    val openOrders by viewModel.openOrders.collectAsStateWithLifecycle()
    val collected by viewModel.collectedCentimes.collectAsStateWithLifecycle()
    val spendingsTotal by viewModel.shiftSpendingsTotalCentimes.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var pickingTakeOver by remember { mutableStateOf(false) }
    var takeOverTarget by remember { mutableStateOf<Staff?>(null) }
    var showAddSpending by remember { mutableStateOf(false) }

    val current = shift
    if (showAddSpending && current != null) {
        AddShiftSpendingDialog(
            onDismiss = { showAddSpending = false },
            onConfirm = { title, amountCentimes ->
                viewModel.addSpending(title, amountCentimes) { showAddSpending = false }
            },
        )
    }

    takeOverTarget?.let { target ->
        QahwaConfirmDialog(
            title = stringResource(R.string.take_over_title),
            message = pluralStringResource(
                R.plurals.take_over_confirm, openOrders.size, current?.staffName ?: "", openOrders.size,
            ),
            confirmText = stringResource(R.string.start_shift),
            onConfirm = {
                viewModel.takeOver(target.id)
                takeOverTarget = null
            },
            onDismiss = { takeOverTarget = null },
        )
    }

    QahwaCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (current == null) {
                Text(
                    text = stringResource(R.string.start_shift),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = qColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.who_is_working),
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.textSecondary,
                )
                StaffPicker(staff = staff, enabled = !isSaving, onPick = { viewModel.start(it.id) })
            } else {
                val since = rememberDateFormat("Hm").format(Date(current.shift.startedAt))
                Text(
                    text = stringResource(R.string.shift_since, current.staffName, since),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = qColors.textPrimary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.collected_so_far),
                        style = MaterialTheme.typography.bodyMedium,
                        color = qColors.textSecondary,
                    )
                    QahwaCurrencyText(centimes = collected, color = qColors.primaryAccent)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.shift_spendings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = qColors.textSecondary,
                    )
                    QahwaCurrencyText(centimes = spendingsTotal, color = qColors.warning)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QahwaSecondaryButton(
                        text = stringResource(R.string.add_spending),
                        onClick = { showAddSpending = true },
                        enabled = !isSaving,
                        leadingIcon = Icons.Default.Add,
                        modifier = Modifier.weight(1f),
                    )
                    QahwaPrimaryButton(
                        text = stringResource(R.string.end_shift),
                        onClick = onEndShift,
                        enabled = !isSaving,
                        leadingIcon = Icons.Default.Stop,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pickingTakeOver) {
                    Text(
                        text = stringResource(R.string.who_is_working),
                        style = MaterialTheme.typography.bodyMedium,
                        color = qColors.textSecondary,
                    )
                    StaffPicker(
                        staff = staff.filter { it.id != current.shift.staffId },
                        enabled = !isSaving,
                        onPick = { takeOverTarget = it; pickingTakeOver = false },
                    )
                } else {
                    TextButton(onClick = { pickingTakeOver = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(
                            text = stringResource(R.string.take_over, current.staffName),
                            color = qColors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddShiftSpendingDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amountCentimes: Long) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    val parsedCentimes = parseMadToCentimes(amountInput)?.takeIf { it > 0L }
    val showAmountError = amountInput.isNotBlank() && parsedCentimes == null
    val canSave = title.isNotBlank() && parsedCentimes != null
    val qColors = QahwaTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_spending),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                QahwaTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = stringResource(R.string.spending_reason),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = stringResource(R.string.spending_amount_mad),
                    isError = showAmountError,
                    errorMessage = if (showAmountError) stringResource(R.string.invalid_number) else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                    }
                    TextButton(
                        onClick = {
                            if (canSave && parsedCentimes != null) {
                                onConfirm(title.trim(), parsedCentimes)
                            }
                        },
                        enabled = canSave,
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = if (canSave) qColors.primaryAccent else qColors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffPicker(staff: List<Staff>, enabled: Boolean, onPick: (Staff) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        staff.forEach { person ->
            QahwaSecondaryButton(
                text = "${person.name} · ${roleLabel(person.role)}",
                onClick = { onPick(person) },
                enabled = enabled,
                leadingIcon = Icons.Default.PlayArrow,
            )
        }
    }
}
