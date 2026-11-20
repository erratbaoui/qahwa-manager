package com.example.qahwamanager.ui.owner

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.model.differenceCentimes
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.theme.QahwaTheme
import java.util.Date

/** Shifts waiting to be settled (oldest first) and the settled history, filterable by waiter. */
@Composable
fun ShiftsScreen(
    onShift: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: ShiftsViewModel = hiltViewModel(),
) {
    val toSettle by viewModel.toSettle.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val staff by viewModel.staff.collectAsStateWithLifecycle()
    val selectedStaffId by viewModel.selectedStaffId.collectAsStateWithLifecycle()
    val totalDifference by viewModel.totalDifferenceCentimes.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var showHistory by remember { mutableStateOf(false) }

    OwnerScaffold(title = stringResource(R.string.shifts), onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(text = stringResource(R.string.to_settle) + if (toSettle.isNotEmpty()) " · ${toSettle.size}" else "", selected = !showHistory) { showHistory = false }
                    Chip(text = stringResource(R.string.history), selected = showHistory) { showHistory = true }
                }
                if (showHistory) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Chip(text = stringResource(R.string.all_staff), selected = selectedStaffId == null) { viewModel.selectStaff(null) }
                        staff.forEach { person ->
                            Chip(text = person.name, selected = selectedStaffId == person.id) { viewModel.selectStaff(person.id) }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.running_difference), style = MaterialTheme.typography.bodyMedium, color = qColors.textSecondary)
                        DifferenceText(totalDifference)
                    }
                }
            }
            val shown = if (showHistory) history else toSettle
            if (shown.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    QahwaEmptyState(message = stringResource(if (showHistory) R.string.no_history else R.string.nothing_to_settle))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    items(shown, key = { it.shift.id }) { shift ->
                        ShiftRow(shift = shift, settled = showHistory, onClick = { onShift(shift.shift.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val qColors = QahwaTheme.colors
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = qColors.primaryAccent,
            selectedLabelColor = qColors.cardSurface,
            labelColor = qColors.textPrimary,
        ),
    )
}

@Composable
private fun ShiftRow(shift: ShiftWithStaff, settled: Boolean, onClick: () -> Unit) {
    val qColors = QahwaTheme.colors
    val day = rememberDateFormat("EEEdMMM")
    val time = rememberDateFormat("Hm")
    val s = shift.shift
    QahwaCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(shift.staffName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = qColors.textPrimary)
                Text(
                    text = day.format(Date(s.startedAt)) + " · " + time.format(Date(s.startedAt)) + " – " + (s.endedAt?.let { time.format(Date(it)) } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = qColors.textSecondary,
                )
                if (s.endedByTakeover) {
                    Text(stringResource(R.string.ended_by_takeover), style = MaterialTheme.typography.bodySmall, color = qColors.warning)
                }
            }
            if (settled) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.expected_cash) + " " + formatMad(s.expectedCentimes ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.textSecondary,
                    )
                    Text(
                        text = stringResource(R.string.counted_cash) + " " + formatMad(s.countedCentimes ?: 0),
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.textSecondary,
                    )
                    DifferenceText(s.differenceCentimes ?: 0)
                }
            } else {
                QahwaCurrencyText(centimes = s.expectedCentimes ?: 0, color = qColors.textSecondary)
            }
        }
    }
}

/** "Short 30.00 MAD" in the error colour, "Over 5.00 MAD" in the success colour, or "Exact". */
@Composable
fun DifferenceText(differenceCentimes: Long, modifier: Modifier = Modifier) {
    val qColors = QahwaTheme.colors
    val (text, color) = when {
        differenceCentimes < 0 -> stringResource(R.string.short_by, formatMad(-differenceCentimes)) to qColors.error
        differenceCentimes > 0 -> stringResource(R.string.over_by, formatMad(differenceCentimes)) to qColors.success
        else -> stringResource(R.string.exact) to qColors.textPrimary
    }
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color, modifier = modifier)
}
