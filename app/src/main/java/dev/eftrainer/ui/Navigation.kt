package dev.eftrainer.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
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

private class ScreenNav(
    private val nav: NavHostController,
    val entry: NavBackStackEntry,
) {
    private val onTop get() = nav.currentBackStackEntry == entry

    fun back() {
        if (onTop) nav.popBackStack()
    }

    fun home() {
        if (onTop) nav.popBackStack(CurrentRoute, inclusive = false)
    }

    fun <R : Any> open(route: R) {
        if (onTop) nav.navigate(route)
    }
}

private inline fun <reified T : Any> NavGraphBuilder.screen(
    nav: NavHostController,
    fromLeft: Boolean = false,
    noinline content: @Composable (ScreenNav) -> Unit,
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
        val screen = remember(entry) { ScreenNav(nav, entry) }
        val settled =
            entry.lifecycle.currentStateFlow
                .collectAsState()
                .value == Lifecycle.State.RESUMED
        Box(Modifier.fillMaxSize().background(C.Paper)) {
            content(screen)
            if (!settled) Box(Modifier.matchParentSize().swallowTouches())
        }
    }
}

private fun Modifier.swallowTouches(): Modifier =
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) awaitPointerEvent().changes.forEach { it.consume() }
        }
    }

@Composable
fun AppNavigation(
    onHomeReady: () -> Unit,
    nav: NavHostController = rememberNavController(),
) {
    NavHost(
        nav,
        startDestination = CurrentRoute,
        exitTransition = { fadeOut(tween(SLIDE_MS, easing = EaseOutCubic), targetAlpha = 1f) },
        popEnterTransition = { EnterTransition.None },
    ) {
        chapterScreens(nav, onHomeReady)
        screen<SettingsRoute>(nav) { screen ->
            SettingsScreen(
                onBack = screen::back,
                onVoice = { screen.open(VoiceRoute) },
            )
        }
        screen<VoiceRoute>(nav) { screen -> VoicePickerScreen(onBack = screen::back) }
        screen<SectionRoute>(nav) { screen ->
            val id = screen.entry.toRoute<SectionRoute>().sectionId
            SectionScreen(
                sectionId = id,
                onBack = screen::back,
                onRules = { start -> screen.open(RulesRoute(id, start)) },
                onStart = { screen.open(SessionRoute(id)) },
                onAgain = { screen.open(SessionRoute(id, again = true)) },
            )
        }
        screen<RulesRoute>(nav) { screen ->
            val route = screen.entry.toRoute<RulesRoute>()
            RulesScreen(
                sectionId = route.sectionId,
                startTasks = route.startTasks,
                onBack = screen::back,
                onStart = { screen.open(SessionRoute(route.sectionId)) },
            )
        }
        screen<SessionRoute>(nav) { screen ->
            val route = screen.entry.toRoute<SessionRoute>()
            SessionScreen(
                route.sectionId,
                onClose = { if (route.again) screen.back() else screen.home() },
                again = route.again,
            )
        }
    }
}

private fun NavGraphBuilder.chapterScreens(
    nav: NavHostController,
    onHomeReady: () -> Unit,
) {
    screen<CurrentRoute>(nav) { screen ->
        val container = appContainer()
        val home = viewModel(key = "home") { CurrentChapterViewModel(container) }
        val ready = home.state.collectAsStateWithLifecycle().value != null
        if (ready) SideEffect(onHomeReady)
        CurrentChapterScreen(
            onAllChapters = { screen.open(ChaptersRoute) },
            onSection = { screen.open(SectionRoute(it)) },
            onReview = { screen.open(SessionRoute()) },
            onSettings = { screen.open(SettingsRoute) },
            onStats = { screen.open(StatsRoute) },
            vm = home,
        )
    }
    screen<StatsRoute>(nav) { screen ->
        StatsScreen(onBack = screen::back, onSection = { screen.open(SectionRoute(it)) })
    }
    screen<ChaptersRoute>(nav, fromLeft = true) { screen ->
        AllChaptersScreen(
            onBack = screen::back,
            onChapter = { screen.open(ChapterRoute(it)) },
        )
    }
    screen<ChapterRoute>(nav) { screen ->
        CurrentChapterScreen(
            onAllChapters = screen::back,
            onSection = { screen.open(SectionRoute(it)) },
            onReview = {},
            chapterId = screen.entry.toRoute<ChapterRoute>().chapterId,
        )
    }
}
