package com.aysha.assistant.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aysha.assistant.R
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.OrbStyle
import com.aysha.assistant.core.UserGender
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.ClayRecessedBox
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LiquidAuroraBackground
import com.aysha.assistant.core.ui.LocalAyshaColors
import kotlinx.coroutines.delay

@Composable
fun SplashOnboardingScreen(
    initialUserName: String = "Friend",
    initialGeminiKey: String = "",
    keyTesting: Boolean = false,
    keyTestResult: String? = null,
    keyTestOk: Boolean = false,
    onTestGeminiKey: (String) -> Unit = {},
    accessibilityConnected: Boolean = false,
    notificationConnected: Boolean = false,
    onRequestPermission: (String) -> Unit = {},
    onRequestAllRuntimePermissions: () -> Unit = {},
    onOpenSystemSettings: (String) -> Unit = {},
    onSaveGeminiKey: (String) -> Unit = {},
    onGetStarted: (name: String, gender: UserGender, lang: AssistantLanguage, orbStyle: OrbStyle) -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current

    // step 0 = brief splash, step 1 = Apni Gemini key daalo, step 2 = Permissions, step 3 = Background me zinda rakho
    var step by remember { mutableIntStateOf(0) }
    var geminiKeyDraft by remember { mutableStateOf(initialGeminiKey) }
    var nameDraft by remember { mutableStateOf(if (initialUserName == "Friend") "" else initialUserName) }
    var genderDraft by remember { mutableStateOf(UserGender.UNSPECIFIED) }
    var permTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(950L)
        step = 1
    }

    LiquidAuroraBackground {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "setup_wizard_3steps",
        ) { curStep ->
            if (curStep == 0) {
                Box(
                    modifier = Modifier.fillMaxSize().clickable { step = 1 },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.aysha_portrait),
                            contentDescription = "AYSHA",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(144.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .border(3.dp, Color.White, RoundedCornerShape(36.dp)),
                        )
                        Text("AYSHA", color = c.textPrimary, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Ek second… load ho raha hai 💗", color = c.textSecondary, fontSize = 14.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Header matching Screenshots 19–23: App icon + "AYSHA set up karo" + "Step X / 3" + 3-bar progress
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.aysha_portrait),
                                contentDescription = "AYSHA",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(12.dp)),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("AYSHA set up karo", color = c.textPrimary, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                Text("Step $curStep / 3", color = c.textSecondary, fontSize = 12.5.sp)
                            }
                        }
                        if (curStep > 1) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(26.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (bar in 1..3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(if (bar <= curStep) c.accentPrimary else c.glassBorder),
                            )
                        }
                    }

                    when (curStep) {
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("Apni Gemini key daalo", color = c.textPrimary, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "AYSHA ki awaaz aur dimaag Google ke Gemini par chalte hain. Key free hai aur lagbhag ek minute lagta hai.",
                                    color = c.textSecondary,
                                    fontSize = 13.5.sp,
                                )
                                SetupStepNumberRow(1, "Neeche \"Free key lo\" dabao. Google AI Studio khulega.")
                                SetupStepNumberRow(2, "Apne Google account se sign in karo.")
                                SetupStepNumberRow(3, "\"Create API key\" dabao. Koi bhi project chuno. Phir \"Copy\" dabao.")
                                SetupStepNumberRow(4, "AYSHA me wapas aao aur \"Paste karo\" dabao.")

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 22.dp,
                                    elevated = true,
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                            )
                                        }
                                    },
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Free key lo", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                ClayRecessedBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        BasicTextField(
                                            value = geminiKeyDraft,
                                            onValueChange = { geminiKeyDraft = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = c.textPrimary, fontSize = 14.5.sp),
                                            cursorBrush = SolidColor(c.accentPrimary),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { inner ->
                                                if (geminiKeyDraft.isEmpty()) Text("Apni key yahan daalo", color = c.textSecondary, fontSize = 14.5.sp)
                                                inner()
                                            },
                                        )
                                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                GlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    cornerRadius = 22.dp,
                                    elevated = true,
                                    onClick = {
                                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val txt = cb.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.trim()?.replace(Regex("\\s+"), "").orEmpty()
                                        if (txt.isEmpty()) {
                                            Toast.makeText(context, "Abhi kuch copy nahi hua hai. Pehle key copy karo.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            geminiKeyDraft = txt
                                            onTestGeminiKey(txt)
                                        }
                                    },
                                    contentPadding = PaddingValues(vertical = 14.dp),
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Key, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Paste & Connect karo", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    }
                                }

                                if (geminiKeyDraft.isNotBlank()) {
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 20.dp,
                                        borderColor = if (keyTestOk) c.success else null,
                                        onClick = { onTestGeminiKey(geminiKeyDraft.trim().replace(Regex("\\s+"), "")) },
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Text(
                                            text = when {
                                                keyTesting -> "Gemini API verify ho rahi hai..."
                                                keyTestResult != null -> keyTestResult
                                                else -> "Tap karke API key test aur connect karo"
                                            },
                                            color = when {
                                                keyTesting -> c.accentPrimary
                                                keyTestResult == null -> c.textSecondary
                                                keyTestOk -> c.success
                                                else -> c.error
                                            },
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }

                                Text(
                                    "Key free hai. Ye encrypted rehti hai aur sirf tumhare liye AYSHA chalane me use hoti hai.",
                                    color = c.textSecondary,
                                    fontSize = 12.5.sp,
                                )
                            }

                            AuroraGradientButton(
                                text = "-> Save karke aage badho",
                                enabled = geminiKeyDraft.isNotBlank(),
                                onClick = {
                                    val clean = geminiKeyDraft.trim().replace(Regex("\\s+"), "")
                                    onSaveGeminiKey(clean)
                                    step = 2
                                },
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Maine key pehle hi daal di hai",
                                    color = c.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { step = 2 }.padding(6.dp),
                                )
                                Text(
                                    "Abhi skip karo",
                                    color = c.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.clickable { step = 2 }.padding(6.dp),
                                )
                            }
                        }

                        2 -> {
                            fun hasPerm(p: String): Boolean {
                                val unused = permTick
                                return ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
                            }
                            val micOk = hasPerm(Manifest.permission.RECORD_AUDIO)
                            val notifOk = if (Build.VERSION.SDK_INT >= 33) hasPerm(Manifest.permission.POST_NOTIFICATIONS) else true
                            val camOk = hasPerm(Manifest.permission.CAMERA)
                            val callOk = hasPerm(Manifest.permission.CALL_PHONE)
                            val contactsOk = hasPerm(Manifest.permission.READ_CONTACTS)
                            val smsOk = hasPerm(Manifest.permission.SEND_SMS)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("AYSHA ko apna phone use karne do", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "Ek tap — Android ek ek karke sab poochega. Sirf microphone zaroori hai; baaki se wo kaam ki banti hai.",
                                    color = c.textSecondary,
                                    fontSize = 13.sp,
                                )
                                PermissionSetupCard(Icons.Rounded.Mic, "Microphone", "Taaki tum AYSHA se baat kar sako. Zaroori hai.", micOk) {
                                    onRequestPermission("mic")
                                    permTick++
                                }
                                PermissionSetupCard(Icons.Rounded.Notifications, "Notifications", "AYSHA ki apni notification, jo use background me chalu rakhti hai.", notifOk) {
                                    onRequestPermission("notifications")
                                    permTick++
                                }
                                PermissionSetupCard(Icons.Rounded.CameraAlt, "Camera", "Taaki wo photo le sake ya jo tum phone se dikhao wo dekh sake.", camOk) {
                                    onRequestPermission("camera")
                                    permTick++
                                }
                                PermissionSetupCard(Icons.Rounded.Call, "Phone calls", "Taaki wo tumhare liye call laga sake.", callOk) {
                                    onRequestPermission("call")
                                    permTick++
                                }
                                PermissionSetupCard(Icons.Rounded.Contacts, "Contacts", "Taaki sirf naam kaafi ho — number wo khud dhoondh legi.", contactsOk) {
                                    onRequestPermission("contacts")
                                    permTick++
                                }
                                PermissionSetupCard(Icons.Rounded.Sms, "SMS", "Taaki wo text message bhej sake.", smsOk) {
                                    onRequestPermission("sms")
                                    permTick++
                                }
                                PermissionSetupCard(Icons.Rounded.LocationOn, "Location", "Mausam, raasta, aur tum kahan ho.", false) { onOpenSystemSettings("general") }
                                PermissionSetupCard(Icons.Rounded.PhotoLibrary, "Gallery aur files", "Taaki wo koi photo ya file dhoondh kar bhej sake.", false) { onOpenSystemSettings("general") }
                                PermissionSetupCard(Icons.Rounded.Headset, "Call uthana aur sambhalna", "Taaki wo bata sake kaun call kar raha hai aur call utha sake.", callOk) { onRequestPermission("call") }
                                PermissionSetupCard(Icons.Rounded.Bluetooth, "Bluetooth", "Taaki uski awaaz tumhare earbuds ya speaker par aaye.", true) {}
                            }

                            AuroraGradientButton(
                                text = "✓ Sab allow karo",
                                onClick = {
                                    onRequestAllRuntimePermissions()
                                    permTick++
                                },
                            )
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 24.dp,
                                elevated = true,
                                onClick = { step = 3 },
                                contentPadding = PaddingValues(vertical = 14.dp),
                            ) {
                                Text("-> Aage badho", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.align(Alignment.Center))
                            }
                        }

                        else -> {
                            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                            val batteryOk = pm.isIgnoringBatteryOptimizations(context.packageName)
                            val overlayOk = Settings.canDrawOverlays(context)

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("AYSHA ko background me zinda rakho", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "Battery zaroori hai — isi se screen off hone par wo chup nahi hoti. Baaki optional hain.",
                                    color = c.textSecondary,
                                    fontSize = 13.sp,
                                )
                                PermissionSetupCard(Icons.Rounded.BatteryChargingFull, "Battery — optimisation band", "Zaroori hai. Iske bina phone background me band kar deta hai.", batteryOk) {
                                    onOpenSystemSettings("battery")
                                }
                                PermissionSetupCard(Icons.Rounded.Layers, "Dusre apps ke upar dikhna", "Taaki uska orb tum jo bhi kar rahe ho uske upar tair sake.", overlayOk) {
                                    onOpenSystemSettings("overlay")
                                }
                                PermissionSetupCard(Icons.Rounded.NotificationsActive, "Notification access", "Messages padhne aur kaun call kar raha hai ye jaanne ke liye.", notificationConnected) {
                                    onOpenSystemSettings("notifications")
                                }
                                PermissionSetupCard(Icons.Rounded.AccessibilityNew, "Accessibility service", "Taaki wo apps khol sake, tap kar sake aur screen padh sake.", accessibilityConnected) {
                                    onOpenSystemSettings("accessibility")
                                }

                                // Quick Name + Gender card before finishing
                                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp, contentPadding = PaddingValues(14.dp)) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Tumhara naam & pehchan", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        ClayRecessedBox(modifier = Modifier.fillMaxWidth(), cornerRadius = 16.dp, contentPadding = PaddingValues(12.dp)) {
                                            BasicTextField(
                                                value = nameDraft,
                                                onValueChange = { nameDraft = it },
                                                singleLine = true,
                                                textStyle = TextStyle(color = c.textPrimary, fontSize = 14.sp),
                                                cursorBrush = SolidColor(c.accentPrimary),
                                                decorationBox = { inner ->
                                                    if (nameDraft.isEmpty()) Text("Apna naam likho (e.g. Rahul)", color = c.textSecondary, fontSize = 14.sp)
                                                    inner()
                                                },
                                            )
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            UserGender.entries.forEach { g ->
                                                GlassPill(
                                                    text = g.label,
                                                    dotColor = if (genderDraft == g) c.accentPrimary else null,
                                                    onClick = { genderDraft = g },
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            AuroraGradientButton(
                                text = "✓ Ho gaya",
                                onClick = {
                                    onGetStarted(
                                        nameDraft.trim().ifBlank { initialUserName.ifBlank { "Friend" } },
                                        genderDraft,
                                        AssistantLanguage.HINGLISH,
                                        OrbStyle.RED_HUD,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepNumberRow(num: Int, text: String) {
    val c = LocalAyshaColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(c.accentPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(num.toString(), color = c.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = c.textPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PermissionSetupCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        elevated = true,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (granted) Color(0xFFDCFCE7) else Color(0xFFDBEAFE)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) Color(0xFF16A34A) else Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = c.textSecondary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (granted) {
                Icon(Icons.Rounded.Check, contentDescription = "Allowed", tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp))
            } else {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFF3E4CE))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Baaki hai", color = Color(0xFFB46B22), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
