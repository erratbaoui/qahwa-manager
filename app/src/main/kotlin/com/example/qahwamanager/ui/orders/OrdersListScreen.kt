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
import com.example.qahwamanager.ui.common.QahwaStatusBadge
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.common.tableLabel
import com.example.qahwamanager.ui.theme.QahwaTheme
import java.util.Date

@Composable
fun OrdersListScreen(
    onOrderClick: (Long) -> Unit,
    onNewOrder: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val orders by viewModel.openOrders.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors

    Scaffold(
        containerColor = qColors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewOrder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                QahwaPageHeader(title = stringResource(R.string.orders))
            }

            if (orders.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    QahwaEmptyState(message = stringResource(R.string.no_open_orders))
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
    val dateFormat = rememberDateFormat("Hm")
    val qColors = QahwaTheme.colors

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

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                QahwaCurrencyText(
                    centimes = order.totalCentimes,
                    color = qColors.primaryAccent,
                    style = MaterialTheme.typography.titleMedium,
                )
                QahwaStatusBadge(status = order.status)
            }
        }
    }
}
