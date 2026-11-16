package com.example.qahwamanager.ui.menu

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.ui.common.LanguageSelectorButton
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.getLocalizedCategoryName
import com.example.qahwamanager.ui.common.getLocalizedName
import com.example.qahwamanager.ui.theme.BackgroundMain
import com.example.qahwamanager.ui.theme.DividerColor
import com.example.qahwamanager.ui.theme.ErrorRed
import com.example.qahwamanager.ui.theme.PrimaryDark
import com.example.qahwamanager.ui.theme.PrimaryGreen
import com.example.qahwamanager.ui.theme.SuccessGreen
import com.example.qahwamanager.ui.theme.TextMain
import com.example.qahwamanager.ui.theme.TextSecondary

private val MENU_CATEGORIES = listOf("coffee", "tea", "food", "juice")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(viewModel: MenuViewModel = hiltViewModel()) {
    val items by viewModel.menuItems.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<MenuItem?>(null) }

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
        AddMenuItemDialog(
            defaultCategory = selectedCategory,
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                viewModel.addItem(item)
                showAddDialog = false
            },
        )
    }

    Scaffold(
        containerColor = BackgroundMain,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryGreen,
                contentColor = Color.White,
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
                QahwaPageHeader(
                    title = stringResource(R.string.menu),
                    action = { LanguageSelectorButton() },
                )
            }

            // Category Tab Row
            Surface(
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 840.dp),
                ) {
                    val selectedIndex = MENU_CATEGORIES.indexOf(selectedCategory).coerceAtLeast(0)
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        containerColor = Color.White,
                        contentColor = PrimaryGreen,
                        edgePadding = 20.dp,
                        divider = {},
                    ) {
                        MENU_CATEGORIES.forEachIndexed { index, category ->
                            val isSelected = selectedIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { viewModel.selectCategory(category) },
                                text = {
                                    Text(
                                        text = getLocalizedCategoryName(category),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                },
                                selectedContentColor = PrimaryDark,
                                unselectedContentColor = TextSecondary,
                            )
                        }
                    }
                }
            }

            // Items list
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    QahwaEmptyState(
                        message = stringResource(R.string.no_items_yet),
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
                            onToggleAvailability = { viewModel.toggleAvailability(item) },
                            onDelete = { deleteTarget = item },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    onToggleAvailability: () -> Unit,
    onDelete: () -> Unit,
) {
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
                    text = item.getLocalizedName(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
                if (item.nameAr.isNotBlank() && item.name != item.nameAr) {
                    Text(
                        text = item.nameAr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                QahwaCurrencyText(
                    amount = item.price,
                    style = MaterialTheme.typography.labelLarge,
                    color = PrimaryDark,
                )
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
                        checkedTrackColor = SuccessGreen,
                        uncheckedTrackColor = DividerColor,
                        uncheckedThumbColor = TextSecondary,
                    ),
                )
                Text(
                    text = if (item.isAvailable) stringResource(R.string.available)
                    else stringResource(R.string.unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isAvailable) SuccessGreen else TextSecondary,
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AddMenuItemDialog(
    defaultCategory: String,
    onDismiss: () -> Unit,
    onConfirm: (MenuItem) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var nameFr by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }

    val priceVal = price.toDoubleOrNull()
    val isValid = name.isNotBlank() && priceVal != null && priceVal > 0

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
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
                    color = TextMain,
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
                        color = TextSecondary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MENU_CATEGORIES.forEach { cat ->
                            val isSelected = category.equals(cat, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(getLocalizedCategoryName(cat)) },
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
                        Text(stringResource(R.string.cancel), color = TextSecondary)
                    }
                    QahwaPrimaryButton(
                        text = stringResource(R.string.add_item),
                        onClick = {
                            if (isValid) {
                                onConfirm(
                                    MenuItem(
                                        name = name.trim(),
                                        nameAr = nameAr.trim(),
                                        nameFr = nameFr.trim(),
                                        category = category,
                                        price = priceVal,
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
