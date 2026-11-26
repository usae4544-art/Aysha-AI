package com.aysha.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.BuildConfig
import com.aysha.assistant.R
import com.aysha.assistant.core.CompanionMode
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.PersonalityStyle
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

enum class SettingsSubRoute {
    HUB,
    AI_PROVIDERS,
    VOICE_LANGUAGE,
    APPEARANCE,
    WAKE_WORD,
    AUTOMATION,
    MEMORY_HISTORY,
    PHONE_CONTROL,
    PRIVACY_SECURITY,
    RULES_AGENTS,
    SCANNER,
    WHITEBOARD,
    BROWSER,
    LICENSES,
    ABOUT,
}

private data class SettingsMenuEntry(
    val category: String,
    val route: SettingsSubRoute,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
)

@Composable
fun SettingsHubScreen(
    viewModel: AyshaViewModel,
    onBackToOrb: () -> Unit,
    onNavigate: (SettingsSubRoute) -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val license by viewModel.licenseInfo.collectAsState()
    val mood by viewModel.assistantMood.collectAsState()
    var showSearch by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }

    val items = listOf(
        SettingsMenuEntry("Kaam-kaaj", SettingsSubRoute.RULES_AGENTS, "Rules, Sub-Agents & Mood", "${settings.activeSubAgent.label} · ${mood.label}", Icons.Rounded.Tune, Color(0xFFEC4899)),
        SettingsMenuEntry("Kaam-kaaj", SettingsSubRoute.SCANNER, "Camera Vision Scanner", "\"Kya dikh raha hai?\" visual AI", Icons.Rounded.CameraAlt, Color(0xFF0EA5E9)),
        SettingsMenuEntry("Kaam-kaaj", SettingsSubRoute.WHITEBOARD, "Study Whiteboard", "Step-by-step tutor & code notes", Icons.Rounded.EditNote, Color(0xFF10B981)),
        SettingsMenuEntry("Kaam-kaaj", SettingsSubRoute.BROWSER, "In-App Mini Browser", "Browse & summarize any page", Icons.Rounded.Language, Color(0xFF3B82F6)),
        SettingsMenuEntry("Kaam-kaaj", SettingsSubRoute.AUTOMATION, "Automation & Macros", "Voice workflows & trained tasks", Icons.Rounded.Bolt, Color(0xFFEA580C)),

        SettingsMenuEntry("Brain & Voice", SettingsSubRoute.AI_PROVIDERS, "AI Provider Settings", "Gemini, Groq, OpenAI & Offline", Icons.Rounded.Psychology, Color(0xFF7C3AED)),
        SettingsMenuEntry("Brain & Voice", SettingsSubRoute.VOICE_LANGUAGE, "Voice, Barge-In & Awareness", "Personas, Follow-up & Sensitivity", Icons.Rounded.VolumeUp, Color(0xFFF97316)),
        SettingsMenuEntry("Brain & Voice", SettingsSubRoute.WAKE_WORD, "Wake Word & Decision Log", settings.wakeWordsCsv, Icons.Rounded.Mic, Color(0xFF06B6D4)),
        SettingsMenuEntry("Brain & Voice", SettingsSubRoute.MEMORY_HISTORY, "Memory, Documents & Chats", "Semantic recall, Docs & Purani yaadein", Icons.Rounded.History, Color(0xFF6366F1)),

        SettingsMenuEntry("System & Safety", SettingsSubRoute.APPEARANCE, "Theme, Surface & Orbs", "${settings.mayaPalette.label} · ${settings.orbStyle.label}", Icons.Rounded.ColorLens, Color(0xFF10B981)),
        SettingsMenuEntry("System & Safety", SettingsSubRoute.PHONE_CONTROL, "Phone Control & Floating Orb", "Apps, Calls, SMS & Overlay", Icons.Rounded.PhoneAndroid, Color(0xFF14B8A6)),
        SettingsMenuEntry("System & Safety", SettingsSubRoute.PRIVACY_SECURITY, "Privacy, Backup & SOS", "AES-256 Vault, Restore & Emergency", Icons.Rounded.Lock, Color(0xFF8B5CF6)),
        SettingsMenuEntry("System & Safety", SettingsSubRoute.LICENSES, "Open-Source Licenses", "MIT License & Third-Party Credits", Icons.Rounded.Description, Color(0xFF64748B)),
        SettingsMenuEntry("System & Safety", SettingsSubRoute.ABOUT, "About AYSHA", "Version ${BuildConfig.VERSION_NAME} · MIT Licensed", Icons.Rounded.Info, Color(0xFF2563EB)),
    )

    val filtered = items.filter {
        search.isBlank() ||
            it.title.contains(search, ignoreCase = true) ||
            it.subtitle.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackToOrb) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                }
                Text("AYSHA Hub & Settings", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { showSearch = !showSearch; if (!showSearch) search = "" }) {
                Icon(Icons.Rounded.Search, contentDescription = "Search", tint = c.textPrimary)
            }
        }

        AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search settings or tools...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevated = true,
            onClick = { showProfileDialog = true },
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.aysha_portrait),
                    contentDescription = "AYSHA",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${settings.assistantName} · ${settings.userName}", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("${settings.companionMode.label} · ${settings.personalityStyle.label}", color = c.textSecondary, fontSize = 12.sp)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            contentPadding = PaddingValues(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val licStatusText = when {
                    license.status == LicenseStatus.ADMIN_ACTIVE -> "Owner Admin · All-Time"
                    license.isLifetime -> "${license.plan.label} · Active"
                    else -> {
                        val remDays = (license.remainingMs() / 86_400_000L).coerceAtLeast(0L)
                        val remHours = ((license.remainingMs() % 86_400_000L) / 3_600_000L).coerceAtLeast(0L)
                        "${license.plan.label} · ${remDays}d ${remHours}h left"
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = license.email.ifBlank { "Google Account" },
                            color = c.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                        )
                        Text(licStatusText, color = c.success, fontSize = 12.sp)
                    }
                    GlassPill(text = "Refresh", dotColor = c.accentPrimary) {
                        viewModel.refreshLicenseStatus(silent = false)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassPill(text = "Plans / Coupon", dotColor = c.accentSecondary) {
                        viewModel.openLicensePortalInBrowser()
                    }
                    GlassPill(text = "Sign Out", dotColor = c.error) {
                        viewModel.signOutGoogle()
                    }
                }
            }
        }

        val grouped = filtered.groupBy { it.category }
        grouped.forEach { (cat, entries) ->
            Text(cat, color = c.accentPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            entries.forEach { entry ->
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    onClick = { onNavigate(entry.route) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ColoredIconBadge(icon = entry.icon, tintColor = entry.iconBg, size = 38.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                            Text(entry.subtitle, color = c.textSecondary, fontSize = 12.sp)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Save Conversational Memory", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Automatically extract facts, preferences & rules", color = c.textSecondary, fontSize = 11.5.sp)
                }
                Switch(
                    checked = settings.autoMemoryEnabled,
                    onCheckedChange = { on -> viewModel.updateSettings { it.copy(autoMemoryEnabled = on) } },
                )
            }
        }
    }

    if (showProfileDialog) {
        var nameInput by remember { mutableStateOf(settings.assistantName) }
        var userInput by remember { mutableStateOf(settings.userName) }
        var styleInput by remember { mutableStateOf(settings.personalityStyle) }
        var companionInput by remember { mutableStateOf(settings.companionMode) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Assistant Identity & Companion Mode") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Assistant Name") }, singleLine = true)
                    OutlinedTextField(value = userInput, onValueChange = { userInput = it }, label = { Text("Your Name") }, singleLine = true)
                    Text("Companion Mode", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    CompanionMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (mode == companionInput) c.accentPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { companionInput = mode }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.label, fontSize = 12.5.sp)
                        }
                    }
                    Text("Personality Style", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    PersonalityStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (style == styleInput) c.accentSecondary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { styleInput = style }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(style.label, fontSize = 12.5.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSettings {
                        it.copy(
                            assistantName = nameInput.trim().ifBlank { "AYSHA" },
                            userName = userInput.trim().ifBlank { "Friend" },
                            personalityStyle = styleInput,
                            companionMode = companionInput,
                        )
                    }
                    showProfileDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Cancel") } },
        )
    }
}
