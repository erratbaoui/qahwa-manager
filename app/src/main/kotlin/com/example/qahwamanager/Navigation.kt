package com.example.qahwamanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.qahwamanager.ui.home.HomeScreen
import com.example.qahwamanager.ui.inventory.InventoryScreen
import com.example.qahwamanager.ui.menu.MenuScreen
import com.example.qahwamanager.ui.orders.NewOrderScreen
import com.example.qahwamanager.ui.orders.OrderDetailScreen
import com.example.qahwamanager.ui.orders.OrdersListScreen
import com.example.qahwamanager.ui.sales.SalesScreen
import com.example.qahwamanager.ui.theme.QahwaTheme

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Home)
    val currentTab = backStack.firstOrNull() ?: Home
    val qColors = QahwaTheme.colors

    val tabs = listOf(
        Triple(Home as NavKey, R.string.home, Icons.Default.Home),
        Triple(Orders as NavKey, R.string.orders, Icons.Default.ShoppingCart),
        Triple(Menu as NavKey, R.string.menu, Icons.Default.Restaurant),
        Triple(Inventory as NavKey, R.string.inventory, Icons.Default.Inventory),
        Triple(Sales as NavKey, R.string.sales, Icons.AutoMirrored.Filled.TrendingUp),
    )

    Scaffold(
        containerColor = qColors.background,
        bottomBar = {
            Column {
                HorizontalDivider(color = qColors.divider, thickness = 1.dp)
                NavigationBar(
                    containerColor = qColors.cardSurface,
                    tonalElevation = 0.dp,
                    windowInsets = NavigationBarDefaults.windowInsets,
                ) {
                    tabs.forEach { (destination, labelRes, icon) ->
                        val selected = currentTab == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                backStack.clear()
                                backStack.add(destination)
                            },
                            icon = {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(qColors.primaryAccent.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(labelRes),
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = qColors.primaryAccent,
                                selectedTextColor = qColors.primaryAccent,
                                unselectedIconColor = qColors.textSecondary,
                                unselectedTextColor = qColors.textSecondary,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            entryProvider = entryProvider {
                entry<Home> {
                    HomeScreen(onNewOrder = { backStack.add(NewOrder) })
                }
                entry<Menu> {
                    MenuScreen()
                }
                entry<Orders> {
                    OrdersListScreen(
                        onOrderClick = { orderId -> backStack.add(OrderDetail(orderId)) },
                        onNewOrder = { backStack.add(NewOrder) },
                    )
                }
                entry<NewOrder> {
                    NewOrderScreen(
                        onOrderPlaced = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<OrderDetail> { key ->
                    OrderDetailScreen(
                        orderId = key.orderId,
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<Sales> { SalesScreen() }
                entry<Inventory> { InventoryScreen() }
            },
        )
    }
}
