package com.example.qahwamanager.ui.menu

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.ui.common.LanguageSelectorButton
import com.example.qahwamanager.util.parseMadToCentimes

val CATEGORIES = listOf("coffee", "tea", "food", "juice")
val CATEGORY_LABELS = mapOf(
    "coffee" to R.string.category_coffee,
    "tea" to R.string.category_tea,
    "food" to R.string.category_food,
    "juice" to R.string.category_juice
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

    MenuScreenContent(
        items = items,
        selectedCategory = selectedCategory,
        onSelectCategory = viewModel::selectCategory,
        onToggleAvailability = viewModel::toggleAvailability,
        onDeleteItem = viewModel::deleteItem,
        onAddItem = viewModel::addItem
    )
}

// Refactored UI into a stateless Composable to allow Compose Preview rendering without ViewModel instantiation errors.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreenContent(
    items: List<MenuItem>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
    onToggleAvailability: (MenuItem) -> Unit,
    onDeleteItem: (MenuItem) -> Unit,
    onAddItem: (MenuItem) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.menu)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    LanguageSelectorButton()
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_item))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category tabs
            PrimaryScrollableTabRow(
                selectedTabIndex = CATEGORIES.indexOf(selectedCategory).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                CATEGORIES.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { onSelectCategory(category) },
                        text = { Text(categoryLabel(category)) }
                    )
                }
            }

            // Items list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    MenuItemCard(
                        item = item,
                        onToggleAvailability = { onToggleAvailability(item) },
                        onDelete = { onDeleteItem(item) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddMenuItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { item ->
                onAddItem(item)
                showAddDialog = false
            },
            defaultCategory = selectedCategory
        )
    }
}

// Added preview using mock data for MenuScreenContent
@Preview(showBackground = true)
@Composable
private fun MenuScreenPreview() {
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
                isAvailable = true
            ),
            MenuItem(
                id = 2,
                name = "Cappuccino",
                nameAr = "كابتشينو",
                nameFr = "Cappuccino",
                category = "coffee",
                priceCentimes = 2000,
                description = "Espresso with milk foam",
                isAvailable = false
            )
        ),
        selectedCategory = "coffee",
        onSelectCategory = {},
        onToggleAvailability = {},
        onDeleteItem = {},
        onAddItem = {}
    )
}

@Composable
private fun MenuItemCard(
    item: MenuItem,
    onToggleAvailability: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isAvailable)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.nameFr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.nameAr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatMad(item.priceCentimes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked = item.isAvailable,
                    onCheckedChange = { onToggleAvailability() }
                )
                Text(
                    text = if (item.isAvailable) stringResource(R.string.available) else stringResource(R.string.unavailable),
                    style = MaterialTheme.typography.labelSmall
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMenuItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (MenuItem) -> Unit,
    defaultCategory: String
) {
    var name by remember { mutableStateOf("") }
    var nameAr by remember { mutableStateOf("") }
    var nameFr by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(defaultCategory) }
    // Accepts "12", "12.5" and "12,50"; anything else (or zero) is flagged instead of dropped silently.
    val priceCentimes = parseMadToCentimes(price)?.takeIf { it > 0 }
    val showPriceError = price.isNotBlank() && priceCentimes == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name_en)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameFr,
                    onValueChange = { nameFr = it },
                    label = { Text(stringResource(R.string.name_fr)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = { nameAr = it },
                    label = { Text(stringResource(R.string.name_ar)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text(stringResource(R.string.price_mad)) },
                    isError = showPriceError,
                    supportingText = if (showPriceError) {
                        { Text(stringResource(R.string.invalid_number)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description)) },
                    modifier = Modifier.fillMaxWidth()
                )
                // Category selector
                Text(stringResource(R.string.category), style = MaterialTheme.typography.labelMedium)
                PrimaryScrollableTabRow(
                    selectedTabIndex = CATEGORIES.indexOf(category).coerceAtLeast(0)
                ) {
                    CATEGORIES.forEach { cat ->
                        Tab(
                            selected = category == cat,
                            onClick = { category = cat },
                            text = { Text(categoryLabel(cat)) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && priceCentimes != null,
                onClick = {
                    if (name.isNotBlank() && priceCentimes != null) {
                        onConfirm(
                            MenuItem(
                                name = name.trim(),
                                nameAr = nameAr.trim(),
                                nameFr = nameFr.trim(),
                                category = category,
                                priceCentimes = priceCentimes,
                                description = description.trim()
                            )
                        )
                    }
                }
            ) { Text(stringResource(R.string.add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

