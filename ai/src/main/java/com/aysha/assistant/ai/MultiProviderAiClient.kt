package com.aysha.assistant.ai

import android.util.Base64
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.PendingActionConfirmation
import com.aysha.assistant.core.SafeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiTurnResult(
    val text: String,
    val providerTag: String,
    val toolsUsed: List<String> = emptyList(),
    val confirmationRequired: PendingActionConfirmation? = null,
    val inAppTimerRequest: Pair<Int, String>? = null,
)

data class ConnectivityCheckResult(
    val ok: Boolean,
    val message: String,
    val models: List<String> = emptyList(),
)

class MultiProviderAiClient {

    private val http = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedWorkingGeminiModel: String? = null

    private fun sanitizeKey(raw: String): String =
        raw.trim().replace(Regex("\\s+"), "")

    suspend fun testConnection(
        provider: BrainProvider,
        apiKey: String,
        baseUrl: String = provider.defaultBaseUrl,
        model: String = provider.defaultModel,
    ): ConnectivityCheckResult = withContext(Dispatchers.IO) {
        if (provider == BrainProvider.OFFLINE) {
            return@withContext ConnectivityCheckResult(
                ok = true,
                message = "Offline Mode active — zero API key required!",
                models = listOf("aysha-local-engine"),
            )
        }

        val cleanKey = sanitizeKey(apiKey)
        if (provider.requiresApiKey && cleanKey.isEmpty()) {
            return@withContext ConnectivityCheckResult(
                ok = false,
                message = "${provider.displayName} API key empty hai. Key enter karke test karo.",
            )
        }

        when (provider) {
            BrainProvider.GEMINI -> testGemini(cleanKey)
            else -> testOpenAiCompatible(provider, baseUrl, cleanKey, model)
        }
    }

