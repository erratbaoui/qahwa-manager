package com.example.qahwamanager.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.differenceCentimes
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.data.repository.StaffRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The owner's Shifts screen: ended shifts waiting to be settled, and the settled history per waiter. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShiftsViewModel @Inject constructor(
    shiftRepository: ShiftRepository,
    staffRepository: StaffRepository
) : ViewModel() {

    val toSettle: StateFlow<List<ShiftWithStaff>> = shiftRepository.observeToSettle()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val staff: StateFlow<List<Staff>> = staffRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedStaffId = MutableStateFlow<Long?>(null)
    /** History filter; null means everyone. */
    val selectedStaffId: StateFlow<Long?> = _selectedStaffId.asStateFlow()

    val history: StateFlow<List<ShiftWithStaff>> = _selectedStaffId
        .flatMapLatest { shiftRepository.observeHistory(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Sum of counted minus expected over the shifts shown; negative means cash went missing. */
    val totalDifferenceCentimes: StateFlow<Long> = history
        .map { list -> list.sumOf { it.shift.differenceCentimes ?: 0L } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    fun selectStaff(id: Long?) {
        _selectedStaffId.value = id
    }
}
