package com.example.qahwamanager.ui.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class SalesFilter { TODAY, THIS_WEEK, ALL_TIME }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SalesViewModel @Inject constructor(
    private val salesRepository: SalesRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(SalesFilter.TODAY)
    val filter: StateFlow<SalesFilter> = _filter.asStateFlow()

    // Recomputed whenever the filter changes or the day rolls over.
    val paidOrders: StateFlow<List<Order>> =
        combine(_filter, salesRepository.dayStarts()) { filter, dayStart ->
            when (filter) {
                SalesFilter.TODAY -> dayStart
                SalesFilter.THIS_WEEK -> salesRepository.startOfWeek(dayStart)
                SalesFilter.ALL_TIME -> 0L
            }
        }
            .distinctUntilChanged()
            .flatMapLatest { startMs -> salesRepository.getPaidOrdersSince(startMs) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Total of [paidOrders] in centimes. */
    val totalRevenue: StateFlow<Long> = paidOrders
        .map { orders -> orders.sumOf { it.totalCentimes } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun setFilter(filter: SalesFilter) { _filter.value = filter }
}
