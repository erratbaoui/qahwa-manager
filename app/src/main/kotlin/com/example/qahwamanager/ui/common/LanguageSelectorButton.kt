package com.example.qahwamanager.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.R

data class SupportedLanguage(
    val code: String,
    val flag: String,
    val displayNameRes: Int,
    val nativeName: String,
)

val SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage(code = "en", flag = "🇬🇧", displayNameRes = R.string.language_english, nativeName = "English"),
    SupportedLanguage(code = "ar", flag = "🇲🇦", displayNameRes = R.string.language_arabic, nativeName = "العربية"),
    SupportedLanguage(code = "fr", flag = "🇫🇷", displayNameRes = R.string.language_french, nativeName = "Français"),
)

@Composable
fun LanguageSelectorButton(
    modifier: Modifier = Modifier,
    useTonalStyle: Boolean = false,
) {
    var showDialog by remember { mutableStateOf(false) }
    val currentLangCode by AppLocaleManager.currentLanguage.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val currentLang = SUPPORTED_LANGUAGES.find { it.code.equals(currentLangCode, ignoreCase = true) }
        ?: SUPPORTED_LANGUAGES.first()

    if (useTonalStyle) {
        Surface(
            onClick = { showDialog = true },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = modifier,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = stringResource(R.string.change_language),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "${currentLang.flag} ${currentLang.code.uppercase()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        IconButton(
            onClick = { showDialog = true },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = modifier,
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = stringResource(R.string.change_language),
            )
        }
    }

    if (showDialog) {
        LanguageDialog(
            currentCode = currentLangCode,
            onDismiss = { showDialog = false },
            onLanguageSelected = { selected ->
                showDialog = false
                AppLocaleManager.setLanguage(context, selected.code)
            },
        )
    }
}

@Composable
private fun LanguageDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (SupportedLanguage) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp),
            )
        },
        title = {
            Text(
                text = stringResource(R.string.change_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SUPPORTED_LANGUAGES.forEach { lang ->
                    val isSelected = lang.code.equals(currentCode, ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onLanguageSelected(lang) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                        border = if (isSelected) {
                            CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            )
                        } else null,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = lang.flag,
                                    fontSize = 24.sp,
                                )
                                Column {
                                    Text(
                                        text = lang.nativeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    Text(
                                        text = stringResource(lang.displayNameRes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
