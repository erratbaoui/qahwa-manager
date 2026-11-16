package com.example.qahwamanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary                = PrimaryGreen,
    onPrimary              = White,
    primaryContainer       = PrimaryContainer,
    onPrimaryContainer     = PrimaryDark,
    secondary              = SecondaryBeige,
    onSecondary            = TextMain,
    secondaryContainer     = SecondaryContainer,
    onSecondaryContainer   = TextMain,
    tertiary               = SuccessGreen,
    onTertiary             = White,
    tertiaryContainer      = PrimaryContainer,
    onTertiaryContainer    = PrimaryDark,
    background             = BackgroundMain,
    onBackground           = TextMain,
    surface                = BackgroundCard,
    onSurface              = TextMain,
    surfaceVariant         = SurfaceVariant,
    onSurfaceVariant       = TextSecondary,
    outline                = OutlineColor,
    outlineVariant         = DividerColor,
    error                  = ErrorRed,
    onError                = White,
    errorContainer         = ErrorContainer,
    onErrorContainer       = ErrorRed,
)

@Composable
fun QahwaManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // The design system is light-only by spec; dark theme falls back gracefully to same scheme.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
