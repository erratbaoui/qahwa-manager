package com.example.qahwamanager.ui.owner

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaDivider
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.common.tableLabel
import com.example.qahwamanager.ui.theme.QahwaTheme
import java.util.Date

/**
 * One shift's settlement: expected cash against what the owner counted, with everything that
 * explains a gap (cancellations, tables left open, wrong-PIN attempts, takeover). ENDED shifts
 * take a counted amount and *Confirm settlement*; SETTLED shifts are read-only.
 */
@Composable
fun ShiftReportScreen(
    shiftId: Long,
    onBack: () -> Unit,
    viewModel: ShiftReportViewModel = hiltViewModel(),
) {
    val report by remember(shiftId) { viewModel.report(shiftId) }.collectAsStateWithLifecycle(initialValue = null)
    val countedInput by viewModel.countedInput.collectAsStateWithLifecycle()
    val countedCentimes by viewModel.countedCentimes.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val gate = LocalOwnerGate.current
    val qColors = QahwaTheme.colors
    val day = rememberDateFormat("EEEdMMM")
    val time = rememberDateFormat("Hm")

    val current = report
    LaunchedEffect(current?.shift?.shift?.countedCentimes) {
        viewModel.prefillIfEmpty(current?.shift?.shift?.countedCentimes)
    }
    OwnerScaffold(
        title = current?.let { stringResource(R.string.settle_title, it.shift.staffName) } ?: stringResource(R.string.shifts),
        onBack = onBack,
    ) { innerPadding ->
        if (current == null) return@OwnerScaffold
        val shift = current.shift.shift
        val settled = shift.status == ShiftStatus.SETTLED
        val counted = if (settled) shift.countedCentimes else countedCentimes
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = day.format(Date(shift.startedAt)) + " · " + time.format(Date(shift.startedAt)) + " – " + (shift.endedAt?.let { time.format(Date(it)) } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.textSecondary,
                )
                QahwaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReportRow(stringResource(R.string.orders_collected), current.paidOrders.size.toString())
                        ReportRow(stringResource(R.string.expected_cash), formatMad(current.expectedCentimes), bold = true)
                        if (settled) {
                            ReportRow(stringResource(R.string.counted_cash), formatMad(shift.countedCentimes ?: 0), bold = true)
                        } else {
                            QahwaTextField(
                                value = countedInput,
                                onValueChange = viewModel::setCountedInput,
                                label = stringResource(R.string.counted_cash),
                                isError = countedInput.isNotBlank() && countedCentimes == null,
                                errorMessage = if (countedInput.isNotBlank() && countedCentimes == null) stringResource(R.string.invalid_number) else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        QahwaDivider()
                        if (counted != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                DifferenceText(counted - current.expectedCentimes)
                            }
                        }
                        ReportRow(
                            stringResource(R.string.cancelled_by_owner),
                            pluralStringResource(R.plurals.orders_count, current.cancelledOrders.size, current.cancelledOrders.size) +
                                " · " + formatMad(current.cancelledOrders.sumOf { it.totalCentimes }),
                        )
                        ReportRow(stringResource(R.string.left_open), current.leftOpen.size.toString())
                        current.leftOpen.forEach { row ->
                            val order = current.leftOpenOrders.find { it.id == row.orderId }
                            ReportRow(
                                "    " + (order?.let { tableLabel(it) } ?: stringResource(R.string.order_title, row.orderId)),
                                formatMad(row.totalCentimes),
                                muted = true,
                            )
                        }
                        ReportRow(stringResource(R.string.wrong_pin_attempts), shift.failedOwnerPinAttempts.toString())
                        if (shift.endedByTakeover) {
                            Text(stringResource(R.string.ended_by_takeover), style = MaterialTheme.typography.bodyMedium, color = qColors.warning, fontWeight = FontWeight.SemiBold)
                        }
                        if (settled) {
                            Text(
                                text = stringResource(R.string.settled_on, shift.settledAt?.let { day.format(Date(it)) + " " + time.format(Date(it)) } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = qColors.textSecondary,
                            )
                        } else {
                            QahwaPrimaryButton(
                                text = stringResource(R.string.confirm_settlement),
                                onClick = { gate.require { viewModel.settle(shiftId, onBack) } },
                                enabled = countedCentimes != null && !isSaving,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                QahwaSectionHeader(title = stringResource(R.string.orders_collected))
                current.paidOrders.forEach { order -> OrderLine(order, time) }
            }
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String, bold: Boolean = false, muted: Boolean = false) {
    val qColors = QahwaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = if (muted) qColors.textSecondary else qColors.textPrimary)
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = if (muted) qColors.textSecondary else qColors.textPrimary,
        )
    }
}

@Composable
private fun OrderLine(order: Order, time: java.text.SimpleDateFormat) {
    val qColors = QahwaTheme.colors
    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(tableLabel(order), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = qColors.textPrimary)
                Text(
                    stringResource(R.string.order_title, order.id) + (order.paidAt?.let { " · " + time.format(Date(it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = qColors.textSecondary,
                )
            }
            QahwaCurrencyText(centimes = order.totalCentimes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
