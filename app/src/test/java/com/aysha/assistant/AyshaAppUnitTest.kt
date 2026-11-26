package com.aysha.assistant

import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.VoicePersonalities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AyshaAppUnitTest {

    @Test
    fun `default settings are 100 percent free first and named AYSHA`() {
        val defaults = AppSettings()
        assertEquals("AYSHA", defaults.assistantName)
        assertTrue(defaults.fallbackEnabled)
        assertEquals(ThemeMode.DARK, defaults.themeMode)
        assertEquals(AccentPalette.COSMIC_CYAN, defaults.accentPalette)
        assertEquals(BrainProvider.GEMINI, defaults.activeProvider)
    }

    @Test
    fun `voice personalities include both female and male personas`() {
        val names = VoicePersonalities.all.map { it.name }
        assertTrue(names.contains("Aysha Natural"))
        assertTrue(names.contains("Sara Friendly"))
        assertTrue(names.contains("Aarav Confident"))
        assertTrue(names.contains("Kabir Warm"))
        assertEquals("aysha_natural", VoicePersonalities.byId("unknown").id)
    }

    @Test
    fun `orb states include IDLE LISTENING PROCESSING SPEAKING`() {
        val states = OrbState.entries.map { it.name }
        assertTrue(states.containsAll(listOf("IDLE", "LISTENING", "PROCESSING", "SPEAKING")))
    }
}
