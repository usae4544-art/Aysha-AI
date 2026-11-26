package com.aysha.assistant.core

import androidx.compose.ui.graphics.Color

enum class OrbState(val displayLabel: String) {
    IDLE("Ready"),
    LISTENING("Listening..."),
    PROCESSING("Thinking..."),
    SPEAKING("Speaking..."),
    AUTOMATING("Automating..."),
    CONNECTING("Connecting..."),
    ERROR("Attention Needed"),
}

enum class ThemeMode(val label: String) {
    DARK("Dark Mode"),
    LIGHT("Light Mode"),
}

enum class AccentPalette(
    val label: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val glowHex: Long,
) {
    COSMIC_CYAN("Cosmic Cyan", 0xFF22D3EE, 0xFF7C3AED, 0xFF38BDF8),
    ELECTRIC_VIOLET("Electric Violet", 0xFF8B5CF6, 0xFFEC4899, 0xFFA78BFA),
    AURORA_TEAL("Aurora Teal", 0xFF14B8A6, 0xFF0EA5E9, 0xFF2DD4BF),
    EMERALD_PULSE("Emerald Pulse", 0xFF10B981, 0xFF06B6D4, 0xFF34D399),
    SOLAR_AMBER("Solar Amber", 0xFFF59E0B, 0xFFEC4899, 0xFFFBBF24),
    NEON_ROSE("Neon Rose", 0xFFF43F5E, 0xFF8B5CF6, 0xFFFB7185);

    val primaryColor: Color get() = Color(primaryHex)
    val secondaryColor: Color get() = Color(secondaryHex)
    val glowColor: Color get() = Color(glowHex)
}

enum class AssistantLanguage(
    val label: String,
    val sttLocaleTag: String,
    val ttsLocaleTag: String,
) {
    HINGLISH("Hinglish (Mix)", "en-IN", "en-IN"),
    HINDI("Hindi (हिंदी)", "hi-IN", "hi-IN"),
    ENGLISH("English", "en-US", "en-US"),
}

enum class BrainProvider(
    val displayName: String,
    val subtitle: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val requiresApiKey: Boolean,
) {
    GEMINI(
        displayName = "Google Gemini",
        subtitle = "Gemini 3.8 Flash / Live",
        defaultBaseUrl = "https://generativelanguage.googleapis.com/v1beta",
        defaultModel = "gemini-3.8-flash",
        requiresApiKey = true,
    ),
    GROQ(
        displayName = "Groq",
        subtitle = "Ultra-fast Llama / Mixtral",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        requiresApiKey = true,
    ),
    OPENAI(
        displayName = "OpenAI-Compatible",
        subtitle = "OpenAI, OpenRouter, DeepSeek",
        defaultBaseUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        requiresApiKey = true,
    ),
    LOCAL_MODEL(
        displayName = "Local / LAN Model",
        subtitle = "Ollama / llama.cpp server",
        defaultBaseUrl = "http://127.0.0.1:11434/v1",
        defaultModel = "llama3.2",
        requiresApiKey = false,
    ),
    OFFLINE(
        displayName = "Offline Mode",
        subtitle = "100% On-Device Commands",
        defaultBaseUrl = "",
        defaultModel = "aysha-local-engine",
        requiresApiKey = false,
    ),
}

enum class VoiceEngineMode(val label: String) {
    STANDARD_VOICE("Voice & Offline TTS"),
    GEMINI_LIVE("Gemini Live Audio"),
    TEXT_ONLY("Text Chat"),
}

enum class VoiceGender(val label: String) {
    FEMALE("Female"),
    MALE("Male"),
}

data class VoicePersonality(
    val id: String,
    val name: String,
    val subtitle: String,
    val gender: VoiceGender,
    val basePitch: Float,
    val rateMultiplier: Float,
    val toneHint: String,
    val geminiLiveVoice: String,
)

object VoicePersonalities {
    val all: List<VoicePersonality> = listOf(
        VoicePersonality(
            id = "aysha_natural",
            name = "Aysha Natural",
            subtitle = "Natural & Balanced Voice",
            gender = VoiceGender.FEMALE,
            basePitch = 1.04f,
            rateMultiplier = 1.00f,
            toneHint = "Warm, intelligent, natural conversational companion.",
            geminiLiveVoice = "Aoede",
        ),
        VoicePersonality(
            id = "sara_friendly",
            name = "Sara Friendly",
            subtitle = "Upbeat & Cheerful Voice",
            gender = VoiceGender.FEMALE,
            basePitch = 1.12f,
            rateMultiplier = 1.06f,
            toneHint = "Energetic, friendly, encouraging and playful.",
            geminiLiveVoice = "Zephyr",
        ),
        VoicePersonality(
            id = "priya_calm",
            name = "Priya Calm",
            subtitle = "Calm & Mindful Voice",
            gender = VoiceGender.FEMALE,
            basePitch = 0.96f,
            rateMultiplier = 0.93f,
            toneHint = "Gentle, patient, thoughtful and soothing.",
            geminiLiveVoice = "Kore",
        ),
        VoicePersonality(
            id = "riya_soft",
            name = "Riya Soft",
            subtitle = "Soft & Caring Voice",
            gender = VoiceGender.FEMALE,
            basePitch = 1.08f,
            rateMultiplier = 0.96f,
            toneHint = "Soft-spoken, caring and empathetic companion.",
            geminiLiveVoice = "Leda",
        ),
        VoicePersonality(
            id = "aarav_confident",
            name = "Aarav Confident",
            subtitle = "Crisp & Confident Voice",
            gender = VoiceGender.MALE,
            basePitch = 0.84f,
            rateMultiplier = 1.02f,
            toneHint = "Clear, confident, direct and reliable.",
            geminiLiveVoice = "Charon",
        ),
        VoicePersonality(
            id = "kabir_warm",
            name = "Kabir Warm",
            subtitle = "Deep & Relaxed Voice",
            gender = VoiceGender.MALE,
            basePitch = 0.78f,
            rateMultiplier = 0.95f,
            toneHint = "Calm, grounded, helpful and steady.",
            geminiLiveVoice = "Fenrir",
        ),
    )

