package com.example.qahwamanager.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Called on every touch so owner mode's idle timer restarts. MainNavigation provides it; the default is a no-op. */
val LocalOwnerTouch = staticCompositionLocalOf<() -> Unit> { {} }

/**
 * Compose dialogs live in their own window, so the Activity's dispatchTouchEvent never sees taps
 * inside them. Put this on a dialog's surface so typing a PIN or a name still counts as activity.
 */
@Composable
fun Modifier.resetsOwnerIdleTimer(): Modifier {
    val touch = LocalOwnerTouch.current
    return pointerInput(touch) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                touch()
            }
        }
    }
}
