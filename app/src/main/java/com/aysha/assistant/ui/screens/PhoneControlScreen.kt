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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun PhoneControlScreen(
    viewModel: AyshaViewModel,
    onRequestRuntimePermission: (String) -> Unit,
    onOpenAutomation: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val perms by viewModel.permissionAudit.collectAsState()

    var showCallPrompt by remember { mutableStateOf(false) }
    var showSmsPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

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
            Column {
                Text("Phone Control & Powers", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Official Android intents & permission-gated actions", color = c.textSecondary, fontSize = 12.sp)
            }
        }

        // Floating Glass Orb Card
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, elevated = true, contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Floating Glass Assistant Orb", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Draggable overlay with tap quick-menu & long-press voice", color = c.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.floatingOrbEnabled,
                    onCheckedChange = { enable -> viewModel.toggleFloatingOrb(enable) },
                )
            }
        }

        // Quick Actions Grid matching Screen 13
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneActionTile("Open Camera", "Launch Camera", Icons.Rounded.CameraAlt, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("camera") }
            PhoneActionTile("Open Gallery", "Browse Photos", Icons.Rounded.PhotoLibrary, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("gallery") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneActionTile("Make a Call", "Confirm first", Icons.Rounded.Call, Modifier.weight(1f)) { showCallPrompt = true }
            PhoneActionTile("Compose SMS", "Confirm first", Icons.Rounded.Sms, Modifier.weight(1f)) { showSmsPrompt = true }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneActionTile("Set Alarm", "7:00 AM Intent", Icons.Rounded.Alarm, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("alarm") }
            PhoneActionTile("Flashlight", "Toggle Torch", Icons.Rounded.FlashlightOn, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("torch") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneActionTile("Media Control", "Play / Pause", Icons.Rounded.PlayArrow, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("media") }
            PhoneActionTile("Notifications", "Read Recent", Icons.Rounded.NotificationsActive, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("notifications") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneActionTile("Battery Info", "Charge & Temp", Icons.Rounded.BatteryChargingFull, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("battery") }
            PhoneActionTile("Open Browser", "Google / YouTube", Icons.Rounded.Public, Modifier.weight(1f)) { viewModel.runPhoneQuickAction("youtube") }
        }

        Button(
            onClick = onOpenAutomation,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) { Text("Open Automation & Macros") }

        // Permission Manager
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Permission Manager", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            GlassPill("Refresh", onClick = { viewModel.refreshPermissions() })
        }

        perms.forEach { p ->
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = if (p.isGranted) Icons.Rounded.CheckCircle else Icons.Rounded.Security,
                            contentDescription = null,
                            tint = if (p.isGranted) c.success else c.warning,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(p.title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(p.purpose, color = c.textSecondary, fontSize = 11.5.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassPill(
                        text = if (p.isGranted) "Granted" else "Grant",
                        dotColor = if (p.isGranted) c.success else c.warning,
                        onClick = { onRequestRuntimePermission(p.id) },
                    )
                }
            }
        }
    }

    if (showCallPrompt) {
        var target by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCallPrompt = false },
            title = { Text("Make a Phone Call") },
            text = {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Contact Name or Phone Number") },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = {
                    showCallPrompt = false
                    if (target.isNotBlank()) viewModel.requestCallConfirmation(target)
                }) { Text("Next (Confirm)") }
            },
            dismissButton = { TextButton(onClick = { showCallPrompt = false }) { Text("Cancel") } },
        )
    }

    if (showSmsPrompt) {
        var target by remember { mutableStateOf("") }
        var body by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSmsPrompt = false },
            title = { Text("Compose SMS") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Contact Name or Number") }, singleLine = true)
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Body") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSmsPrompt = false
                    if (target.isNotBlank() && body.isNotBlank()) viewModel.requestSmsConfirmation(target, body)
                }) { Text("Next (Confirm)") }
            },
            dismissButton = { TextButton(onClick = { showSmsPrompt = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PhoneActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                Text(subtitle, color = c.textSecondary, fontSize = 11.sp)
            }
        }
    }
}
