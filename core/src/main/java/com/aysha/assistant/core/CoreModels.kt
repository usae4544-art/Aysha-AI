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

enum class MayaPalette(
    val label: String,
    val blurb: String,
    val isDark: Boolean,
    val bgHex: Long,
    val bgDeepHex: Long,
    val surfaceHex: Long,
    val elevatedHex: Long,
    val borderHex: Long,
    val textPrimaryHex: Long,
    val textSecondaryHex: Long,
    val accentHex: Long,
    val secondaryHex: Long,
) {
    OBSIDIAN(
        label = "Obsidian",
        blurb = "Pure black & graphite, sharp minimal contrast",
        isDark = true,
        bgHex = 0xFF000000,
        bgDeepHex = 0xFF07080C,
        surfaceHex = 0xFF101218,
        elevatedHex = 0xFF171A23,
        borderHex = 0x24FFFFFF,
        textPrimaryHex = 0xFFFAFAFA,
        textSecondaryHex = 0xFF9CA3AF,
        accentHex = 0xFF22D3EE,
        secondaryHex = 0xFF6366F1,
    ),
    MIDNIGHT(
        label = "Midnight",
        blurb = "Near-black deep midnight blue, crisp accent",
        isDark = true,
        bgHex = 0xFF040715,
        bgDeepHex = 0xFF0B1230,
        surfaceHex = 0xFF0F1838,
        elevatedHex = 0xFF16214D,
        borderHex = 0x477DD3FC,
        textPrimaryHex = 0xFFF8FAFC,
        textSecondaryHex = 0xFF94A3B8,
        accentHex = 0xFF22D3EE,
        secondaryHex = 0xFF7C3AED,
    ),
    ABYSS(
        label = "Abyss",
        blurb = "Deep oceanic teal & cyan, clinical dark",
        isDark = true,
        bgHex = 0xFF020C10,
        bgDeepHex = 0xFF051A22,
        surfaceHex = 0xFF09242E,
        elevatedHex = 0xFF0F313D,
        borderHex = 0x422DD4BF,
        textPrimaryHex = 0xFFF0FDFA,
        textSecondaryHex = 0xFF99F6E4,
        accentHex = 0xFF14B8A6,
        secondaryHex = 0xFF06B6D4,
    ),
    EMBER(
        label = "Ember",
        blurb = "Warm carbon & amber, high contrast at night",
        isDark = true,
        bgHex = 0xFF0B0705,
        bgDeepHex = 0xFF17100B,
        surfaceHex = 0xFF211610,
        elevatedHex = 0xFF2B1D15,
        borderHex = 0x44F59E0B,
        textPrimaryHex = 0xFFFFFBEB,
        textSecondaryHex = 0xFFD6B99B,
        accentHex = 0xFFF59E0B,
        secondaryHex = 0xFFEA580C,
    ),
    NOCTURNE(
        label = "Nocturne",
        blurb = "Deep indigo & violet accent, calm night mood",
        isDark = true,
        bgHex = 0xFF080614,
        bgDeepHex = 0xFF130F29,
        surfaceHex = 0xFF1A1538,
        elevatedHex = 0xFF241E4D,
        borderHex = 0x45A78BFA,
        textPrimaryHex = 0xFFF5F3FF,
        textSecondaryHex = 0xFFC4B5FD,
        accentHex = 0xFF8B5CF6,
        secondaryHex = 0xFFEC4899,
    ),
    ROSEWOOD(
        label = "Rosewood",
        blurb = "Warm plum & soft rose, gentlest dark palette",
        isDark = true,
        bgHex = 0xFF10060C,
        bgDeepHex = 0xFF1E0B16,
        surfaceHex = 0xFF29101E,
        elevatedHex = 0xFF361728,
        borderHex = 0x45FB7185,
        textPrimaryHex = 0xFFFFF1F2,
        textSecondaryHex = 0xFFFDA4AF,
        accentHex = 0xFFF43F5E,
        secondaryHex = 0xFFC026D3,
    ),
    CLAY_NAVY(
        label = "Clay Navy",
        blurb = "Deep navy with sculpted 3D neon-blue tiles",
        isDark = true,
        bgHex = 0xFF0A1226,
        bgDeepHex = 0xFF121E3D,
        surfaceHex = 0xFF18274F,
        elevatedHex = 0xFF1F3263,
        borderHex = 0x4438BDF8,
        textPrimaryHex = 0xFFF1F5F9,
        textSecondaryHex = 0xFF93C5FD,
        accentHex = 0xFF38BDF8,
        secondaryHex = 0xFF6366F1,
    ),
    CLAY_DAY(
        label = "Clay Day",
        blurb = "Soft sky-white canvas with puffy 3D clay cards",
        isDark = false,
        bgHex = 0xFFE8F0FE,
        bgDeepHex = 0xFFD9E6FC,
        surfaceHex = 0xFFF8FAFF,
        elevatedHex = 0xFFFFFFFF,
        borderHex = 0x443B82F6,
        textPrimaryHex = 0xFF0F172A,
        textSecondaryHex = 0xFF475569,
        accentHex = 0xFF2563EB,
        secondaryHex = 0xFF7C3AED,
    ),
    DAYLIGHT(
        label = "Daylight",
        blurb = "Clean frosted daylight glass",
        isDark = false,
        bgHex = 0xFFEEF4FF,
        bgDeepHex = 0xFFDBE7FE,
        surfaceHex = 0xFFFFFFFF,
        elevatedHex = 0xFFFFFFFF,
        borderHex = 0x440EA5E9,
        textPrimaryHex = 0xFF0F172A,
        textSecondaryHex = 0xFF475569,
        accentHex = 0xFF0EA5E9,
        secondaryHex = 0xFF6366F1,
    );
}

