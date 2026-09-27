package dev.eftrainer.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.eftrainer.ui.chapters.AllChaptersScreen
import dev.eftrainer.ui.current.CurrentChapterScreen
import dev.eftrainer.ui.exercise.SessionScreen
import dev.eftrainer.ui.section.RulesScreen
import dev.eftrainer.ui.section.SectionScreen
import dev.eftrainer.ui.settings.SettingsScreen
import dev.eftrainer.ui.settings.VoicePickerScreen
import kotlinx.serialization.Serializable

// Маршруты — как роуты в react-router, только типизированные классы.
@Serializable object CurrentRoute
@Serializable object ChaptersRoute
@Serializable object SettingsRoute
@Serializable object VoiceRoute
@Serializable data class SectionRoute(val sectionId: String)
@Serializable data class RulesRoute(val sectionId: String, val startTasks: Boolean)
@Serializable data class SessionRoute(val sectionId: String? = null)   // null — Review

@Composable
fun AppNavigation(nav: NavHostController = rememberNavController()) {
    // Сессия закрывается всегда на главный экран, минуя раздел и правила.
    fun home() { nav.popBackStack(CurrentRoute, inclusive = false) }

    NavHost(
        nav,
        startDestination = CurrentRoute,
        // Старый экран гаснет сразу, новый быстро проявляется — без наложения текста.
        // (ExitTransition.None оставлял старый экран видимым до конца перехода.)
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(0)) },
    ) {
        composable<CurrentRoute> {
            CurrentChapterScreen(
                onAllChapters = { nav.navigate(ChaptersRoute) },
                onSection = { nav.navigate(SectionRoute(it)) },
                onReview = { nav.navigate(SessionRoute()) },
                onSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<ChaptersRoute> { AllChaptersScreen(onBack = { nav.popBackStack() }) }
        composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }, onVoice = { nav.navigate(VoiceRoute) }) }
        composable<VoiceRoute> { VoicePickerScreen(onBack = { nav.popBackStack() }) }
        composable<SectionRoute> { entry ->
            val id = entry.toRoute<SectionRoute>().sectionId
            SectionScreen(
                sectionId = id,
                onBack = { nav.popBackStack() },
                onRules = { start -> nav.navigate(RulesRoute(id, start)) },
                onStart = { nav.navigate(SessionRoute(id)) },
            )
        }
        composable<RulesRoute> { entry ->
            val route = entry.toRoute<RulesRoute>()
            RulesScreen(
                sectionId = route.sectionId,
                startTasks = route.startTasks,
                onBack = { nav.popBackStack() },
                onStart = { nav.navigate(SessionRoute(route.sectionId)) },
            )
        }
        composable<SessionRoute> { entry ->
            SessionScreen(entry.toRoute<SessionRoute>().sectionId, onClose = ::home)
        }
    }
}
