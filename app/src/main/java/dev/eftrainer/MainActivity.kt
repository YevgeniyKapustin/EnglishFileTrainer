package dev.eftrainer

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.eftrainer.core.theme.AppTheme
import dev.eftrainer.core.theme.C
import dev.eftrainer.data.settings.ThemeMode
import dev.eftrainer.ui.AppNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()   // до первого кадра; стиль значков уточняется ниже по теме
        val theme = (application as App).container.theme
        setContent {
            val mode by theme.mode.collectAsStateWithLifecycle()
            val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
            // Значки статус-бара и навигации — светлые на тёмной теме, тёмные на светлой.
            LaunchedEffect(dark) {
                val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            AppTheme(dark) {
                // safeDrawing включает клавиатуру: низ экрана с кнопкой поднимается над ней.
                Box(Modifier.fillMaxSize().background(C.Paper).windowInsetsPadding(WindowInsets.safeDrawing)) {
                    AppNavigation()
                }
            }
        }
    }
}
