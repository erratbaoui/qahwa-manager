package com.example.qahwamanager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.qahwamanager.ui.home.HomeScreen
import com.example.qahwamanager.ui.inventory.InventoryScreen
import com.example.qahwamanager.ui.menu.MenuScreen
import com.example.qahwamanager.ui.orders.NewOrderScreen
import com.example.qahwamanager.ui.orders.OrderDetailScreen
import com.example.qahwamanager.ui.orders.OrdersListScreen
import com.example.qahwamanager.ui.sales.SalesScreen

@Composable
fun MainNavigation() {
    var currentTab by remember { mutableStateOf<NavKey>(Home) }
    val backStack = rememberNavBackStack(Home)

    val tabs = listOf(
        Triple(Home as NavKey, R.string.home, Icons.Default.Home),
        Triple(Menu as NavKey, R.string.menu, Icons.Default.Restaurant),
        Triple(Orders as NavKey, R.string.orders, Icons.Default.ShoppingCart),
        Triple(Sales as NavKey, R.string.sales, Icons.Default.TrendingUp),
        Triple(Inventory as NavKey, R.string.inventory, Icons.Default.Inventory),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { (destination, labelRes, icon) ->
                    NavigationBarItem(
                        selected = currentTab == destination,
                        onClick = {
                            currentTab = destination
                            backStack.clear()
                            backStack.add(destination)
                        },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            entryProvider = entryProvider {
                entry<Home> {
                    HomeScreen(
                        onNewOrder = {
                            backStack.add(NewOrder)
                        }
                    )
                }
                entry<Menu> {
                    MenuScreen()
                }
                entry<Orders> {
                    OrdersListScreen(
                        onOrderClick = { orderId -> backStack.add(OrderDetail(orderId)) },
                        onNewOrder = { backStack.add(NewOrder) }
                    )
                }
                entry<NewOrder> {
                    NewOrderScreen(
                        onOrderPlaced = { backStack.removeLastOrNull() },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<OrderDetail> { key ->
                    OrderDetailScreen(
                        orderId = key.orderId,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Sales> {
                    SalesScreen()
                }
                entry<Inventory> {
                    InventoryScreen()
                }
            }
        )
    }
}

