package com.example.qahwamanager.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.qahwamanager.ui.theme.QahwaTheme

/** Shows a freshly issued recovery code. Can't be dismissed by tapping outside: the owner must acknowledge it. */
@Composable
fun RecoveryCodeDialog(code: String, onDone: () -> Unit) {
    val qColors = QahwaTheme.colors
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = qColors.background,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier
                .widthIn(max = 400.dp)
                .resetsOwnerIdleTimer(),
        ) {
            RecoveryCodeCard(code = code, onDone = onDone, modifier = Modifier.padding(24.dp))
        }
    }
}
