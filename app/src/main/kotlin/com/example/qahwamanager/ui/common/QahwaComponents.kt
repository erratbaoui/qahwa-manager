package com.example.qahwamanager.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.qahwamanager.ui.auth.resetsOwnerIdleTimer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.qahwamanager.R
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.ui.theme.BrandAmberGold
import com.example.qahwamanager.ui.theme.BrandCoffeeBrown
import com.example.qahwamanager.ui.theme.BrandCream
import com.example.qahwamanager.ui.theme.BrandForest
import com.example.qahwamanager.ui.theme.BrandForestDeep
import com.example.qahwamanager.ui.theme.BrandForestMedium
import com.example.qahwamanager.ui.theme.QahwaTheme

// ── Brand Emblem & Signature Elements (From Reference Photos) ────────────────

/**
 * Renders the Qahwa Manager coffee bean emblem inspired by the two brand photos:
 * - Light mode / filled circle style (Photo 1): Deep forest green gradient circle,
 *   cream coffee bean, and warm amber gold dot at top-right.
 * - Dark mode / concentric ring style (Photo 2): Translucent concentric rings,
 *   tilted cream coffee bean, and warm amber gold dot at top-right.
 */
@Composable
fun QahwaBrandEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
    concentricRingStyle: Boolean = false,
) {
    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        if (concentricRingStyle) {
            // Photo 2 style: Outer thin ring + translucent inner circle
            drawCircle(
                color = BrandCream.copy(alpha = 0.14f),
                radius = radius * 0.96f,
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )
            drawCircle(
                color = BrandCream.copy(alpha = 0.07f),
                radius = radius * 0.72f,
                center = center,
            )
        } else {
            // Photo 1 style: Rich forest green gradient circle
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BrandForestMedium,
                        BrandForest,
                        BrandForestDeep,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(this.size.width, this.size.height),
                ),
                radius = radius * 0.90f,
                center = center,
            )
        }

        // Cream Coffee Bean in the center (tilted slightly as in Photo 2)
        val beanRotation = if (concentricRingStyle) -14f else -6f
        rotate(degrees = beanRotation, pivot = center) {
            val beanWidth = radius * 0.68f
            val beanHeight = radius * 0.94f
            drawOval(
                color = BrandCream,
                topLeft = Offset(center.x - beanWidth / 2f, center.y - beanHeight / 2f),
                size = Size(beanWidth, beanHeight),
            )

            // S-curve crease down the center of the coffee bean
            val creasePath = Path().apply {
                moveTo(center.x - beanWidth * 0.06f, center.y - beanHeight * 0.44f)
                cubicTo(
                    center.x - beanWidth * 0.18f, center.y - beanHeight * 0.12f,
                    center.x + beanWidth * 0.22f, center.y + beanHeight * 0.12f,
                    center.x + beanWidth * 0.06f, center.y + beanHeight * 0.44f,
                )
            }
            drawPath(
                path = creasePath,
                color = BrandForestDeep,
                style = Stroke(width = (radius * 0.085f).coerceAtLeast(2f), cap = StrokeCap.Round),
            )
        }

        // Signature Warm Amber Gold Dot at top-right (~2 o'clock)
        val dotAngleRad = Math.toRadians(-42.0)
        val dotDist = if (concentricRingStyle) radius * 0.62f else radius * 0.78f
        val dotCenter = Offset(
            x = center.x + (dotDist * kotlin.math.cos(dotAngleRad)).toFloat(),
            y = center.y + (dotDist * kotlin.math.sin(dotAngleRad)).toFloat(),
        )
        drawCircle(
            color = BrandAmberGold,
            radius = radius * 0.135f,
            center = dotCenter,
        )
    }
}

/**
 * The three signature palette dots from Photo 1:
 * Deep Forest Green (#193D2E), Warm Coffee Brown (#7D5D44), and Warm Amber Gold (#E2A165).
 */
@Composable
fun QahwaBrandDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp,
    spacing: Dp = 10.dp,
) {
    val isDark = QahwaTheme.colors.isDark
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(if (isDark) BrandCream else BrandForestDeep)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(BrandCoffeeBrown)
        )
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(BrandAmberGold)
        )
    }
}

// ── Theme Toggle & Header Actions ────────────────────────────────────────────

@Composable
fun ThemeToggleButton(
    modifier: Modifier = Modifier,
    useTonalStyle: Boolean = false,
) {
    val context = LocalContext.current
    val isDark = QahwaTheme.colors.isDark
    val icon = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode
    val label = if (isDark) stringResource(R.string.light_mode) else stringResource(R.string.dark_mode)

    if (useTonalStyle) {
        Surface(
            onClick = { AppThemeManager.toggleDarkLight(context, isDark) },
            shape = RoundedCornerShape(10.dp),
            color = BrandCream.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, BrandCream.copy(alpha = 0.22f)),
            modifier = modifier,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.toggle_theme),
                    tint = BrandAmberGold,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandCream,
                )
            }
        }
    } else {
        IconButton(
            onClick = { AppThemeManager.toggleDarkLight(context, isDark) },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = QahwaTheme.colors.primaryAccent,
            ),
            modifier = modifier,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(R.string.toggle_theme),
            )
        }
    }
}

