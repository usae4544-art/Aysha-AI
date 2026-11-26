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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aysha.assistant.R
import com.aysha.assistant.core.HudColorOption
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.OrbStyle
import com.aysha.assistant.core.ui.LocalAyshaColors
import kotlin.math.PI
import kotlin.math.cos
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
                    colors = listOf(c.accentPrimary.copy(alpha = 0.42f), c.accentSecondary.copy(alpha = 0.18f), Color.Transparent),
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
    orbStyle: OrbStyle = OrbStyle.AYSHA_2047,
    hudColor: HudColorOption = HudColorOption.CYAN,
    showCenterIcon: Boolean = true,
) {
    val c = LocalAyshaColors.current
    val inf = rememberInfiniteTransition(label = "voice_orb")

    val smoothLevel by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = tween(90),
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
                    OrbState.LISTENING, OrbState.SPEAKING -> 2600
                    else -> 6500
                },
                easing = LinearEasing,
            ),
        ),
        label = "orb_spin",
    )

    val styleColors = resolveOrbPalette(orbStyle, hudColor, orbState, c.error, c.success)
    val statePrimary = styleColors.first
    val stateSecondary = styleColors.second

    val breathe = 1f + sin(phase.toDouble()).toFloat() * (if (orbState == OrbState.IDLE) 0.025f else 0.055f)
    val totalScale = (breathe + smoothLevel * 0.14f).coerceIn(0.95f, 1.22f)

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
            drawStyledOrb(
                style = orbStyle,
                primary = statePrimary,
                secondary = stateSecondary,
                phase = phase,
                spin = spin,
                level = smoothLevel,
                scale = totalScale,
                isActive = orbState != OrbState.IDLE,
            )
        }

        if (showCenterIcon) {
            val icon = when (orbState) {
                OrbState.LISTENING, OrbState.SPEAKING -> Icons.Rounded.Stop
                OrbState.PROCESSING, OrbState.CONNECTING -> Icons.Rounded.AutoAwesome
                else -> Icons.Rounded.Mic
            }
            Icon(
                imageVector = icon,
                contentDescription = orbState.displayLabel,
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.size((orbSize.value * 0.25f).coerceIn(18f, 40f).dp),
            )
        }
    }
}

private fun resolveOrbPalette(
    orbStyle: OrbStyle,
    hudColor: HudColorOption,
    orbState: OrbState,
    errorColor: Color,
    successColor: Color,
): Pair<Color, Color> {
    if (orbState == OrbState.ERROR) return errorColor to Color(0xFFFB7185)
    if (orbState == OrbState.AUTOMATING) return successColor to Color(0xFF22D3EE)

    return when (orbStyle) {
        OrbStyle.JARVIS, OrbStyle.JARVIS_3D -> Color(0xFFF59E0B) to Color(0xFFEA580C)
        OrbStyle.FRIDAY -> Color(0xFFFACC15) to Color(0xFFF97316)
        OrbStyle.RED_HUD -> Color(0xFFEF4444) to Color(0xFFF97316)
        OrbStyle.ULTRON -> Color(0xFF38BDF8) to Color(0xFF2563EB)
        else -> Color(hudColor.primaryHex) to Color(hudColor.secondaryHex)
    }
}

