package com.example.qahwamanager.security

import org.junit.Assert.assertEquals
import org.junit.Test

class PinLockoutTest {
    @Test
    fun freeAttempts_haveNoLock() {
        for (n in 0..4) assertEquals("failures=$n", 0L, PinLockout.lockDurationMillis(n))
        assertEquals(5, PinLockout.FREE_ATTEMPTS)
    }

    @Test
    fun fifthFailure_locksOneMinute_thenDoubles() {
        assertEquals(60_000L, PinLockout.lockDurationMillis(5))
        assertEquals(120_000L, PinLockout.lockDurationMillis(6))
        assertEquals(240_000L, PinLockout.lockDurationMillis(7))
    }

    @Test
    fun lock_isCappedAtOneHour() {
        assertEquals(3_600_000L, PinLockout.lockDurationMillis(20))
        assertEquals(3_600_000L, PinLockout.lockDurationMillis(100))
    }
}
