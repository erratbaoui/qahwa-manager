package com.example.qahwamanager.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.theme.QahwaTheme

/** A thin bar at the top of every screen while owner mode is on, with the way to end it. */
@Composable
fun OwnerModeBanner(onLock: () -> Unit, modifier: Modifier = Modifier) {
    val qColors = QahwaTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(qColors.goldAccent.copy(alpha = 0.22f))
            .statusBarsPadding()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = qColors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.owner_mode),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = qColors.textPrimary,
            )
        }
        TextButton(onClick = onLock) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = qColors.textPrimary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = " " + stringResource(R.string.lock),
                color = qColors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
