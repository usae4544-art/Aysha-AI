package com.aysha.assistant.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MemoryHistoryScreen(
    viewModel: AyshaViewModel,
    onBack: (() -> Unit)? = null,
) {
    val c = LocalAyshaColors.current
    val messages by viewModel.messages.collectAsState()
    val memories by viewModel.memories.collectAsState()

    var tab by remember { mutableIntStateOf(0) } // 0 = Chats, 1 = Memory
    var search by remember { mutableStateOf("") }
    var editingMemory by remember { mutableStateOf<MemoryEntry?>(null) }
    var showAddMemory by remember { mutableStateOf(false) }

    val filteredMsgs = messages.reversed().filter {
        search.isBlank() || it.text.contains(search, ignoreCase = true) || it.providerTag.contains(search, ignoreCase = true)
    }
    val filteredMems = memories.filter {
        search.isBlank() || it.title.contains(search, ignoreCase = true) || it.content.contains(search, ignoreCase = true) || it.tags.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                    }
                }
                Text("Memory & History", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = { viewModel.exportAndShareChats(asJson = true) }) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = "Export JSON", tint = c.accentPrimary)
                }
                if (tab == 1) {
                    IconButton(onClick = { showAddMemory = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Memory", tint = c.accentPrimary)
                    }
                }
            }
        }

        // Segmented Chats | Memory matching Screen 10
        GlassCard(cornerRadius = 16.dp, contentPadding = PaddingValues(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Chats (${messages.size})", "Memory (${memories.size})").forEachIndexed { idx, label ->
                    val active = tab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (active) Brush.horizontalGradient(listOf(c.accentPrimary, c.accentSecondary))
                                else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent)),
                            )
                            .clickable { tab = idx }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, color = if (active) Color.White else c.textSecondary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            }
        }

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text(if (tab == 0) "Search chats..." else "Search personal memories...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        if (tab == 0) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredMsgs, key = { it.id }) { msg ->
                    val timeStr = SimpleDateFormat("MMM dd · hh:mm a", Locale.getDefault()).format(Date(msg.timestampMs))
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${msg.role.name} · $timeStr",
                                    color = c.accentPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = msg.text,
                                    color = c.textPrimary,
                                    fontSize = 13.5.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = { viewModel.deleteMessage(msg.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = c.error)
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.exportAndShareChats(asJson = false) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Export Markdown") }
                Button(
                    onClick = { viewModel.clearChatHistory() },
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Clear History") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredMems, key = { it.id }) { mem ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mem.title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                                Text(mem.content, color = c.textSecondary, fontSize = 13.sp)
                                if (mem.tags.isNotBlank()) {
                                    Text("#${mem.tags}", color = c.accentPrimary, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { editingMemory = mem }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit", tint = c.accentPrimary)
                            }
                            IconButton(onClick = { viewModel.deleteMemory(mem.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = c.error)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { showAddMemory = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Personal Memory")
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
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (comma separated)") }, singleLine = true)
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
                        viewModel.updateMemory(
                            current.copy(
                                title = title.trim().ifBlank { current.title },
                                content = content.trim(),
                                tags = tags.trim(),
                                approvedByUser = approved,
                            ),
                        )
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
}
