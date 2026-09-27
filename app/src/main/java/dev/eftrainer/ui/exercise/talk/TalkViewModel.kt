package dev.eftrainer.ui.exercise.talk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Talk
import dev.eftrainer.domain.ai.ChatLine
import dev.eftrainer.domain.ai.ExplainPrompt
import dev.eftrainer.domain.ai.TalkPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Один разговор с ИИ (одна попытка упражнения [Talk]): реплики, ожидание ответа, оценка.
 * Живёт, пока открыт экран сессии, — поворот экрана и перерисовки разговор не сбрасывают.
 */
class TalkViewModel(private val client: AiClient, private val talk: Talk) : ViewModel() {

    sealed interface Status {
        data object Talking : Status                  // ход ученика
        data object Waiting : Status                  // ИИ отвечает
        data object Grading : Status                  // ИИ оценивает разговор
        data class Failed(val reason: AiException.Reason, val grading: Boolean) : Status
        data class Graded(val grade: TalkPrompt.Grade) : Status
    }

    data class Ui(val lines: List<ChatLine>, val status: Status) {
        val studentLines: List<String> get() = lines.filter { it.student }.map { it.text }
    }

    private val _state = MutableStateFlow(Ui(listOf(ChatLine(false, talk.opener)), Status.Talking))
    val state: StateFlow<Ui> = _state.asStateFlow()

    fun send(text: String) {
        val s = _state.value
        if (s.status != Status.Talking || text.isBlank()) return
        val lines = s.lines + ChatLine(true, text.trim())
        _state.value = s.copy(lines = lines)
        if (lines.count { it.student } >= TalkPrompt.MAX_TURNS) grade() else answer()
    }

    /** Ученик сам заканчивает разговор. */
    fun finish() {
        val s = _state.value
        if (s.status == Status.Talking && s.lines.any { it.student }) grade()
    }

    fun retry() {
        val f = _state.value.status as? Status.Failed ?: return
        if (f.grading) grade() else answer()
    }

    private fun answer() {
        _state.update { it.copy(status = Status.Waiting) }
        viewModelScope.launch {
            val lines = _state.value.lines
            try {
                val reply = TalkPrompt.reply(client.chat(TalkPrompt.conversation(talk, lines)), lines.count { it.student })
                _state.value = Ui(lines + ChatLine(false, reply.text), Status.Talking)
                if (reply.ended) grade()
            } catch (e: AiException) {
                _state.update { it.copy(status = Status.Failed(e.reason, grading = false)) }
            }
        }
    }

    private fun grade() {
        _state.update { it.copy(status = Status.Grading) }
        viewModelScope.launch {
            val chapter = Chapters.locate(talk.id).chapter
            val prompt = TalkPrompt.grading(talk, ExplainPrompt.rules(chapter.sections), _state.value.lines)
            try {
                // Не разобрали ответ — ещё одна попытка, потом незачёт с общей фразой (упражнение вернётся в конце сессии).
                val grade = TalkPrompt.parseGrade(client.chat(prompt))
                    ?: TalkPrompt.parseGrade(client.chat(prompt))
                    ?: TalkPrompt.Grade(pass = false, summary = "")
                _state.update { it.copy(status = Status.Graded(grade)) }
            } catch (e: AiException) {
                _state.update { it.copy(status = Status.Failed(e.reason, grading = true)) }
            }
        }
    }
}
