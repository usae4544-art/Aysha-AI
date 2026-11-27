package com.aysha.assistant

import com.aysha.assistant.core.AdminUserRecord
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
import com.aysha.assistant.core.StorePricingConfig
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
        assertEquals(ThemeMode.LIGHT, defaults.themeMode)
        assertEquals(MayaPalette.CLAY_DAY, defaults.mayaPalette)
        assertEquals(OrbStyle.RED_HUD, defaults.orbStyle)
        assertEquals(OrbRenderMode.HUD_ORB, defaults.orbRenderMode)
        assertEquals(CompanionIntimacyLevel.FRIENDLY, defaults.companionIntimacy)
        assertEquals(UserGender.UNSPECIFIED, defaults.userGender)
        assertFalse(defaults.adultAgeVerified)
        assertEquals(SurfaceStyle.CLAY, defaults.surfaceStyle)
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
        assertTrue(names.contains("Ananya Velvet"))
        assertTrue(names.contains("Aarav Confident"))
        assertTrue(names.contains("Kabir Warm"))
        assertTrue(names.contains("Rohan Upbeat"))
        assertTrue(VoicePersonalities.all.size >= 18)
        assertTrue(VoicePersonalities.all.all { it.geminiLiveVoice.isNotBlank() })
        assertEquals(VoicePersonalities.all.size, VoicePersonalities.all.map { it.id }.distinct().size)
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

    @Test
    fun `blocked account fails closed even with active or lifetime license`() {
        val now = 1_800_000_000_000L
        val blockedFlagLic = LicenseInfo(
            uid = "u_blocked",
            email = "blocked@example.com",
            plan = LicensePlan.LIFETIME,
            isLifetime = true,
            expiresAtMs = now + 1_000_000_000L,
            status = LicenseStatus.ACTIVE,
            blocked = true,
            blockedReason = "Suspended by admin",
        )
        assertFalse(blockedFlagLic.isUnlocked(now))
        assertEquals(0L, blockedFlagLic.remainingMs(now))

        val blockedStatusLic = LicenseInfo(
            uid = "u_blocked2",
            email = "blocked2@example.com",
            plan = LicensePlan.ONE_YEAR,
            isLifetime = true,
            status = LicenseStatus.BLOCKED,
        )
        assertFalse(blockedStatusLic.isUnlocked(now))
        assertEquals(0L, blockedStatusLic.remainingMs(now))
        assertFalse(AppSettings().cachedAccountBlocked)
    }

    @Test
    fun `store pricing config defaults and admin user record hold expected values`() {
        val pricing = StorePricingConfig()
        assertEquals(199, pricing.priceFor(LicensePlan.ONE_MONTH))
        assertEquals(349, pricing.priceFor(LicensePlan.TWO_MONTHS))
        assertEquals(999, pricing.priceFor(LicensePlan.ONE_YEAR))
        assertEquals(1599, pricing.priceFor(LicensePlan.TWO_YEARS))
        assertEquals(2999, pricing.priceFor(LicensePlan.LIFETIME))

        val rec = AdminUserRecord(
            uid = "uid123",
            email = "user@test.com",
            displayName = "Test User",
            lastLoginSource = "android",
            blocked = true,
        )
        assertEquals("android", rec.lastLoginSource)
        assertTrue(rec.blocked)
    }
}
