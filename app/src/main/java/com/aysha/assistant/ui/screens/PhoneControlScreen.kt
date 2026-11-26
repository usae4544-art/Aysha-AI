package com.aysha.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun PhoneControlScreen(
    viewModel: AyshaViewModel,
    onRequestRuntimePermission: (String) -> Unit,
    onOpenAutomation: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val perms by viewModel.permissionAudit.collectAsState()

    var showCallPrompt by remember { mutableStateOf(false) }
    var showSmsPrompt by remember { mutableStateOf(false) }
    var showPermissionsList by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

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
            Text("Phone Control", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Exact rows matching Screen 13
        PhoneControlRowCard(
            title = "Open Camera",
            subtitle = "Open Gallery",
            icon = Icons.Rounded.CameraAlt,
            badgeColor = Color(0xFF7C3AED),
            onPrimaryClick = { viewModel.runPhoneQuickAction("camera") },
            secondaryPill = "Gallery",
            onSecondaryClick = { viewModel.runPhoneQuickAction("gallery") },
        )

        PhoneControlRowCard(
            title = "Make a Call",
            subtitle = "Compose SMS",
            icon = Icons.Rounded.Call,
            badgeColor = Color(0xFF0EA5E9),
            onPrimaryClick = { showCallPrompt = true },
            secondaryPill = "SMS",
            onSecondaryClick = { showSmsPrompt = true },
        )

        PhoneControlRowCard(
            title = "Set Alarm",
            subtitle = "Open Settings",
            icon = Icons.Rounded.Alarm,
            badgeColor = Color(0xFF10B981),
            onPrimaryClick = { viewModel.runPhoneQuickAction("alarm") },
            secondaryPill = "Settings",
            onSecondaryClick = { viewModel.openSystemSettings("general") },
        )

        PhoneControlRowCard(
            title = "Open YouTube",
            subtitle = "Open Chrome",
            icon = Icons.Rounded.PlayArrow,
            badgeColor = Color(0xFFEF4444),
            onPrimaryClick = { viewModel.runPhoneQuickAction("youtube") },
            secondaryPill = "Chrome",
            onSecondaryClick = { viewModel.runPhoneQuickAction("chrome") },
        )

        PhoneControlRowCard(
            title = "Flashlight & Media",
            subtitle = "Notifications & Battery Info",
            icon = Icons.Rounded.FlashlightOn,
            badgeColor = Color(0xFFF59E0B),
            onPrimaryClick = { viewModel.runPhoneQuickAction("torch") },
            secondaryPill = "Notifications",
            onSecondaryClick = { viewModel.runPhoneQuickAction("notifications") },
        )

        // Floating Glass Assistant Switch
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, elevated = true, contentPadding = PaddingValues(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Floating Assistant Orb", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                    Text("Show draggable AYSHA glass orb on home screen", color = c.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.floatingOrbEnabled,
                    onCheckedChange = { enable -> viewModel.toggleFloatingOrb(enable) },
                )
            }
        }

        // Quick Battery & Automation strip
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill("Battery Info", onClick = { viewModel.runPhoneQuickAction("battery") })
            GlassPill("Media Play/Pause", onClick = { viewModel.runPhoneQuickAction("media") })
            GlassPill("Automation", onClick = onOpenAutomation)
        }

        // Bottom "Require Permission" + "Grant Permission" Button matching Screen 13
        Text(
            text = "Require Permission",
            color = c.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        AuroraGradientButton(
            text = if (showPermissionsList) "Hide Permission Manager" else "Grant Permission",
            icon = Icons.Rounded.CameraAlt,
            onClick = { showPermissionsList = !showPermissionsList },
        )

        AnimatedVisibility(visible = showPermissionsList) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(p.title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                    Text(p.purpose, color = c.textSecondary, fontSize = 11.sp)
                                }
                            }
                            GlassPill(
                                text = if (p.isGranted) "Granted" else "Grant",
                                dotColor = if (p.isGranted) c.success else c.warning,
                                onClick = { onRequestRuntimePermission(p.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCallPrompt) {
        var target by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCallPrompt = false },
            title = { Text("Make a Phone Call") },
            text = { OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Contact Name or Number") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    showCallPrompt = false
                    if (target.isNotBlank()) viewModel.requestCallConfirmation(target)
                }) { Text("Next") }
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
                    OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Text") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSmsPrompt = false
                    if (target.isNotBlank() && body.isNotBlank()) viewModel.requestSmsConfirmation(target, body)
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showSmsPrompt = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PhoneControlRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badgeColor: Color,
    onPrimaryClick: () -> Unit,
    secondaryPill: String,
    onSecondaryClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        onClick = onPrimaryClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ColoredIconBadge(icon = icon, tintColor = badgeColor, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = c.textSecondary, fontSize = 12.sp)
            }
            GlassPill(text = secondaryPill, onClick = onSecondaryClick)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
        }
    }
}
