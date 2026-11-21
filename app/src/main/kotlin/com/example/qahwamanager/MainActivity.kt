package com.example.qahwamanager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.qahwamanager.notifications.LowStockScheduler
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.ui.common.AppLocaleManager
import com.example.qahwamanager.ui.common.AppThemeManager
import com.example.qahwamanager.ui.common.ThemeMode
import com.example.qahwamanager.ui.auth.OwnerSetupScreen
import com.example.qahwamanager.ui.cloud.CafeWorkspaceScreen
import com.example.qahwamanager.ui.cloud.CloudAuthScreen
import com.example.qahwamanager.ui.root.RootScreen
import com.example.qahwamanager.ui.root.RootViewModel
import com.example.qahwamanager.ui.splash.SplashScreen
import com.example.qahwamanager.ui.theme.QahwaManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var ownerSession: OwnerSession

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
        // Owner mode ends on its own after two idle minutes; any touch anywhere resets the timer.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { ownerSession.enforceIdleTimeout() }
        }

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
            val rootViewModel: RootViewModel = hiltViewModel()
            val rootScreen by rootViewModel.screen.collectAsStateWithLifecycle()
            val isOwnerMode by ownerSession.isOwnerMode.collectAsStateWithLifecycle()

            // Protect sensitive owner screens (PIN setup, recovery/invite codes, earnings) from screenshots & Recents.
            val protectScreen = isOwnerMode || rootScreen == RootScreen.SETUP
            LaunchedEffect(protectScreen) {
                if (protectScreen) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

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
                                when (rootScreen) {
                                    RootScreen.LOADING -> Box(Modifier.fillMaxSize())
                                    RootScreen.CLOUD_AUTH -> CloudAuthScreen()
                                    RootScreen.CAFE_WORKSPACE -> CafeWorkspaceScreen()
                                    RootScreen.SETUP -> OwnerSetupScreen(rootViewModel)
                                    RootScreen.APP -> MainNavigation(ownerSession)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        ownerSession.touch()
        return super.dispatchTouchEvent(ev)
    }

    /** Leaving the app locks owner mode; a rotation doesn't. */
    override fun onStop() {
        if (!isChangingConfigurations) ownerSession.lock()
        super.onStop()
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
