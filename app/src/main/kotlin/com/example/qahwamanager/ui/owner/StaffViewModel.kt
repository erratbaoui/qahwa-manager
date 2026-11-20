package com.example.qahwamanager.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.repository.StaffRepository
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StaffError { BLANK_NAME, OWNER_ALWAYS_ACTIVE }

/** Owner-only staff management: add, rename, deactivate/reactivate. Nobody is deleted. */
@HiltViewModel
class StaffViewModel @Inject constructor(
    private val staffRepository: StaffRepository,
    private val ownerSession: OwnerSession
) : ViewModel() {

    val staff: StateFlow<List<Staff>> = staffRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<StaffError?>(null)
    val error: StateFlow<StaffError?> = _error.asStateFlow()

    fun add(name: String) = ownerOnly { staffRepository.addWaiter(name) }

    fun rename(id: Long, name: String) = ownerOnly { staffRepository.rename(id, name) }

    fun setActive(id: Long, active: Boolean) = ownerOnly { staffRepository.setActive(id, active) }

    fun clearError() {
        _error.value = null
    }

    private fun ownerOnly(block: suspend () -> Unit) {
        if (!ownerSession.isOwnerMode.value) return
        viewModelScope.launch {
            try {
                block()
                _error.value = null
            } catch (e: IllegalArgumentException) {
                _error.value = if (e.message?.contains("owner", ignoreCase = true) == true) StaffError.OWNER_ALWAYS_ACTIVE else StaffError.BLANK_NAME
            }
        }
    }
}
