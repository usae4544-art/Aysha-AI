package com.aysha.assistant.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import java.io.ByteArrayOutputStream
import java.util.Calendar

@Composable
fun HomeOrbScreen(
    viewModel: AyshaViewModel,
    onOpenChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMoreSheet: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenAppearance: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onOpenWhiteboard: () -> Unit = {},
    onOpenBrowser: () -> Unit = {},
    onOpenRulesAgents: () -> Unit = {},
    onOpenAutomation: () -> Unit = {},
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val lastReply by viewModel.lastAssistantReply.collectAsState()
    val sessionSeconds by viewModel.sessionElapsedSeconds.collectAsState()
    val followUpLeft by viewModel.followUpSecondsLeft.collectAsState()
    val timerRemaining by viewModel.timerRemainingSeconds.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    val timerLabel by viewModel.timerLabel.collectAsState()
    val liveMuted by viewModel.liveMuted.collectAsState()
    val wakeRunning by viewModel.wakeRunning.collectAsState()
    val mood by viewModel.assistantMood.collectAsState()
    val energy by viewModel.assistantEnergy.collectAsState()
    val pendingImage by viewModel.pendingAttachmentJpeg.collectAsState()

    var draft by rememberSaveable { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        val bos = ByteArrayOutputStream()
                        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, bos)
                        viewModel.attachPendingImage(bos.toByteArray())
                    }
                }
            }
        }
    }

    val greeting = remember(settings.userName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 5..11 -> "Good morning, ${settings.userName}"
            hour in 12..16 -> "Good afternoon, ${settings.userName}"
            hour in 17..21 -> "Good evening, ${settings.userName}"
            else -> "Abhi tak jaag rahe ho, ${settings.userName}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c.glassSurfaceElevated)
                        .border(1.dp, c.glassBorder, CircleShape)
                        .clickable(onClick = onOpenProviders),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Mode", tint = c.accentPrimary, modifier = Modifier.size(19.dp))
                }
                val modePillText = when {
                    settings.offlineOnlyMode -> "Offline"
                    settings.activeProvider.requiresApiKey && settings.currentKeyFor(settings.activeProvider).isBlank() -> "100% Free"
                    else -> settings.activeProvider.displayName
                }
                GlassPill(
                    text = if (wakeRunning) "Wake ON · $modePillText" else "${settings.activeSubAgent.shortTag} · $modePillText",
                    dotColor = if (wakeRunning) c.success else c.accentPrimary,
                    onClick = onOpenProviders,
                )
                if (sessionSeconds > 0) {
                    GlassPill(text = viewModel.formatClock(sessionSeconds), dotColor = c.accentSecondary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c.glassSurfaceElevated)
                        .border(1.dp, c.glassBorder, CircleShape)
                        .clickable(onClick = onOpenAppearance),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.ColorLens, contentDescription = "Orbs & Theme", tint = c.accentPrimary, modifier = Modifier.size(19.dp))
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(c.glassSurfaceElevated)
                        .border(1.dp, c.glassBorder, CircleShape)
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = c.textPrimary, modifier = Modifier.size(19.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Maya-style Personalized Greeting + Mood & Energy Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    color = c.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${settings.orbStyle.label} · ${settings.companionMode.label}",
                    color = c.textSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            GlassPill(
                text = "${mood.label} · $energy%",
                dotColor = Color(mood.colorHex),
                onClick = onOpenRulesAgents,
            )
        }

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
                                contentDescription = "Pause",
                                tint = c.accentPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        IconButton(onClick = { viewModel.resetTimer() }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.Stop, contentDescription = "Reset", tint = c.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Maya-style Quick Action Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QuickChip("Scanner", Icons.Rounded.CameraAlt, onOpenScanner)
            QuickChip("Whiteboard", Icons.Rounded.EditNote, onOpenWhiteboard)
            QuickChip("Browser", Icons.Rounded.Language, onOpenBrowser)
            QuickChip("Rules & Agents", Icons.Rounded.Tune, onOpenRulesAgents)
            QuickChip("Vibes", Icons.Rounded.MusicNote, onOpenRulesAgents)
            QuickChip("Automation", Icons.Rounded.Bolt, onOpenAutomation)
            QuickChip("SOS", Icons.Rounded.Warning) { viewModel.triggerSosConfirmation() }
        }

        // Main Center Stage
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
                    GlassCard(
                        cornerRadius = 50.dp,
                        elevated = true,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 9.dp),
                    ) {
                        val label = when {
                            orbState == OrbState.CONNECTING -> "Connect ho rahi hai..."
                            followUpLeft > 0 -> "Follow-up (${followUpLeft}s) · Sun rahi hai..."
                            else -> "Sun rahi hai..."
                        }
                        Text(label, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        EqualizerWaveformRow(active = true, audioLevel = audioLevel, barCount = 7, maxHeight = 56.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        AyshaVoiceOrb(
                            orbState = orbState,
                            audioLevel = audioLevel,
                            onTap = { viewModel.stopActiveVoiceOrSpeech() },
                            orbSize = 158.dp,
                            orbStyle = settings.orbStyle,
                            hudColor = settings.hudColor,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        EqualizerWaveformRow(active = true, audioLevel = audioLevel, barCount = 7, maxHeight = 56.dp)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    if (statusText.isNotBlank() && !statusText.contains("Listening", ignoreCase = true)) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(0.92f),
                            cornerRadius = 18.dp,
                            contentPadding = PaddingValues(12.dp),
                        ) {
                            Text(
                                text = statusText,
                                color = c.textPrimary,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(
                            cornerRadius = 50.dp,
                            elevated = true,
                            onClick = { viewModel.stopActiveVoiceOrSpeech() },
                            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 12.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = c.textPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tap to stop", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (settings.voiceEngineMode == VoiceEngineMode.GEMINI_LIVE) {
                            GlassCard(
                                cornerRadius = 50.dp,
                                elevated = true,
                                onClick = { viewModel.toggleLiveMute() },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                        modifier = Modifier.fillMaxWidth(0.92f),
                        cornerRadius = 20.dp,
                        elevated = true,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                    Spacer(modifier = Modifier.height(16.dp))
                    AyshaVoiceOrb(
                        orbState = orbState,
                        audioLevel = audioLevel.coerceAtLeast(0.45f),
                        onTap = { viewModel.stopActiveVoiceOrSpeech() },
                        orbSize = 156.dp,
                        orbStyle = settings.orbStyle,
                        hudColor = settings.hudColor,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    EqualizerWaveformRow(
                        active = true,
                        audioLevel = audioLevel.coerceAtLeast(0.45f),
                        barCount = 24,
                        maxHeight = 40.dp,
                    )
                    if (settings.bargeInEnabled) {
                        Text(
                            text = "Barge-in ON — beech me bol kar rok sakte ho",
                            color = c.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GlassCard(
                            cornerRadius = 50.dp,
                            elevated = true,
                            onClick = { viewModel.stopActiveVoiceOrSpeech() },
                            contentPadding = PaddingValues(horizontal = 26.dp, vertical = 11.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop", tint = c.textPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Stop", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            }
                        }
                        GlassCard(
                            cornerRadius = 50.dp,
                            elevated = true,
                            onClick = { viewModel.replayLastResponse() },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp),
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = "Replay", tint = c.accentPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                else -> {
                    AyshaAvatarRing(size = 86.dp, isSpeaking = false)
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(0.94f),
                        cornerRadius = 18.dp,
                        elevated = true,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    ) {
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
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        EqualizerWaveformRow(
                            active = orbState == OrbState.PROCESSING || orbState == OrbState.AUTOMATING,
                            audioLevel = audioLevel,
                            barCount = 5,
                            maxHeight = 42.dp,
                        )
                        AyshaVoiceOrb(
                            orbState = orbState,
                            audioLevel = audioLevel,
                            onTap = { viewModel.onOrbTap() },
                            orbSize = 132.dp,
                            orbStyle = settings.orbStyle,
                            hudColor = settings.hudColor,
                        )
                        EqualizerWaveformRow(
                            active = orbState == OrbState.PROCESSING || orbState == OrbState.AUTOMATING,
                            audioLevel = audioLevel,
                            barCount = 5,
                            maxHeight = 42.dp,
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        HomeDockCircle("Chat", Icons.Rounded.ChatBubbleOutline, onOpenChat)
                        HomeDockCircle("Voice", Icons.Rounded.Mic) { viewModel.onOrbTap() }
                        HomeDockCircle("Scanner", Icons.Rounded.CameraAlt, onOpenScanner)
                        HomeDockCircle("Hub", Icons.Rounded.Apps, onOpenMoreSheet)
                    }
                }
            }
        }

        pendingImage?.let { attached ->
            Spacer(modifier = Modifier.height(6.dp))
            GlassPill(
                text = "Image attached (${attached.size / 1024} KB) · Tap to remove",
                dotColor = c.success,
                onClick = { viewModel.attachPendingImage(null) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (orbState != OrbState.LISTENING && orbState != OrbState.SPEAKING) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                elevated = true,
                contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (pendingImage != null) Icons.Rounded.Close else Icons.Rounded.PhotoLibrary,
                            contentDescription = "Attach image",
                            tint = if (pendingImage != null) c.warning else c.textSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = c.textPrimary, fontSize = 14.5.sp),
                        cursorBrush = SolidColor(c.accentPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (draft.isNotBlank() || pendingImage != null) {
                                    viewModel.sendTextMessage(draft)
                                    draft = ""
                                }
                            },
                        ),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) {
                                Text("${settings.assistantName} se kuchh bhi poochho...", color = c.textSecondary, fontSize = 14.sp)
                            }
                            inner()
                        },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(c.accentPrimary, c.accentSecondary)))
                            .clickable {
                                if (draft.isNotBlank() || pendingImage != null) {
                                    viewModel.sendTextMessage(draft)
                                    draft = ""
                                } else {
                                    viewModel.onOrbTap()
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (draft.isNotBlank() || pendingImage != null) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(c.glassSurfaceElevated)
            .border(1.dp, c.glassBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = c.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
private fun HomeDockCircle(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(c.glassSurfaceElevated)
                .border(1.dp, c.glassBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = c.textPrimary, modifier = Modifier.size(19.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