enum class SurfaceStyle(val label: String, val blurb: String) {
    GLASS("Glass", "Translucent layers, top edge light, deep depth"),
    CLAY("Clay", "Sculpted puffy 3D tiles with tactile bevel"),
    FLAT("Flat", "Solid matte cards with 1dp hairline border"),
    SOFT("Soft", "Subtle elevation lifted softly off background"),
}

enum class CornerStyle(val label: String, val radiusDp: Int) {
    SHARP("Sharp", 8),
    SOFT("Soft", 14),
    ROUNDED("Rounded", 20),
    PILLOW("Pillow", 28),
}

enum class UiFontFamily(val label: String, val blurb: String) {
    INTER("Inter", "App's signature crisp geometric UI typeface"),
    NUNITO("Nunito", "Friendly rounded typeface built for soft & clay cards"),
    JETBRAINS_MONO("Monospace", "Fixed-width technical typeface for clarity"),
    SYSTEM("System", "Your device's default system font"),
    SERIF("Serif", "Editorial book-like reading feel"),
}

enum class UiTextScale(val label: String, val factor: Float) {
    COMPACT("Compact", 0.92f),
    NORMAL("Default", 1.00f),
    LARGE("Large", 1.08f),
    LARGER("Larger", 1.16f),
}

enum class OrbStyle(val label: String, val tagline: String) {
    AYSHA_2047("AYSHA 2047", "Signature HUD — saare looks ek me"),
    NOVA_HUD("Aysha Nova", "Uski apni luminous neon ring"),
    NOVA_3D("Aysha Nova 3D", "Sheesha, roshni, andar se zinda"),
    LIQUID_CORE("Liquid Core", "Zinda plasma wave, machine nahi"),
    JARVIS("J.A.R.V.I.S.", "Amber neural circuits & arcs"),
    JARVIS_3D("J.A.R.V.I.S. 3D", "Gold-amber depth & orbital rings"),
    FRIDAY("F.R.I.D.A.Y.", "Golden visor HUD — sabse alag"),
    RED_HUD("Red HUD", "Crimson rays & core neurons"),
    PARTICLE_SWARM("Particle Swarm", "Orbital data swarm constellation"),
    PULSE_REACTOR("Pulse Reactor", "Reactor core & segmented glyph rings"),
    ULTRON("Ultron", "Electric cyan & geometric shard ring"),
}

enum class HudColorOption(val label: String, val primaryHex: Long, val secondaryHex: Long) {
    CYAN("Cyan", 0xFF22D3EE, 0xFF0EA5E9),
    BLUE("Blue", 0xFF3B82F6, 0xFF6366F1),
    VIOLET("Violet", 0xFF8B5CF6, 0xFFC084FC),
    PINK("Pink", 0xFFEC4899, 0xFFF43F5E),
    GOLD("Gold", 0xFFFACC15, 0xFFF59E0B),
    GREEN("Green", 0xFF10B981, 0xFF22C55E),
    ORANGE("Orange", 0xFFF97316, 0xFFFB923C),
    RED("Red", 0xFFEF4444, 0xFFF43F5E),
    WHITE("White", 0xFFF8FAFC, 0xFF94A3B8),
}

