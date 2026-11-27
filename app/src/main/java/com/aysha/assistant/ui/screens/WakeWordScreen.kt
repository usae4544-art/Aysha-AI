package com.aysha.assistant.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.core.ui.StaggeredAnimatedEntry
import com.aysha.assistant.ui.AyshaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WakeWordScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val wakeRunning by viewModel.wakeRunning.collectAsState()
    val wakeStatus by viewModel.wakeStatusText.collectAsState()
    val wakeEnergy by viewModel.wakeMicEnergy.collectAsState()
    val decisionLogs by viewModel.awarenessDecisionLogs.collectAsState()

    var wakeInput by remember(settings.wakeWordsCsv) { mutableStateOf(settings.wakeWordsCsv) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Wake Word & Decision Log", color = c.textPrimary, fontSize = 20.5.sp, fontWeight = FontWeight.Bold)
        }

        StaggeredAnimatedEntry(index = 0) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Mic, contentDescription = null, tint = if (wakeRunning) c.success else c.textSecondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("On-Device Wake Word", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                                Text(wakeStatus, color = if (wakeRunning) c.success else c.textSecondary, fontSize = 12.sp)
                            }
                        }
                        Switch(checked = settings.wakeWordEnabled, onCheckedChange = { enable -> viewModel.toggleWakeWord(enable) })
                    }

                    Text("Live Acoustic Energy Gate (${settings.awarenessSensitivity.label})", color = c.textSecondary, fontSize = 11.5.sp)
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
        }

        StaggeredAnimatedEntry(index = 1) {
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
                        val finalCsv = wakeInput.ifBlank { "Hey Aysha,Hello Aysha,Wake up Aysha" }
                        viewModel.applyWakePhrasesAndRestart(finalCsv)
                        Toast.makeText(context, "Wake phrases saved! Background listening updated.", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save Wake Phrases & Activate") }
            }
            }
        }

        // Maya-style Awareness Decision Log
        StaggeredAnimatedEntry(index = 2) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Awareness Decision Log (${decisionLogs.size})", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    if (decisionLogs.isNotEmpty()) {
                        OutlinedButton(onClick = { viewModel.clearAwarenessLogs() }) { Text("Clear Log") }
                    }
                }
                if (decisionLogs.isEmpty()) {
                    Text("Abhi kuch nahi. Wake Word on karo — har audio window ka local decision yahan dikhega.", color = c.textSecondary, fontSize = 12.sp)
                } else {
                    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    decisionLogs.take(12).forEach { log ->
                        Text(
                            text = "${fmt.format(Date(log.timestampMs))} · ${if (log.forwarded) "Jawab diya" else "Chup rahi"} · \"${log.heardText}\" (rms=${String.format(Locale.US, "%.2f", log.energy)})",
                            color = if (log.forwarded) c.success else c.textSecondary,
                            fontSize = 11.5.sp,
                        )
                    }
                }
            }
            }
        }

        StaggeredAnimatedEntry(index = 3) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "100% On-Device Audio Processing: Audio stays inside your phone's local Vosk acoustic recognizer with a 500ms pre-roll ring buffer & energy VAD gate so 'Hey Aysha' wakes smoothly without uploading background audio.",
                        color = c.textSecondary,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }
    }
}
