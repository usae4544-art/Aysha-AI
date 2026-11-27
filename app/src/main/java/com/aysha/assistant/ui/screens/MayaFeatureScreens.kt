package com.aysha.assistant.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aysha.assistant.core.AssistantMood
import com.aysha.assistant.core.CompanionMode
import com.aysha.assistant.core.SubAgentRole
import com.aysha.assistant.core.VibePresets
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import com.aysha.assistant.ui.components.MayaBottomNavBar
import com.aysha.assistant.ui.components.MayaTopBar
import com.aysha.assistant.ui.components.PrimaryBottomTab
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun RulesAndAgentsScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val mood by viewModel.assistantMood.collectAsState()
    val energy by viewModel.assistantEnergy.collectAsState()
    var showAddRule by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Rules, Agents & Mood", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Companion Mood & Energy Card
        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Live Companion Mood", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${mood.label} · Energy $energy%", color = Color(mood.colorHex), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistantMood.entries.forEach { m ->
                        GlassPill(
                            text = m.label,
                            dotColor = if (mood == m) Color(m.colorHex) else null,
                            onClick = { viewModel.setAssistantMood(m) },
                        )
                    }
                }
            }
        }

        // Sub-Agent Selector
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Active Sub-Agent Specialist", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(settings.activeSubAgent.specialtyInstruction, color = c.textSecondary, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubAgentRole.entries.forEach { role ->
                        val active = settings.activeSubAgent == role
                        GlassPill(
                            text = role.label,
                            dotColor = if (active) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(activeSubAgent = role) } },
                        )
                    }
                }
            }
        }

        // Companion Mode Selector
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Companion Mode", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompanionMode.entries.forEach { mode ->
                        val active = settings.companionMode == mode
                        GlassPill(
                            text = mode.label,
                            dotColor = if (active) c.accentSecondary else null,
                            onClick = { viewModel.updateSettings { it.copy(companionMode = mode) } },
                        )
                    }
                }
            }
        }

        // Vibes Quick Music Moods
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aaj ka Vibe?", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VibePresets.all.forEach { preset ->
                        GlassPill(
                            text = "${preset.title} (${preset.subtitle})",
                            dotColor = Color(preset.colorHex),
                            onClick = { viewModel.activateVibe(preset) },
                        )
                    }
                }
            }
        }

        // AYSHA Rules ("Aysha ke Rules")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("AYSHA ke Rules (${rules.size})", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            GlassPill(text = "+ Rule jodo", dotColor = c.accentPrimary, onClick = { showAddRule = true })
        }
        Text(
            "In rules ko AYSHA hamesha follow karti hai (ya voice me bolo: \"Ab se hamesha chhote jawab dena\").",
            color = c.textSecondary,
            fontSize = 12.sp,
        )

        if (rules.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Text("Abhi koi custom rule nahi — + Rule jodo dabao ya chat me bolo 'Ab se hamesha...'", color = c.textSecondary, fontSize = 13.sp)
            }
        } else {
            rules.forEach { rule ->
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = rule.enabled, onCheckedChange = { viewModel.toggleRule(rule) })
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(rule.text, color = c.textPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteRule(rule.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = c.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddRule) {
        var ruleText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRule = false },
            title = { Text("Naya Rule Jodo") },
            text = {
                OutlinedTextField(
                    value = ruleText,
                    onValueChange = { ruleText = it },
                    label = { Text("jaise: Hamesha chhote jawab dena") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (ruleText.isNotBlank()) viewModel.saveRule(ruleText)
                    showAddRule = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddRule = false }) { Text("Cancel") } },
        )
    }
}

