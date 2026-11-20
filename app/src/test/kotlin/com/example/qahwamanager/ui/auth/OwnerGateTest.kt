package com.example.qahwamanager.ui.auth

import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnerGateTest {

    @Test
    fun require_inOwnerMode_runsNow() = runTest {
        val session = OwnerSession(testClock(0))
        session.unlock()
        val gate = OwnerGate(session)
        var runs = 0

        gate.require { runs++ }

        assertEquals(1, runs)
        assertNull(gate.pending)
    }

    @Test
    fun require_locked_storesPending_andDoesNotRun() = runTest {
        val gate = OwnerGate(OwnerSession(testClock(0)))
        var runs = 0

        gate.require { runs++ }

        assertEquals(0, runs)
        assertNotNull(gate.pending)
    }

    @Test
    fun gate_runsPendingActionOnceAfterVerify() = runTest {
        val session = OwnerSession(testClock(0))
        val gate = OwnerGate(session)
        var runs = 0
        gate.require { runs++ }

        gate.onVerified()
        gate.onVerified()

        assertEquals(1, runs)
        assertTrue(session.isOwnerMode.value)
        assertNull(gate.pending)
    }

    @Test
    fun onDismissed_dropsPending() = runTest {
        val session = OwnerSession(testClock(0))
        val gate = OwnerGate(session)
        var runs = 0
        gate.require { runs++ }

        gate.onDismissed()
        gate.onVerified()

        assertEquals(0, runs)
        assertNull(gate.pending)
        assertFalse("dismissing never unlocks by itself", runs > 0)
    }
}
