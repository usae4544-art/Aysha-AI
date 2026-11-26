package com.aysha.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.MacroDefinition
import com.aysha.assistant.core.MacroStep
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun AutomationScreen(
    viewModel: AyshaViewModel,
    onBack: (() -> Unit)? = null,
) {
    val c = LocalAyshaColors.current
    val macros by viewModel.macros.collectAsState()
    val accConnected by viewModel.accessibilityConnected.collectAsState()
    val screenInspect by viewModel.screenInspectResult.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Automation & Workflows", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Create your own voice commands & multi-step macros", color = c.textSecondary, fontSize = 12.sp)
            }
        }

        // + Add Command Button matching Screen 9
        Button(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Command / Workflow")
        }

        // Accessibility Service Status & Screen Inspector
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevated = true,
            borderColor = if (accConnected) c.success else c.warning,
            contentPadding = PaddingValues(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessibilityNew, contentDescription = null, tint = if (accConnected) c.success else c.warning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Accessibility Automation", color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(if (accConnected) "Connected & Ready" else "Disabled — Enable for tap/type macros", color = c.textSecondary, fontSize = 11.5.sp)
                        }
                    }
                    GlassPill(
                        text = if (accConnected) "Active" else "Enable",
                        dotColor = if (accConnected) c.success else c.warning,
                        onClick = { viewModel.openSystemSettings("accessibility") },
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.inspectCurrentScreen() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Rounded.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Inspect Screen Elements")
                }

                screenInspect?.let { out ->
                    Text(out, color = c.textSecondary, fontSize = 11.5.sp)
                }
            }
        }

        // Macro List Cards
        macros.forEach { macro ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = c.accentPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(macro.name, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            val phrase = macro.triggerPhrase.ifBlank { macro.name.lowercase() }
                            Text("Say: \"$phrase\" · ${macro.steps.size} step(s)", color = c.accentPrimary, fontSize = 11.5.sp)
                            Text(macro.intent, color = c.textSecondary, fontSize = 12.sp)
                        }
                    }
                    Row {
                        IconButton(onClick = { viewModel.runMacro(macro) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Run", tint = c.success)
                        }
                        if (!macro.isBuiltIn) {
                            IconButton(onClick = { viewModel.deleteMacro(macro.name) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = c.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phrase by remember { mutableStateOf("") }
        var appTarget by remember { mutableStateOf("") }
        var timerMinutes by remember { mutableStateOf("") }
        var tapLabel by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Create Voice Command / Workflow") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Workflow Name (e.g. Coding Mode)") }, singleLine = true)
                    OutlinedTextField(value = phrase, onValueChange = { phrase = it }, label = { Text("Trigger Phrase (e.g. start coding)") }, singleLine = true)
                    OutlinedTextField(value = appTarget, onValueChange = { appTarget = it }, label = { Text("Step 1: Open App (optional, e.g. Termux)") }, singleLine = true)
                    OutlinedTextField(value = timerMinutes, onValueChange = { timerMinutes = it }, label = { Text("Step 2: Focus Timer Minutes (optional)") }, singleLine = true)
                    OutlinedTextField(value = tapLabel, onValueChange = { tapLabel = it }, label = { Text("Step 3: Tap Button Text (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val steps = buildList {
                        if (appTarget.isNotBlank()) {
                            add(MacroStep("open_app", mapOf("name" to appTarget.trim())))
                        }
                        val mins = timerMinutes.toIntOrNull()
                        if (mins != null && mins > 0) {
                            add(MacroStep("start_timer", mapOf("seconds" to (mins * 60).toString(), "label" to name.ifBlank { "Focus" })))
                        }
                        if (tapLabel.isNotBlank()) {
                            add(MacroStep("wait", mapOf("seconds" to "3")))
                            add(MacroStep("tap_text", mapOf("text" to tapLabel.trim())))
                        }
                    }
                    if (name.isNotBlank() && steps.isNotEmpty()) {
                        viewModel.saveMacro(
                            MacroDefinition(
                                name = name.trim(),
                                triggerPhrase = phrase.trim().ifBlank { name.trim().lowercase() },
                                intent = "Custom workflow (${steps.size} steps)",
                                steps = steps,
                                isBuiltIn = false,
                            ),
                        )
                    }
                    showAddDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            },
        )
    }
}
