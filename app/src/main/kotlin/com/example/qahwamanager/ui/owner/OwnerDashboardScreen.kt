package com.example.qahwamanager.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.auth.resetsOwnerIdleTimer
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaDivider
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.formatQuantity
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.theme.BrandAmberGold
import com.example.qahwamanager.ui.theme.BrandCream
import com.example.qahwamanager.ui.theme.BrandForestLight
import com.example.qahwamanager.ui.theme.QahwaTheme
import com.example.qahwamanager.util.parseMadToCentimes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Owner analytics dashboard: total earnings, daily earnings chart, morning vs afternoon shift
 * comparison, and top-performing staff leaderboard.
 */
@Composable
fun OwnerDashboardScreen(
    onBack: () -> Unit,
    viewModel: OwnerDashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val waiters by viewModel.waiters.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val gate = LocalOwnerGate.current
    var showAddPastEarnings by remember { mutableStateOf(false) }

    if (showAddPastEarnings) {
        AddPastEarningsDialog(
            waiters = waiters,
            isSaving = isSaving,
            onDismiss = { showAddPastEarnings = false },
            onConfirm = { staffId, utcDateMillis, slot, amountCentimes ->
                gate.require {
                    viewModel.addPastEarnings(
                        staffId = staffId,
                        utcDateMillis = utcDateMillis,
                        slot = slot,
                        amountCentimes = amountCentimes,
                        onDone = { showAddPastEarnings = false },
                    )
                }
            },
        )
    }

    OwnerScaffold(title = stringResource(R.string.owner_dashboard), onBack = onBack) { innerPadding ->
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
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Period filter chips
                PeriodFilterRow(
                    selected = state.period,
                    onSelect = viewModel::selectPeriod,
                )

                // Backdate earnings for previous dates
                QahwaPrimaryButton(
                    text = stringResource(R.string.add_past_earnings),
                    onClick = { showAddPastEarnings = true },
                    leadingIcon = Icons.Default.Add,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.totalShifts == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QahwaEmptyState(
                            message = stringResource(R.string.dashboard_empty),
                            icon = Icons.Default.BarChart,
                        )
                    }
                } else {
                    // 1. Hero Earnings Card with period explanation, daily avg & best day
                    EarningsHeroCard(state = state)

                    // 2. Daily Earnings Bar Chart
                    if (state.dailyEarnings.isNotEmpty()) {
                        SectionTitleWithHint(
                            title = stringResource(R.string.daily_earnings),
                            hint = stringResource(R.string.daily_earnings_hint),
                        )
                        DailyEarningsChartCard(days = state.dailyEarnings)
                    }

                    // 3. Morning vs Afternoon Lucrativeness Comparison
                    SectionTitleWithHint(
                        title = stringResource(R.string.morning_vs_afternoon),
                        hint = stringResource(R.string.morning_vs_afternoon_hint),
                    )
                    MorningVsAfternoonCard(
                        morning = state.morningStats,
                        afternoon = state.afternoonStats,
                    )

                    // 4. Top Performing Waiters Leaderboard
                    SectionTitleWithHint(
                        title = stringResource(R.string.top_performing_staff),
                        hint = stringResource(R.string.top_waiters_hint),
                    )
                    TopStaffCard(
                        staff = state.topStaff,
                        totalEarningsCentimes = state.totalEarningsCentimes,
                    )
                }

                // 5. Week-by-Week Earnings Breakdown (always visible whenever any waiter shift exists)
                if (state.weeklyEarnings.isNotEmpty()) {
                    SectionTitleWithHint(
                        title = stringResource(R.string.weekly_earnings),
                        hint = stringResource(R.string.weekly_earnings_hint),
                    )
                    WeeklyEarningsCard(weeks = state.weeklyEarnings)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PeriodFilterRow(
    selected: DashboardPeriod,
    onSelect: (DashboardPeriod) -> Unit,
) {
    val qColors = QahwaTheme.colors
    val items = listOf(
        DashboardPeriod.TODAY to R.string.today,
        DashboardPeriod.THIS_WEEK to R.string.this_week,
        DashboardPeriod.THIS_MONTH to R.string.this_month,
        DashboardPeriod.ALL_TIME to R.string.all_time,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (period, labelRes) ->
            val isSelected = selected == period
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(period) },
                label = {
                    Text(
                        text = stringResource(labelRes),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = qColors.cardSurface,
                    labelColor = qColors.textSecondary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = qColors.divider,
                    selectedBorderColor = Color.Transparent,
                ),
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}

@Composable
private fun SectionTitleWithHint(title: String, hint: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        QahwaSectionHeader(title = title)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = QahwaTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun EarningsHeroCard(state: OwnerDashboardUiState) {
    val qColors = QahwaTheme.colors
    val bestDayFormat = rememberDateFormat("EEE, d MMM")
    val periodLabel = when (state.period) {
        DashboardPeriod.TODAY -> stringResource(R.string.today)
        DashboardPeriod.THIS_WEEK -> stringResource(R.string.this_week)
        DashboardPeriod.THIS_MONTH -> stringResource(R.string.this_month)
        DashboardPeriod.ALL_TIME -> stringResource(R.string.all_time)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BrandCream.copy(alpha = 0.12f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = qColors.heroBannerBrush)
                .padding(22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.total_earnings),
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandAmberGold,
                    )
                    Text(
                        text = stringResource(R.string.dashboard_hero_subtitle, periodLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandCream.copy(alpha = 0.72f),
                    )
                }
                Text(
                    text = formatMad(state.totalEarningsCentimes),
                    style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold,
                    color = BrandCream,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.shifts_completed),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandCream.copy(alpha = 0.72f),
                        )
                        Text(
                            text = formatQuantity(state.totalShifts),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandCream,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.avg_per_day),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandCream.copy(alpha = 0.72f),
                        )
                        Text(
                            text = formatMad(state.averagePerDayCentimes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandCream,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.avg_per_shift),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandCream.copy(alpha = 0.72f),
                        )
                        Text(
                            text = formatMad(state.averagePerShiftCentimes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandAmberGold,
                        )
                    }
                }
                state.bestDay?.let { peak ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandCream.copy(alpha = 0.10f),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.best_day_highlight,
                                bestDayFormat.format(Date(peak.dayStartMillis)),
                                formatMad(peak.totalCentimes),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandAmberGold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyEarningsChartCard(days: List<DailyEarning>) {
    val qColors = QahwaTheme.colors
    val dayLabel = rememberDateFormat("EEE")
    val dateLabel = rememberDateFormat("d/M")
    val maxEarnings = days.maxOfOrNull { it.totalCentimes }?.coerceAtLeast(1L) ?: 1L

    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { day ->
                    val fraction = (day.totalCentimes.toFloat() / maxEarnings.toFloat()).coerceIn(0.06f, 1f)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            text = formatQuantity((day.totalCentimes / 100).toDouble()),
                            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                            color = qColors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (day.totalCentimes == maxEarnings) BrandAmberGold else qColors.primaryAccent),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = dayLabel.format(Date(day.dayStartMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textPrimary,
                            maxLines = 1,
                        )
                        Text(
                            text = dateLabel.format(Date(day.dayStartMillis)),
                            style = MaterialTheme.typography.labelSmall,
                            color = qColors.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyEarningsCard(weeks: List<WeeklyEarning>) {
    val qColors = QahwaTheme.colors
    val rangeFormat = rememberDateFormat("d MMM")
    val maxTotal = weeks.maxOfOrNull { it.totalCentimes }?.coerceAtLeast(1L) ?: 1L

    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            weeks.forEachIndexed { index, week ->
                if (index > 0) QahwaDivider()
                val isBest = week.totalCentimes == maxTotal && weeks.size > 1
                val ratio = (week.totalCentimes.toFloat() / maxTotal.toFloat()).coerceIn(0.05f, 1f)
                val rangeText = "${rangeFormat.format(Date(week.weekStartMillis))} – ${rangeFormat.format(Date(week.weekEndMillis))}"

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = rangeText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = qColors.textPrimary,
                                )
                                if (isBest) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = BrandAmberGold.copy(alpha = 0.18f),
                                    ) {
                                        Text(
                                            text = stringResource(R.string.best_week),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (qColors.isDark) BrandAmberGold else qColors.primaryAccent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = pluralStringResource(R.plurals.shifts_count, week.shiftCount, week.shiftCount) +
                                    " · " + stringResource(R.string.avg_short, formatMad(week.averagePerShiftCentimes)),
                                style = MaterialTheme.typography.bodySmall,
                                color = qColors.textSecondary,
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            QahwaCurrencyText(
                                centimes = week.totalCentimes,
                                color = if (isBest) BrandAmberGold else qColors.primaryAccent,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            week.changePercent?.let { pct ->
                                val positive = pct >= 0
                                val label = if (positive) "+$pct%" else "$pct%"
                                Text(
                                    text = stringResource(R.string.vs_prev_week, label),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (positive) qColors.success else qColors.warning,
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(qColors.elevatedSurface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isBest) BrandAmberGold else qColors.primaryAccent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MorningVsAfternoonCard(
    morning: SlotStats,
    afternoon: SlotStats,
) {
    val qColors = QahwaTheme.colors
    val combinedTotal = (morning.totalCentimes + afternoon.totalCentimes).coerceAtLeast(1L)
    val morningShare = (morning.totalCentimes.toFloat() / combinedTotal.toFloat()).coerceIn(0f, 1f)
    val morningPct = (morningShare * 100).toInt()
    val afternoonPct = 100 - morningPct
    val avgDiff = kotlin.math.abs(morning.averageCentimes - afternoon.averageCentimes)

    val winnerText = when {
        morning.shiftCount == 0 && afternoon.shiftCount == 0 -> null
        morning.averageCentimes > afternoon.averageCentimes ->
            stringResource(R.string.mornings_more_lucrative_by, formatMad(avgDiff))
        afternoon.averageCentimes > morning.averageCentimes ->
            stringResource(R.string.afternoons_more_lucrative_by, formatMad(avgDiff))
        else -> stringResource(R.string.shifts_equally_lucrative)
    }

    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            winnerText?.let { banner ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandAmberGold.copy(alpha = 0.16f),
                ) {
                    Text(
                        text = banner,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (qColors.isDark) BrandAmberGold else qColors.primaryAccent,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SlotColumn(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WbSunny,
                    iconTint = BrandAmberGold,
                    title = stringResource(R.string.morning_shifts),
                    subtitle = stringResource(R.string.morning_hours),
                    stats = morning,
                )
                SlotColumn(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.NightsStay,
                    iconTint = BrandForestLight,
                    title = stringResource(R.string.afternoon_shifts),
                    subtitle = stringResource(R.string.afternoon_hours),
                    stats = afternoon,
                )
            }

            // Proportional split bar with percentage labels
            if (morning.totalCentimes > 0L || afternoon.totalCentimes > 0L) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "${stringResource(R.string.morning_shifts)} · $morningPct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textSecondary,
                        )
                        Text(
                            text = "${stringResource(R.string.afternoon_shifts)} · $afternoonPct%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textSecondary,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(qColors.elevatedSurface),
                    ) {
                        if (morningShare > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(morningShare.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(BrandAmberGold),
                            )
                        }
                        val afternoonShare = 1f - morningShare
                        if (afternoonShare > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(afternoonShare.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(BrandForestLight),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotColumn(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    stats: SlotStats,
) {
    val qColors = QahwaTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = qColors.background,
        border = BorderStroke(1.dp, qColors.divider),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = qColors.textPrimary,
                )
            }
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = qColors.textSecondary)
            Spacer(Modifier.height(4.dp))
            QahwaCurrencyText(
                centimes = stats.totalCentimes,
                style = MaterialTheme.typography.titleMedium,
                color = qColors.textPrimary,
            )
            Text(
                text = pluralStringResource(R.plurals.shifts_count, stats.shiftCount, stats.shiftCount),
                style = MaterialTheme.typography.bodySmall,
                color = qColors.textSecondary,
            )
            Text(
                text = stringResource(R.string.avg_short, formatMad(stats.averageCentimes)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = qColors.primaryAccent,
            )
        }
    }
}

@Composable
private fun TopStaffCard(
    staff: List<StaffPerformance>,
    totalEarningsCentimes: Long,
) {
    val qColors = QahwaTheme.colors
    val maxEarnings = staff.maxOfOrNull { it.totalCentimes }?.coerceAtLeast(1L) ?: 1L
    val safeTotal = totalEarningsCentimes.coerceAtLeast(1L)

    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            staff.forEachIndexed { index, person ->
                if (index > 0) QahwaDivider()
                val ratio = (person.totalCentimes.toFloat() / maxEarnings.toFloat()).coerceIn(0.05f, 1f)
                val sharePct = ((person.totalCentimes * 100L) / safeTotal).toInt()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (index == 0) BrandAmberGold else qColors.elevatedSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (index == 0) Color(0xFF143023) else qColors.textPrimary,
                                )
                            }
                            Column {
                                Text(
                                    text = person.staffName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = qColors.textPrimary,
                                )
                                Text(
                                    text = pluralStringResource(R.plurals.shifts_count, person.shiftCount, person.shiftCount) +
                                        " · " + stringResource(R.string.avg_short, formatMad(person.averageCentimes)) +
                                        " · $sharePct%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.textSecondary,
                                )
                            }
                        }
                        QahwaCurrencyText(
                            centimes = person.totalCentimes,
                            color = if (index == 0) BrandAmberGold else qColors.primaryAccent,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(qColors.elevatedSurface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (index == 0) BrandAmberGold else qColors.primaryAccent),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddPastEarningsDialog(
    waiters: List<Staff>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (staffId: Long, utcDateMillis: Long, slot: ShiftSlot, amountCentimes: Long) -> Unit,
) {
    val qColors = QahwaTheme.colors
    val todayUtcMillis = remember {
        val local = Calendar.getInstance()
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis
    }
    var selectedWaiterId by remember(waiters) { mutableStateOf(waiters.firstOrNull()?.id) }
    var selectedDateUtcMillis by remember { mutableStateOf(todayUtcMillis) }
    var selectedSlot by remember { mutableStateOf(ShiftSlot.MORNING) }
    var amountInput by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedCentimes = parseMadToCentimes(amountInput)
    val showAmountError = amountInput.isNotBlank() && parsedCentimes == null
    val canSave = selectedWaiterId != null && parsedCentimes != null && !isSaving

    val dateFormatter = remember {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= todayUtcMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateUtcMillis = it }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier
                .resetsOwnerIdleTimer()
                .widthIn(max = 460.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_past_earnings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )

                // Waiter selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.select_waiter),
                        style = MaterialTheme.typography.labelLarge,
                        color = qColors.textSecondary,
                    )
                    if (waiters.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_waiters_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = qColors.warning,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            waiters.forEach { waiter ->
                                val isSelected = selectedWaiterId == waiter.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedWaiterId = waiter.id },
                                    label = {
                                        Text(
                                            text = waiter.name,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = qColors.background,
                                        labelColor = qColors.textSecondary,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = qColors.divider,
                                        selectedBorderColor = Color.Transparent,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                )
                            }
                        }
                    }
                }

                // Date selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.select_date),
                        style = MaterialTheme.typography.labelLarge,
                        color = qColors.textSecondary,
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = qColors.background,
                        border = BorderStroke(1.dp, qColors.divider),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = qColors.primaryAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = dateFormatter.format(Date(selectedDateUtcMillis)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = qColors.textPrimary,
                                )
                            }
                            TextButton(onClick = { showDatePicker = true }) {
                                Text(
                                    text = stringResource(R.string.change_date),
                                    color = qColors.primaryAccent,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                // Shift period selector (Morning / Afternoon)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.shift_period),
                        style = MaterialTheme.typography.labelLarge,
                        color = qColors.textSecondary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            ShiftSlot.MORNING to R.string.morning_shifts,
                            ShiftSlot.AFTERNOON to R.string.afternoon_shifts,
                        ).forEach { (slot, labelRes) ->
                            val isSelected = selectedSlot == slot
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSlot = slot },
                                label = {
                                    Text(
                                        text = stringResource(labelRes),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = qColors.background,
                                    labelColor = qColors.textSecondary,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = qColors.divider,
                                    selectedBorderColor = Color.Transparent,
                                ),
                                shape = RoundedCornerShape(10.dp),
                            )
                        }
                    }
                }

                // Amount collected (MAD)
                QahwaTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = stringResource(R.string.amount_collected_mad),
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
                            val waiterId = selectedWaiterId
                            val amount = parsedCentimes
                            if (waiterId != null && amount != null) {
                                onConfirm(waiterId, selectedDateUtcMillis, selectedSlot, amount)
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
