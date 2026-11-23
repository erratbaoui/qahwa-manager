package com.example.qahwamanager.ui.owner

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.cloud.SyncStatus
import com.example.qahwamanager.ui.auth.ChangePinDialog
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.auth.resetsOwnerIdleTimer
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaConfirmDialog
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.theme.QahwaTheme

/** The Owner tab: Dashboard, Sales, Shifts, Staff, Co-owner cloud sync, PIN & Sign out. */
@Composable
fun OwnerHomeScreen(
    onDashboard: () -> Unit,
    onSales: () -> Unit,
    onShifts: () -> Unit,
    onStaff: () -> Unit,
    onLoyalty: () -> Unit = {},
    onNewRecoveryCode: () -> Unit,
    viewModel: ShiftsViewModel = hiltViewModel(),
    cloudViewModel: OwnerCloudViewModel = hiltViewModel(),
) {
    val toSettle by viewModel.toSettle.collectAsStateWithLifecycle()
    val activeCafe by cloudViewModel.activeCafe.collectAsStateWithLifecycle()
    val syncStatus by cloudViewModel.syncStatus.collectAsStateWithLifecycle()
    val invite by cloudViewModel.invite.collectAsStateWithLifecycle()
    val cloudError by cloudViewModel.error.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    val gate = LocalOwnerGate.current
    val context = LocalContext.current

    var changingPin by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }

    if (changingPin) ChangePinDialog(onDismiss = { changingPin = false })

    if (confirmSignOut) {
        QahwaConfirmDialog(
            title = stringResource(R.string.sign_out_confirm_title),
            message = stringResource(R.string.sign_out_confirm_message),
            confirmText = stringResource(R.string.sign_out),
            isDestructive = true,
            onConfirm = {
                confirmSignOut = false
                gate.require { cloudViewModel.signOut() }
            },
            onDismiss = { confirmSignOut = false },
        )
    }

    invite?.let { activeInvite ->
        var copied by remember(activeInvite.code) { mutableStateOf(false) }
        Dialog(onDismissRequest = cloudViewModel::dismissInvite) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = qColors.cardSurface,
                border = BorderStroke(1.dp, qColors.divider),
                modifier = Modifier
                    .resetsOwnerIdleTimer()
                    .widthIn(max = 420.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.invite_co_owner),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = qColors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.invite_co_owner_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = qColors.textSecondary,
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = qColors.background,
                        border = BorderStroke(1.dp, qColors.divider),
                    ) {
                        Text(
                            text = activeInvite.code.chunked(4).joinToString("-"),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = qColors.primaryAccent,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        )
                    }
                    QahwaPrimaryButton(
                        text = stringResource(if (copied) R.string.code_copied else R.string.copy_code),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Invite code", activeInvite.code))
                            copied = true
                        },
                        leadingIcon = Icons.Default.ContentCopy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = cloudViewModel::dismissInvite) {
                        Text(stringResource(R.string.ok), color = qColors.textSecondary)
                    }
                }
            }
        }
    }

    Scaffold(containerColor = qColors.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QahwaPageHeader(title = stringResource(R.string.owner))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    activeCafe?.let { cafe ->
                        CloudSyncBanner(cafeName = cafe.name, status = syncStatus)
                    }
                    cloudError?.let { err ->
                        Text(text = err, style = MaterialTheme.typography.bodySmall, color = qColors.error)
                    }
                    OwnerRow(icon = Icons.Default.BarChart, title = stringResource(R.string.owner_dashboard), onClick = onDashboard)
                    OwnerRow(icon = Icons.AutoMirrored.Filled.TrendingUp, title = stringResource(R.string.sales), onClick = onSales)
                    OwnerRow(icon = Icons.Default.Schedule, title = stringResource(R.string.shifts), badge = toSettle.size, onClick = onShifts)
                    OwnerRow(icon = Icons.Default.Group, title = stringResource(R.string.staff), onClick = onStaff)
                    OwnerRow(icon = Icons.Default.CardGiftcard, title = stringResource(R.string.loyalty_title), onClick = onLoyalty)
                    OwnerRow(
                        icon = Icons.Default.PersonAdd,
                        title = stringResource(R.string.invite_co_owner),
                        onClick = { gate.require { cloudViewModel.generateInviteCode() } },
                    )
                    OwnerRow(icon = Icons.Default.Password, title = stringResource(R.string.change_pin), onClick = { changingPin = true })
                    OwnerRow(icon = Icons.Default.Key, title = stringResource(R.string.new_recovery_code), onClick = onNewRecoveryCode)
                    OwnerRow(icon = Icons.AutoMirrored.Filled.Logout, title = stringResource(R.string.sign_out), onClick = { confirmSignOut = true })
                }
            }
        }
    }
}

@Composable
private fun CloudSyncBanner(cafeName: String, status: SyncStatus) {
    val qColors = QahwaTheme.colors
    val (icon, statusRes) = when (status) {
        SyncStatus.SYNCED -> Icons.Default.CloudDone to R.string.sync_status_synced
        SyncStatus.SYNCING -> Icons.Default.CloudSync to R.string.sync_status_syncing
        SyncStatus.OFFLINE -> Icons.Default.CloudOff to R.string.sync_status_offline
        SyncStatus.ERROR -> Icons.Default.CloudOff to R.string.sync_status_error
        SyncStatus.IDLE -> Icons.Default.CloudSync to R.string.sync_status_syncing
    }
    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = qColors.primaryAccent)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cafeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = qColors.textPrimary,
                )
                Text(
                    text = stringResource(statusRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = qColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun OwnerRow(icon: ImageVector, title: String, onClick: () -> Unit, badge: Int = 0) {
    val qColors = QahwaTheme.colors
    QahwaCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = qColors.primaryAccent)
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = qColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (badge > 0) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(qColors.warning),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badge.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = qColors.cardSurface)
                }
            }
        }
    }
}
