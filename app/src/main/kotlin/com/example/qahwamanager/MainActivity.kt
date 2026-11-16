package com.example.qahwamanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.notifications.LowStockScheduler
import com.example.qahwamanager.ui.common.AppLocaleManager
import com.example.qahwamanager.ui.common.AppThemeManager
import com.example.qahwamanager.ui.common.ThemeMode
import com.example.qahwamanager.ui.splash.SplashScreen
import com.example.qahwamanager.ui.theme.QahwaManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LowStockScheduler.ensureScheduledAndCheckIfDue(this)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.forLanguageTag(AppLocaleManager.getSavedLanguage(newBase))
        Locale.setDefault(locale)

        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        AppLocaleManager.init(this)
        AppThemeManager.init(this)
        requestNotificationPermissionAndSchedule()

        enableEdgeToEdge()
        setContent {
            val currentLangCode by AppLocaleManager.currentLanguage.collectAsStateWithLifecycle()
            val themeMode by AppThemeManager.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }

            val layoutDirection = if (currentLangCode.startsWith("ar", ignoreCase = true)) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            var splashCompleted by rememberSaveable { mutableStateOf(false) }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                QahwaManagerTheme(darkTheme = isDark) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        Crossfade(
                            targetState = splashCompleted,
                            animationSpec = tween(350),
                            label = "splashCrossfade",
                        ) { completed ->
                            if (!completed) {
                                SplashScreen(onSplashFinished = { splashCompleted = true })
                            } else {
                                MainNavigation()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                LowStockScheduler.ensureScheduledAndCheckIfDue(this)
            } else {
                LowStockScheduler.scheduleBiHourlyCheck(this)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            LowStockScheduler.ensureScheduledAndCheckIfDue(this)
        }
    }
}
