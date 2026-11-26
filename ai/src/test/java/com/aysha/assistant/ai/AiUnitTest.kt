package com.aysha.assistant.ai

import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.AssistantMood
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.SubAgentRole
import com.aysha.assistant.core.UserRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiUnitTest {

    @Test
    fun `tool registry emits valid gemini and openai schemas`() {
        val gemini = ToolRegistry.toGeminiFunctionDeclarations()
        val openai = ToolRegistry.toOpenAiTools()
        assertEquals(ToolRegistry.allTools.size, gemini.length())
        assertEquals(ToolRegistry.allTools.size, openai.length())
        assertEquals("function", openai.getJSONObject(0).getString("type"))
    }

    @Test
    fun `offline command parser resolves core commands and rules without keys`() {
        val math = OfflineCommandParser.parse("25 * 48 kitna hota hai") as OfflineResolution.DirectReply
        assertTrue(math.reply.contains("1200"))

        val timer = OfflineCommandParser.parse("5 minute ka timer lagao") as OfflineResolution.InAppTimer
        assertEquals(300, timer.seconds)

        val torch = OfflineCommandParser.parse("Torch on karo") as OfflineResolution.ToolCall
        assertEquals("set_torch", torch.toolName)
        assertEquals(true, torch.args.getBoolean("enabled"))

        val study = OfflineCommandParser.parse("Aysha, turn on my study mode") as OfflineResolution.ToolCall
        assertEquals("run_macro", study.toolName)
        assertEquals("Study Mode", study.args.getString("name"))

        val call = OfflineCommandParser.parse("Call Rahul") as OfflineResolution.ToolCall
        assertEquals("prepare_phone_call", call.toolName)

        val rule = OfflineCommandParser.parse("Ab se hamesha chhote jawab dena") as OfflineResolution.ToolCall
        assertEquals("save_rule", rule.toolName)
        assertTrue(rule.args.getString("rule").contains("chhote jawab", ignoreCase = true))

        val sos = OfflineCommandParser.parse("emergency sos") as OfflineResolution.ToolCall
        assertEquals("trigger_sos", sos.toolName)
    }

    @Test
    fun `provider fallback chain builds configured providers ending with offline`() {
        val settings = AppSettings(
            activeProvider = BrainProvider.GEMINI,
            geminiApiKey = "AIzaTest1234",
            groqApiKey = "gsk_Test1234",
            fallbackEnabled = true,
            offlineOnlyMode = false,
        )
        val client = MultiProviderAiClient()
        val chain = client.buildProviderChain(settings)
        assertEquals(listOf(BrainProvider.GEMINI, BrainProvider.GROQ, BrainProvider.LOCAL_MODEL, BrainProvider.OFFLINE), chain)

        val offlineOnly = client.buildProviderChain(settings.copy(offlineOnlyMode = true))
        assertEquals(listOf(BrainProvider.OFFLINE), offlineOnly)
    }

    @Test
    fun `prompt builder includes AYSHA identity, rules, subagent and excludes paid locks`() {
        val prompt = PromptBuilder.buildSystemInstruction(
            settings = AppSettings(
                assistantName = "AYSHA",
                userName = "Aman",
                language = AssistantLanguage.HINGLISH,
                activeSubAgent = SubAgentRole.CODING,
            ),
            memories = listOf(MemoryEntry(1, "City", "Jaipur")),
            accessibilityEnabled = true,
            notificationsEnabled = false,
            rules = listOf(UserRule(1, "Keep replies crisp and short")),
            mood = AssistantMood.KHUSH,
            energy = 92,
            documentSnippets = listOf("[Resume] Android & Kotlin developer"),
        )
        assertTrue(prompt.contains("AYSHA"))
        assertTrue(prompt.contains("Aman"))
        assertTrue(prompt.contains("Jaipur"))
        assertTrue(prompt.contains("Keep replies crisp and short"))
        assertTrue(prompt.contains("Coding & Tech"))
        assertTrue(prompt.contains("Android & Kotlin developer"))
        assertTrue(prompt.contains("NO subscriptions"))
        assertFalse(prompt.contains("Maya"))
    }

    @Test
    fun `smart memory extractor detects natural facts, rules, mood shifts and hindi reminders`() {
        val facts = SmartMemoryExtractor.extractAutoFacts("Mera naam Aman hai aur mujhe cold coffee pasand hai")
        assertTrue(facts.any { it.content.contains("Aman") })

        val rule = SmartMemoryExtractor.extractAutoRule("Always reply in 2 lines")
        assertNotNull(rule)
        assertTrue(rule!!.contains("2 lines"))

        val (warmMood, warmEnergy) = SmartMemoryExtractor.analyzeMoodShift("Thank you, you are the best!", AssistantMood.ROOTHI, 60)
        assertEquals(AssistantMood.KHUSH, warmMood)
        assertTrue(warmEnergy > 60)

        val (sulkyMood, sulkyEnergy) = SmartMemoryExtractor.analyzeMoodShift("You are stupid and useless", AssistantMood.KHUSH, 80)
        assertEquals(AssistantMood.ROOTHI, sulkyMood)
        assertTrue(sulkyEnergy < 80)

        val rem = SmartMemoryExtractor.extractNaturalReminder("Kal subah 8 baje gym jana yaad dilana")!!
        assertEquals("08:00", rem.clockTime24h)
        assertTrue(rem.tomorrow)
        assertTrue(rem.message.contains("gym", ignoreCase = true))

        val relRem = SmartMemoryExtractor.extractNaturalReminder("15 minute baad pani peene ka reminder lagao")!!
        assertEquals(15, relRem.minutesFromNow)
        assertEquals("", relRem.clockTime24h)
    }

    @Test
    fun `license manager parses oauth callback deep link and firestore license documents`() {
        val parsed = AyshaLicenseManager.parseDeepLinkCallback(
            "aysha://auth/callback?refreshToken=AEu49TestToken&uid=uid123&email=user%40gmail.com",
        )
        assertNotNull(parsed)
        assertEquals("AEu49TestToken", parsed!!.refreshToken)
        assertEquals("uid123", parsed.uid)
        assertEquals("user@gmail.com", parsed.email)

        assertEquals(null, AyshaLicenseManager.parseDeepLinkCallback("https://example.com/?refreshToken=abc"))
        assertEquals(null, AyshaLicenseManager.parseDeepLinkCallback("aysha://auth/callback?uid=only"))

        val serverNow = 1_800_000_000_000L
        val futureJson = """
            {
              "fields": {
                "planId": { "stringValue": "TWO_MONTHS" },
                "status": { "stringValue": "active" },
                "isLifetime": { "booleanValue": false },
                "expiresAtMs": { "integerValue": "${serverNow + 86_400_000L}" },
                "couponCode": { "stringValue": "FESTIVE" }
              }
            }
        """.trimIndent()
        val activeLic = AyshaLicenseManager.parseFirestoreLicenseDocument(
            jsonStr = futureJson,
            uid = "uid123",
            email = "user@gmail.com",
            displayName = "Test User",
            serverNowMs = serverNow,
            deviceNowMs = serverNow,
        )
        assertEquals(LicenseStatus.ACTIVE, activeLic.status)
        assertEquals(LicensePlan.TWO_MONTHS, activeLic.plan)
        assertEquals("FESTIVE", activeLic.couponCode)
        assertTrue(activeLic.isUnlocked(serverNow))

        val revokedJson = """
            {
              "fields": {
                "planId": { "stringValue": "LIFETIME" },
                "status": { "stringValue": "revoked" },
                "isLifetime": { "booleanValue": true }
              }
            }
        """.trimIndent()
        val revokedLic = AyshaLicenseManager.parseFirestoreLicenseDocument(
            jsonStr = revokedJson,
            uid = "uid123",
            email = "user@gmail.com",
            displayName = "Test User",
            serverNowMs = serverNow,
            deviceNowMs = serverNow,
        )
        assertEquals(LicenseStatus.REVOKED, revokedLic.status)
        assertFalse(revokedLic.isUnlocked(serverNow))

        val dateMs = AyshaLicenseManager.parseHttpServerDateMs("Thu, 26 Nov 2026 17:37:49 GMT")
        assertTrue(dateMs > 1_700_000_000_000L)
    }
}
