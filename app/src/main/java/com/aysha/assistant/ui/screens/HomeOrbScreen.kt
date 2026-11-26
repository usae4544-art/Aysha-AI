package com.aysha.assistant.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.VoiceEngineMode
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import com.aysha.assistant.ui.components.AyshaAvatarRing
import com.aysha.assistant.ui.components.AyshaVoiceOrb
import com.aysha.assistant.ui.components.EqualizerWaveformRow

@Composable
fun HomeOrbScreen(
    viewModel: AyshaViewModel,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenFloating: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val lastReply by viewModel.lastAssistantReply.collectAsState()
    val sessionSeconds by viewModel.sessionElapsedSeconds.collectAsState()
    val timerRemaining by viewModel.timerRemainingSeconds.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    val timerLabel by viewModel.timerLabel.collectAsState()
    val liveMuted by viewModel.liveMuted.collectAsState()
    val wakeRunning by viewModel.wakeRunning.collectAsState()

    var draft by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val modePillText = when {
                    settings.offlineOnlyMode -> "Offline Mode"
                    settings.activeProvider.requiresApiKey && settings.currentKeyFor(settings.activeProvider).isBlank() -> "Free Offline"
                    else -> settings.activeProvider.displayName
                }
                GlassPill(
                    text = modePillText,
                    dotColor = if (wakeRunning) c.success else c.accentPrimary,
                    onClick = onOpenSettings,
                )
                if (wakeRunning) {
                    GlassPill(text = "Hey Aysha", dotColor = c.success)
                }
                if (sessionSeconds > 0) {
                    GlassPill(text = viewModel.formatClock(sessionSeconds), dotColor = c.accentSecondary)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onOpenFloating) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = "Floating Orb",
                        tint = if (settings.floatingOrbEnabled) c.accentPrimary else c.textSecondary,
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = c.textPrimary,
                    )
                }
            }
        }

        // Active Countdown Timer Pill
        if (timerRemaining > 0L) {
            Spacer(modifier = Modifier.height(6.dp))
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                elevated = true,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$timerLabel · ${viewModel.formatClock(timerRemaining)}",
                            color = c.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Row {
                        IconButton(onClick = { viewModel.pauseOrResumeTimer() }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = if (timerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Pause Timer",
                                tint = c.accentPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { viewModel.resetTimer() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Reset Timer", tint = c.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Scrollable Center Stage adapting smoothly across IDLE / LISTENING / PROCESSING / SPEAKING
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (orbState) {
                OrbState.LISTENING, OrbState.CONNECTING -> {
                    GlassPill(
                        text = if (orbState == OrbState.CONNECTING) "Connecting..." else "Listening...",
                        dotColor = c.accentPrimary,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        EqualizerWaveformRow(active = true, audioLevel = audioLevel, barCount = 7, maxHeight = 56.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        AyshaVoiceOrb(
                            orbState = orbState,
                            audioLevel = audioLevel,
                            onTap = { viewModel.stopActiveVoiceOrSpeech() },
                            orbSize = 152.dp,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        EqualizerWaveformRow(active = true, audioLevel = audioLevel, barCount = 7, maxHeight = 56.dp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(
                            text = statusText,
                            color = c.textPrimary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassCard(
                            cornerRadius = 50.dp,
                            elevated = true,
                            onClick = { viewModel.stopActiveVoiceOrSpeech() },
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Stop, contentDescription = null, tint = c.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tap to stop", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                        if (settings.voiceEngineMode == VoiceEngineMode.GEMINI_LIVE) {
                            GlassCard(
                                cornerRadius = 50.dp,
                                elevated = true,
                                onClick = { viewModel.toggleLiveMute() },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Icon(
                                    imageVector = if (liveMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                    contentDescription = "Mute",
                                    tint = if (liveMuted) c.warning else c.accentPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                OrbState.SPEAKING -> {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 20.dp,
                        elevated = true,
                        contentPadding = PaddingValues(14.dp),
                    ) {
                        Text(
                            text = lastReply,
                            color = c.textPrimary,
                            fontSize = 13.5.sp,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    AyshaAvatarRing(size = 156.dp, isSpeaking = true)
                    Spacer(modifier = Modifier.height(18.dp))
                    EqualizerWaveformRow(active = true, audioLevel = audioLevel.coerceAtLeast(0.45f), barCount = 24, maxHeight = 46.dp)
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(
                            cornerRadius = 50.dp,
                            elevated = true,
                            onClick = { viewModel.stopActiveVoiceOrSpeech() },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 11.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = c.error, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        GlassCard(
                            cornerRadius = 50.dp,
                            elevated = true,
                            onClick = { viewModel.replayLastResponse() },
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Replay, contentDescription = "Replay", tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Replay", color = c.textPrimary, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                            }
                        }
                    }
                }

                else -> {
                    // IDLE, PROCESSING, AUTOMATING, ERROR
                    AyshaAvatarRing(size = 104.dp, isSpeaking = false)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = settings.assistantName.uppercase(),
                        color = c.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        elevated = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = when (orbState) {
                                    OrbState.PROCESSING, OrbState.AUTOMATING -> statusText
                                    OrbState.ERROR -> statusText
                                    else -> lastReply
                                },
                                color = if (orbState == OrbState.ERROR) c.error else c.textPrimary,
                                fontSize = 13.5.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        EqualizerWaveformRow(
                            active = orbState == OrbState.PROCESSING || orbState == OrbState.AUTOMATING,
                            audioLevel = audioLevel,
                            barCount = 6,
                            maxHeight = 40.dp,
                        )
                        AyshaVoiceOrb(
                            orbState = orbState,
                            audioLevel = audioLevel,
                            onTap = { viewModel.onOrbTap() },
                            orbSize = 132.dp,
                        )
                        EqualizerWaveformRow(
                            active = orbState == OrbState.PROCESSING || orbState == OrbState.AUTOMATING,
                            audioLevel = audioLevel,
                            barCount = 6,
                            maxHeight = 40.dp,
                        )
                    }

                    // Quick Action Dock matching mockup (Chat, Voice, Settings, More)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        QuickDockAction("Chat", Icons.Rounded.ChatBubbleOutline, c.accentPrimary, onOpenChat)
                        QuickDockAction("Voice", Icons.Rounded.Mic, c.accentSecondary) { viewModel.onOrbTap() }
                        QuickDockAction("Replay", Icons.Rounded.Replay, c.accentGlow) { viewModel.replayLastResponse() }
                        QuickDockAction("More", Icons.Rounded.Apps, c.textPrimary, onOpenMore)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Starter chips for zero-key & quick commands
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val quickChips = listOf(
                            "Start Study Mode",
                            "Battery check karo",
                            "5 min timer lagao",
                            "Weather in Delhi",
                            "Open YouTube",
                        )
                        quickChips.forEach { prompt ->
                            GlassPill(
                                text = prompt,
                                onClick = { viewModel.sendTextMessage(prompt) },
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keyboard-safe bottom glass text input
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevated = true,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    singleLine = true,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 14.5.sp),
                    cursorBrush = SolidColor(c.accentPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (draft.isNotBlank()) {
                                viewModel.sendTextMessage(draft)
                                draft = ""
                            }
                        },
                    ),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) {
                            Text("Type a message...", color = c.textSecondary, fontSize = 14.5.sp)
                        }
                        inner()
                    },
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(c.accentPrimary, c.accentSecondary)))
                        .clickable {
                            if (draft.isNotBlank()) {
                                viewModel.sendTextMessage(draft)
                                draft = ""
                            } else {
                                viewModel.onOrbTap()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (draft.isNotBlank()) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic,
                        contentDescription = "Send or Talk",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickDockAction(
    label: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        GlassCard(
            cornerRadius = 14.dp,
            contentPadding = PaddingValues(10.dp),
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
