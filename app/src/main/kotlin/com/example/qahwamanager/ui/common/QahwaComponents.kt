package com.example.qahwamanager.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.ui.theme.BackgroundCard
import com.example.qahwamanager.ui.theme.BackgroundMain
import com.example.qahwamanager.ui.theme.DividerColor
import com.example.qahwamanager.ui.theme.ErrorContainer
import com.example.qahwamanager.ui.theme.ErrorRed
import com.example.qahwamanager.ui.theme.OutlineColor
import com.example.qahwamanager.ui.theme.PrimaryContainer
import com.example.qahwamanager.ui.theme.PrimaryDark
import com.example.qahwamanager.ui.theme.PrimaryGreen
import com.example.qahwamanager.ui.theme.SuccessGreen
import com.example.qahwamanager.ui.theme.TextMain
import com.example.qahwamanager.ui.theme.TextSecondary
import com.example.qahwamanager.ui.theme.WarningContainer
import com.example.qahwamanager.ui.theme.WarningTerracotta

// ── Quantity control ─────────────────────────────────────────────────────────

/**
 * Universal, accessible quantity control with tabular numeral centering.
 * The value text is locked in the mathematical center between "−" and "+",
 * perfectly aligned across all states and screen sizes without dynamic layout shift.
 */
@Composable
fun QahwaQuantityControl(
    quantity: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    minQuantity: Int = 0,
    enabled: Boolean = true,
) {
    QahwaQuantityControl(
        valueText = quantity.toString(),
        onAdd = onAdd,
        onRemove = onRemove,
        modifier = modifier,
        canRemove = quantity > minQuantity,
        enabled = enabled,
    )
}

@Composable
fun QahwaQuantityControl(
    valueText: String,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    canRemove: Boolean = true,
    enabled: Boolean = true,
) {
    val decreaseLabel = stringResource(R.string.decrease_quantity)
    val increaseLabel = stringResource(R.string.increase_quantity)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // "−" Button
        QuantityButton(
            onClick = onRemove,
            icon = Icons.Default.Remove,
            contentDescription = decreaseLabel,
            enabled = enabled && canRemove,
        )

        // Perfectly centered value display with tabular numbers
        Box(
            modifier = Modifier
                .widthIn(min = 38.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) TextMain else TextSecondary.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }

        // "+" Button
        QuantityButton(
            onClick = onAdd,
            icon = Icons.Default.Add,
            contentDescription = increaseLabel,
            enabled = enabled,
        )
    }
}

@Composable
private fun QuantityButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (enabled) PrimaryGreen.copy(alpha = 0.12f) else DividerColor.copy(alpha = 0.6f)
    val tint = if (enabled) PrimaryDark else TextSecondary.copy(alpha = 0.4f)
    val border = if (enabled) {
        BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
    } else {
        BorderStroke(1.dp, DividerColor)
    }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(36.dp)
            .clip(CircleShape)
            .background(bg)
            .border(border, CircleShape)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = tint,
        )
    }
}

// ── Buttons ──────────────────────────────────────────────────────────────────

@Composable
fun QahwaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryGreen,
            contentColor = Color.White,
            disabledContainerColor = PrimaryGreen.copy(alpha = 0.38f),
            disabledContentColor = Color.White.copy(alpha = 0.38f),
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun QahwaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PrimaryDark,
            disabledContentColor = TextSecondary.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) PrimaryGreen.copy(alpha = 0.6f) else DividerColor,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun QahwaDestructiveButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = ErrorRed,
            disabledContentColor = ErrorRed.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) ErrorRed.copy(alpha = 0.5f) else DividerColor,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Text Fields ──────────────────────────────────────────────────────────────

@Composable
fun QahwaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            enabled = enabled,
            singleLine = singleLine,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = BackgroundMain,
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = DividerColor,
                focusedLabelColor = PrimaryDark,
                unfocusedLabelColor = TextSecondary,
                cursorColor = PrimaryGreen,
                errorBorderColor = ErrorRed,
                errorLabelColor = ErrorRed,
            ),
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

// ── Cards ────────────────────────────────────────────────────────────────────

@Composable
fun QahwaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    content: @Composable () -> Unit,
) {
    val borderStroke = if (selected) {
        BorderStroke(1.5.dp, PrimaryGreen)
    } else {
        BorderStroke(1.dp, DividerColor)
    }

    val containerColor = if (selected) {
        PrimaryGreen.copy(alpha = 0.04f)
    } else {
        BackgroundCard
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = borderStroke,
        ) { content() }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = borderStroke,
        ) { content() }
    }
}

// ── Status Badges ────────────────────────────────────────────────────────────

@Composable
fun QahwaStatusBadge(status: OrderStatus) {
    val (bg, fg, labelRes) = when (status) {
        OrderStatus.OPEN -> Triple(PrimaryGreen.copy(alpha = 0.12f), PrimaryDark, R.string.status_open)
        OrderStatus.PAID -> Triple(SuccessGreen.copy(alpha = 0.12f), SuccessGreen, R.string.status_paid)
        OrderStatus.CANCELLED -> Triple(ErrorRed.copy(alpha = 0.12f), ErrorRed, R.string.status_cancelled)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// ── Section Header ───────────────────────────────────────────────────────────

@Composable
fun QahwaSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = modifier,
    )
}

// ── Divider ──────────────────────────────────────────────────────────────────

@Composable
fun QahwaDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = DividerColor, thickness = 1.dp)
}

// ── Currency Text ─────────────────────────────────────────────────────────────

@Composable
fun QahwaCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = TextMain,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Text(
        text = stringResource(R.string.currency_format, amount),
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

// ── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun QahwaEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DividerColor.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp),
                )
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextSecondary,
                )
            }
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// ── Confirm Dialog ───────────────────────────────────────────────────────────

@Composable
fun QahwaConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMain,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    ) {
                        Text(stringResource(R.string.cancel), color = TextSecondary)
                    }
                    if (isDestructive) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        ) {
                            Text(confirmText, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        ) {
                            Text(confirmText, color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Stat Card ────────────────────────────────────────────────────────────────

@Composable
fun QahwaStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextMain,
) {
    QahwaCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
            )
        }
    }
}

// ── Page Header ──────────────────────────────────────────────────────────────

@Composable
fun QahwaPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextMain,
        )
        action?.invoke()
    }
}

// ── Low Stock Warning ────────────────────────────────────────────────────────

@Composable
fun QahwaLowStockWarning(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = WarningContainer,
        border = BorderStroke(1.dp, WarningTerracotta.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(WarningTerracotta),
            )
            Column {
                Text(
                    text = stringResource(R.string.low_stock_alerts),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = WarningTerracotta,
                )
                Text(
                    text = stringResource(R.string.low_stock_count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = WarningTerracotta.copy(alpha = 0.85f),
                )
            }
        }
    }
}
