package com.example.qahwamanager.ui.orders

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaDivider
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaQuantityControl
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.common.currentLanguage
import com.example.qahwamanager.ui.common.formatMad
import com.example.qahwamanager.ui.common.formatQuantity
import com.example.qahwamanager.ui.shift.ShiftCard
import com.example.qahwamanager.ui.theme.QahwaTheme
import com.example.qahwamanager.util.localizedName

private val POS_CATEGORIES = listOf("all", "coffee", "tea", "food", "juice")

@Composable
private fun posCategoryLabel(category: String): String = when (category) {
    "all" -> stringResource(R.string.all_categories)
    "coffee" -> stringResource(R.string.category_coffee)
    "tea" -> stringResource(R.string.category_tea)
    "food" -> stringResource(R.string.category_food)
    "juice" -> stringResource(R.string.category_juice)
    else -> category
}

@Composable
fun NewOrderScreen(
    onOrderPlaced: () -> Unit,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val menuItems by viewModel.availableMenuItems.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val activeShift by viewModel.activeShift.collectAsStateWithLifecycle()
    val language = currentLanguage()
    val qColors = QahwaTheme.colors
    val focusManager = LocalFocusManager.current

    var tableNumber by rememberSaveable { mutableStateOf("") }
    var isTakeaway by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("all") }

    val filteredItems = remember(menuItems, selectedCategory) {
        if (selectedCategory == "all") {
            menuItems
        } else {
            menuItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
    }
    val cartQtyById = remember(cart) { cart.associate { it.menuItem.id to it.quantity } }
    val cartTotalCentimes = remember(cart) { cart.sumOf { it.menuItem.priceCentimes * it.quantity } }

    val canPlaceOrder = !isSaving && cart.isNotEmpty() && (isTakeaway || tableNumber.isNotBlank())

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
                            text = stringResource(R.string.new_order),
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
        if (activeShift == null) {
            // Nobody is on shift: orders can't be taken until someone starts one.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                ShiftCard(onEndShift = {}, modifier = Modifier.widthIn(max = 600.dp))
            }
            return@Scaffold
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
        ) {
            val isTabletOrWide = maxWidth >= 720.dp

            if (isTabletOrWide) {
                // Two-pane POS layout for tablets & wide viewports
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight(),
                    ) {
                        CategoryChipsRow(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                        )
                        Spacer(Modifier.height(10.dp))
                        ProductGrid(
                            items = filteredItems,
                            language = language,
                            cartQtyById = cartQtyById,
                            onAddToCart = viewModel::addToCart,
                            onRemoveFromCart = viewModel::removeFromCart,
                            modifier = Modifier.weight(1f),
                            minCellSize = 160.dp,
                        )
                    }

                    QahwaCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                        ) {
                            TableTakeawaySelector(
                                tableNumber = tableNumber,
                                onTableNumberChange = { tableNumber = it },
                                isTakeaway = isTakeaway,
                                onTakeawayChange = {
                                    isTakeaway = it
                                    if (it) tableNumber = ""
                                },
                                onDone = { focusManager.clearFocus() },
                            )

                            Spacer(Modifier.height(14.dp))
                            QahwaDivider()
                            Spacer(Modifier.height(10.dp))

                            QahwaSectionHeader(title = stringResource(R.string.order_summary))
                            Spacer(Modifier.height(8.dp))

                            if (cart.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    QahwaEmptyState(
                                        message = stringResource(R.string.select_items),
                                        icon = Icons.Default.ShoppingCart,
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(cart, key = { it.menuItem.id }) { cartItem ->
                                        CartItemRow(
                                            cartItem = cartItem,
                                            language = language,
                                            onAdd = { viewModel.addToCart(cartItem.menuItem) },
                                            onRemove = { viewModel.removeFromCart(cartItem.menuItem) },
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            QahwaDivider()
                            Spacer(Modifier.height(12.dp))

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
                                    centimes = cartTotalCentimes,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = qColors.primaryAccent,
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            QahwaPrimaryButton(
                                text = stringResource(R.string.place_order),
                                onClick = {
                                    val table = if (isTakeaway) Order.TAKEAWAY else tableNumber.trim()
                                    if (table.isNotBlank() && cart.isNotEmpty()) {
                                        viewModel.placeOrder(table) { onOrderPlaced() }
                                    }
                                },
                                enabled = canPlaceOrder,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                // Mobile layout
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TableTakeawaySelector(
                            tableNumber = tableNumber,
                            onTableNumberChange = { tableNumber = it },
                            isTakeaway = isTakeaway,
                            onTakeawayChange = {
                                isTakeaway = it
                                if (it) tableNumber = ""
                            },
                            onDone = { focusManager.clearFocus() },
                        )
                        CategoryChipsRow(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                        )
                    }

                    ProductGrid(
                        items = filteredItems,
                        language = language,
                        cartQtyById = cartQtyById,
                        onAddToCart = viewModel::addToCart,
                        onRemoveFromCart = viewModel::removeFromCart,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        minCellSize = 145.dp,
                    )

                    if (cart.isNotEmpty()) {
                        Surface(
                            color = qColors.cardSurface,
                            shadowElevation = 0.dp,
                            border = BorderStroke(1.dp, qColors.divider),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.order_summary),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = qColors.textSecondary,
                                )

                                LazyColumn(modifier = Modifier.height(64.dp)) {
                                    items(cart, key = { it.menuItem.id }) { cartItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(
                                                text = "${formatQuantity(cartItem.quantity)}× ${cartItem.menuItem.displayName(language)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = qColors.textPrimary,
                                            )
                                            Text(
                                                text = formatMad(cartItem.menuItem.priceCentimes * cartItem.quantity),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = qColors.textSecondary,
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.total),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = qColors.textPrimary,
                                    )
                                    QahwaCurrencyText(
                                        centimes = cartTotalCentimes,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = qColors.primaryAccent,
                                    )
                                }

                                QahwaPrimaryButton(
                                    text = stringResource(R.string.place_order),
                                    onClick = {
                                        val table = if (isTakeaway) Order.TAKEAWAY else tableNumber.trim()
                                        if (table.isNotBlank() && cart.isNotEmpty()) {
                                            viewModel.placeOrder(table) { onOrderPlaced() }
                                        }
                                    },
                                    enabled = canPlaceOrder,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableTakeawaySelector(
    tableNumber: String,
    onTableNumberChange: (String) -> Unit,
    isTakeaway: Boolean,
    onTakeawayChange: (Boolean) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qColors = QahwaTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QahwaTextField(
            value = if (isTakeaway) "" else tableNumber,
            onValueChange = onTableNumberChange,
            label = stringResource(R.string.table_number),
            enabled = !isTakeaway,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.weight(1f),
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isTakeaway) qColors.primaryAccent.copy(alpha = 0.14f) else qColors.cardSurface,
            border = BorderStroke(
                1.dp,
                if (isTakeaway) qColors.primaryAccent else qColors.divider,
            ),
            modifier = Modifier.height(52.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.takeaway),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isTakeaway) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isTakeaway) qColors.primaryAccent else qColors.textPrimary,
                )
                Switch(
                    checked = isTakeaway,
                    onCheckedChange = onTakeawayChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = qColors.divider,
                        uncheckedThumbColor = qColors.textSecondary,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val qColors = QahwaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        POS_CATEGORIES.forEach { cat ->
            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = {
                    Text(
                        text = posCategoryLabel(cat),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
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
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}

@Composable
private fun ProductGrid(
    items: List<MenuItem>,
    language: String,
    cartQtyById: Map<Long, Int>,
    onAddToCart: (MenuItem) -> Unit,
    onRemoveFromCart: (MenuItem) -> Unit,
    modifier: Modifier = Modifier,
    minCellSize: androidx.compose.ui.unit.Dp = 150.dp,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            QahwaEmptyState(message = stringResource(R.string.select_items))
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = minCellSize),
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(items, key = { it.id }) { item ->
                MenuItemSelectCard(
                    item = item,
                    language = language,
                    quantity = cartQtyById[item.id] ?: 0,
                    onAdd = { onAddToCart(item) },
                    onRemove = { onRemoveFromCart(item) },
                )
            }
        }
    }
}

@Composable
private fun MenuItemSelectCard(
    item: MenuItem,
    language: String,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val qColors = QahwaTheme.colors
    QahwaCard(
        selected = quantity > 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
            ) {
                Text(
                    text = item.displayName(language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = qColors.textPrimary,
                )
            }

            QahwaCurrencyText(
                centimes = item.priceCentimes,
                style = MaterialTheme.typography.labelLarge,
                color = qColors.primaryAccent,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                QahwaQuantityControl(
                    quantity = quantity,
                    onAdd = onAdd,
                    onRemove = onRemove,
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    cartItem: CartItem,
    language: String,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val qColors = QahwaTheme.colors
    Surface(
        color = qColors.background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, qColors.divider),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cartItem.menuItem.displayName(language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = qColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                QahwaCurrencyText(
                    centimes = cartItem.menuItem.priceCentimes * cartItem.quantity,
                    style = MaterialTheme.typography.bodySmall,
                    color = qColors.textSecondary,
                )
            }

            QahwaQuantityControl(
                quantity = cartItem.quantity,
                onAdd = onAdd,
                onRemove = onRemove,
            )
        }
    }
}

private fun MenuItem.displayName(language: String) = localizedName(name, nameAr, nameFr, language)
