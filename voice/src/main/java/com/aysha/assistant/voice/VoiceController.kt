package com.aysha.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.SafeLogger
import com.aysha.assistant.core.VoicePersonalities
import com.aysha.assistant.core.VoicePersonality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TtsVoiceInfo(
    val name: String,
    val displayName: String,
    val genderLabel: String,
    val toneHint: String,
)

object SpeechTextFormatter {
    fun cleanForSpeech(raw: String): String {
        return raw
            .replace(Regex("[*_`#>]"), " ")
            .replace(Regex("https?://\\S+"), "link")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun splitChunks(text: String, maxChars: Int = 900): List<String> {
        val clean = cleanForSpeech(text)
        if (clean.length <= maxChars) return if (clean.isEmpty()) emptyList() else listOf(clean)
        val out = mutableListOf<String>()
        var idx = 0
        while (idx < clean.length) {
            val end = (idx + maxChars).coerceAtMost(clean.length)
            if (end == clean.length) {
                out.add(clean.substring(idx).trim())
                break
            }
            val splitAt = clean.lastIndexOf('.', end)
                .coerceAtLeast(clean.lastIndexOf('!', end))
                .coerceAtLeast(clean.lastIndexOf('?', end))
                .coerceAtLeast(clean.lastIndexOf(' ', end))
            val cut = if (splitAt > idx + 120) splitAt + 1 else end
            out.add(clean.substring(idx, cut).trim())
            idx = cut
        }
        return out.filter { it.isNotBlank() }
    }
}

class VoiceController(context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bargeInDetector = BargeInDetector(appContext)
    private val neuralTts = GeminiNeuralTtsEngine()

    private var speechRecognizer: SpeechRecognizer? = null
    private var pendingStartRunnable: Runnable? = null

    @Volatile
    private var geminiApiKey: String = ""

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(
        VoicePersonalities.all.map {
            TtsVoiceInfo(
                name = it.geminiLiveVoice,
                displayName = it.name,
                genderLabel = it.gender.label,
                toneHint = it.subtitle,
            )
        },
    )
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _ttsAvailable = MutableStateFlow(true)
    val ttsAvailable: StateFlow<Boolean> = _ttsAvailable.asStateFlow()

    @Volatile
    private var lastSpokenPayload: SpokenRequest? = null

    private data class SpokenRequest(
        val text: String,
        val localeTag: String,
        val rate: Float,
        val pitch: Float,
        val volume: Float,
        val personalityId: String,
        val preferredVoiceName: String,
        val apiKey: String,
    )

    fun setGeminiApiKey(key: String) {
        val clean = GeminiTtsProtocol.sanitizeKey(key)
        geminiApiKey = clean
        _ttsAvailable.value = clean.isNotEmpty()
    }

    fun isSpeechRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(
        localeTag: String = "en-IN",
        keepContinuousStateOnSoftTimeout: Boolean = false,
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit,
        onSoftTimeout: (() -> Unit)? = null,
        onError: (String) -> Unit,
    ) {
        mainHandler.post {
            pendingStartRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingStartRunnable = null

            val hadActiveRecognizer = speechRecognizer != null
            stopSpeaking()
            destroyRecognizerOnMain()

            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                _isListening.value = false
                onError("Device Speech Recognition available nahi hai. Gemini Live Orb tap karo ya text chat use karo.")
                return@post
            }

            val startAction = Runnable {
                pendingStartRunnable = null
                val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(appContext) }.getOrNull()
                if (recognizer == null) {
                    _isListening.value = false
                    onError("SpeechRecognizer initialize nahi ho paya.")
                    return@Runnable
                }
                speechRecognizer = recognizer

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                        _partialText.value = ""
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _rmsLevel.value = 0f
                    }

                    override fun onError(error: Int) {
                        _rmsLevel.value = 0f
                        _partialText.value = ""
                        destroyRecognizerOnMain()
                        val isSoftTimeout = error == SpeechRecognizer.ERROR_NO_MATCH ||
                            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        if (isSoftTimeout && onSoftTimeout != null) {
                            if (!keepContinuousStateOnSoftTimeout) {
                                _isListening.value = false
                            }
                            onSoftTimeout()
                            return
                        }
                        _isListening.value = false
                        val msg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio capture error — mic check karo."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required hai."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Speech network error — Gemini Live mode use karo ya mic retry karo."
                            SpeechRecognizer.ERROR_NO_MATCH -> "Awaz clearly samajh nahi aayi. Dobara boliye."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Kuch sunai nahi diya."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy hai, ek second me try karo."
                            else -> "Speech recognition error ($error)"
                        }
                        onError(msg)
                    }

