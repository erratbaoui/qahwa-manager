package com.example.qahwamanager.ui.sales

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.ui.common.LanguageSelectorButton
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaDivider
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.theme.BackgroundMain
import com.example.qahwamanager.ui.theme.DividerColor
import com.example.qahwamanager.ui.theme.PrimaryDark
import com.example.qahwamanager.ui.theme.PrimaryGreen
import com.example.qahwamanager.ui.theme.SuccessGreen
import com.example.qahwamanager.ui.theme.TextMain
import com.example.qahwamanager.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesScreen(viewModel: SalesViewModel = hiltViewModel()) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val orders by viewModel.paidOrders.collectAsStateWithLifecycle()
    val totalRevenue by viewModel.totalRevenue.collectAsStateWithLifecycle()

    Scaffold(containerColor = BackgroundMain) { innerPadding ->
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
                QahwaPageHeader(
                    title = stringResource(R.string.sales),
                    action = { LanguageSelectorButton() },
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .align(Alignment.CenterHorizontally),
            ) {
                // Filter chips
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
                                selectedContainerColor = PrimaryGreen,
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = TextSecondary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = DividerColor,
                                selectedBorderColor = Color.Transparent,
                            ),
                            shape = RoundedCornerShape(8.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Revenue card
                QahwaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.total_revenue),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                        )
                        QahwaCurrencyText(
                            amount = totalRevenue,
                            style = MaterialTheme.typography.displaySmall,
                            color = PrimaryDark,
                        )
                        Text(
                            text = stringResource(R.string.orders_count, orders.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
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
                            message = stringResource(R.string.no_sales_this_period),
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
    val config = LocalConfiguration.current
    val activeLocale = remember(config) {
        if (!config.locales.isEmpty) config.locales[0] else Locale.getDefault()
    }
    val dateFormat = remember(activeLocale) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", activeLocale)
    }
    val formattedDate = dateFormat.format(Date(order.createdAt))

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
                    text = if (order.tableNumber == "takeaway") {
                        stringResource(R.string.takeaway)
                    } else {
                        stringResource(R.string.table_label, order.tableNumber)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.order_label, order.id) + " • " + formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
            QahwaCurrencyText(
                amount = order.totalAmount,
                color = SuccessGreen,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
