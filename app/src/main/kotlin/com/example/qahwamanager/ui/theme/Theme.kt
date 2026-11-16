package com.example.qahwamanager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class QahwaExtendedColors(
    val isDark: Boolean,
    val background: Color,
    val cardSurface: Color,
    val elevatedSurface: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val goldAccent: Color,
    val coffeeAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val divider: Color,
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val heroBannerBrush: Brush,
    val splashBackgroundBrush: Brush,
)

private val LightExtendedColors = QahwaExtendedColors(
    isDark = false,
    background = LightBackground,
    cardSurface = LightSurface,
    elevatedSurface = LightSurfaceVariant,
    primaryAccent = BrandForest,
    secondaryAccent = BrandForestDeep,
    goldAccent = BrandAmberGold,
    coffeeAccent = BrandCoffeeBrown,
    textPrimary = LightTextMain,
    textSecondary = LightTextSecondary,
    divider = LightDivider,
    success = SuccessGreen,
    successContainer = Color(0xFFDDF0E6),
    warning = WarningTerracotta,
    warningContainer = WarningContainer,
    error = ErrorRed,
    errorContainer = ErrorContainer,
    heroBannerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF285944),
            Color(0xFF1B4030),
            Color(0xFF143023),
        )
    ),
    splashBackgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF6F2ED),
            Color(0xFFF4EFEA),
            Color(0xFFEBE3DA),
        )
    ),
)

private val DarkExtendedColors = QahwaExtendedColors(
    isDark = true,
    background = DarkBackground,
    cardSurface = DarkSurface,
    elevatedSurface = DarkSurfaceVariant,
    primaryAccent = BrandAmberGold,
    secondaryAccent = BrandAmberSoft,
    goldAccent = BrandAmberGold,
    coffeeAccent = BrandCoffeeLight,
    textPrimary = DarkTextMain,
    textSecondary = DarkTextSecondary,
    divider = DarkDivider,
    success = SuccessGreenDarkMode,
    successContainer = Color(0xFF1D4332),
    warning = WarningAmberDarkMode,
    warningContainer = WarningContainerDark,
    error = ErrorRedDarkMode,
    errorContainer = ErrorContainerDark,
    heroBannerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF285944),
            Color(0xFF1D4130),
            Color(0xFF153124),
        )
    ),
    splashBackgroundBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF285944),
            Color(0xFF1C4130),
            Color(0xFF122A1E),
        )
    ),
)

val LocalQahwaColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary                = LightPrimary,
    onPrimary              = LightOnPrimary,
    primaryContainer       = LightPrimaryContainer,
    onPrimaryContainer     = LightOnPrimaryContainer,
    secondary              = LightSecondary,
    onSecondary            = LightOnSecondary,
    secondaryContainer     = LightSecondaryContainer,
    onSecondaryContainer   = LightOnSecondaryContainer,
    tertiary               = LightTertiary,
    onTertiary             = LightOnTertiary,
    tertiaryContainer      = LightTertiaryContainer,
    onTertiaryContainer    = BrandCoffeeBrown,
    background             = LightBackground,
    onBackground           = LightTextMain,
    surface                = LightSurface,
    onSurface              = LightTextMain,
    surfaceVariant         = LightSurfaceVariant,
    onSurfaceVariant       = LightTextSecondary,
    outline                = LightOutline,
    outlineVariant         = LightDivider,
    error                  = ErrorRed,
    onError                = White,
    errorContainer         = ErrorContainer,
    onErrorContainer       = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary                = DarkPrimary,
    onPrimary              = DarkOnPrimary,
    primaryContainer       = DarkPrimaryContainer,
    onPrimaryContainer     = DarkOnPrimaryContainer,
    secondary              = DarkSecondary,
    onSecondary            = DarkOnSecondary,
    secondaryContainer     = DarkSecondaryContainer,
    onSecondaryContainer   = DarkOnSecondaryContainer,
    tertiary               = DarkTertiary,
    onTertiary             = DarkOnTertiary,
    tertiaryContainer      = DarkTertiaryContainer,
    onTertiaryContainer    = DarkOnTertiaryContainer,
    background             = DarkBackground,
    onBackground           = DarkTextMain,
    surface                = DarkSurface,
    onSurface              = DarkTextMain,
    surfaceVariant         = DarkSurfaceVariant,
    onSurfaceVariant       = DarkTextSecondary,
    outline                = DarkOutline,
    outlineVariant         = DarkDivider,
    error                  = ErrorRedDarkMode,
    onError                = DarkBackground,
    errorContainer         = ErrorContainerDark,
    onErrorContainer       = ErrorRedDarkMode,
)

object QahwaTheme {
    val colors: QahwaExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQahwaColors.current
}

@Composable
fun QahwaManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalQahwaColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
