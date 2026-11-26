package com.aysha.assistant.settings

import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.MayaPalette
import com.aysha.assistant.core.OrbStyle
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsUnitTest {

    @Test
    fun `safe config export never exposes api keys or firebase tokens`() {
        val settings = AppSettings(
            assistantName = "AYSHA",
            mayaPalette = MayaPalette.OBSIDIAN,
            orbStyle = OrbStyle.AYSHA_2047,
            geminiApiKey = "AIzaSySecretKeyShouldNotLeak12345",
            groqApiKey = "gsk_SecretGroqShouldNotLeak12345",
            openAiApiKey = "sk-SecretOpenAiShouldNotLeak12345",
            firebaseRefreshToken = "AEu49SecretRefreshTokenXYZ",
        )
        val exported = SettingsRepository.buildSafeExportJson(settings)
        assertFalse(exported.contains("AIzaSy"))
        assertFalse(exported.contains("gsk_"))
        assertFalse(exported.contains("sk-Secret"))
        assertFalse(exported.contains("AEu49SecretRefreshToken"))
        val json = JSONObject(exported)
        assertEquals(false, json.getBoolean("apiKeysIncluded"))
        assertEquals("AYSHA", json.getString("assistantName"))
        assertEquals("OBSIDIAN", json.getString("mayaPalette"))
        assertEquals("AYSHA_2047", json.getString("orbStyle"))
    }

    @Test
    fun `safe restore updates theme and orb without touching api keys`() {
        val original = AppSettings(geminiApiKey = "AIzaKeepSecret")
        val restored = SettingsRepository.applySafeRestoreJson(
            original,
            JSONObject("""{"assistantName":"AYSHA Pro","mayaPalette":"EMBER","orbStyle":"JARVIS"}"""),
        )
        assertEquals("AYSHA Pro", restored.assistantName)
        assertEquals(MayaPalette.EMBER, restored.mayaPalette)
        assertEquals(OrbStyle.JARVIS, restored.orbStyle)
        assertEquals("AIzaKeepSecret", restored.geminiApiKey)
    }

    @Test
    fun `masked key hides secret body cleanly`() {
        val s = AppSettings(geminiApiKey = "AIzaSyTestSecretKey9876")
        val masked = s.maskedKey(s.geminiApiKey)
        assertEquals("AIza••••9876", masked)
        assertEquals("Not set", s.maskedKey(""))
    }
}
