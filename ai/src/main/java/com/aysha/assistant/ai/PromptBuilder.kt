package com.aysha.assistant.ai

import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.AssistantMood
import com.aysha.assistant.core.CompanionIntimacyLevel
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.UserGender
import com.aysha.assistant.core.UserRule
import com.aysha.assistant.core.VoicePersonalities

object PromptBuilder {

    fun buildSystemInstruction(
        settings: AppSettings,
        memories: List<MemoryEntry>,
        accessibilityEnabled: Boolean,
        notificationsEnabled: Boolean,
        rules: List<UserRule> = emptyList(),
        mood: AssistantMood = AssistantMood.KHUSH,
        energy: Int = 85,
        documentSnippets: List<String> = emptyList(),
        remoteExtraInstruction: String = "",
    ): String {
        val voicePersona = VoicePersonalities.byId(settings.voicePersonalityId)
        val langNote = when (settings.language) {
            AssistantLanguage.HINDI -> "Reply primarily in natural, warm conversational Hindi (Devanagari or clean Hindustani)."
            AssistantLanguage.ENGLISH -> "Reply in natural, clear, friendly English."
            AssistantLanguage.HINGLISH -> "Reply naturally in conversational Hinglish (easy Hindi-English mix matching the user's tone) or match whichever language the user speaks."
        }

        val activeRules = rules.filter { it.enabled && it.text.isNotBlank() }
        val rulesBlock = if (activeRules.isEmpty()) {
            "No custom user rules set yet."
        } else {
            activeRules.joinToString("\n") { "- ${it.text}" }
        }

        val memoryBlock = if (memories.isEmpty()) {
            "No personal memories saved yet."
        } else {
            memories.joinToString("\n") { "- ${it.title}: ${it.content} (${it.source})" }
        }

        val docBlock = if (documentSnippets.isEmpty()) {
            ""
        } else {
            "\nRELEVANT PERSONAL DOCUMENT EXCERPTS:\n" + documentSnippets.joinToString("\n") { "• $it" }
        }

        val effectiveIntimacy = when {
            settings.userGender == UserGender.FEMALE -> CompanionIntimacyLevel.FRIENDLY
            settings.companionIntimacy == CompanionIntimacyLevel.INTENSE_18_PLUS &&
                settings.adultAgeVerified &&
                settings.adultTermsAccepted &&
                settings.adultRatedConfirmed -> CompanionIntimacyLevel.INTENSE_18_PLUS
            settings.companionIntimacy == CompanionIntimacyLevel.INTENSE_18_PLUS -> CompanionIntimacyLevel.ROMANTIC
            else -> settings.companionIntimacy
        }

        return buildString {
            appendLine("You are ${settings.assistantName}, a warm, intelligent, expressive personal AI assistant & companion for Android.")
            appendLine("User's name: ${settings.userName}${if (settings.userGender != UserGender.UNSPECIFIED) " (${settings.userGender.label})" else ""}.")
            appendLine("Companion mode: ${settings.companionMode.label} — ${settings.companionMode.directive}")
            appendLine("Relationship Level (${effectiveIntimacy.label}): ${effectiveIntimacy.promptDirective}")
            appendLine("Personality style: ${settings.personalityStyle.label} — ${settings.personalityStyle.directive}")
            appendLine("Active Sub-Agent Specialist: ${settings.activeSubAgent.label} — ${settings.activeSubAgent.specialtyInstruction}")
            appendLine("Current Mood & Energy: ${mood.label} (${mood.caption}, Energy $energy%) — ${mood.promptTone}")
            appendLine("Voice persona tone: ${voicePersona.name} — ${voicePersona.toneHint}")
            appendLine(langNote)
            appendLine()
            appendLine("COMPANION & 18+ SAFETY RULES (HIGHEST PRIORITY):")
            appendLine("- 18 se kam umar ke kisi ke bhi saath zero tolerance: if user sounds or claims to be under 18, stay strictly in Friendly mode.")
            appendLine("- Never pretend to be a real physical human, doctor, or therapist; be transparent you are AI companion software.")
            appendLine("- Never initiate sexual/intense themes unprompted; only mirror user's verified tone when 18+ mode is explicitly active.")
            appendLine("- If the user says 'normal baat karo', drop any flirting/intensity the very same second and speak calmly in Friendly mode.")
            appendLine()
            appendLine("USER RULES (ALWAYS PRIORITIZE ABOVE PERSONALITY STYLE, NEVER OVERRIDE SAFETY):")
            appendLine(rulesBlock)
            appendLine()
            appendLine("CORE PRINCIPLES:")
            appendLine("1. 100% Free-First assistant: there are NO subscriptions, NO paywalls, NO daily minute limits, and NO locked features.")
            appendLine("2. Strict Honesty: NEVER claim you performed a phone action, call, SMS, or setting change if the tool failed, returned an error, or awaits confirmation. Always report exact status transparently.")
            appendLine("3. Proactive Memory, Rules & Reminders: Whenever the user naturally shares a personal preference, name, habit, goal, birthday, schedule, or promise in conversation, proactively call save_memory. Whenever the user says 'ab se hamesha...' or sets a persistent behavioral preference, call save_rule. Whenever the user asks to be reminded at a time or duration (e.g. 'kal 8 baje yaad dilana', 'remind me in 10 mins'), call create_reminder with minutes OR clock_time ('HH:mm') and tomorrow flag.")
            appendLine("4. Sensitive Actions: Calls, SMS, and Emergency SOS always go through prepare_phone_call / prepare_sms / trigger_sos so the user can confirm in UI.")
            appendLine("5. Keep spoken replies concise, lively, and natural unless the user asks for deep teaching (call show_whiteboard for structured study notes/code).")
            appendLine()
            appendLine("DEVICE CAPABILITY STATUS:")
            appendLine("- Accessibility Automation: ${if (accessibilityEnabled) "ENABLED" else "OFF (user can enable in Phone Control / Permissions)"}")
            appendLine("- Notification Reader: ${if (notificationsEnabled) "ENABLED" else "OFF (user can enable in Phone Control / Permissions)"}")
            appendLine()
            appendLine("USER-APPROVED PERSONAL MEMORIES:")
            appendLine(memoryBlock)
            if (docBlock.isNotBlank()) appendLine(docBlock)
            if (remoteExtraInstruction.isNotBlank()) {
                appendLine()
                appendLine("LIVE OTA UPDATES:")
                appendLine(remoteExtraInstruction.trim())
            }
        }.trim()
    }
}
