package com.aysha.assistant.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Memory
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.core.ui.StaggeredAnimatedEntry
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
            Text("AI Provider Settings", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Google Gemini primary + optional cloud fallback
        val providerCards = listOf(
            Triple(BrainProvider.GEMINI, Icons.Rounded.AutoAwesome, Color(0xFF3B82F6)),
            Triple(BrainProvider.GROQ, Icons.Rounded.Bolt, Color(0xFFEF4444)),
            Triple(BrainProvider.OPENAI, Icons.Rounded.Dns, Color(0xFF334155)),
        )

        providerCards.forEachIndexed { idx, (provider, icon, badgeColor) ->
            val selected = settings.activeProvider == provider
            val currentKey = settings.currentKeyFor(provider)
            val keyStatus = if (currentKey.isBlank()) "Enter API Key" else "${settings.maskedKey(currentKey)} · ${settings.currentModelFor(provider)}"

            StaggeredAnimatedEntry(index = idx) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 18.dp,
                    elevated = selected,
                    borderColor = if (selected) c.accentPrimary else null,
                    onClick = {
                        viewModel.updateSettings { it.copy(activeProvider = provider, offlineOnlyMode = false) }
                        editingProvider = provider
                    },
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ColoredIconBadge(icon = icon, tintColor = badgeColor, size = 38.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(provider.displayName, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            Text(keyStatus, color = c.textSecondary, fontSize = 12.sp)
                        }
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = c.accentPrimary)
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
                        }
                    }
                }
            }
        }

        // Fallback Provider Switch
        StaggeredAnimatedEntry(index = 3) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fallback Provider", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Auto Switch (Gemini primary)", color = c.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.fallbackEnabled,
                    onCheckedChange = { on -> viewModel.updateSettings { it.copy(fallbackEnabled = on) } },
                )
            }
            }
        }

        // Test Connection Gradient Button
        if (testing) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 25.dp, contentPadding = PaddingValues(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Testing Connection...", color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            AuroraGradientButton(
                text = "Test Gemini Connection",
                onClick = {
                    val p = if (settings.activeProvider == BrainProvider.OFFLINE) BrainProvider.GEMINI else settings.activeProvider
                    val url = if (p == BrainProvider.OPENAI) settings.openAiBaseUrl else p.defaultBaseUrl
                    viewModel.testProviderConnection(
                        provider = p,
                        apiKey = settings.currentKeyFor(p),
                        baseUrl = url,
                        model = settings.currentModelFor(p),
                    )
                },
            )
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
            Text("Discovered Models (Tap to select)", color = c.textSecondary, fontSize = 12.sp)
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
                                    BrainProvider.OFFLINE -> s
                                }
                            }
                        },
                    )
                }
            }
        }

        Text(
            text = "AYSHA runs on Google Gemini API\nAdd your free Gemini API key to start.",
            color = c.textSecondary,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
    }

    editingProvider?.let { provider ->
        var keyDraft by remember(provider) { mutableStateOf(settings.currentKeyFor(provider)) }
        var modelDraft by remember(provider) { mutableStateOf(settings.currentModelFor(provider)) }
        var baseUrlDraft by remember(provider) {
            mutableStateOf(
                if (provider == BrainProvider.OPENAI) settings.openAiBaseUrl else provider.defaultBaseUrl,
            )
        }
        var showKey by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingProvider = null },
            title = { Text("${provider.displayName} API Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (provider == BrainProvider.OPENAI) {
                        OutlinedTextField(value = baseUrlDraft, onValueChange = { baseUrlDraft = it }, label = { Text("Base URL") }, singleLine = true)
                    }
                    OutlinedTextField(
                        value = keyDraft,
                        onValueChange = { keyDraft = it },
                        label = { Text(if (provider.requiresApiKey) "API Key (Keystore Encrypted)" else "Optional Bearer Token") },
                        singleLine = true,
                        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showKey = !showKey }) {
                                Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, contentDescription = null)
                            }
                        },
                    )
                    OutlinedTextField(value = modelDraft, onValueChange = { modelDraft = it }, label = { Text("Model ID") }, singleLine = true)
                    if (keyDraft.isNotBlank()) {
                        OutlinedButton(onClick = { keyDraft = "" }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Remove Key")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateSettings { s ->
                        val next = when (provider) {
                            BrainProvider.GEMINI -> s.copy(geminiApiKey = keyDraft.trim(), geminiTextModel = modelDraft.trim().ifBlank { provider.defaultModel })
                            BrainProvider.GROQ -> s.copy(groqApiKey = keyDraft.trim(), groqModel = modelDraft.trim().ifBlank { provider.defaultModel })
                            BrainProvider.OPENAI -> s.copy(openAiApiKey = keyDraft.trim(), openAiBaseUrl = baseUrlDraft.trim().ifBlank { provider.defaultBaseUrl }, openAiModel = modelDraft.trim().ifBlank { provider.defaultModel })
                            BrainProvider.OFFLINE -> s
                        }
                        next.copy(activeProvider = provider, offlineOnlyMode = false)
                    }
                    editingProvider = null
                }) { Text("Save & Activate") }
            },
            dismissButton = { TextButton(onClick = { editingProvider = null }) { Text("Cancel") } },
        )
    }
}
