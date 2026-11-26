package com.aysha.assistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun PrivacyDashboardScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val perms by viewModel.permissionAudit.collectAsState()
    val grantedCount = perms.count { it.isGranted }
    val logs by viewModel.toolLogs.collectAsState()

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
            Text("Privacy & Security Dashboard", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = c.success)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Android Keystore AES-256-GCM Active", color = c.textPrimary, fontWeight = FontWeight.Bold)
                    Text("API keys are encrypted in hardware-backed storage and automatically redacted from logs & backups.", color = c.textSecondary, fontSize = 12.sp)
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.accentPrimary)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Permissions Granted: $grantedCount / ${perms.size}", color = c.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Every sensitive permission (Mic, Accessibility, Notification Access, Calls, SMS, Overlay) stays strictly under your control.", color = c.textSecondary, fontSize = 12.sp)
                }
            }
        }

        OutlinedButton(
            onClick = { viewModel.shareSafeConfigBackup() },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export Safe Config Backup (No API Keys)") }

        OutlinedButton(
            onClick = { viewModel.clearAllApiKeys() },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Delete All Stored API Keys") }

        Button(
            onClick = { viewModel.wipeAllLocalData() },
            colors = ButtonDefaults.buttonColors(containerColor = c.error),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Wipe All Local Chats, Memories & Logs") }

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
}
