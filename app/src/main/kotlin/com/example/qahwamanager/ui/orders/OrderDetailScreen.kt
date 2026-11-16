package com.example.qahwamanager.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.common.currentLanguage
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.formatQuantity
import com.example.qahwamanager.ui.common.statusLabel
import com.example.qahwamanager.ui.common.tableLabel
import com.example.qahwamanager.util.localizedName
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val order by remember(orderId) { viewModel.observeOrder(orderId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val orderItems by remember(orderId) { viewModel.observeOrderItems(orderId) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val language = currentLanguage()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.order_title, orderId)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            order?.let { ord ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tableLabel(ord),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = statusLabel(ord.status),
                                style = MaterialTheme.typography.labelLarge,
                                color = when (ord.status) {
                                    OrderStatus.OPEN -> MaterialTheme.colorScheme.primary
                                    OrderStatus.PAID -> MaterialTheme.colorScheme.tertiary
                                    OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                }
            }

            Text(stringResource(R.string.order_items), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(orderItems, key = { it.id }) { item ->
                    OrderItemRow(item = item, language = language)
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatMad(order?.totalCentimes ?: 0L),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(8.dp))

            if (order?.status == OrderStatus.OPEN) {
                // Navigate back only once the status change is stored.
                Button(
                    onClick = { viewModel.markAsPaid(orderId, onDone = onBack) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text(stringResource(R.string.mark_as_paid))
                }
                OutlinedButton(
                    onClick = { viewModel.cancelOrder(orderId, onDone = onBack) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.cancel_order))
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem, language: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${formatQuantity(item.quantity)}× " +
                localizedName(item.menuItemName, item.menuItemNameAr, item.menuItemNameFr, language),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = formatMad(item.unitPriceCentimes * item.quantity),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

