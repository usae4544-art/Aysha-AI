package com.aysha.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.SafeLogger
import com.aysha.assistant.core.VoicePersonalities
import com.aysha.assistant.core.VoicePersonality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class TtsVoiceInfo(
    val name: String,
    val localeTag: String,
    val isOffline: Boolean,
    val quality: Int,
)

object SpeechTextFormatter {
    fun cleanForSpeech(raw: String): String {
        return raw
            .replace(Regex("[*_`#>]"), " ")
            .replace(Regex("https?://\\S+"), "link")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun splitChunks(text: String, maxChars: Int = 1200): List<String> {
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

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<TtsVoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = _availableVoices.asStateFlow()

    private val _ttsAvailable = MutableStateFlow(true)
    val ttsAvailable: StateFlow<Boolean> = _ttsAvailable.asStateFlow()

    @Volatile
    private var pendingSpeakDone: (() -> Unit)? = null

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
    )

    init {
        initTts()
    }

    private fun initTts() {
        runCatching {
            tts = TextToSpeech(appContext) { status ->
                ttsReady = status == TextToSpeech.SUCCESS
                _ttsAvailable.value = ttsReady
                if (ttsReady) {
                    refreshInstalledVoices()
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                            _rmsLevel.value = 0.42f
                        }

                        override fun onDone(utteranceId: String?) {
                            val isFinal = utteranceId?.endsWith("_LAST") != false
                            if (isFinal) {
                                bargeInDetector.stop()
                                _isSpeaking.value = false
                                _rmsLevel.value = 0f
                                val cb = pendingSpeakDone
                                pendingSpeakDone = null
                                if (cb != null) mainHandler.post { cb() }
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            bargeInDetector.stop()
                            _isSpeaking.value = false
                            _rmsLevel.value = 0f
                            pendingSpeakDone = null
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            bargeInDetector.stop()
                            _isSpeaking.value = false
                            _rmsLevel.value = 0f
                            pendingSpeakDone = null
                        }
                    })
                }
            }
        }.onFailure { err ->
            SafeLogger.e(TAG, "TTS initialization failed", err)
            _ttsAvailable.value = false
        }
    }

    fun refreshInstalledVoices() {
        val engine = tts ?: return
        runCatching {
            val voices = engine.voices.orEmpty()
                .filter { v ->
                    val lang = v.locale.language.lowercase()
                    lang == "en" || lang == "hi"
                }
                .sortedWith(
                    compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenBy { it.locale.toLanguageTag() }
                        .thenBy { it.name },
                )
                .map { v ->
                    TtsVoiceInfo(
                        name = v.name,
                        localeTag = v.locale.toLanguageTag(),
                        isOffline = !v.isNetworkConnectionRequired,
                        quality = v.quality,
                    )
                }
            _availableVoices.value = voices
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(
        localeTag: String = "en-IN",
        onPartialResult: (String) -> Unit = {},
        onFinalResult: (String) -> Unit,
        onSoftTimeout: (() -> Unit)? = null,
        onError: (String) -> Unit,
    ) {
        mainHandler.post {
            stopSpeaking()
            destroyRecognizerOnMain()

            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                _isListening.value = false
                onError("Device Speech Recognition available nahi hai. Google Speech Services check karo ya text chat use karo.")
                return@post
            }

            val recognizer = runCatching { SpeechRecognizer.createSpeechRecognizer(appContext) }.getOrNull()
            if (recognizer == null) {
                _isListening.value = false
                onError("SpeechRecognizer initialize nahi ho paya.")
                return@post
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
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    _partialText.value = ""
                    destroyRecognizerOnMain()
                    if ((error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) && onSoftTimeout != null) {
                        onSoftTimeout()
                        return
                    }
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio capture error — mic check karo."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required hai."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Speech network error — offline Hindi/English speech pack download karo ya mic retry karo."
                        SpeechRecognizer.ERROR_NO_MATCH -> "Awaz clearly samajh nahi aayi. Dobara boliye."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Kuch sunai nahi diya."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer busy hai, ek second me try karo."
                        else -> "Speech recognition error ($error)"
                    }
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _rmsLevel.value = 0f
                    val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = list?.firstOrNull().orEmpty().trim()
                    _partialText.value = ""
                    destroyRecognizerOnMain()
                    if (best.isNotEmpty()) {
                        onFinalResult(best)
                    } else if (onSoftTimeout != null) {
                        onSoftTimeout()
                    } else {
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
    }

    fun stopListening() {
        mainHandler.post {
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

    fun speak(
        text: String,
        localeTag: String = "en-IN",
        rate: Float = 1.0f,
        pitch: Float = 1.0f,
        volume: Float = 1.0f,
        personalityId: String = "aysha_natural",
        preferredVoiceName: String = "",
        bargeInEnabled: Boolean = false,
        onBargeIn: (() -> Unit)? = null,
        onDone: (() -> Unit)? = null,
    ) {
        val chunks = SpeechTextFormatter.splitChunks(text)
        if (chunks.isEmpty()) {
            onDone?.invoke()
            return
        }
        lastSpokenPayload = SpokenRequest(
            text = text,
            localeTag = localeTag,
            rate = rate,
            pitch = pitch,
            volume = volume,
            personalityId = personalityId,
            preferredVoiceName = preferredVoiceName,
        )

        val engine = tts
        if (!ttsReady || engine == null) {
            onDone?.invoke()
            return
        }

        val personality = VoicePersonalities.byId(personalityId)
        val effectiveRate = (rate * personality.rateMultiplier).coerceIn(0.5f, 2.0f)
        val effectivePitch = (pitch * personality.basePitch).coerceIn(0.5f, 1.8f)
        val safeVolume = volume.coerceIn(0.0f, 1.0f)

        mainHandler.post {
            bargeInDetector.stop()
            pendingSpeakDone = onDone
            val locale = Locale.forLanguageTag(localeTag)
            val langRes = engine.setLanguage(locale)
            if (langRes == TextToSpeech.LANG_MISSING_DATA || langRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.setLanguage(Locale.forLanguageTag("en-IN"))
            }

            selectVoiceIfAvailable(engine, personality, preferredVoiceName, localeTag)
            engine.setSpeechRate(effectiveRate)
            engine.setPitch(effectivePitch)

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, safeVolume)
            }
            val baseId = UUID.randomUUID().toString()
            chunks.forEachIndexed { idx, chunk ->
                val queueMode = if (idx == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val suffix = if (idx == chunks.lastIndex) "_LAST" else "_$idx"
                engine.speak(chunk, queueMode, params, "$baseId$suffix")
            }
            if (bargeInEnabled && onBargeIn != null) {
                bargeInDetector.start {
                    mainHandler.post {
                        stopSpeaking()
                        onBargeIn()
                    }
                }
            }
        }
    }

    fun previewPersonality(
        personality: VoicePersonality,
        language: AssistantLanguage,
        rate: Float,
        pitch: Float,
        volume: Float,
        preferredVoiceName: String = "",
    ) {
        val sampleText = when (language) {
            AssistantLanguage.HINDI -> "नमस्ते! मैं ${personality.name.substringBefore(" ")} हूँ, आपकी पर्सनल AI असिस्टेंट।"
            AssistantLanguage.HINGLISH -> "Hello! Main ${personality.name.substringBefore(" ")} hoon, aapki personal AI companion. Batao aaj kya help karoon?"
            AssistantLanguage.ENGLISH -> "Hello! I am ${personality.name.substringBefore(" ")}, your personal AI assistant. How can I help you today?"
        }
        speak(
            text = sampleText,
            localeTag = language.ttsLocaleTag,
            rate = rate,
            pitch = pitch,
            volume = volume,
            personalityId = personality.id,
            preferredVoiceName = preferredVoiceName,
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
        )
        return true
    }

    fun canReplay(): Boolean = lastSpokenPayload != null

    fun stopSpeaking() {
        bargeInDetector.stop()
        pendingSpeakDone = null
        mainHandler.post {
            _isSpeaking.value = false
            _rmsLevel.value = 0f
            runCatching { tts?.stop() }
        }
    }

    private fun selectVoiceIfAvailable(
        engine: TextToSpeech,
        personality: VoicePersonality,
        preferredVoiceName: String,
        localeTag: String,
    ) {
        runCatching {
            val all = engine.voices ?: return
            if (preferredVoiceName.isNotBlank()) {
                val explicit = all.firstOrNull { it.name == preferredVoiceName }
                if (explicit != null) {
                    engine.voice = explicit
                    return
                }
            }
            val targetLang = localeTag.substringBefore("-").lowercase()
            val candidate = all
                .filter { it.locale.language.equals(targetLang, ignoreCase = true) }
                .sortedWith(
                    compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                        .thenByDescending { v ->
                            val lower = v.name.lowercase()
                            val wantsFemale = personality.gender.name == "FEMALE"
                            when {
                                wantsFemale && (lower.contains("female") || lower.contains("-f-") || lower.contains("sfg") || lower.contains("iob")) -> 2
                                !wantsFemale && (lower.contains("male") || lower.contains("-m-") || lower.contains("iom") || lower.contains("gba")) -> 2
                                else -> 0
                            }
                        }
                        .thenByDescending { it.quality },
                )
                .firstOrNull()
            if (candidate != null) {
                engine.voice = candidate
            }
        }
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        mainHandler.post {
            runCatching { tts?.shutdown() }
            tts = null
            ttsReady = false
        }
    }

    companion object {
        private const val TAG = "AyshaVoiceController"
    }
}