@Composable
fun ScannerScreen(
    viewModel: AyshaViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    onOpenNotifications: () -> Unit = {},
    onSelectBottomTab: ((PrimaryBottomTab) -> Unit)? = null,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settings by viewModel.settings.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val scannerReply by viewModel.scannerReply.collectAsState()
    val analyzing by viewModel.scannerAnalyzing.collectAsState()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var question by remember { mutableStateOf("Kya dikh raha hai? Hinglish me detail me batao.") }
    var useFrontCam by remember { mutableStateOf(false) }
    var autoRecActive by remember { mutableStateOf(false) }
    var hasCamPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCamPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCamPermission) {
            camPermLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scanInf = rememberInfiniteTransition(label = "scanner_laser")
    val sweep by scanInf.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )

    val tempFile = remember { File(context.cacheDir, "aysha_scan_capture.jpg") }

    fun loadUriToBytes(uri: Uri, autoAnalyze: Boolean = false) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream)
                if (bmp != null) {
                    previewBitmap = bmp
                    val bos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 82, bos)
                    val bytes = bos.toByteArray()
                    imageBytes = bytes
                    if (autoAnalyze) viewModel.analyzeScannerImage(bytes, question)
                }
            }
        }
    }

    fun captureLiveFrameAndAnalyze(silentAuto: Boolean = false) {
        val ic = imageCaptureRef
        if (ic == null) {
            Toast.makeText(context, "Camera ready nahi hai — ek second ruko ya PICK use karo", Toast.LENGTH_SHORT).show()
            return
        }
        val opts = ImageCapture.OutputFileOptions.Builder(tempFile).build()
        ic.takePicture(
            opts,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bmp = BitmapFactory.decodeFile(tempFile.absolutePath)
                    if (bmp != null) {
                        if (!silentAuto) previewBitmap = bmp
                        val bos = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos)
                        val bytes = bos.toByteArray()
                        imageBytes = bytes
                        viewModel.analyzeScannerImage(bytes, question)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    if (!silentAuto) {
                        Toast.makeText(context, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    DisposableEffect(hasCamPermission, useFrontCam, lifecycleOwner) {
        if (!hasCamPermission) {
            onDispose { }
        } else {
            val future = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                runCatching {
                    val provider = future.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val cap = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    val selector = if (useFrontCam) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, cap)
                    imageCaptureRef = cap
                }
            }
            future.addListener(listener, ContextCompat.getMainExecutor(context))
            onDispose {
                runCatching { future.get().unbindAll() }
            }
        }
    }

    LaunchedEffect(autoRecActive, hasCamPermission) {
        while (autoRecActive && hasCamPermission) {
            delay(4500L)
            if (!analyzing && autoRecActive) {
                captureLiveFrameAndAnalyze(silentAuto = true)
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) loadUriToBytes(uri, autoAnalyze = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
    ) {
        MayaTopBar(
            title = "Scanner",
            showMenuInsteadOfBack = onOpenDrawer != null,
            onLeadingClick = { onOpenDrawer?.invoke() ?: onBack() },
            onNotificationsClick = onOpenNotifications,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Screenshot-3 exact Neural Link Viewfinder Stage with real live CameraX feed
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp, max = 380.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFF05080F))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(22.dp)),
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Captured Scan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (hasCamPermission) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Live Camera Preview ke liye Camera permission dein", color = Color(0xFF93C5FD), fontSize = 13.sp)
                        GlassPill("Enable Live Camera", dotColor = Color(0xFF22C55E)) {
                            camPermLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                }

                // Neural reticle crosshair + corner brackets + animated horizontal laser sweep
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h * 0.48f
                    val rad = size.minDimension * 0.28f

                    // 4 Corner HUD Brackets (Screenshot 3 exact look)
                    val pad = 16.dp.toPx()
                    val blen = 28.dp.toPx()
                    val bracketColor = Color(0xFF4FA3FF).copy(alpha = 0.72f)
                    val bStroke = 2.dp.toPx()
                    // Top-left
                    drawLine(bracketColor, Offset(pad, pad), Offset(pad + blen, pad), bStroke)
                    drawLine(bracketColor, Offset(pad, pad), Offset(pad, pad + blen), bStroke)
                    // Top-right
                    drawLine(bracketColor, Offset(w - pad, pad), Offset(w - pad - blen, pad), bStroke)
                    drawLine(bracketColor, Offset(w - pad, pad), Offset(w - pad, pad + blen), bStroke)
                    // Bottom-left
                    drawLine(bracketColor, Offset(pad, h - pad), Offset(pad + blen, h - pad), bStroke)
                    drawLine(bracketColor, Offset(pad, h - pad), Offset(pad, h - pad - blen), bStroke)
                    // Bottom-right
                    drawLine(bracketColor, Offset(w - pad, h - pad), Offset(w - pad - blen, h - pad), bStroke)
                    drawLine(bracketColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - blen), bStroke)

                    drawCircle(
                        color = Color(0xFF4FA3FF).copy(alpha = 0.45f),
                        radius = rad,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))),
                    )
                    drawCircle(
                        color = Color(0xFF4FA3FF).copy(alpha = 0.78f),
                        radius = rad * 0.65f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    drawCircle(color = Color(0xFF4FA3FF), radius = 3.dp.toPx(), center = Offset(cx, cy))

                    val lineY = h * sweep
                    drawRect(
                        brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0x444FA3FF))),
                        topLeft = Offset(w * 0.08f, (lineY - 46.dp.toPx()).coerceAtLeast(0f)),
                        size = androidx.compose.ui.geometry.Size(w * 0.84f, 46.dp.toPx()),
                    )
                    drawLine(
                        color = Color(0xFF60B2FF),
                        start = Offset(w * 0.08f, lineY),
                        end = Offset(w * 0.92f, lineY),
                        strokeWidth = 2.2.dp.toPx(),
                    )
                }

                // HUD Header overlay
                val camLabel = if (useFrontCam) "FRONT CAM" else "BACK CAM"
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (autoRecActive) Color(0xFFEF4444) else Color(0xFF22C55E)),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${settings.assistantName.uppercase()} VISION", color = Color(0xFF60B2FF), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = camLabel,
                            color = Color(0xFF60B2FF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            modifier = Modifier.clickable {
                                previewBitmap = null
                                useFrontCam = !useFrontCam
                            },
                        )
                    }
                    Text("> NEURAL LINK ACTIVE — poochho \"kya dikh raha hai?\"", color = Color(0xFF4ADE80), fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
                }

                // Telemetry Footer + FLIP / Glowing Capture Orb / REC
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = when {
                                analyzing -> "AI-FEED ANALYZING...\nNEURAL VISION LOCK"
                                autoRecActive -> "REC ACTIVE · LIVE SCAN\nAUTO-ANALYZING EVERY 4.5S"
                                else -> "FRM 004128  SIG 30\nAI-FEED 0.8 FPS  LATENCY OK"
                            },
                            color = Color(0xFF4FA3FF),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                        )
                        if (previewBitmap != null) {
                            GlassPill("Live Cam", dotColor = Color(0xFF22C55E)) {
                                previewBitmap = null
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                previewBitmap = null
                                useFrontCam = !useFrontCam
                                Toast.makeText(context, if (useFrontCam) "FRONT CAM" else "BACK CAM", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF162236)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.FlipCameraAndroid, contentDescription = "Flip", tint = Color(0xFF60B2FF))
                            }
                            Text("FLIP", color = Color(0xFF60B2FF), fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(Color.White, Color(0xFF93C5FD), Color(0xFF2563EB))))
                                .clickable {
                                    if (!hasCamPermission) {
                                        camPermLauncher.launch(Manifest.permission.CAMERA)
                                    } else {
                                        captureLiveFrameAndAnalyze(silentAuto = false)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (analyzing) CircularProgressIndicator(color = Color(0xFF1E3A8A), modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            else Icon(Icons.Rounded.CameraAlt, contentDescription = "Capture", tint = Color(0xFF0F172A), modifier = Modifier.size(26.dp))
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                if (!hasCamPermission) {
                                    camPermLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    autoRecActive = !autoRecActive
                                    Toast.makeText(context, if (autoRecActive) "Live Auto-Scan ON" else "Live Auto-Scan OFF", Toast.LENGTH_SHORT).show()
                                }
                            },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (autoRecActive) Color(0xFF7F1D1D) else Color(0xFF162236)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Videocam,
                                    contentDescription = "REC",
                                    tint = if (autoRecActive) Color(0xFFFCA5A5) else Color(0xFF94A3B8),
                                )
                            }
                            Text("REC", color = if (autoRecActive) Color(0xFFFCA5A5) else Color(0xFF94A3B8), fontFamily = FontFamily.Monospace, fontSize = 10.5.sp)
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPill("Gallery Pick", dotColor = c.accentPrimary) { galleryLauncher.launch("image/*") }
                GlassPill("Kya dikh raha hai?") { question = "Kya dikh raha hai? Hinglish me explain karo." }
                GlassPill("Solve Question") { question = "Isme likha sawaal step-by-step solve karo." }
                GlassPill("Extract Text") { question = "Is image ka saara text read karke do." }
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("${settings.assistantName} se kya poochhna hai?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )

            AuroraGradientButton(
                text = "Analyze with ${settings.assistantName} Vision",
                icon = Icons.Rounded.AutoAwesome,
                onClick = {
                    val bytes = imageBytes
                    if (bytes == null) {
                        Toast.makeText(context, "Pehle camera se photo lo ya gallery se pick karo", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.analyzeScannerImage(bytes, question)
                    }
                },
            )

            scannerReply?.let { reply ->
                GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${settings.assistantName} Vision Analysis", color = c.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            IconButton(onClick = { viewModel.speakMessage(reply) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak", tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(reply, color = c.textPrimary, fontSize = 13.5.sp)
                    }
                }
            }
        }

        if (onSelectBottomTab != null) {
            MayaBottomNavBar(
                activeTab = PrimaryBottomTab.SCAN,
                orbState = orbState,
                onSelectTab = onSelectBottomTab,
                onMicOrStopClick = { viewModel.onOrbTap() },
            )
        }
    }
}

