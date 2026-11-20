package com.example.qahwamanager.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.common.QahwaBrandEmblem
import com.example.qahwamanager.ui.common.QahwaLowStockWarning
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.common.QahwaStatCard
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.formatQuantity
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.shift.ShiftCard
import com.example.qahwamanager.ui.shift.ShiftViewModel
import com.example.qahwamanager.ui.theme.BrandAmberGold
import com.example.qahwamanager.ui.theme.BrandCoffeeBrown
import com.example.qahwamanager.ui.theme.BrandCream
import com.example.qahwamanager.ui.theme.BrandForestLight
import com.example.qahwamanager.ui.theme.QahwaTheme
import java.util.Date

@Composable
fun HomeScreen(
    onNewOrder: () -> Unit,
    onEndShift: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    shiftViewModel: ShiftViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOwnerMode by shiftViewModel.isOwnerMode.collectAsStateWithLifecycle()
    val activeShift by shiftViewModel.activeShift.collectAsStateWithLifecycle()
    val today = rememberDateFormat("EEEEdMMMMyyyy").format(Date())
    val qColors = QahwaTheme.colors

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
                .widthIn(max = 840.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── Brand Hero Card (Inspired by Reference Photos 1 & 2) ──────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, BrandCream.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(brush = qColors.heroBannerBrush)
                        .padding(22.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Three signature brand dots from Photo 1
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(BrandCream)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(BrandCoffeeBrown)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(BrandAmberGold)
                                )
                            }

                            QahwaTopBarActions(useTonalStyle = true)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            QahwaBrandEmblem(
                                size = 76.dp,
                                concentricRingStyle = true,
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = "Qahwa manager",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandCream,
                                )
                                Text(
                                    text = stringResource(R.string.brand_tagline),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 2.2.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                    color = BrandAmberGold,
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = today,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandCream.copy(alpha = 0.78f),
                                )
                            }
                        }
                    }
                }
            }

            // ── Current shift ─────────────────────────────────────────────────
            ShiftCard(onEndShift = onEndShift, viewModel = shiftViewModel)

            // ── Summary Statistics (café revenue is the owner's business) ─────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isOwnerMode) {
                    QahwaStatCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.today_revenue),
                        value = formatMad(uiState.todayRevenueCentimes),
                        valueColor = qColors.primaryAccent,
                        accentDotColor = BrandAmberGold,
                    )
                }
                QahwaStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.open_orders),
                    value = formatQuantity(uiState.openOrdersCount),
                    valueColor = qColors.textPrimary,
                    accentDotColor = if (qColors.isDark) BrandForestLight else BrandCoffeeBrown,
                )
            }

            // ── Low Stock Alert Banner ────────────────────────────────────────
            if (uiState.lowStockCount > 0) {
                QahwaLowStockWarning(count = uiState.lowStockCount)
            }

            // ── Quick Actions ─────────────────────────────────────────────────
            QahwaSectionHeader(title = stringResource(R.string.quick_actions))

            QahwaPrimaryButton(
                text = stringResource(R.string.new_order),
                onClick = onNewOrder,
                enabled = activeShift != null,
                leadingIcon = Icons.Default.Add,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
