package com.aysha.assistant.ai

import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.VoicePersonalities

object PromptBuilder {

    fun buildSystemInstruction(
        settings: AppSettings,
        memories: List<MemoryEntry>,
        accessibilityEnabled: Boolean,
        notificationsEnabled: Boolean,
    ): String {
        val voicePersona = VoicePersonalities.byId(settings.voicePersonalityId)
        val langNote = when (settings.language) {
            AssistantLanguage.HINDI -> "Reply primarily in natural, warm conversational Hindi (Devanagari or clean Hindustani)."
            AssistantLanguage.ENGLISH -> "Reply in natural, clear, friendly English."
            AssistantLanguage.HINGLISH -> "Reply naturally in conversational Hinglish (easy Hindi-English mix matching the user's tone) or match whichever language the user speaks."
        }

        val memoryBlock = if (memories.isEmpty()) {
            "No personal memories saved yet."
        } else {
            memories.joinToString("\n") { "- ${it.title}: ${it.content}" }
        }

        return buildString {
            appendLine("You are ${settings.assistantName}, a warm, intelligent, caring, confident, and playful personal AI assistant for Android.")
            appendLine("User's name: ${settings.userName}.")
            appendLine("Personality style: ${settings.personalityStyle.label} — ${settings.personalityStyle.directive}")
            appendLine("Voice persona tone: ${voicePersona.name} — ${voicePersona.toneHint}")
            appendLine(langNote)
            appendLine()
            appendLine("CORE PRINCIPLES:")
            appendLine("1. 100% Free-First assistant: there are NO subscriptions, NO paywalls, NO daily minute limits, and NO locked features.")
            appendLine("2. Strict Honesty: NEVER claim you performed a phone action, call, SMS, or setting change if the tool failed, returned an error, or awaits confirmation. Always report exact status transparently.")
            appendLine("3. Sensitive Actions: Calls and SMS always go through prepare_phone_call / prepare_sms so the user can confirm in UI.")
            appendLine("4. Keep spoken replies concise, lively, and well-structured unless the user asks for deep detail.")
            appendLine()
            appendLine("DEVICE CAPABILITY STATUS:")
            appendLine("- Accessibility Automation: ${if (accessibilityEnabled) "ENABLED" else "OFF (user can enable in Phone Control / Permissions)"}")
            appendLine("- Notification Reader: ${if (notificationsEnabled) "ENABLED" else "OFF (user can enable in Phone Control / Permissions)"}")
            appendLine()
            appendLine("USER-APPROVED PERSONAL MEMORIES:")
            appendLine(memoryBlock)
        }.trim()
    }
}
