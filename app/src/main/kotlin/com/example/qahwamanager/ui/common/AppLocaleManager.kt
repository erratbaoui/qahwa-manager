package com.example.qahwamanager.ui.common

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object AppLocaleManager {
    private const val PREFS_NAME = "qahwa_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    private val _currentLanguage = MutableStateFlow(getInitialLanguage())
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private fun getInitialLanguage(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val code = appLocales[0]?.language
            if (!code.isNullOrBlank()) {
                return normalizeLanguageCode(code)
            }
        }
        val defaultLang = Locale.getDefault().language
        return normalizeLanguageCode(defaultLang)
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLang = prefs.getString(KEY_LANGUAGE, null)
        val initialLang = if (!savedLang.isNullOrBlank()) {
            normalizeLanguageCode(savedLang)
        } else {
            getInitialLanguage()
        }
        _currentLanguage.value = initialLang
        applyToSystem(initialLang)
    }

    fun setLanguage(context: Context, languageCode: String) {
        val normalized = normalizeLanguageCode(languageCode)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, normalized).apply()

        applyToSystem(normalized)
        _currentLanguage.value = normalized
    }

    private fun applyToSystem(languageCode: String) {
        try {
            val targetLocale = Locale.forLanguageTag(languageCode)
            Locale.setDefault(targetLocale)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(languageCode)
            )
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    private fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "ar", "arabic" -> "ar"
            "fr", "french" -> "fr"
            else -> "en"
        }
    }
}
