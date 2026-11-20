package com.example.qahwamanager.data.repository

import com.example.qahwamanager.data.db.OwnerDao
import com.example.qahwamanager.data.db.ShiftDao
import com.example.qahwamanager.data.model.OwnerCredentials
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.di.HashingDispatcher
import com.example.qahwamanager.security.PinCheck
import com.example.qahwamanager.security.PinHasher
import com.example.qahwamanager.security.PinLockout
import com.example.qahwamanager.security.RecoveryCode
import com.example.qahwamanager.security.isValidPin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The owner's PIN and recovery code. Secrets are stored only as salted hashes. The lockout state is
 * persisted with them, so it survives the app being killed, and every failed attempt is also
 * counted on the active shift: a waiter guessing the owner's PIN is itself worth seeing.
 */
@Singleton
class OwnerAuthRepository @Inject constructor(
    private val ownerDao: OwnerDao,
    private val shiftDao: ShiftDao,
    private val clock: Clock,
    @HashingDispatcher private val hashingDispatcher: CoroutineDispatcher
) {
    fun observeOwnerExists(): Flow<Boolean> = ownerDao.observeExists()

    /** First-run setup. Returns the raw recovery code, to be shown once. */
    suspend fun setUpOwner(name: String, pin: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Owner name is blank" }
        require(isValidPin(pin)) { "PIN must be 4 digits" }
        check(ownerDao.getCredentials() == null) { "Owner already set up" }

        val code = RecoveryCode.generate()
        val credentials = withContext(hashingDispatcher) {
            val pinSalt = PinHasher.newSalt()
            val recoverySalt = PinHasher.newSalt()
            OwnerCredentials(
                pinHash = PinHasher.hash(pin, pinSalt),
                pinSalt = pinSalt,
                recoveryHash = PinHasher.hash(code, recoverySalt),
                recoverySalt = recoverySalt,
            )
        }
        ownerDao.createOwner(Staff(name = trimmed, isOwner = true, createdAt = clock.nowMillis()), credentials)
        return code
    }

    suspend fun checkPin(pin: String): PinCheck {
        val credentials = requireCredentials()
        lockedOut(credentials)?.let { return it }
        if (!isValidPin(pin) || !matches(pin, credentials.pinSalt, credentials.pinHash)) {
            return recordFailure(credentials)
        }
        clearFailures(credentials)
        return PinCheck.Ok
    }

    /**
     * Sets a new PIN from the recovery code and returns a fresh recovery code (the old one is
     * spent). Returns null when the code is wrong, which counts as a failed attempt, or during a
     * lockout.
     */
    suspend fun resetPinWithRecoveryCode(code: String, newPin: String): String? {
        require(isValidPin(newPin)) { "PIN must be 4 digits" }
        val credentials = requireCredentials()
        if (lockedOut(credentials) != null) return null
        val normalized = RecoveryCode.normalize(code)
        if (normalized.length != RecoveryCode.LENGTH ||
            !matches(normalized, credentials.recoverySalt, credentials.recoveryHash)
        ) {
            recordFailure(credentials)
            return null
        }
        val newCode = RecoveryCode.generate()
        val updated = withContext(hashingDispatcher) {
            val pinSalt = PinHasher.newSalt()
            val recoverySalt = PinHasher.newSalt()
            credentials.copy(
                pinHash = PinHasher.hash(newPin, pinSalt),
                pinSalt = pinSalt,
                recoveryHash = PinHasher.hash(newCode, recoverySalt),
                recoverySalt = recoverySalt,
                failedAttemptsInRow = 0,
                lockedUntil = 0,
            )
        }
        ownerDao.upsertCredentials(updated)
        return newCode
    }

    /** Changes the PIN once [currentPin] checks out. The recovery code is kept. */
    suspend fun changePin(currentPin: String, newPin: String): PinCheck {
        require(isValidPin(newPin)) { "PIN must be 4 digits" }
        val result = checkPin(currentPin)
        if (result != PinCheck.Ok) return result
        val credentials = requireCredentials()
        val updated = withContext(hashingDispatcher) {
            val pinSalt = PinHasher.newSalt()
            credentials.copy(pinHash = PinHasher.hash(newPin, pinSalt), pinSalt = pinSalt)
        }
        ownerDao.upsertCredentials(updated)
        return PinCheck.Ok
    }

    /**
     * Where the owner stands without checking anything: [PinCheck.LockedOut] during a lockout,
     * [PinCheck.Wrong] with the tries left after earlier failures, otherwise [PinCheck.Ok].
     */
    suspend fun attemptStatus(): PinCheck {
        val credentials = requireCredentials()
        lockedOut(credentials)?.let { return it }
        val failures = credentials.failedAttemptsInRow
        return if (failures == 0) PinCheck.Ok else PinCheck.Wrong(PinLockout.FREE_ATTEMPTS - failures)
    }

    private suspend fun requireCredentials(): OwnerCredentials =
        checkNotNull(ownerDao.getCredentials()) { "Owner not set up" }

    private fun lockedOut(credentials: OwnerCredentials): PinCheck.LockedOut? =
        if (credentials.lockedUntil > clock.nowMillis()) PinCheck.LockedOut(credentials.lockedUntil) else null

    private suspend fun matches(secret: String, salt: String, hash: String): Boolean =
        withContext(hashingDispatcher) { PinHasher.matches(secret, salt, hash) }

    private suspend fun recordFailure(credentials: OwnerCredentials): PinCheck {
        val failures = credentials.failedAttemptsInRow + 1
        val lockMillis = PinLockout.lockDurationMillis(failures)
        val lockedUntil = if (lockMillis > 0) clock.nowMillis() + lockMillis else 0L
        ownerDao.upsertCredentials(credentials.copy(failedAttemptsInRow = failures, lockedUntil = lockedUntil))
        shiftDao.recordFailedOwnerPinOnActive()
        return if (lockMillis > 0) PinCheck.LockedOut(lockedUntil)
        else PinCheck.Wrong(attemptsBeforeLock = PinLockout.FREE_ATTEMPTS - failures)
    }

    private suspend fun clearFailures(credentials: OwnerCredentials) {
        if (credentials.failedAttemptsInRow != 0 || credentials.lockedUntil != 0L) {
            ownerDao.upsertCredentials(credentials.copy(failedAttemptsInRow = 0, lockedUntil = 0))
        }
    }
}
