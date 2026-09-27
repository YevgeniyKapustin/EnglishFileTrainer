package dev.eftrainer

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewTreeObserver
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
import androidx.core.view.ViewCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.eftrainer.core.theme.AppTheme
import dev.eftrainer.core.theme.C
import dev.eftrainer.data.settings.ThemeMode
import dev.eftrainer.reminder.Notifier
import dev.eftrainer.ui.AppNavigation
import dev.eftrainer.ui.current.IntroGate

class MainActivity : ComponentActivity() {
    private var homeReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ViewCompat.addOnUnhandledKeyEventListener(window.decorView) { _, event ->
            backReleasedAfterKeyboardTookPress(event)
        }
        if (savedInstanceState == null) holdSplashUntilHomeReady() else IntroGate.open.value = true
        val container = (application as App).container
        container.sounds.open()
        val theme = container.theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            container.reminder.enabled.value &&
            !Notifier.canPost(this)
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
            val mode by theme.mode.collectAsStateWithLifecycle()
            val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && isSystemInDarkTheme())
            LaunchedEffect(dark) {
                val bars =
                    if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            AppTheme(dark) {
                Box(Modifier.fillMaxSize().background(C.Paper).windowInsetsPadding(WindowInsets.safeDrawing)) {
                    AppNavigation(onHomeReady = ::markHomeReady)
                }
            }
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) (application as App).container.sounds.close()
        super.onDestroy()
    }

    private fun backReleasedAfterKeyboardTookPress(event: KeyEvent): Boolean {
        val orphanBackRelease =
            event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP && !event.isCanceled
        if (orphanBackRelease) onBackPressedDispatcher.onBackPressed()
        return orphanBackRelease
    }

    private fun markHomeReady() {
        if (homeReady) return
        homeReady = true
        reportFullyDrawn()
    }

    private fun holdSplashUntilHomeReady() {
        val fanOpenAt = Process.getStartUptimeMillis() + SPLASH_FAN_MS
        val giveUpAt = SystemClock.uptimeMillis() + SPLASH_HOLD_CAP_MS
        val content = findViewById<View>(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    val now = SystemClock.uptimeMillis()
                    val release = (homeReady && now >= fanOpenAt) || now >= giveUpAt
                    if (release) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        IntroGate.open.value = true
                    }
                    return release
                }
            },
        )
    }

    private companion object {
        const val SPLASH_FAN_MS = 700L
        const val SPLASH_HOLD_CAP_MS = 3_000L
    }
}
