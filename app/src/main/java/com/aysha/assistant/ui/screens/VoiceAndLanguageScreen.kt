package com.aysha.assistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.R
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.VoiceEngineMode
import com.aysha.assistant.core.VoiceGender
import com.aysha.assistant.core.VoicePersonalities
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import java.util.Locale

@Composable
fun VoiceAndLanguageScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val installedVoices by viewModel.availableTtsVoices.collectAsState()
    var genderTab by remember { mutableStateOf(VoiceGender.FEMALE) }

    val voicesForTab = VoicePersonalities.all.filter { it.gender == genderTab }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Voice & Language", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Language Picker
        Text("Conversation Language", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistantLanguage.entries.forEach { lang ->
                val active = settings.language == lang
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    elevated = active,
                    borderColor = if (active) c.accentPrimary else null,
                    onClick = { viewModel.updateSettings { it.copy(language = lang) } },
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    Text(
                        text = lang.label,
                        color = if (active) c.accentPrimary else c.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        // Voice Engine Mode
        Text("Voice Engine Mode", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceEngineMode.entries.forEach { mode ->
                val active = settings.voiceEngineMode == mode
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 14.dp,
                    elevated = active,
                    borderColor = if (active) c.accentPrimary else null,
                    onClick = { viewModel.updateSettings { it.copy(voiceEngineMode = mode) } },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = mode.label,
                        color = if (active) c.accentPrimary else c.textPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }

        // Female / Male Segmented Selector matching Screen 12
        Text("Voice Selection & Personalities", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        GlassCard(cornerRadius = 16.dp, contentPadding = PaddingValues(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                VoiceGender.entries.forEach { g ->
                    val selected = genderTab == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) Brush.horizontalGradient(listOf(c.accentPrimary, c.accentSecondary))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                            )
                            .clickable { genderTab = g }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = g.label,
                            color = if (selected) Color.White else c.textSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }

        voicesForTab.forEach { persona ->
            val selected = settings.voicePersonalityId == persona.id
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                elevated = selected,
                borderColor = if (selected) c.accentPrimary else null,
                onClick = { viewModel.updateSettings { it.copy(voicePersonalityId = persona.id) } },
                contentPadding = PaddingValues(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.aysha_portrait),
                        contentDescription = persona.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(persona.name, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text(persona.subtitle, color = c.textSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { viewModel.previewVoicePersonality(persona) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Preview", tint = c.accentPrimary)
                    }
                    if (selected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = c.accentPrimary)
                    }
                }
            }
        }

        // System Installed Offline TTS Voices
        if (installedVoices.isNotEmpty()) {
            Text("Device TTS Engine Voices (${installedVoices.size})", color = c.textSecondary, fontSize = 12.5.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassPill(
                    text = "Auto Select",
                    dotColor = if (settings.selectedTtsVoiceName.isBlank()) c.accentPrimary else null,
                    onClick = { viewModel.updateSettings { it.copy(selectedTtsVoiceName = "") } },
                )
                installedVoices.take(12).forEach { v ->
                    val label = "${v.localeTag}${if (v.isOffline) " (Offline)" else ""}"
                    GlassPill(
                        text = label,
                        dotColor = if (settings.selectedTtsVoiceName == v.name) c.accentPrimary else null,
                        onClick = { viewModel.updateSettings { it.copy(selectedTtsVoiceName = v.name) } },
                    )
                }
            }
        }

        // Speech Speed, Volume & Pitch Sliders
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Speed", color = c.textPrimary, fontWeight = FontWeight.Medium)
                    Text(String.format(Locale.US, "%.1fx", settings.ttsSpeechRate), color = c.accentPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.ttsSpeechRate,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(ttsSpeechRate = v) } },
                    valueRange = 0.6f..1.8f,
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Volume", color = c.textPrimary, fontWeight = FontWeight.Medium)
                    Text("${(settings.ttsVolume * 100).toInt()}%", color = c.accentPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.ttsVolume,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(ttsVolume = v) } },
                    valueRange = 0.1f..1.0f,
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Voice Pitch", color = c.textPrimary, fontWeight = FontWeight.Medium)
                    Text(String.format(Locale.US, "%.2fx", settings.ttsPitch), color = c.accentPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = settings.ttsPitch,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(ttsPitch = v) } },
                    valueRange = 0.7f..1.4f,
                )
            }
        }

        // Auto-Speak & Continuous Mode
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Speak Replies", color = c.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Speak responses aloud automatically", color = c.textSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.autoSpeakReplies,
                        onCheckedChange = { on -> viewModel.updateSettings { it.copy(autoSpeakReplies = on) } },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Conversation", color = c.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Automatically listen again after AYSHA finishes speaking", color = c.textSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.continuousConversation,
                        onCheckedChange = { on -> viewModel.updateSettings { it.copy(continuousConversation = on) } },
                    )
                }
            }
        }
    }
}
