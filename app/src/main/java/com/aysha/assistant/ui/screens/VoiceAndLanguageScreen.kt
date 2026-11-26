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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import com.aysha.assistant.core.AwarenessSensitivity
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Voice & Awareness Engine", color = c.textPrimary, fontSize = 20.5.sp, fontWeight = FontWeight.Bold)
        }

        GlassCard(cornerRadius = 50.dp, contentPadding = PaddingValues(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                VoiceGender.entries.forEach { g ->
                    val selected = genderTab == g
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (selected) Brush.horizontalGradient(listOf(c.accentPrimary, c.accentSecondary))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                            )
                            .clickable { genderTab = g }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(g.label, color = if (selected) Color.White else c.textSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.aysha_portrait),
                        contentDescription = persona.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(42.dp).clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(persona.name, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("(${persona.subtitle})", color = c.textSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { viewModel.previewVoicePersonality(persona) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Preview", tint = c.accentPrimary)
                    }
                    if (selected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = c.accentSecondary)
                    } else {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Speed", color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text(String.format(Locale.US, "%.1fx", settings.ttsSpeechRate), color = c.accentPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(value = settings.ttsSpeechRate, onValueChange = { v -> viewModel.updateSettings { it.copy(ttsSpeechRate = v) } }, valueRange = 0.6f..1.8f)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Volume", color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("${(settings.ttsVolume * 100).toInt()}%", color = c.accentPrimary, fontWeight = FontWeight.Bold)
                }
                Slider(value = settings.ttsVolume, onValueChange = { v -> viewModel.updateSettings { it.copy(ttsVolume = v) } }, valueRange = 0.1f..1.0f)
            }
        }

        // Maya-style Awareness, Barge-In & Follow-Up Controls
        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Maya-Grade Voice & Awareness", color = c.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Speaker Barge-In (AEC3/NS)", color = c.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Bol kar AYSHA ko beech me rok sako", color = c.textSecondary, fontSize = 11.5.sp)
                    }
                    Switch(checked = settings.bargeInEnabled, onCheckedChange = { on -> viewModel.updateSettings { it.copy(bargeInEnabled = on) } })
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Awareness Gate", color = c.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Smart VAD gate for background wake detection", color = c.textSecondary, fontSize = 11.5.sp)
                    }
                    Switch(checked = settings.awarenessEnabled, onCheckedChange = { on -> viewModel.updateSettings { it.copy(awarenessEnabled = on) } })
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mute During Music / TV", color = c.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Avoid false wakes when media is playing", color = c.textSecondary, fontSize = 11.5.sp)
                    }
                    Switch(checked = settings.muteDuringMedia, onCheckedChange = { on -> viewModel.updateSettings { it.copy(muteDuringMedia = on) } })
                }

                Text("Awareness Sensitivity (${settings.awarenessSensitivity.label})", color = c.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AwarenessSensitivity.entries.forEach { sens ->
                        GlassPill(
                            text = sens.label,
                            dotColor = if (settings.awarenessSensitivity == sens) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(awarenessSensitivity = sens) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Text("Follow-Up Window (${settings.followUpWindowSeconds}s)", color = c.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text("Jawab ke baad bina 'Hey Aysha' bole baat jaari rakho", color = c.textSecondary, fontSize = 11.5.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 8, 12, 20, 30).forEach { secs ->
                        GlassPill(
                            text = if (secs == 0) "Off" else "${secs}s",
                            dotColor = if (settings.followUpWindowSeconds == secs) c.accentSecondary else null,
                            onClick = { viewModel.updateSettings { it.copy(followUpWindowSeconds = secs) } },
                        )
                    }
                }
            }
        }

        Text("Language & Engine", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
                    Text(lang.label, color = if (active) c.accentPrimary else c.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

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
                    Text(mode.label, color = if (active) c.accentPrimary else c.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        if (installedVoices.isNotEmpty()) {
            Text("Device TTS Voices (${installedVoices.size})", color = c.textSecondary, fontSize = 12.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassPill("Auto Select", dotColor = if (settings.selectedTtsVoiceName.isBlank()) c.accentPrimary else null) {
                    viewModel.updateSettings { it.copy(selectedTtsVoiceName = "") }
                }
                installedVoices.take(12).forEach { v ->
                    GlassPill("${v.localeTag}${if (v.isOffline) " (Offline)" else ""}", dotColor = if (settings.selectedTtsVoiceName == v.name) c.accentPrimary else null) {
                        viewModel.updateSettings { it.copy(selectedTtsVoiceName = v.name) }
                    }
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-Speak Replies", color = c.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Speak answers aloud automatically", color = c.textSecondary, fontSize = 11.5.sp)
                    }
                    Switch(checked = settings.autoSpeakReplies, onCheckedChange = { on -> viewModel.updateSettings { it.copy(autoSpeakReplies = on) } })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Continuous Conversation", color = c.textPrimary, fontWeight = FontWeight.Medium)
                        Text("Keep listening until tapped to stop", color = c.textSecondary, fontSize = 11.5.sp)
                    }
                    Switch(checked = settings.continuousConversation, onCheckedChange = { on -> viewModel.updateSettings { it.copy(continuousConversation = on) } })
                }
            }
        }
    }
}
