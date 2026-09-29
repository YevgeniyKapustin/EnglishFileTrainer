package dev.eftrainer.data.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dev.eftrainer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AiServer(
    val url: String,
    val appKey: String,
) {
    val configured: Boolean get() = url.isNotBlank() && appKey.isNotBlank()
    val chatUrl: String get() = url.trim().trimEnd('/') + "/v1/chat"

    companion object {
        fun fromBuildConfig() = AiServer(BuildConfig.AI_SERVER_URL, BuildConfig.AI_APP_KEY)
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
    server: AiServer,
    connectivity: Connectivity,
): Flow<Boolean> = connectivity.online.map { online -> server.configured && online }

class AiException(
    val reason: Reason,
    cause: Throwable? = null,
) : Exception(reason.name, cause) {
    enum class Reason { UNAVAILABLE, LIMIT, NETWORK, SERVER }
}

internal fun failureFor(code: Int): AiException.Reason? =
    when (code) {
        HttpURLConnection.HTTP_OK -> null
        HttpURLConnection.HTTP_UNAUTHORIZED -> AiException.Reason.UNAVAILABLE
        HTTP_TOO_MANY_REQUESTS -> AiException.Reason.LIMIT
        else -> AiException.Reason.SERVER
    }

internal fun chatBody(messages: List<Pair<String, String>>): JSONObject =
    JSONObject()
        .put("temperature", TEMPERATURE)
        .put(
            "messages",
            JSONArray(messages.map { (role, text) -> JSONObject().put("role", role).put(CONTENT, text) }),
        )

internal fun replyText(json: JSONObject): String =
    json
        .getJSONArray("choices")
        .getJSONObject(0)
        .getJSONObject("message")
        .getString(CONTENT)
        .trim()

class AiClient(
    private val server: AiServer,
) {
    suspend fun chat(messages: List<Pair<String, String>>): String {
        if (!server.configured) throw AiException(AiException.Reason.UNAVAILABLE)
        return withContext(Dispatchers.IO) { post(messages) }
    }

    private fun post(messages: List<Pair<String, String>>): String {
        val conn =
            (URL(server.chatUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer ${server.appKey.trim()}")
            }
        return try {
            conn.outputStream.use { it.write(chatBody(messages).toString().toByteArray()) }
            readReply(conn)
        } catch (e: IOException) {
            throw AiException(AiException.Reason.NETWORK, e)
        } catch (e: JSONException) {
            throw AiException(AiException.Reason.SERVER, e)
        } finally {
            conn.disconnect()
        }
    }

    private fun readReply(conn: HttpURLConnection): String {
        failureFor(conn.responseCode)?.let { throw AiException(it) }
        return replyText(JSONObject(conn.inputStream.use { it.readBytes().decodeToString() }))
    }
}

private const val CONTENT = "content"
private const val TEMPERATURE = 0.3
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val CONNECT_TIMEOUT_MS = 15_000
private const val READ_TIMEOUT_MS = 60_000
