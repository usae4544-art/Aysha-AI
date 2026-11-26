package com.aysha.assistant.settings

import com.aysha.assistant.core.AppSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsUnitTest {

    @Test
    fun `safe config export never exposes api keys`() {
        val settings = AppSettings(
            assistantName = "AYSHA",
            geminiApiKey = "AIzaSySecretKeyShouldNotLeak12345",
            groqApiKey = "gsk_SecretGroqShouldNotLeak12345",
            openAiApiKey = "sk-SecretOpenAiShouldNotLeak12345",
        )
        val exported = SettingsRepository.buildSafeExportJson(settings)
        assertFalse(exported.contains("AIzaSy"))
        assertFalse(exported.contains("gsk_"))
        assertFalse(exported.contains("sk-Secret"))
        val json = JSONObject(exported)
        assertEquals(false, json.getBoolean("apiKeysIncluded"))
        assertEquals("AYSHA", json.getString("assistantName"))
    }

    @Test
    fun `masked key hides secret body cleanly`() {
        val s = AppSettings(geminiApiKey = "AIzaSyTestSecretKey9876")
        val masked = s.maskedKey(s.geminiApiKey)
        assertEquals("AIza••••9876", masked)
        assertEquals("Not set", s.maskedKey(""))
    }
}
