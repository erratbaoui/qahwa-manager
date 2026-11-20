package com.example.qahwamanager.ui.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
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
import com.example.qahwamanager.data.model.StaffRole
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.auth.resetsOwnerIdleTimer
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.roleLabel
import com.example.qahwamanager.ui.theme.QahwaTheme

/** Add, edit name/role, deactivate or reactivate staff. Deactivated people keep their history. */
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
    var editing by remember { mutableStateOf<Staff?>(null) }

    if (adding) {
        StaffDialog(
            title = stringResource(R.string.add_staff),
            initialName = "",
            initialRole = StaffRole.WAITER,
            roleEditable = true,
            onDismiss = { adding = false },
            onConfirm = { name, role ->
                adding = false
                gate.require { viewModel.add(name, role) }
            },
        )
    }
    editing?.let { person ->
        StaffDialog(
            title = stringResource(if (person.isOwner) R.string.rename else R.string.edit_staff),
            initialName = person.name,
            initialRole = person.role,
            roleEditable = !person.isOwner,
            onDismiss = { editing = null },
            onConfirm = { name, role ->
                editing = null
                gate.require { viewModel.update(person.id, name, role) }
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
                    text = stringResource(R.string.add_staff),
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
                        onEdit = { editing = person },
                        onToggleActive = { gate.require { viewModel.setActive(person.id, !person.isActive) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun StaffRow(person: Staff, onEdit: () -> Unit, onToggleActive: () -> Unit) {
    val qColors = QahwaTheme.colors
    val roleText = roleLabel(person.role)
    QahwaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = person.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (person.isActive) qColors.textPrimary else qColors.textSecondary,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = roleText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (person.role == StaffRole.OWNER) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (person.role == StaffRole.OWNER) qColors.primaryAccent else qColors.textSecondary,
                    )
                    if (!person.isActive) {
                        Text(
                            text = "· " + stringResource(R.string.unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = qColors.textSecondary,
                        )
                    }
                }
            }
            TextButton(onClick = onEdit) {
                Text(
                    text = stringResource(if (person.isOwner) R.string.rename else R.string.edit_staff_action),
                    color = qColors.primaryAccent,
                )
            }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StaffDialog(
    title: String,
    initialName: String,
    initialRole: StaffRole,
    roleEditable: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, StaffRole) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedRole by remember { mutableStateOf(initialRole) }
    val qColors = QahwaTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier
                .resetsOwnerIdleTimer()
                .widthIn(max = 420.dp),
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
                if (roleEditable) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.staff_role),
                            style = MaterialTheme.typography.labelMedium,
                            color = qColors.textSecondary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            StaffRole.entries.forEach { role ->
                                val selected = selectedRole == role
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedRole = role },
                                    label = {
                                        Text(
                                            text = roleLabel(role),
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = qColors.cardSurface,
                                        labelColor = qColors.textSecondary,
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor = qColors.divider,
                                        selectedBorderColor = Color.Transparent,
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = qColors.textSecondary) }
                    TextButton(onClick = { onConfirm(name, selectedRole) }, enabled = name.isNotBlank()) {
                        Text(stringResource(R.string.save), color = qColors.primaryAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
