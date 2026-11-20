package com.example.qahwamanager.security

import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OwnerSessionTest {

    private fun TestScope.session(): OwnerSession {
        val session = OwnerSession(testClock(1_700_000_000_000L))
        backgroundScope.launch { session.enforceIdleTimeout() }
        runCurrent()
        return session
    }

    @Test
    fun newSession_startsLocked() = runTest {
        assertFalse(session().isOwnerMode.value)
    }

    @Test
    fun unlock_thenIdle119s_stillOwner() = runTest {
        val session = session()
        session.unlock()
        runCurrent()

        advanceTimeBy(119_000)
        runCurrent()

        assertTrue(session.isOwnerMode.value)
    }

    @Test
    fun unlock_thenIdle120s_locks() = runTest {
        val session = session()
        session.unlock()
        runCurrent()

        advanceTimeBy(120_000)
        runCurrent()

        assertFalse(session.isOwnerMode.value)
    }

    @Test
    fun touchAt60s_extendsTo180s() = runTest {
        val session = session()
        session.unlock()
        runCurrent()
        advanceTimeBy(60_000)
        session.touch()

        advanceTimeBy(119_000)
        runCurrent()
        assertTrue("owner mode holds at 179 s", session.isOwnerMode.value)

        advanceTimeBy(1_000)
        runCurrent()
        assertFalse("locked at 180 s", session.isOwnerMode.value)
    }

    @Test
    fun lock_isImmediate() = runTest {
        val session = session()
        session.unlock()
        runCurrent()

        session.lock()

        assertFalse(session.isOwnerMode.value)
        advanceTimeBy(200_000)
        runCurrent()
        assertFalse(session.isOwnerMode.value)
    }

    @Test
    fun unlockAgainAfterLock_restartsTheTimer() = runTest {
        val session = session()
        session.unlock()
        runCurrent()
        advanceTimeBy(100_000)
        session.lock()
        runCurrent()

        session.unlock()
        runCurrent()
        advanceTimeBy(119_000)
        runCurrent()

        assertTrue(session.isOwnerMode.value)
    }
}
