package com.aysha.assistant.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("About AYSHA", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))
        AyshaInfinityLogo(size = 104.dp)

        Text(
            text = "AYSHA",
            color = c.textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 4.sp,
        )
        Text(
            text = "Your Personal AI Assistant",
            color = c.accentPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Version : ${BuildConfig.VERSION_NAME}",
            color = c.textSecondary,
            fontSize = 13.sp,
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 20.dp,
            elevated = true,
            contentPadding = PaddingValues(18.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Smart • Secure • Free\nAlways with You",
                    color = c.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Original Kotlin + Jetpack Compose + Room + Android Keystore + On-Device Vosk Wake Word architecture.\nNo subscriptions, no ads, no tracking, no forced cloud lock-in.",
                    color = c.textSecondary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Package: ${BuildConfig.APPLICATION_ID}", color = c.textSecondary, fontSize = 12.sp)
                Text("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})", color = c.textSecondary, fontSize = 12.sp)
                Text("ABI: ${Build.SUPPORTED_ABIS.joinToString(", ")}", color = c.textSecondary, fontSize = 12.sp)
            }
        }

        GlassPill("100% Free & Open Architecture", dotColor = c.success)
    }
}