private fun DrawScope.drawStyledOrb(
    style: OrbStyle,
    primary: Color,
    secondary: Color,
    phase: Float,
    spin: Float,
    level: Float,
    scale: Float,
    isActive: Boolean,
) {
    val r = size.minDimension * 0.46f
    val cPos = Offset(size.width / 2f, size.height / 2f)

    // Soft ambient radial halo
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                primary.copy(alpha = (0.34f + level * 0.26f).coerceIn(0.2f, 0.7f)),
                secondary.copy(alpha = 0.14f),
                Color.Transparent,
            ),
            center = cPos,
            radius = r * (1.06f + level * 0.16f),
        ),
        center = cPos,
        radius = r * (1.06f + level * 0.16f),
    )

    when (style) {
        OrbStyle.AYSHA_2047 -> {
            // Outer telemetry ticks
            val tickCount = 36
            for (i in 0 until tickCount) {
                val a = (i * 10f + spin * 0.4f) * (PI / 180.0)
                val inner = r * 0.85f
                val outer = r * (if (i % 3 == 0) 0.96f else 0.91f) + level * 8f
                drawLine(
                    color = primary.copy(alpha = if (i % 3 == 0) 0.72f else 0.28f),
                    start = Offset(cPos.x + (cos(a) * inner).toFloat(), cPos.y + (sin(a) * inner).toFloat()),
                    end = Offset(cPos.x + (cos(a) * outer).toFloat(), cPos.y + (sin(a) * outer).toFloat()),
                    strokeWidth = if (i % 3 == 0) 2.2.dp.toPx() else 1.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            // Counter-rotating HUD arcs
            rotate(spin) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(primary, secondary, Color.White.copy(alpha = 0.8f), primary)),
                    startAngle = 0f,
                    sweepAngle = 250f,
                    useCenter = false,
                    topLeft = Offset(cPos.x - r * 0.78f, cPos.y - r * 0.78f),
                    size = Size(r * 1.56f, r * 1.56f),
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            rotate(-spin * 1.35f) {
                drawArc(
                    color = secondary.copy(alpha = 0.85f),
                    startAngle = 30f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cPos.x - r * 0.68f, cPos.y - r * 0.68f),
                    size = Size(r * 1.36f, r * 1.36f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            // Core sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.9f), secondary.copy(alpha = 0.85f), Color(0xFF050811)),
                    center = Offset(cPos.x - r * 0.18f, cPos.y - r * 0.20f),
                    radius = r * 0.62f * scale,
                ),
                center = cPos,
                radius = r * 0.58f * scale,
            )
        }

        OrbStyle.NOVA_HUD, OrbStyle.NOVA_3D -> {
            if (isActive) {
                val pulseR = r * (0.76f + (sin(phase.toDouble()).toFloat() * 0.5f + 0.5f) * 0.2f)
                drawCircle(color = primary.copy(alpha = 0.3f), center = cPos, radius = pulseR, style = Stroke(width = 1.8.dp.toPx()))
            }
            rotate(spin) {
                drawCircle(
                    brush = Brush.sweepGradient(listOf(primary, secondary, Color.White, primary)),
                    center = cPos,
                    radius = r * 0.74f * scale,
                    style = Stroke(width = if (style == OrbStyle.NOVA_3D) 5.dp.toPx() else 3.5.dp.toPx()),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = if (style == OrbStyle.NOVA_3D) {
                        listOf(Color.White.copy(alpha = 0.45f), primary.copy(alpha = 0.88f), secondary.copy(alpha = 0.92f), Color(0xFF04060D))
                    } else {
                        listOf(primary.copy(alpha = 0.85f), secondary.copy(alpha = 0.88f), Color(0xFF060913))
                    },
                    center = Offset(cPos.x - r * 0.22f, cPos.y - r * 0.24f),
                    radius = r * 0.68f * scale,
                ),
                center = cPos,
                radius = r * 0.66f * scale,
            )
        }

        OrbStyle.LIQUID_CORE -> {
            val path = Path()
            val pts = 48
            for (i in 0..pts) {
                val ang = (i.toFloat() / pts) * 2f * PI.toFloat()
                val wobble = sin(ang * 3f + phase) * 0.08f + cos(ang * 5f - phase * 1.4f) * (0.04f + level * 0.12f)
                val rad = r * (0.68f + wobble) * scale
                val px = cPos.x + cos(ang) * rad
                val py = cPos.y + sin(ang) * rad
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            drawPath(
                path = path,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.35f), primary, secondary, Color(0xFF05070E)),
                    center = Offset(cPos.x - r * 0.15f, cPos.y - r * 0.15f),
                    radius = r * 0.85f,
                ),
            )
            drawPath(
                path = path,
                brush = Brush.linearGradient(listOf(primary, secondary)),
                style = Stroke(width = 2.5.dp.toPx()),
            )
        }

        OrbStyle.JARVIS, OrbStyle.JARVIS_3D, OrbStyle.PULSE_REACTOR -> {
            for (ringIdx in 0..2) {
                val factor = 0.86f - ringIdx * 0.14f
                val dir = if (ringIdx % 2 == 0) 1f else -1.3f
                rotate(spin * dir + ringIdx * 45f) {
                    for (seg in 0..2) {
                        drawArc(
                            color = if (ringIdx == 0) primary else secondary.copy(alpha = 0.85f),
                            startAngle = seg * 120f + 10f,
                            sweepAngle = 78f,
                            useCenter = false,
                            topLeft = Offset(cPos.x - r * factor, cPos.y - r * factor),
                            size = Size(r * 2f * factor, r * 2f * factor),
                            style = Stroke(width = (3.4f - ringIdx * 0.7f).dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f), primary.copy(alpha = 0.85f), Color(0xFF0A0805)),
                    center = cPos,
                    radius = r * 0.44f * scale,
                ),
                center = cPos,
                radius = r * 0.44f * scale,
            )
        }

        OrbStyle.FRIDAY -> {
            // Golden visor horizontal bracket arcs + wave core
            rotate(spin * 0.35f) {
                drawArc(
                    color = primary,
                    startAngle = -55f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(cPos.x - r * 0.84f, cPos.y - r * 0.84f),
                    size = Size(r * 1.68f, r * 1.68f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                )
                drawArc(
                    color = secondary,
                    startAngle = 125f,
                    sweepAngle = 110f,
                    useCenter = false,
                    topLeft = Offset(cPos.x - r * 0.84f, cPos.y - r * 0.84f),
                    size = Size(r * 1.68f, r * 1.68f),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.85f), secondary.copy(alpha = 0.6f), Color(0xFF0A0904))),
                center = cPos,
                radius = r * 0.62f * scale,
            )
            // Visor wave lines
            val wavePath = Path()
            val steps = 28
            for (i in 0..steps) {
                val xNorm = (i.toFloat() / steps) * 2f - 1f
                val x = cPos.x + xNorm * r * 0.48f
                val y = cPos.y + sin(xNorm * 5f + phase * 1.8f) * r * (0.08f + level * 0.16f) * (1f - xNorm * xNorm)
                if (i == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
            }
            drawPath(wavePath, color = Color.White.copy(alpha = 0.9f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        OrbStyle.RED_HUD -> {
            val rayCount = 16
            for (i in 0 until rayCount) {
                val a = (i * (360f / rayCount) + spin * 0.5f) * (PI / 180.0)
                val len = r * (0.64f + 0.28f * (sin(phase + i).toFloat() * 0.5f + 0.5f) + level * 0.12f)
                drawLine(
                    color = if (i % 2 == 0) primary else secondary,
                    start = Offset(cPos.x + (cos(a) * r * 0.3f).toFloat(), cPos.y + (sin(a) * r * 0.3f).toFloat()),
                    end = Offset(cPos.x + (cos(a) * len).toFloat(), cPos.y + (sin(a) * len).toFloat()),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.7f), primary, Color(0xFF140406))),
                center = cPos,
                radius = r * 0.46f * scale,
            )
        }

        OrbStyle.PARTICLE_SWARM -> {
            val count = 24
            for (i in 0 until count) {
                val angle = (i * (2f * PI.toFloat() / count)) + (spin * PI.toFloat() / 180f) * (if (i % 2 == 0) 1f else -0.7f)
                val orbit = r * (0.56f + (i % 4) * 0.10f + level * 0.12f)
                val px = cPos.x + cos(angle) * orbit
                val py = cPos.y + sin(angle) * orbit
                drawCircle(
                    color = if (i % 2 == 0) primary else secondary,
                    radius = (2.4f + (i % 3) * 1.1f).dp.toPx(),
                    center = Offset(px, py),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(listOf(primary.copy(alpha = 0.9f), secondary.copy(alpha = 0.7f), Color(0xFF05070D))),
                center = cPos,
                radius = r * 0.48f * scale,
            )
        }

        OrbStyle.ULTRON -> {
            val sides = 8
            val shardPath = Path()
            for (i in 0 until sides) {
                val a = (i * (2f * PI.toFloat() / sides)) + spin * PI.toFloat() / 180f
                val rad = r * (if (i % 2 == 0) 0.84f else 0.74f) * scale
                val x = cPos.x + cos(a) * rad
                val y = cPos.y + sin(a) * rad
                if (i == 0) shardPath.moveTo(x, y) else shardPath.lineTo(x, y)
            }
            shardPath.close()
            drawPath(
                path = shardPath,
                brush = Brush.linearGradient(listOf(primary, secondary, Color.White.copy(alpha = 0.7f))),
                style = Stroke(width = 3.dp.toPx()),
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.4f), primary.copy(alpha = 0.88f), Color(0xFF030816))),
                center = cPos,
                radius = r * 0.56f * scale,
            )
        }
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
