package com.example.qahwamanager.ui.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object AppLocaleManager {
    private const val PREFS_NAME = "qahwa_locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    private val _currentLanguage = MutableStateFlow("en")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    fun getSavedLanguage(context: Context): String {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_LANGUAGE, null)
            if (!saved.isNullOrBlank()) {
                normalizeLanguageCode(saved)
            } else {
                val appLocales = AppCompatDelegate.getApplicationLocales()
                if (!appLocales.isEmpty) {
                    val code = appLocales[0]?.language
                    if (!code.isNullOrBlank()) {
                        return normalizeLanguageCode(code)
                    }
                }
                normalizeLanguageCode(Locale.getDefault().language)
            }
        } catch (_: Exception) {
            "en"
        }
    }

    fun init(context: Context) {
        val lang = getSavedLanguage(context)
        _currentLanguage.value = lang
        try {
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
        } catch (_: Exception) {}
    }

    fun setLanguage(context: Context, languageCode: String) {
        val normalized = normalizeLanguageCode(languageCode)
        if (normalized == _currentLanguage.value) return

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANGUAGE, normalized).apply()
        } catch (_: Exception) {}

        _currentLanguage.value = normalized

        try {
            val targetLocale = Locale.forLanguageTag(normalized)
            Locale.setDefault(targetLocale)
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(normalized)
            )
        } catch (_: Exception) {}

        val activity = context.findActivity()
        activity?.recreate()
    }

    fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase().take(2)) {
            "ar" -> "ar"
            "fr" -> "fr"
            else -> "en"
        }
    }
}

fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
