package com.example.qahwamanager.security

/**
 * Wrong-PIN policy: [FREE_ATTEMPTS] tries in a row cost nothing, the next locks the pad for a
 * minute, and every further failure doubles the lock, up to an hour.
 */
object PinLockout {
    const val FREE_ATTEMPTS = 5
    private const val FIRST_LOCK_MS = 60_000L
    private const val MAX_LOCK_MS = 3_600_000L

    fun lockDurationMillis(failuresInRow: Int): Long {
        if (failuresInRow < FREE_ATTEMPTS) return 0
        val doublings = minOf(failuresInRow - FREE_ATTEMPTS, 6)
        return minOf(FIRST_LOCK_MS shl doublings, MAX_LOCK_MS)
    }
}

/** The outcome of checking the owner's PIN (or recovery code). */
sealed interface PinCheck {
    data object Ok : PinCheck
    /** Wrong, with how many more wrong tries are allowed before the pad locks. */
    data class Wrong(val attemptsBeforeLock: Int) : PinCheck
    /** Locked until [untilMillis] (epoch millis). A correct PIN is refused until then. */
    data class LockedOut(val untilMillis: Long) : PinCheck
}
