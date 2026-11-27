package com.aysha.assistant.ai

import com.aysha.assistant.core.AdultModeGuard
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.AssistantMood
import com.aysha.assistant.core.BirthYearValidation
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.CompanionIntimacyLevel
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.SubAgentRole
import com.aysha.assistant.core.UserGender
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
    fun `provider fallback chain prioritizes Gemini and excludes offline and local mode`() {
        val settings = AppSettings(
            activeProvider = BrainProvider.GEMINI,
            geminiApiKey = "AIzaTest1234",
            groqApiKey = "gsk_Test1234",
            fallbackEnabled = true,
            offlineOnlyMode = false,
        )
        val client = MultiProviderAiClient()
        val chain = client.buildProviderChain(settings)
        assertEquals(listOf(BrainProvider.GEMINI, BrainProvider.GROQ), chain)

        val evenIfOldFlag = client.buildProviderChain(settings.copy(offlineOnlyMode = true))
        assertEquals(listOf(BrainProvider.GEMINI, BrainProvider.GROQ), evenIfOldFlag)
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
                "couponCode": { "stringValue": "FESTIVE" },
                "licenseKey": { "stringValue": "AYSHA-9F4K-7M2P-8X3Q-W5R9-V2D6-K8T4" }
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
        assertEquals("AYSHA-9F4K-7M2P-8X3Q-W5R9-V2D6-K8T4", activeLic.licenseKey)
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

    @Test
    fun `adult birth year validation, normal reset phrase, and prompt intimacy safety`() {
        assertTrue(AdultModeGuard.validateBirthYear("", currentYear = 2026) is BirthYearValidation.Invalid)
        assertTrue(AdultModeGuard.validateBirthYear("19", currentYear = 2026) is BirthYearValidation.Invalid)
        assertTrue(AdultModeGuard.validateBirthYear("2012", currentYear = 2026) is BirthYearValidation.Invalid)
        val ok = AdultModeGuard.validateBirthYear("1998", currentYear = 2026) as BirthYearValidation.Valid
        assertEquals(1998, ok.birthYear)
        assertEquals(28, ok.estimatedAge)

        assertTrue(AdultModeGuard.isNormalResetPhrase("normal baat karo"))
        assertTrue(AdultModeGuard.isNormalResetPhrase("Hey Aysha please normal baat karo abhi"))
        assertFalse(AdultModeGuard.isNormalResetPhrase("kaisa mausam hai aaj"))

        // Unverified Intense 18+ automatically falls back to Romantic, and Female preference locks to Friendly
        val unverifiedPrompt = PromptBuilder.buildSystemInstruction(
            settings = AppSettings(
                companionIntimacy = CompanionIntimacyLevel.INTENSE_18_PLUS,
                adultAgeVerified = false,
            ),
            memories = emptyList(),
            accessibilityEnabled = false,
            notificationsEnabled = false,
        )
        assertTrue(unverifiedPrompt.contains("Relationship Level (Romantic)"))
        assertFalse(unverifiedPrompt.contains("Maya"))

        val femalePrompt = PromptBuilder.buildSystemInstruction(
            settings = AppSettings(
                companionIntimacy = CompanionIntimacyLevel.INTENSE_18_PLUS,
                userGender = UserGender.FEMALE,
                adultAgeVerified = true,
                adultTermsAccepted = true,
                adultRatedConfirmed = true,
            ),
            memories = emptyList(),
            accessibilityEnabled = false,
            notificationsEnabled = false,
        )
        assertTrue(femalePrompt.contains("Relationship Level (Friendly)"))
        assertFalse(femalePrompt.contains("Maya"))
    }

    @Test
    fun `remote config version comparison and prompt extra work`() {
        assertTrue(RemoteConfigManager.isNewerVersion("2.5.0", "2.4.0"))
        assertTrue(RemoteConfigManager.isNewerVersion("v2.5.1", "2.5.0"))
        assertFalse(RemoteConfigManager.isNewerVersion("2.5.0", "2.5.0"))
        assertFalse(RemoteConfigManager.isNewerVersion("2.4.0", "2.5.0"))

        val withOta = PromptBuilder.buildSystemInstruction(
            settings = AppSettings(),
            memories = emptyList(),
            accessibilityEnabled = false,
            notificationsEnabled = false,
            remoteExtraInstruction = "Always wish the user a happy festival today.",
        )
        assertTrue(withOta.contains("LIVE OTA UPDATES"))
        assertTrue(withOta.contains("happy festival today"))
    }

    @Test
    fun `gemini tool declarations omit parameters for zero-arg tools`() {
        val decls = ToolRegistry.toGeminiFunctionDeclarations()
        var checkedOpenCamera = false
        for (i in 0 until decls.length()) {
            val fn = decls.getJSONObject(i)
            if (fn.getString("name") == "open_camera") {
                checkedOpenCamera = true
                assertFalse(fn.has("parameters"))
            }
            if (fn.has("parameters")) {
                val props = fn.getJSONObject("parameters").getJSONObject("properties")
                assertTrue(props.length() > 0)
            }
        }
        assertTrue(checkedOpenCamera)
    }

    @Test
    fun `gemini candidate models prioritize low-demand flash-lite and retry 503 overload errors`() {
        val client = MultiProviderAiClient()

        // Legacy congested default should put gemini-2.5-flash-lite first ahead of gemini-2.5-flash
        val fromLegacy = client.buildGeminiCandidateModels("gemini-2.5-flash")
        assertEquals("gemini-2.5-flash-lite", fromLegacy.first())
        assertTrue(fromLegacy.indexOf("gemini-2.5-flash-lite") < fromLegacy.indexOf("gemini-2.5-flash"))
        assertTrue(fromLegacy.contains("gemini-3.5-flash-lite"))
        assertTrue(fromLegacy.contains("gemini-3.1-flash-lite"))

        // Custom selected model stays primary while keeping lite fallbacks behind it
        val fromCustom = client.buildGeminiCandidateModels("gemini-3.5-flash-lite")
        assertEquals("gemini-3.5-flash-lite", fromCustom.first())
        assertTrue(fromCustom.contains("gemini-2.5-flash-lite"))

        // 503 / high demand / 429 / 500 must trigger failover, whereas 401 auth failure stays terminal
        assertTrue(client.isRetryableGeminiError(503, "This model is currently experiencing high demand."))
        assertTrue(client.isRetryableGeminiError(429, "Quota exceeded"))
        assertTrue(client.isRetryableGeminiError(500, "Internal error"))
        assertTrue(client.isRetryableGeminiError(400, "Service unavailable right now, try again"))
        assertFalse(client.isRetryableGeminiError(401, "API key not valid"))
    }
}
