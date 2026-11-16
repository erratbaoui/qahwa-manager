package com.example.qahwamanager.ui.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaDivider
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaQuantityControl
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.formatQuantity
import com.example.qahwamanager.ui.theme.QahwaTheme
import com.example.qahwamanager.util.parseDecimalInput

@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<InventoryItem?>(null) }
    var deleteTarget by remember { mutableStateOf<InventoryItem?>(null) }
    val qColors = QahwaTheme.colors

    deleteTarget?.let { target ->
        QahwaConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteItem(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
            isDestructive = true,
        )
    }

    if (showAddDialog) {
        InventoryItemDialog(
            item = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                viewModel.addItem(item)
                showAddDialog = false
            },
        )
    }

    editItem?.let { item ->
        InventoryItemDialog(
            item = item,
            onDismiss = { editItem = null },
            onConfirm = { updated ->
                viewModel.updateItem(updated)
                editItem = null
            },
        )
    }

    Scaffold(
        containerColor = qColors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_item),
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
                QahwaPageHeader(title = stringResource(R.string.inventory))
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    QahwaEmptyState(
                        message = stringResource(R.string.select_items),
                        icon = Icons.Default.Inventory2,
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
                    items(items, key = { it.id }) { item ->
                        val step = if (item.unit.equals("pcs", ignoreCase = true)) 1.0 else 0.5
                        InventoryItemCard(
                            item = item,
                            onEdit = { editItem = item },
                            onDelete = { deleteTarget = item },
                            onIncrement = { viewModel.adjustQuantity(item, step) },
                            onDecrement = { viewModel.adjustQuantity(item, -step) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val isLow = item.quantity <= item.lowStockThreshold
    val formattedQty = formatQuantity(item.quantity)
    val qColors = QahwaTheme.colors

    QahwaCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = qColors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "$formattedQty ${item.unit}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFeatureSettings = "tnum",
                            ),
                            fontWeight = FontWeight.Bold,
                            color = if (isLow) qColors.warning else qColors.textPrimary,
                        )
                        if (isLow) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = stringResource(R.string.low_stock),
                                tint = qColors.warning,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.min_stock, formatQuantity(item.lowStockThreshold), item.unit),
                        style = MaterialTheme.typography.labelSmall,
                        color = qColors.textSecondary,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_item),
                            tint = qColors.primaryAccent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            tint = qColors.error,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            QahwaDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isLow) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = qColors.warningContainer,
                        border = BorderStroke(1.dp, qColors.warning.copy(alpha = 0.4f)),
                    ) {
                        Text(
                            text = stringResource(R.string.low_stock),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.warning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.quantity_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = qColors.textSecondary,
                    )
                }

                QahwaQuantityControl(
                    valueText = formattedQty,
                    onAdd = onIncrement,
                    onRemove = onDecrement,
                    canRemove = item.quantity > 0,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryItemDialog(
    item: InventoryItem?,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit,
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(item?.unit ?: "kg") }
    var threshold by remember { mutableStateOf(item?.lowStockThreshold?.toString() ?: "1.0") }
    val commonUnits = listOf("kg", "L", "pcs", "g", "pack")
    val qColors = QahwaTheme.colors

    val parsedQuantity = parseDecimalInput(quantity)
    val parsedThreshold = parseDecimalInput(threshold)
    val quantityError = quantity.isNotBlank() && parsedQuantity == null
    val thresholdError = threshold.isNotBlank() && parsedThreshold == null
    val isValid = name.isNotBlank() && parsedQuantity != null && parsedThreshold != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (item == null) stringResource(R.string.add_item) else stringResource(R.string.edit_item),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )

                QahwaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.item_name),
                    modifier = Modifier.fillMaxWidth(),
                )

                QahwaTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = stringResource(R.string.quantity),
                    isError = quantityError,
                    errorMessage = if (quantityError) stringResource(R.string.invalid_number) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    listOf(-5.0, -1.0, 1.0, 5.0).forEach { step ->
                        OutlinedButton(
                            onClick = {
                                val cur = parseDecimalInput(quantity) ?: 0.0
                                val updated = maxOf(0.0, cur + step)
                                quantity = updated.toString()
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, qColors.divider),
                        ) {
                            Text(
                                text = if (step > 0) "+${step.toInt()}" else step.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = qColors.textPrimary,
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = qColors.textSecondary,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        commonUnits.forEach { u ->
                            val isSelected = unit.equals(u, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { unit = u },
                                label = { Text(u) },
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
                                shape = RoundedCornerShape(8.dp),
                            )
                        }
                    }
                }

                QahwaTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = stringResource(R.string.low_stock_threshold),
                    isError = thresholdError,
                    errorMessage = if (thresholdError) stringResource(R.string.invalid_number) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                    }

                    QahwaPrimaryButton(
                        text = if (item == null) stringResource(R.string.add_item) else stringResource(R.string.save),
                        onClick = {
                            if (isValid) {
                                onConfirm(
                                    InventoryItem(
                                        id = item?.id ?: 0,
                                        name = name.trim(),
                                        quantity = parsedQuantity,
                                        unit = unit.trim(),
                                        lowStockThreshold = parsedThreshold,
                                    ),
                                )
                            }
                        },
                        enabled = isValid,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    )
                }
            }
        }
    }
}
