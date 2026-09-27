package dev.eftrainer.ui

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Transition
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
)

@Serializable data class SessionRoute(
    val sectionId: String? = null,
    val taskId: String? = null,
)

@Serializable data class ChapterRoute(
    val chapterId: String,
)

private const val SLIDE_MS = 320
private const val SLIDE_SHARE = 6

private class ScreenNav(
    private val nav: NavHostController,
    val entry: NavBackStackEntry,
    private val dropKeyboard: () -> Unit,
) {
    private fun leave(go: () -> Unit) {
        if (nav.currentBackStackEntry != entry) return
        dropKeyboard()
        go()
    }

    fun back() = leave { nav.popBackStack() }

    fun home() = leave { nav.popBackStack(CurrentRoute, inclusive = false) }

    fun <R : Any> open(route: R) = leave { nav.navigate(route) }
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
        val focus = LocalFocusManager.current
        val keyboard = LocalSoftwareKeyboardController.current
        val screen =
            remember(entry) {
                ScreenNav(nav, entry) {
                    keyboard?.hide()
                    focus.clearFocus(force = true)
                }
            }
        Box(Modifier.fillMaxSize().background(C.Paper).swallowTouchesUntilSettled(entry, transition)) {
            content(screen)
        }
    }
}

private fun Modifier.swallowTouchesUntilSettled(
    entry: NavBackStackEntry,
    transition: Transition<EnterExitState>,
): Modifier =
    pointerInput(entry, transition) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val shown = entry.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                val still = transition.currentState == EnterExitState.Visible && !transition.isRunning
                if (!shown || !still) event.changes.forEach { it.consume() }
            }
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
        screen<VoiceRoute>(nav, fromLeft = true) { screen -> VoicePickerScreen(onBack = screen::back) }
        screen<SectionRoute>(nav) { screen ->
            val id = screen.entry.toRoute<SectionRoute>().sectionId
            SectionScreen(
                sectionId = id,
                onBack = screen::back,
                onRules = { screen.open(RulesRoute(id)) },
                onTask = { taskId -> screen.open(SessionRoute(id, taskId = taskId)) },
            )
        }
        screen<RulesRoute>(nav) { screen ->
            RulesScreen(sectionId = screen.entry.toRoute<RulesRoute>().sectionId, onBack = screen::back)
        }
        screen<SessionRoute>(nav) { screen ->
            val route = screen.entry.toRoute<SessionRoute>()
            SessionScreen(
                route.sectionId,
                onClose = { if (route.taskId != null) screen.back() else screen.home() },
                taskId = route.taskId,
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
