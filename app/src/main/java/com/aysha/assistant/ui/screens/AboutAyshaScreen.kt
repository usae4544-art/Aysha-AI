package com.aysha.assistant.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.BuildConfig
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.components.MayaTopBar

@Composable
fun AboutAyshaScreen(
    languageLabel: String = "Baat-cheet wali bhasha ke saath (Hinglish)",
    onOpenLanguage: () -> Unit = {},
    onOpenLicenses: () -> Unit = {},
    onNotifications: () -> Unit = {},
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        MayaTopBar(
            title = "About",
            showMenuInsteadOfBack = false,
            onLeadingClick = onBack,
            onNotificationsClick = onNotifications,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(14.dp))
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = "AYSHA",
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(56.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text("AYSHA", color = c.textPrimary, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Tumhari AI companion, tumhare phone pe", color = c.textSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(22.dp))

                Text("Banane wale", color = c.textSecondary, fontSize = 12.5.sp)
                Text("AYSHA Open AI", color = Color(0xFF3B82F6), fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "AYSHA tumse baat karti hai, tumhara khayal rakhti hai — aur tumhara poora phone chalati hai.",
                    color = c.textPrimary,
                    fontSize = 14.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.height(26.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    elevated = true,
                    onClick = onOpenLanguage,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ColoredIconBadge(icon = Icons.Rounded.Language, tintColor = Color(0xFF3B82F6), size = 38.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App ki bhasha", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            Text(languageLabel, color = c.textSecondary, fontSize = 12.5.sp)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 22.dp,
                    elevated = true,
                    onClick = onOpenLicenses,
                    contentPadding = PaddingValues(16.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ColoredIconBadge(icon = Icons.Rounded.Description, tintColor = Color(0xFF3B82F6), size = 38.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Open-source licences", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            Text("Dusron ka kaam jis par ye app bana hai", color = c.textSecondary, fontSize = 12.5.sp)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textSecondary)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassPill(text = "Website", dotColor = c.accentPrimary) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://usae4544-art.github.io/Aysha-AI/"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }.onFailure { Toast.makeText(context, "usae4544-art.github.io/Aysha-AI", Toast.LENGTH_SHORT).show() }
                    }
                    GlassPill(text = "Releases", dotColor = c.success) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/usae4544-art/Aysha-AI/releases"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }
                }
                Text("v${BuildConfig.VERSION_NAME} • Powered by Gemini Live & Open AI", color = c.textSecondary, fontSize = 12.sp)
            }
        }
    }
}
