package com.aysha.assistant.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.R
import com.aysha.assistant.core.AssistantLanguage
import com.aysha.assistant.core.HudColorOption
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.OrbStyle
import com.aysha.assistant.core.UserGender
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LiquidAuroraBackground
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.components.AyshaInfinityLogo
import com.aysha.assistant.ui.components.AyshaVoiceOrb
import kotlinx.coroutines.delay

@Composable
fun SplashOnboardingScreen(
    initialUserName: String = "Friend",
    onGetStarted: (name: String, gender: UserGender, lang: AssistantLanguage, orbStyle: OrbStyle) -> Unit,
) {
    val c = LocalAyshaColors.current
    // step 0 = Animated splash, 1 = Welcome highlights, 2 = Quick setup (Name/Gender/Lang/Orb)
    var step by remember { mutableIntStateOf(0) }
    var progress by remember { mutableFloatStateOf(0.15f) }

    var nameInput by remember { mutableStateOf(if (initialUserName == "Friend") "" else initialUserName) }
    var genderInput by remember { mutableStateOf(UserGender.UNSPECIFIED) }
    var langInput by remember { mutableStateOf(AssistantLanguage.HINGLISH) }
    var orbInput by remember { mutableStateOf(OrbStyle.AYSHA_2047) }

    LaunchedEffect(Unit) {
        for (i in 1..10) {
            delay(110L)
            progress = i / 10f
        }
        step = 1
    }

    LiquidAuroraBackground {
        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(380)) togetherWith fadeOut(tween(260)) },
            label = "splash_onboarding_steps",
        ) { currentStep ->
            when (currentStep) {
                0 -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { step = 1 },
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.aysha_portrait),
                            contentDescription = "AYSHA",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(480.dp)
                                .align(Alignment.TopCenter),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            c.background.copy(alpha = 0.35f),
                                            c.background.copy(alpha = 0.94f),
                                            c.background,
                                        ),
                                    ),
                                ),
                        )
                        IconButton(
                            onClick = { step = 1 },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .statusBarsPadding()
                                .padding(8.dp),
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Skip", tint = Color.White.copy(alpha = 0.7f))
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 28.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            AyshaInfinityLogo(size = 86.dp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "AYSHA",
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 5.sp,
                            )
                            Text(
                                text = "Your Personal AI Companion",
                                color = c.accentPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Ek second… load ho raha hai 💗",
                                color = c.textSecondary,
                                fontSize = 13.5.sp,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = c.accentPrimary,
                                trackColor = c.glassBorder,
                            )
                        }
                    }
                }

                1 -> {
                    val features = listOf(
                        "Live 3D Orb & Hinglish Voice",
                        "Smart Chat & Purani Yaadein",
                        "Phone Control & Automation",
                        "Companion Modes & Secret Safety",
                        "Scanner, Whiteboard & Vibes",
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(6.dp))

                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 26.dp,
                            elevated = true,
                            contentPadding = PaddingValues(18.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(156.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = 2.5.dp,
                                                brush = Brush.linearGradient(listOf(c.accentPrimary, c.accentSecondary)),
                                                shape = CircleShape,
                                            ),
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.aysha_portrait),
                                            contentDescription = "AYSHA",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(end = 14.dp, top = 6.dp)
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Brush.linearGradient(listOf(c.accentSecondary, c.accentPrimary))),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text("AYSHA", color = c.textPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp)
                                Text("Step 1 / 2 · Tumhari AI dost & assistant", color = c.accentPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                                Spacer(modifier = Modifier.height(16.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    features.forEach { item ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = c.success, modifier = Modifier.size(19.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(item, color = c.textPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        AuroraGradientButton(
                            text = "AYSHA set up karo ->",
                            onClick = { step = 2 },
                            height = 56.dp,
                            cornerRadius = 28.dp,
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Step 2 / 2 · Personal Setup", color = c.accentPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Thoda apne baare me batao", color = c.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)

                        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Tumhara naam") },
                                    placeholder = { Text("jaise Rahul ya Priya") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text("Aap kaun hain? (Optional)", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    UserGender.entries.forEach { g ->
                                        val active = genderInput == g
                                        GlassPill(
                                            text = g.label,
                                            dotColor = if (active) c.accentPrimary else null,
                                            onClick = { genderInput = g },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                Text("Baat karne ki bhasha", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistantLanguage.entries.forEach { l ->
                                        val active = langInput == l
                                        GlassPill(
                                            text = l.label.substringBefore(" "),
                                            dotColor = if (active) c.accentSecondary else null,
                                            onClick = { langInput = l },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Apna favourite Orb chuno", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, modifier = Modifier.fillMaxWidth())
                                AyshaVoiceOrb(
                                    orbState = OrbState.SPEAKING,
                                    audioLevel = 0.45f,
                                    onTap = {},
                                    orbSize = 98.dp,
                                    orbStyle = orbInput,
                                    hudColor = HudColorOption.CYAN,
                                    showCenterIcon = false,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    listOf(OrbStyle.AYSHA_2047, OrbStyle.NOVA_3D, OrbStyle.JARVIS_3D, OrbStyle.LIQUID_CORE, OrbStyle.ULTRON).forEach { st ->
                                        GlassPill(
                                            text = st.label,
                                            dotColor = if (orbInput == st) c.accentPrimary else null,
                                            onClick = { orbInput = st },
                                        )
                                    }
                                }
                            }
                        }

                        AuroraGradientButton(
                            text = "Home par jao",
                            onClick = {
                                onGetStarted(
                                    nameInput.trim().ifBlank { initialUserName.ifBlank { "Friend" } },
                                    genderInput,
                                    langInput,
                                    orbInput,
                                )
                            },
                            height = 56.dp,
                            cornerRadius = 28.dp,
                        )
                    }
                }
            }
        }
    }
}
