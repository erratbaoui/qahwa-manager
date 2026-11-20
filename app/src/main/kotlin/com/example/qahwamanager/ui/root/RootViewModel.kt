package com.example.qahwamanager.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.repository.OwnerAuthRepository
import com.example.qahwamanager.security.RecoveryCode
import com.example.qahwamanager.security.isValidPin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RootScreen { LOADING, SETUP, APP }

enum class SetupError { BLANK_NAME, INVALID_PIN, PIN_MISMATCH }

/** Decides what the app shows at its root: owner setup until an owner exists, then the app. */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val ownerAuthRepository: OwnerAuthRepository
) : ViewModel() {

    private val _recoveryCodeToShow = MutableStateFlow<String?>(null)
    /** The freshly generated recovery code, formatted, until the owner acknowledges it. */
    val recoveryCodeToShow: StateFlow<String?> = _recoveryCodeToShow.asStateFlow()

    private val settingUp = MutableStateFlow(false)

    val screen: StateFlow<RootScreen> = combine(
        ownerAuthRepository.observeOwnerExists(), _recoveryCodeToShow, settingUp,
    ) { exists, code, busy ->
        // Setup stays on screen while the code is being shown, even though the owner now exists.
        if (!exists || code != null || busy) RootScreen.SETUP else RootScreen.APP
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RootScreen.LOADING)

    /** Validates synchronously and returns the first problem; null means setup is under way. */
    fun setUp(name: String, pin: String, pinConfirm: String): SetupError? {
        if (name.isBlank()) return SetupError.BLANK_NAME
        if (!isValidPin(pin)) return SetupError.INVALID_PIN
        if (pin != pinConfirm) return SetupError.PIN_MISMATCH
        if (settingUp.value) return null
        settingUp.value = true
        viewModelScope.launch {
            try {
                val code = ownerAuthRepository.setUpOwner(name, pin)
                _recoveryCodeToShow.value = RecoveryCode.format(code)
            } catch (e: IllegalStateException) {
                // An owner already exists (a double tap raced us); the app just opens.
            } finally {
                settingUp.value = false
            }
        }
        return null
    }

    fun acknowledgeRecoveryCode() {
        _recoveryCodeToShow.value = null
    }
}
