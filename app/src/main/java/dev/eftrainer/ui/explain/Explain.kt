package dev.eftrainer.ui.explain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.contextmenu.builder.item
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.rememberSelectionState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.eftrainer.R
import dev.eftrainer.core.theme.C
import dev.eftrainer.core.theme.T
import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.aiAvailable
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.domain.ai.AiChat
import dev.eftrainer.domain.ai.ExplainPrompt
import dev.eftrainer.domain.ai.reply
import dev.eftrainer.ui.appContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// «Explain» на выделенный текст (plans/ai.md, «Explain»): выделил → пункт в меню выделения →
// нижняя панель с ответом ИИ и уточняющими вопросами. Без ИИ меню выделения обычное.

/** Что даёт [ExplainHost] вложенным [Selectable]: открыть объяснение выделенного текста. */
private class ExplainTarget(val open: (String) -> Unit)

private val LocalExplain = staticCompositionLocalOf<ExplainTarget?> { null }

/** Контекст экрана для запроса: глава/раздел, на экране задания — ещё задание и упражнение. */
fun explainContext(sectionId: String): ExplainPrompt.Context {
    val chapter = Chapters.chapterOfSection(sectionId)
    return ExplainPrompt.Context(chapter, Chapters.section(sectionId), studiedUpTo(chapter.id))
}

fun explainContext(exerciseId: String, answer: String?, expected: String?): ExplainPrompt.Context {
    val loc = Chapters.locate(exerciseId)
    val exercise = loc.task.exercises.first { it.id == exerciseId }
    return ExplainPrompt.Context(loc.chapter, loc.section, studiedUpTo(loc.chapter.id), loc.task, exercise, answer, expected)
}

/** Главы открываются по порядку: всё до этой включительно уже пройдено или проходится. */
private fun studiedUpTo(chapterId: String) =
    Chapters.all.let { all -> all.take(all.indexOfFirst { it.id == chapterId } + 1) }

/**
 * Обёртка экрана: держит панель «Explain». [context] — текущий контекст экрана
 * (на экране задания меняется с каждым упражнением).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplainHost(context: ExplainPrompt.Context, content: @Composable () -> Unit) {
    val container = appContainer()
    val aiOn by remember { aiAvailable(container.ai, container.connectivity) }.collectAsStateWithLifecycle(false)
    val vm = viewModel(key = "explain") { ExplainViewModel(container.aiClient) }
    val ctx by rememberUpdatedState(context)
    val target = remember(vm) { ExplainTarget { text -> vm.open(text, ExplainPrompt.selection(text, ctx)) } }

    CompositionLocalProvider(LocalExplain provides if (aiOn) target else null) { content() }

    val open by vm.state.collectAsStateWithLifecycle()
    open?.let { s ->
        ModalBottomSheet(
            onDismissRequest = vm::close,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = C.Paper,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "“${s.selection}”",
                    style = T.rowEn.copy(fontSize = 16.sp, lineHeight = 24.sp, fontStyle = FontStyle.Italic, color = C.Muted),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                ChatThread(s.chat, canAsk = aiOn, onAsk = vm::ask, onRetry = vm::retry, maxHeight = 360.dp)
            }
        }
    }
}

/**
 * Выделяемый текст. Внутри [ExplainHost] при включённом ИИ в меню выделения есть «Explain»;
 * иначе меню стандартное (Copy, Select all).
 */
@Composable
fun Selectable(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val target = LocalExplain.current
    val selection = rememberSelectionState()
    val label = stringResource(R.string.explain)
    val menu = if (target == null) Modifier else Modifier.appendTextContextMenuComponents {
        // Меню собирается при показе: пункт только если выделено что-то в этом контейнере
        // (у полей ввода внутри своё выделение — там «Explain» не нужен).
        val text = selection.selectedTexts.joinToString(" ") { it.text }.trim()
        if (text.isNotEmpty()) {
            item(key = ExplainKey, label = label) {
                close()
                selection.clear()
                target.open(text)
            }
        }
    }
    Box(modifier.then(menu)) {
        SelectionContainer(selection) { content() }
    }
}

private object ExplainKey

/** Панель «Explain»: выделенный текст и чат. Живёт с экраном — переживает поворот. */
class ExplainViewModel(private val client: AiClient) : ViewModel() {

    data class Open(val selection: String, val chat: AiChat)

    private val _state = MutableStateFlow<Open?>(null)
    val state: StateFlow<Open?> = _state.asStateFlow()
    private var job: Job? = null

    fun open(selection: String, prompt: List<Pair<String, String>>) {
        job?.cancel()
        send(Open(selection, AiChat(prompt).start()))
    }

    fun ask(question: String) {
        val s = _state.value ?: return
        if (s.chat.loading || question.isBlank()) return
        send(s.copy(chat = s.chat.ask(question)))
    }

    fun retry() {
        val s = _state.value ?: return
        if (s.chat.error == null) return
        send(s.copy(chat = s.chat.start()))
    }

    fun close() {
        job?.cancel()
        _state.value = null
    }

    private fun send(s: Open) {
        _state.value = s
        job = viewModelScope.launch {
            val chat = client.reply(s.chat)
            if (_state.value === s) _state.value = s.copy(chat = chat)
        }
    }
}