@Composable
fun WhiteboardScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val cards by viewModel.whiteboardCards.collectAsState()
    var topicDraft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                }
                Text("Study Whiteboard", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            if (cards.isNotEmpty()) {
                IconButton(onClick = { viewModel.clearWhiteboard() }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear", tint = c.error)
                }
            }
        }

        if (cards.isEmpty()) {
            GlassCard(modifier = Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "AYSHA yahin padhayegi — koi bhi math, science, coding ya exam sawaal poochho!",
                        color = c.textSecondary,
                        fontSize = 14.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(card.title, color = c.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { viewModel.speakMessage(card.steps.joinToString(". ")) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak", tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                            card.steps.forEachIndexed { idx, step ->
                                Text("${idx + 1}. $step", color = c.textPrimary, fontSize = 13.5.sp)
                            }
                            if (card.codeSnippet.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF05070B))
                                        .padding(10.dp),
                                ) {
                                    Text(card.codeSnippet, color = Color(0xFF22D3EE), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = topicDraft,
                onValueChange = { topicDraft = it },
                placeholder = { Text("Explain Ohm's Law, Python loops, Quadratic...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
            )
            Button(
                onClick = {
                    if (topicDraft.isNotBlank()) {
                        viewModel.askWhiteboard(topicDraft)
                        topicDraft = ""
                    }
                },
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Teach")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    var urlInput by remember { mutableStateOf("https://duckduckgo.com") }
    var desktopMode by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
            )
            IconButton(onClick = {
                val trimmed = urlInput.trim()
                val target = when {
                    trimmed.startsWith("https://", ignoreCase = true) || trimmed.startsWith("http://", ignoreCase = true) -> trimmed
                    trimmed.contains("://") -> "https://duckduckgo.com"
                    else -> "https://$trimmed"
                }
                webViewRef?.loadUrl(target)
            }) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Go", tint = c.accentPrimary)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill("Back") { webViewRef?.goBack() }
            GlassPill(if (desktopMode) "Desktop ON" else "Desktop OFF") {
                desktopMode = !desktopMode
                webViewRef?.settings?.userAgentString = if (desktopMode) {
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/126.0.0.0 Safari/537.36"
                } else null
                webViewRef?.reload()
            }
            GlassPill("Copy Link") {
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("URL", webViewRef?.url ?: urlInput))
                Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
            }
            GlassPill("Summarize Page") {
                webViewRef?.evaluateJavascript("(function(){return document.body.innerText.slice(0,3000);})()") { text ->
                    val clean = text.trim('"').replace("\\n", "\n").take(2500)
                    if (clean.isNotBlank()) {
                        viewModel.sendTextMessage("Is webpage ko Hinglish me short me summarize karo:\n\n$clean", speakReply = true)
                        Toast.makeText(context, "AYSHA summarizing page...", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            GlassPill("External") {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webViewRef?.url ?: urlInput)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp)),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                        ): Boolean {
                            val scheme = request?.url?.scheme?.lowercase().orEmpty()
                            return scheme != "https" && scheme != "http"
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            if (!url.isNullOrBlank()) urlInput = url
                        }
                    }
                    loadUrl(urlInput)
                    webViewRef = this
                }
            },
        )
    }
}

@Composable
fun LicensesScreen(
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val items = listOf(
        "AYSHA – Your Personal AI Assistant (MIT License)" to
            "Copyright (c) 2026 AYSHA AI Contributors (usae4544-art)\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software, to deal in the Software without restriction including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software.",
        "Vosk Android Speech Recognition (Apache-2.0)" to
            "On-device wake-word and offline speech model powered by Alpha Cephei Vosk (`com.alphacephei:vosk-android`) and JNA (`net.java.dev.jna:jna`).",
        "AndroidX, Jetpack Compose & Room (Apache-2.0)" to
            "Material 3, Compose UI, DataStore Preferences, Room SQLite, Lifecycle, Navigation.",
        "OkHttp (Apache-2.0)" to
            "HTTP & WebSocket networking client by Square.",
        "Inter, Nunito & JetBrains Mono Fonts (SIL OFL 1.1)" to
            "Bundled open-source typefaces licensed under the SIL Open Font License v1.1.",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Open-Source Licenses", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
            Text(
                "AYSHA is 100% Free & Open Source under the MIT License — zero subscriptions, zero ads, and zero locked features.",
                color = c.accentPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
            )
        }

        items.forEach { (title, body) ->
            GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Text(body, color = c.textSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}
