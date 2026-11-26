package com.aysha.assistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.BuildConfig
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.components.AyshaInfinityLogo

@Composable
fun AboutAyshaScreen(
    onOpenLicenses: () -> Unit = {},
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("About AYSHA", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AyshaInfinityLogo(size = 112.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text("AYSHA", color = c.textPrimary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 5.sp)
            Text("Your Personal AI Assistant", color = c.accentPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Text("Version : ${BuildConfig.VERSION_NAME}", color = c.textSecondary, fontSize = 13.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Open Source (MIT License) · Google License Sync", color = c.textSecondary, fontSize = 13.sp)

            Spacer(modifier = Modifier.height(20.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, elevated = true, contentPadding = PaddingValues(18.dp)) {
                Text(
                    text = "11 Signature Orbs · 9 Palettes · Barge-In & Awareness Gate\nSemantic Memory · Rules · Scanner · Whiteboard · Automation",
                    color = c.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Package: ${BuildConfig.APPLICATION_ID}", color = c.textSecondary, fontSize = 12.sp)
                    Text("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})", color = c.textSecondary, fontSize = 12.sp)
                    Text("Security: AndroidKeyStore AES-256-GCM • Offline Vosk Wake Word", color = c.textSecondary, fontSize = 11.5.sp)
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            GlassPill(text = "Website", dotColor = c.accentPrimary) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://usae4544-art.github.io/Aysha-AI/"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { Toast.makeText(context, "https://usae4544-art.github.io/Aysha-AI/", Toast.LENGTH_SHORT).show() }
            }
            GlassPill(text = "Releases", dotColor = c.success) {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/usae4544-art/Aysha-AI/releases"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure { Toast.makeText(context, "AYSHA v${BuildConfig.VERSION_NAME}", Toast.LENGTH_SHORT).show() }
            }
            GlassPill(text = "Licenses", dotColor = c.accentSecondary, onClick = onOpenLicenses)
        }
    }
}
