package com.example.qahwamanager.ui.loyalty

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.LoyaltyConfig
import com.example.qahwamanager.data.model.LoyaltyCustomer
import com.example.qahwamanager.nfc.NfcWriteState
import com.example.qahwamanager.ui.auth.LocalOwnerGate
import com.example.qahwamanager.ui.auth.resetsOwnerIdleTimer
import com.example.qahwamanager.ui.common.QahwaCard
import com.example.qahwamanager.ui.common.QahwaEmptyState
import com.example.qahwamanager.ui.common.QahwaPrimaryButton
import com.example.qahwamanager.ui.common.QahwaSecondaryButton
import com.example.qahwamanager.ui.common.QahwaStatCard
import com.example.qahwamanager.ui.common.QahwaTextField
import com.example.qahwamanager.ui.common.QahwaTopBarActions
import com.example.qahwamanager.ui.common.formatQuantity
import com.example.qahwamanager.ui.common.rememberDateFormat
import com.example.qahwamanager.ui.theme.BrandAmberGold
import com.example.qahwamanager.ui.theme.BrandCoffeeBrown
import com.example.qahwamanager.ui.theme.BrandForestLight
import com.example.qahwamanager.ui.theme.QahwaTheme
import java.net.URLEncoder
import java.util.Date

/**
 * Tappy-style Digital Loyalty Stamp Card & NFC/QR Hub:
 * - Waiters can enroll customers by Name + Phone, search by phone/QR code, add stamps (+1), and redeem free rewards.
 * - Owners can filter 15-day inactive regulars ("Gbrine") for 1-tap WhatsApp win-back, configure stamps/reward/Google Review link,
 *   and write the café's loyalty URL directly onto physical NTAG213/NTAG215 NFC table stands.
 */
