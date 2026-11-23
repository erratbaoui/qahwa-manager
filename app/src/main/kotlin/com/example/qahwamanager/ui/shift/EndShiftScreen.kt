package com.example.qahwamanager.ui.shift

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.StaffRole
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSecondaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.roleLabel
import com.example.qahwamanager.ui.common.tableLabel
import com.example.qahwamanager.ui.theme.QahwaTheme
import com.example.qahwamanager.util.parseMadToCentimes

/**
 * End of shift: optionally collect from open tables, then tap *End shift* to enter the total
 * cash collected (e.g., from counted paper tickets in the bowl) and close the shift.
 */
@Composable
fun EndShiftScreen(
    onBack: () -> Unit,
    viewModel: ShiftViewModel = hiltViewModel(),
) {
    val shift by viewModel.activeShift.collectAsStateWithLifecycle()
    val openOrders by viewModel.openOrders.collectAsStateWithLifecycle()
    val collected by viewModel.collectedCentimes.collectAsStateWithLifecycle()
    val spendings by viewModel.shiftSpendings.collectAsStateWithLifecycle()
    val spendingsTotal by viewModel.shiftSpendingsTotalCentimes.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    val isWaiter = shift?.staffRole == StaffRole.WAITER
    var showHandoverDialog by remember { mutableStateOf(false) }
    var showConfirmLeaveOpen by remember { mutableStateOf(false) }
    var showAddSpending by remember { mutableStateOf(false) }

    if (showAddSpending) {
        AddShiftSpendingDialog(
            onDismiss = { showAddSpending = false },
            onConfirm = { title, amountCentimes ->
                viewModel.addSpending(title, amountCentimes) { showAddSpending = false }
            },
        )
    }

    if (showHandoverDialog && isWaiter) {
        EndShiftAmountDialog(
            staffName = shift?.staffName ?: "",
            openOrders = openOrders,
            onDismiss = { showHandoverDialog = false },
            onConfirm = { amountCentimes ->
                showHandoverDialog = false
                viewModel.endShift(countedCentimes = amountCentimes, onDone = onBack)
            },
        )
    }
    if (showConfirmLeaveOpen) {
        QahwaConfirmDialog(
            title = stringResource(R.string.end_shift),
            message = pluralStringResource(
                R.plurals.leave_open_confirm,
                openOrders.size,
                openOrders.size,
                formatMad(openOrders.sumOf { it.totalCentimes }),
            ),
            confirmText = stringResource(R.string.end_shift),
            onConfirm = {
                showConfirmLeaveOpen = false
                viewModel.endShift(countedCentimes = null, onDone = onBack)
            },
            onDismiss = { showConfirmLeaveOpen = false },
        )
    }

    Scaffold(
        containerColor = qColors.background,
        topBar = {
            Surface(color = qColors.cardSurface, border = BorderStroke(1.dp, qColors.divider)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = qColors.textPrimary)
                        }
                        Text(
                            text = stringResource(R.string.end_shift),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textPrimary,
                        )
                    }
                    Row(modifier = Modifier.padding(end = 8.dp)) { QahwaTopBarActions() }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                shift?.let { current ->
                    QahwaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(current.staffName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = qColors.textPrimary)
                                Text(
                                    text = if (isWaiter) {
                                        stringResource(R.string.collected_so_far)
                                    } else {
                                        roleLabel(current.staffRole)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.textSecondary,
                                )
                            }
                            if (isWaiter) {
                                QahwaCurrencyText(centimes = collected, color = qColors.primaryAccent)
                            }
                        }
                    }
                }
                QahwaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.shift_spendings),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = qColors.textPrimary,
                                )
                                QahwaCurrencyText(
                                    centimes = spendingsTotal,
                                    color = qColors.warning,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            QahwaSecondaryButton(
                                text = stringResource(R.string.add_spending),
                                onClick = { showAddSpending = true },
                                enabled = !isSaving && shift != null,
                                leadingIcon = Icons.Default.Add,
                            )
                        }
                        spendings.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = s.title, style = MaterialTheme.typography.bodySmall, color = qColors.textSecondary)
                                Text(text = formatMad(s.amountCentimes), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = qColors.warning)
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.collect_open_tables),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                if (openOrders.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        QahwaEmptyState(message = stringResource(R.string.no_open_orders), icon = Icons.Default.Check)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                    ) {
                        items(openOrders, key = { it.id }) { order ->
                            OpenOrderRow(order = order, enabled = !isSaving, onMarkPaid = { viewModel.markPaid(order.id) })
                        }
                    }
                }
            }
            Surface(color = qColors.cardSurface, border = BorderStroke(1.dp, qColors.divider)) {
                Box(modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp)) {
                    QahwaPrimaryButton(
                        text = stringResource(R.string.end_shift),
                        onClick = {
                            when {
                                isWaiter -> showHandoverDialog = true
                                openOrders.isNotEmpty() -> showConfirmLeaveOpen = true
                                else -> viewModel.endShift(countedCentimes = null, onDone = onBack)
                            }
                        },
                        enabled = !isSaving && shift != null,
                        leadingIcon = Icons.Default.Stop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EndShiftAmountDialog(
    staffName: String,
    openOrders: List<Order>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val parsedCentimes = parseMadToCentimes(input)
    val showError = input.isNotBlank() && parsedCentimes == null
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
                    text = stringResource(R.string.enter_amount_collected_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.enter_amount_collected_hint, staffName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.textSecondary,
                )
                if (openOrders.isNotEmpty()) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.leave_open_confirm,
                            openOrders.size,
                            openOrders.size,
                            formatMad(openOrders.sumOf { it.totalCentimes }),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.warning,
                        fontWeight = FontWeight.Medium,
                    )
                }
                QahwaTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = stringResource(R.string.amount_collected_mad),
                    isError = showError,
                    errorMessage = if (showError) stringResource(R.string.invalid_number) else null,
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
                        onClick = { parsedCentimes?.let(onConfirm) },
                        enabled = parsedCentimes != null,
                    ) {
                        Text(
                            text = stringResource(R.string.end_shift),
                            color = if (parsedCentimes != null) qColors.primaryAccent else qColors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpenOrderRow(order: Order, enabled: Boolean, onMarkPaid: () -> Unit) {
    val qColors = QahwaTheme.colors
    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tableLabel(order), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = qColors.textPrimary)
                QahwaCurrencyText(centimes = order.totalCentimes, style = MaterialTheme.typography.bodyMedium, color = qColors.textSecondary)
            }
            QahwaSecondaryButton(
                text = stringResource(R.string.mark_as_paid),
                onClick = onMarkPaid,
                enabled = enabled,
                leadingIcon = Icons.Default.Check,
            )
        }
    }
}
