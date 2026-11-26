package com.aysha.assistant

import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AwarenessSensitivity
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.CompanionIntimacyLevel
import com.aysha.assistant.core.LicenseInfo
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.MayaPalette
import com.aysha.assistant.core.OrbRenderMode
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.OrbStyle
import com.aysha.assistant.core.SurfaceStyle
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.UserGender
import com.aysha.assistant.core.VoicePersonalities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AyshaAppUnitTest {

    @Test
    fun `default settings are 100 percent free first and named AYSHA`() {
        val defaults = AppSettings()
        assertEquals("AYSHA", defaults.assistantName)
        assertTrue(defaults.fallbackEnabled)
        assertEquals(ThemeMode.DARK, defaults.themeMode)
        assertEquals(MayaPalette.OBSIDIAN, defaults.mayaPalette)
        assertEquals(OrbStyle.AYSHA_2047, defaults.orbStyle)
        assertEquals(OrbRenderMode.HUD_ORB, defaults.orbRenderMode)
        assertEquals(CompanionIntimacyLevel.FRIENDLY, defaults.companionIntimacy)
        assertEquals(UserGender.UNSPECIFIED, defaults.userGender)
        assertFalse(defaults.adultAgeVerified)
        assertEquals(SurfaceStyle.GLASS, defaults.surfaceStyle)
        assertEquals(AwarenessSensitivity.BALANCED, defaults.awarenessSensitivity)
        assertEquals(AccentPalette.COSMIC_CYAN, defaults.accentPalette)
        assertEquals(BrainProvider.GEMINI, defaults.activeProvider)
    }

    @Test
    fun `voice personalities include both female and male personas`() {
        val names = VoicePersonalities.all.map { it.name }
        assertTrue(names.contains("Aysha Natural"))
        assertTrue(names.contains("Aysha Silk"))
        assertTrue(names.contains("Sara Friendly"))
        assertTrue(names.contains("Aarav Confident"))
        assertTrue(names.contains("Kabir Warm"))
        assertEquals("aysha_natural", VoicePersonalities.byId("unknown").id)
    }

    @Test
    fun `orb states and styles cover full HUD suite`() {
        val states = OrbState.entries.map { it.name }
        assertTrue(states.containsAll(listOf("IDLE", "LISTENING", "PROCESSING", "SPEAKING")))
        assertTrue(OrbStyle.entries.size >= 11)
        assertTrue(MayaPalette.entries.size >= 9)
    }

    @Test
    fun `license clock rollback defense prevents expired bypass`() {
        val tServer = 1_800_000_000_000L
        val tDevice = 1_800_000_000_000L
        val expiredLic = LicenseInfo(
            uid = "u1",
            email = "user@example.com",
            plan = LicensePlan.ONE_MONTH,
            isLifetime = false,
            expiresAtMs = tServer - 10_000L,
            lastServerNowMs = tServer,
            lastVerifiedDeviceMs = tDevice,
            status = LicenseStatus.ACTIVE,
        )
        assertFalse(expiredLic.isUnlocked(tDevice))
        // Even if device clock is rolled back 10 years, server time floor holds
        assertFalse(expiredLic.isUnlocked(tDevice - 300_000_000_000L))

        val adminLic = LicenseInfo(
            uid = "admin1",
            email = "usae4544@gmail.com",
            plan = LicensePlan.LIFETIME,
            isLifetime = true,
            status = LicenseStatus.ADMIN_ACTIVE,
        )
        assertTrue(adminLic.isUnlocked(tDevice))
    }
}
