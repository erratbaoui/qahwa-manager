package com.example.qahwamanager.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class SalesFilter { TODAY, THIS_WEEK, ALL_TIME }

data class SalesUiState(
    val filter: SalesFilter = SalesFilter.TODAY,
    val totalRevenue: Double = 0.0,
    val orders: List<Order> = emptyList()
)

@HiltViewModel
class SalesViewModel @Inject constructor(
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(SalesFilter.TODAY)
    val filter: StateFlow<SalesFilter> = _filter.asStateFlow()

    val paidOrders: StateFlow<List<Order>> = _filter.flatMapLatest { f ->
        val startMs = when (f) {
            SalesFilter.TODAY -> startOfToday()
            SalesFilter.THIS_WEEK -> startOfWeek()
            SalesFilter.ALL_TIME -> 0L
        }
        salesRepository.getPaidOrdersSince(startMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalRevenue: StateFlow<Double> = paidOrders
        .map { orders -> orders.sumOf { it.totalAmount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    fun setFilter(filter: SalesFilter) { _filter.value = filter }

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun startOfWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