@Composable
fun LoyaltyScreen(
    onBack: () -> Unit,
    viewModel: LoyaltyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOwnerMode by viewModel.isOwnerMode.collectAsStateWithLifecycle()
    val nfcState by viewModel.nfcState.collectAsStateWithLifecycle()
    val gate = LocalOwnerGate.current
    val qColors = QahwaTheme.colors
    val context = LocalContext.current

    var showEnrollDialog by rememberSaveable { mutableStateOf(false) }
    var showQuickStampDialog by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showStandLinkDialog by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { viewModel.cancelNfcWrite() }
    }

    if (showEnrollDialog) {
        EnrollCustomerDialog(
            onDismiss = { showEnrollDialog = false },
            onConfirm = { name, phone ->
                viewModel.enrollCustomer(name, phone) { showEnrollDialog = false }
            },
        )
    }

    if (showQuickStampDialog) {
        QuickStampByCodeDialog(
            onDismiss = { showQuickStampDialog = false },
            onScanCamera = {
                QrCodeScannerHelper.startQrScan(
                    context = context,
                    onScanned = { rawCode ->
                        viewModel.stampByCodeOrPhone(rawCode) { matched ->
                            if (matched) showQuickStampDialog = false
                        }
                    },
                )
            },
            onSubmit = { input, onResult ->
                viewModel.stampByCodeOrPhone(input) { matched ->
                    if (matched) showQuickStampDialog = false
                    onResult(matched)
                }
            },
        )
    }

    if (showSettingsDialog) {
        LoyaltySettingsDialog(
            config = state.config,
            onDismiss = { showSettingsDialog = false },
            onConfirm = { stampsRequired, rewardTitle, googleReviewUrl, wifiSsid, wifiPassword ->
                gate.require {
                    viewModel.updateProgramSettings(
                        stampsRequired = stampsRequired,
                        rewardTitle = rewardTitle,
                        googleReviewUrl = googleReviewUrl,
                        wifiSsid = wifiSsid,
                        wifiPassword = wifiPassword,
                    ) {
                        showSettingsDialog = false
                    }
                }
            },
        )
    }

    if (showStandLinkDialog) {
        StandLinkDialog(
            cafeName = state.cafeName,
            standUrl = state.standUrl,
            printStudioUrl = state.printStudioUrl,
            config = state.config,
            onWriteNfc = {
                showStandLinkDialog = false
                gate.require { viewModel.startWritingNfcStand() }
            },
            onDismiss = { showStandLinkDialog = false },
        )
    }

    if (nfcState !is NfcWriteState.Idle) {
        NfcWriteDialog(
            nfcState = nfcState,
            onDismiss = viewModel::cancelNfcWrite,
        )
    }

    Scaffold(
        containerColor = qColors.background,
        topBar = {
            Surface(color = qColors.cardSurface, border = BorderStroke(1.dp, qColors.divider)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = qColors.textPrimary,
                            )
                        }
                        Text(
                            text = stringResource(R.string.loyalty_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = qColors.textPrimary,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        IconButton(onClick = { showStandLinkDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = stringResource(R.string.nfc_qr_stand),
                                tint = qColors.primaryAccent,
                            )
                        }
                        IconButton(onClick = { gate.require { showSettingsDialog = true } }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.loyalty_settings),
                                tint = qColors.primaryAccent,
                            )
                        }
                        QahwaTopBarActions()
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    // Summary stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        QahwaStatCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.loyalty_members),
                            value = formatQuantity(state.allCustomers.size),
                            valueColor = qColors.primaryAccent,
                            accentDotColor = BrandAmberGold,
                        )
                        QahwaStatCard(
                            modifier = Modifier.weight(1f),
                            label = stringResource(R.string.rewards_redeemed),
                            value = formatQuantity(state.totalRewardsRedeemed),
                            valueColor = qColors.textPrimary,
                            accentDotColor = if (qColors.isDark) BrandForestLight else BrandCoffeeBrown,
                        )
                    }
                }

                item {
                    // Quick actions: New member + Stamp by code/phone + Print Table QR Stickers
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            QahwaPrimaryButton(
                                text = stringResource(R.string.enroll_customer),
                                onClick = { showEnrollDialog = true },
                                leadingIcon = Icons.Default.PersonAdd,
                                modifier = Modifier.weight(1f),
                            )
                            QahwaSecondaryButton(
                                text = stringResource(R.string.quick_stamp_code),
                                onClick = {
                                    QrCodeScannerHelper.startQrScan(
                                        context = context,
                                        onScanned = { rawCode -> viewModel.stampByCodeOrPhone(rawCode) },
                                        onFallbackRequired = { showQuickStampDialog = true },
                                    )
                                },
                                leadingIcon = Icons.Default.QrCode2,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        QahwaSecondaryButton(
                            text = stringResource(R.string.print_table_qr_codes),
                            onClick = { showStandLinkDialog = true },
                            leadingIcon = Icons.Default.Print,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    // Search + Filter (All vs 15-day Inactive "Gbrine")
                    QahwaTextField(
                        value = state.query,
                        onValueChange = viewModel::setQuery,
                        label = stringResource(R.string.search_customer_phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = !state.showInactiveOnly,
                            onClick = { viewModel.setShowInactiveOnly(false) },
                            label = {
                                Text("${stringResource(R.string.all_staff)} (${state.allCustomers.size})")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = qColors.cardSurface,
                                labelColor = qColors.textSecondary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = !state.showInactiveOnly,
                                borderColor = qColors.divider,
                                selectedBorderColor = Color.Transparent,
                            ),
                        )
                        FilterChip(
                            selected = state.showInactiveOnly,
                            onClick = { viewModel.setShowInactiveOnly(true) },
                            label = {
                                Text("${stringResource(R.string.inactive_customers_15d)} (${state.inactiveCustomers.size})")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = qColors.cardSurface,
                                labelColor = qColors.textSecondary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.showInactiveOnly,
                                borderColor = qColors.divider,
                                selectedBorderColor = Color.Transparent,
                            ),
                        )
                    }
                }

                if (state.filteredCustomers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            QahwaEmptyState(
                                message = stringResource(R.string.no_loyalty_customers),
                                icon = Icons.Default.CardGiftcard,
                            )
                        }
                    }
                } else {
                    items(state.filteredCustomers, key = { it.id }) { customer ->
                        LoyaltyCustomerCard(
                            customer = customer,
                            config = state.config,
                            isOwnerMode = isOwnerMode,
                            onAddStamp = { viewModel.addStamp(customer.id) },
                            onRedeem = { viewModel.redeemReward(customer.id) },
                            onWhatsAppWinBack = {
                                openWhatsAppWinBack(context, customer, state.config.rewardTitle)
                            },
                            onDelete = {
                                gate.require { viewModel.deleteCustomer(customer.id) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LoyaltyCustomerCard(
    customer: LoyaltyCustomer,
    config: LoyaltyConfig,
    isOwnerMode: Boolean,
    onAddStamp: () -> Unit,
    onRedeem: () -> Unit,
    onWhatsAppWinBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val qColors = QahwaTheme.colors
    val dateFormat = rememberDateFormat("d MMM yyyy")
    val formattedLastVisit = remember(customer.lastVisitAt, dateFormat) {
        dateFormat.format(Date(customer.lastVisitAt))
    }
    val rewardReady = customer.currentStamps >= config.stampsRequired

    QahwaCard(
        selected = rewardReady,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = qColors.textPrimary,
                    )
                    Text(
                        text = "${customer.phone} · ${stringResource(R.string.last_visit_date, formattedLastVisit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.textSecondary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onWhatsAppWinBack,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.whatsapp_winback),
                            tint = qColors.success,
                        )
                    }
                    if (isOwnerMode) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = qColors.error,
                            )
                        }
                    }
                }
            }

            // Visual Stamp Circles (capped at 12 with +N overflow pill for large stamp thresholds)
            val maxCircles = minOf(config.stampsRequired, 12)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (index in 1..maxCircles) {
                    val filled = index <= customer.currentStamps
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (filled) BrandAmberGold else qColors.elevatedSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalCafe,
                            contentDescription = null,
                            tint = if (filled) Color(0xFF143023) else qColors.textSecondary.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (config.stampsRequired > maxCircles) {
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(qColors.elevatedSurface)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+${config.stampsRequired - maxCircles}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = qColors.textSecondary,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(
                        R.string.stamps_progress,
                        customer.currentStamps,
                        config.stampsRequired,
                        customer.rewardsRedeemed,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (rewardReady) BrandAmberGold else qColors.textSecondary,
                    fontWeight = if (rewardReady) FontWeight.Bold else FontWeight.Normal,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (rewardReady) {
                        QahwaPrimaryButton(
                            text = stringResource(R.string.redeem_reward),
                            onClick = onRedeem,
                            leadingIcon = Icons.Default.CardGiftcard,
                        )
                    }
                    QahwaSecondaryButton(
                        text = stringResource(R.string.add_stamp_plus_one),
                        onClick = onAddStamp,
                        leadingIcon = Icons.Default.Add,
                    )
                }
            }
        }
    }
}