    private fun testGemini(cleanKey: String): ConnectivityCheckResult {
        return runCatching {
            val req = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?pageSize=60&key=$cleanKey")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val err = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                    return@runCatching ConnectivityCheckResult(
                        ok = false,
                        message = SafeLogger.redact("Gemini HTTP ${resp.code}: ${err ?: body.take(180)}"),
                    )
                }
                val arr = JSONObject(body).optJSONArray("models") ?: JSONArray()
                val models = (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.optString("name")?.removePrefix("models/")
                }.sorted()
                val preferred = listOf("gemini-3.8-flash", "gemini-3-flash-preview", "gemini-2.5-flash", "gemini-2.0-flash")
                    .firstOrNull { cand -> models.any { it.equals(cand, ignoreCase = true) } }
                if (preferred != null) {
                    cachedWorkingGeminiModel = preferred
                }
                ConnectivityCheckResult(
                    ok = true,
                    message = "Gemini connected! ${models.size} models verified${preferred?.let { " · Active: $it" }.orEmpty()}.",
                    models = models,
                )
            }
        }.getOrElse { err ->
            ConnectivityCheckResult(false, SafeLogger.redact("Connection failed: ${err.message ?: "Network error"}"))
        }
    }

    private fun testOpenAiCompatible(
        provider: BrainProvider,
        baseUrl: String,
        cleanKey: String,
        model: String,
    ): ConnectivityCheckResult {
        return runCatching {
            val root = baseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }
            val reqBuilder = Request.Builder().url("$root/models").get()
            if (cleanKey.isNotEmpty()) reqBuilder.header("Authorization", "Bearer $cleanKey")
            http.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@runCatching ConnectivityCheckResult(
                        ok = false,
                        message = SafeLogger.redact("${provider.displayName} HTTP ${resp.code}: ${body.take(160)}"),
                    )
                }
                val arr = runCatching { JSONObject(body).optJSONArray("data") }.getOrNull() ?: JSONArray()
                val models = (0 until arr.length()).mapNotNull { i ->
                    arr.optJSONObject(i)?.optString("id")?.takeIf(String::isNotBlank)
                }.sorted()
                ConnectivityCheckResult(
                    ok = true,
                    message = "${provider.displayName} connected! (${models.size.coerceAtLeast(1)} models)",
                    models = models.ifEmpty { listOf(model) },
                )
            }
        }.getOrElse { err ->
            ConnectivityCheckResult(false, SafeLogger.redact("Connection failed: ${err.message ?: "Host unreachable"}"))
        }
    }

    /**
     * Builds the ordered list of providers to attempt for a request, prioritizing Google Gemini API
     * (Local / LAN model mode removed).
     */
    fun buildProviderChain(settings: AppSettings): List<BrainProvider> {
        val candidates = mutableListOf<BrainProvider>()
        fun addIfConfigured(p: BrainProvider) {
            if (p == BrainProvider.OFFLINE) return
            val hasCreds = !p.requiresApiKey || settings.currentKeyFor(p).isNotBlank()
            if (hasCreds && p !in candidates) {
                candidates.add(p)
            }
        }
        addIfConfigured(BrainProvider.GEMINI)
        if (settings.activeProvider != BrainProvider.OFFLINE) {
            addIfConfigured(settings.activeProvider)
        }
        if (settings.fallbackEnabled) {
            listOf(BrainProvider.GEMINI, BrainProvider.GROQ, BrainProvider.OPENAI)
                .forEach { addIfConfigured(it) }
        }
        candidates.add(BrainProvider.OFFLINE)
        return candidates
    }

    suspend fun runTurnWithFallback(
        settings: AppSettings,
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String,
        attachedImageJpeg: ByteArray? = null,
        toolExecutor: ToolExecutor,
        onToolEvent: (String) -> Unit = {},
    ): AiTurnResult = withContext(Dispatchers.IO) {
        val chain = buildProviderChain(settings)
        var lastFailureNote: String? = null

        for (provider in chain) {
            if (provider == BrainProvider.OFFLINE) {
                val offline = runOfflineTurn(settings, userText, toolExecutor, onToolEvent)
                val finalTag = if (lastFailureNote != null) "Offline Fallback" else "Offline Mode"
                val prefix = if (lastFailureNote != null && offline.confirmationRequired == null && offline.inAppTimerRequest == null) {
                    "($lastFailureNote — switched to Offline Mode)\n\n"
                } else {
                    ""
                }
                return@withContext offline.copy(
                    text = prefix + offline.text,
                    providerTag = finalTag,
                )
            }

            val attempt = runCatching {
                when (provider) {
                    BrainProvider.GEMINI -> runGeminiTurn(
                        apiKey = settings.geminiApiKey,
                        model = settings.geminiTextModel,
                        systemInstruction = systemInstruction,
                        history = history,
                        userText = userText,
                        attachedImageJpeg = attachedImageJpeg,
                        toolExecutor = toolExecutor,
                        onToolEvent = onToolEvent,
                    )
                    BrainProvider.GROQ -> runOpenAiTurn(
                        provider = BrainProvider.GROQ,
                        baseUrl = BrainProvider.GROQ.defaultBaseUrl,
                        apiKey = settings.groqApiKey,
                        model = settings.groqModel,
                        systemInstruction = systemInstruction,
                        history = history,
                        userText = userText,
                        attachedImageJpeg = null,
                        toolExecutor = toolExecutor,
                        onToolEvent = onToolEvent,
                    )
                    BrainProvider.OPENAI -> runOpenAiTurn(
                        provider = BrainProvider.OPENAI,
                        baseUrl = settings.openAiBaseUrl,
                        apiKey = settings.openAiApiKey,
                        model = settings.openAiModel,
                        systemInstruction = systemInstruction,
                        history = history,
                        userText = userText,
                        attachedImageJpeg = attachedImageJpeg,
                        toolExecutor = toolExecutor,
                        onToolEvent = onToolEvent,
                    )
                    BrainProvider.OFFLINE -> error("Handled above")
                }
            }

            if (attempt.isSuccess) {
                val res = attempt.getOrThrow()
                return@withContext if (lastFailureNote != null) {
                    res.copy(providerTag = "${res.providerTag} (Fallback)")
                } else {
                    res
                }
            } else {
                val errMsg = SafeLogger.redact(attempt.exceptionOrNull()?.message ?: "${provider.displayName} failed")
                SafeLogger.w(TAG, "Provider ${provider.displayName} failed: $errMsg")
                lastFailureNote = "${provider.displayName} unavailable"
            }
        }

        runOfflineTurn(settings, userText, toolExecutor, onToolEvent)
    }

    private suspend fun runOfflineTurn(
        settings: AppSettings,
        userText: String,
        toolExecutor: ToolExecutor,
        onToolEvent: (String) -> Unit,
    ): AiTurnResult {
        return when (val res = OfflineCommandParser.parse(userText, settings.assistantName, settings.userName, settings.language)) {
            is OfflineResolution.DirectReply -> AiTurnResult(
                text = res.reply,
                providerTag = "Offline Mode",
            )
            is OfflineResolution.InAppTimer -> AiTurnResult(
                text = "⏱ ${res.label} started for **${OfflineCommandParser.formatClock(res.seconds.toLong())}**!",
                providerTag = "Offline Timer",
                inAppTimerRequest = res.seconds to res.label,
            )
            is OfflineResolution.ToolCall -> {
                onToolEvent(res.statusLabel)
                val out = toolExecutor.execute(res.toolName, res.args)
                AiTurnResult(
                    text = out.message,
                    providerTag = "Offline · ${res.toolName}",
                    toolsUsed = listOf(res.toolName),
                    confirmationRequired = out.confirmationRequired,
                )
            }
        }
    }

    private suspend fun runGeminiTurn(
        apiKey: String,
        model: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String,
        attachedImageJpeg: ByteArray?,
        toolExecutor: ToolExecutor,
        onToolEvent: (String) -> Unit,
    ): AiTurnResult {
        val cleanKey = sanitizeKey(apiKey)
        require(cleanKey.isNotEmpty()) { "Gemini API key missing" }
        val candidateModels = listOfNotNull(
            model.trim().ifBlank { null },
            cachedWorkingGeminiModel,
            BrainProvider.GEMINI.defaultModel,
            "gemini-3-flash-preview",
            "gemini-2.5-flash",
            "gemini-2.0-flash",
        ).distinct()

        val contents = JSONArray()
        for (msg in history.takeLast(8)) {
            if (msg.role != ChatRole.USER && msg.role != ChatRole.ASSISTANT) continue
            if (msg.text.isBlank()) continue
            contents.put(
                JSONObject()
                    .put("role", if (msg.role == ChatRole.USER) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", msg.text))),
            )
        }

        val userParts = JSONArray().apply {
            if (userText.isNotBlank()) put(JSONObject().put("text", userText))
            if (attachedImageJpeg != null) {
                put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", Base64.encodeToString(attachedImageJpeg, Base64.NO_WRAP)),
                    ),
                )
            }
        }
        contents.put(JSONObject().put("role", "user").put("parts", userParts))

        val toolsArray = JSONArray().put(
            JSONObject().put("functionDeclarations", ToolRegistry.toGeminiFunctionDeclarations()),
        )

        val toolsUsed = mutableListOf<String>()
        var pendingConfirm: PendingActionConfirmation? = null
        var activeModel = candidateModels.first()

        for (round in 0 until 5) {
            var respJson: JSONObject? = null
            var lastErr: IllegalStateException? = null
            val modelsForRound = if (round == 0) candidateModels else listOf(activeModel)

            for (tryModel in modelsForRound) {
                val payload = JSONObject().apply {
                    put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                    put("contents", contents)
                    put("tools", toolsArray)
                    put(
                        "generationConfig",
                        JSONObject()
                            .put("temperature", 0.7)
                            .put("maxOutputTokens", 768),
                    )
                }

                val req = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$tryModel:generateContent?key=$cleanKey")
                    .post(payload.toString().toRequestBody(jsonMedia))
                    .build()

                val callOutcome = http.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (resp.isSuccessful) {
                        activeModel = tryModel
                        cachedWorkingGeminiModel = tryModel
                        Result.success(JSONObject(body))
                    } else {
                        val errMsg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                        val ex = IllegalStateException("Gemini HTTP ${resp.code}: ${errMsg ?: body.take(180)}")
                        val isModelNotSupported = resp.code == 404 || (resp.code == 400 && (errMsg?.contains("model", ignoreCase = true) == true))
                        Result.failure<JSONObject>(if (isModelNotSupported) UnsupportedModelException(ex.message ?: "") else ex)
                    }
                }
                if (callOutcome.isSuccess) {
                    respJson = callOutcome.getOrThrow()
                    break
                } else {
                    val err = callOutcome.exceptionOrNull()
                    if (err is UnsupportedModelException) {
                        lastErr = IllegalStateException(err.message)
                        continue
                    } else {
                        throw (err as? IllegalStateException ?: IllegalStateException(err?.message ?: "Gemini call failed"))
                    }
                }
            }

            val resolvedJson = respJson ?: throw (lastErr ?: IllegalStateException("No supported Gemini model responded"))

            val modelContent = resolvedJson.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?: throw IllegalStateException("Gemini returned empty response")
            contents.put(modelContent)

            val parts = modelContent.optJSONArray("parts") ?: JSONArray()
            val functionCalls = mutableListOf<JSONObject>()
            val textParts = mutableListOf<String>()

            for (i in 0 until parts.length()) {
                val p = parts.optJSONObject(i) ?: continue
                if (p.has("text") && !p.optBoolean("thought", false)) {
                    val t = p.optString("text")
                    if (t.isNotBlank()) textParts.add(t)
                }
                p.optJSONObject("functionCall")?.let { functionCalls.add(it) }
            }

            if (functionCalls.isEmpty()) {
                return AiTurnResult(
                    text = textParts.joinToString("\n").ifBlank { "Done." },
                    providerTag = "Gemini · $activeModel",
                    toolsUsed = toolsUsed,
                    confirmationRequired = pendingConfirm,
                )
            }

            val responseParts = JSONArray()
            for (fc in functionCalls) {
                val fnName = fc.optString("name")
                if (fnName.isBlank()) continue
                val fnArgs = fc.optJSONObject("args") ?: JSONObject()
                toolsUsed.add(fnName)
                onToolEvent("Running $fnName...")
                val outcome = toolExecutor.execute(fnName, fnArgs)
                if (outcome.confirmationRequired != null) {
                    pendingConfirm = outcome.confirmationRequired
                }
                responseParts.put(
                    JSONObject().put(
                        "functionResponse",
                        JSONObject()
                            .put("name", fnName)
                            .put("response", JSONObject().put("result", outcome.output)),
                    ),
                )
            }
            contents.put(JSONObject().put("role", "user").put("parts", responseParts))
        }

        return AiTurnResult(
            text = "Actions executed (${toolsUsed.joinToString(", ")}).",
            providerTag = "Gemini · $activeModel",
            toolsUsed = toolsUsed,
            confirmationRequired = pendingConfirm,
        )
    }

    private suspend fun runOpenAiTurn(
        provider: BrainProvider,
        baseUrl: String,
        apiKey: String,
        model: String,
        systemInstruction: String,
        history: List<ChatMessage>,
        userText: String,
        attachedImageJpeg: ByteArray?,
        toolExecutor: ToolExecutor,
        onToolEvent: (String) -> Unit,
    ): AiTurnResult {
        val cleanKey = apiKey.trim()
        if (provider.requiresApiKey) require(cleanKey.isNotEmpty()) { "${provider.displayName} API key missing" }
        val safeModel = model.ifBlank { provider.defaultModel }
        val endpoint = baseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl } + "/chat/completions"

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemInstruction))
            for (msg in history.takeLast(14)) {
                if (msg.role != ChatRole.USER && msg.role != ChatRole.ASSISTANT) continue
                if (msg.text.isBlank()) continue
                put(
                    JSONObject()
                        .put("role", if (msg.role == ChatRole.USER) "user" else "assistant")
                        .put("content", msg.text),
                )
            }
            if (attachedImageJpeg != null) {
                val b64 = Base64.encodeToString(attachedImageJpeg, Base64.NO_WRAP)
                val arr = JSONArray()
                    .put(JSONObject().put("type", "text").put("text", userText.ifBlank { "Describe this image" }))
                    .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$b64")))
                put(JSONObject().put("role", "user").put("content", arr))
            } else {
                put(JSONObject().put("role", "user").put("content", userText))
            }
        }

        val tools = ToolRegistry.toOpenAiTools()
        val toolsUsed = mutableListOf<String>()
        var pendingConfirm: PendingActionConfirmation? = null

        for (round in 0 until 5) {
            val payload = JSONObject().apply {
                put("model", safeModel)
                put("messages", messages)
                put("tools", tools)
                put("tool_choice", "auto")
            }
            val builder = Request.Builder()
                .url(endpoint)
                .post(payload.toString().toRequestBody(jsonMedia))
            if (cleanKey.isNotEmpty()) builder.header("Authorization", "Bearer $cleanKey")

            val respJson = http.newCall(builder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    throw IllegalStateException("${provider.displayName} HTTP ${resp.code}: ${body.take(180)}")
                }
                JSONObject(body)
            }

            val msgObj = respJson.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?: throw IllegalStateException("${provider.displayName} empty message")
            messages.put(msgObj)

            val toolCalls = msgObj.optJSONArray("tool_calls")
            if (toolCalls == null || toolCalls.length() == 0) {
                val reply = msgObj.optString("content").ifBlank { "Done." }
                return AiTurnResult(
                    text = reply,
                    providerTag = "${provider.displayName} · $safeModel",
                    toolsUsed = toolsUsed,
                    confirmationRequired = pendingConfirm,
                )
            }

            for (i in 0 until toolCalls.length()) {
                val tc = toolCalls.optJSONObject(i) ?: continue
                val id = tc.optString("id", "call_$i")
                val fn = tc.optJSONObject("function") ?: continue
                val fnName = fn.optString("name")
                if (fnName.isBlank()) continue
                val fnArgs = runCatching { JSONObject(fn.optString("arguments", "{}")) }.getOrElse { JSONObject() }
                toolsUsed.add(fnName)
                onToolEvent("Running $fnName...")
                val outcome = toolExecutor.execute(fnName, fnArgs)
                if (outcome.confirmationRequired != null) {
                    pendingConfirm = outcome.confirmationRequired
                }
                messages.put(
                    JSONObject()
                        .put("role", "tool")
                        .put("tool_call_id", id)
                        .put("content", outcome.output.toString()),
                )
            }
        }

        return AiTurnResult(
            text = "Actions executed (${toolsUsed.joinToString(", ")}).",
            providerTag = "${provider.displayName} · $safeModel",
            toolsUsed = toolsUsed,
            confirmationRequired = pendingConfirm,
        )
    }

    private class UnsupportedModelException(message: String) : IllegalStateException(message)

    companion object {
        private const val TAG = "AyshaMultiAi"
    }
}
