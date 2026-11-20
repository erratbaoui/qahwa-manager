package com.example.qahwamanager.ui.owner

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.ui.auth.ChangePinDialog
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaPageHeader
import com.example.qahwamanager.ui.theme.QahwaTheme

/** The Owner tab: Sales, Shifts (with how many wait to be settled), Staff, Change PIN. */
@Composable
fun OwnerHomeScreen(
    onSales: () -> Unit,
    onShifts: () -> Unit,
    onStaff: () -> Unit,
    onNewRecoveryCode: () -> Unit,
    viewModel: ShiftsViewModel = hiltViewModel(),
) {
    val toSettle by viewModel.toSettle.collectAsStateWithLifecycle()
    val qColors = QahwaTheme.colors
    var changingPin by remember { mutableStateOf(false) }

    if (changingPin) ChangePinDialog(onDismiss = { changingPin = false })

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
                    OwnerRow(icon = Icons.AutoMirrored.Filled.TrendingUp, title = stringResource(R.string.sales), onClick = onSales)
                    OwnerRow(icon = Icons.Default.Schedule, title = stringResource(R.string.shifts), badge = toSettle.size, onClick = onShifts)
                    OwnerRow(icon = Icons.Default.Group, title = stringResource(R.string.staff), onClick = onStaff)
                    OwnerRow(icon = Icons.Default.Password, title = stringResource(R.string.change_pin), onClick = { changingPin = true })
                    OwnerRow(icon = Icons.Default.Key, title = stringResource(R.string.new_recovery_code), onClick = onNewRecoveryCode)
                }
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
