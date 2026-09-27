package dev.eftrainer.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.eftrainer.core.theme.C
import dev.eftrainer.ui.chapters.AllChaptersScreen
import dev.eftrainer.ui.current.CurrentChapterScreen
import dev.eftrainer.ui.current.CurrentChapterViewModel
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

private const val SLIDE_MS = 320
private const val SLIDE_SHARE = 6

private inline fun <reified T : Any> NavGraphBuilder.screen(
    fromLeft: Boolean = false,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) {
    val shift = { width: Int -> (if (fromLeft) -width else width) / SLIDE_SHARE }
    composable<T>(
        enterTransition = {
            fadeIn(
                tween(SLIDE_MS, easing = EaseOutCubic),
            ) + slideInHorizontally(tween(SLIDE_MS, easing = EaseOutCubic), shift)
        },
        popExitTransition = {
            fadeOut(tween(SLIDE_MS, easing = EaseOutCubic)) +
                slideOutHorizontally(tween(SLIDE_MS, easing = EaseOutCubic), shift)
        },
    ) { entry ->
        Box(Modifier.fillMaxSize().background(C.Paper)) { content(entry) }
    }
}

@Composable
fun AppNavigation(
    onHomeReady: () -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    fun home() {
        nav.popBackStack(CurrentRoute, inclusive = false)
    }

    NavHost(
        nav,
        startDestination = CurrentRoute,
        exitTransition = { fadeOut(tween(SLIDE_MS, easing = EaseOutCubic), targetAlpha = 1f) },
        popEnterTransition = { EnterTransition.None },
    ) {
        chapterScreens(nav, onHomeReady)
        screen<SettingsRoute> {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onVoice = { nav.navigate(VoiceRoute) },
            )
        }
        screen<VoiceRoute> { VoicePickerScreen(onBack = { nav.popBackStack() }) }
        screen<SectionRoute> { entry ->
            val id = entry.toRoute<SectionRoute>().sectionId
            SectionScreen(
                sectionId = id,
                onBack = { nav.popBackStack() },
                onRules = { start -> nav.navigate(RulesRoute(id, start)) },
                onStart = { nav.navigate(SessionRoute(id)) },
                onAgain = { nav.navigate(SessionRoute(id, again = true)) },
            )
        }
        screen<RulesRoute> { entry ->
            val route = entry.toRoute<RulesRoute>()
            RulesScreen(
                sectionId = route.sectionId,
                startTasks = route.startTasks,
                onBack = { nav.popBackStack() },
                onStart = { nav.navigate(SessionRoute(route.sectionId)) },
            )
        }
        screen<SessionRoute> { entry ->
            val route = entry.toRoute<SessionRoute>()
            SessionScreen(
                route.sectionId,
                onClose = { if (route.again) nav.popBackStack() else home() },
                again = route.again,
            )
        }
    }
}

private fun NavGraphBuilder.chapterScreens(
    nav: NavHostController,
    onHomeReady: () -> Unit,
) {
    screen<CurrentRoute> {
        val container = appContainer()
        val home = viewModel(key = "home") { CurrentChapterViewModel(container) }
        val ready = home.state.collectAsStateWithLifecycle().value != null
        if (ready) SideEffect(onHomeReady)
        CurrentChapterScreen(
            onAllChapters = { nav.navigate(ChaptersRoute) },
            onSection = { nav.navigate(SectionRoute(it)) },
            onReview = { nav.navigate(SessionRoute()) },
            onSettings = { nav.navigate(SettingsRoute) },
            onStats = { nav.navigate(StatsRoute) },
            vm = home,
        )
    }
    screen<StatsRoute> {
        StatsScreen(onBack = { nav.popBackStack() }, onSection = { nav.navigate(SectionRoute(it)) })
    }
    screen<ChaptersRoute>(fromLeft = true) {
        AllChaptersScreen(
            onBack = { nav.popBackStack() },
            onChapter = { nav.navigate(ChapterRoute(it)) },
        )
    }
    screen<ChapterRoute> { entry ->
        CurrentChapterScreen(
            onAllChapters = { nav.popBackStack() },
            onSection = { nav.navigate(SectionRoute(it)) },
            onReview = {},
            chapterId = entry.toRoute<ChapterRoute>().chapterId,
        )
    }
}
