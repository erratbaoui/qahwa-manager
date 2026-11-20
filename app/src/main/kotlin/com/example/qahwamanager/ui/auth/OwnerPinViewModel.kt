package com.example.qahwamanager.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.repository.OwnerAuthRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.security.PinCheck
import com.example.qahwamanager.security.RecoveryCode
import com.example.qahwamanager.security.isValidPin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinInputError { INVALID_PIN, PIN_MISMATCH }

/**
 * [error] is the outcome of the last check against the stored PIN or recovery code; [inputError]
 * is a problem with what was typed (caught before anything is checked); [busy] while a check runs.
 */
data class PinUiState(
    val error: PinCheck? = null,
    val busy: Boolean = false,
    val inputError: PinInputError? = null,
    /** A freshly issued recovery code (formatted), shown until the owner acknowledges it. */
    val newRecoveryCode: String? = null
)

@HiltViewModel
class OwnerPinViewModel @Inject constructor(
    private val ownerAuthRepository: OwnerAuthRepository,
    private val ownerSession: OwnerSession
) : ViewModel() {

    private val _state = MutableStateFlow(PinUiState())
    val state: StateFlow<PinUiState> = _state.asStateFlow()

    /** Called when the dialog opens: clears stale input errors and shows a running lockout right away. */
    fun onOpened() {
        _state.update { it.copy(inputError = null) }
        viewModelScope.launch {
            val status = ownerAuthRepository.attemptStatus()
            _state.update { it.copy(error = if (status == PinCheck.Ok) null else status) }
        }
    }

    fun submit(pin: String, onOk: () -> Unit) = check {
        when (val result = ownerAuthRepository.checkPin(pin)) {
            PinCheck.Ok -> {
                ownerSession.unlock()
                _state.update { it.copy(error = null) }
                onOk()
            }
            else -> _state.update { it.copy(error = result) }
        }
    }

    /** Resets the PIN from the recovery code; on success owner mode turns on and [onNewCode] gets the formatted new code. */
    fun resetWithCode(code: String, newPin: String, newPinConfirm: String, onNewCode: (String) -> Unit) {
        validateNewPin(newPin, newPinConfirm) ?: return
        check {
            val fresh = ownerAuthRepository.resetPinWithRecoveryCode(code, newPin)
            if (fresh == null) {
                _state.update { it.copy(error = ownerAuthRepository.attemptStatus()) }
            } else {
                ownerSession.unlock()
                val formatted = RecoveryCode.format(fresh)
                _state.update { it.copy(error = null, newRecoveryCode = formatted) }
                onNewCode(formatted)
            }
        }
    }

    /** Owner-area action: spend the current recovery code and show a new one. Does nothing outside owner mode. */
    fun issueNewRecoveryCode() {
        if (!ownerSession.isOwnerMode.value) return
        check {
            val fresh = ownerAuthRepository.issueNewRecoveryCode()
            _state.update { it.copy(newRecoveryCode = RecoveryCode.format(fresh)) }
        }
    }

    fun acknowledgeRecoveryCode() {
        _state.update { it.copy(newRecoveryCode = null) }
    }

    /** Changes the PIN; the recovery code stays the same. */
    fun changePin(current: String, new: String, confirm: String, onDone: () -> Unit) {
        validateNewPin(new, confirm) ?: return
        check {
            when (val result = ownerAuthRepository.changePin(current, new)) {
                PinCheck.Ok -> {
                    _state.update { it.copy(error = null) }
                    onDone()
                }
                else -> _state.update { it.copy(error = result) }
            }
        }
    }

    /** Returns Unit when the new PIN is usable, null (with [PinUiState.inputError] set) otherwise. */
    private fun validateNewPin(pin: String, confirm: String): Unit? {
        val problem = when {
            !isValidPin(pin) -> PinInputError.INVALID_PIN
            pin != confirm -> PinInputError.PIN_MISMATCH
            else -> null
        }
        _state.update { it.copy(inputError = problem) }
        return if (problem == null) Unit else null
    }

    /** Runs one check at a time; taps that arrive while one is in flight are ignored. */
    private fun check(block: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, inputError = null) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