                    override fun onResults(results: Bundle?) {
                        _rmsLevel.value = 0f
                        val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val best = list?.firstOrNull().orEmpty().trim()
                        _partialText.value = ""
                        destroyRecognizerOnMain()
                        if (best.isNotEmpty()) {
                            _isListening.value = false
                            onFinalResult(best)
                        } else if (onSoftTimeout != null) {
                            if (!keepContinuousStateOnSoftTimeout) _isListening.value = false
                            onSoftTimeout()
                        } else {
                            _isListening.value = false
                            onError("Kuch text recognize nahi hua.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull().orEmpty()
                        if (partial.isNotBlank()) {
                            _partialText.value = partial
                            onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                }
                runCatching {
                    _isListening.value = true
                    recognizer.startListening(intent)
                }.onFailure { err ->
                    _isListening.value = false
                    destroyRecognizerOnMain()
                    onError("Voice listening start nahi ho paya: ${err.message}")
                }
            }

            if (hadActiveRecognizer) {
                pendingStartRunnable = startAction
                mainHandler.postDelayed(startAction, 450L)
            } else {
                startAction.run()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            pendingStartRunnable?.let { mainHandler.removeCallbacks(it) }
            pendingStartRunnable = null
            _isListening.value = false
            _rmsLevel.value = 0f
            _partialText.value = ""
            destroyRecognizerOnMain()
        }
    }

    private fun destroyRecognizerOnMain() {
        runCatching { speechRecognizer?.cancel() }
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    /**
     * Speaks text strictly using Gemini Real Neural TTS (no Android Google TextToSpeech anywhere).
     */
    fun speak(
        text: String,
        localeTag: String = "en-IN",
        rate: Float = 1.0f,
        pitch: Float = 1.0f,
        volume: Float = 1.0f,
        personalityId: String = "aysha_natural",
        preferredVoiceName: String = "",
        apiKey: String = geminiApiKey,
        bargeInEnabled: Boolean = false,
        onBargeIn: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null,
        onDone: (() -> Unit)? = null,
    ) {
        val chunks = SpeechTextFormatter.splitChunks(text)
        if (chunks.isEmpty()) {
            onDone?.invoke()
            return
        }
        val activeKey = GeminiTtsProtocol.sanitizeKey(apiKey.ifBlank { geminiApiKey })
        lastSpokenPayload = SpokenRequest(
            text = text,
            localeTag = localeTag,
            rate = rate,
            pitch = pitch,
            volume = volume,
            personalityId = personalityId,
            preferredVoiceName = preferredVoiceName,
            apiKey = activeKey,
        )

        if (activeKey.isEmpty()) {
            val msg = "Gemini API Key missing hai — real neural voice sunne ke liye AI Provider me key daalo."
            SafeLogger.w(TAG, msg)
            onError?.invoke(msg)
            onDone?.invoke()
            return
        }

        val personality = VoicePersonalities.byId(personalityId)
        val effectiveVoice = preferredVoiceName.trim().ifBlank { personality.geminiLiveVoice }
        val effectiveRate = (rate * personality.rateMultiplier).coerceIn(0.6f, 1.8f)
        val styleDirective = GeminiTtsProtocol.buildStyleDirective(personality.toneHint, effectiveRate)

        bargeInDetector.stop()
        _isSpeaking.value = true

        neuralTts.speak(
            apiKey = activeKey,
            chunks = chunks,
            voiceName = effectiveVoice,
            styleHint = styleDirective,
            rate = effectiveRate,
            volume = volume,
            onStarted = {
                mainHandler.post {
                    _isSpeaking.value = true
                    if (bargeInEnabled && onBargeIn != null) {
                        bargeInDetector.start {
                            mainHandler.post {
                                stopSpeaking()
                                onBargeIn()
                            }
                        }
                    }
                }
            },
            onRms = { level ->
                _rmsLevel.value = level
            },
            onFinished = {
                bargeInDetector.stop()
                _isSpeaking.value = false
                _rmsLevel.value = 0f
                if (onDone != null) mainHandler.post { onDone() }
            },
            onError = { err ->
                bargeInDetector.stop()
                _isSpeaking.value = false
                _rmsLevel.value = 0f
                if (onError != null) mainHandler.post { onError(err) }
                if (onDone != null) mainHandler.post { onDone() }
            },
        )
    }

    fun previewPersonality(
        personality: VoicePersonality,
        language: AssistantLanguage,
        rate: Float,
        pitch: Float,
        volume: Float,
        preferredVoiceName: String = "",
        apiKey: String = geminiApiKey,
        onError: ((String) -> Unit)? = null,
    ) {
        val sampleText = when (language) {
            AssistantLanguage.HINDI -> "नमस्ते! मैं ${personality.name.substringBefore(" ")} हूँ, आपकी रियल एआई असिस्टेंट।"
            AssistantLanguage.HINGLISH -> "Hello! Main ${personality.name.substringBefore(" ")} hoon, aapki real neural AI companion. Batao aaj kya help karoon?"
            AssistantLanguage.ENGLISH -> "Hello! I am ${personality.name.substringBefore(" ")}, your real AI assistant. How can I help you today?"
        }
        speak(
            text = sampleText,
            localeTag = language.ttsLocaleTag,
            rate = rate,
            pitch = pitch,
            volume = volume,
            personalityId = personality.id,
            preferredVoiceName = preferredVoiceName.ifBlank { personality.geminiLiveVoice },
            apiKey = apiKey,
            onError = onError,
        )
    }

    fun replayLast(): Boolean {
        val req = lastSpokenPayload ?: return false
        speak(
            text = req.text,
            localeTag = req.localeTag,
            rate = req.rate,
            pitch = req.pitch,
            volume = req.volume,
            personalityId = req.personalityId,
            preferredVoiceName = req.preferredVoiceName,
            apiKey = req.apiKey.ifBlank { geminiApiKey },
        )
        return true
    }

    fun canReplay(): Boolean = lastSpokenPayload != null

    fun stopSpeaking() {
        bargeInDetector.stop()
        neuralTts.stop()
        _isSpeaking.value = false
        _rmsLevel.value = 0f
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
    }

    companion object {
        private const val TAG = "AyshaVoiceController"
    }
}
