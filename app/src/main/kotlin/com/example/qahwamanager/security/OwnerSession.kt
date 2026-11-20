package com.example.qahwamanager.security

import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owner mode, held in memory only: a fresh process starts locked. It ends on [lock] (the banner's
 * Lock button or the app going to the background) or after [IDLE_TIMEOUT_MS] without a touch.
 */
@Singleton
class OwnerSession @Inject constructor(private val clock: Clock) {

    companion object {
        const val IDLE_TIMEOUT_MS = 120_000L
    }

    private val _isOwnerMode = MutableStateFlow(false)
    val isOwnerMode: StateFlow<Boolean> = _isOwnerMode.asStateFlow()

    @Volatile
    private var lastTouch = 0L

    fun unlock() {
        lastTouch = clock.nowMillis()
        _isOwnerMode.value = true
    }

    fun lock() {
        _isOwnerMode.value = false
    }

    fun touch() {
        lastTouch = clock.nowMillis()
    }

    /** Runs forever: while unlocked, waits for the idle deadline and locks if no newer touch moved it. */
    suspend fun enforceIdleTimeout(): Nothing {
        _isOwnerMode.collectLatest { unlocked ->
            if (!unlocked) return@collectLatest
            while (true) {
                val remaining = lastTouch + IDLE_TIMEOUT_MS - clock.nowMillis()
                if (remaining <= 0) {
                    lock()
                    return@collectLatest
                }
                delay(remaining)
            }
        }
        awaitCancellation()
    }
}
