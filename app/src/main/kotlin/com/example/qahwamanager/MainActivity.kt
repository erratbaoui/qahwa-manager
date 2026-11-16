package com.example.qahwamanager

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.qahwamanager.ui.common.AppLocaleManager
import com.example.qahwamanager.ui.theme.QahwaManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.init(this)

        enableEdgeToEdge()
        setContent {
            val currentLangCode by AppLocaleManager.currentLanguage.collectAsStateWithLifecycle()
            val currentLocale = remember(currentLangCode) {
                Locale.forLanguageTag(currentLangCode)
            }

            val context = LocalContext.current
            val localizedContext = remember(context, currentLocale) {
                val config = Configuration(context.resources.configuration)
                config.setLocale(currentLocale)
                config.setLayoutDirection(currentLocale)
                context.createConfigurationContext(config)
            }

            val layoutDirection = if (currentLangCode.startsWith("ar", ignoreCase = true)) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLayoutDirection provides layoutDirection,
            ) {
                QahwaManagerTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        MainNavigation()
                    }
                }
            }
        }
    }
}
