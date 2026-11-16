package com.example.qahwamanager.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.repository.InventoryRepository
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.data.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val todayRevenueCentimes: Long = 0,
    val openOrdersCount: Int = 0,
    val lowStockCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val orderRepository: OrderRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        salesRepository.getTodayRevenue(),
        orderRepository.getOpenOrders(),
        inventoryRepository.getLowStockItems()
    ) { revenue, openOrders, lowStock ->
        HomeUiState(
            todayRevenueCentimes = revenue,
            openOrdersCount = openOrders.size,
            lowStockCount = lowStock.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}

