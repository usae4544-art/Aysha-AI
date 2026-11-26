package com.aysha.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aysha.assistant.R
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.ui.LocalAyshaColors
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun AyshaAvatarRing(
    size: Dp = 112.dp,
    isSpeaking: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = LocalAyshaColors.current
    val inf = rememberInfiniteTransition(label = "avatar_ring")
    val spin by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(if (isSpeaking) 2600 else 7200, easing = LinearEasing)),
        label = "ring_spin",
    )
    val pulse by inf.animateFloat(
        initialValue = 0.96f,
        targetValue = if (isSpeaking) 1.07f else 1.02f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ring_pulse",
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulse),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.accentPrimary.copy(alpha = 0.45f), c.accentSecondary.copy(alpha = 0.22f), Color.Transparent),
                ),
                radius = this.size.minDimension * 0.56f,
            )
            rotate(spin) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(c.accentPrimary, c.accentSecondary, c.accentGlow, c.accentPrimary),
                    ),
                    radius = this.size.minDimension * 0.47f,
                    style = Stroke(width = 3.5.dp.toPx()),
                )
            }
        }
        Image(
            painter = painterResource(id = R.drawable.aysha_portrait),
            contentDescription = "AYSHA Portrait",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size - 10.dp)
                .clip(CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
        )
    }
}

@Composable
fun AyshaVoiceOrb(
    orbState: OrbState,
    audioLevel: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 148.dp,
) {
    val c = LocalAyshaColors.current
    val inf = rememberInfiniteTransition(label = "voice_orb")

    val smoothLevel by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = tween(100),
        label = "audio_level",
    )

    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "orb_phase",
    )
    val spin by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = when (orbState) {
                    OrbState.PROCESSING, OrbState.CONNECTING -> 1400
                    OrbState.LISTENING, OrbState.SPEAKING -> 2800
                    else -> 6500
                },
                easing = LinearEasing,
            ),
        ),
        label = "orb_spin",
    )

    val statePrimary = when (orbState) {
        OrbState.LISTENING -> c.accentPrimary
        OrbState.PROCESSING, OrbState.CONNECTING -> c.accentSecondary
        OrbState.SPEAKING -> Color(0xFF38BDF8)
        OrbState.AUTOMATING -> c.success
        OrbState.ERROR -> c.error
        OrbState.IDLE -> c.accentPrimary
    }
    val stateSecondary = when (orbState) {
        OrbState.LISTENING -> Color(0xFF8B5CF6)
        OrbState.PROCESSING, OrbState.CONNECTING -> Color(0xFFEC4899)
        OrbState.SPEAKING -> c.accentSecondary
        OrbState.AUTOMATING -> c.accentPrimary
        OrbState.ERROR -> Color(0xFFFB7185)
        OrbState.IDLE -> c.accentSecondary
    }

    val breathe = 1f + sin(phase.toDouble()).toFloat() * (if (orbState == OrbState.IDLE) 0.03f else 0.06f)
    val totalScale = (breathe + smoothLevel * 0.14f).coerceIn(0.95f, 1.20f)

    Box(
        modifier = modifier
            .size(orbSize + 44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension * 0.48f
            // Ambient glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        statePrimary.copy(alpha = 0.42f + smoothLevel * 0.25f),
                        stateSecondary.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
                ),
                radius = r * (1.02f + smoothLevel * 0.18f),
            )

            // Outer concentric ripples for active states
            if (orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING || orbState == OrbState.PROCESSING) {
                val ripple1 = (sin(phase.toDouble()).toFloat() * 0.5f + 0.5f)
                drawCircle(
                    color = statePrimary.copy(alpha = 0.35f * (1f - ripple1 * 0.5f)),
                    radius = r * (0.78f + ripple1 * 0.22f),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            // Rotating luminous gradient ring
            rotate(spin) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            statePrimary,
                            stateSecondary,
                            Color.White.copy(alpha = 0.8f),
                            statePrimary,
                        ),
                    ),
                    radius = r * 0.72f * totalScale,
                    style = Stroke(width = 4.dp.toPx()),
                )
            }

            // Frosted liquid-glass core orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        statePrimary.copy(alpha = 0.85f),
                        stateSecondary.copy(alpha = 0.90f),
                        Color(0xFF0A1026),
                    ),
                    center = Offset(size.width * 0.42f, size.height * 0.38f),
                    radius = r * 0.68f * totalScale,
                ),
                radius = r * 0.66f * totalScale,
            )
        }

        val icon = when (orbState) {
            OrbState.LISTENING, OrbState.SPEAKING -> Icons.Rounded.Stop
            OrbState.PROCESSING, OrbState.CONNECTING -> Icons.Rounded.AutoAwesome
            else -> Icons.Rounded.Mic
        }
        Icon(
            imageVector = icon,
            contentDescription = orbState.displayLabel,
            tint = Color.White,
            modifier = Modifier.size(40.dp),
        )
    }
}

@Composable
fun EqualizerWaveformRow(
    active: Boolean,
    audioLevel: Float,
    barCount: Int = 18,
    maxHeight: Dp = 44.dp,
    modifier: Modifier = Modifier,
) {
    val c = LocalAyshaColors.current
    val inf = rememberInfiniteTransition(label = "eq_wave")
    val tick by inf.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "eq_tick",
    )

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until barCount) {
            val centerBias = 1f - kotlin.math.abs(i - (barCount - 1) / 2f) / (barCount / 2f)
            val wave = (sin(tick + i * 0.65f) * 0.5f + 0.5f)
            val amp = if (active) {
                (0.22f + (0.45f + audioLevel * 0.55f) * wave * (0.55f + centerBias * 0.45f)).coerceIn(0.16f, 1f)
            } else {
                (0.18f + 0.20f * centerBias * wave).coerceIn(0.15f, 0.45f)
            }
            val barColor = if (i % 2 == 0) c.accentPrimary else c.accentSecondary
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(maxHeight * amp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(listOf(barColor, barColor.copy(alpha = 0.55f))),
                    ),
            )
        }
    }
}

@Composable
fun AyshaInfinityLogo(
    size: Dp = 76.dp,
    modifier: Modifier = Modifier,
) {
    val c = LocalAyshaColors.current
    val inf = rememberInfiniteTransition(label = "infinity_logo")
    val glowPulse by inf.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "glow_pulse",
    )

    Box(
        modifier = modifier
            .size(size)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c.accentPrimary.copy(alpha = 0.28f * glowPulse), Color.Transparent),
                ),
                radius = w * 0.52f,
            )
            val path = Path().apply {
                moveTo(w * 0.14f, h * 0.5f)
                cubicTo(w * 0.14f, h * 0.20f, w * 0.42f, h * 0.20f, w * 0.50f, h * 0.50f)
                cubicTo(w * 0.58f, h * 0.80f, w * 0.86f, h * 0.80f, w * 0.86f, h * 0.50f)
                cubicTo(w * 0.86f, h * 0.20f, w * 0.58f, h * 0.20f, w * 0.50f, h * 0.50f)
                cubicTo(w * 0.42f, h * 0.80f, w * 0.14f, h * 0.80f, w * 0.14f, h * 0.50f)
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(listOf(c.accentPrimary, c.accentGlow, c.accentSecondary)),
                style = Stroke(width = (w * 0.085f), cap = StrokeCap.Round),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White, c.accentPrimary, c.accentSecondary)),
                radius = w * 0.10f,
                center = Offset(w * 0.5f, h * 0.5f),
            )
        }
    }
}
