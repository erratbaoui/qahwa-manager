package com.example.qahwamanager.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Language code ("en", "ar", "fr") of the locale the UI is currently rendered in. */
@Composable
fun currentLanguage(): String = LocalConfiguration.current.locales[0].language
