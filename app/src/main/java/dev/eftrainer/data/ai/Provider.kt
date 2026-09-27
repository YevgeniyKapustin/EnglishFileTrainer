package dev.eftrainer.data.ai

/** Поставщик ИИ. Узнаём по виду ключа, модель и адрес фиксированы — пользователь их не выбирает. */
enum class Provider(val title: String) {
    DEEPSEEK("DeepSeek"), OPENAI("OpenAI"), GEMINI("Gemini"), CLAUDE("Claude"), OPENROUTER("OpenRouter")
}

private val DEEPSEEK_KEY = Regex("^sk-[0-9a-f]{32}$")

/** null — пусто или не похоже ни на один ключ. */
fun detectProvider(key: String?): Provider? {
    val k = key?.trim().orEmpty()
    return when {
        k.isEmpty() -> null
        k.startsWith("sk-ant-") -> Provider.CLAUDE
        k.startsWith("sk-or-") -> Provider.OPENROUTER
        k.startsWith("AIza") -> Provider.GEMINI
        DEEPSEEK_KEY.matches(k) -> Provider.DEEPSEEK
        k.startsWith("sk-") -> Provider.OPENAI
        else -> null
    }
}

/** Адрес чата у поставщика. */
internal val Provider.url: String
    get() = when (this) {
        Provider.DEEPSEEK -> "https://api.deepseek.com/chat/completions"
        Provider.OPENAI -> "https://api.openai.com/v1/chat/completions"
        Provider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
        Provider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
        Provider.CLAUDE -> "https://api.anthropic.com/v1/messages"
    }

/** Дешёвая модель поставщика. */
internal val Provider.model: String
    get() = when (this) {
        Provider.DEEPSEEK -> "deepseek-chat"
        Provider.OPENAI -> "gpt-4o-mini"
        Provider.GEMINI -> "gemini-2.5-flash"
        Provider.OPENROUTER -> "deepseek/deepseek-chat"
        Provider.CLAUDE -> "claude-haiku-4-5"
    }

/** У Claude свой API (Messages), остальные — как у OpenAI. */
internal val Provider.openAiCompatible: Boolean get() = this != Provider.CLAUDE

/** Заголовки запроса: у Claude ключ в x-api-key, у остальных — Bearer. */
internal fun headers(provider: Provider, key: String): Map<String, String> =
    if (provider == Provider.CLAUDE) mapOf(
        "Content-Type" to "application/json",
        "x-api-key" to key,
        "anthropic-version" to "2023-06-01",
    ) else mapOf(
        "Content-Type" to "application/json",
        "Authorization" to "Bearer $key",
    )

/** Для Claude: system-сообщения склеиваем в одно поле, остальное — в messages. */
internal fun splitSystem(messages: List<Pair<String, String>>): Pair<String?, List<Pair<String, String>>> {
    val (system, rest) = messages.partition { it.first == "system" }
    return system.joinToString("\n\n") { it.second }.ifEmpty { null } to rest
}

/** Код ответа → ошибка; null — всё хорошо. Gemini на плохой ключ отвечает 400. */
internal fun failureFor(provider: Provider, code: Int): AiException.Reason? = when {
    code == 200 -> null
    code == 401 || code == 402 || code == 403 -> AiException.Reason.BAD_KEY   // 402 — кончились деньги на счёте
    code == 400 && provider == Provider.GEMINI -> AiException.Reason.BAD_KEY
    else -> AiException.Reason.SERVER
}
