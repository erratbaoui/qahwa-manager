package com.example.qahwamanager.ui.orders

import androidx.compose.foundation.BorderStroke
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
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.common.currentLanguage
import com.example.qahwamanager.ui.common.formatQuantity
import com.example.qahwamanager.ui.common.tableLabel
import com.example.qahwamanager.ui.theme.QahwaTheme
import com.example.qahwamanager.util.localizedName

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
    val qColors = QahwaTheme.colors

    var showPayDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showPayDialog) {
        QahwaConfirmDialog(
            title = stringResource(R.string.confirm_payment_title),
            message = stringResource(R.string.confirm_payment_message),
            confirmText = stringResource(R.string.mark_as_paid),
            onConfirm = {
                showPayDialog = false
                viewModel.markAsPaid(orderId, onDone = onBack)
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
                viewModel.cancelOrder(orderId, onDone = onBack)
            },
            onDismiss = { showCancelDialog = false },
            isDestructive = true,
        )
    }

    Scaffold(
        containerColor = qColors.background,
        topBar = {
            Surface(
                color = qColors.cardSurface,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, qColors.divider),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = qColors.textPrimary,
                            )
                        }
                        Text(
                            text = stringResource(R.string.order_title, orderId),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textPrimary,
                        )
                    }
                    Row(modifier = Modifier.padding(end = 8.dp)) {
                        QahwaTopBarActions()
                    }
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
                                    text = tableLabel(ord),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = qColors.textPrimary,
                                )
                                Text(
                                    text = stringResource(R.string.order_title, ord.id),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = qColors.textSecondary,
                                )
                            }
                            QahwaStatusBadge(status = ord.status)
                        }
                    }
                }

                QahwaSectionHeader(title = stringResource(R.string.order_items))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(orderItems, key = { it.id }) { item ->
                        OrderItemRow(item = item, language = language)
                    }
                }

                QahwaDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = qColors.textPrimary,
                    )
                    QahwaCurrencyText(
                        centimes = order?.totalCentimes ?: 0L,
                        style = MaterialTheme.typography.headlineSmall,
                        color = qColors.primaryAccent,
                    )
                }

                Spacer(Modifier.height(4.dp))
            }

            if (order?.status == OrderStatus.OPEN) {
                Surface(
                    color = qColors.cardSurface,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, qColors.divider),
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
                                enabled = !isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Default.Check,
                            )
                            QahwaDestructiveButton(
                                text = stringResource(R.string.cancel_order),
                                onClick = { showCancelDialog = true },
                                enabled = !isSaving,
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
private fun OrderItemRow(item: OrderItem, language: String) {
    val qColors = QahwaTheme.colors
    Surface(
        color = qColors.cardSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, qColors.divider),
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
                    color = qColors.elevatedSurface,
                ) {
                    Text(
                        text = "${formatQuantity(item.quantity)}×",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFeatureSettings = "tnum",
                        ),
                        fontWeight = FontWeight.Bold,
                        color = qColors.primaryAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(
                    text = localizedName(item.menuItemName, item.menuItemNameAr, item.menuItemNameFr, language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = qColors.textPrimary,
                )
            }
            QahwaCurrencyText(
                centimes = item.unitPriceCentimes * item.quantity,
                style = MaterialTheme.typography.bodyMedium,
                color = qColors.textSecondary,
            )
        }
    }
}
