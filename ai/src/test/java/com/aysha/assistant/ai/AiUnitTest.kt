package com.aysha.assistant.ai

import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.MemoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `offline command parser resolves core commands without keys`() {
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
    fun `prompt builder includes AYSHA identity and excludes paid locks`() {
        val prompt = PromptBuilder.buildSystemInstruction(
            settings = AppSettings(assistantName = "AYSHA", userName = "Aman", language = AssistantLanguage.HINGLISH),
            memories = listOf(MemoryEntry(1, "City", "Jaipur")),
            accessibilityEnabled = true,
            notificationsEnabled = false,
        )
        assertTrue(prompt.contains("AYSHA"))
        assertTrue(prompt.contains("Aman"))
        assertTrue(prompt.contains("Jaipur"))
        assertTrue(prompt.contains("NO subscriptions"))
        assertFalse(prompt.contains("Maya"))
    }
}