enum class AssistantMood(
    val label: String,
    val caption: String,
    val colorHex: Long,
    val promptTone: String,
) {
    KHUSH("Khush", "Josh me", 0xFF22C55E, "You feel energetic, cheerful, upbeat and enthusiastic."),
    PYAAR_BHARI("Pyaar bhari", "Sab badhiya", 0xFFEC4899, "You feel extra warm, affectionate, caring and supportive."),
    ROOTHI("Roothi hui", "Mana lo", 0xFFF59E0B, "You are slightly teasingly sulky/playful, still helpful when spoken to nicely."),
    NARAZ("Naraz", "Thoda ruko", 0xFFF43F5E, "You feel a bit brief and guarded until the user speaks politely."),
}

enum class CompanionMode(val label: String, val subtitle: String, val directive: String) {
    BEST_FRIEND(
        "Best Friend & Assistant",
        "Natural Hinglish buddy + full phone helper",
        "Speak like a close, reliable smart companion while executing tasks accurately.",
    ),
    CARE_COMPANION(
        "Caring Companion",
        "Warm, affectionate, attentive check-ins",
        "Use warm, caring, encouraging phrasing and check on the user's comfort and goals.",
    ),
    PROFESSIONAL(
        "Crisp Executive Pro",
        "Zero fluff, fast structured answers",
        "Stay concise, direct, analytical, and action-oriented.",
    ),
    PLAYFUL_ROAST(
        "Playful & Witty",
        "Light banter, jokes, witty comebacks",
        "Blend playful humor and witty Hinglish banter while staying genuinely helpful.",
    ),
}

enum class SubAgentRole(val label: String, val shortTag: String, val specialtyInstruction: String) {
    AUTO("Auto Router", "Auto", "Adapt dynamically between assistant, coder, tutor, and device controller."),
    CODING("Coding & Tech", "Code", "Act as a senior engineer: produce exact code, commands, debugging steps, and clean structure."),
    STUDY("Padhai & Whiteboard", "Study", "Teach step-by-step like a patient tutor; break concepts down clearly with examples."),
    RESEARCH("Deep Research", "Research", "Synthesize facts thoroughly, cite sources/search findings, and verify assumptions."),
    PHONE("Phone Control", "Device", "Prioritize device automation, apps, calls, SMS confirmations, alarms, and accessibility macros."),
    CREATIVE("Content & Writing", "Creative", "Draft engaging captions, scripts, emails, messages, and creative copy."),
    MEMORY("Yaadein & Notes", "Memory", "Focus on organizing personal notes, facts, reminders, and recall."),
}

enum class AwarenessSensitivity(val label: String, val rmsThreshold: Float, val blurb: String) {
    RESERVED("Reserved", 0.065f, "Stays quiet until speech is unmistakably clear"),
    BALANCED("Balanced", 0.040f, "Natural balance between wake accuracy and noise rejection"),
    ALERT("Alert", 0.022f, "Responds quickly even to softer voice cues"),
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
            subtitle = "Warm & Expressive Default",
            gender = VoiceGender.FEMALE,
            basePitch = 1.04f,
            rateMultiplier = 1.00f,
            toneHint = "Warm, intelligent, natural conversational companion.",
            geminiLiveVoice = "Aoede",
        ),
        VoicePersonality(
            id = "maya_warm",
            name = "Aysha Silk",
            subtitle = "Lively Hinglish Companion",
            gender = VoiceGender.FEMALE,
            basePitch = 1.07f,
            rateMultiplier = 1.03f,
            toneHint = "Expressive, affectionate, quick-witted everyday companion.",
            geminiLiveVoice = "Autonoe",
        ),
        VoicePersonality(
            id = "friday_crisp",
            name = "F.R.I.D.A.Y. Crisp",
            subtitle = "Sharp AI Tactical Voice",
            gender = VoiceGender.FEMALE,
            basePitch = 0.99f,
            rateMultiplier = 1.06f,
            toneHint = "Crisp, ultra-capable, poised mission assistant.",
            geminiLiveVoice = "Sulafat",
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
        VoicePersonality(
            id = "venom_deep",
            name = "Atlas Deep",
            subtitle = "Bold Resonant Voice",
            gender = VoiceGender.MALE,
            basePitch = 0.73f,
            rateMultiplier = 0.94f,
            toneHint = "Bold, intense, protective and steady.",
            geminiLiveVoice = "Orus",
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
    val source: String = "aapne bataya",
    val archived: Boolean = false,
)

data class UserRule(
    val id: Long = 0,
    val text: String,
    val enabled: Boolean = true,
    val createdAtMs: Long = System.currentTimeMillis(),
)

data class KnowledgeDocument(
    val id: Long = 0,
    val title: String,
    val content: String,
    val chunkCount: Int = 1,
    val addedAtMs: Long = System.currentTimeMillis(),
)

data class WhiteboardCard(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val topic: String,
    val steps: List<String>,
    val codeSnippet: String = "",
    val timestampMs: Long = System.currentTimeMillis(),
)

data class AwarenessDecisionLog(
    val timestampMs: Long = System.currentTimeMillis(),
    val heardText: String,
    val energy: Float,
    val threshold: Float,
    val forwarded: Boolean,
    val reason: String,
)

data class VibePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val query: String,
    val colorHex: Long,
)

