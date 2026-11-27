package com.aysha.assistant.ai

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Build
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
    private val appContext: Context? = null,
) {
    private data class LiveEndpointCandidate(
        val model: String,
        val apiVersion: String,
    )

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

    @Volatile
    private var liveCandidates: List<LiveEndpointCandidate> = emptyList()

    @Volatile
    private var liveCandidateIdx: Int = 0

    @Volatile
    private var lastServerError: String? = null

    @Volatile
    private var previousAudioMode: Int? = null

    @Volatile
    private var previousSpeakerphone: Boolean? = null

    private var recordJob: Job? = null
    private var playJob: Job? = null
    private var reconnectJob: Job? = null
    private var setupTimeoutJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null
    private var agc: AutomaticGainControl? = null

    // Dual-stage DSP noise suppression state
    private var hpPrevIn = 0f
    private var hpPrevOut = 0f
    private var adaptiveNoiseFloor = 0.012f
    private var voiceGateHangover = 0
    @Volatile
    private var playbackActive = false

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
        val key = settings.geminiApiKey.trim().replace(Regex("\\s+"), "")
        if (key.isEmpty()) {
            _orbState.value = OrbState.ERROR
            _statusText.value = "Gemini API key missing"
            callbacks.onError("Gemini Live ke liye Settings → AI Provider Settings me apni free Google Gemini API key daalo.")
            return
        }
        userStopped = false
        resumeHandle = null
        lastServerError = null
        activeSettings = settings.copy(geminiApiKey = key)
        activeSystemInstruction = systemInstruction
        val configured = settings.geminiLiveModel.trim().removePrefix("models/")
        liveCandidates = buildList {
            if (configured.isNotBlank()) {
                val primaryVer = if (configured.contains("3.")) "v1alpha" else "v1beta"
                val altVer = if (primaryVer == "v1alpha") "v1beta" else "v1alpha"
                add(LiveEndpointCandidate(configured, primaryVer))
                add(LiveEndpointCandidate(configured, altVer))
            }
            add(LiveEndpointCandidate("gemini-3.8-live", "v1alpha"))
            add(LiveEndpointCandidate("gemini-3.8-live", "v1beta"))
            add(LiveEndpointCandidate("gemini-3.1-flash-live-preview", "v1alpha"))
            add(LiveEndpointCandidate("gemini-2.5-flash-native-audio-preview-09-2025", "v1beta"))
            add(LiveEndpointCandidate("gemini-2.5-flash-native-audio-preview-12-2025", "v1beta"))
            add(LiveEndpointCandidate("gemini-2.0-flash-live-001", "v1beta"))
            add(LiveEndpointCandidate("gemini-2.0-flash-exp", "v1alpha"))
        }.distinct()
        liveCandidateIdx = 0
        connect(isResume = false)
    }

    private fun connect(isResume: Boolean) {
        val s = activeSettings ?: return
        val key = s.geminiApiKey.trim().replace(Regex("\\s+"), "")
        if (key.isEmpty() || userStopped) return

        val candidate = liveCandidates.getOrElse(liveCandidateIdx) {
            LiveEndpointCandidate("gemini-3.8-live", "v1alpha")
        }
        val currentModel = candidate.model
        _orbState.value = OrbState.CONNECTING
        _statusText.value = if (isResume) "Reconnecting live voice..." else "Connecting Gemini Live..."
        setupComplete = false
        setupTimeoutJob?.cancel()

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.${candidate.apiVersion}.GenerativeService.BidiGenerateContent?key=$key"
        val req = Request.Builder().url(wsUrl).build()

        webSocket = http.newWebSocket(
            req,
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    val voiceName = VoicePersonalities.byId(s.voicePersonalityId).geminiLiveVoice
                    val payload = buildSetupPayload(currentModel, candidate.apiVersion, voiceName, activeSystemInstruction, resumeHandle)
                    runCatching { ws.send(payload.toString()) }
                    setupTimeoutJob?.cancel()
                    setupTimeoutJob = scope.launch {
                        delay(7000L)
                        if (!setupComplete && !userStopped && webSocket === ws) {
                            SafeLogger.w(TAG, "Setup timeout on $currentModel (${candidate.apiVersion})")
                            lastServerError = "Setup timeout on $currentModel"
                            runCatching { ws.close(1000, "Setup timeout") }
                        }
                    }
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleServerMessage(ws, text)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    setupTimeoutJob?.cancel()
                    val detail = SafeLogger.redact(
                        runCatching { response?.body?.string()?.take(160) }.getOrNull() ?: lastServerError ?: t.message ?: "Connection failed",
                    )
                    val wasSetupDone = setupComplete
                    cleanupAudio()
                    webSocket = null
                    setupComplete = false

                    if (!userStopped && !wasSetupDone && liveCandidateIdx + 1 < liveCandidates.size) {
                        liveCandidateIdx++
                        scheduleReconnect(delayMs = 180L, isResume = false)
                        return
                    }
                    if (!userStopped && !resumeHandle.isNullOrBlank()) {
                        scheduleReconnect()
                        return
                    }
                    _orbState.value = OrbState.ERROR
                    _statusText.value = "Live connection error"
                    callbacks.onError("Gemini Live failed ($currentModel): $detail")
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    setupTimeoutJob?.cancel()
                    val wasSetupDone = setupComplete
                    cleanupAudio()
                    webSocket = null
                    setupComplete = false
                    if (!userStopped && !wasSetupDone && liveCandidateIdx + 1 < liveCandidates.size) {
                        liveCandidateIdx++
                        scheduleReconnect(delayMs = 180L, isResume = false)
                        return
                    }
                    if (!userStopped && !resumeHandle.isNullOrBlank()) {
                        scheduleReconnect()
                        return
                    }
                    if (_orbState.value != OrbState.ERROR && !userStopped) {
                        val closeMsg = lastServerError ?: reason.takeIf { it.isNotBlank() && code != 1000 }
                        if (closeMsg != null && !wasSetupDone) {
                            _orbState.value = OrbState.ERROR
                            _statusText.value = "Live connection error"
                            callbacks.onError("Gemini Live closed ($currentModel): ${SafeLogger.redact(closeMsg)}")
                            return
                        }
                        _orbState.value = OrbState.IDLE
                        _statusText.value = "Tap Orb to talk"
                    }
                }
            },
        )
    }

    private fun scheduleReconnect(delayMs: Long = 650L, isResume: Boolean = true) {
        if (userStopped || reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            _orbState.value = OrbState.CONNECTING
            _statusText.value = if (isResume) "Refreshing live session..." else "Connecting Gemini Live..."
            delay(delayMs)
            if (!userStopped) connect(isResume = isResume)
        }
    }

    private fun buildSetupPayload(
        modelName: String,
        apiVersion: String,
        voiceName: String,
        systemInstruction: String,
        handle: String?,
    ): JSONObject {
        val cleanModel = modelName.removePrefix("models/")
        val isThinking = cleanModel.contains("extended-thinking", ignoreCase = true)
        val isLegacy20 = cleanModel.startsWith("gemini-2.0")

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
                    if (isThinking) put("thinkingConfig", JSONObject().put("thinkingLevel", "LOW"))
                },
            )
            put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
            if (!isLegacy20) {
                put("inputAudioTranscription", JSONObject())
                put("outputAudioTranscription", JSONObject())
                if (apiVersion == "v1alpha" || cleanModel.startsWith("gemini-2.5") || cleanModel.startsWith("gemini-3.")) {
                    put("contextWindowCompression", JSONObject().put("slidingWindow", JSONObject()))
                    put(
                        "sessionResumption",
                        JSONObject().apply {
                            if (!handle.isNullOrBlank()) put("handle", handle)
                        },
                    )
                }
            }
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
        msg.optJSONObject("error")?.let { errObj ->
            val errTxt = errObj.optString("message").ifBlank { errObj.toString().take(160) }
            lastServerError = errTxt
            SafeLogger.w(TAG, "Server error from Live WebSocket: $errTxt")
            runCatching { ws.close(1000, "Model error fallback") }
            return
        }
        if (msg.has("setupComplete")) {
            setupTimeoutJob?.cancel()
            setupComplete = true
            lastServerError = null
            _orbState.value = OrbState.LISTENING
            _statusText.value = "Listening (Noise Cancel ON)..."
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
    private fun createLiveAudioRecordWithFallback(inRate: Int, minRec: Int): AudioRecord? {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
        )
        for (src in sources) {
            val ar = runCatching {
                AudioRecord.Builder()
                    .setAudioSource(src)
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
            if (ar != null && ar.state == AudioRecord.STATE_INITIALIZED) return ar
            runCatching { ar?.release() }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun configureCommunicationAudioRouting() {
        val am = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            if (previousAudioMode == null) previousAudioMode = am.mode
            if (previousSpeakerphone == null) previousSpeakerphone = am.isSpeakerphoneOn
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            val hasExternalDevice = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { dev ->
                dev.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    dev.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    dev.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    (Build.VERSION.SDK_INT >= 31 && dev.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            }
            if (!hasExternalDevice) {
                if (Build.VERSION.SDK_INT >= 31) {
                    val speaker = am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                    if (speaker != null) am.setCommunicationDevice(speaker)
                }
                am.isSpeakerphoneOn = true
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun restoreAudioRouting() {
        val am = appContext?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice()
            previousSpeakerphone?.let { am.isSpeakerphoneOn = it }
            previousAudioMode?.let { am.mode = it }
        }
        previousSpeakerphone = null
        previousAudioMode = null
    }

    @SuppressLint("MissingPermission")
    private fun startAudioLoops() {
        cleanupAudio()
        configureCommunicationAudioRouting()
        val inRate = 16000
        val minRec = AudioRecord.getMinBufferSize(inRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minRec <= 0) {
            restoreAudioRouting()
            _orbState.value = OrbState.ERROR
            callbacks.onError("AudioRecord hardware unavailable.")
            return
        }
        val recorder = createLiveAudioRecordWithFallback(inRate, minRec)
        if (recorder == null) {
            restoreAudioRouting()
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
        if (AutomaticGainControl.isAvailable()) {
            agc = runCatching { AutomaticGainControl.create(recorder.audioSessionId)?.apply { enabled = true } }.getOrNull()
        }
        hpPrevIn = 0f
        hpPrevOut = 0f
        adaptiveNoiseFloor = 0.012f
        voiceGateHangover = 0
        playbackActive = false

        val outRate = 24000
        val minPlay = AudioTrack.getMinBufferSize(outRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(9600)
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
                .setBufferSizeInBytes(minPlay * 3)
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
                val filteredLevel = applyHighPassAndAdaptiveNoiseGate(buf, read)
                if (!playbackActive) {
                    _audioLevel.value = filteredLevel
                }
                val b64 = Base64.encodeToString(buf, 0, read, Base64.NO_WRAP)
                val msg = JSONObject().put(
                    "realtimeInput",
                    JSONObject().put("audio", JSONObject().put("data", b64).put("mimeType", "audio/pcm;rate=16000")),
                )
                runCatching { webSocket?.send(msg.toString()) }
            }
        }

        playJob = scope.launch {
            var startedPlaying = false
            while (isActive && webSocket != null) {
                if (!startedPlaying && playbackQueue.size < 2) {
                    delay(35L)
                    if (playbackQueue.isEmpty()) continue
                }
                val chunk = playbackQueue.poll(180, TimeUnit.MILLISECONDS)
                if (chunk == null) {
                    startedPlaying = false
                    playbackActive = false
                    if (_orbState.value == OrbState.SPEAKING) {
                        _orbState.value = OrbState.LISTENING
                        _statusText.value = "Listening (Noise Cancel ON)..."
                    }
                    continue
                }
                if (!startedPlaying && chunk.size >= 4) {
                    applySoftOnsetRamp(chunk)
                }
                startedPlaying = true
                playbackActive = true
                _audioLevel.value = computeRms(chunk, chunk.size)
                runCatching { player.write(chunk, 0, chunk.size) }
            }
            playbackActive = false
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
        lastServerError = null
        setupTimeoutJob?.cancel()
        setupTimeoutJob = null
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
        playbackActive = false
        recordJob?.cancel()
        playJob?.cancel()
        recordJob = null
        playJob = null
        playbackQueue.clear()
        runCatching { aec?.release() }
        runCatching { ns?.release() }
        runCatching { agc?.release() }
        aec = null
        ns = null
        agc = null
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
        restoreAudioRouting()
    }

    /**
     * Software dual-stage DSP noise suppression:
     * 1) 90Hz high-pass filter removes fan/wind/AC rumble & DC offset.
     * 2) Adaptive noise-floor soft gate cleanly suppresses ambient hum without chopping voice.
     */
    private fun applyHighPassAndAdaptiveNoiseGate(pcm: ByteArray, length: Int): Float {
        if (length < 2) return 0f
        var prevX = hpPrevIn
        var prevY = hpPrevOut
        var sumSq = 0.0
        var count = 0
        var i = 0
        while (i + 1 < length) {
            val raw = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort().toFloat()
            val hp = 0.96f * (prevY + raw - prevX)
            prevX = raw
            prevY = hp
            val clamped = hp.toInt().coerceIn(-32768, 32767)
            pcm[i] = (clamped and 0xFF).toByte()
            pcm[i + 1] = ((clamped shr 8) and 0xFF).toByte()
            val norm = clamped / 32768.0
            sumSq += norm * norm
            count++
            i += 2
        }
        hpPrevIn = prevX
        hpPrevOut = prevY
        if (count == 0) return 0f

        val rms = sqrt(sumSq / count).toFloat()
        if (rms < adaptiveNoiseFloor * 1.4f) {
            adaptiveNoiseFloor = (adaptiveNoiseFloor * 0.92f + rms * 0.08f).coerceIn(0.004f, 0.045f)
        }
        val baseThreshold = (adaptiveNoiseFloor * 2.1f).coerceIn(0.012f, 0.075f)
        val effectiveThreshold = if (playbackActive) baseThreshold * 2.4f else baseThreshold

        if (rms >= effectiveThreshold) {
            voiceGateHangover = 7 // ~280ms tail so word endings never chop cleanly
            return (rms * 4.5f).coerceIn(0f, 1f)
        }
        if (voiceGateHangover > 0) {
            voiceGateHangover--
            return (rms * 4.5f).coerceIn(0f, 1f)
        }
        // Soft-knee attenuation on steady ambient background noise
        val gain = 0.06f
        var j = 0
        while (j + 1 < length) {
            val sample = ((pcm[j + 1].toInt() shl 8) or (pcm[j].toInt() and 0xFF)).toShort().toFloat()
            val damped = (sample * gain).toInt()
            pcm[j] = (damped and 0xFF).toByte()
            pcm[j + 1] = ((damped shr 8) and 0xFF).toByte()
            j += 2
        }
        return 0f
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun applySoftOnsetRamp(pcm16Le: ByteArray, fadeSamples: Int = 180) {
        val count = fadeSamples.coerceAtMost(pcm16Le.size / 2)
        if (count <= 1) return
        for (i in 0 until count) {
            val idx = i * 2
            val lo = pcm16Le[idx].toInt() and 0xFF
            val hi = pcm16Le[idx + 1].toInt()
            val sample = ((hi shl 8) or lo).toShort().toInt()
            val scaled = (sample * i) / count
            pcm16Le[idx] = (scaled and 0xFF).toByte()
            pcm16Le[idx + 1] = ((scaled shr 8) and 0xFF).toByte()
        }
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

    companion object {
        private const val TAG = "AyshaGeminiLive"
    }
}
