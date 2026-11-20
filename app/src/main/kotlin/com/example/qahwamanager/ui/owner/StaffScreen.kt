package com.example.qahwamanager.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
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
import com.example.qahwamanager.ui.auth.resetsOwnerIdleTimer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.theme.QahwaTheme

/** Add, rename, deactivate or reactivate staff. Deactivated people keep their history. */
@Composable
fun StaffScreen(
    onBack: () -> Unit,
    viewModel: StaffViewModel = hiltViewModel(),
) {
    val staff by viewModel.staff.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val gate = LocalOwnerGate.current
    val qColors = QahwaTheme.colors
    var adding by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Staff?>(null) }

    if (adding) {
        NameDialog(
            title = stringResource(R.string.add_waiter),
            initial = "",
            onDismiss = { adding = false },
            onConfirm = { name ->
                adding = false
                gate.require { viewModel.add(name) }
            },
        )
    }
    renaming?.let { person ->
        NameDialog(
            title = stringResource(R.string.rename),
            initial = person.name,
            onDismiss = { renaming = null },
            onConfirm = { name ->
                renaming = null
                gate.require { viewModel.rename(person.id, name) }
            },
        )
    }

    OwnerScaffold(title = stringResource(R.string.staff), onBack = onBack) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QahwaPrimaryButton(
                    text = stringResource(R.string.add_waiter),
                    onClick = { adding = true },
                    leadingIcon = Icons.Default.PersonAdd,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let {
                    Text(
                        text = stringResource(if (it == StaffError.BLANK_NAME) R.string.name_required else R.string.owner_always_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = qColors.error,
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                items(staff, key = { it.id }) { person ->
                    StaffRow(
                        person = person,
                        onRename = { renaming = person },
                        onToggleActive = { gate.require { viewModel.setActive(person.id, !person.isActive) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffRow(person: Staff, onRename: () -> Unit, onToggleActive: () -> Unit) {
    val qColors = QahwaTheme.colors
    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (person.isActive) qColors.textPrimary else qColors.textSecondary,
                )
                if (person.isOwner) {
                    Text(stringResource(R.string.owner), style = MaterialTheme.typography.bodySmall, color = qColors.primaryAccent)
                } else if (!person.isActive) {
                    Text(stringResource(R.string.unavailable), style = MaterialTheme.typography.bodySmall, color = qColors.textSecondary)
                }
            }
            TextButton(onClick = onRename) { Text(stringResource(R.string.rename), color = qColors.primaryAccent) }
            if (!person.isOwner) {
                TextButton(onClick = onToggleActive) {
                    Text(
                        text = stringResource(if (person.isActive) R.string.deactivate else R.string.reactivate),
                        color = if (person.isActive) qColors.error else qColors.success,
                    )
                }
            }
        }
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    val qColors = QahwaTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.resetsOwnerIdleTimer().widthIn(max = 420.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = qColors.textPrimary)
                QahwaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.waiter_name),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = qColors.textSecondary) }
                    TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                        Text(stringResource(R.string.save), color = qColors.primaryAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
