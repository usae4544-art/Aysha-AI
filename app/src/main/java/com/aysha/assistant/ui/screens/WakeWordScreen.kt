package com.aysha.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun WakeWordScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val wakeRunning by viewModel.wakeRunning.collectAsState()
    val wakeStatus by viewModel.wakeStatusText.collectAsState()
    val wakeEnergy by viewModel.wakeMicEnergy.collectAsState()

    var wakeInput by remember(settings.wakeWordsCsv) { mutableStateOf(settings.wakeWordsCsv) }

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
            Text("Wake Word System", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Mic Privacy Indicator & Toggle
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = if (wakeRunning) c.success else c.textSecondary,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("On-Device Wake Word", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            Text(wakeStatus, color = if (wakeRunning) c.success else c.textSecondary, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = settings.wakeWordEnabled,
                        onCheckedChange = { enable -> viewModel.toggleWakeWord(enable) },
                    )
                }

                // Live Mic Acoustic Level Meter
                Text("Live Acoustic Energy Gate (VAD)", color = c.textSecondary, fontSize = 11.5.sp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(c.glassBorder),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(wakeEnergy.coerceIn(0.02f, 1f))
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(c.accentPrimary, c.accentSecondary))),
                    )
                }
            }
        }

        // Configurable Wake Phrases
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Wake Phrases (Comma-separated)", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassPill("Hey Aysha")
                    GlassPill("Hello Aysha")
                    GlassPill("Wake up Aysha")
                }
                OutlinedTextField(
                    value = wakeInput,
                    onValueChange = { wakeInput = it },
                    label = { Text("Custom Wake Words") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        viewModel.updateSettings {
                            it.copy(wakeWordsCsv = wakeInput.ifBlank { "Hey Aysha,Hello Aysha,Wake up Aysha" })
                        }
                        if (settings.wakeWordEnabled) {
                            viewModel.toggleWakeWord(false)
                            viewModel.toggleWakeWord(true)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save Wake Phrases") }
            }
        }

        // Privacy Guarantee
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "100% On-Device Audio Processing: Audio stays inside your phone's local Vosk acoustic recognizer with an energy VAD gate to save battery. Private conversations are never recorded or uploaded in the background.",
                    color = c.textSecondary,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}
