package com.example.qahwamanager.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.cloud.AlwaysReadyCloudSession
import com.example.qahwamanager.data.cloud.CloudSessionController
import com.example.qahwamanager.data.repository.OwnerAuthRepository
import com.example.qahwamanager.security.RecoveryCode
import com.example.qahwamanager.security.isValidPin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RootScreen { LOADING, CLOUD_AUTH, CAFE_WORKSPACE, SETUP, APP }

enum class SetupError { BLANK_NAME, INVALID_PIN, PIN_MISMATCH }

/**
 * Decides what the app shows at its root:
 * 1. Cloud sign-in (`CLOUD_AUTH`)
 * 2. Create or join a coffee shop (`CAFE_WORKSPACE`)
 * 3. Local 4-digit counter PIN setup (`SETUP`)
 * 4. Main app (`APP`) with live Firestore sync running
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val ownerAuthRepository: OwnerAuthRepository,
    private val cloudSession: CloudSessionController = AlwaysReadyCloudSession,
) : ViewModel() {

    private val _recoveryCodeToShow = MutableStateFlow<String?>(null)
    /** The freshly generated recovery code, formatted, until the owner acknowledges it. */
    val recoveryCodeToShow: StateFlow<String?> = _recoveryCodeToShow.asStateFlow()

    private val settingUp = MutableStateFlow(false)
    private val checkingCloudWorkspace = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            cloudSession.observeCurrentUser()
                .distinctUntilChanged()
                .collect { user ->
                    if (user == null) {
                        cloudSession.stopSync()
                    } else if (cloudSession.activeCafe.value == null) {
                        checkingCloudWorkspace.value = true
                        try {
                            runCatching { cloudSession.refreshActiveCafeFromCloud() }
                        } finally {
                            checkingCloudWorkspace.value = false
                        }
                    } else {
                        cloudSession.startSyncForCafe(cloudSession.activeCafe.value!!.cafeId)
                    }
                }
        }
        viewModelScope.launch {
            cloudSession.activeCafe.collect { cafe ->
                if (cafe != null && cloudSession.currentUser() != null) {
                    cloudSession.startSyncForCafe(cafe.cafeId)
                }
            }
        }
    }

    val screen: StateFlow<RootScreen> = combine(
        cloudSession.observeCurrentUser(),
        cloudSession.activeCafe,
        checkingCloudWorkspace,
        combine(ownerAuthRepository.observeOwnerExists(), _recoveryCodeToShow, settingUp) { exists, code, busy ->
            Triple(exists, code, busy)
        },
    ) { cloudUser, activeCafe, checkingWorkspace, (exists, code, busy) ->
        when {
            cloudUser == null -> RootScreen.CLOUD_AUTH
            checkingWorkspace -> RootScreen.LOADING
            activeCafe == null -> RootScreen.CAFE_WORKSPACE
            !exists || code != null || busy -> RootScreen.SETUP
            else -> RootScreen.APP
        }
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
