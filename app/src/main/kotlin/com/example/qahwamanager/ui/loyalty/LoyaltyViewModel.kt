package com.example.qahwamanager.ui.loyalty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.cloud.CafeWorkspaceRepository
import com.example.qahwamanager.data.model.LoyaltyConfig
import com.example.qahwamanager.data.model.LoyaltyCustomer
import com.example.qahwamanager.data.repository.LoyaltyRepository
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.nfc.NfcStandWriter
import com.example.qahwamanager.nfc.NfcWriteState
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoyaltyUiState(
    val cafeName: String = "Qahwa Café",
    val config: LoyaltyConfig = LoyaltyConfig(),
    val allCustomers: List<LoyaltyCustomer> = emptyList(),
    val filteredCustomers: List<LoyaltyCustomer> = emptyList(),
    val inactiveCustomers: List<LoyaltyCustomer> = emptyList(),
    val totalStampsIssued: Int = 0,
    val totalRewardsRedeemed: Int = 0,
    val standUrl: String = "",
    val printStudioUrl: String = "",
    val query: String = "",
    val showInactiveOnly: Boolean = false,
)

@HiltViewModel
class LoyaltyViewModel @Inject constructor(
    private val loyaltyRepository: LoyaltyRepository,
    workspaceRepository: CafeWorkspaceRepository,
    private val nfcStandWriter: NfcStandWriter,
    private val ownerSession: OwnerSession,
    private val clock: Clock,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _showInactiveOnly = MutableStateFlow(false)
    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    val isOwnerMode: StateFlow<Boolean> = ownerSession.isOwnerMode
    val nfcState: StateFlow<NfcWriteState> = nfcStandWriter.state

    val uiState: StateFlow<LoyaltyUiState> = combine(
        loyaltyRepository.config,
        loyaltyRepository.observeCustomers(),
        workspaceRepository.activeCafe,
        _query,
        _showInactiveOnly,
    ) { config, customers, cafe, query, inactiveOnly ->
        val now = clock.nowMillis()
        val inactiveCutoff = now - LoyaltyRepository.INACTIVE_THRESHOLD_MS
        val inactive = customers.filter { it.lastVisitAt < inactiveCutoff }
        val baseList = if (inactiveOnly) inactive else customers
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) {
            baseList
        } else {
            baseList.filter {
                it.name.lowercase().contains(q) ||
                    it.phone.lowercase().contains(q) ||
                    it.remoteId.lowercase().contains(q)
            }
        }
        val cafeId = cafe?.cafeId ?: "default"
        val cafeName = cafe?.name?.ifBlank { "Qahwa Café" } ?: "Qahwa Café"
        val standUrl = "https://qahwa-manager-pos-2026.web.app/?cafe=$cafeId"
        val printStudioUrl = "https://qahwa-manager-pos-2026.web.app/print.html?cafe=$cafeId&stamps=${config.stampsRequired}"
        LoyaltyUiState(
            cafeName = cafeName,
            config = config,
            allCustomers = customers,
            filteredCustomers = filtered,
            inactiveCustomers = inactive,
            totalStampsIssued = customers.sumOf { it.lifetimeStamps },
            totalRewardsRedeemed = customers.sumOf { it.rewardsRedeemed },
            standUrl = standUrl,
            printStudioUrl = printStudioUrl,
            query = query,
            showInactiveOnly = inactiveOnly,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LoyaltyUiState(),
    )

    fun setQuery(query: String) {
        _query.value = query
    }

    fun setShowInactiveOnly(inactiveOnly: Boolean) {
        _showInactiveOnly.value = inactiveOnly
    }

    fun enrollCustomer(name: String, phone: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                loyaltyRepository.enrollCustomer(name, phone, addInitialStamp = true)
            }.onSuccess {
                onDone()
            }
        }
    }

    fun addStamp(customerId: Long) {
        viewModelScope.launch {
            loyaltyRepository.addStamp(customerId)
        }
    }

    fun redeemReward(customerId: Long) {
        viewModelScope.launch {
            loyaltyRepository.redeemReward(customerId)
        }
    }

    fun stampByCodeOrPhone(codeOrPhone: String, onMatched: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val matched = loyaltyRepository.stampByCodeOrPhone(codeOrPhone)
            onMatched(matched != null)
        }
    }

    fun updateProgramSettings(
        stampsRequired: Int,
        rewardTitle: String,
        googleReviewUrl: String,
        wifiSsid: String = uiState.value.config.wifiSsid,
        wifiPassword: String = uiState.value.config.wifiPassword,
        onDone: () -> Unit,
    ) {
        if (!ownerSession.isOwnerMode.value) return
        loyaltyRepository.updateConfig(
            stampsRequired = stampsRequired,
            rewardTitle = rewardTitle,
            googleReviewUrl = googleReviewUrl,
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
            cafeName = uiState.value.cafeName,
        )
        onDone()
    }

    fun deleteCustomer(customerId: Long) {
        if (!ownerSession.isOwnerMode.value) return
        viewModelScope.launch {
            loyaltyRepository.deleteCustomer(customerId)
        }
    }

    fun startWritingNfcStand() {
        if (!ownerSession.isOwnerMode.value) return
        val url = uiState.value.standUrl
        if (url.isNotBlank()) {
            nfcStandWriter.startWriting(url)
        }
    }

    fun cancelNfcWrite() {
        nfcStandWriter.cancel()
    }
}
