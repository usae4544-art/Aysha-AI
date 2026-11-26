package com.aysha.assistant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun AiProvidersScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val testing by viewModel.connectivityTesting.collectAsState()
    val testResult by viewModel.connectivityResult.collectAsState()
    val discoveredModels by viewModel.discoveredModels.collectAsState()

    var editingProvider by remember { mutableStateOf<BrainProvider?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("AI Provider Settings", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Selectable Provider Cards matching Screen 8
        listOf(BrainProvider.GEMINI, BrainProvider.GROQ, BrainProvider.OPENAI, BrainProvider.LOCAL_MODEL).forEach { provider ->
            val selected = settings.activeProvider == provider && !settings.offlineOnlyMode
            val currentKey = settings.currentKeyFor(provider)
            val keyStatus = if (provider.requiresApiKey) {
                if (currentKey.isBlank()) "Enter API Key" else "Key Saved (${settings.maskedKey(currentKey)})"
            } else {
                settings.localBaseUrl
            }

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                elevated = selected,
                borderColor = if (selected) c.accentPrimary else null,
                onClick = { viewModel.updateSettings { it.copy(activeProvider = provider, offlineOnlyMode = false) } },
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(provider.displayName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                        Text(
                            "$keyStatus · ${settings.currentModelFor(provider)}",
                            color = c.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { editingProvider = provider }) {
                            Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (currentKey.isBlank() && provider.requiresApiKey) "Add Key" else "Configure")
                        }
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = c.accentPrimary)
                        }
                    }
                }
            }
        }

        // Local Offline Mode Switch
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Offline Mode Only", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Works completely offline with zero network calls", color = c.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.offlineOnlyMode,
                    onCheckedChange = { on -> viewModel.updateSettings { it.copy(offlineOnlyMode = on) } },
                )
            }
        }

        // Fallback Provider Auto Switch
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fallback Provider (Auto Switch)", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Auto-switch across configured providers & offline mode on failure", color = c.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.fallbackEnabled,
                    onCheckedChange = { on -> viewModel.updateSettings { it.copy(fallbackEnabled = on) } },
                )
            }
        }

        // Test Connection Button
        Button(
            onClick = {
                val p = if (settings.offlineOnlyMode) BrainProvider.OFFLINE else settings.activeProvider
                val url = when (p) {
                    BrainProvider.OPENAI -> settings.openAiBaseUrl
                    BrainProvider.LOCAL_MODEL -> settings.localBaseUrl
                    else -> p.defaultBaseUrl
                }
                viewModel.testProviderConnection(
                    provider = p,
                    apiKey = settings.currentKeyFor(p),
                    baseUrl = url,
                    model = settings.currentModelFor(p),
                )
            },
            enabled = !testing,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (testing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Testing Connection...")
            } else {
                Text("Test Connection")
            }
        }

        testResult?.let { res ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 16.dp,
                borderColor = if (res.ok) c.success else c.error,
                contentPadding = PaddingValues(12.dp),
            ) {
                Text(
                    text = res.message,
                    color = if (res.ok) c.success else c.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        if (discoveredModels.isNotEmpty()) {
            Text("Available Models (Tap to select)", color = c.textSecondary, fontSize = 12.5.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                discoveredModels.take(15).forEach { modelId ->
                    GlassPill(
                        text = modelId,
                        onClick = {
                            viewModel.updateSettings { s ->
                                when (s.activeProvider) {
                                    BrainProvider.GEMINI -> s.copy(geminiTextModel = modelId)
                                    BrainProvider.GROQ -> s.copy(groqModel = modelId)
                                    BrainProvider.OPENAI -> s.copy(openAiModel = modelId)
                                    BrainProvider.LOCAL_MODEL -> s.copy(localModel = modelId)
                                    BrainProvider.OFFLINE -> s
                                }
                            }
                        },
                    )
                }
            }
        }

        Text(
            text = "No API Key? No Problem! Aysha works in offline mode too.\nAPI keys are encrypted in Android Keystore and never logged.",
            color = c.textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }

    editingProvider?.let { provider ->
        var keyDraft by remember(provider) { mutableStateOf(settings.currentKeyFor(provider)) }
        var modelDraft by remember(provider) { mutableStateOf(settings.currentModelFor(provider)) }
        var baseUrlDraft by remember(provider) {
            mutableStateOf(
                when (provider) {
                    BrainProvider.OPENAI -> settings.openAiBaseUrl
                    BrainProvider.LOCAL_MODEL -> settings.localBaseUrl
                    else -> provider.defaultBaseUrl
                },
            )
        }
        var showKey by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = { Text("${provider.displayName} Configuration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (provider == BrainProvider.OPENAI || provider == BrainProvider.LOCAL_MODEL) {
                        OutlinedTextField(
                            value = baseUrlDraft,
                            onValueChange = { baseUrlDraft = it },
                            label = { Text("Base URL") },
                            singleLine = true,
                        )
                    }
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text(if (provider.requiresApiKey) "API Key (Encrypted in Keystore)" else "Optional Bearer Token") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                            }
                        },
                    )
                    OutlinedTextField(
                        value = modelDraft,
                        onValueChange = { modelDraft = it },
                        label = { Text("Model ID") },
                        singleLine = true,
                    )
                    if (keyDraft.isNotBlank()) {
                        OutlinedButton(
                            onClick = { keyDraft = "" },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove API Key")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSettings { s ->
                        when (provider) {
                            BrainProvider.GEMINI -> s.copy(
                                geminiApiKey = keyDraft.trim(),
                                geminiTextModel = modelDraft.trim().ifBlank { provider.defaultModel },
                            )
                            BrainProvider.GROQ -> s.copy(
                                groqApiKey = keyDraft.trim(),
                                groqModel = modelDraft.trim().ifBlank { provider.defaultModel },
                            )
                            BrainProvider.OPENAI -> s.copy(
                                openAiApiKey = keyDraft.trim(),
                                openAiBaseUrl = baseUrlDraft.trim().ifBlank { provider.defaultBaseUrl },
                                openAiModel = modelDraft.trim().ifBlank { provider.defaultModel },
                            )
                            BrainProvider.LOCAL_MODEL -> s.copy(
                                localApiKey = keyDraft.trim(),
                                localBaseUrl = baseUrlDraft.trim().ifBlank { provider.defaultBaseUrl },
                                localModel = modelDraft.trim().ifBlank { provider.defaultModel },
                            )
                            BrainProvider.OFFLINE -> s
                        }
                    }
                    editingProvider = null
                }) { Text("Save Encrypted") }
            },
            dismissButton = {
                TextButton(onClick = { editingProvider = null }) { Text("Cancel") }
            },
        )
    }
}
