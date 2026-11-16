package com.example.qahwamanager.ui.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaDestructiveButton
import com.example.qahwamanager.ui.common.QahwaDivider
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.common.QahwaStatusBadge
import com.example.qahwamanager.ui.theme.BackgroundMain
import com.example.qahwamanager.ui.theme.DividerColor
import com.example.qahwamanager.ui.theme.PrimaryDark
import com.example.qahwamanager.ui.theme.TextMain
import com.example.qahwamanager.ui.theme.TextSecondary

@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val orderItems by viewModel.getOrderItems(orderId).collectAsStateWithLifecycle()
    var order by remember { mutableStateOf<Order?>(null) }
    var showPayDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(orderId) { order = viewModel.getOrderById(orderId) }

    if (showPayDialog) {
        QahwaConfirmDialog(
            title = stringResource(R.string.confirm_payment_title),
            message = stringResource(R.string.confirm_payment_message),
            confirmText = stringResource(R.string.mark_as_paid),
            onConfirm = {
                showPayDialog = false
                viewModel.markAsPaid(orderId)
                onBack()
            },
            onDismiss = { showPayDialog = false },
        )
    }

    if (showCancelDialog) {
        QahwaConfirmDialog(
            title = stringResource(R.string.confirm_cancel_title),
            message = stringResource(R.string.confirm_cancel_message),
            confirmText = stringResource(R.string.cancel_order),
            onConfirm = {
                showCancelDialog = false
                viewModel.cancelOrder(orderId)
                onBack()
            },
            onDismiss = { showCancelDialog = false },
            isDestructive = true,
        )
    }

    Scaffold(
        containerColor = BackgroundMain,
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 0.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = TextMain,
                        )
                    }
                    Text(
                        text = stringResource(R.string.order_label, orderId),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                    )
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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Order metadata card
                order?.let { ord ->
                    QahwaCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = if (ord.tableNumber == "takeaway") {
                                        stringResource(R.string.takeaway)
                                    } else {
                                        stringResource(R.string.table_label, ord.tableNumber)
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMain,
                                )
                                Text(
                                    text = stringResource(R.string.order_label, ord.id),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                )
                            }
                            QahwaStatusBadge(status = ord.status)
                        }
                    }
                }

                // Order items list
                QahwaSectionHeader(
                    title = stringResource(R.string.order_items_title, orderItems.size),
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(orderItems) { item ->
                        OrderItemRow(item = item)
                    }
                }

                // Total breakdown
                QahwaDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.total_revenue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                    )
                    QahwaCurrencyText(
                        amount = order?.totalAmount ?: 0.0,
                        style = MaterialTheme.typography.headlineSmall,
                        color = PrimaryDark,
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            // Actions for open orders
            if (order?.status == OrderStatus.OPEN) {
                Surface(
                    color = Color.White,
                    shadowElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 840.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            QahwaPrimaryButton(
                                text = stringResource(R.string.mark_as_paid),
                                onClick = { showPayDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Default.Check,
                            )
                            QahwaDestructiveButton(
                                text = stringResource(R.string.cancel_order),
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Default.Close,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BackgroundMain,
                ) {
                    Text(
                        text = "${item.quantity}×",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = item.menuItemName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextMain,
                )
            }
            QahwaCurrencyText(
                amount = item.unitPrice * item.quantity,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
