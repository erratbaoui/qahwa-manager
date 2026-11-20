package com.example.qahwamanager.ui.auth

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.qahwamanager.security.OwnerSession

/**
 * Asks for the owner PIN before an owner-only action. Wrap the *final* action (save, delete,
 * confirm), not the opening of a form: if owner mode expired while the form was open, the PIN is
 * asked again and the form isn't lost. While [pending] is non-null the UI shows the PIN dialog.
 */
@Stable
class OwnerGate(private val session: OwnerSession) {

    var pending: (() -> Unit)? by mutableStateOf(null)
        private set

    /** Runs [action] now in owner mode; otherwise holds it until [onVerified]. */
    fun require(action: () -> Unit) {
        if (session.isOwnerMode.value) action() else pending = action
    }

    /** The PIN checked out: owner mode turns on and the held action runs exactly once. */
    fun onVerified() {
        session.unlock()
        val action = pending
        pending = null
        action?.invoke()
    }

    fun onDismissed() {
        pending = null
    }
}

val LocalOwnerGate = staticCompositionLocalOf<OwnerGate> { error("OwnerGate not provided") }
