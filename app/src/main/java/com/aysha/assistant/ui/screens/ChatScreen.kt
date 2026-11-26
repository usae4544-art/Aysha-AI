package com.aysha.assistant.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.R
import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val statusText by viewModel.statusText.collectAsState()
    val listState = rememberLazyListState()
    var draft by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(messages.size) {
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
        // Screen 5 Header: <- Avatar AYSHA Online + Call + Export + Clear
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                    }
                    Image(
                        painter = painterResource(id = R.drawable.aysha_portrait),
                        contentDescription = "AYSHA",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).clip(CircleShape),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(settings.assistantName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(c.success))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (settings.offlineOnlyMode) "Online (Offline Mode)" else "Online",
                                color = c.textSecondary,
                                fontSize = 11.5.sp,
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = {
                        viewModel.onOrbTap()
                        onSwitchToVoiceOrb()
                    }) {
                        Icon(Icons.Rounded.Call, contentDescription = "Voice Call", tint = c.accentPrimary)
                    }
                    IconButton(onClick = { viewModel.exportAndShareChats(asJson = false) }) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Export", tint = c.textPrimary)
                    }
                    IconButton(onClick = { viewModel.clearChatHistory() }) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear", tint = c.error)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (messages.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GlassCard(cornerRadius = 22.dp, contentPadding = PaddingValues(20.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Namaste ${settings.userName}!", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(modifier = Modifier.height(6.dp))
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
                        onCopy = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cb.setPrimaryClip(ClipData.newPlainText("AYSHA", msg.text))
                            Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                        },
                    )
                }
            }
        }

        if (statusText.contains("thinking", ignoreCase = true) || statusText.contains("Running", ignoreCase = true)) {
            Text(statusText, color = c.accentPrimary, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        }

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 28.dp,
            elevated = true,
            contentPadding = PaddingValues(start = 6.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        viewModel.onOrbTap()
                        onSwitchToVoiceOrb()
                    },
                    modifier = Modifier.size(38.dp),
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
                            if (draft.isNotBlank()) {
                                viewModel.sendTextMessage(draft)
                                draft = ""
                            }
                        },
                    ),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("Type a message...", color = c.textSecondary, fontSize = 14.5.sp)
                        inner()
                    },
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(c.accentPrimary, c.accentSecondary)))
                        .clickable {
                            if (draft.isNotBlank()) {
                                viewModel.sendTextMessage(draft)
                                draft = ""
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onSpeak: () -> Unit,
    onCopy: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val isUser = message.role == ChatRole.USER
    val isSystem = message.role == ChatRole.SYSTEM
    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestampMs))

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
                    .background(Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF6366F1))))
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
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Speak", tint = c.accentPrimary, modifier = Modifier.size(16.dp).clickable(onClick = onSpeak))
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", tint = c.textSecondary, modifier = Modifier.size(15.dp).clickable(onClick = onCopy))
                        }
                    }
                }
            }
        }
    }
}