@Composable
fun QahwaTopBarActions(
    useTonalStyle: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ThemeToggleButton(useTonalStyle = useTonalStyle)
        LanguageSelectorButton(useTonalStyle = useTonalStyle)
    }
}

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
        valueText = formatQuantity(quantity),
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
    val qColors = QahwaTheme.colors

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
                .widthIn(min = 40.dp)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Bold,
                color = if (enabled) qColors.textPrimary else qColors.textSecondary.copy(alpha = 0.5f),
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
    val qColors = QahwaTheme.colors
    val bg = if (enabled) {
        qColors.primaryAccent.copy(alpha = if (qColors.isDark) 0.18f else 0.12f)
    } else {
        qColors.divider.copy(alpha = 0.45f)
    }
    val tint = if (enabled) qColors.primaryAccent else qColors.textSecondary.copy(alpha = 0.4f)
    val border = if (enabled) {
        BorderStroke(1.dp, qColors.primaryAccent.copy(alpha = 0.32f))
    } else {
        BorderStroke(1.dp, qColors.divider)
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
    val containerColor = MaterialTheme.colorScheme.primary
    val contentColor = MaterialTheme.colorScheme.onPrimary

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.38f),
            disabledContentColor = contentColor.copy(alpha = 0.38f),
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
    val qColors = QahwaTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = qColors.primaryAccent,
            disabledContentColor = qColors.textSecondary.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) qColors.primaryAccent.copy(alpha = 0.6f) else qColors.divider,
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
    val qColors = QahwaTheme.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = qColors.error,
            disabledContentColor = qColors.error.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            1.dp,
            if (enabled) qColors.error.copy(alpha = 0.55f) else qColors.divider,
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
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    val qColors = QahwaTheme.colors
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
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = qColors.cardSurface,
                unfocusedContainerColor = qColors.cardSurface,
                disabledContainerColor = qColors.elevatedSurface.copy(alpha = 0.5f),
                focusedTextColor = qColors.textPrimary,
                unfocusedTextColor = qColors.textPrimary,
                focusedBorderColor = qColors.primaryAccent,
                unfocusedBorderColor = qColors.divider,
                focusedLabelColor = qColors.primaryAccent,
                unfocusedLabelColor = qColors.textSecondary,
                cursorColor = qColors.primaryAccent,
                errorBorderColor = qColors.error,
                errorLabelColor = qColors.error,
            ),
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = qColors.error,
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
    val qColors = QahwaTheme.colors
    val borderStroke = if (selected) {
        BorderStroke(1.5.dp, qColors.primaryAccent)
    } else {
        BorderStroke(1.dp, qColors.divider)
    }

    val containerColor = if (selected) {
        if (qColors.isDark) qColors.elevatedSurface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        qColors.cardSurface
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
    val qColors = QahwaTheme.colors
    val (bg, fg, labelRes) = when (status) {
        OrderStatus.OPEN -> Triple(
            qColors.goldAccent.copy(alpha = 0.16f),
            if (qColors.isDark) qColors.goldAccent else BrandCoffeeBrown,
            R.string.status_open,
        )
        OrderStatus.PAID -> Triple(
            qColors.success.copy(alpha = 0.16f),
            qColors.success,
            R.string.status_paid,
        )
        OrderStatus.CANCELLED -> Triple(
            qColors.error.copy(alpha = 0.16f),
            qColors.error,
            R.string.status_cancelled,
        )
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
        color = QahwaTheme.colors.textSecondary,
        modifier = modifier,
    )
}

// ── Divider ──────────────────────────────────────────────────────────────────

@Composable
fun QahwaDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = QahwaTheme.colors.divider, thickness = 1.dp)
}

// ── Currency Text (Centimes & Double Overloads) ──────────────────────────────

@Composable
fun QahwaCurrencyText(
    centimes: Long,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = QahwaTheme.colors.textPrimary,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Text(
        text = formatMad(centimes),
        style = style.copy(fontFeatureSettings = "tnum"),
        color = color,
        fontWeight = fontWeight,
        modifier = modifier,
    )
}

@Composable
fun QahwaCurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = QahwaTheme.colors.textPrimary,
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
    val qColors = QahwaTheme.colors
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(qColors.elevatedSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = qColors.goldAccent,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else {
            QahwaBrandEmblem(
                size = 64.dp,
                concentricRingStyle = qColors.isDark,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = qColors.textSecondary,
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
    val qColors = QahwaTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = qColors.cardSurface,
            border = BorderStroke(1.dp, qColors.divider),
            modifier = Modifier.resetsOwnerIdleTimer().widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.textPrimary,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = qColors.textSecondary,
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
                        Text(stringResource(R.string.cancel), color = qColors.textSecondary)
                    }
                    if (isDestructive) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = qColors.error),
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(confirmText, fontWeight = FontWeight.SemiBold)
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
    valueColor: Color = QahwaTheme.colors.textPrimary,
    accentDotColor: Color = BrandAmberGold,
) {
    val qColors = QahwaTheme.colors
    QahwaCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentDotColor),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = qColors.textSecondary,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFeatureSettings = "tnum",
                ),
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
        }
    }
}

// ── Page Header ──────────────────────────────────────────────────────────────

@Composable
fun QahwaPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = { QahwaTopBarActions() },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QahwaBrandEmblem(
                size = 34.dp,
                concentricRingStyle = QahwaTheme.colors.isDark,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = QahwaTheme.colors.textPrimary,
            )
        }
        action?.invoke()
    }
}

// ── Low Stock Warning ────────────────────────────────────────────────────────

@Composable
fun QahwaLowStockWarning(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    val qColors = QahwaTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = qColors.warningContainer,
        border = BorderStroke(1.dp, qColors.warning.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(qColors.warning),
            )
            Column {
                Text(
                    text = stringResource(R.string.low_stock_alerts),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = qColors.warning,
                )
                Text(
                    text = pluralStringResource(R.plurals.low_stock_count, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = qColors.textPrimary.copy(alpha = 0.85f),
                )
            }
        }
    }
}
