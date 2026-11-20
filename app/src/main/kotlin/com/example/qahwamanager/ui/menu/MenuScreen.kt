package com.example.qahwamanager.ui.menu

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.theme.QahwaManagerTheme
import com.example.qahwamanager.ui.theme.QahwaTheme
import com.example.qahwamanager.util.parseMadToCentimes

val CATEGORIES = listOf("coffee", "tea", "food", "juice")
val CATEGORY_LABELS = mapOf(
    "coffee" to R.string.category_coffee,
    "tea" to R.string.category_tea,
    "food" to R.string.category_food,
    "juice" to R.string.category_juice,
)

@Composable
private fun categoryLabel(category: String): String =
    CATEGORY_LABELS[category]?.let { stringResource(it) } ?: category

@Composable
fun MenuScreen(
    viewModel: MenuViewModel = hiltViewModel()
) {
    val items by viewModel.menuItems.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    val gate = LocalOwnerGate.current

    MenuScreenContent(
        items = items,
        selectedCategory = selectedCategory,
        onSelectCategory = viewModel::selectCategory,
        onToggleAvailability = viewModel::toggleAvailability,
        // Owner-only: the PIN is asked when the final action fires, so an open form isn't lost.
        onDeleteItem = { item -> gate.require { viewModel.deleteItem(item) } },
        onAddItem = { item -> gate.require { viewModel.addItem(item) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreenContent(
    items: List<MenuItem>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onToggleAvailability: (MenuItem) -> Unit,
    onDeleteItem: (MenuItem) -> Unit,
    onAddItem: (MenuItem) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MenuItem?>(null) }
    val qColors = QahwaTheme.colors

    deleteTarget?.let { target ->
        QahwaConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                onDeleteItem(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
            isDestructive = true,
        )
    }

    if (showAddDialog) {
        AddMenuItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                onAddItem(item)
                showAddDialog = false
            },
            defaultCategory = selectedCategory,
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
                QahwaPageHeader(title = stringResource(R.string.menu))
            }

            Surface(
                color = qColors.cardSurface,
                border = BorderStroke(1.dp, qColors.divider),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                ) {
                    val selectedIndex = CATEGORIES.indexOf(selectedCategory).coerceAtLeast(0)
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = qColors.cardSurface,
                        contentColor = qColors.primaryAccent,
                        edgePadding = 20.dp,
                        divider = {},
                    ) {
                        CATEGORIES.forEachIndexed { index, category ->
                            val isSelected = selectedIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { onSelectCategory(category) },
                                text = {
                                    Text(
                                        text = categoryLabel(category),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                selectedContentColor = qColors.primaryAccent,
                                unselectedContentColor = qColors.textSecondary,
                            )
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    QahwaEmptyState(
                        message = stringResource(R.string.select_items),
                        icon = Icons.Default.RestaurantMenu,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 840.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        MenuItemCard(
                            item = item,
                            onToggleAvailability = { onToggleAvailability(item) },
                            onDelete = { deleteTarget = item },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
    QahwaManagerTheme(darkTheme = false) {
        MenuScreenContent(
            items = listOf(
                MenuItem(
                    id = 1,
                    name = "Espresso",
                    nameAr = "إسبريسو",
                    nameFr = "Espresso",
                    category = "coffee",
                    priceCentimes = 1500,
                    description = "Rich espresso shot",
                    isAvailable = true,
                ),
            ),
            selectedCategory = "coffee",
            onSelectCategory = {},
            onToggleAvailability = {},
            onDeleteItem = {},
            onAddItem = {},
        )
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    onToggleAvailability: () -> Unit,
    onDelete: () -> Unit,
) {
    val qColors = QahwaTheme.colors
    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                if (item.nameFr.isNotBlank() && item.nameFr != item.name) {
                    Text(
                        text = item.nameFr,
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.textSecondary,
                    )
                }
                if (item.nameAr.isNotBlank()) {
                    Text(
                        text = item.nameAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.textSecondary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                QahwaCurrencyText(
                    centimes = item.priceCentimes,
                    style = MaterialTheme.typography.labelLarge,
                    color = qColors.primaryAccent,
                )
                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.textSecondary,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Switch(
                    checked = item.isAvailable,
                    onCheckedChange = { onToggleAvailability() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = qColors.success,
                        uncheckedTrackColor = qColors.divider,
                        uncheckedThumbColor = qColors.textSecondary,
                    ),
                )
                Text(
                    text = if (item.isAvailable) stringResource(R.string.available)
                    else stringResource(R.string.unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isAvailable) qColors.success else qColors.textSecondary,
                )
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
    }
}

@Composable
private fun AddMenuItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (MenuItem) -> Unit,
    defaultCategory: String,
) {
    var name by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var nameFr by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    val qColors = QahwaTheme.colors

    val priceCentimes = parseMadToCentimes(price)?.takeIf { it > 0 }
    val showPriceError = price.isNotBlank() && priceCentimes == null
    val isValid = name.isNotBlank() && priceCentimes != null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.widthIn(max = 440.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.add_item),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )

                QahwaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.name_en),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = nameFr,
                    onValueChange = { nameFr = it },
                    label = stringResource(R.string.name_fr),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = stringResource(R.string.name_ar),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = stringResource(R.string.price_mad),
                    isError = showPriceError,
                    errorMessage = if (showPriceError) stringResource(R.string.invalid_number) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = stringResource(R.string.description),
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.category),
                        style = MaterialTheme.typography.labelMedium,
                        color = qColors.textSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CATEGORIES.forEach { cat ->
                            val isSelected = category.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(categoryLabel(cat)) },
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
                        text = stringResource(R.string.add),
                        onClick = {
                            if (isValid) {
                                onConfirm(
                                    MenuItem(
                                        name = name.trim(),
                                        nameAr = nameAr.trim(),
                                        nameFr = nameFr.trim(),
                                        category = category,
                                        priceCentimes = priceCentimes,
                                        description = description.trim(),
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
