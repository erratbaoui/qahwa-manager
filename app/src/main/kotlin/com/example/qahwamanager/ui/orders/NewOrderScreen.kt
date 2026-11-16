package com.example.qahwamanager.ui.orders

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaCurrencyText
import com.example.qahwamanager.ui.common.QahwaDivider
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaQuantityControl
import com.example.qahwamanager.ui.common.QahwaSectionHeader
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.getLocalizedCategoryName
import com.example.qahwamanager.ui.common.getLocalizedName
import com.example.qahwamanager.ui.theme.BackgroundMain
import com.example.qahwamanager.ui.theme.DividerColor
import com.example.qahwamanager.ui.theme.PrimaryDark
import com.example.qahwamanager.ui.theme.PrimaryGreen
import com.example.qahwamanager.ui.theme.TextMain
import com.example.qahwamanager.ui.theme.TextSecondary

private val POS_CATEGORIES = listOf("coffee", "tea", "food", "juice")

@Composable
fun NewOrderScreen(
    onOrderPlaced: () -> Unit,
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val menuItems by viewModel.availableMenuItems.collectAsStateWithLifecycle()
    val cart by viewModel.cart.collectAsStateWithLifecycle()
    var tableNumber by remember { mutableStateOf("") }
    var isTakeaway by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("coffee") }
    val focusManager = LocalFocusManager.current

    val filteredItems = remember(menuItems, selectedCategory) {
        menuItems.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    val canPlaceOrder = cart.isNotEmpty() && (isTakeaway || tableNumber.isNotBlank())

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
                        text = stringResource(R.string.new_order),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMain,
                    )
                }
            }
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val isTabletOrWide = maxWidth >= 720.dp

            if (isTabletOrWide) {
                // Two-pane responsive layout for tablets/desktops
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Left Pane: Category Filter + Product Grid
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
                            cart = cart,
                            onAddToCart = viewModel::addToCart,
                            onRemoveFromCart = viewModel::removeFromCart,
                            modifier = Modifier.weight(1f),
                            minCellSize = 160.dp,
                        )
                    }

                    // Right Pane: Order Details & Cart Checkout
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

                            QahwaSectionHeader(
                                title = stringResource(R.string.order_summary),
                            )
                            Spacer(Modifier.height(8.dp))

                            if (cart.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    QahwaEmptyState(
                                        message = stringResource(R.string.no_items_yet),
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
                                    text = stringResource(R.string.cart_total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMain,
                                )
                                QahwaCurrencyText(
                                    amount = viewModel.cartTotal,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = PrimaryDark,
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            QahwaPrimaryButton(
                                text = stringResource(R.string.place_order),
                                onClick = {
                                    val table = if (isTakeaway) "takeaway" else tableNumber.trim()
                                    if (canPlaceOrder) {
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
                // Mobile Portrait layout
                Column(
                    modifier = Modifier.fillMaxSize(),
                ) {
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
                        cart = cart,
                        onAddToCart = viewModel::addToCart,
                        onRemoveFromCart = viewModel::removeFromCart,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        minCellSize = 145.dp,
                    )

                    // Docked Cart summary if items selected
                    if (cart.isNotEmpty()) {
                        Surface(
                            color = Color.White,
                            shadowElevation = 0.dp,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.order_summary),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = TextSecondary,
                                        )
                                        Text(
                                            text = "${cart.sumOf { it.quantity }} items",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = TextMain,
                                        )
                                    }
                                    QahwaCurrencyText(
                                        amount = viewModel.cartTotal,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = PrimaryDark,
                                    )
                                }

                                Spacer(Modifier.height(10.dp))

                                QahwaPrimaryButton(
                                    text = stringResource(R.string.place_order),
                                    onClick = {
                                        val table = if (isTakeaway) "takeaway" else tableNumber.trim()
                                        if (canPlaceOrder) {
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
            color = if (isTakeaway) PrimaryGreen.copy(alpha = 0.12f) else Color.White,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isTakeaway) PrimaryGreen else DividerColor,
            ),
            modifier = Modifier.height(52.dp),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.takeaway),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isTakeaway) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isTakeaway) PrimaryDark else TextMain,
                )
                Switch(
                    checked = isTakeaway,
                    onCheckedChange = onTakeawayChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PrimaryGreen,
                        uncheckedTrackColor = DividerColor,
                        uncheckedThumbColor = TextSecondary,
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        POS_CATEGORIES.forEach { cat ->
            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = {
                    Text(
                        text = getLocalizedCategoryName(cat),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
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
                shape = RoundedCornerShape(10.dp),
            )
        }
    }
}

@Composable
private fun ProductGrid(
    items: List<MenuItem>,
    cart: List<CartItem>,
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
            QahwaEmptyState(
                message = stringResource(R.string.no_items_yet),
            )
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
                val cartItem = cart.find { it.menuItem.id == item.id }
                ProductCard(
                    item = item,
                    quantity = cartItem?.quantity ?: 0,
                    onAdd = { onAddToCart(item) },
                    onRemove = { onRemoveFromCart(item) },
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    item: MenuItem,
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    val isInCart = quantity > 0

    QahwaCard(
        selected = isInCart,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text(
                    text = item.getLocalizedName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = TextMain,
                )
            }

            QahwaCurrencyText(
                amount = item.price,
                style = MaterialTheme.typography.labelLarge,
                color = PrimaryDark,
            )

            // Perfectly centered quantity control
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
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = BackgroundMain,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
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
                    text = cartItem.menuItem.getLocalizedName(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                QahwaCurrencyText(
                    amount = cartItem.menuItem.price * cartItem.quantity,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
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
