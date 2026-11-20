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
import androidx.compose.material.icons.filled.AdminPanelSettings
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.qahwamanager.security.OwnerSession
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.auth.LocalOwnerTouch
import com.example.qahwamanager.ui.auth.OwnerGate
import com.example.qahwamanager.ui.auth.OwnerModeBanner
import com.example.qahwamanager.ui.auth.OwnerPinDialog
import com.example.qahwamanager.ui.auth.OwnerPinViewModel
import com.example.qahwamanager.ui.auth.RecoveryCodeDialog
import com.example.qahwamanager.ui.home.HomeScreen
import com.example.qahwamanager.ui.inventory.InventoryScreen
import com.example.qahwamanager.ui.menu.MenuScreen
import com.example.qahwamanager.ui.owner.OwnerHomeScreen
import com.example.qahwamanager.ui.owner.ShiftReportScreen
import com.example.qahwamanager.ui.owner.ShiftsScreen
import com.example.qahwamanager.ui.owner.StaffScreen
import com.example.qahwamanager.ui.orders.NewOrderScreen
import com.example.qahwamanager.ui.orders.OrderDetailScreen
import com.example.qahwamanager.ui.orders.OrdersListScreen
import com.example.qahwamanager.ui.sales.SalesScreen
import com.example.qahwamanager.ui.shift.EndShiftScreen
import com.example.qahwamanager.ui.theme.QahwaTheme

@Composable
fun MainNavigation(ownerSession: OwnerSession) {
    val backStack = rememberNavBackStack(Home)
    val currentTab = backStack.firstOrNull() ?: Home
    val qColors = QahwaTheme.colors
    val gate = remember(ownerSession) { OwnerGate(ownerSession) }
    val isOwnerMode by ownerSession.isOwnerMode.collectAsStateWithLifecycle()
    // Activity-scoped, so a recovery code being shown survives rotation.
    val pinViewModel: OwnerPinViewModel = hiltViewModel()
    val pinState by pinViewModel.state.collectAsStateWithLifecycle()
    val goHome: () -> Unit = {
        backStack.clear()
        backStack.add(Home)
    }

    // Any owner-only action tapped while locked parks itself on the gate; the PIN pad runs it.
    if (gate.pending != null) {
        OwnerPinDialog(
            onVerified = gate::onVerified,
            onDismiss = {
                gate.onDismissed()
                // Giving up on the PIN while the owner area is still on screen leaves it.
                if (!ownerSession.isOwnerMode.value && backStack.firstOrNull() == Owner) goHome()
            },
            viewModel = pinViewModel,
        )
    }
    pinState.newRecoveryCode?.let { code ->
        RecoveryCodeDialog(code = code, onDone = pinViewModel::acknowledgeRecoveryCode)
    }

    // Owner mode ending on its own (idle timeout, app sent to the background) while the owner area
    // is open keeps the screen and its form (a half-typed counted amount) under the PIN pad; the
    // owner either re-enters the PIN and carries on, or dismisses and goes Home.
    LaunchedEffect(isOwnerMode) {
        if (!isOwnerMode && backStack.firstOrNull() == Owner && gate.pending == null) {
            gate.require {}
        }
    }

    CompositionLocalProvider(LocalOwnerGate provides gate, LocalOwnerTouch provides ownerSession::touch) {

    val tabs = listOf(
        Triple(Home as NavKey, R.string.home, Icons.Default.Home),
        Triple(Orders as NavKey, R.string.orders, Icons.Default.ShoppingCart),
        Triple(Menu as NavKey, R.string.menu, Icons.Default.Restaurant),
        Triple(Inventory as NavKey, R.string.inventory, Icons.Default.Inventory),
        Triple(Owner as NavKey, R.string.owner, Icons.Default.AdminPanelSettings),
    )

    Scaffold(
        containerColor = qColors.background,
        topBar = {
            if (isOwnerMode) OwnerModeBanner(onLock = {
                // An explicit Lock means "I'm done": leave the owner area before locking.
                if (backStack.firstOrNull() == Owner) goHome()
                ownerSession.lock()
            })
        },
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
                                val open: () -> Unit = {
                                    backStack.clear()
                                    backStack.add(destination)
                                }
                                if (destination == Owner) gate.require(open) else open()
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
                    HomeScreen(
                        onNewOrder = { backStack.add(NewOrder) },
                        onEndShift = { backStack.add(EndShift) },
                    )
                }
                entry<EndShift> {
                    EndShiftScreen(onBack = { backStack.removeLastOrNull() })
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
                entry<Owner> {
                    OwnerHomeScreen(
                        onSales = { backStack.add(Sales) },
                        onShifts = { backStack.add(Shifts) },
                        onStaff = { backStack.add(StaffList) },
                        onNewRecoveryCode = { gate.require { pinViewModel.issueNewRecoveryCode() } },
                    )
                }
                entry<Shifts> {
                    ShiftsScreen(
                        onShift = { shiftId -> backStack.add(ShiftReport(shiftId)) },
                        onBack = { backStack.removeLastOrNull() },
                    )
                }
                entry<ShiftReport> { key ->
                    ShiftReportScreen(shiftId = key.shiftId, onBack = { backStack.removeLastOrNull() })
                }
                entry<StaffList> {
                    StaffScreen(onBack = { backStack.removeLastOrNull() })
                }
            },
        )
    }
    }
}
