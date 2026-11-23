package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.cloud.NoOpSyncPublisher
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.example.qahwamanager.data.db.LoyaltyCustomerDao
import com.example.qahwamanager.data.model.LoyaltyConfig
import com.example.qahwamanager.data.model.LoyaltyCustomer
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoyaltyRepository @Inject constructor(
    private val loyaltyDao: LoyaltyCustomerDao,
    private val clock: Clock,
    private val syncPublisher: SyncPublisher = NoOpSyncPublisher,
) {
    private val _config = MutableStateFlow(LoyaltyConfig())
    val config: StateFlow<LoyaltyConfig> = _config.asStateFlow()

    fun observeCustomers(): Flow<List<LoyaltyCustomer>> = loyaltyDao.observeAll()

    fun updateConfig(
        stampsRequired: Int,
        rewardTitle: String,
        googleReviewUrl: String,
        wifiSsid: String = _config.value.wifiSsid,
        wifiPassword: String = _config.value.wifiPassword,
        cafeName: String = _config.value.cafeName,
    ) {
        val coercedStamps = stampsRequired.coerceIn(1, 50)
        val cleanedReward = rewardTitle.trim().ifEmpty { "1 Free Coffee" }
        val updated = LoyaltyConfig(
            cafeName = cafeName.trim(),
            stampsRequired = coercedStamps,
            rewardTitle = cleanedReward,
            googleReviewUrl = googleReviewUrl.trim(),
            wifiSsid = wifiSsid.trim(),
            wifiPassword = wifiPassword.trim(),
            updatedAt = clock.nowMillis(),
        )
        _config.value = updated
        syncPublisher.pushLoyaltyConfig(updated)
    }

    fun applyRemoteConfig(remote: LoyaltyConfig) {
        if (remote.updatedAt > _config.value.updatedAt) {
            _config.value = remote
        }
    }

    /** Enrolls a new customer or returns the existing one matching [phone] (and adds +1 initial stamp). */
    suspend fun enrollCustomer(name: String, phone: String, addInitialStamp: Boolean = true): LoyaltyCustomer {
        val cleanedName = name.trim()
        val normalizedPhone = normalizeMoroccanPhone(phone)
        require(cleanedName.isNotEmpty()) { "Customer name is blank" }
        require(normalizedPhone.length >= 8) { "Invalid phone number" }

        val existing = loyaltyDao.getByPhone(normalizedPhone)
        val now = clock.nowMillis()
        if (existing != null) {
            return if (addInitialStamp) {
                addStamp(existing.id) ?: existing
            } else {
                existing
            }
        }

        val initialStamps = if (addInitialStamp) 1 else 0
        val created = LoyaltyCustomer(
            name = cleanedName,
            phone = normalizedPhone,
            currentStamps = initialStamps,
            lifetimeStamps = initialStamps,
            rewardsRedeemed = 0,
            lastVisitAt = now,
            createdAt = now,
            remoteId = deterministicRemoteId(normalizedPhone),
            updatedAt = now,
        )
        val id = loyaltyDao.insert(created)
        syncPublisher.pushLoyaltyCustomer(id)
        return created.copy(id = id)
    }

    /** Adds [count] stamp(s) to the customer's card and updates their last visit timestamp. */
    suspend fun addStamp(customerId: Long, count: Int = 1): LoyaltyCustomer? {
        require(count > 0) { "Stamp count must be positive" }
        val existing = loyaltyDao.getById(customerId) ?: return null
        val now = clock.nowMillis()
        val updated = existing.copy(
            currentStamps = (existing.currentStamps + count).coerceAtMost(100),
            lifetimeStamps = existing.lifetimeStamps + count,
            lastVisitAt = now,
            updatedAt = now,
        )
        loyaltyDao.update(updated)
        syncPublisher.pushLoyaltyCustomer(customerId)
        return updated
    }

    /**
     * Redeems one reward when the customer has reached [LoyaltyConfig.stampsRequired].
     * Deducts the required stamps and increments [LoyaltyCustomer.rewardsRedeemed].
     */
    suspend fun redeemReward(customerId: Long): LoyaltyCustomer? {
        val existing = loyaltyDao.getById(customerId) ?: return null
        val needed = _config.value.stampsRequired
        if (existing.currentStamps < needed) return null
        val now = clock.nowMillis()
        val updated = existing.copy(
            currentStamps = (existing.currentStamps - needed).coerceAtLeast(0),
            rewardsRedeemed = existing.rewardsRedeemed + 1,
            lastVisitAt = now,
            updatedAt = now,
        )
        loyaltyDao.update(updated)
        syncPublisher.pushLoyaltyCustomer(customerId)
        return updated
    }

    /**
     * Stamps a customer by scanned QR payload (`qahwa://loyalty/{remoteId}` or raw `remoteId`)
     * or by phone number, adding [count] stamp(s).
     */
    suspend fun stampByCodeOrPhone(rawInput: String, count: Int = 1): LoyaltyCustomer? {
        val safeCount = count.coerceIn(1, 50)
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null
        val afterScheme = trimmed.substringAfter("qahwa://loyalty/", missingDelimiterValue = trimmed)
        val extractedId = afterScheme
            .substringAfter("customer=", missingDelimiterValue = afterScheme)
            .substringBefore("&")
            .trim()
        val byRemote = loyaltyDao.getByRemoteId(extractedId)?.takeIf { !it.isDeleted }
        if (byRemote != null) {
            return addStamp(byRemote.id, safeCount)
        }
        val byPhone = loyaltyDao.getByPhone(normalizeMoroccanPhone(trimmed))
        if (byPhone != null) {
            return addStamp(byPhone.id, safeCount)
        }
        return null
    }

    suspend fun deleteCustomer(customerId: Long) {
        val existing = loyaltyDao.getById(customerId) ?: return
        val tombstoned = existing.copy(isDeleted = true, updatedAt = clock.nowMillis())
        loyaltyDao.update(tombstoned)
        syncPublisher.pushLoyaltyCustomer(customerId)
    }

    companion object {
        const val INACTIVE_THRESHOLD_DAYS = 15L
        const val INACTIVE_THRESHOLD_MS = INACTIVE_THRESHOLD_DAYS * 24L * 60L * 60L * 1000L

        /** Normalizes Moroccan phone numbers (`06...` / `07...` -> `+2126...` / `+2127...`). */
        fun normalizeMoroccanPhone(raw: String): String {
            val digitsAndPlus = raw.filter { it.isDigit() || it == '+' }
            return when {
                digitsAndPlus.startsWith("06") && digitsAndPlus.length == 10 ->
                    "+212" + digitsAndPlus.drop(1)
                digitsAndPlus.startsWith("07") && digitsAndPlus.length == 10 ->
                    "+212" + digitsAndPlus.drop(1)
                digitsAndPlus.startsWith("212") && !digitsAndPlus.startsWith("+") ->
                    "+$digitsAndPlus"
                else -> digitsAndPlus
            }
        }

        /** Deterministic document ID (`phone_<digits>`) shared by the Android POS and Table QR Web App. */
        fun deterministicRemoteId(normalizedPhone: String): String =
            "phone_" + normalizedPhone.filter { it.isDigit() }
    }
}
