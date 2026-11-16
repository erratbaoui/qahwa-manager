package com.example.qahwamanager.ui.common

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.find { it.storageKey.equals(key, ignoreCase = true) } ?: SYSTEM
    }
}

object AppThemeManager {
    private const val PREFS_NAME = "qahwa_theme_prefs"
    private const val KEY_THEME_MODE = "selected_theme_mode"

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_THEME_MODE, null)
            _themeMode.value = ThemeMode.fromKey(saved)
        } catch (_: Exception) {
            _themeMode.value = ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeMode.value = mode
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
        } catch (_: Exception) {}
    }

    fun toggleDarkLight(context: Context, currentlyDark: Boolean) {
        val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        setThemeMode(context, next)
    }
}
