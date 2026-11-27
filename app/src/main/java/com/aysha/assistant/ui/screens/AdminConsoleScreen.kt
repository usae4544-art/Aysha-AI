package com.aysha.assistant.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CurrencyRupee
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.AdminUserRecord
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.StorePricingConfig
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.core.ui.StaggeredAnimatedEntry
import com.aysha.assistant.ui.AyshaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminConsoleScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val users by viewModel.adminUsers.collectAsState()
    val storePricing by viewModel.storePricing.collectAsState()
    val busy by viewModel.adminBusy.collectAsState()
    val feedback by viewModel.adminFeedback.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Users & Block, 1 = Prices & UPI
    var search by remember { mutableStateOf("") }
    var blockTarget by remember { mutableStateOf<AdminUserRecord?>(null) }
    var blockReasonDraft by remember { mutableStateOf("Policy / Payment Hold") }
    var grantTarget by remember { mutableStateOf<AdminUserRecord?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAdminDashboard()
    }

    if (!viewModel.isVerifiedOwnerAdmin()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Owner Administrator Access Only", color = c.error, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    val filteredUsers = users.filter { u ->
        search.isBlank() ||
            u.email.contains(search, ignoreCase = true) ||
            u.displayName.contains(search, ignoreCase = true) ||
            u.uid.contains(search, ignoreCase = true) ||
            u.deviceModel.contains(search, ignoreCase = true)
    }
    val blockedCount = users.count { it.blocked }
    val activePlanCount = users.count { !it.blocked && it.licenseStatus.equals("active", ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                Column {
                    Text("Owner Admin Control", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Logins, Block Users & Live Pricing", color = c.textSecondary, fontSize = 12.sp)
                }
            }
            IconButton(onClick = { viewModel.loadAdminDashboard() }, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Sync", tint = c.accentPrimary)
                }
            }
        }

        feedback?.let { msg ->
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 14.dp,
                borderColor = c.accentPrimary,
                contentPadding = PaddingValues(12.dp),
            ) {
                Text(msg, color = c.textPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Quick Metrics Row
        StaggeredAnimatedEntry(index = 0) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                    Column {
                        Text("Logged Users", color = c.textSecondary, fontSize = 11.sp)
                        Text("${users.size}", color = c.textPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                }
                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                    Column {
                        Text("Active Plans", color = c.textSecondary, fontSize = 11.sp)
                        Text("$activePlanCount", color = c.success, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                }
                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                    Column {
                        Text("Blocked", color = c.textSecondary, fontSize = 11.sp)
                        Text("$blockedCount", color = c.error, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassPill(
                text = "Users & Block (${users.size})",
                dotColor = if (activeTab == 0) c.accentPrimary else null,
                onClick = { activeTab = 0 },
                modifier = Modifier.weight(1f),
            )
            GlassPill(
                text = "Price Control (Kam/Jyada)",
                dotColor = if (activeTab == 1) c.accentSecondary else null,
                onClick = { activeTab = 1 },
                modifier = Modifier.weight(1f),
            )
        }

        AnimatedContent(
            targetState = activeTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "admin_tab_switch",
        ) { tab ->
            if (tab == 0) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text("Search email, name, device or UID...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (filteredUsers.isEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                            Text(
                                text = if (busy) "Loading user logins from Firestore..." else "No users found matching filter.",
                                color = c.textSecondary,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        filteredUsers.forEachIndexed { idx, user ->
                            StaggeredAnimatedEntry(index = idx.coerceAtMost(10)) {
                                val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                                val seenText = if (user.lastLoginAtMs > 0L) fmt.format(Date(user.lastLoginAtMs)) else "Unknown time"
                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 18.dp,
                                    elevated = !user.blocked,
                                    borderColor = if (user.blocked) c.error.copy(alpha = 0.7f) else null,
                                    contentPadding = PaddingValues(14.dp),
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                ColoredIconBadge(
                                                    icon = if (user.blocked) Icons.Rounded.Block else Icons.Rounded.People,
                                                    tintColor = if (user.blocked) c.error else c.accentPrimary,
                                                    size = 36.dp,
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = user.displayName.ifBlank { "User" },
                                                        color = c.textPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.5.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                    Text(
                                                        text = user.email.ifBlank { user.uid },
                                                        color = c.textSecondary,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                            GlassPill(
                                                text = if (user.blocked) "BLOCKED" else user.plan.label,
                                                dotColor = if (user.blocked) c.error else if (user.licenseStatus.equals("active", true)) c.success else c.warning,
                                            )
                                        }

                                        Text(
                                            text = "Source: ${user.lastLoginSource.uppercase()} · Seen: $seenText" +
                                                (if (user.deviceModel.isNotBlank()) " · ${user.deviceModel}" else "") +
                                                (if (user.appVersion.isNotBlank()) " · v${user.appVersion}" else ""),
                                            color = c.textSecondary,
                                            fontSize = 11.5.sp,
                                        )

                                        if (user.blocked && user.blockedReason.isNotBlank()) {
                                            Text(
                                                text = "Block reason: ${user.blockedReason}",
                                                color = c.error,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            OutlinedButton(
                                                onClick = { grantTarget = user },
                                                enabled = !busy,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Plan / Revoke", fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    if (user.blocked) {
                                                        viewModel.adminToggleBlockUser(user.uid, block = false)
                                                    } else {
                                                        blockReasonDraft = "Blocked by administrator"
                                                        blockTarget = user
                                                    }
                                                },
                                                enabled = !busy,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f),
                                            ) {
                                                Icon(
                                                    imageVector = if (user.blocked) Icons.Rounded.CheckCircle else Icons.Rounded.Block,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (user.blocked) "Unblock User" else "Block User", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Live Pricing & UPI Editor
                var upiIdDraft by remember(storePricing) { mutableStateOf(storePricing.upiId) }
                var upiNameDraft by remember(storePricing) { mutableStateOf(storePricing.upiName) }
                val priceDrafts = remember(storePricing) {
                    mutableStateMapOf<LicensePlan, Int>().apply {
                        listOf(
                            LicensePlan.ONE_MONTH,
                            LicensePlan.TWO_MONTHS,
                            LicensePlan.ONE_YEAR,
                            LicensePlan.TWO_YEARS,
                            LicensePlan.LIFETIME,
                        ).forEach { plan ->
                            put(plan, storePricing.prices[plan] ?: 0)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColoredIconBadge(Icons.Rounded.CurrencyRupee, c.accentPrimary, size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Live Plan Prices (INR) & UPI", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            OutlinedTextField(
                                value = upiIdDraft,
                                onValueChange = { upiIdDraft = it },
                                label = { Text("UPI ID (VPA)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = upiNameDraft,
                                onValueChange = { upiNameDraft = it },
                                label = { Text("Payee Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    priceDrafts.keys.toList().forEachIndexed { idx, plan ->
                        val amount = priceDrafts[plan] ?: 0
                        StaggeredAnimatedEntry(index = idx) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(plan.label, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("₹$amount INR", color = c.accentPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(onClick = { priceDrafts[plan] = (amount - 50).coerceAtLeast(0) }) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "-50", tint = c.error)
                                        }
                                        OutlinedTextField(
                                            value = amount.toString(),
                                            onValueChange = { txt ->
                                                val parsed = txt.filter(Char::isDigit).toIntOrNull() ?: 0
                                                priceDrafts[plan] = parsed.coerceIn(0, 1_000_000)
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.width(92.dp),
                                        )
                                        IconButton(onClick = { priceDrafts[plan] = (amount + 50).coerceAtMost(1_000_000) }) {
                                            Icon(Icons.Rounded.Add, contentDescription = "+50", tint = c.success)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AuroraGradientButton(
                        text = if (busy) "Saving..." else "Publish Live Prices to Cloud",
                        enabled = !busy,
                        onClick = {
                            viewModel.adminSaveStorePricing(
                                StorePricingConfig(
                                    upiId = upiIdDraft.trim(),
                                    upiName = upiNameDraft.trim(),
                                    currency = "INR",
                                    prices = priceDrafts.toMap(),
                                ),
                            )
                        },
                    )
                }
            }
        }
    }

    blockTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { blockTarget = null },
            title = { Text("Block ${target.displayName.ifBlank { target.email }}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Blocking immediately locks this Google account out of AYSHA AI APK and Website.",
                        fontSize = 13.sp,
                    )
                    OutlinedTextField(
                        value = blockReasonDraft,
                        onValueChange = { blockReasonDraft = it },
                        label = { Text("Reason (shown to user)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val reason = blockReasonDraft
                    blockTarget = null
                    viewModel.adminToggleBlockUser(target.uid, block = true, reason = reason)
                }) { Text("Confirm Block") }
            },
            dismissButton = {
                TextButton(onClick = { blockTarget = null }) { Text("Cancel") }
            },
        )
    }

    grantTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { grantTarget = null },
            title = { Text("Manage License · ${target.email.ifBlank { target.uid }}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        LicensePlan.ONE_MONTH,
                        LicensePlan.TWO_MONTHS,
                        LicensePlan.ONE_YEAR,
                        LicensePlan.TWO_YEARS,
                        LicensePlan.LIFETIME,
                    ).forEach { p ->
                        OutlinedButton(
                            onClick = {
                                grantTarget = null
                                viewModel.adminGrantOrRevokeUser(target.uid, target.email, p)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Grant ${p.label}")
                        }
                    }
                    Button(
                        onClick = {
                            grantTarget = null
                            viewModel.adminGrantOrRevokeUser(target.uid, target.email, LicensePlan.NONE)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Revoke Active License")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { grantTarget = null }) { Text("Close") }
            },
        )
    }
}
