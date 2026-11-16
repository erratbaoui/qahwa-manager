package com.example.qahwamanager.ui.orders

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
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.ui.common.LanguageSelectorButton
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.common.QahwaStatusBadge
import com.example.qahwamanager.ui.theme.BackgroundMain
import com.example.qahwamanager.ui.theme.PrimaryDark
import com.example.qahwamanager.ui.theme.PrimaryGreen
import com.example.qahwamanager.ui.theme.TextMain
import com.example.qahwamanager.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OrdersListScreen(
    onOrderClick: (Long) -> Unit,
    onNewOrder: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val orders by viewModel.openOrders.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = BackgroundMain,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewOrder,
                containerColor = PrimaryGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.new_order),
                )
            }
        },
    ) { innerPadding ->
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
                    title = stringResource(R.string.orders),
                    action = { LanguageSelectorButton() },
                )
            }

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    QahwaEmptyState(
                        message = stringResource(R.string.no_orders_yet),
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    items(orders, key = { it.id }) { order ->
                        OrderCard(order = order, onClick = { onOrderClick(order.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    val config = LocalConfiguration.current
    val activeLocale = remember(config) {
        if (!config.locales.isEmpty) config.locales[0] else Locale.getDefault()
    }
    val timeFormat = remember(activeLocale) {
        SimpleDateFormat("HH:mm", activeLocale)
    }
    val formattedTime = timeFormat.format(Date(order.createdAt))

    QahwaCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
                    text = stringResource(R.string.order_label, order.id) + " • " + formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QahwaCurrencyText(
                    amount = order.totalAmount,
                    color = PrimaryDark,
                    style = MaterialTheme.typography.titleMedium,
                )
                QahwaStatusBadge(status = order.status)
            }
        }
    }
}
