package com.example.qahwamanager.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.ui.common.LanguageSelectorButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val items by viewModel.inventoryItems.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(value = false) }
    var editItem by remember { mutableStateOf<InventoryItem?>(value = null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inventory)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                actions = {
                    LanguageSelectorButton()
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(16.dp),
        ) {
            items(items, key = { it.id }) { item ->
                InventoryItemCard(
                    item = item,
                    onEdit = { editItem = item },
                    onIncrement = {
                        val step = if (item.unit.equals("pcs", ignoreCase = true)) 1.0 else 0.5
                        viewModel.adjustQuantity(item, step)
                    },
                    onDecrement = {
                        val step = if (item.unit.equals("pcs", ignoreCase = true)) 1.0 else 0.5
                        viewModel.adjustQuantity(item, -step)
                    },
                    onDelete = { viewModel.deleteItem(item) },
                )
            }
        }
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
}

@Composable
private fun InventoryItemCard(
    item: InventoryItem,
    onEdit: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
) {
    val isLowStock = item.quantity <= item.lowStockThreshold
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(
            containerColor = if (isLowStock) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (isLowStock) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = stringResource(R.string.low_stock),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLowStock) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Min: ${item.lowStockThreshold} ${item.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (isLowStock) {
                                Text(
                                    text = "• " + stringResource(R.string.low_stock),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Quick Action Buttons (Edit & Delete)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_item),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quantity Modifier Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.quantity) + ":",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IconButton(
                            onClick = onDecrement,
                            enabled = item.quantity > 0,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Remove,
                                contentDescription = "Decrease",
                                modifier = Modifier.size(16.dp),
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraSmall,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        ) {
                            Text(
                                text = "${item.quantity} ${item.unit}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }

                        IconButton(
                            onClick = onIncrement,
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Increase",
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryItemDialog(
    item: InventoryItem?,
    onDismiss: () -> Unit,
    onConfirm: (InventoryItem) -> Unit,
) {
    var name by remember { mutableStateOf(value = item?.name ?: "") }
    var quantity by remember { mutableStateOf(value = item?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(value = item?.unit ?: "kg") }
    var threshold by remember { mutableStateOf(value = item?.lowStockThreshold?.toString() ?: "1.0") }

    val commonUnits = listOf("kg", "L", "pcs", "g", "pack")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (item == null) stringResource(R.string.add_item)
                else stringResource(R.string.edit_item),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.item_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Quick Quantity Step Adjusters (+1, +5, -1, -5)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val stepValues = listOf(-5.0, -1.0, 1.0, 5.0)
                    stepValues.forEach { step ->
                        OutlinedButton(
                            onClick = {
                                val current = quantity.toDoubleOrNull() ?: 0.0
                                val updated = maxOf(0.0, current + step)
                                quantity = updated.toString()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).height(32.dp),
                        ) {
                            Text(
                                text = if (step > 0) "+${step.toInt()}" else step.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }

                // Unit selection chips
                Column {
                    Text(
                        text = stringResource(R.string.unit),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        commonUnits.forEach { u ->
                            FilterChip(
                                selected = unit.equals(u, ignoreCase = true),
                                onClick = { unit = u },
                                label = { Text(u) },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text(stringResource(R.string.low_stock_threshold)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 0.0
                    val thr = threshold.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank()) {
                        onConfirm(
                            InventoryItem(
                                id = item?.id ?: 0,
                                name = name.trim(),
                                quantity = qty,
                                unit = unit.trim(),
                                lowStockThreshold = thr,
                            ),
                        )
                    }
                },
            ) {
                Text(
                    if (item == null) stringResource(R.string.add_item)
                    else stringResource(R.string.save),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
