package com.example.qahwamanager.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.repository.ShiftReport
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.util.parseMadToCentimes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One shift's report and, while it's ENDED, the settlement form. */
@HiltViewModel
class ShiftReportViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    private val ownerSession: OwnerSession
) : ViewModel() {

    fun report(shiftId: Long): Flow<ShiftReport?> = shiftRepository.observeReport(shiftId)

    private val _countedInput = MutableStateFlow("")
    /** What the owner typed, verbatim ("610,50" stays "610,50"). */
    val countedInput: StateFlow<String> = _countedInput.asStateFlow()

    /** The typed amount in centimes, or null while it isn't a valid amount. Never settles as 0 by accident. */
    val countedCentimes: StateFlow<Long?> = _countedInput
        .map { parseMadToCentimes(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun setCountedInput(text: String) {
        _countedInput.value = text
    }

    /** Settles with the typed amount. Does nothing without a valid amount, outside owner mode, or while saving. */
    fun settle(shiftId: Long, onDone: () -> Unit) {
        val counted = parseMadToCentimes(_countedInput.value) ?: return
        if (!ownerSession.isOwnerMode.value || _isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                if (shiftRepository.settle(shiftId, counted)) onDone()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