object VibePresets {
    val all = listOf(
        VibePreset("chill", "Chill", "Lo-fi · sukoon", "lofi chill hindi beats", 0xFF22D3EE),
        VibePreset("study", "Study Beats", "Focus mode · deep work", "deep focus instrumental study music", 0xFF3B82F6),
        VibePreset("gym", "Gym / Energy", "Full power workout", "high energy gym workout bollywood", 0xFFF97316),
        VibePreset("romantic", "Romantic", "Warm acoustic vibes", "hindi romantic acoustic songs", 0xFFEC4899),
        VibePreset("party", "Party", "Full masti · upbeat", "latest party hits bollywood", 0xFF8B5CF6),
        VibePreset("sad", "Late Night", "Soulful calm tracks", "soulful hindi unplugged songs", 0xFF64748B),
    )
}

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
    val playedCount: Int = 0,
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
    EMERGENCY_SOS,
}

data class PendingActionConfirmation(
    val id: Long = System.currentTimeMillis(),
    val type: PendingConfirmationType,
    val title: String,
    val summary: String,
    val target: String,
    val messageBody: String = "",
)

enum class LicensePlan(
    val id: String,
    val label: String,
    val durationDays: Int,
) {
    NONE("NONE", "No Active Plan", 0),
    ONE_MONTH("ONE_MONTH", "1 Month", 30),
    TWO_MONTHS("TWO_MONTHS", "2 Months", 60),
    ONE_YEAR("ONE_YEAR", "1 Year", 365),
    TWO_YEARS("TWO_YEARS", "2 Years", 730),
    LIFETIME("LIFETIME", "Lifetime (All-Time)", 0);

    companion object {
        fun fromId(raw: String?): LicensePlan {
            val clean = raw.orEmpty().trim().uppercase()
            return entries.firstOrNull { it.id == clean } ?: NONE
        }
    }
}

enum class LicenseStatus {
    SIGNED_OUT,
    CHECKING,
    ACTIVE,
    ADMIN_ACTIVE,
    EXPIRED,
    REVOKED,
    UNCONFIGURED,
    NETWORK_ERROR,
}

