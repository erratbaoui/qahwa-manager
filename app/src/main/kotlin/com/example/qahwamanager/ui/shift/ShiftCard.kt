package com.example.qahwamanager.ui.shift

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSecondaryButton
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.common.roleLabel
import com.example.qahwamanager.ui.theme.QahwaTheme
import java.util.Date

/**
 * The shift card: with no shift running, a name per active staff member to start one; with a
 * shift, who's on, since when, cash collected so far, *End shift*, and a *Take over* link for the
 * next waiter when the previous one went home without ending theirs.
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
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var pickingTakeOver by remember { mutableStateOf(false) }
    var takeOverTarget by remember { mutableStateOf<Staff?>(null) }

    val current = shift
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
                QahwaPrimaryButton(
                    text = stringResource(R.string.end_shift),
                    onClick = onEndShift,
                    enabled = !isSaving,
                    leadingIcon = Icons.Default.Stop,
                    modifier = Modifier.fillMaxWidth(),
                )
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