    fun byId(id: String): VoicePersonality =
        all.firstOrNull { it.id == id } ?: all.first()
}

enum class PersonalityStyle(val label: String, val directive: String) {
    FRIENDLY("Friendly & Caring", "Be warm, empathetic, conversational, and encouraging."),
    INTELLIGENT("Smart & Crisp", "Be analytical, sharp, clear, and efficient."),
    PLAYFUL("Playful & Witty", "Add tasteful wit and positive energy while staying genuinely helpful."),
    CALM("Calm & Focused", "Keep responses serene, structured, and reassuring."),
}

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM,
}

data class ChatMessage(
    val id: Long = 0,
    val role: ChatRole,
    val text: String,
    val providerTag: String = "",
    val imagePath: String? = null,
    val timestampMs: Long = System.currentTimeMillis(),
)

data class MemoryEntry(
    val id: Long = 0,
    val title: String,
    val content: String,
    val tags: String = "",
    val approvedByUser: Boolean = true,
    val updatedAtMs: Long = System.currentTimeMillis(),
)

data class MacroStep(
    val tool: String,
    val args: Map<String, String> = emptyMap(),
    val optional: Boolean = false,
)

data class MacroDefinition(
    val name: String,
    val triggerPhrase: String = "",
    val intent: String,
    val params: List<String> = emptyList(),
    val steps: List<MacroStep> = emptyList(),
    val isBuiltIn: Boolean = false,
)

data class ToolLogEntry(
    val id: Long = 0,
    val toolName: String,
    val summary: String,
    val success: Boolean,
    val timestampMs: Long = System.currentTimeMillis(),
)

data class CapturedNotification(
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val postedAtMs: Long,
)

enum class PendingConfirmationType {
    PHONE_CALL,
    SEND_SMS,
}

data class PendingActionConfirmation(
    val id: Long = System.currentTimeMillis(),
    val type: PendingConfirmationType,
    val title: String,
    val summary: String,
    val target: String,
    val messageBody: String = "",
)

data class AppSettings(
    val assistantName: String = "AYSHA",
    val userName: String = "Friend",
    val language: AssistantLanguage = AssistantLanguage.HINGLISH,
    val activeProvider: BrainProvider = BrainProvider.GEMINI,
    val fallbackEnabled: Boolean = true,
    val offlineOnlyMode: Boolean = false,
    val voiceEngineMode: VoiceEngineMode = VoiceEngineMode.STANDARD_VOICE,
    val voicePersonalityId: String = "aysha_natural",
    val selectedTtsVoiceName: String = "",
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVolume: Float = 1.0f,
    val autoSpeakReplies: Boolean = true,
    val continuousConversation: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val wakeWordsCsv: String = "Hey Aysha,Hello Aysha,Wake up Aysha",
    val floatingOrbEnabled: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentPalette: AccentPalette = AccentPalette.COSMIC_CYAN,
    val glassTransparency: Float = 0.72f,
    val personalityStyle: PersonalityStyle = PersonalityStyle.FRIENDLY,
    val geminiTextModel: String = "gemini-3.8-flash",
    val geminiLiveModel: String = "gemini-3.8-live",
    val groqModel: String = "llama-3.3-70b-versatile",
    val openAiBaseUrl: String = "https://api.openai.com/v1",
    val openAiModel: String = "gpt-4o-mini",
    val localBaseUrl: String = "http://127.0.0.1:11434/v1",
    val localModel: String = "llama3.2",
    val onboardingCompleted: Boolean = false,
    val soundFeedbackEnabled: Boolean = true,
    val notificationSummaryEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val groqApiKey: String = "",
    val openAiApiKey: String = "",
    val localApiKey: String = "",
) {
    fun currentKeyFor(provider: BrainProvider): String = when (provider) {
        BrainProvider.GEMINI -> geminiApiKey
        BrainProvider.GROQ -> groqApiKey
        BrainProvider.OPENAI -> openAiApiKey
        BrainProvider.LOCAL_MODEL -> localApiKey
        BrainProvider.OFFLINE -> ""
    }

    fun currentModelFor(provider: BrainProvider): String = when (provider) {
        BrainProvider.GEMINI -> geminiTextModel.ifBlank { provider.defaultModel }
        BrainProvider.GROQ -> groqModel.ifBlank { provider.defaultModel }
        BrainProvider.OPENAI -> openAiModel.ifBlank { provider.defaultModel }
        BrainProvider.LOCAL_MODEL -> localModel.ifBlank { provider.defaultModel }
        BrainProvider.OFFLINE -> provider.defaultModel
    }

    fun maskedKey(key: String): String {
        val clean = key.trim()
        return when {
            clean.isEmpty() -> "Not set"
            clean.length <= 8 -> "••••••••"
            else -> "${clean.take(4)}••••${clean.takeLast(4)}"
        }
    }
}
