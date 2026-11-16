package com.example.qahwamanager.ui.sales

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.common.tableLabel
import com.example.qahwamanager.ui.theme.BrandAmberGold
import com.example.qahwamanager.ui.theme.BrandCream
import com.example.qahwamanager.ui.theme.QahwaTheme
import java.util.Date

@Composable
fun SalesScreen(viewModel: SalesViewModel = hiltViewModel()) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val orders by viewModel.paidOrders.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors

    Scaffold(containerColor = qColors.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                QahwaPageHeader(title = stringResource(R.string.sales))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        SalesFilter.TODAY to stringResource(R.string.today),
                        SalesFilter.THIS_WEEK to stringResource(R.string.this_week),
                        SalesFilter.ALL_TIME to stringResource(R.string.all_time),
                    ).forEach { (f, label) ->
                        val isSelected = filter == f
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setFilter(f) },
                            label = {
                                Text(
                                    text = label,
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

                Spacer(Modifier.height(12.dp))

                // Hero Revenue Summary Card inspired by Photo 2
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BrandCream.copy(alpha = 0.12f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(brush = qColors.heroBannerBrush)
                            .padding(22.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.total_revenue),
                                style = MaterialTheme.typography.labelLarge,
                                color = BrandAmberGold,
                            )
                            Text(
                                text = formatMad(totalRevenue),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFeatureSettings = "tnum",
                                ),
                                fontWeight = FontWeight.Bold,
                                color = BrandCream,
                            )
                            Text(
                                text = pluralStringResource(R.plurals.orders_count, orders.size, orders.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandCream.copy(alpha = 0.78f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    QahwaSectionHeader(title = stringResource(R.string.order_history))
                }

                Spacer(Modifier.height(8.dp))

                if (orders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        QahwaEmptyState(
                            message = stringResource(R.string.no_paid_orders),
                            icon = Icons.Default.Assessment,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    ) {
                        items(orders, key = { it.id }) { order ->
                            SaleOrderCard(order = order)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleOrderCard(order: Order) {
    val dateFormat = rememberDateFormat("yyyyMMMdHm")
    val qColors = QahwaTheme.colors

    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = tableLabel(order),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.order_title, order.id) + " • " + dateFormat.format(Date(order.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = qColors.textSecondary,
                )
            }
            QahwaCurrencyText(
                centimes = order.totalCentimes,
                color = qColors.success,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
