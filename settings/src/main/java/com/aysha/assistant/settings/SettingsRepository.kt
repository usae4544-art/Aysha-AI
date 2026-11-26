package com.aysha.assistant.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.PersonalityStyle
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.VoiceEngineMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.ayshaSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "aysha_preferences")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val assistantName = stringPreferencesKey("assistant_name")
        val userName = stringPreferencesKey("user_name")
        val language = stringPreferencesKey("language")
        val activeProvider = stringPreferencesKey("active_provider")
        val fallbackEnabled = booleanPreferencesKey("fallback_enabled")
        val offlineOnlyMode = booleanPreferencesKey("offline_only_mode")
        val voiceEngineMode = stringPreferencesKey("voice_engine_mode")
        val voicePersonalityId = stringPreferencesKey("voice_personality_id")
        val selectedTtsVoiceName = stringPreferencesKey("selected_tts_voice_name")
        val ttsSpeechRate = floatPreferencesKey("tts_speech_rate")
        val ttsPitch = floatPreferencesKey("tts_pitch")
        val ttsVolume = floatPreferencesKey("tts_volume")
        val autoSpeakReplies = booleanPreferencesKey("auto_speak_replies")
        val continuousConversation = booleanPreferencesKey("continuous_conversation")
        val wakeWordEnabled = booleanPreferencesKey("wake_word_enabled")
        val wakeWordsCsv = stringPreferencesKey("wake_words_csv")
        val floatingOrbEnabled = booleanPreferencesKey("floating_orb_enabled")
        val themeMode = stringPreferencesKey("theme_mode")
        val accentPalette = stringPreferencesKey("accent_palette")
        val glassTransparency = floatPreferencesKey("glass_transparency")
        val personalityStyle = stringPreferencesKey("personality_style")
        val geminiTextModel = stringPreferencesKey("gemini_text_model")
        val geminiLiveModel = stringPreferencesKey("gemini_live_model")
        val groqModel = stringPreferencesKey("groq_model")
        val openAiBaseUrl = stringPreferencesKey("openai_base_url")
        val openAiModel = stringPreferencesKey("openai_model")
        val localBaseUrl = stringPreferencesKey("local_base_url")
        val localModel = stringPreferencesKey("local_model")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val soundFeedbackEnabled = booleanPreferencesKey("sound_feedback_enabled")
        val notificationSummaryEnabled = booleanPreferencesKey("notification_summary_enabled")

        // AES-256-GCM Keystore encrypted blobs
        val encGeminiKey = stringPreferencesKey("enc_gemini_key")
        val encGroqKey = stringPreferencesKey("enc_groq_key")
        val encOpenAiKey = stringPreferencesKey("enc_openai_key")
        val encLocalKey = stringPreferencesKey("enc_local_key")
    }

    private val _discoveredModels = MutableStateFlow<List<String>>(emptyList())
    val discoveredModels: StateFlow<List<String>> = _discoveredModels.asStateFlow()

    fun setDiscoveredModels(models: List<String>) {
        _discoveredModels.value = models
    }

    val settingsFlow: Flow<AppSettings> = context.ayshaSettingsDataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }
        .map { p ->
            AppSettings(
                assistantName = p[Keys.assistantName]?.ifBlank { "AYSHA" } ?: "AYSHA",
                userName = p[Keys.userName]?.ifBlank { "Friend" } ?: "Friend",
                language = enumOrDefault(p[Keys.language], AssistantLanguage.HINGLISH),
                activeProvider = enumOrDefault(p[Keys.activeProvider], BrainProvider.GEMINI),
                fallbackEnabled = p[Keys.fallbackEnabled] ?: true,
                offlineOnlyMode = p[Keys.offlineOnlyMode] ?: false,
                voiceEngineMode = enumOrDefault(p[Keys.voiceEngineMode], VoiceEngineMode.STANDARD_VOICE),
                voicePersonalityId = p[Keys.voicePersonalityId]?.ifBlank { "aysha_natural" } ?: "aysha_natural",
                selectedTtsVoiceName = p[Keys.selectedTtsVoiceName] ?: "",
                ttsSpeechRate = (p[Keys.ttsSpeechRate] ?: 1.0f).coerceIn(0.5f, 2.0f),
                ttsPitch = (p[Keys.ttsPitch] ?: 1.0f).coerceIn(0.6f, 1.5f),
                ttsVolume = (p[Keys.ttsVolume] ?: 1.0f).coerceIn(0.0f, 1.0f),
                autoSpeakReplies = p[Keys.autoSpeakReplies] ?: true,
                continuousConversation = p[Keys.continuousConversation] ?: false,
                wakeWordEnabled = p[Keys.wakeWordEnabled] ?: false,
                wakeWordsCsv = p[Keys.wakeWordsCsv]?.ifBlank { "Hey Aysha,Hello Aysha,Wake up Aysha" }
                    ?: "Hey Aysha,Hello Aysha,Wake up Aysha",
                floatingOrbEnabled = p[Keys.floatingOrbEnabled] ?: false,
                themeMode = enumOrDefault(p[Keys.themeMode], ThemeMode.DARK),
                accentPalette = enumOrDefault(p[Keys.accentPalette], AccentPalette.COSMIC_CYAN),
                glassTransparency = (p[Keys.glassTransparency] ?: 0.72f).coerceIn(0.35f, 0.95f),
                personalityStyle = enumOrDefault(p[Keys.personalityStyle], PersonalityStyle.FRIENDLY),
                geminiTextModel = p[Keys.geminiTextModel]?.ifBlank { "gemini-3.8-flash" } ?: "gemini-3.8-flash",
                geminiLiveModel = p[Keys.geminiLiveModel]?.ifBlank { "gemini-3.8-live" } ?: "gemini-3.8-live",
                groqModel = p[Keys.groqModel]?.ifBlank { "llama-3.3-70b-versatile" } ?: "llama-3.3-70b-versatile",
                openAiBaseUrl = p[Keys.openAiBaseUrl]?.ifBlank { "https://api.openai.com/v1" } ?: "https://api.openai.com/v1",
                openAiModel = p[Keys.openAiModel]?.ifBlank { "gpt-4o-mini" } ?: "gpt-4o-mini",
                localBaseUrl = p[Keys.localBaseUrl]?.ifBlank { "http://127.0.0.1:11434/v1" } ?: "http://127.0.0.1:11434/v1",
                localModel = p[Keys.localModel]?.ifBlank { "llama3.2" } ?: "llama3.2",
                onboardingCompleted = p[Keys.onboardingCompleted] ?: false,
                soundFeedbackEnabled = p[Keys.soundFeedbackEnabled] ?: true,
                notificationSummaryEnabled = p[Keys.notificationSummaryEnabled] ?: true,
                geminiApiKey = KeystoreCrypto.decrypt(p[Keys.encGeminiKey].orEmpty()),
                groqApiKey = KeystoreCrypto.decrypt(p[Keys.encGroqKey].orEmpty()),
                openAiApiKey = KeystoreCrypto.decrypt(p[Keys.encOpenAiKey].orEmpty()),
                localApiKey = KeystoreCrypto.decrypt(p[Keys.encLocalKey].orEmpty()),
            )
        }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val current = settingsFlow.first()
        val next = transform(current)
        context.ayshaSettingsDataStore.edit { prefs ->
            prefs[Keys.assistantName] = next.assistantName.ifBlank { "AYSHA" }
            prefs[Keys.userName] = next.userName.ifBlank { "Friend" }
            prefs[Keys.language] = next.language.name
            prefs[Keys.activeProvider] = next.activeProvider.name
            prefs[Keys.fallbackEnabled] = next.fallbackEnabled
            prefs[Keys.offlineOnlyMode] = next.offlineOnlyMode
            prefs[Keys.voiceEngineMode] = next.voiceEngineMode.name
            prefs[Keys.voicePersonalityId] = next.voicePersonalityId
            prefs[Keys.selectedTtsVoiceName] = next.selectedTtsVoiceName
            prefs[Keys.ttsSpeechRate] = next.ttsSpeechRate.coerceIn(0.5f, 2.0f)
            prefs[Keys.ttsPitch] = next.ttsPitch.coerceIn(0.6f, 1.5f)
            prefs[Keys.ttsVolume] = next.ttsVolume.coerceIn(0.0f, 1.0f)
            prefs[Keys.autoSpeakReplies] = next.autoSpeakReplies
            prefs[Keys.continuousConversation] = next.continuousConversation
            prefs[Keys.wakeWordEnabled] = next.wakeWordEnabled
            prefs[Keys.wakeWordsCsv] = next.wakeWordsCsv
            prefs[Keys.floatingOrbEnabled] = next.floatingOrbEnabled
            prefs[Keys.themeMode] = next.themeMode.name
            prefs[Keys.accentPalette] = next.accentPalette.name
            prefs[Keys.glassTransparency] = next.glassTransparency.coerceIn(0.35f, 0.95f)
            prefs[Keys.personalityStyle] = next.personalityStyle.name
            prefs[Keys.geminiTextModel] = next.geminiTextModel
            prefs[Keys.geminiLiveModel] = next.geminiLiveModel
            prefs[Keys.groqModel] = next.groqModel
            prefs[Keys.openAiBaseUrl] = next.openAiBaseUrl
            prefs[Keys.openAiModel] = next.openAiModel
            prefs[Keys.localBaseUrl] = next.localBaseUrl
            prefs[Keys.localModel] = next.localModel
            prefs[Keys.onboardingCompleted] = next.onboardingCompleted
            prefs[Keys.soundFeedbackEnabled] = next.soundFeedbackEnabled
            prefs[Keys.notificationSummaryEnabled] = next.notificationSummaryEnabled

            if (next.geminiApiKey != current.geminiApiKey) {
                prefs[Keys.encGeminiKey] = KeystoreCrypto.encrypt(next.geminiApiKey)
            }
            if (next.groqApiKey != current.groqApiKey) {
                prefs[Keys.encGroqKey] = KeystoreCrypto.encrypt(next.groqApiKey)
            }
            if (next.openAiApiKey != current.openAiApiKey) {
                prefs[Keys.encOpenAiKey] = KeystoreCrypto.encrypt(next.openAiApiKey)
            }
            if (next.localApiKey != current.localApiKey) {
                prefs[Keys.encLocalKey] = KeystoreCrypto.encrypt(next.localApiKey)
            }
        }
    }

    suspend fun clearAllApiKeys() {
        update {
            it.copy(
                geminiApiKey = "",
                groqApiKey = "",
                openAiApiKey = "",
                localApiKey = "",
            )
        }
    }

    suspend fun exportSafeConfigJson(): String =
        buildSafeExportJson(settingsFlow.first())

    companion object {
        fun buildSafeExportJson(s: AppSettings): String {
            return JSONObject().apply {
                put("app", "AYSHA – Your Personal AI Assistant")
                put("assistantName", s.assistantName)
                put("userName", s.userName)
                put("language", s.language.name)
                put("activeProvider", s.activeProvider.name)
                put("fallbackEnabled", s.fallbackEnabled)
                put("offlineOnlyMode", s.offlineOnlyMode)
                put("voicePersonalityId", s.voicePersonalityId)
                put("themeMode", s.themeMode.name)
                put("accentPalette", s.accentPalette.name)
                put("personalityStyle", s.personalityStyle.name)
                put("wakeWordsCsv", s.wakeWordsCsv)
                put("apiKeysIncluded", false)
            }.toString(2)
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
            if (raw.isNullOrBlank()) return default
            return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
        }
    }
}
