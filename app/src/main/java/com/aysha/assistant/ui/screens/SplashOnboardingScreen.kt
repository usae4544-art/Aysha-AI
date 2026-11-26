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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LiquidAuroraBackground
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.components.AyshaInfinityLogo
import kotlinx.coroutines.delay

@Composable
fun SplashOnboardingScreen(
    onGetStarted: () -> Unit,
) {
    val c = LocalAyshaColors.current
    var showWelcomeCard by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0.15f) }

    LaunchedEffect(Unit) {
        for (i in 1..10) {
            delay(130L)
            progress = i / 10f
        }
        showWelcomeCard = true
    }

    LiquidAuroraBackground {
        AnimatedContent(
            targetState = showWelcomeCard,
            transitionSpec = { fadeIn(tween(420)) togetherWith fadeOut(tween(300)) },
            label = "splash_welcome",
        ) { isWelcome ->
            if (!isWelcome) {
                // Screen 1 from Mockup: Hero Portrait + Floating Infinity Logo + Title + Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showWelcomeCard = true },
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
                        onClick = { showWelcomeCard = true },
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
                            text = "Your Personal AI Assistant",
                            color = c.accentPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Powered by Intelligence\nMade for You",
                            color = c.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(22.dp))
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
            } else {
                // Screen 15 from Mockup: AYSHA Smart AI Assistant Welcome Card + Checkmarks + CTA
                val features = listOf(
                    "Voice Assistant",
                    "AI Chat",
                    "Floating Assistant",
                    "Phone Control",
                    "Automation",
                    "100% Free",
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
                                        .size(168.dp)
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

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "AYSHA",
                                color = c.textPrimary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 4.sp,
                            )
                            Text(
                                text = "Your Smart AI Assistant",
                                color = c.accentPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                features.forEach { item ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = c.success,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = item,
                                            color = c.textPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    AuroraGradientButton(
                        text = "Enjoy the Smart Experience!",
                        onClick = onGetStarted,
                        height = 56.dp,
                        cornerRadius = 28.dp,
                    )
                }
            }
        }
    }
}
