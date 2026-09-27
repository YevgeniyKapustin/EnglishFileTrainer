package dev.eftrainer.domain.ai

import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiException

/** Реплика в чате с ИИ: ученик или ИИ. */
data class ChatLine(val student: Boolean, val text: String)

/**
 * Мини-чат «Объяснить» (plans/ai.md): первый запрос с контекстом ([prompt], не показывается)
 * + видимые реплики. Уточняющий вопрос уходит вместе со всем, что было раньше.
 * Как reducer во фронте: каждый шаг возвращает новое состояние.
 */
data class AiChat(
    val prompt: List<Pair<String, String>>,   // system + первый вопрос с контекстом
    val lines: List<ChatLine> = emptyList(),
    val loading: Boolean = false,
    val error: AiException.Reason? = null,
) {
    /** Всё, что уходит в запрос: контекст и вся переписка. */
    val messages: List<Pair<String, String>>
        get() = prompt + lines.map { (if (it.student) "user" else "assistant") to it.text }

    /** Первый ответ уже есть — можно уточнять. */
    val answered: Boolean get() = lines.any { !it.student }

    fun start() = copy(loading = true, error = null)
    fun ask(question: String) = copy(lines = lines + ChatLine(true, question.trim()), loading = true, error = null)
    fun answer(text: String) = copy(lines = lines + ChatLine(false, text), loading = false, error = null)
    fun fail(reason: AiException.Reason) = copy(loading = false, error = reason)
}

/** Отправить переписку и дописать ответ (или ошибку). Повтор после ошибки — тот же вызов. */
suspend fun AiClient.reply(chat: AiChat): AiChat = try {
    chat.answer(chat(chat.messages))
} catch (e: AiException) {
    chat.fail(e.reason)
}
