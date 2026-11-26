package com.aysha.assistant.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.AwarenessSensitivity
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.CompanionIntimacyLevel
import com.aysha.assistant.core.CompanionMode
import com.aysha.assistant.core.CornerStyle
import com.aysha.assistant.core.HudColorOption
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.MayaPalette
import com.aysha.assistant.core.OrbRenderMode
import com.aysha.assistant.core.OrbStyle
import com.aysha.assistant.core.PersonalityStyle
import com.aysha.assistant.core.SubAgentRole
import com.aysha.assistant.core.SurfaceStyle
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.UiFontFamily
import com.aysha.assistant.core.UiTextScale
import com.aysha.assistant.core.UserGender
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
        val mayaPalette = stringPreferencesKey("maya_palette")
        val surfaceStyle = stringPreferencesKey("surface_style")
        val cornerStyle = stringPreferencesKey("corner_style")
        val uiFont = stringPreferencesKey("ui_font")
        val uiScale = stringPreferencesKey("ui_scale")
        val orbStyle = stringPreferencesKey("orb_style")
        val hudColor = stringPreferencesKey("hud_color")
        val hudCoreColor = stringPreferencesKey("hud_core_color")
        val hudMainColor = stringPreferencesKey("hud_main_color")
        val hudRaysLengthPct = floatPreferencesKey("hud_rays_length_pct")
        val hudRingEnabled = booleanPreferencesKey("hud_ring_enabled")
        val orbRenderMode = stringPreferencesKey("orb_render_mode")
        val glassTransparency = floatPreferencesKey("glass_transparency")
        val personalityStyle = stringPreferencesKey("personality_style")
        val companionMode = stringPreferencesKey("companion_mode")
        val companionIntimacy = stringPreferencesKey("companion_intimacy")
        val userGender = stringPreferencesKey("user_gender")
        val adultBirthYear = intPreferencesKey("adult_birth_year")
        val adultAgeVerified = booleanPreferencesKey("adult_age_verified")
        val adultTermsAccepted = booleanPreferencesKey("adult_terms_accepted")
        val adultRatedConfirmed = booleanPreferencesKey("adult_rated_confirmed")
        val adultVerifiedAtMs = longPreferencesKey("adult_verified_at_ms")
        val realisticTypingEnabled = booleanPreferencesKey("realistic_typing_enabled")
        val activeSubAgent = stringPreferencesKey("active_sub_agent")
        val awarenessEnabled = booleanPreferencesKey("awareness_enabled")
        val awarenessSensitivity = stringPreferencesKey("awareness_sensitivity")
        val followUpWindowSeconds = intPreferencesKey("followup_window_seconds")
        val bargeInEnabled = booleanPreferencesKey("barge_in_enabled")
        val muteDuringMedia = booleanPreferencesKey("mute_during_media")
        val emergencyContact = stringPreferencesKey("emergency_contact")
        val emergencySosMessage = stringPreferencesKey("emergency_sos_message")
        val appLockEnabled = booleanPreferencesKey("app_lock_enabled")
        val selectedVibeId = stringPreferencesKey("selected_vibe_id")
        val geminiTextModel = stringPreferencesKey("gemini_text_model")
        val geminiLiveModel = stringPreferencesKey("gemini_live_model")
        val groqModel = stringPreferencesKey("groq_model")
        val openAiBaseUrl = stringPreferencesKey("openai_base_url")
        val openAiModel = stringPreferencesKey("openai_model")
        val localBaseUrl = stringPreferencesKey("local_base_url")
        val localModel = stringPreferencesKey("local_model")
        val onboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val autoMemoryEnabled = booleanPreferencesKey("auto_memory_enabled")
        val soundFeedbackEnabled = booleanPreferencesKey("sound_feedback_enabled")
        val notificationSummaryEnabled = booleanPreferencesKey("notification_summary_enabled")

        // AES-256-GCM Keystore encrypted blobs
        val encGeminiKey = stringPreferencesKey("enc_gemini_key")
        val encGroqKey = stringPreferencesKey("enc_groq_key")
        val encOpenAiKey = stringPreferencesKey("enc_openai_key")
        val encLocalKey = stringPreferencesKey("enc_local_key")
        val encFirebaseRefreshToken = stringPreferencesKey("enc_firebase_refresh_token")

        // Google auth & cached server-verified license
        val authUid = stringPreferencesKey("auth_uid")
        val authEmail = stringPreferencesKey("auth_email")
        val authDisplayName = stringPreferencesKey("auth_display_name")
        val cachedLicensePlan = stringPreferencesKey("cached_license_plan")
        val cachedLicenseStatus = stringPreferencesKey("cached_license_status")
        val cachedLicenseLifetime = booleanPreferencesKey("cached_license_lifetime")
        val cachedLicenseExpiresAtMs = longPreferencesKey("cached_license_expires_at_ms")
        val cachedLicenseServerNowMs = longPreferencesKey("cached_license_server_now_ms")
        val cachedLicenseVerifiedDeviceMs = longPreferencesKey("cached_license_verified_device_ms")
        val cachedLicenseCouponCode = stringPreferencesKey("cached_license_coupon_code")
        val cachedLicenseKey = stringPreferencesKey("cached_license_key")
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
            val mayaPal = enumOrDefault(p[Keys.mayaPalette], MayaPalette.OBSIDIAN)
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
                themeMode = if (mayaPal.isDark) ThemeMode.DARK else ThemeMode.LIGHT,
                accentPalette = enumOrDefault(p[Keys.accentPalette], AccentPalette.COSMIC_CYAN),
                mayaPalette = mayaPal,
                surfaceStyle = enumOrDefault(p[Keys.surfaceStyle], SurfaceStyle.GLASS),
                cornerStyle = enumOrDefault(p[Keys.cornerStyle], CornerStyle.ROUNDED),
                uiFont = enumOrDefault(p[Keys.uiFont], UiFontFamily.INTER),
                uiScale = enumOrDefault(p[Keys.uiScale], UiTextScale.NORMAL),
                orbStyle = enumOrDefault(p[Keys.orbStyle], OrbStyle.AYSHA_2047),
                hudColor = enumOrDefault(p[Keys.hudColor], HudColorOption.CYAN),
                hudCoreColor = enumOrDefault(p[Keys.hudCoreColor], HudColorOption.CYAN),
                hudMainColor = enumOrDefault(p[Keys.hudMainColor], HudColorOption.CYAN),
                hudRaysLengthPct = (p[Keys.hudRaysLengthPct] ?: 0.72f).coerceIn(0.35f, 1.25f),
                hudRingEnabled = p[Keys.hudRingEnabled] ?: true,
                orbRenderMode = enumOrDefault(p[Keys.orbRenderMode], OrbRenderMode.HUD_ORB),
                glassTransparency = (p[Keys.glassTransparency] ?: 0.72f).coerceIn(0.35f, 0.95f),
                personalityStyle = enumOrDefault(p[Keys.personalityStyle], PersonalityStyle.FRIENDLY),
                companionMode = enumOrDefault(p[Keys.companionMode], CompanionMode.BEST_FRIEND),
                companionIntimacy = enumOrDefault(p[Keys.companionIntimacy], CompanionIntimacyLevel.FRIENDLY),
                userGender = enumOrDefault(p[Keys.userGender], UserGender.UNSPECIFIED),
                adultBirthYear = p[Keys.adultBirthYear] ?: 0,
                adultAgeVerified = p[Keys.adultAgeVerified] ?: false,
                adultTermsAccepted = p[Keys.adultTermsAccepted] ?: false,
                adultRatedConfirmed = p[Keys.adultRatedConfirmed] ?: false,
                adultVerifiedAtMs = p[Keys.adultVerifiedAtMs] ?: 0L,
                realisticTypingEnabled = p[Keys.realisticTypingEnabled] ?: false,
                activeSubAgent = enumOrDefault(p[Keys.activeSubAgent], SubAgentRole.AUTO),
                awarenessEnabled = p[Keys.awarenessEnabled] ?: true,
                awarenessSensitivity = enumOrDefault(p[Keys.awarenessSensitivity], AwarenessSensitivity.BALANCED),
                followUpWindowSeconds = (p[Keys.followUpWindowSeconds] ?: 12).coerceIn(0, 60),
                bargeInEnabled = p[Keys.bargeInEnabled] ?: true,
                muteDuringMedia = p[Keys.muteDuringMedia] ?: false,
                emergencyContact = p[Keys.emergencyContact] ?: "",
                emergencySosMessage = p[Keys.emergencySosMessage]?.ifBlank { "Emergency! Please call me or check on me right now." }
                    ?: "Emergency! Please call me or check on me right now.",
                appLockEnabled = p[Keys.appLockEnabled] ?: false,
                selectedVibeId = p[Keys.selectedVibeId]?.ifBlank { "chill" } ?: "chill",
                geminiTextModel = p[Keys.geminiTextModel]?.ifBlank { "gemini-3.8-flash" } ?: "gemini-3.8-flash",
                geminiLiveModel = p[Keys.geminiLiveModel]?.ifBlank { "gemini-3.8-live" } ?: "gemini-3.8-live",
                groqModel = p[Keys.groqModel]?.ifBlank { "llama-3.3-70b-versatile" } ?: "llama-3.3-70b-versatile",
                openAiBaseUrl = p[Keys.openAiBaseUrl]?.ifBlank { "https://api.openai.com/v1" } ?: "https://api.openai.com/v1",
                openAiModel = p[Keys.openAiModel]?.ifBlank { "gpt-4o-mini" } ?: "gpt-4o-mini",
                localBaseUrl = p[Keys.localBaseUrl]?.ifBlank { "http://127.0.0.1:11434/v1" } ?: "http://127.0.0.1:11434/v1",
                localModel = p[Keys.localModel]?.ifBlank { "llama3.2" } ?: "llama3.2",
                onboardingCompleted = p[Keys.onboardingCompleted] ?: false,
                autoMemoryEnabled = p[Keys.autoMemoryEnabled] ?: true,
                soundFeedbackEnabled = p[Keys.soundFeedbackEnabled] ?: true,
                notificationSummaryEnabled = p[Keys.notificationSummaryEnabled] ?: true,
                geminiApiKey = KeystoreCrypto.decrypt(p[Keys.encGeminiKey].orEmpty()),
                groqApiKey = KeystoreCrypto.decrypt(p[Keys.encGroqKey].orEmpty()),
                openAiApiKey = KeystoreCrypto.decrypt(p[Keys.encOpenAiKey].orEmpty()),
                localApiKey = KeystoreCrypto.decrypt(p[Keys.encLocalKey].orEmpty()),
                firebaseRefreshToken = KeystoreCrypto.decrypt(p[Keys.encFirebaseRefreshToken].orEmpty()),
                authUid = p[Keys.authUid].orEmpty(),
                authEmail = p[Keys.authEmail].orEmpty(),
                authDisplayName = p[Keys.authDisplayName].orEmpty(),
                cachedLicensePlan = LicensePlan.fromId(p[Keys.cachedLicensePlan]),
                cachedLicenseStatus = p[Keys.cachedLicenseStatus] ?: LicenseStatus.SIGNED_OUT.name,
                cachedLicenseLifetime = p[Keys.cachedLicenseLifetime] ?: false,
                cachedLicenseExpiresAtMs = p[Keys.cachedLicenseExpiresAtMs] ?: 0L,
                cachedLicenseServerNowMs = p[Keys.cachedLicenseServerNowMs] ?: 0L,
                cachedLicenseVerifiedDeviceMs = p[Keys.cachedLicenseVerifiedDeviceMs] ?: 0L,
                cachedLicenseCouponCode = p[Keys.cachedLicenseCouponCode].orEmpty(),
                cachedLicenseKey = p[Keys.cachedLicenseKey].orEmpty(),
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
            prefs[Keys.themeMode] = if (next.mayaPalette.isDark) ThemeMode.DARK.name else ThemeMode.LIGHT.name
            prefs[Keys.accentPalette] = next.accentPalette.name
            prefs[Keys.mayaPalette] = next.mayaPalette.name
            prefs[Keys.surfaceStyle] = next.surfaceStyle.name
            prefs[Keys.cornerStyle] = next.cornerStyle.name
            prefs[Keys.uiFont] = next.uiFont.name
            prefs[Keys.uiScale] = next.uiScale.name
            prefs[Keys.orbStyle] = next.orbStyle.name
            prefs[Keys.hudColor] = next.hudColor.name
            prefs[Keys.hudCoreColor] = next.hudCoreColor.name
            prefs[Keys.hudMainColor] = next.hudMainColor.name
            prefs[Keys.hudRaysLengthPct] = next.hudRaysLengthPct.coerceIn(0.35f, 1.25f)
            prefs[Keys.hudRingEnabled] = next.hudRingEnabled
            prefs[Keys.orbRenderMode] = next.orbRenderMode.name
            prefs[Keys.glassTransparency] = next.glassTransparency.coerceIn(0.35f, 0.95f)
            prefs[Keys.personalityStyle] = next.personalityStyle.name
            prefs[Keys.companionMode] = next.companionMode.name
            prefs[Keys.companionIntimacy] = next.companionIntimacy.name
            prefs[Keys.userGender] = next.userGender.name
            prefs[Keys.adultBirthYear] = next.adultBirthYear
            prefs[Keys.adultAgeVerified] = next.adultAgeVerified
            prefs[Keys.adultTermsAccepted] = next.adultTermsAccepted
            prefs[Keys.adultRatedConfirmed] = next.adultRatedConfirmed
            prefs[Keys.adultVerifiedAtMs] = next.adultVerifiedAtMs
            prefs[Keys.realisticTypingEnabled] = next.realisticTypingEnabled
            prefs[Keys.activeSubAgent] = next.activeSubAgent.name
            prefs[Keys.awarenessEnabled] = next.awarenessEnabled
            prefs[Keys.awarenessSensitivity] = next.awarenessSensitivity.name
            prefs[Keys.followUpWindowSeconds] = next.followUpWindowSeconds.coerceIn(0, 60)
            prefs[Keys.bargeInEnabled] = next.bargeInEnabled
            prefs[Keys.muteDuringMedia] = next.muteDuringMedia
            prefs[Keys.emergencyContact] = next.emergencyContact.trim()
            prefs[Keys.emergencySosMessage] = next.emergencySosMessage.trim()
            prefs[Keys.appLockEnabled] = next.appLockEnabled
            prefs[Keys.selectedVibeId] = next.selectedVibeId
            prefs[Keys.geminiTextModel] = next.geminiTextModel
            prefs[Keys.geminiLiveModel] = next.geminiLiveModel
            prefs[Keys.groqModel] = next.groqModel
            prefs[Keys.openAiBaseUrl] = next.openAiBaseUrl
            prefs[Keys.openAiModel] = next.openAiModel
            prefs[Keys.localBaseUrl] = next.localBaseUrl
            prefs[Keys.localModel] = next.localModel
            prefs[Keys.onboardingCompleted] = next.onboardingCompleted
            prefs[Keys.autoMemoryEnabled] = next.autoMemoryEnabled
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
            if (next.firebaseRefreshToken != current.firebaseRefreshToken) {
                prefs[Keys.encFirebaseRefreshToken] = KeystoreCrypto.encrypt(next.firebaseRefreshToken)
            }
            prefs[Keys.authUid] = next.authUid
            prefs[Keys.authEmail] = next.authEmail
            prefs[Keys.authDisplayName] = next.authDisplayName
            prefs[Keys.cachedLicensePlan] = next.cachedLicensePlan.id
            prefs[Keys.cachedLicenseStatus] = next.cachedLicenseStatus
            prefs[Keys.cachedLicenseLifetime] = next.cachedLicenseLifetime
            prefs[Keys.cachedLicenseExpiresAtMs] = next.cachedLicenseExpiresAtMs
            prefs[Keys.cachedLicenseServerNowMs] = next.cachedLicenseServerNowMs
            prefs[Keys.cachedLicenseVerifiedDeviceMs] = next.cachedLicenseVerifiedDeviceMs
            prefs[Keys.cachedLicenseCouponCode] = next.cachedLicenseCouponCode
            prefs[Keys.cachedLicenseKey] = next.cachedLicenseKey
        }
    }

    suspend fun clearAuthAndLicense() {
        update {
            it.copy(
                firebaseRefreshToken = "",
                authUid = "",
                authEmail = "",
                authDisplayName = "",
                cachedLicensePlan = LicensePlan.NONE,
                cachedLicenseStatus = LicenseStatus.SIGNED_OUT.name,
                cachedLicenseLifetime = false,
                cachedLicenseExpiresAtMs = 0L,
                cachedLicenseServerNowMs = 0L,
                cachedLicenseVerifiedDeviceMs = 0L,
                cachedLicenseCouponCode = "",
                cachedLicenseKey = "",
            )
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

    suspend fun withdrawAdultConfirmation() {
        update {
            it.copy(
                companionIntimacy = CompanionIntimacyLevel.FRIENDLY,
                adultBirthYear = 0,
                adultAgeVerified = false,
                adultTermsAccepted = false,
                adultRatedConfirmed = false,
                adultVerifiedAtMs = 0L,
            )
        }
    }

    suspend fun exportSafeConfigJson(): String =
        buildSafeExportJson(settingsFlow.first())

    suspend fun restoreSafeSettings(jsonObj: JSONObject) {
        update { cur -> applySafeRestoreJson(cur, jsonObj) }
    }

    companion object {
        fun buildSafeExportJson(s: AppSettings): String {
            return JSONObject().apply {
                put("app", "AYSHA – Your Personal AI Assistant")
                put("version", "2.0.0")
                put("assistantName", s.assistantName)
                put("userName", s.userName)
                put("language", s.language.name)
                put("activeProvider", s.activeProvider.name)
                put("fallbackEnabled", s.fallbackEnabled)
                put("offlineOnlyMode", s.offlineOnlyMode)
                put("voicePersonalityId", s.voicePersonalityId)
                put("themeMode", s.themeMode.name)
                put("accentPalette", s.accentPalette.name)
                put("mayaPalette", s.mayaPalette.name)
                put("surfaceStyle", s.surfaceStyle.name)
                put("cornerStyle", s.cornerStyle.name)
                put("uiFont", s.uiFont.name)
                put("uiScale", s.uiScale.name)
                put("orbStyle", s.orbStyle.name)
                put("hudColor", s.hudColor.name)
                put("hudCoreColor", s.hudCoreColor.name)
                put("hudMainColor", s.hudMainColor.name)
                put("hudRaysLengthPct", s.hudRaysLengthPct.toDouble())
                put("hudRingEnabled", s.hudRingEnabled)
                put("orbRenderMode", s.orbRenderMode.name)
                put("personalityStyle", s.personalityStyle.name)
                put("companionMode", s.companionMode.name)
                put("userGender", s.userGender.name)
                put("activeSubAgent", s.activeSubAgent.name)
                put("awarenessEnabled", s.awarenessEnabled)
                put("awarenessSensitivity", s.awarenessSensitivity.name)
                put("followUpWindowSeconds", s.followUpWindowSeconds)
                put("bargeInEnabled", s.bargeInEnabled)
                put("muteDuringMedia", s.muteDuringMedia)
                put("wakeWordsCsv", s.wakeWordsCsv)
                put("emergencyContact", s.emergencyContact)
                put("apiKeysIncluded", false)
            }.toString(2)
        }

        fun applySafeRestoreJson(cur: AppSettings, obj: JSONObject): AppSettings {
            val palette = enumOrDefault(obj.optString("mayaPalette"), cur.mayaPalette)
            return cur.copy(
                assistantName = obj.optString("assistantName").ifBlank { cur.assistantName },
                userName = obj.optString("userName").ifBlank { cur.userName },
                language = enumOrDefault(obj.optString("language"), cur.language),
                activeProvider = enumOrDefault(obj.optString("activeProvider"), cur.activeProvider),
                fallbackEnabled = obj.optBoolean("fallbackEnabled", cur.fallbackEnabled),
                offlineOnlyMode = obj.optBoolean("offlineOnlyMode", cur.offlineOnlyMode),
                voicePersonalityId = obj.optString("voicePersonalityId").ifBlank { cur.voicePersonalityId },
                mayaPalette = palette,
                themeMode = if (palette.isDark) ThemeMode.DARK else ThemeMode.LIGHT,
                accentPalette = enumOrDefault(obj.optString("accentPalette"), cur.accentPalette),
                surfaceStyle = enumOrDefault(obj.optString("surfaceStyle"), cur.surfaceStyle),
                cornerStyle = enumOrDefault(obj.optString("cornerStyle"), cur.cornerStyle),
                uiFont = enumOrDefault(obj.optString("uiFont"), cur.uiFont),
                uiScale = enumOrDefault(obj.optString("uiScale"), cur.uiScale),
                orbStyle = enumOrDefault(obj.optString("orbStyle"), cur.orbStyle),
                hudColor = enumOrDefault(obj.optString("hudColor"), cur.hudColor),
                hudCoreColor = enumOrDefault(obj.optString("hudCoreColor"), cur.hudCoreColor),
                hudMainColor = enumOrDefault(obj.optString("hudMainColor"), cur.hudMainColor),
                hudRaysLengthPct = obj.optDouble("hudRaysLengthPct", cur.hudRaysLengthPct.toDouble()).toFloat().coerceIn(0.35f, 1.25f),
                hudRingEnabled = obj.optBoolean("hudRingEnabled", cur.hudRingEnabled),
                orbRenderMode = enumOrDefault(obj.optString("orbRenderMode"), cur.orbRenderMode),
                personalityStyle = enumOrDefault(obj.optString("personalityStyle"), cur.personalityStyle),
                companionMode = enumOrDefault(obj.optString("companionMode"), cur.companionMode),
                userGender = enumOrDefault(obj.optString("userGender"), cur.userGender),
                activeSubAgent = enumOrDefault(obj.optString("activeSubAgent"), cur.activeSubAgent),
                awarenessEnabled = obj.optBoolean("awarenessEnabled", cur.awarenessEnabled),
                awarenessSensitivity = enumOrDefault(obj.optString("awarenessSensitivity"), cur.awarenessSensitivity),
                followUpWindowSeconds = obj.optInt("followUpWindowSeconds", cur.followUpWindowSeconds).coerceIn(0, 60),
                bargeInEnabled = obj.optBoolean("bargeInEnabled", cur.bargeInEnabled),
                muteDuringMedia = obj.optBoolean("muteDuringMedia", cur.muteDuringMedia),
                wakeWordsCsv = obj.optString("wakeWordsCsv").ifBlank { cur.wakeWordsCsv },
                emergencyContact = obj.optString("emergencyContact", cur.emergencyContact),
            )
        }

        private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
            if (raw.isNullOrBlank()) return default
            return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
        }
    }
}