private fun openWhatsAppWinBack(context: Context, customer: LoyaltyCustomer, rewardTitle: String) {
    val cleanDigits = customer.phone.filter { it.isDigit() }
    val msg = context.getString(R.string.whatsapp_winback_message, customer.name, rewardTitle)
    val encoded = URLEncoder.encode(msg, "UTF-8")
    val uri = Uri.parse("https://wa.me/$cleanDigits?text=$encoded")
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

@Composable
private fun EnrollCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    val canSave = name.isNotBlank() && phone.filter { it.isDigit() }.length >= 8
    val qColors = QahwaTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.enroll_customer),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                QahwaTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.waiter_name),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = stringResource(R.string.customer_phone),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                    }
                    TextButton(
                        onClick = { if (canSave) onConfirm(name.trim(), phone.trim()) },
                        enabled = canSave,
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = if (canSave) qColors.primaryAccent else qColors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStampByCodeDialog(
    onDismiss: () -> Unit,
    onScanCamera: () -> Unit,
    onSubmit: (String, (Boolean) -> Unit) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    var notFound by remember { mutableStateOf(false) }
    val qColors = QahwaTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.quick_stamp_code),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                QahwaSecondaryButton(
                    text = stringResource(R.string.scan_qr_camera),
                    onClick = onScanCamera,
                    leadingIcon = Icons.Default.CameraAlt,
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        notFound = false
                    },
                    label = stringResource(R.string.enter_qr_or_phone),
                    isError = notFound,
                    errorMessage = if (notFound) stringResource(R.string.customer_not_found) else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                    }
                    TextButton(
                        onClick = {
                            onSubmit(input) { matched -> notFound = !matched }
                        },
                        enabled = input.isNotBlank(),
                    ) {
                        Text(
                            text = stringResource(R.string.add_stamp_plus_one),
                            color = qColors.primaryAccent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoyaltySettingsDialog(
    config: LoyaltyConfig,
    onDismiss: () -> Unit,
    onConfirm: (
        stampsRequired: Int,
        rewardTitle: String,
        googleReviewUrl: String,
        wifiSsid: String,
        wifiPassword: String,
    ) -> Unit,
) {
    var stampsInput by rememberSaveable { mutableStateOf(config.stampsRequired.toString()) }
    var rewardInput by rememberSaveable { mutableStateOf(config.rewardTitle) }
    var reviewUrlInput by rememberSaveable { mutableStateOf(config.googleReviewUrl) }
    var wifiSsidInput by rememberSaveable { mutableStateOf(config.wifiSsid) }
    var wifiPasswordInput by rememberSaveable { mutableStateOf(config.wifiPassword) }
    val parsedStamps = stampsInput.toIntOrNull()?.takeIf { it in 1..50 }
    val canSave = parsedStamps != null && rewardInput.isNotBlank()
    val qColors = QahwaTheme.colors

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier
                .resetsOwnerIdleTimer()
                .widthIn(max = 440.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.loyalty_settings),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                QahwaTextField(
                    value = stampsInput,
                    onValueChange = { stampsInput = it },
                    label = stringResource(R.string.stamps_required_label),
                    isError = stampsInput.isNotBlank() && parsedStamps == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = rewardInput,
                    onValueChange = { rewardInput = it },
                    label = stringResource(R.string.reward_title_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = reviewUrlInput,
                    onValueChange = { reviewUrlInput = it },
                    label = stringResource(R.string.google_review_url_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = wifiSsidInput,
                    onValueChange = { wifiSsidInput = it },
                    label = stringResource(R.string.wifi_ssid_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaTextField(
                    value = wifiPasswordInput,
                    onValueChange = { wifiPasswordInput = it },
                    label = stringResource(R.string.wifi_password_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                    }
                    TextButton(
                        onClick = {
                            if (parsedStamps != null && canSave) {
                                onConfirm(
                                    parsedStamps,
                                    rewardInput.trim(),
                                    reviewUrlInput.trim(),
                                    wifiSsidInput.trim(),
                                    wifiPasswordInput.trim(),
                                )
                            }
                        },
                        enabled = canSave,
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            color = if (canSave) qColors.primaryAccent else qColors.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StandLinkDialog(
    cafeName: String,
    standUrl: String,
    printStudioUrl: String,
    config: LoyaltyConfig,
    onWriteNfc: () -> Unit,
    onDismiss: () -> Unit,
) {
    val qColors = QahwaTheme.colors
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    var tableCountInput by remember { mutableStateOf("12") }
    val tableCount = tableCountInput.toIntOrNull()?.coerceIn(1, 40) ?: 12
    val qrMatrix = remember(standUrl) {
        if (standUrl.isNotBlank()) QrCodeGenerator.encode(standUrl) else emptyArray()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier
                .resetsOwnerIdleTimer()
                .widthIn(max = 460.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.nfc_qr_stand),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.nfc_qr_stand_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.textSecondary,
                )

                // Live scannable QR Code Preview on screen
                if (qrMatrix.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(2.dp, BrandAmberGold),
                            modifier = Modifier.size(184.dp),
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                            ) {
                                val n = qrMatrix.size
                                val cell = size.width / n
                                for (r in 0 until n) {
                                    for (c in 0 until n) {
                                        if (qrMatrix[r][c]) {
                                            drawRect(
                                                color = Color(0xFF143023),
                                                topLeft = Offset(c * cell, r * cell),
                                                size = Size(cell * 1.03f, cell * 1.03f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                QahwaTextField(
                    value = tableCountInput,
                    onValueChange = { tableCountInput = it },
                    label = stringResource(R.string.table_count_label),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                QahwaPrimaryButton(
                    text = stringResource(R.string.print_a4_pdf_sheet, tableCount),
                    onClick = {
                        TableQrPrintHelper.printTableQrSheet(
                            context = context,
                            cafeName = cafeName,
                            baseStandUrl = standUrl,
                            tableCount = tableCount,
                            stampsRequired = config.stampsRequired,
                            rewardTitle = config.rewardTitle,
                        )
                    },
                    leadingIcon = Icons.Default.Print,
                    modifier = Modifier.fillMaxWidth(),
                )

                QahwaSecondaryButton(
                    text = stringResource(R.string.open_web_print_studio),
                    onClick = {
                        val encodedName = URLEncoder.encode(cafeName, "UTF-8")
                        val encodedReward = URLEncoder.encode(config.rewardTitle, "UTF-8")
                        val fullUrl = "$printStudioUrl&name=$encodedName&reward=$encodedReward&tables=$tableCount"
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)))
                        }
                    },
                    leadingIcon = Icons.Default.QrCode2,
                    modifier = Modifier.fillMaxWidth(),
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = qColors.background,
                    border = BorderStroke(1.dp, qColors.divider),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = standUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = qColors.primaryAccent,
                        modifier = Modifier.padding(14.dp),
                    )
                }

                QahwaSecondaryButton(
                    text = stringResource(if (copied) R.string.code_copied else R.string.copy_stand_link),
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Loyalty Stand URL", standUrl))
                        copied = true
                    },
                    leadingIcon = Icons.Default.ContentCopy,
                    modifier = Modifier.fillMaxWidth(),
                )
                QahwaSecondaryButton(
                    text = stringResource(R.string.write_nfc_tag),
                    onClick = onWriteNfc,
                    leadingIcon = Icons.Default.Nfc,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.ok), color = qColors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcWriteDialog(
    nfcState: NfcWriteState,
    onDismiss: () -> Unit,
) {
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
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = qColors.primaryAccent,
                    modifier = Modifier.size(44.dp),
                )
                val statusText = when (nfcState) {
                    is NfcWriteState.WaitingForTag -> stringResource(R.string.nfc_touch_tag_prompt)
                    is NfcWriteState.Success -> stringResource(R.string.nfc_write_success)
                    is NfcWriteState.Error -> nfcState.message
                    NfcWriteState.Idle -> ""
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = when (nfcState) {
                        is NfcWriteState.Error -> qColors.error
                        is NfcWriteState.Success -> qColors.success
                        else -> qColors.textPrimary
                    },
                )
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.ok), color = qColors.primaryAccent)
                }
            }
        }
    }
}
