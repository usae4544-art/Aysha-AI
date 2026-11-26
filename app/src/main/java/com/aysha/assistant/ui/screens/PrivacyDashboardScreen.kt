package com.aysha.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun PrivacyDashboardScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val perms by viewModel.permissionAudit.collectAsState()
    val grantedCount = perms.count { it.isGranted }
    val logs by viewModel.toolLogs.collectAsState()
    val backupFeedback by viewModel.backupFeedback.collectAsState()

    var showRestoreDialog by remember { mutableStateOf(false) }
    var showClearKeysConfirm by remember { mutableStateOf(false) }
    var wipeStep by remember { mutableStateOf(0) }
    var sosContactDraft by remember(settings.emergencyContact) { mutableStateOf(settings.emergencyContact) }
    var sosMessageDraft by remember(settings.emergencySosMessage) { mutableStateOf(settings.emergencySosMessage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Privacy, Backup & SOS", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = c.success)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Android Keystore AES-256-GCM Active", color = c.textPrimary, fontWeight = FontWeight.Bold)
                    Text("API keys stay encrypted on-device and are automatically redacted from logs & backups.", color = c.textSecondary, fontSize = 12.sp)
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.accentPrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Permissions Granted: $grantedCount / ${perms.size}", color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Sensitive permissions stay strictly under your control.", color = c.textSecondary, fontSize = 12.sp)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Device Lock Guard", color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Require screen PIN/Biometric when opening AYSHA", color = c.textSecondary, fontSize = 11.5.sp)
                    }
                    Switch(checked = settings.appLockEnabled, onCheckedChange = { on -> viewModel.updateSettings { it.copy(appLockEnabled = on) } })
                }
            }
        }

        // Emergency SOS Config Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, borderColor = c.error.copy(alpha = 0.4f), contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = c.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency SOS Setup", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                OutlinedTextField(
                    value = sosContactDraft,
                    onValueChange = { sosContactDraft = it },
                    label = { Text("Emergency Phone Number (default 112)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = sosMessageDraft,
                    onValueChange = { sosMessageDraft = it },
                    label = { Text("Emergency Alert Message") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            viewModel.updateSettings {
                                it.copy(emergencyContact = sosContactDraft.trim(), emergencySosMessage = sosMessageDraft.trim())
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save SOS") }
                    Button(
                        onClick = { viewModel.triggerSosConfirmation() },
                        colors = ButtonDefaults.buttonColors(containerColor = c.error),
                        modifier = Modifier.weight(1f),
                    ) { Text("Test SOS") }
                }
            }
        }

        // Backup & Restore Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Backup & Restore", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Export or restore memories, rules, documents, custom macros & theme (API keys never exported).", color = c.textSecondary, fontSize = 12.sp)
                AuroraGradientButton(text = "Export Full Safe Backup (JSON)", onClick = { viewModel.exportFullBackupAndShare() })
                OutlinedButton(onClick = { showRestoreDialog = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Restore Backup from JSON")
                }
                backupFeedback?.let {
                    Text(it, color = c.accentPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        OutlinedButton(onClick = { showClearKeysConfirm = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Text("Delete All Stored API Keys")
        }

        Button(
            onClick = { wipeStep = 1 },
            colors = ButtonDefaults.buttonColors(containerColor = c.error),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Wipe All Local Chats, Memories, Rules & Logs") }

        if (logs.isNotEmpty()) {
            Text("Recent Tool Activity Audit (${logs.size})", color = c.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            logs.take(15).forEach { log ->
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, contentPadding = PaddingValues(10.dp)) {
                    Column {
                        Text(
                            text = "${log.toolName} · ${if (log.success) "OK" else "Blocked/Error"}",
                            color = if (log.success) c.success else c.warning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(log.summary, color = c.textSecondary, fontSize = 11.5.sp)
                    }
                }
            }
        }
    }

    if (showRestoreDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Paste AYSHA Backup JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("aysha-backup-v2 JSON") },
                    minLines = 5,
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (jsonInput.isNotBlank()) viewModel.restoreFullBackup(jsonInput)
                    showRestoreDialog = false
                }) { Text("Restore") }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") } },
        )
    }

    if (showClearKeysConfirm) {
        AlertDialog(
            onDismissRequest = { showClearKeysConfirm = false },
            title = { Text("Saari API keys delete karein?") },
            text = { Text("Is phone se saari saved Gemini, Groq aur OpenAI keys hata di jayengi. Offline mode chalta rahega.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    onClick = {
                        viewModel.clearAllApiKeys()
                        showClearKeysConfirm = false
                    },
                ) { Text("Haan, delete karo") }
            },
            dismissButton = { TextButton(onClick = { showClearKeysConfirm = false }) { Text("Cancel") } },
        )
    }

    if (wipeStep == 1) {
        AlertDialog(
            onDismissRequest = { wipeStep = 0 },
            title = { Text("Saari jaankari mita dein?") },
            text = { Text("Tumhari saari chats, yaadein, documents, rules aur activity logs delete ho jayenge.") },
            confirmButton = {
                Button(onClick = { wipeStep = 2 }) { Text("Aage badho") }
            },
            dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text("Rehne do") } },
        )
    }

    if (wipeStep == 2) {
        AlertDialog(
            onDismissRequest = { wipeStep = 0 },
            title = { Text("Pakka sab mita dein?") },
            text = { Text("Aakhri check: ek baar wipe hone par ye local data wapas nahi aayega.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    onClick = {
                        viewModel.wipeAllLocalData()
                        wipeStep = 0
                    },
                ) { Text("Haan, sab bhula do") }
            },
            dismissButton = { TextButton(onClick = { wipeStep = 0 }) { Text("Rehne do") } },
        )
    }
}
