package com.aysha.assistant.wakeword

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import com.aysha.assistant.core.AwarenessDecisionLog
import com.aysha.assistant.core.AwarenessSensitivity
import com.aysha.assistant.core.SafeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import kotlin.math.sqrt

enum class WakeModelState {
    IDLE,
    PREPARING,
    READY,
    ERROR,
}

/**
 * Battery-efficient on-device wake-word detector with Maya-style Awareness Gate:
 * 1) Adjustable RMS VAD sensitivity gate (Reserved / Balanced / Alert).
 * 2) Optional Mute-During-Media guard to avoid false wakes from TV or music.
 * 3) Local Vosk speech recognizer processes active voice windows offline.
 * 4) Logs wake decisions locally into WakeWordStatusBus for user inspection.
 */
class WakeWordEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private val modelManager = VoskModelManager(appContext)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _modelState = MutableStateFlow(WakeModelState.IDLE)
    val modelState: StateFlow<WakeModelState> = _modelState.asStateFlow()

    private val _statusText = MutableStateFlow("Wake word off")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _micEnergy = MutableStateFlow(0f)
    val micEnergy: StateFlow<Float> = _micEnergy.asStateFlow()

    private val _wakeEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val wakeEvents: SharedFlow<String> = _wakeEvents.asSharedFlow()

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var ns: NoiseSuppressor? = null
    private var aec: AcousticEchoCanceler? = null
    private var listenJob: Job? = null

    @Volatile
    private var configuredCsv = "Hey Aysha,Hello Aysha,Wake up Aysha"

    @Volatile
    private var assistantName = "AYSHA"

    @Volatile
    private var awarenessEnabled = true

    @Volatile
    private var sensitivity = AwarenessSensitivity.BALANCED

    @Volatile
    private var muteDuringMedia = false

    @Volatile
    private var pausedForActiveSpeech = false

    @Volatile
    private var lastTriggerMs = 0L

    @Volatile
    private var lastLoggedTranscript = ""

    fun updateConfig(
        wakeWordsCsv: String,
        name: String,
        awareness: Boolean = awarenessEnabled,
        sens: AwarenessSensitivity = sensitivity,
        muteMedia: Boolean = muteDuringMedia,
    ) {
        configuredCsv = wakeWordsCsv
        assistantName = name
        awarenessEnabled = awareness
        sensitivity = sens
        muteDuringMedia = muteMedia
    }

    fun start(
        wakeWordsCsv: String,
        assistantName: String,
        awareness: Boolean = true,
        sens: AwarenessSensitivity = AwarenessSensitivity.BALANCED,
        muteMedia: Boolean = false,
    ) {
        updateConfig(wakeWordsCsv, assistantName, awareness, sens, muteMedia)
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _modelState.value = WakeModelState.ERROR
            _statusText.value = "Microphone permission needed for wake word"
            return
        }
        if (listenJob?.isActive == true) return

        pausedForActiveSpeech = false
        listenJob = scope.launch {
            _modelState.value = WakeModelState.PREPARING
            _statusText.value = "Preparing on-device wake model..."
            WakeWordStatusBus.publish(state = WakeModelState.PREPARING, status = _statusText.value)
            val pathRes = modelManager.ensureModelPrepared()
            if (pathRes.isFailure) {
                _modelState.value = WakeModelState.ERROR
                _statusText.value = pathRes.exceptionOrNull()?.message ?: "Wake model prepare failed"
                _isRunning.value = false
                WakeWordStatusBus.publish(running = false, state = WakeModelState.ERROR, status = _statusText.value)
                return@launch
            }

            val path = pathRes.getOrThrow()
            val loaded = runCatching {
                LibVosk.setLogLevel(LogLevel.WARNINGS)
                Model(path)
            }.onFailure { err ->
                SafeLogger.e(TAG, "Failed loading Vosk model", err)
            }.getOrNull()

            if (loaded == null) {
                _modelState.value = WakeModelState.ERROR
                _statusText.value = "Offline wake model failed to load"
                _isRunning.value = false
                WakeWordStatusBus.publish(running = false, state = WakeModelState.ERROR, status = _statusText.value)
                return@launch
            }

            model = loaded
            recognizer = runCatching { Recognizer(loaded, 16000f) }.getOrNull()
            if (recognizer == null) {
                _modelState.value = WakeModelState.ERROR
                _statusText.value = "Wake recognizer init failed"
                WakeWordStatusBus.publish(running = false, state = WakeModelState.ERROR, status = _statusText.value)
                cleanupAudio()
                return@launch
            }

            _modelState.value = WakeModelState.READY
            _isRunning.value = true
            _statusText.value = "Listening for Hey Aysha (${sensitivity.label})"
            WakeWordStatusBus.publish(running = true, state = WakeModelState.READY, status = _statusText.value)
            runCaptureLoop()
        }
    }

    fun pauseForActiveSession() {
        pausedForActiveSpeech = true
        cleanupAudio()
        _micEnergy.value = 0f
        if (_isRunning.value) {
            _statusText.value = "Paused while speaking/listening"
        }
    }

    fun resumeAfterActiveSession() {
        if (!_isRunning.value) return
        pausedForActiveSpeech = false
        if (listenJob?.isActive != true && model != null) {
            listenJob = scope.launch {
                _statusText.value = "Listening for Hey Aysha (${sensitivity.label})"
                runCaptureLoop()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun runCaptureLoop() {
        val sampleRate = 16000
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) {
            _modelState.value = WakeModelState.ERROR
            _statusText.value = "AudioRecord unavailable"
            _isRunning.value = false
            return
        }

        val recorder = runCatching {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minBuf.coerceAtLeast(6400) * 2)
                .build()
        }.getOrNull()

        if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
            runCatching { recorder?.release() }
            _statusText.value = "Microphone busy"
            _isRunning.value = false
            return
        }

        audioRecord = recorder
        if (NoiseSuppressor.isAvailable()) {
            ns = runCatching { NoiseSuppressor.create(recorder.audioSessionId)?.apply { enabled = true } }.getOrNull()
        }
        if (AcousticEchoCanceler.isAvailable()) {
            aec = runCatching { AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true } }.getOrNull()
        }

        val started = runCatching { recorder.startRecording() }.isSuccess
        if (!started) {
            cleanupAudio()
            _statusText.value = "Could not start wake mic"
            _isRunning.value = false
            return
        }

        val buf = ByteArray(3200) // 100ms @ 16kHz PCM16
        var activeHangoverFrames = 0

        while (scope.isActive && _isRunning.value && !pausedForActiveSpeech) {
            val read = runCatching { recorder.read(buf, 0, buf.size) }.getOrDefault(-1)
            if (read <= 0) {
                delay(25L)
                continue
            }

            if (muteDuringMedia && audioManager.isMusicActive) {
                _micEnergy.value = 0f
                WakeWordStatusBus.publish(energy = 0f, status = "Muted while music/TV playing")
                delay(250L)
                continue
            }

            val energy = computeNormalizedRms(buf, read)
            _micEnergy.value = energy
            WakeWordStatusBus.publish(energy = energy)

            val threshold = if (awarenessEnabled) sensitivity.rmsThreshold else 0.035f
            if (energy > threshold) {
                activeHangoverFrames = 12
            } else if (activeHangoverFrames > 0) {
                activeHangoverFrames--
            }

            if (activeHangoverFrames <= 0) {
                continue
            }

            val rec = recognizer ?: continue
            val isFinal = rec.acceptWaveForm(buf, read)
            val transcript = if (isFinal) {
                runCatching { JSONObject(rec.result).optString("text") }.getOrDefault("")
            } else {
                runCatching { JSONObject(rec.partialResult).optString("partial") }.getOrDefault("")
            }

            if (transcript.isNotBlank()) {
                val matched = WakePhraseMatcher.matchTranscript(
                    transcript = transcript,
                    configuredCsv = configuredCsv,
                    assistantName = assistantName,
                )
                val now = System.currentTimeMillis()
                if (matched != null && now - lastTriggerMs > 2800L) {
                    lastTriggerMs = now
                    rec.reset()
                    activeHangoverFrames = 0
                    SafeLogger.d(TAG, "Wake word detected: $matched")
                    WakeWordStatusBus.logDecision(
                        AwarenessDecisionLog(
                            heardText = transcript,
                            energy = energy,
                            threshold = threshold,
                            forwarded = true,
                            reason = "Matched '$matched'",
                        ),
                    )
                    _wakeEvents.tryEmit(matched)
                } else if (isFinal && transcript != lastLoggedTranscript) {
                    lastLoggedTranscript = transcript
                    WakeWordStatusBus.logDecision(
                        AwarenessDecisionLog(
                            heardText = transcript,
                            energy = energy,
                            threshold = threshold,
                            forwarded = false,
                            reason = "Chup rahi (no wake phrase)",
                        ),
                    )
                }
            }
        }
        cleanupAudio()
    }

    fun stop() {
        _isRunning.value = false
        pausedForActiveSpeech = false
        listenJob?.cancel()
        listenJob = null
        cleanupAudio()
        runCatching { recognizer?.close() }
        runCatching { model?.close() }
        recognizer = null
        model = null
        _micEnergy.value = 0f
        _modelState.value = WakeModelState.IDLE
        _statusText.value = "Wake word off"
        WakeWordStatusBus.publish(running = false, state = WakeModelState.IDLE, status = "Wake word off", energy = 0f)
    }

    private fun cleanupAudio() {
        runCatching { ns?.release() }
        runCatching { aec?.release() }
        ns = null
        aec = null
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    fun destroy() {
        stop()
        scope.cancel()
    }

    private fun computeNormalizedRms(pcm: ByteArray, length: Int): Float {
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
        return (sqrt(sumSq / count).toFloat() * 4.0f).coerceIn(0f, 1f)
    }

    companion object {
        private const val TAG = "AyshaWakeWord"
    }
}
