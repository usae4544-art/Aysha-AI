package com.aysha.assistant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.R
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryHistoryScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val archivedMemories by viewModel.archivedMemories.collectAsState()
    val documents by viewModel.documents.collectAsState()

    var tab by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var editingMemory by remember { mutableStateOf<MemoryEntry?>(null) }
    var showAddMemory by remember { mutableStateOf(false) }
    var showAddDoc by remember { mutableStateOf(false) }
    var showClearChatsConfirm by remember { mutableStateOf(false) }
    var deleteMemoryTarget by remember { mutableStateOf<MemoryEntry?>(null) }
    var wipeStep by remember { mutableIntStateOf(0) }

    val filteredMsgs = messages.reversed().filter {
        search.isBlank() || it.text.contains(search, ignoreCase = true) || it.providerTag.contains(search, ignoreCase = true)
    }
    val filteredMems = memories.filter {
        search.isBlank() || it.title.contains(search, ignoreCase = true) || it.content.contains(search, ignoreCase = true)
    }
    val filteredDocs = documents.filter {
        search.isBlank() || it.title.contains(search, ignoreCase = true) || it.content.contains(search, ignoreCase = true)
    }
    val filteredArchived = archivedMemories.filter {
        search.isBlank() || it.title.contains(search, ignoreCase = true) || it.content.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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
                Text("Yaadein, Docs & Chats", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = { viewModel.exportAndShareChats(asJson = true) }) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "Export", tint = c.accentPrimary)
                }
                if (tab == 1) {
                    IconButton(onClick = { showAddMemory = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Memory", tint = c.accentPrimary)
                    }
                } else if (tab == 2) {
                    IconButton(onClick = { showAddDoc = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Doc", tint = c.accentPrimary)
                    }
                }
            }
        }

        // 4 Maya-style tabs: Chats | Yaadein | Docs | Purani
        GlassCard(cornerRadius = 50.dp, contentPadding = PaddingValues(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Chats", "Yaadein", "Docs", "Purani").forEachIndexed { idx, label ->
                    val active = tab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (active) Brush.horizontalGradient(listOf(c.accentPrimary, c.accentSecondary))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                            )
                            .clickable { tab = idx }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (active) Color.White else c.textSecondary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search chats, yaadein or docs...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        when (tab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredMsgs, key = { it.id }) { msg ->
                        val timeStr = SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault()).format(Date(msg.timestampMs))
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.aysha_portrait),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(36.dp).clip(CircleShape),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(msg.text, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${msg.role.name} · $timeStr", color = c.textSecondary, fontSize = 11.5.sp)
                                }
                                IconButton(onClick = { viewModel.deleteMessage(msg.id) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = c.error, modifier = Modifier.size(18.dp))
                                }
                                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF6B1D36).copy(alpha = 0.72f))
                        .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                        .clickable { if (messages.isNotEmpty()) showClearChatsConfirm = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = Color(0xFFFDA4AF), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Chat History", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                    }
                }
            }

            1 -> {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, elevated = true, contentPadding = PaddingValues(12.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mere baare me · ${settings.userName}", color = c.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        val profileFacts = memories.filter { it.tags.contains("profile") || it.tags.contains("location") || it.tags.contains("preference") }.take(3)
                        if (profileFacts.isEmpty()) {
                            Text("Abhi profile details khaali hain — chat me apna naam, shehar ya pasand batao!", color = c.textSecondary, fontSize = 12.sp)
                        } else {
                            profileFacts.forEach { f -> Text("• ${f.title}: ${f.content}", color = c.textPrimary, fontSize = 12.5.sp) }
                        }
                    }
                }

                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Memory in Chat", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                            Text("Baat ke baad khud kaam ki baatein yaad rakhe", color = c.textSecondary, fontSize = 11.sp)
                        }
                        Switch(checked = settings.autoMemoryEnabled, onCheckedChange = { on -> viewModel.updateSettings { it.copy(autoMemoryEnabled = on) } })
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredMems, key = { it.id }) { mem ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                ColoredIconBadge(icon = Icons.Rounded.Psychology, tintColor = c.accentSecondary, size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mem.title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                    Text(mem.content, color = c.textSecondary, fontSize = 12.5.sp)
                                    Text("Source: ${mem.source}", color = c.accentPrimary, fontSize = 10.5.sp)
                                }
                                IconButton(onClick = { editingMemory = mem }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { viewModel.archiveMemory(mem.id, true) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Archive, contentDescription = "Archive", tint = c.warning, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                AuroraGradientButton(text = "Add Personal Memory", icon = Icons.Rounded.Add, onClick = { showAddMemory = true })
            }

            2 -> {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredDocs, key = { it.id }) { doc ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                ColoredIconBadge(icon = Icons.Rounded.Description, tintColor = Color(0xFF0EA5E9), size = 36.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(doc.title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                    Text(doc.content, color = c.textSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${doc.chunkCount} searchable chunks", color = c.accentPrimary, fontSize = 10.5.sp)
                                }
                                IconButton(onClick = { viewModel.deleteDocument(doc.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = c.error)
                                }
                            }
                        }
                    }
                }

                AuroraGradientButton(text = "Add Document / Resume / Notes", icon = Icons.Rounded.Add, onClick = { showAddDoc = true })
            }

            else -> {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filteredArchived, key = { it.id }) { mem ->
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(mem.title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(mem.content, color = c.textSecondary, fontSize = 12.sp)
                                }
                                IconButton(onClick = { viewModel.archiveMemory(mem.id, false) }) {
                                    Icon(Icons.Rounded.Restore, contentDescription = "Restore", tint = c.success)
                                }
                                IconButton(onClick = { deleteMemoryTarget = mem }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = c.error)
                                }
                            }
                        }
                    }
                }
                if (archivedMemories.isNotEmpty() || memories.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color(0xFF6B1D36).copy(alpha = 0.72f))
                            .border(1.dp, Color(0xFFF43F5E).copy(alpha = 0.5f), RoundedCornerShape(23.dp))
                            .clickable { wipeStep = 1 },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Sab bhula do (2-Step Confirm)", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showAddMemory || editingMemory != null) {
        val current = editingMemory
        var title by remember(current) { mutableStateOf(current?.title ?: "") }
        var content by remember(current) { mutableStateOf(current?.content ?: "") }
        var tags by remember(current) { mutableStateOf(current?.tags ?: "") }
        var approved by remember(current) { mutableStateOf(current?.approvedByUser ?: true) }

        AlertDialog(
            onDismissRequest = {
                showAddMemory = false
                editingMemory = null
            },
            title = { Text(if (current == null) "Save Personal Memory" else "Edit Memory") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Topic Title") }, singleLine = true)
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Fact or Preference") })
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags") }, singleLine = true)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Approved for AI context", fontSize = 13.sp)
                        Switch(checked = approved, onCheckedChange = { approved = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (current == null) {
                        viewModel.saveMemory(title, content, tags)
                    } else {
                        viewModel.updateMemory(current.copy(title = title, content = content, tags = tags, approvedByUser = approved))
                    }
                    showAddMemory = false
                    editingMemory = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddMemory = false
                    editingMemory = null
                }) { Text("Cancel") }
            },
        )
    }

    if (showAddDoc) {
        var docTitle by remember { mutableStateOf("") }
        var docBody by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDoc = false },
            title = { Text("Add Knowledge Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = docTitle, onValueChange = { docTitle = it }, label = { Text("Title (e.g. Resume, Exam Syllabus)") }, singleLine = true)
                    OutlinedTextField(value = docBody, onValueChange = { docBody = it }, label = { Text("Paste text or notes...") }, minLines = 4)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (docBody.isNotBlank()) {
                        viewModel.saveDocument(docTitle.ifBlank { "Personal Note" }, docBody)
                    }
                    showAddDoc = false
                }) { Text("Save & Index") }
            },
            dismissButton = { TextButton(onClick = { showAddDoc = false }) { Text("Cancel") } },
        )
    }

    if (showClearChatsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatsConfirm = false },
            title = { Text("Ye chat mita dein?") },
            text = { Text("Saari purani chat messages is phone se delete ho jayengi.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearChatsConfirm = false
                    },
                ) { Text("Haan, clear karo") }
            },
            dismissButton = { TextButton(onClick = { showClearChatsConfirm = false }) { Text("Rehne do") } },
        )
    }

    deleteMemoryTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteMemoryTarget = null },
            title = { Text("Ye yaad delete karein?") },
            text = { Text("\"${target.title}: ${target.content}\"") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    onClick = {
                        viewModel.deleteMemory(target.id)
                        deleteMemoryTarget = null
                    },
                ) { Text("Delete karo") }
            },
            dismissButton = { TextButton(onClick = { deleteMemoryTarget = null }) { Text("Cancel") } },
        )
    }

    if (wipeStep == 1) {
        AlertDialog(
            onDismissRequest = { wipeStep = 0 },
            title = { Text("Saari yaadein bhula dein?") },
            text = { Text("Tumhari saari saved yaadein aur chats hamesha ke liye chali jayengi.") },
            confirmButton = {
                Button(onClick = { wipeStep = 2 }) { Text("Aage badho") }
            },
            dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text("Rehne do") } },
        )
    }

    if (wipeStep == 2) {
        AlertDialog(
            onDismissRequest = { wipeStep = 0 },
            title = { Text("Pakka sab bhula dein?") },
            text = { Text("Aakhri confirmation — iske baad sab kuch zero se shuru hoga.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    onClick = {
                        viewModel.wipeAllLocalData()
                        wipeStep = 0
                    },
                ) { Text("Haan, bhula do") }
            },
            dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text("Rehne do") } },
        )
    }
}
