package com.aysha.assistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.aysha.assistant.R
import com.aysha.assistant.core.PersonalityStyle
import com.aysha.assistant.core.ui.GlassCard
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
    ABOUT,
}

private data class SettingsMenuEntry(
    val route: SettingsSubRoute,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val iconBg: Color,
)

@Composable
fun SettingsHubScreen(
    viewModel: AyshaViewModel,
    onNavigate: (SettingsSubRoute) -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    var search by remember { mutableStateOf("") }
    var showProfileDialog by remember { mutableStateOf(false) }

    val items = listOf(
        SettingsMenuEntry(SettingsSubRoute.AI_PROVIDERS, "AI Provider Settings", "Gemini, Groq, OpenAI, Local & Offline", Icons.Rounded.Psychology, Color(0xFF7C3AED)),
        SettingsMenuEntry(SettingsSubRoute.VOICE_LANGUAGE, "Voice & Language", "Hindi, English, Hinglish • 6 Personalities", Icons.Rounded.GraphicEq, Color(0xFFF59E0B)),
        SettingsMenuEntry(SettingsSubRoute.APPEARANCE, "Appearance", "Dark / Light Liquid Glass • Accent Colors", Icons.Rounded.ColorLens, Color(0xFF10B981)),
        SettingsMenuEntry(SettingsSubRoute.WAKE_WORD, "Wake Word", settings.wakeWordsCsv, Icons.Rounded.Mic, Color(0xFF0EA5E9)),
        SettingsMenuEntry(SettingsSubRoute.AUTOMATION, "Automation", "Custom Voice Commands & Workflows", Icons.Rounded.Bolt, Color(0xFFEC4899)),
        SettingsMenuEntry(SettingsSubRoute.MEMORY_HISTORY, "Memory & History", "Chats, Personal Memory & Export", Icons.Rounded.History, Color(0xFF6366F1)),
        SettingsMenuEntry(SettingsSubRoute.PHONE_CONTROL, "Phone Control & Permissions", "Camera, Calls, SMS, Alarms & Apps", Icons.Rounded.PhoneAndroid, Color(0xFF14B8A6)),
        SettingsMenuEntry(SettingsSubRoute.PRIVACY_SECURITY, "Privacy & Security", "Keystore Vault, Permissions & Data", Icons.Rounded.Lock, Color(0xFF8B5CF6)),
        SettingsMenuEntry(SettingsSubRoute.ABOUT, "About AYSHA", "Version 1.0.0 • Free & Open Architecture", Icons.Rounded.Info, Color(0xFF3B82F6)),
    )

    val filtered = items.filter {
        search.isBlank() ||
            it.title.contains(search, ignoreCase = true) ||
            it.subtitle.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", color = c.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Profile Banner (Editable Assistant Name & Personality)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevated = true,
            onClick = { showProfileDialog = true },
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.aysha_portrait),
                    contentDescription = "AYSHA",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(54.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(settings.assistantName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        "Companion for ${settings.userName} · ${settings.personalityStyle.label}",
                        color = c.textSecondary,
                        fontSize = 12.5.sp,
                    )
                }
                Icon(Icons.Rounded.Edit, contentDescription = "Edit Profile", tint = c.accentPrimary)
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search settings...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        filtered.forEach { entry ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                onClick = { onNavigate(entry.route) },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(entry.iconBg.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(entry.icon, contentDescription = null, tint = entry.iconBg, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(entry.subtitle, color = c.textSecondary, fontSize = 12.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
                }
            }
        }
    }

    if (showProfileDialog) {
        var nameInput by remember { mutableStateOf(settings.assistantName) }
        var userInput by remember { mutableStateOf(settings.userName) }
        var styleInput by remember { mutableStateOf(settings.personalityStyle) }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Assistant Identity & Personality") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Assistant Name") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                    )
                    Text("Personality Style", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    PersonalityStyle.entries.forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (style == styleInput) c.accentPrimary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { styleInput = style }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(style.label, fontSize = 13.sp)
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
                        )
                    }
                    showProfileDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("Cancel") }
            },
        )
    }
}
