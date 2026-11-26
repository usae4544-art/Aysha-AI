package com.aysha.assistant.ai

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.util.Base64
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.PendingActionConfirmation
import com.aysha.assistant.core.SafeLogger
import com.aysha.assistant.core.VoicePersonalities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

interface LiveCallbacks {
    fun onUserTranscript(text: String)
    fun onModelTranscript(text: String)
    fun onToolStatus(summary: String)
    fun onConfirmationRequired(confirmation: PendingActionConfirmation)
    fun onError(message: String)
}

class GeminiLiveClient(
    private val toolExecutor: ToolExecutor,
    private val callbacks: LiveCallbacks,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _statusText = MutableStateFlow("Tap Orb to talk")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    @Volatile
    private var webSocket: WebSocket? = null

    @Volatile
    private var setupComplete = false

    @Volatile
    private var userStopped = false

    @Volatile
    private var muted = false

    @Volatile
    private var resumeHandle: String? = null

    @Volatile
    private var activeSettings: AppSettings? = null

    @Volatile
    private var activeSystemInstruction: String = ""

    private var recordJob: Job? = null
    private var playJob: Job? = null
    private var reconnectJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null

    private val playbackQueue = LinkedBlockingQueue<ByteArray>(220)
    private val userTranscriptBuf = StringBuilder()
    private val modelTranscriptBuf = StringBuilder()

    fun isActive(): Boolean = webSocket != null || reconnectJob?.isActive == true

    fun setMuted(isMuted: Boolean) {
        muted = isMuted
        if (isMuted && setupComplete) {
            runCatching {
                webSocket?.send(JSONObject().put("realtimeInput", JSONObject().put("audioStreamEnd", true)).toString())
            }
        }
        _statusText.value = if (isMuted) "Mic muted" else "Listening..."
    }

    fun start(settings: AppSettings, systemInstruction: String) {
        stop()
        val key = settings.geminiApiKey.trim()
        if (key.isEmpty()) {
            _orbState.value = OrbState.ERROR
            _statusText.value = "Gemini API key missing"
            callbacks.onError("Gemini Live ke liye Settings → AI Provider Settings me apni free Google Gemini API key daalo.")
            return
        }
        userStopped = false
        resumeHandle = null
        activeSettings = settings
        activeSystemInstruction = systemInstruction
        connect(isResume = false)
    }

    private fun connect(isResume: Boolean) {
        val s = activeSettings ?: return
        val key = s.geminiApiKey.trim()
        if (key.isEmpty() || userStopped) return

        _orbState.value = OrbState.CONNECTING
        _statusText.value = if (isResume) "Reconnecting live voice..." else "Connecting Gemini Live..."
        setupComplete = false

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$key"
        val req = Request.Builder().url(wsUrl).build()

        webSocket = http.newWebSocket(
            req,
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    val modelId = s.geminiLiveModel.trim().ifBlank { "gemini-3.8-live" }
                    val voiceName = VoicePersonalities.byId(s.voicePersonalityId).geminiLiveVoice
                    val payload = buildSetupPayload(modelId, voiceName, activeSystemInstruction, resumeHandle)
                    runCatching { ws.send(payload.toString()) }
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleServerMessage(ws, text)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    val detail = SafeLogger.redact(
                        runCatching { response?.body?.string()?.take(160) }.getOrNull() ?: t.message ?: "Connection failed",
                    )
                    cleanupAudio()
                    webSocket = null
                    setupComplete = false

                    if (!userStopped && !resumeHandle.isNullOrBlank()) {
                        scheduleReconnect()
                        return
                    }
                    _orbState.value = OrbState.ERROR
                    _statusText.value = "Live connection error"
                    callbacks.onError("Gemini Live failed: $detail")
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    cleanupAudio()
                    webSocket = null
                    setupComplete = false
                    if (!userStopped && !resumeHandle.isNullOrBlank()) {
                        scheduleReconnect()
                        return
                    }
                    if (_orbState.value != OrbState.ERROR) {
                        _orbState.value = OrbState.IDLE
                        _statusText.value = "Tap Orb to talk"
                    }
                }
            },
        )
    }

    private fun scheduleReconnect() {
        if (userStopped || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            _orbState.value = OrbState.CONNECTING
            _statusText.value = "Refreshing live session..."
            delay(650L)
            if (!userStopped) connect(isResume = true)
        }
    }

    private fun buildSetupPayload(
        modelName: String,
        voiceName: String,
        systemInstruction: String,
        handle: String?,
    ): JSONObject {
        val cleanModel = modelName.removePrefix("models/")
        val isThinking = cleanModel.contains("extended-thinking", ignoreCase = true)

        val setup = JSONObject().apply {
            put("model", "models/$cleanModel")
            put(
                "generationConfig",
                JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName)),
                        ),
                    )
                    if (isThinking) put("thinkingConfig", JSONObject().put("thinkingLevel", "low"))
                },
            )
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            put("inputAudioTranscription", JSONObject())
            put("outputAudioTranscription", JSONObject())
            put("contextWindowCompression", JSONObject().put("slidingWindow", JSONObject()))
            put(
                "sessionResumption",
                JSONObject().apply {
                    if (!handle.isNullOrBlank()) put("handle", handle)
                },
            )
            put(
                "tools",
                JSONArray().put(
                    JSONObject().put("functionDeclarations", ToolRegistry.toGeminiFunctionDeclarations(nonBlocking = isThinking)),
                ),
            )
        }
        return JSONObject().put("setup", setup)
    }

    private fun handleServerMessage(ws: WebSocket, raw: String) {
        val msg = runCatching { JSONObject(raw) }.getOrNull() ?: return
        if (msg.has("setupComplete")) {
            setupComplete = true
            _orbState.value = OrbState.LISTENING
            _statusText.value = "Listening..."
            startAudioLoops()
            return
        }

        msg.optJSONObject("sessionResumptionUpdate")?.let { upd ->
            if (upd.optBoolean("resumable", false)) {
                val h = upd.optString("newHandle")
                if (h.isNotBlank()) resumeHandle = h
            }
        }
        if (msg.has("goAway") && !userStopped && !resumeHandle.isNullOrBlank()) {
            runCatching { ws.close(1000, "Resuming") }
        }

        val interactionStatus = msg.optString("interactionStatus")
        if (interactionStatus == "IDLE" && playbackQueue.isEmpty() && _orbState.value != OrbState.ERROR) {
            _orbState.value = OrbState.LISTENING
            _statusText.value = "Listening..."
        }

        val serverContent = msg.optJSONObject("serverContent")
        if (serverContent != null) {
            if (serverContent.optBoolean("interrupted", false)) {
                playbackQueue.clear()
                runCatching {
                    audioTrack?.pause()
                    audioTrack?.flush()
                    audioTrack?.play()
                }
                flushModelTranscript()
                _orbState.value = OrbState.LISTENING
                _statusText.value = "Listening..."
            }

            serverContent.optJSONObject("inputTranscription")?.optString("text")?.let { chunk ->
                if (chunk.isNotEmpty()) {
                    userTranscriptBuf.append(chunk)
                    _statusText.value = userTranscriptBuf.toString().takeLast(60)
                }
            }
            serverContent.optJSONObject("outputTranscription")?.optString("text")?.let { chunk ->
                if (chunk.isNotEmpty()) {
                    flushUserTranscript()
                    modelTranscriptBuf.append(chunk)
                }
            }

            val parts = serverContent.optJSONObject("modelTurn")?.optJSONArray("parts")
            if (parts != null) {
                flushUserTranscript()
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val inline = part.optJSONObject("inlineData")
                    if (inline != null) {
                        val b64 = inline.optString("data")
                        if (b64.isNotEmpty()) {
                            val pcm = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
                            if (pcm != null && pcm.isNotEmpty()) {
                                if (playbackQueue.remainingCapacity() == 0) playbackQueue.poll()
                                playbackQueue.offer(pcm)
                                _orbState.value = OrbState.SPEAKING
                                _statusText.value = "Speaking..."
                            }
                        }
                    }
                    val txt = part.optString("text")
                    if (txt.isNotBlank() && !part.optBoolean("thought", false)) {
                        modelTranscriptBuf.append(txt)
                    }
                }
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                flushUserTranscript()
                flushModelTranscript()
                if (interactionStatus != "IN_PROGRESS" && (_orbState.value == OrbState.SPEAKING || _orbState.value == OrbState.PROCESSING)) {
                    _orbState.value = OrbState.LISTENING
                    _statusText.value = "Listening..."
                }
            }
        }

        val toolCall = msg.optJSONObject("toolCall")
        if (toolCall != null) {
            val calls = toolCall.optJSONArray("functionCalls")
            if (calls != null && calls.length() > 0) {
                flushUserTranscript()
                _orbState.value = OrbState.AUTOMATING
                _statusText.value = "Running action..."
                scope.launch {
                    val responses = JSONArray()
                    for (i in 0 until calls.length()) {
                        val fc = calls.optJSONObject(i) ?: continue
                        val id = fc.optString("id")
                        val name = fc.optString("name")
                        if (name.isBlank()) continue
                        val args = fc.optJSONObject("args") ?: JSONObject()
                        callbacks.onToolStatus("Running $name...")
                        val outcome = toolExecutor.execute(name, args)
                        if (outcome.confirmationRequired != null) {
                            callbacks.onConfirmationRequired(outcome.confirmationRequired)
                        }
                        responses.put(
                            JSONObject().apply {
                                if (id.isNotBlank()) put("id", id)
                                put("name", name)
                                put("response", JSONObject().put("result", outcome.output))
                            },
                        )
                    }
                    val reply = JSONObject().put("toolResponse", JSONObject().put("functionResponses", responses))
                    runCatching { ws.send(reply.toString()) }
                    if (_orbState.value == OrbState.AUTOMATING) _orbState.value = OrbState.PROCESSING
                }
            }
        }
    }

    private fun flushUserTranscript() {
        val text = userTranscriptBuf.toString().trim()
        if (text.isNotEmpty()) {
            userTranscriptBuf.setLength(0)
            callbacks.onUserTranscript(text)
        }
    }

    private fun flushModelTranscript() {
        val text = modelTranscriptBuf.toString().trim()
        if (text.isNotEmpty()) {
            modelTranscriptBuf.setLength(0)
            callbacks.onModelTranscript(text)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioLoops() {
        cleanupAudio()
        val inRate = 16000
        val minRec = AudioRecord.getMinBufferSize(inRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minRec <= 0) {
            _orbState.value = OrbState.ERROR
            callbacks.onError("AudioRecord hardware unavailable.")
            return
        }
        val recorder = runCatching {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(inRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minRec.coerceAtLeast(3200) * 2)
                .build()
        }.getOrNull()

        if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { recorder?.release() }
            _orbState.value = OrbState.ERROR
            callbacks.onError("Microphone open nahi ho paya.")
            return
        }
        audioRecord = recorder
        if (AcousticEchoCanceler.isAvailable()) {
            aec = runCatching { AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true } }.getOrNull()
        }
        if (NoiseSuppressor.isAvailable()) {
            ns = runCatching { NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true } }.getOrNull()
        }

        val outRate = 24000
        val minPlay = AudioTrack.getMinBufferSize(outRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4800)
        val player = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(outRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minPlay * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull()

        if (player == null || player.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { player?.release() }
            cleanupAudio()
            _orbState.value = OrbState.ERROR
            callbacks.onError("Speaker output initialize nahi ho paya.")
            return
        }
        audioTrack = player

        if (runCatching { player.play(); recorder.startRecording() }.isFailure) {
            cleanupAudio()
            _orbState.value = OrbState.ERROR
            callbacks.onError("Mic recording start failed.")
            return
        }

        recordJob = scope.launch {
            val buf = ByteArray(1280)
            while (isActive && webSocket != null && setupComplete) {
                val read = runCatching { recorder.read(buf, 0, buf.size) }.getOrDefault(-1)
                if (read <= 0) {
                    if (read < 0) delay(20L)
                    continue
                }
                if (muted) {
                    _audioLevel.value = 0f
                    continue
                }
                _audioLevel.value = computeRms(buf, read)
                val b64 = Base64.encodeToString(buf, 0, read, Base64.NO_WRAP)
                val msg = JSONObject().put(
                    "realtimeInput",
                    JSONObject().put("audio", JSONObject().put("data", b64).put("mimeType", "audio/pcm;rate=16000")),
                )
                runCatching { webSocket?.send(msg.toString()) }
            }
        }

        playJob = scope.launch {
            while (isActive && webSocket != null) {
                val chunk = playbackQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                _audioLevel.value = computeRms(chunk, chunk.size)
                runCatching { player.write(chunk, 0, chunk.size) }
                if (playbackQueue.isEmpty() && _orbState.value == OrbState.SPEAKING) {
                    _orbState.value = OrbState.LISTENING
                    _statusText.value = "Listening..."
                }
            }
        }
    }

    fun sendText(text: String) {
        val ws = webSocket ?: return
        if (!setupComplete || text.isBlank()) return
        runCatching { ws.send(JSONObject().put("realtimeInput", JSONObject().put("text", text.trim())).toString()) }
    }

    fun interruptPlayback() {
        playbackQueue.clear()
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.play()
        }
        _orbState.value = OrbState.LISTENING
        _statusText.value = "Listening..."
    }

    fun stop() {
        userStopped = true
        resumeHandle = null
        reconnectJob?.cancel()
        reconnectJob = null
        flushUserTranscript()
        flushModelTranscript()
        runCatching { webSocket?.close(1000, "Stopped") }
        webSocket = null
        setupComplete = false
        cleanupAudio()
        _audioLevel.value = 0f
        _orbState.value = OrbState.IDLE
        _statusText.value = "Tap Orb to talk"
    }

    private fun cleanupAudio() {
        recordJob?.cancel()
        playJob?.cancel()
        recordJob = null
        playJob = null
        playbackQueue.clear()
        runCatching { aec?.release() }
        runCatching { ns?.release() }
        aec = null
        ns = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
        }
        runCatching { audioTrack?.release() }
        audioTrack = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun computeRms(pcm: ByteArray, length: Int): Float {
        if (length < 2) return 0f
        var sumSq = 0.0
        var count = 0
        var i = 0
        while (i + 1 < length) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort()
            val norm = sample / 32768.0
            sumSq += norm * norm
            count++
            i += 2
        }
        if (count == 0) return 0f
        return (sqrt(sumSq / count).toFloat() * 4.5f).coerceIn(0f, 1f)
    }
}
