package com.aysha.assistant.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.BuildConfig
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import com.aysha.assistant.ui.components.MayaTopBar

@Composable
fun NotificationsScreen(
    viewModel: AyshaViewModel,
    noticesCleared: Boolean,
    onClearAll: () -> Unit,
    onOpenTarget: (SettingsSubRoute) -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val notices = if (noticesCleared) emptyList() else listOf(
        Triple(
            "${settings.assistantName} ${BuildConfig.VERSION_NAME}: Soft 3D Clay UI & Neural Orb",
            "Naya Clay Day 3D design, 5-tab floating bottom dock, left navigation drawer aur 3D Neural Constellation orb live ho gaya hai. Appearance Studio se Dark ya Midnight bhi chuno.",
            "Appearance dekho" to SettingsSubRoute.APPEARANCE,
        ),
        Triple(
            "Secret Settings & 18+ Age Verification Guard",
            "Friendly, Romantic aur Intense (18+) levels ke saath Birth-Year verification, 2-step confirmation aur instant 'normal baat karo' reset add kar diya gaya hai.",
            "Secret settings kholo" to SettingsSubRoute.SECRET_SETTINGS,
        ),
        Triple(
            "Accessibility & Payment Guard + Unique License Keys",
            "Payment apps ke liye Accessibility quick toggle, Xiaomi/Oppo Autostart guide, Google Sign-In auto unlock aur unique license key sync active hain.",
            "Accessibility dekho" to SettingsSubRoute.ACCESSIBILITY_FIX,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MayaTopBar(
            title = "Notifications",
            showMenuInsteadOfBack = false,
            unreadNotifications = !noticesCleared,
            onLeadingClick = onBack,
            onNotificationsClick = {},
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${notices.size} notifications", color = c.textSecondary, fontSize = 12.5.sp)
                if (notices.isNotEmpty()) {
                    Text(
                        text = "Sab hatao",
                        color = c.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onClearAll),
                    )
                }
            }

            if (notices.isEmpty()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(20.dp)) {
                    Text(
                        "Abhi koi notification nahi. Kuch naya aayega to yahin dikhega.",
                        color = c.textSecondary,
                        fontSize = 13.5.sp,
                    )
                }
            } else {
                notices.forEachIndexed { idx, (title, body, cta) ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = if (idx == 0) Icons.Rounded.LocalOffer else Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = c.accentPrimary,
                                    modifier = Modifier.size(19.dp).padding(top = 2.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Text(body, color = c.textSecondary, fontSize = 13.5.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(if (idx == 0) "Abhi" else "$idx din pehle", color = c.textSecondary, fontSize = 11.5.sp)
                                Text(
                                    text = cta.first,
                                    color = c.accentPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { onOpenTarget(cta.second) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccessibilityFixScreen(
    viewModel: AyshaViewModel,
    onNotifications: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val accConnected by viewModel.accessibilityConnected.collectAsState()
    val adbCmd = "adb shell pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MayaTopBar(
            title = "Accessibility",
            showMenuInsteadOfBack = false,
            onLeadingClick = onBack,
            onNotificationsClick = onNotifications,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.AccessibilityNew,
                        contentDescription = null,
                        tint = if (accConnected) c.success else Color(0xFFF59E0B),
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (accConnected) "Accessibility chalu hai" else "Accessibility band hai",
                            color = c.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (accConnected) {
                                "${settings.assistantName} ab apps khol sakti hai, tap aur scroll kar sakti hai."
                            } else {
                                "${settings.assistantName} baat kar sakti hai, par ON hone tak dusre apps me tap ya padh nahi sakti."
                            },
                            color = c.textSecondary,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            AuroraGradientButton(
                text = if (accConnected) "Accessibility Settings Kholo" else "ON karo",
                icon = Icons.Rounded.BatteryChargingFull,
                onClick = { viewModel.openSystemSettings("accessibility") },
            )

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Jo screen khulegi usme: ${settings.assistantName} pe tap karo, switch ON karo, phir Allow dabao.",
                        color = c.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Text(
                "BAAR BAAR BAND HO RAHI HAI?",
                color = c.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Notification panel ki \"Active apps\" list me Stop mat dabao — usse ${settings.assistantName} ke saath uski accessibility bhi band ho jaati hai. Paytm/PhonePe kholte waqt yahin se pause kar sakte ho.",
                        color = c.textSecondary,
                        fontSize = 12.5.sp,
                    )
                    A11yTroubleshootingRow(
                        icon = Icons.Rounded.BatteryChargingFull,
                        title = "Battery: ${settings.assistantName} ko optimise mat karo",
                        subtitle = "Taaki phone use background me band na kare",
                        onClick = { viewModel.openSystemSettings("battery") },
                    )
                    A11yTroubleshootingRow(
                        icon = Icons.Rounded.Settings,
                        title = "Autostart",
                        subtitle = "Xiaomi, Oppo, realme, vivo, Infinix ki apni list hoti hai",
                        onClick = { viewModel.openSystemSettings("general") },
                    )
                    A11yTroubleshootingRow(
                        icon = Icons.Rounded.Lightbulb,
                        title = "Switch grey dikh raha hai?",
                        subtitle = "App info -> 3 dots (upar right) -> Allow restricted settings, phir dobara try karo",
                        onClick = { viewModel.openSystemSettings("accessibility") },
                    )
                }
            }

            Text(
                "ONE-TAP FIX (ADVANCED)",
                color = c.textSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            )

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Android kisi app ko accessibility wapas ON karne deta hai sirf ek permission se, jo ek baar computer se (ya root se) di jaati hai:",
                        color = c.textSecondary,
                        fontSize = 12.5.sp,
                    )
                    Text(
                        adbCmd,
                        color = c.textPrimary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 18.dp,
                            onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cb.setPrimaryClip(ClipData.newPlainText("ADB", adbCmd))
                                Toast.makeText(context, "ADB command copied!", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(vertical = 11.dp, horizontal = 10.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Command ...", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        GlassCard(
                            modifier = Modifier.weight(1f),
                            cornerRadius = 18.dp,
                            onClick = {
                                val rootTry = runCatching {
                                    val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "pm grant ${BuildConfig.APPLICATION_ID} android.permission.WRITE_SECURE_SETTINGS"))
                                    p.waitFor() == 0
                                }.getOrDefault(false)
                                Toast.makeText(
                                    context,
                                    if (rootTry) "Permission granted via Root!" else "Root available nahi — PC se ADB command chalao",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            contentPadding = PaddingValues(vertical = 11.dp, horizontal = 10.dp),
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Key, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Root se do", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun A11yTroubleshootingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColoredIconBadge(icon = icon, tintColor = c.accentPrimary, size = 36.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            Text(subtitle, color = c.textSecondary, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
    }
}

@Composable
fun GamesScreen(
    viewModel: AyshaViewModel,
    onOpenChat: () -> Unit,
    onNotifications: () -> Unit,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val license by viewModel.licenseInfo.collectAsState()
    var selectedGame by remember { mutableStateOf("Ludo") }
    var activeBoard by remember { mutableStateOf(false) }

    // Interactive 3x3 Tic-Tac-Toe quick arena
    val board = remember { mutableStateListOf("", "", "", "", "", "", "", "", "") }
    var winnerText by remember { mutableStateOf<String?>(null) }

    fun checkWinner(): String? {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6),
        )
        for (l in lines) {
            val a = board[l[0]]
            if (a.isNotEmpty() && a == board[l[1]] && a == board[l[2]]) return a
        }
        if (board.all { it.isNotEmpty() }) return "Draw"
        return null
    }

    fun playMove(idx: Int) {
        if (board[idx].isNotEmpty() || winnerText != null) return
        board[idx] = "X"
        winnerText = checkWinner()
        if (winnerText == null) {
            val openIdx = board.indices.filter { board[it].isEmpty() }.randomOrNull()
            if (openIdx != null) {
                board[openIdx] = "O"
                winnerText = checkWinner()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E0D24))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MayaTopBar(
            title = "Games with ${settings.assistantName}",
            showMenuInsteadOfBack = false,
            onLeadingClick = onBack,
            onNotificationsClick = onNotifications,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1A183D))
                    .border(1.dp, Color(0xFF39366E), RoundedCornerShape(18.dp))
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("GAME ZONE", color = Color(0xFFF8FAFC), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(
                                text = if (license.isUnlocked()) "${settings.userName} · Pro Arena Unlocked" else "License se online match khulta hai",
                                color = Color(0xFFA5B4FC),
                                fontSize = 12.sp,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text("READY", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text("RANDOM MATCH", color = Color(0xFFFACC15), fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                    Text("Kisi ke bhi saath, ya ${settings.assistantName} ke saath", color = Color(0xFFCBD5E1), fontSize = 12.sp)

                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Ludo", "Chess", "Saanp", "Carrom").forEach { g ->
                            val active = selectedGame == g
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) Color(0xFF2DD4BF) else Color(0xFF23204F))
                                    .clickable { selectedGame = g }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Text(g, color = if (active) Color(0xFF0A1428) else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFACC15))
                            .clickable {
                                activeBoard = true
                                viewModel.sendTextMessage("$selectedGame khelte hain! Pehli chaal chalo.", speakReply = true)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF14122D))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PLAY ${selectedGame.uppercase()}", color = Color(0xFF14122D), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                    }
                }
            }

            if (activeBoard) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF1B1940))
                        .border(1.dp, Color(0xFF4F46E5), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = when (winnerText) {
                                "X" -> "Tum jeet gaye! 🎉"
                                "O" -> "${settings.assistantName} jeet gayi! 😎"
                                "Draw" -> "Match Draw!"
                                else -> "Mini Arena vs ${settings.assistantName} (X vs O)"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        for (r in 0..2) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (col in 0..2) {
                                    val i = r * 3 + col
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF2A275A))
                                            .clickable { playMove(i) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            board[i],
                                            color = if (board[i] == "X") Color(0xFF38BDF8) else Color(0xFFF43F5E),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {
                                for (i in 0..8) board[i] = ""
                                winnerText = null
                            }) { Text("Reset") }
                            Button(
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DD4BF)),
                                onClick = onOpenChat,
                            ) { Text("Chat me Khelo", color = Color(0xFF0A1428)) }
                        }
                    }
                }
            }

            Text("GAMES", color = Color(0xFFA5B4FC), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            val games = listOf(
                Triple("Ludo", "2 se 4 player", Color(0xFF2563EB)),
                Triple("Chess", "Rating 200 se", Color(0xFF7C3AED)),
                Triple("Snake & Ladder", "2 se 4 player", Color(0xFF15803D)),
                Triple("Carrom", "1 vs 1", Color(0xFF92400E)),
            )
            games.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { (title, sub, col) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF1A183D))
                                .border(1.dp, Color(0xFF34316B), RoundedCornerShape(18.dp))
                                .padding(12.dp),
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(col.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(title.first().toString(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(sub, color = Color(0xFFA5B4FC), fontSize = 11.5.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF3B82F6))
                                            .clickable {
                                                viewModel.sendTextMessage("${settings.assistantName}, mere saath $title khelo! Rules batao aur shuru karo.", speakReply = true)
                                                onOpenChat()
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text(settings.assistantName, color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF2DD4BF))
                                            .clickable { activeBoard = true }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("Arena", color = Color(0xFF0A1428), fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppLanguageScreen(
    viewModel: AyshaViewModel,
    onNotifications: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val choices = listOf(
        Triple("Baat-cheet wali bhasha ke saath (${settings.language.label.substringBefore(" ")})", AssistantLanguage.HINGLISH, true),
        Triple("Hinglish", AssistantLanguage.HINGLISH, false),
        Triple("English (India)", AssistantLanguage.ENGLISH, false),
        Triple("हिन्दी (Hindi)", AssistantLanguage.HINDI, false),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MayaTopBar(
            title = "App ki bhasha",
            showMenuInsteadOfBack = false,
            onLeadingClick = onBack,
            onNotificationsClick = onNotifications,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Screens aur buttons ki bhasha", color = c.textSecondary, fontSize = 13.sp)
            choices.forEachIndexed { idx, (label, lang, _) ->
                val selected = (idx == 0 && settings.language == AssistantLanguage.HINGLISH) ||
                    (idx > 0 && settings.language == lang && !(idx == 1 && settings.language == AssistantLanguage.HINGLISH))
                val isActive = if (idx == 0) settings.language == AssistantLanguage.HINGLISH else idx > 1 && settings.language == lang
                val shape = RoundedCornerShape(22.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(
                            if (isActive) Brush.verticalGradient(listOf(Color(0xFF60B2FF), Color(0xFF2D85FF)))
                            else Brush.verticalGradient(listOf(c.glassSurfaceElevated, c.glassSurface)),
                            shape,
                        )
                        .border(1.dp, if (isActive) Color.White.copy(alpha = 0.55f) else c.glassBorder, shape)
                        .clickable { viewModel.updateSettings { it.copy(language = lang) } }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(label, color = if (isActive) Color(0xFF0A1428) else c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (isActive) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerToolWorkspaceScreen(
    title: String,
    route: SettingsSubRoute,
    viewModel: AyshaViewModel,
    onOpenChat: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenMemory: () -> Unit,
    onNotifications: () -> Unit,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val license by viewModel.licenseInfo.collectAsState()
    val logs by viewModel.toolLogs.collectAsState()
    var promptDraft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MayaTopBar(
            title = if (route == SettingsSubRoute.SMART_HOME) "${settings.assistantName} Home" else title,
            showMenuInsteadOfBack = false,
            onLeadingClick = onBack,
            onNotificationsClick = onNotifications,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (route) {
                SettingsSubRoute.SMART_HOME -> {
                    Text(
                        "Is WiFi pe apne ${settings.assistantName} smart boxes ya voice devices connect karo aur command dekar control karo.",
                        color = c.textSecondary,
                        fontSize = 13.5.sp,
                    )
                    AuroraGradientButton(
                        text = "Scan Wi-Fi Devices...",
                        icon = Icons.Rounded.Router,
                        onClick = { viewModel.sendTextMessage("Nearby smart home lights aur Wi-Fi devices check karo.") },
                    )
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColoredIconBadge(Icons.Rounded.Bluetooth, c.accentPrimary, size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Abhi koi hardware box add nahi hua.", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            Text("Voice se bhi smart routines chalao jaise: \"Lights off karo\" ya \"Morning routine chalu karo\".", color = c.textSecondary, fontSize = 12.5.sp)
                        }
                    }
                }

                SettingsSubRoute.TERMINAL -> {
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColoredIconBadge(Icons.Rounded.Terminal, Color(0xFF0EA5E9), size = 34.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Command & Automation Console", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                            OutlinedTextField(
                                value = promptDraft,
                                onValueChange = { promptDraft = it },
                                label = { Text("Ask command / Termux / ADB help") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AuroraGradientButton(text = "Run / Generate Command", onClick = {
                                if (promptDraft.isNotBlank()) {
                                    viewModel.sendTextMessage("Mujhe is kaam ke liye exact terminal command aur steps do: $promptDraft", speakReply = false)
                                    onOpenChat()
                                }
                            })
                        }
                    }
                    Text("Recent Execution Logs (${logs.size})", color = c.textSecondary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    if (logs.isEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                            Text("Abhi koi automation log nahi.", color = c.textSecondary, fontSize = 13.sp)
                        }
                    } else {
                        logs.take(12).forEach { log ->
                            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                                Text("${log.toolName}: ${log.summary}", color = c.textPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
                            }
                        }
                    }
                }

                SettingsSubRoute.LICENSE_INFO -> {
                    val storePricing by viewModel.storePricing.collectAsState()
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(48.dp))
                        Text(
                            text = if (license.isUnlocked()) "${settings.assistantName} Pro active hai" else "${settings.assistantName} Pro unlock karo",
                            color = c.textPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Account: ${license.email.ifBlank { "Google Sign-In" }}\nPlan: ${license.plan.label}\nKey: ${license.licenseKey.ifBlank { "Web portal synced" }}",
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Live Plans (${storePricing.currency})", color = c.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                                storePricing.prices.forEach { (plan, price) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(plan.label, color = c.textPrimary, fontSize = 13.sp)
                                        Text("₹$price", color = c.success, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        AuroraGradientButton(text = "Refresh License Status", onClick = { viewModel.refreshLicenseStatus(silent = false) })
                        GlassPill(text = "Manage / Coupon Portal", dotColor = c.accentPrimary) { viewModel.openLicensePortalInBrowser() }
                    }
                }

                else -> {
                    val isPhoto = route == SettingsSubRoute.PHOTOS
                    val isDoc = route == SettingsSubRoute.DOCUMENTS
                    val isCode = route == SettingsSubRoute.WEBSITE_CODING
                    val icon = when {
                        isPhoto -> Icons.Rounded.AutoAwesome
                        isCode -> Icons.Rounded.Code
                        route == SettingsSubRoute.MARKETS -> Icons.Rounded.TrendingUp
                        else -> Icons.Rounded.Storefront
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(16.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ColoredIconBadge(icon, c.accentPrimary, size = 38.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                            Text(
                                when {
                                    isPhoto -> "${settings.assistantName} se bolo \"ek photo samjhao\" ya Scanner se camera vision chalao."
                                    isDoc -> "${settings.assistantName} ke banaye notes, study sheets aur saved resume/documents ek jagah."
                                    isCode -> "Website HTML/React, Kotlin Android ya Python script turant generate aur explain karwao."
                                    else -> "${settings.assistantName} se live analysis, captions, plans aur insights poochho."
                                },
                                color = c.textSecondary,
                                fontSize = 13.sp,
                            )
                            OutlinedTextField(
                                value = promptDraft,
                                onValueChange = { promptDraft = it },
                                label = { Text("$title ke liye request likho") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            AuroraGradientButton(text = if (isDoc) "Save Note & Ask ${settings.assistantName}" else "${settings.assistantName} se Poochho", onClick = {
                                if (isDoc && promptDraft.isNotBlank()) {
                                    viewModel.addQuickNote("Quick Note", promptDraft)
                                }
                                val req = promptDraft.ifBlank { "$title me meri help karo (${settings.userName})" }
                                viewModel.sendTextMessage(req, speakReply = false)
                                onOpenChat()
                            })
                            if (isPhoto) {
                                GlassPill(text = "Visual Camera Scanner Kholo", dotColor = c.accentPrimary, onClick = onOpenScanner)
                            } else if (isDoc) {
                                val savedDocs by viewModel.documents.collectAsState()
                                GlassPill(text = "Save as Note (${savedDocs.size} Saved)", dotColor = c.success) {
                                    if (promptDraft.isNotBlank()) {
                                        viewModel.addQuickNote("Note", promptDraft)
                                        promptDraft = ""
                                    }
                                }
                                GlassPill(text = "Indexed Documents & Yaadein Kholo", dotColor = c.accentPrimary, onClick = onOpenMemory)
                                savedDocs.take(5).forEach { doc ->
                                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 14.dp, contentPadding = PaddingValues(10.dp)) {
                                        Text("${doc.title}: ${doc.content.take(120)}", color = c.textPrimary, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