data class LicenseInfo(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val plan: LicensePlan = LicensePlan.NONE,
    val isLifetime: Boolean = false,
    val expiresAtMs: Long = 0L,
    val lastServerNowMs: Long = 0L,
    val lastVerifiedDeviceMs: Long = 0L,
    val status: LicenseStatus = LicenseStatus.SIGNED_OUT,
    val couponCode: String = "",
    val message: String = "",
    val firebaseConfigured: Boolean = false,
) {
    fun estimatedServerNowMs(nowDeviceMs: Long = System.currentTimeMillis()): Long {
        return if (lastServerNowMs > 0L && lastVerifiedDeviceMs > 0L) {
            lastServerNowMs + (nowDeviceMs - lastVerifiedDeviceMs).coerceAtLeast(0L)
        } else {
            nowDeviceMs
        }
    }

    fun remainingMs(nowDeviceMs: Long = System.currentTimeMillis()): Long {
        if (status == LicenseStatus.ADMIN_ACTIVE || (status == LicenseStatus.ACTIVE && isLifetime)) {
            return Long.MAX_VALUE
        }
        if (expiresAtMs <= 0L) return 0L
        return (expiresAtMs - estimatedServerNowMs(nowDeviceMs)).coerceAtLeast(0L)
    }

    fun isUnlocked(nowDeviceMs: Long = System.currentTimeMillis()): Boolean {
        if (uid.isBlank() || email.isBlank()) return false
        return when (status) {
            LicenseStatus.ADMIN_ACTIVE -> true
            LicenseStatus.ACTIVE -> {
                if (isLifetime || plan == LicensePlan.LIFETIME) {
                    true
                } else {
                    val rem = remainingMs(nowDeviceMs)
                    val offlineElapsed = if (lastVerifiedDeviceMs > 0L) {
                        (nowDeviceMs - lastVerifiedDeviceMs).coerceAtLeast(0L)
                    } else {
                        0L
                    }
                    rem > 0L && offlineElapsed <= MAX_OFFLINE_GRACE_MS
                }
            }
            else -> false
        }
    }

    companion object {
        const val MAX_OFFLINE_GRACE_MS = 7L * 24L * 60L * 60L * 1000L
    }
}

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
    val mayaPalette: MayaPalette = MayaPalette.OBSIDIAN,
    val surfaceStyle: SurfaceStyle = SurfaceStyle.GLASS,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val uiFont: UiFontFamily = UiFontFamily.INTER,
    val uiScale: UiTextScale = UiTextScale.NORMAL,
    val orbStyle: OrbStyle = OrbStyle.AYSHA_2047,
    val hudColor: HudColorOption = HudColorOption.CYAN,
    val glassTransparency: Float = 0.72f,
    val personalityStyle: PersonalityStyle = PersonalityStyle.FRIENDLY,
    val companionMode: CompanionMode = CompanionMode.BEST_FRIEND,
    val activeSubAgent: SubAgentRole = SubAgentRole.AUTO,
    val awarenessEnabled: Boolean = true,
    val awarenessSensitivity: AwarenessSensitivity = AwarenessSensitivity.BALANCED,
    val followUpWindowSeconds: Int = 12,
    val bargeInEnabled: Boolean = true,
    val muteDuringMedia: Boolean = false,
    val emergencyContact: String = "",
    val emergencySosMessage: String = "Emergency! Please call me or check on me right now.",
    val appLockEnabled: Boolean = false,
    val selectedVibeId: String = "chill",
    val geminiTextModel: String = "gemini-3.8-flash",
    val geminiLiveModel: String = "gemini-3.8-live",
    val groqModel: String = "llama-3.3-70b-versatile",
    val openAiBaseUrl: String = "https://api.openai.com/v1",
    val openAiModel: String = "gpt-4o-mini",
    val localBaseUrl: String = "http://127.0.0.1:11434/v1",
    val localModel: String = "llama3.2",
    val onboardingCompleted: Boolean = false,
    val autoMemoryEnabled: Boolean = true,
    val soundFeedbackEnabled: Boolean = true,
    val notificationSummaryEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val groqApiKey: String = "",
    val openAiApiKey: String = "",
    val localApiKey: String = "",
    val firebaseRefreshToken: String = "",
    val authUid: String = "",
    val authEmail: String = "",
    val authDisplayName: String = "",
    val cachedLicensePlan: LicensePlan = LicensePlan.NONE,
    val cachedLicenseStatus: String = LicenseStatus.SIGNED_OUT.name,
    val cachedLicenseLifetime: Boolean = false,
    val cachedLicenseExpiresAtMs: Long = 0L,
    val cachedLicenseServerNowMs: Long = 0L,
    val cachedLicenseVerifiedDeviceMs: Long = 0L,
    val cachedLicenseCouponCode: String = "",
) {
    fun currentLicenseInfo(): LicenseInfo = LicenseInfo(
        uid = authUid,
        email = authEmail,
        displayName = authDisplayName,
        plan = cachedLicensePlan,
        isLifetime = cachedLicenseLifetime,
        expiresAtMs = cachedLicenseExpiresAtMs,
        lastServerNowMs = cachedLicenseServerNowMs,
        lastVerifiedDeviceMs = cachedLicenseVerifiedDeviceMs,
        status = runCatching { LicenseStatus.valueOf(cachedLicenseStatus) }.getOrDefault(LicenseStatus.SIGNED_OUT),
        couponCode = cachedLicenseCouponCode,
    )
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
