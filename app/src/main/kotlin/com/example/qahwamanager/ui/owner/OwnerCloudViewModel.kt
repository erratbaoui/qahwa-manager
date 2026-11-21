package com.example.qahwamanager.ui.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.cloud.CafeInvite
import com.example.qahwamanager.data.cloud.CafeWorkspace
import com.example.qahwamanager.data.cloud.CafeWorkspaceRepository
import com.example.qahwamanager.data.cloud.FirestoreSyncEngine
import com.example.qahwamanager.data.cloud.SyncStatus
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OwnerCloudViewModel @Inject constructor(
    private val workspaceRepository: CafeWorkspaceRepository,
    syncEngine: FirestoreSyncEngine,
    private val ownerSession: OwnerSession,
) : ViewModel() {

    val activeCafe: StateFlow<CafeWorkspace?> = workspaceRepository.activeCafe
    val syncStatus: StateFlow<SyncStatus> = syncEngine.syncStatus

    private val _invite = MutableStateFlow<CafeInvite?>(null)
    val invite: StateFlow<CafeInvite?> = _invite.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun generateInviteCode() {
        if (!ownerSession.isOwnerMode.value || _isBusy.value) return
        _isBusy.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching { workspaceRepository.generateInviteCode() }
                .onSuccess { _invite.value = it }
                .onFailure { _error.value = it.localizedMessage ?: "Failed to create invite code" }
            _isBusy.value = false
        }
    }

    fun dismissInvite() {
        _invite.value = null
        _error.value = null
    }

    fun signOut() {
        if (!ownerSession.isOwnerMode.value || _isBusy.value) return
        _isBusy.value = true
        viewModelScope.launch {
            try {
                ownerSession.lock()
                workspaceRepository.signOutAndClearLocal()
            } finally {
                _isBusy.value = false
            }
        }
    }
}
