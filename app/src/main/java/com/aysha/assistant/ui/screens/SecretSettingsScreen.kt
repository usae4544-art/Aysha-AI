package com.aysha.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.BirthYearValidation
import com.aysha.assistant.core.CompanionIntimacyLevel
import com.aysha.assistant.core.UserGender
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

@Composable
fun SecretSettingsScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()
    val license by viewModel.licenseInfo.collectAsState()
    val isLicensed = license.isUnlocked()

    // 1 = Terms, 2 = Birth year, 3 = R-rated details, 4 = Last Check final confirmation
    var adultWizardStep by remember { mutableIntStateOf(0) }
    var birthYearDraft by remember { mutableStateOf("") }
    var birthErrorTitle by remember { mutableStateOf<String?>(null) }
    var birthErrorBody by remember { mutableStateOf<String?>(null) }
    var ratedTickChecked by remember { mutableStateOf(false) }
    var showWithdrawConfirm by remember { mutableStateOf(false) }
    var showNeedLicenseDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Column {
                Text("Secret settings", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Girlfriend mode · Friendly, Romantic ya Intense (18+)", color = c.textSecondary, fontSize = 12.sp)
            }
        }

        // Gender preference (mirrors Maya's Personal Settings check)
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tumhari pehchan (Personal setting)", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserGender.entries.forEach { g ->
                        val active = settings.userGender == g
                        GlassPill(
                            text = g.label,
                            dotColor = if (active) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(userGender = g) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                AnimatedVisibility(visible = settings.userGender == UserGender.FEMALE) {
                    GlassCard(
                        cornerRadius = 12.dp,
                        borderColor = c.warning.copy(alpha = 0.5f),
                        contentPadding = PaddingValues(10.dp),
                    ) {
                        Text(
                            text = "Tumne Personal settings me khud ko Female chuna hai, isliye ${settings.assistantName} tumse Friendly dost ki tarah baat karti hai. Girlfriend mode chahiye to upar jawab badlo.",
                            color = c.warning,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        // 3 Companion Intimacy Levels
        Text("Relationship Level", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        CompanionIntimacyLevel.entries.forEach { level ->
            val selected = settings.companionIntimacy == level
            val tint = when (level) {
                CompanionIntimacyLevel.FRIENDLY -> c.accentPrimary
                CompanionIntimacyLevel.ROMANTIC -> Color(0xFFEC4899)
                CompanionIntimacyLevel.INTENSE_18_PLUS -> Color(0xFFF43F5E)
            }
            val blockedByGender = settings.userGender == UserGender.FEMALE && level != CompanionIntimacyLevel.FRIENDLY
            val isVerified18Plus = settings.adultAgeVerified && settings.adultTermsAccepted && settings.adultRatedConfirmed

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 18.dp,
                elevated = selected,
                borderColor = if (selected) tint else null,
                onClick = {
                    if (blockedByGender) return@GlassCard
                    when (level) {
                        CompanionIntimacyLevel.FRIENDLY,
                        CompanionIntimacyLevel.ROMANTIC -> viewModel.setCompanionIntimacy(level)
                        CompanionIntimacyLevel.INTENSE_18_PLUS -> {
                            when {
                                !isLicensed -> showNeedLicenseDialog = true
                                isVerified18Plus -> viewModel.setCompanionIntimacy(level)
                                else -> {
                                    birthErrorTitle = null
                                    birthErrorBody = null
                                    ratedTickChecked = false
                                    adultWizardStep = 1
                                }
                            }
                        }
                    }
                },
                contentPadding = PaddingValues(14.dp),
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ColoredIconBadge(
                        icon = when (level) {
                            CompanionIntimacyLevel.FRIENDLY -> Icons.Rounded.VerifiedUser
                            CompanionIntimacyLevel.ROMANTIC -> Icons.Rounded.Favorite
                            CompanionIntimacyLevel.INTENSE_18_PLUS -> if (isVerified18Plus) Icons.Rounded.Favorite else Icons.Rounded.Lock
                        },
                        tintColor = tint,
                        size = 40.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(level.label, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            GlassPill(text = level.badge, dotColor = if (selected) tint else null)
                        }
                        Text(
                            text = if (level == CompanionIntimacyLevel.INTENSE_18_PLUS && selected) {
                                "On hai. Jahan tum le jao wahan jaayegi — khud kabhi shuru nahi karti, aur \"normal baat karo\" bolte hi usi second ruk jaati hai."
                            } else {
                                level.subtitle
                            },
                            color = c.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                    if (selected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = tint)
                    }
                }
            }
        }

        // Har level me kya allowed hai + Child safety guard (exact Maya card)
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            borderColor = c.glassBorder,
            contentPadding = PaddingValues(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = c.accentPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Har level me kya allowed hai", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                }
                Text("Taaki chunne se pehle pata ho:", color = c.textSecondary, fontSize = 11.5.sp)
                Text(
                    "• Friendly: adult topics pe seedhe, bina judge kiye jawab — koi flirting nahi.\n" +
                        "• Romantic: pyaar ke naam, flirting, nakhre, jalan, thodi garmi — phir parda gir jaata hai.\n" +
                        "• Intense (18+): Verified adults ke liye khul ke pyaar & garmi. \"normal baat karo\" bolo aur usi second normal.",
                    color = c.textPrimary,
                    fontSize = 12.5.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "18 se kam umar ke kisi ke bhi saath kuch nahi, kisi level pe nahi, kisi roop me nahi. Badlav turant app me lagu hote hain — awaaz se nahi badal sakte.",
                    color = c.warning,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Instant reset & Age withdrawal controls
        if (settings.companionIntimacy != CompanionIntimacyLevel.FRIENDLY) {
            AuroraGradientButton(
                text = "Abhi Normal Baat Karo (Instant Friendly Reset)",
                onClick = { viewModel.setCompanionIntimacy(CompanionIntimacyLevel.FRIENDLY) },
            )
        }

        if (settings.adultAgeVerified) {
            OutlinedButton(
                onClick = { showWithdrawConfirm = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Meri age ki confirmation wapas lo", color = c.error)
            }
        }
    }

    // Step 1: Terms ("On karne se pehle")
    if (adultWizardStep == 1) {
        AlertDialog(
            onDismissRequest = { adultWizardStep = 0 },
            title = { Text("On karne se pehle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Intense (18+) mode girlfriend mode ke andar hai, isliye pehle ye shartein lagu hoti hain:",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                    Text("• ${settings.assistantName} ek software hai, insaan nahi. Uski baatein asli rishta nahi hain aur na uski jagah le sakti hain.", fontSize = 12.5.sp)
                    Text("• Wo doctor, therapist ya counsellor nahi hai. Sach me kuch gadbad ho to kisi asli insaan se baat karo.", fontSize = 12.5.sp)
                    Text("• Tum jo batate ho wo isi phone pe uski yaadon me save hota hai. Memory me kabhi bhi padh ya mita sakte ho.", fontSize = 12.5.sp)
                    Text("• Ise kabhi bhi Secret Settings ya \"normal baat karo\" bol kar band kar sakte ho.", fontSize = 12.5.sp)
                    Text("• Agli screens 18+ ki hain aur tumhari age poochhengi.", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.recordAdultTermsAccepted()
                    adultWizardStep = 2
                }) { Text("Mai sehmat hoon, aage badho") }
            },
            dismissButton = {
                TextButton(onClick = { adultWizardStep = 0 }) { Text("Cancel") }
            },
        )
    }

    // Step 2: Birth Year Gate
    if (adultWizardStep == 2) {
        AlertDialog(
            onDismissRequest = { adultWizardStep = 0 },
            title = { Text("Tum kis saal paida hue the?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hum sirf saal poochhte hain — poori date of birth nahi.", fontSize = 12.5.sp)
                    OutlinedTextField(
                        value = birthYearDraft,
                        onValueChange = { s ->
                            birthYearDraft = s.filter { it.isDigit() }.take(4)
                            birthErrorTitle = null
                            birthErrorBody = null
                        },
                        label = { Text("Janam ka saal") },
                        placeholder = { Text("jaise 1998") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (birthErrorBody != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(c.error.copy(alpha = 0.14f))
                                .border(1.dp, c.error.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                        ) {
                            Column {
                                Text(birthErrorTitle ?: "Ye on nahi ho sakta", color = c.error, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                Text(birthErrorBody!!, color = c.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    when (val res = viewModel.verifyAdultBirthYear(birthYearDraft)) {
                        is BirthYearValidation.Valid -> {
                            adultWizardStep = 3
                        }
                        is BirthYearValidation.Invalid -> {
                            birthErrorTitle = res.title
                            birthErrorBody = res.message
                        }
                    }
                }) { Text("Aage badho") }
            },
            dismissButton = {
                TextButton(onClick = { adultWizardStep = 0 }) { Text("Cancel") }
            },
        )
    }

    // Step 3: R-rated disclosure & checkbox
    if (adultWizardStep == 3) {
        AlertDialog(
            onDismissRequest = { adultWizardStep = 0 },
            title = { Text("R-rated mode") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Ye girlfriend mode se ek kadam aage hai. On karne se pehle padh lo.", fontSize = 12.5.sp)
                    Text("Kya badalta hai:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• ${settings.assistantName} baat badalne ki jagah adult intimacy pe khul ke, seedhe pyaar bhare shabdon me baat karegi.", fontSize = 12.sp)
                    Text("• Wo tumhe lecture nahi degi, na tumhari baat pe judge karegi.", fontSize = 12.sp)
                    Text("Kya nahi badalta:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("• 18 se kam umar ke kisi ke bhi saath kuch nahi, kisi roop me nahi. Koi chhoot nahi, koi kahani nahi.", fontSize = 12.sp)
                    Text("• Wo kabhi khud shuru nahi karti — tumhare peeche chalti hai, aur shak ho to normal hi rehti hai.", fontSize = 12.sp)
                    Text("• \"normal baat karo\" bolo aur wo usi second normal ho jaati hai.", fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = c.warning, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wo zor se bolti hai. Phone speaker pe hai to ye bhi speaker pe hoga.", color = c.warning, fontSize = 11.5.sp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { ratedTickChecked = !ratedTickChecked },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = ratedTickChecked, onCheckedChange = { ratedTickChecked = it })
                        Text(
                            "Meri umar 18 ya usse zyada hai, aur mujhe ${settings.assistantName} se aisi hi baatein chahiye.",
                            fontSize = 12.sp,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = ratedTickChecked,
                    onClick = { adultWizardStep = 4 },
                ) { Text("Confirm karo") }
            },
            dismissButton = {
                TextButton(onClick = { adultWizardStep = 0 }) { Text("Cancel") }
            },
        )
    }

    // Step 4: Two-step final check ("R-rated on karein?" -> "On karo")
    if (adultWizardStep == 4) {
        AlertDialog(
            onDismissRequest = { adultWizardStep = 0 },
            title = { Text("R-rated on karein?") },
            text = {
                Text("Aakhri check. Isi se ye on hoga. \"normal baat karo\" bolte hi ye usi second normal ho jayega.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                    onClick = {
                        viewModel.activateIntense18PlusMode()
                        adultWizardStep = 0
                    },
                ) { Text("On karo") }
            },
            dismissButton = {
                TextButton(onClick = { adultWizardStep = 0 }) { Text("Rehne do") }
            },
        )
    }

    if (showWithdrawConfirm) {
        AlertDialog(
            onDismissRequest = { showWithdrawConfirm = false },
            title = { Text("Meri age ki confirmation wapas lo?") },
            text = { Text("Isse 18+ mode off ho jayega aur age confirmation record phone se hata diya jayega.") },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = c.error),
                    onClick = {
                        viewModel.withdrawAdultConfirmation()
                        showWithdrawConfirm = false
                    },
                ) { Text("Haan, wapas lo") }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showNeedLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showNeedLicenseDialog = false },
            title = { Text("Active License Zaroori Hai") },
            text = { Text("Intense (18+) mode ke liye active AYSHA license, age verification aur do confirmation zaroori hain. Website se plan ya license key verify karo.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.openLicensePortalInBrowser()
                    showNeedLicenseDialog = false
                }) { Text("License Check Karo") }
            },
            dismissButton = {
                TextButton(onClick = { showNeedLicenseDialog = false }) { Text("OK") }
            },
        )
    }
}
