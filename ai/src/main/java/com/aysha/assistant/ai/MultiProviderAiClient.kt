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
import java.util.concurrent.atomic.AtomicInteger

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
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var cachedWorkingGeminiModel: String? = null

    // Circuit breaker state per provider
    private val circuitBreakerFailures = mutableMapOf<BrainProvider, AtomicInteger>()
    private val circuitBreakerLastFailure = mutableMapOf<BrainProvider, Long>()
    private val CIRCUIT_BREAKER_THRESHOLD = 3
    private val CIRCUIT_BREAKER_RESET_MS = 60_000L // 1 minute

    private fun sanitizeKey(raw: String): String =
        raw.trim().replace(Regex("\\s+"), "")

    private fun isCircuitOpen(provider: BrainProvider): Boolean {
        val failures = circuitBreakerFailures[provider]?.get() ?: 0
        val lastFailure = circuitBreakerLastFailure[provider] ?: 0L
        if (failures >= CIRCUIT_BREAKER_THRESHOLD) {
            if (System.currentTimeMillis() - lastFailure > CIRCUIT_BREAKER_RESET_MS) {
                // Reset circuit breaker after timeout
                circuitBreakerFailures[provider]?.set(0)
                circuitBreakerLastFailure.remove(provider)
                return false
            }
            return true
        }
        return false
    }

    private fun recordFailure(provider: BrainProvider) {
        val failures = circuitBreakerFailures.getOrPut(provider) { AtomicInteger() }
        failures.incrementAndGet()
        circuitBreakerLastFailure[provider] = System.currentTimeMillis()
    }

    private fun recordSuccess(provider: BrainProvider) {
        circuitBreakerFailures[provider]?.set(0)
        circuitBreakerLastFailure.remove(provider)
    }

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
                val preferred = LITE_FIRST_GEMINI_MODELS
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
            // OpenRouter requires additional headers
            if (root.contains("openrouter.ai", ignoreCase = true)) {
                reqBuilder.header("HTTP-Referer", "https://aysha.ai")
                reqBuilder.header("X-Title", "AYSHA AI Assistant")
            }
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
     * Builds the ordered list of cloud providers prioritizing Google Gemini API (100% Online, zero Offline model).
     * Validates that API keys are present and non-empty before adding to chain.
     * Skips providers with open circuit breakers.
     */
    fun buildProviderChain(settings: AppSettings): List<BrainProvider> {
        val candidates = mutableListOf<BrainProvider>()
        fun addIfConfigured(p: BrainProvider) {
            if (p == BrainProvider.OFFLINE) return
            val key = settings.currentKeyFor(p)
            val hasCreds = !p.requiresApiKey || key.isNotBlank()
            if (hasCreds && p !in candidates && !isCircuitOpen(p)) {
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
        if (chain.isEmpty()) {
            return@withContext AiTurnResult(
                text = "Gemini API Key missing hai — Settings → AI Provider Settings me apni free Google Gemini API key daalo.",
                providerTag = "Gemini Key Required",
            )
        }

        var lastFailureNote: String? = null
        for (provider in chain) {
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
                        baseUrl = settings.openAiBaseUrl.ifBlank { BrainProvider.OPENAI.defaultBaseUrl },
                        apiKey = settings.openAiApiKey,
                        model = settings.openAiModel,
                        systemInstruction = systemInstruction,
                        history = history,
                        userText = userText,
                        attachedImageJpeg = attachedImageJpeg,
                        toolExecutor = toolExecutor,
                        onToolEvent = onToolEvent,
                    )
                    BrainProvider.OFFLINE -> error("Offline mode removed")
                }
            }

            if (attempt.isSuccess) {
                val res = attempt.getOrThrow()
                recordSuccess(provider)
                return@withContext if (lastFailureNote != null) {
                    res.copy(providerTag = "${res.providerTag} (Fallback)")
                } else {
                    res
                }
            } else {
                val errMsg = SafeLogger.redact(attempt.exceptionOrNull()?.message ?: "${provider.displayName} failed")
                SafeLogger.w(TAG, "Provider ${provider.displayName} failed: $errMsg")
                recordFailure(provider)
                lastFailureNote = "${provider.displayName}: ${errMsg.take(160)}"
            }
        }

        AiTurnResult(
            text = lastFailureNote ?: "Gemini API se response nahi mila. Internet aur API key check karo.",
            providerTag = "API Error",
        )
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

    internal fun buildGeminiCandidateModels(configuredModel: String): List<String> {
        val cleanConfigured = configuredModel.trim()
        val isLegacyCongestedDefault = cleanConfigured.isBlank() ||
            cleanConfigured.equals("gemini-2.5-flash", ignoreCase = true)
        val cached = cachedWorkingGeminiModel
            ?.takeUnless { it.equals("gemini-2.5-flash", ignoreCase = true) }

        return buildList {
            if (!isLegacyCongestedDefault) add(cleanConfigured)
            if (cached != null) add(cached)
            addAll(LITE_FIRST_GEMINI_MODELS)
        }.distinct()
    }

    internal fun isRetryableGeminiError(httpCode: Int, message: String?): Boolean {
        if (httpCode in RETRYABLE_HTTP_CODES) return true
        if (message.isNullOrBlank()) return false
        return RETRYABLE_ERROR_HINTS.any { hint -> message.contains(hint, ignoreCase = true) }
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
        val candidateModels = buildGeminiCandidateModels(model)

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
            val modelsForRound = if (round == 0) {
                candidateModels
            } else {
                listOf(activeModel) + candidateModels.filter { it != activeModel }
            }

            for (tryModel in modelsForRound) {
                var includeTools = true
                for (schemaAttempt in 0..1) {
                    val payload = JSONObject().apply {
                        put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                        put("contents", contents)
                        if (includeTools) put("tools", toolsArray)
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

                    val callOutcome = runCatching {
                        http.newCall(req).execute().use { resp ->
                            val body = resp.body?.string().orEmpty()
                            if (resp.isSuccessful) {
                                activeModel = tryModel
                                cachedWorkingGeminiModel = tryModel
                                JSONObject(body)
                            } else {
                                val errMsg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                                val detail = "Gemini HTTP ${resp.code}: ${errMsg ?: body.take(180)}"
                                val isSchemaErr = resp.code == 400 && (
                                    errMsg?.contains("tool", ignoreCase = true) == true ||
                                        errMsg?.contains("function", ignoreCase = true) == true ||
                                        errMsg?.contains("schema", ignoreCase = true) == true ||
                                        errMsg?.contains("parameters", ignoreCase = true) == true
                                    )
                                if (isSchemaErr && includeTools) {
                                    throw RetryWithoutToolsException(detail)
                                }
                                val isModelNotSupported = resp.code == 404 || (resp.code == 400 && (errMsg?.contains("model", ignoreCase = true) == true))
                                when {
                                    isModelNotSupported -> throw UnsupportedModelException(detail)
                                    isRetryableGeminiError(resp.code, errMsg) -> throw TransientOverloadException(detail)
                                    else -> throw IllegalStateException(detail)
                                }
                            }
                        }
                    }.recoverCatching { err ->
                        when (err) {
                            is RetryWithoutToolsException,
                            is UnsupportedModelException,
                            is TransientOverloadException -> throw err
                            is java.io.IOException -> throw TransientOverloadException("Gemini network/timeout on $tryModel: ${err.message ?: "timed out"}")
                            else -> throw err
                        }
                    }

                    if (callOutcome.isSuccess) {
                        respJson = callOutcome.getOrThrow()
                        break
                    } else {
                        val err = callOutcome.exceptionOrNull()
                        if (err is RetryWithoutToolsException) {
                            includeTools = false
                            continue
                        }
                        if (err is UnsupportedModelException || err is TransientOverloadException) {
                            if (cachedWorkingGeminiModel == tryModel) {
                                cachedWorkingGeminiModel = null
                            }
                            SafeLogger.w(TAG, "Switching away from Gemini model $tryModel: ${err.message}")
                        }
                        lastErr = err as? IllegalStateException ?: IllegalStateException(err?.message ?: "Gemini call failed")
                        break
                    }
                }
                if (respJson != null) break
                if (lastErr != null && lastErr !is UnsupportedModelException && lastErr !is TransientOverloadException) {
                    throw lastErr
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
        val root = baseUrl.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }
        val endpoint = "$root/chat/completions"

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
            // OpenRouter requires additional headers (case-insensitive check)
            if (root.contains("openrouter.ai", ignoreCase = true)) {
                builder.header("HTTP-Referer", "https://aysha.ai")
                builder.header("X-Title", "AYSHA AI Assistant")
            }

            val respJson = http.newCall(builder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    if (resp.code in RETRYABLE_HTTP_CODES) {
                        throw TransientOverloadException("${provider.displayName} overloaded (${resp.code})")
                    }
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
    private class RetryWithoutToolsException(message: String) : IllegalStateException(message)
    private class TransientOverloadException(message: String) : IllegalStateException(message)

    companion object {
        private const val TAG = "AyshaMultiAi"
        private val RETRYABLE_HTTP_CODES = setOf(408, 429, 500, 502, 503, 504)
        private val RETRYABLE_ERROR_HINTS = listOf(
            "high demand",
            "overloaded",
            "unavailable",
            "resource exhausted",
            "rate limit",
            "quota",
            "temporarily",
            "try again",
        )
        private val LITE_FIRST_GEMINI_MODELS = listOf(
            "gemini-2.5-flash-lite",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash",
            "gemini-3.6-flash",
            "gemini-3.8-flash",
            "gemini-3-flash-preview",
        )
    }
}