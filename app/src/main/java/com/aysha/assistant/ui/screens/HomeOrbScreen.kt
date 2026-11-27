package com.aysha.assistant.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.aysha.assistant.core.ui.ClayRecessedBox
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import com.aysha.assistant.ui.components.AyshaVoiceOrb
import com.aysha.assistant.ui.components.EqualizerWaveformRow
import com.aysha.assistant.ui.components.MayaBottomNavBar
import com.aysha.assistant.ui.components.MayaTopBar
import com.aysha.assistant.ui.components.PrimaryBottomTab
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeOrbScreen(
    viewModel: AyshaViewModel,
    unreadNotifications: Boolean = true,
    onOpenDrawer: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenChat: () -> Unit,
    onOpenYaadein: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenMoreSheet: () -> Unit,
    onOpenProviders: () -> Unit,
    onOpenAppearance: () -> Unit = {},
    onOpenScanner: () -> Unit = {},
    onOpenWhiteboard: () -> Unit = {},
    onOpenBrowser: () -> Unit = {},
    onOpenRulesAgents: () -> Unit = {},
    onOpenAutomation: () -> Unit = {},
    onOpenSecretSettings: () -> Unit = {},
    onOpenLicenseInfo: () -> Unit = {},
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val license by viewModel.licenseInfo.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val lastReply by viewModel.lastAssistantReply.collectAsState()
    val followUpLeft by viewModel.followUpSecondsLeft.collectAsState()
    val timerRemaining by viewModel.timerRemainingSeconds.collectAsState()
    val timerRunning by viewModel.timerRunning.collectAsState()
    val timerLabel by viewModel.timerLabel.collectAsState()
    val liveMuted by viewModel.liveMuted.collectAsState()
    val mood by viewModel.assistantMood.collectAsState()
    val energy by viewModel.assistantEnergy.collectAsState()
    val pendingImage by viewModel.pendingAttachmentJpeg.collectAsState()
    val updateInfo by viewModel.updateAvailableInfo.collectAsState()
    val remoteConfig by viewModel.remoteConfig.collectAsState()

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

    val (greetingTop, greetingName) = remember(settings.userName) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val prefix = when {
            hour in 5..11 -> "Good morning,"
            hour in 12..16 -> "Good afternoon,"
            hour in 17..21 -> "Good evening,"
            else -> "Abhi tak jaag rahe ho,"
        }
        prefix to settings.userName
    }
    val dayNum = remember { SimpleDateFormat("dd", Locale.getDefault()).format(Date()) }
    val dayMonth = remember { SimpleDateFormat("EEE, MMM", Locale.getDefault()).format(Date()) }

    val ecgInf = rememberInfiniteTransition(label = "ecg_bg")
    val ecgPhase by ecgInf.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "ecg_wave",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        // Maya-exact Top Header: [☰]  AYSHA  [🌗 🔔]
        MayaTopBar(
            title = settings.assistantName,
            showMenuInsteadOfBack = true,
            unreadNotifications = unreadNotifications,
            isDarkMode = c.isDark,
            onToggleDarkMode = { viewModel.toggleDarkMode(!c.isDark) },
            onLeadingClick = onOpenDrawer,
            onNotificationsClick = onOpenNotifications,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Live OTA Update Banner (shows automatically when a new APK release is published)
            updateInfo?.let { upd ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    cornerRadius = 18.dp,
                    elevated = true,
                    borderColor = c.accentPrimary,
                    onClick = { viewModel.downloadAndOpenUpdateApk(upd.apkDownloadUrl) },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Naya Update Available · v${upd.latestVersion}",
                                color = c.textPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                            )
                            Text(
                                text = upd.updateNotes.ifBlank { "Tap karke latest APK download aur install karein" },
                                color = c.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        GlassPill(
                            text = "Update",
                            dotColor = c.success,
                            onClick = { viewModel.downloadAndOpenUpdateApk(upd.apkDownloadUrl) },
                        )
                    }
                }
            }

            // Live OTA Announcement Banner (editable from Firestore without APK update)
            if (remoteConfig.announcementBanner.isNotBlank()) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = remoteConfig.announcementBanner,
                        color = c.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            // Screenshot-exact soft blue License / Companion Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (c.isDark) c.glassSurfaceElevated
                        else Color(0xFFCDE1FD).copy(alpha = 0.72f),
                    )
                    .border(
                        1.dp,
                        if (c.isDark) c.glassBorder else Color.White.copy(alpha = 0.65f),
                        RoundedCornerShape(18.dp),
                    )
                    .clickable(onClick = onOpenLicenseInfo)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = c.accentPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${license.plan.label} • ${settings.companionIntimacy.label}",
                                color = c.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Saare tools aur unlimited baat chalu hai",
                                color = c.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Secret ->",
                        color = c.accentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onOpenSecretSettings),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Screenshot-exact Greeting Left + Energy Tile Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greetingTop,
                        color = c.textSecondary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = greetingName,
                        color = c.textPrimary,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitleLine = when (orbState) {
                        OrbState.CONNECTING -> "Connect ho rahi hai…"
                        OrbState.LISTENING -> if (liveMuted) "Mic mute hai" else "Sun rahi hai…"
                        OrbState.SPEAKING -> "Bol rahi hai…"
                        OrbState.PROCESSING, OrbState.AUTOMATING -> statusText
                        OrbState.ERROR -> statusText
                        else -> if (lastReply.isNotBlank() && lastReply != "Ready") lastReply else "Ready — tap mic to talk"
                    }
                    Text(
                        text = subtitleLine,
                        color = if (orbState == OrbState.ERROR) c.error else c.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Top-Right Sculpted Clay Energy Tile
                GlassCard(
                    cornerRadius = 18.dp,
                    elevated = true,
                    onClick = onOpenRulesAgents,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = energy.toString(),
                                color = c.textPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                            )
                            Text(
                                text = "Energy",
                                color = c.textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
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
                            Text("$timerLabel · ${viewModel.formatClock(timerRemaining)}", color = c.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row {
                            IconButton(onClick = { viewModel.pauseOrResumeTimer() }, modifier = Modifier.size(30.dp)) {
                                Icon(if (timerRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = "Pause", tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { viewModel.resetTimer() }, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Reset", tint = c.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Centerpiece Neural HUD Orb Stage with soft ECG backdrop
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(226.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cy = h * 0.56f
                    val path = Path().apply {
                        moveTo(0f, cy)
                        var x = 0f
                        while (x <= w) {
                            val norm = x / w
                            val spike = when {
                                norm in 0.14f..0.18f -> kotlin.math.sin((norm - 0.14f) * 78f + ecgPhase) * -32f
                                norm in 0.80f..0.88f -> kotlin.math.sin((norm - 0.80f) * 65f - ecgPhase) * 28f
                                else -> kotlin.math.sin(norm * 14f + ecgPhase) * 7f
                            }
                            lineTo(x, cy + spike)
                            x += 12f
                        }
                    }
                    drawPath(
                        path = path,
                        color = c.accentPrimary.copy(alpha = if (c.isDark) 0.25f else 0.18f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawCircle(
                        color = c.accentPrimary.copy(alpha = if (c.isDark) 0.12f else 0.08f),
                        radius = size.minDimension * 0.48f,
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }

                AnimatedContent(
                    targetState = orbState,
                    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                    label = "home_orb_stage",
                ) { stage ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AyshaVoiceOrb(
                            orbState = stage,
                            audioLevel = if (stage == OrbState.SPEAKING) audioLevel.coerceAtLeast(0.45f) else audioLevel,
                            onTap = { viewModel.onOrbTap() },
                            orbSize = 166.dp,
                            orbStyle = settings.orbStyle,
                            hudColor = settings.hudColor,
                            hudCoreColor = settings.hudCoreColor,
                            hudMainColor = settings.hudMainColor,
                            hudRaysLengthPct = settings.hudRaysLengthPct,
                            hudRingEnabled = settings.hudRingEnabled,
                            renderMode = settings.orbRenderMode,
                            showCenterIcon = false,
                        )
                        if (stage == OrbState.LISTENING || stage == OrbState.SPEAKING) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                EqualizerWaveformRow(active = true, audioLevel = audioLevel, barCount = 10, maxHeight = 20.dp)
                                if (settings.voiceEngineMode == VoiceEngineMode.GEMINI_LIVE) {
                                    GlassPill(
                                        text = if (liveMuted) "Unmute" else "Mute",
                                        dotColor = if (liveMuted) c.warning else c.success,
                                        onClick = { viewModel.toggleLiveMute() },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Exact Screenshot 3-Pill Strip: [Gaane] [Padhai] [Journal]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HomeQuickClayPill("Gaane", Icons.Rounded.MusicNote, Modifier.weight(1f), onOpenRulesAgents)
                HomeQuickClayPill("Padhai", Icons.Rounded.MenuBook, Modifier.weight(1f), onOpenWhiteboard)
                HomeQuickClayPill("Journal", Icons.Rounded.Edit, Modifier.weight(1f)) {
                    viewModel.sendTextMessage("Aaj ka chhota journal note likhte hain — puchho mera din kaisa raha?", speakReply = false)
                    onOpenChat()
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Exact Screenshot 3-Card Telemetry Row: [Mausam] [Aaj] [Mood]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f).height(94.dp),
                    cornerRadius = 20.dp,
                    elevated = true,
                    onClick = {
                        viewModel.sendTextMessage("Aaj ka mausam aur weather forecast batao", speakReply = true)
                        onOpenChat()
                    },
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Cloud, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mausam", color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text("—", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Check live", color = c.textSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f).height(94.dp),
                    cornerRadius = 20.dp,
                    elevated = true,
                    onClick = onOpenAutomation,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aaj", color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                        Text(dayNum, color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                        Text(dayMonth, color = c.textSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }

                GlassCard(
                    modifier = Modifier.weight(1f).height(94.dp),
                    cornerRadius = 20.dp,
                    elevated = true,
                    onClick = onOpenSecretSettings,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = Color(mood.colorHex).copy(alpha = 0.22f),
                            modifier = Modifier.size(46.dp).align(Alignment.CenterEnd),
                        )
                        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mood", color = c.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                            Text(mood.label, color = c.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(mood.caption, color = c.textSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            pendingImage?.let { attached ->
                GlassPill(
                    text = "Photo attached (${attached.size / 1024} KB) · Tap to remove",
                    dotColor = c.success,
                    onClick = { viewModel.attachPendingImage(null) },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Recessed Clay Prompt Bar matching Screenshot 1/3/18
            ClayRecessedBox(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(start = 10.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = if (pendingImage != null) Icons.Rounded.Close else Icons.Rounded.AttachFile,
                            contentDescription = "Attach",
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
                                Text("${settings.assistantName} se kuch bhi poochho...", color = c.textSecondary, fontSize = 14.sp)
                            }
                            inner()
                        },
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(c.glassSurfaceElevated)
                            .border(1.dp, c.glassBorder, CircleShape)
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
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = c.textPrimary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }

            // Slim secondary shortcut pills (Secret, Appearance, Browser, SOS) cleanly below fold
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassPill("Orbs & Theme", dotColor = c.accentPrimary, onClick = onOpenAppearance)
                GlassPill("Secret (18+)", dotColor = Color(0xFFEC4899), onClick = onOpenSecretSettings)
                GlassPill("Browser", onClick = onOpenBrowser)
                GlassPill("All Tools", onClick = onOpenMoreSheet)
                GlassPill("SOS", dotColor = c.error) { viewModel.triggerSosConfirmation() }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Exact 5-Slot Bottom Clay Navigation Dock
        MayaBottomNavBar(
            activeTab = PrimaryBottomTab.HOME,
            orbState = orbState,
            onSelectTab = { tab ->
                when (tab) {
                    PrimaryBottomTab.HOME -> Unit
                    PrimaryBottomTab.SCAN -> onOpenScanner()
                    PrimaryBottomTab.YAADEIN -> onOpenYaadein()
                    PrimaryBottomTab.CHAT -> onOpenChat()
                }
            },
            onMicOrStopClick = { viewModel.onOrbTap() },
        )
    }
}

@Composable
private fun HomeQuickClayPill(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    GlassCard(
        modifier = modifier,
        cornerRadius = 22.dp,
        elevated = true,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}
