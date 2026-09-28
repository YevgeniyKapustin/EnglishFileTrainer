package dev.eftrainer.ui.exercise

import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiException
import dev.eftrainer.data.content.Blank
import dev.eftrainer.data.content.Chapters
import dev.eftrainer.data.content.Exercise
import dev.eftrainer.data.content.IrregularVerbs
import dev.eftrainer.data.content.Rewrite
import dev.eftrainer.domain.ai.AcceptPrompt
import dev.eftrainer.domain.ai.ExplainPrompt
import dev.eftrainer.domain.ai.RuleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SecondOpinion(
    private val state: MutableStateFlow<SessionUi>,
    private val scope: CoroutineScope,
    private val client: AiClient,
) {
    fun ask(
        q: SessionUi.Question,
        inputs: List<String>,
        onVerdict: (SessionUi.Question, accepted: Boolean) -> Unit,
    ) {
        if (q.aiCheck == AiCheck.Checking) return
        state.value = q.copy(aiCheck = AiCheck.Checking)
        scope.launch {
            val accepted = accepts(q.exercise, inputs)
            val now = state.value as? SessionUi.Question ?: return@launch
            if (now.attempt != q.attempt || now.feedback != null) return@launch
            onVerdict(now.copy(aiCheck = null), accepted)
        }
    }

    private suspend fun accepts(
        e: Exercise,
        inputs: List<String>,
    ): Boolean {
        val loc = Chapters.locate(e.id)
        val rules = ExplainPrompt.rules(RuleScope.forChapter(Chapters.all, loc.chapter, loc.section))
        val prompt =
            when (e) {
                is Rewrite -> AcceptPrompt.asking(loc.task, e, inputs.single(), rules)
                is Blank -> AcceptPrompt.asking(loc.task, e, inputs, rules)
                else -> return false
            }
        val reply = scope.async { client.chat(prompt) }
        return try {
            withTimeoutOrNull(WAIT_MILLIS) { reply.await() }?.let(AcceptPrompt::parseAccepted) == true
        } catch (_: AiException) {
            false
        } finally {
            reply.cancel()
        }
    }

    companion object {
        private const val WAIT_MILLIS = 15_000L

        fun fits(
            e: Exercise,
            aiOn: Boolean,
        ): Boolean = (e is Rewrite || e is Blank) && aiOn && !e.id.startsWith(IrregularVerbs.ID_PREFIX)
    }
}
