package dev.eftrainer.domain.ai

import dev.eftrainer.data.ai.AiClient
import dev.eftrainer.data.ai.AiException

data class ChatLine(
    val student: Boolean,
    val text: String,
)

data class AiChat(
    val prompt: List<Pair<String, String>>,
    val lines: List<ChatLine> = emptyList(),
    val loading: Boolean = false,
    val error: AiException.Reason? = null,
) {
    val messages: List<Pair<String, String>>
        get() = prompt + lines.map { (if (it.student) "user" else "assistant") to it.text }

    val answered: Boolean get() = lines.any { !it.student }

    fun start() = copy(loading = true, error = null)

    fun ask(question: String) = copy(lines = lines + ChatLine(true, question.trim()), loading = true, error = null)

    fun answer(text: String) = copy(lines = lines + ChatLine(false, text), loading = false, error = null)

    fun fail(reason: AiException.Reason) = copy(loading = false, error = reason)
}

suspend fun AiClient.reply(chat: AiChat): AiChat =
    try {
        chat.answer(chat(chat.messages))
    } catch (e: AiException) {
        chat.fail(e.reason)
    }
