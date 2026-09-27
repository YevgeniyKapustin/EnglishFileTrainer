package dev.eftrainer.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
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
import dev.eftrainer.ui.stats.StatsScreen
import kotlinx.serialization.Serializable

@Serializable object CurrentRoute

@Serializable object ChaptersRoute

@Serializable object SettingsRoute

@Serializable object VoiceRoute

@Serializable object StatsRoute

@Serializable data class SectionRoute(
    val sectionId: String,
)

@Serializable data class RulesRoute(
    val sectionId: String,
    val startTasks: Boolean,
)

@Serializable data class SessionRoute(
    val sectionId: String? = null,
    val again: Boolean = false,
)

@Serializable data class ChapterRoute(
    val chapterId: String,
)

@Composable
fun AppNavigation(nav: NavHostController = rememberNavController()) {
    fun home() {
        nav.popBackStack(CurrentRoute, inclusive = false)
    }

    NavHost(
        nav,
        startDestination = CurrentRoute,
        enterTransition = { fadeIn(tween(150)) },
        exitTransition = { fadeOut(tween(0)) },
    ) {
        chapterScreens(nav)
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onVoice = { nav.navigate(VoiceRoute) },
            )
        }
        composable<VoiceRoute> { VoicePickerScreen(onBack = { nav.popBackStack() }) }
        composable<SectionRoute> { entry ->
            val id = entry.toRoute<SectionRoute>().sectionId
            SectionScreen(
                sectionId = id,
                onBack = { nav.popBackStack() },
                onRules = { start -> nav.navigate(RulesRoute(id, start)) },
                onStart = { nav.navigate(SessionRoute(id)) },
                onAgain = { nav.navigate(SessionRoute(id, again = true)) },
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
            val route = entry.toRoute<SessionRoute>()
            SessionScreen(
                route.sectionId,
                onClose = { if (route.again) nav.popBackStack() else home() },
                again = route.again,
            )
        }
    }
}

private fun NavGraphBuilder.chapterScreens(nav: NavHostController) {
    composable<CurrentRoute> {
        CurrentChapterScreen(
            onAllChapters = { nav.navigate(ChaptersRoute) },
            onSection = { nav.navigate(SectionRoute(it)) },
            onReview = { nav.navigate(SessionRoute()) },
            onSettings = { nav.navigate(SettingsRoute) },
            onStats = { nav.navigate(StatsRoute) },
        )
    }
    composable<StatsRoute> {
        StatsScreen(onBack = { nav.popBackStack() }, onSection = { nav.navigate(SectionRoute(it)) })
    }
    composable<ChaptersRoute> {
        AllChaptersScreen(
            onBack = { nav.popBackStack() },
            onChapter = { nav.navigate(ChapterRoute(it)) },
        )
    }
    composable<ChapterRoute> { entry ->
        CurrentChapterScreen(
            onAllChapters = { nav.popBackStack() },
            onSection = { nav.navigate(SectionRoute(it)) },
            onReview = {},
            chapterId = entry.toRoute<ChapterRoute>().chapterId,
        )
    }
}
