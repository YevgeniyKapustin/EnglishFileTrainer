package dev.eftrainer.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class AiSettings(
    context: Context,
) {
    private val prefs =
        context.getSharedPreferences("ai", Context.MODE_PRIVATE).also { p ->
            val old = p.getString(OLD_KEY, null)
            if (old != null && !p.contains(KEY)) {
                p.edit {
                    putString(KEY, old)
                    remove(OLD_KEY)
                }
            }
        }
    private val _key = MutableStateFlow(prefs.getString(KEY, null))
    val key: StateFlow<String?> = _key.asStateFlow()
    val provider: Provider? get() = detectProvider(key.value)

    fun setKey(value: String?) {
        val v = value?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit { if (v == null) remove(KEY) else putString(KEY, v) }
        _key.value = v
    }

    private companion object {
        const val KEY = "ai_key"
        const val OLD_KEY = "deepseek_key"
    }
}

class Connectivity(
    context: Context,
) {
    private val cm = context.getSystemService(ConnectivityManager::class.java)
    private val _online = MutableStateFlow(cm.getNetworkCapabilities(cm.activeNetwork).hasInternet())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    init {
        cm.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(
                    network: Network,
                    caps: NetworkCapabilities,
                ) {
                    _online.value =
                        caps.hasInternet()
                }

                override fun onLost(network: Network) {
                    _online.value = false
                }
            },
        )
    }

    private fun NetworkCapabilities?.hasInternet() =
        this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

fun aiAvailable(
    settings: AiSettings,
    connectivity: Connectivity,
) = combine(settings.key, connectivity.online) { key, online -> detectProvider(key) != null && online }

class AiException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(reason.name, cause) {
    enum class Reason { NO_KEY, BAD_KEY, NETWORK, SERVER }
}

class AiClient(
    private val settings: AiSettings,
) {
    suspend fun chat(messages: List<Pair<String, String>>): String {
        val key = settings.key.value ?: throw AiException(AiException.Reason.NO_KEY)
        val provider = detectProvider(key) ?: throw AiException(AiException.Reason.NO_KEY)
        return send(provider, key, messages, maxTokens = null)
    }

    suspend fun verify(key: String) {
        val k = key.trim()
        val provider = detectProvider(k) ?: throw AiException(AiException.Reason.BAD_KEY)
        send(provider, k, listOf("user" to "Hi"), maxTokens = 5)
    }

    private suspend fun send(
        provider: Provider,
        key: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int?,
    ): String =
        withContext(Dispatchers.IO) {
            val body =
                if (provider.openAiCompatible) {
                    openAiBody(
                        provider,
                        messages,
                        maxTokens,
                    )
                } else {
                    claudeBody(messages, maxTokens)
                }
            val conn =
                (URL(provider.url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    headers(provider, key).forEach { (name, value) -> setRequestProperty(name, value) }
                }
            try {
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
                failureFor(provider, conn.responseCode)?.let { throw AiException(it) }
                val json = JSONObject(conn.inputStream.use { it.readBytes().decodeToString() })
                if (provider.openAiCompatible) openAiText(json) else claudeText(json)
            } catch (e: IOException) {
                throw AiException(AiException.Reason.NETWORK, e)
            } catch (e: JSONException) {
                throw AiException(AiException.Reason.SERVER, e)
            } finally {
                conn.disconnect()
            }
        }

    private fun messagesJson(messages: List<Pair<String, String>>) =
        JSONArray(messages.map { (role, text) -> JSONObject().put("role", role).put(CONTENT, text) })

    private fun openAiBody(
        provider: Provider,
        messages: List<Pair<String, String>>,
        maxTokens: Int?,
    ) = JSONObject()
        .put(MODEL, provider.model)
        .put(TEMPERATURE, TEMPERATURE_VALUE)
        .put(MESSAGES, messagesJson(messages))
        .apply { if (maxTokens != null) put(MAX_TOKENS, maxTokens) }

    private fun claudeBody(
        messages: List<Pair<String, String>>,
        maxTokens: Int?,
    ): JSONObject {
        val (system, rest) = splitSystem(messages)
        return JSONObject()
            .put(MODEL, Provider.CLAUDE.model)
            .put(MAX_TOKENS, maxTokens ?: CLAUDE_DEFAULT_MAX_TOKENS)
            .put(TEMPERATURE, TEMPERATURE_VALUE)
            .put(MESSAGES, messagesJson(rest))
            .apply { if (system != null) put("system", system) }
    }

    private fun openAiText(json: JSONObject) =
        json
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString(CONTENT)
            .trim()

    private fun claudeText(json: JSONObject): String {
        val content = json.getJSONArray(CONTENT)
        return (0 until content.length())
            .map { content.getJSONObject(it) }
            .filter { it.optString("type") == "text" }
            .joinToString("") { it.getString("text") }
            .trim()
    }

    private companion object {
        const val TEMPERATURE = "temperature"
        const val MODEL = "model"
        const val MESSAGES = "messages"
        const val MAX_TOKENS = "max_tokens"
        const val CONTENT = "content"
        const val TEMPERATURE_VALUE = 0.3
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 60_000
        const val CLAUDE_DEFAULT_MAX_TOKENS = 600
    }
}
