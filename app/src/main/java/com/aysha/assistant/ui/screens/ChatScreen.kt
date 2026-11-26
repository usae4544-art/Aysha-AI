package com.aysha.assistant.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.R
import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.CompanionIntimacyLevel
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
    onSwitchToVoiceOrb: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val orbState by viewModel.orbState.collectAsState()
    val pendingImage by viewModel.pendingAttachmentJpeg.collectAsState()
    val listState = rememberLazyListState()

    var draft by rememberSaveable { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showPastChatsSheet by remember { mutableStateOf(false) }
    var replyTargetLabel by remember { mutableStateOf<String?>(null) }

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

    LaunchedEffect(messages.size, orbState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        // Header: <- Avatar + Live Pulse + Level + Past Chats + Voice Call + Export + Clear Confirmation
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            elevated = true,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                    }
                    Image(
                        painter = painterResource(id = R.drawable.aysha_portrait),
                        contentDescription = settings.assistantName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(settings.assistantName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            if (settings.companionIntimacy != CompanionIntimacyLevel.FRIENDLY) {
                                GlassPill(
                                    text = settings.companionIntimacy.label,
                                    dotColor = Color(0xFFEC4899),
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(c.success))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (settings.offlineOnlyMode) "Offline Mode" else "Live · Online",
                                color = c.textSecondary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = { showPastChatsSheet = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.History, contentDescription = "Purani baatein", tint = c.textPrimary, modifier = Modifier.size(19.dp))
                    }
                    IconButton(
                        onClick = {
                            viewModel.onOrbTap()
                            onSwitchToVoiceOrb()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(Icons.Rounded.Call, contentDescription = "Voice Call", tint = c.accentPrimary, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = { viewModel.exportAndShareChats(asJson = false) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Export", tint = c.textPrimary, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = { if (messages.isNotEmpty()) showClearConfirm = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear", tint = c.error, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Namaste ${settings.userName}!", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            "Kuch bhi baat karo — main automatic aapki important baatein aur reminders yaad rakhungi!",
                            color = c.textSecondary,
                            fontSize = 13.5.sp,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubbleItem(
                        message = msg,
                        onSpeak = { viewModel.speakMessage(msg.text) },
                        onMeaning = {
                            replyTargetLabel = "Simple Hinglish Meaning"
                            viewModel.sendTextMessage(
                                "Iska simple Hinglish aur English me matlab samjhao:\n\"${msg.text.take(280)}\"",
                                speakReply = false,
                            )
                        },
                        onCopy = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText(settings.assistantName, msg.text))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }

        // Animated Maya-style Thinking Indicator
        AnimatedVisibility(visible = orbState == OrbState.PROCESSING || orbState == OrbState.AUTOMATING) {
            TypingDotsIndicator(label = "${settings.assistantName} soch rahi hai...")
        }

        // Active Reply-to / Language Context Bar + Attachment Pill
        replyTargetLabel?.let { tag ->
            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                GlassPill(
                    text = "Reply mode: $tag · Tap to cancel",
                    dotColor = c.accentPrimary,
                    onClick = { replyTargetLabel = null },
                )
            }
        }

        pendingImage?.let { attached ->
            Row(modifier = Modifier.padding(bottom = 4.dp)) {
                GlassPill(
                    text = "Photo attached (${attached.size / 1024} KB) · Tap to remove",
                    dotColor = c.success,
                    onClick = { viewModel.attachPendingImage(null) },
                )
            }
        }

        // Quick Conversation Chips (Hinglish & Normal Reset Guard)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GlassPill("Hinglish me batao") { replyTargetLabel = "Hinglish" }
            GlassPill("English reply") { replyTargetLabel = "English" }
            GlassPill("Short jawab") { replyTargetLabel = "Short & Crisp" }
            if (settings.companionIntimacy != CompanionIntimacyLevel.FRIENDLY) {
                GlassPill("Normal baat karo", dotColor = c.warning) {
                    viewModel.sendTextMessage("normal baat karo", speakReply = false)
                }
            }
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevated = true,
            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = if (pendingImage != null) Icons.Rounded.Close else Icons.Rounded.PhotoLibrary,
                        contentDescription = "Attach",
                        tint = if (pendingImage != null) c.warning else c.textSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                }
                IconButton(
                    onClick = {
                        viewModel.onOrbTap()
                        onSwitchToVoiceOrb()
                    },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Voice", tint = c.accentPrimary, modifier = Modifier.size(20.dp))
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp, horizontal = 4.dp),
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 14.5.sp),
                    cursorBrush = SolidColor(c.accentPrimary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (draft.isNotBlank() || pendingImage != null) {
                                val outbound = replyTargetLabel?.let { "[Reply style: $it] $draft" } ?: draft
                                viewModel.sendTextMessage(outbound)
                                draft = ""
                                replyTargetLabel = null
                            }
                        },
                    ),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("${settings.assistantName} se kuchh bhi poochho...", color = c.textSecondary, fontSize = 14.sp)
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(c.accentPrimary, c.accentSecondary)))
                        .clickable {
                            if (draft.isNotBlank() || pendingImage != null) {
                                val outbound = replyTargetLabel?.let { "[Reply style: $it] $draft" } ?: draft
                                viewModel.sendTextMessage(outbound)
                                draft = ""
                                replyTargetLabel = null
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    // Maya-style Clear Chat Confirmation Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Ye chat clear karein?") },
            text = { Text("Saari chat messages is phone se hamesha ke liye clear ho jayengi.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearConfirm = false
                    },
                ) { Text("Clear karo") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            },
        )
    }

    // Purani baatein (Past Chats History Sheet)
    if (showPastChatsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPastChatsSheet = false },
            containerColor = c.backgroundDeep,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Purani baatein (${messages.size})", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (messages.isEmpty()) {
                    Text("Abhi koi purani baat saved nahi hai.", color = c.textSecondary, fontSize = 13.sp)
                } else {
                    LazyColumn(
                        modifier = Modifier.height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(messages.reversed().take(40), key = { it.id }) { m ->
                            val timeStr = SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault()).format(Date(m.timestampMs))
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, contentPadding = PaddingValues(12.dp)) {
                                Column {
                                    Text(m.text, color = c.textPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${m.role.name} · $timeStr", color = c.textSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun TypingDotsIndicator(label: String) {
    val c = LocalAyshaColors.current
    val inf = rememberInfiniteTransition(label = "typing_dots")
    val dot1 by inf.animateFloat(0f, -6f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d1")
    val dot2 by inf.animateFloat(0f, -6f, infiniteRepeatable(tween(420, delayMillis = 120, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d2")
    val dot3 by inf.animateFloat(0f, -6f, infiniteRepeatable(tween(420, delayMillis = 240, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "d3")

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(modifier = Modifier.offset(y = dot1.dp).size(7.dp).clip(CircleShape).background(c.accentPrimary))
        Box(modifier = Modifier.offset(y = dot2.dp).size(7.dp).clip(CircleShape).background(c.accentPrimary.copy(alpha = 0.8f)))
        Box(modifier = Modifier.offset(y = dot3.dp).size(7.dp).clip(CircleShape).background(c.accentSecondary))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, color = c.accentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onSpeak: () -> Unit,
    onMeaning: () -> Unit,
    onCopy: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val isUser = message.role == ChatRole.USER
    val isSystem = message.role == ChatRole.SYSTEM
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestampMs))
    var shown by remember(message.id) { mutableStateOf(true) }

    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 5 },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!isUser && !isSystem) {
                Image(
                    painter = painterResource(id = R.drawable.aysha_portrait),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(30.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            if (isUser) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp))
                        .background(Brush.linearGradient(listOf(c.accentPrimary, c.accentSecondary)))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(message.text, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(timeStr, color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp, modifier = Modifier.align(Alignment.End))
                    }
                }
            } else {
                GlassCard(
                    modifier = Modifier.widthIn(max = 295.dp),
                    cornerRadius = 18.dp,
                    elevated = !isSystem,
                    borderColor = if (isSystem) c.warning.copy(alpha = 0.6f) else null,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Column {
                        Text(
                            text = message.text,
                            color = if (isSystem) c.warning else c.textPrimary,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = listOf(message.providerTag, timeStr).filter { it.isNotBlank() }.joinToString(" · "),
                                color = c.textSecondary,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (!isSystem) {
                                    Icon(Icons.Rounded.Translate, contentDescription = "Meaning", tint = c.textSecondary, modifier = Modifier.size(15.dp).clickable(onClick = onMeaning))
                                }
                                Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak", tint = c.accentPrimary, modifier = Modifier.size(16.dp).clickable(onClick = onSpeak))
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = c.textSecondary, modifier = Modifier.size(15.dp).clickable(onClick = onCopy))
                            }
                        }
                    }
                }
            }
        }
    }
}
