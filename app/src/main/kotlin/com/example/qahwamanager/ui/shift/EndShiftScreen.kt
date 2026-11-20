package com.example.qahwamanager.ui.shift

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSecondaryButton
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.tableLabel
import com.example.qahwamanager.ui.theme.QahwaTheme

/**
 * End of shift: the waiter collects from every open table (each gets *Mark paid*), then ends the
 * shift. Ending with tables still open asks for confirmation; those tables are recorded as left
 * open for the owner.
 */
@Composable
fun EndShiftScreen(
    onBack: () -> Unit,
    viewModel: ShiftViewModel = hiltViewModel(),
) {
    val shift by viewModel.activeShift.collectAsStateWithLifecycle()
    val openOrders by viewModel.openOrders.collectAsStateWithLifecycle()
    val collected by viewModel.collectedCentimes.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var confirmLeaveOpen by remember { mutableStateOf(false) }

    if (confirmLeaveOpen) {
        QahwaConfirmDialog(
            title = stringResource(R.string.end_shift),
            message = pluralStringResource(
                R.plurals.leave_open_confirm, openOrders.size, openOrders.size, formatMad(openOrders.sumOf { it.totalCentimes }),
            ),
            confirmText = stringResource(R.string.end_shift),
            onConfirm = {
                confirmLeaveOpen = false
                viewModel.endShift(onBack)
            },
            onDismiss = { confirmLeaveOpen = false },
            isDestructive = true,
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
                .padding(innerPadding),
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
                                Text(stringResource(R.string.collected_so_far), style = MaterialTheme.typography.bodySmall, color = qColors.textSecondary)
                            }
                            QahwaCurrencyText(centimes = collected, color = qColors.primaryAccent)
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
                        onClick = { if (openOrders.isEmpty()) viewModel.endShift(onBack) else confirmLeaveOpen = true },
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
