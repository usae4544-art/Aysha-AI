package com.aysha.assistant.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aysha.assistant.R
import com.aysha.assistant.core.HudColorOption
import com.aysha.assistant.core.OrbRenderMode
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.OrbStyle
import com.aysha.assistant.core.ui.LocalAyshaColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val AYSHA_ORB_AGSL = """
uniform float2 uCenter;
uniform float  uRadius;
uniform float  uTime;
uniform float  uLevel;
uniform int    uPhase;
uniform int    uLook;
uniform float2 uTilt;
uniform float3 uColA;
uniform float3 uColB;
uniform float3 uColC;
uniform float3 uRim;
uniform float  uGlow;
uniform int    uSteps;
uniform int    uInner;
uniform int    uOct;
uniform int    uChroma;

const float R = 1.0;
const float VIEW = 1.12;

float ss(float e0, float e1, float x) {
  float t = clamp((x - e0) / (e1 - e0), 0.0, 1.0);
  return t * t * (3.0 - 2.0 * t);
}

float3 hash3(float3 p) {
  p = float3(dot(p, float3(127.1, 311.7, 74.7)),
             dot(p, float3(269.5, 183.3, 246.1)),
             dot(p, float3(113.5, 271.9, 124.6)));
  return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float vnoise(float3 p) {
  float3 i = floor(p);
  float3 f = fract(p);
  float3 u = f * f * (3.0 - 2.0 * f);
  return mix(mix(mix(dot(hash3(i + float3(0.0, 0.0, 0.0)), f - float3(0.0, 0.0, 0.0)),
                     dot(hash3(i + float3(1.0, 0.0, 0.0)), f - float3(1.0, 0.0, 0.0)), u.x),
                 mix(dot(hash3(i + float3(0.0, 1.0, 0.0)), f - float3(0.0, 1.0, 0.0)),
                     dot(hash3(i + float3(1.0, 1.0, 0.0)), f - float3(1.0, 1.0, 0.0)), u.x), u.y),
             mix(mix(dot(hash3(i + float3(0.0, 0.0, 1.0)), f - float3(0.0, 0.0, 1.0)),
                     dot(hash3(i + float3(1.0, 0.0, 1.0)), f - float3(1.0, 0.0, 1.0)), u.x),
                 mix(dot(hash3(i + float3(0.0, 1.0, 1.0)), f - float3(0.0, 1.0, 1.0)),
                     dot(hash3(i + float3(1.0, 1.0, 1.0)), f - float3(1.0, 1.0, 1.0)), u.x), u.y), u.z);
}

float fbm(float3 p) {
  float a = 0.5;
  float s = 0.0;
  float an = 0.0;
  for (int i = 0; i < 4; i++) {
    if (i >= uOct) break;
    s += a * vnoise(p);
    an += a;
    p = p * 2.03 + float3(1.7, 9.2, 3.1);
    a *= 0.5;
  }
  return s * (0.9375 / max(an, 0.5));
}

float ridged(float3 p) {
  float a = 0.55;
  float s = 0.0;
  float an = 0.0;
  for (int i = 0; i < 4; i++) {
    if (i >= uOct) break;
    s += a * (1.0 - abs(vnoise(p)));
    an += a;
    p = p * 2.1 + float3(3.1, 1.3, 7.7);
    a *= 0.5;
  }
  return s * (1.03125 / max(an, 0.55));
}

float2 hash2(float2 p) {
  p = float2(dot(p, float2(127.1, 311.7)), dot(p, float2(269.5, 183.3)));
  return fract(sin(p) * 43758.5453);
}

float voro(float3 n, float scale) {
  float2 uv = float2(atan(n.z, n.x), asin(clamp(n.y, -1.0, 1.0))) * scale;
  float2 i = floor(uv);
  float2 f = fract(uv);
  float d1 = 8.0;
  float d2 = 8.0;
  for (int y = -1; y <= 1; y++) {
    for (int x = -1; x <= 1; x++) {
      float2 g = float2(float(x), float(y));
      float2 r = g + hash2(i + g) - f;
      float d = dot(r, r);
      if (d < d1) { d2 = d1; d1 = d; } else if (d < d2) { d2 = d; }
    }
  }
  return sqrt(d2) - sqrt(d1);
}

float2x2 rot(float a) {
  float c = cos(a);
  float s = sin(a);
  return float2x2(c, -s, s, c);
}

float surf(float3 p) {
  float rip = 0.0;
  if (uPhase == 3) {
    rip = uLevel * 0.10 * vnoise(p * 4.0 + float3(0.0, uTime * 2.4, 0.0))
        + uLevel * 0.025 * sin(14.0 * p.y + uTime * 9.0);
  }
  if (uPhase == 1) {
    rip = -0.025 * uLevel * vnoise(p * 6.0 - uTime);
  }
  return length(p) - R - rip;
}

float3 nrm(float3 p) {
  float2 e = float2(0.002, 0.0);
  return normalize(float3(surf(p + e.xyy) - surf(p - e.xyy),
                          surf(p + e.yxy) - surf(p - e.yxy),
                          surf(p + e.yyx) - surf(p - e.yyx)));
}

float3 env(float3 d) {
  float3 sky = mix(float3(0.02, 0.04, 0.10), float3(0.08, 0.13, 0.30), ss(-0.6, 0.9, d.y));
  float band = ss(0.985, 1.0, dot(d, normalize(float3(-0.5, 0.8, 0.3))));
  float band2 = ss(0.97, 1.0, dot(d, normalize(float3(0.7, 0.4, -0.2)))) * 0.6;
  float strip = ss(0.02, 0.0, abs(d.y - 0.55)) * ss(0.3, 0.0, abs(d.x)) * 0.35;
  return sky + float3(1.0) * (band + band2) + float3(0.8, 0.9, 1.0) * strip;
}

float3 inside(float3 p0, float3 rd) {
  float3 acc = float3(0.0);
  float t = 0.0;
  float3 p = p0;
  float listening = (uPhase == 1) ? 1.0 : 0.0;
  float thinking = (uPhase == 2) ? 1.0 : 0.0;
  float speaking = (uPhase == 3) ? 1.0 : 0.0;
  float spin = uTime * 0.22 + thinking * uTime * 0.9;
  float breathe = 1.0 + 0.08 * sin(uTime * 1.1);
  float pulse = 1.0 + speaking * 0.9 * uLevel + listening * 0.3 * uLevel;
  float inv = 1.0 / float(uInner);
  float stepLen = 1.95 * inv;
  float w = 1.508 * inv;
  float2x2 spinM = rot(spin);
  float2x2 leanM = rot(0.4);
  float2x2 ringM = rot(0.9 + 0.3 * sin(uTime * 0.4));
  for (int i = 0; i < 20; i++) {
    if (i >= uInner) break;
    float3 q = p;
    q.xz = spinM * q.xz;
    q.xy = leanM * q.xy;
    float rr = length(p);
    float3 flow = float3(0.0, uTime * 0.30, 0.0) + listening * normalize(q + 0.0001) * uTime * 0.9;
    float n1 = fbm(q * 2.1 * breathe - flow);
    float n2 = ridged(q * 3.4 + float3(uTime * 0.12, -uTime * 0.2, 0.0));
    float veins = pow(max(n2 - 0.56, 0.0001) * 2.5, 2.2);
    float3 q2 = q;
    q2.yz = ringM * q2.yz;
    float ring1 = ss(0.045, 0.0, abs(length(q.xz) - 0.62)) * ss(0.05, 0.0, abs(q.y)) * (0.9 + 0.6 * n1);
    float ring2 = ss(0.04, 0.0, abs(length(q2.xz) - 0.5)) * ss(0.05, 0.0, abs(q2.y)) * (0.4 + 0.6 * n2);
    float rings = (ring1 + ring2) * (0.7 + 0.8 * uLevel);
    float cloud = pow(max(ss(0.24, 0.92, n1 + 0.3), 0.0001), 1.25) * ss(1.0, 0.15, rr);
    float band = thinking * ss(0.09, 0.0, abs(q.y - 0.3 * sin(uTime * 1.6))) * ss(0.95, 0.4, rr) * (0.6 + 0.6 * n2);
    float core = exp(-rr * rr * 9.0) * pulse;
    float3 col = mix(uColA, uColB, ss(0.15, 0.85, n1 + 0.35)) * cloud * (0.95 + 0.5 * uLevel)
               + uColC * rings * 0.9
               + mix(uColB, uColC, ss(0.9, 0.2, rr)) * veins * ss(1.0, 0.25, rr) * (1.3 + 1.2 * uLevel)
               + uColC * band * 2.4
               + mix(uColC, float3(1.0), 0.6) * core * 1.1;
    acc += col * w * (1.0 - 0.5 * float(i) * inv);
    t += stepLen;
    p = p0 + rd * t;
    if (length(p) > R + 0.05) break;
  }
  return acc;
}

half4 main(float2 fragCoord) {
  float2 uv = (fragCoord - uCenter) / max(uRadius, 1.0) * VIEW;
  uv.y = -uv.y;
  float edge = 1.0 - ss(0.96, VIEW, length(uv));
  float3 ro = float3(0.0, 0.0, 3.2);
  float3 rd = normalize(float3(uv, -2.6));
  float2x2 tx = rot(uTilt.x * 0.35);
  float2x2 ty = rot(-uTilt.y * 0.3);
  ro.xz = tx * ro.xz;
  rd.xz = tx * rd.xz;
  ro.yz = ty * ro.yz;
  rd.yz = ty * rd.yz;
  float3 col = float3(0.0);
  float alpha = 0.0;

  float t = 0.0;
  bool hit = false;
  float3 p = ro;
  for (int i = 0; i < 42; i++) {
    if (i >= uSteps) break;
    p = ro + rd * t;
    float d = surf(p);
    if (d < 0.002) { hit = true; break; }
    t += d * 0.85;
    if (t > 6.0) break;
  }
  float dist = length(cross(rd, -ro));
  float tight = exp(-max(dist - R, 0.0) * 9.0) * (0.55 + 1.0 * uLevel);
  float wide = exp(-max(dist - R, 0.0) * 2.2) * (0.18 + 0.5 * uLevel);
  float act = (uPhase > 0) ? 1.0 : 0.55;
  col += uRim * (tight + wide) * act;

  if (hit) {
    float3 n = nrm(p);
    float3 v = -rd;
    float fres = pow(max(1.0 - max(dot(n, v), 0.0), 0.0001), 3.0);
    float3 rr = refract(rd, n, 1.0 / 1.42);
    float3 glow = inside(p + rr * 0.02, rr);
    if (uChroma == 1) {
      float3 rr2 = refract(rd, n, 1.0 / 1.5);
      glow.b = mix(glow.b, inside(p + rr2 * 0.02, rr2).b, 0.5);
    }
    float3 refl = env(reflect(rd, n));
    float3 l1 = normalize(float3(-0.5, 0.8, 0.6));
    float3 l2 = normalize(float3(0.7, 0.3, 0.5));
    float sp = pow(max(dot(reflect(-l1, n), v), 0.0001), 180.0) + 0.45 * pow(max(dot(reflect(-l2, n), v), 0.0001), 65.0);
    float detail = 0.0;
    if (uLook == 1) {
      float3 q = abs(fract(n * 6.0 + float3(0.0, uTime * 0.05, 0.0)) - 0.5);
      float grid = ss(0.06, 0.0, min(min(q.x, q.y), q.z)) * 1.6;
      float f = fbm(n * 5.0 + uTime * 0.15);
      float travel = 0.5 + 0.5 * sin(uTime * 3.0 + (n.x + n.y) * 12.0);
      detail = grid * ss(0.05, 0.45, f) * (0.7 + 0.6 * travel);
    }
    if (uLook == 2) {
      float e = voro(n, 2.2 + 0.6 * sin(uTime * 0.6));
      detail = ss(0.09, 0.0, e) * (0.7 + 0.5 * sin(uTime * 2.0 + e * 20.0));
    }
    float3 surfaceCol = glow * (0.9 - 0.4 * fres) + refl * (0.10 + 0.85 * fres) + float3(1.0) * sp * 1.3 + uRim * detail * (0.9 + 0.9 * uLevel);
    surfaceCol += uRim * fres * (0.7 + 0.8 * uLevel);
    col = surfaceCol + col * 0.4;
    alpha = 1.0;
  } else {
    alpha = clamp((tight + wide) * 1.3, 0.0, 1.0) * edge;
    col *= edge;
  }
  col *= uGlow;
  col = col / (1.0 + col * 0.35);
  col = pow(max(col, float3(0.0001)), float3(0.90));
  col = clamp(col, 0.0, 1.0);
  return half4(col * alpha, alpha);
}
"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private object AyshaOrbAgslHelper {
    fun buildShaderOrNull(): RuntimeShader? = runCatching { RuntimeShader(AYSHA_ORB_AGSL) }.getOrNull()

    fun configureAndBrush(
        shader: RuntimeShader,
        center: Offset,
        radius: Float,
        timeSec: Float,
        level: Float,
        orbState: OrbState,
        orbStyle: OrbStyle,
        coreColor: Color,
        mainColor: Color,
    ): ShaderBrush {
        val phaseInt = when (orbState) {
            OrbState.LISTENING -> 1
            OrbState.PROCESSING, OrbState.CONNECTING, OrbState.AUTOMATING -> 2
            OrbState.SPEAKING -> 3
            else -> 0
        }
        val lookInt = when (orbStyle) {
            OrbStyle.JARVIS_3D -> 1
            OrbStyle.ULTRON -> 2
            else -> 0
        }
        shader.setFloatUniform("uCenter", center.x, center.y)
        shader.setFloatUniform("uRadius", radius.coerceAtLeast(10f))
        shader.setFloatUniform("uTime", timeSec)
        shader.setFloatUniform("uLevel", level.coerceIn(0f, 1.2f))
        shader.setIntUniform("uPhase", phaseInt)
        shader.setIntUniform("uLook", lookInt)
        shader.setFloatUniform("uTilt", sin(timeSec * 0.6f) * 0.22f, cos(timeSec * 0.45f) * 0.16f)
        shader.setFloatUniform("uColA", mainColor.red * 0.45f, mainColor.green * 0.45f, mainColor.blue * 0.5f)
        shader.setFloatUniform("uColB", mainColor.red, mainColor.green, mainColor.blue)
        shader.setFloatUniform("uColC", coreColor.red, coreColor.green, coreColor.blue)
        shader.setFloatUniform("uRim", mainColor.red, mainColor.green, mainColor.blue)
        shader.setFloatUniform("uGlow", 1.16f + level * 0.28f)
        shader.setIntUniform("uSteps", 36)
        shader.setIntUniform("uInner", 14)
        shader.setIntUniform("uOct", 3)
        shader.setIntUniform("uChroma", 1)
        return ShaderBrush(shader)
    }
}

@Composable
fun AyshaAvatarRing(
    size: Dp = 112.dp,
    isSpeaking: Boolean = false,
    audioLevel: Float = 0f,
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
    val smoothAudio by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 420f),
        label = "avatar_audio",
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(pulse + smoothAudio * 0.06f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c.accentPrimary.copy(alpha = (0.38f + smoothAudio * 0.25f).coerceIn(0.25f, 0.75f)),
                        c.accentSecondary.copy(alpha = 0.18f),
                        Color.Transparent,
                    ),
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
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                .graphicsLayer {
                    val zoom = 1f + smoothAudio * 0.045f
                    scaleX = zoom
                    scaleY = zoom
                },
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
    hudCoreColor: HudColorOption = hudColor,
    hudMainColor: HudColorOption = hudColor,
    hudRaysLengthPct: Float = 0.72f,
    hudRingEnabled: Boolean = true,
    renderMode: OrbRenderMode = OrbRenderMode.HUD_ORB,
    showCenterIcon: Boolean = true,
) {
    val c = LocalAyshaColors.current
    val inf = rememberInfiniteTransition(label = "voice_orb")

    val smoothLevel by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 460f),
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
    val agslSec by inf.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "agsl_time",
    )

    val agslShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            AyshaOrbAgslHelper.buildShaderOrNull()
        } else {
            null
        }
    }

    val styleColors = resolveOrbPalette(orbStyle, hudMainColor, hudCoreColor, orbState, c.error, c.success)
    val mainPrimary = styleColors.first
    val coreAccent = styleColors.second

    val breathe = 1f + sin(phase.toDouble()).toFloat() * (if (orbState == OrbState.IDLE) 0.025f else 0.055f)
    val totalScale = (breathe + smoothLevel * 0.14f).coerceIn(0.95f, 1.22f)

    if (renderMode == OrbRenderMode.ANIMATED_AVATAR) {
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
                drawHudTelemetryRing(
                    primary = mainPrimary,
                    secondary = coreAccent,
                    spin = spin,
                    level = smoothLevel,
                    raysPct = hudRaysLengthPct,
                    ringEnabled = hudRingEnabled,
                )
            }
            AyshaAvatarRing(
                size = orbSize * 0.92f,
                isSpeaking = orbState == OrbState.SPEAKING || orbState == OrbState.LISTENING,
                audioLevel = smoothLevel,
            )
        }
        return
    }

    val useAgsl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        agslShader != null &&
        orbStyle in setOf(
            OrbStyle.AYSHA_2047,
            OrbStyle.NOVA_3D,
            OrbStyle.JARVIS_3D,
            OrbStyle.LIQUID_CORE,
            OrbStyle.PULSE_REACTOR,
            OrbStyle.ULTRON,
        )

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
            val cPos = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.46f

            if (useAgsl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && agslShader != null) {
                drawHudTelemetryRing(
                    primary = mainPrimary,
                    secondary = coreAccent,
                    spin = spin,
                    level = smoothLevel,
                    raysPct = hudRaysLengthPct,
                    ringEnabled = hudRingEnabled,
                )
                val shaderRadius = r * 0.68f * totalScale
                val brush = AyshaOrbAgslHelper.configureAndBrush(
                    shader = agslShader,
                    center = cPos,
                    radius = shaderRadius,
                    timeSec = agslSec,
                    level = smoothLevel,
                    orbState = orbState,
                    orbStyle = orbStyle,
                    coreColor = coreAccent,
                    mainColor = mainPrimary,
                )
                drawCircle(
                    brush = brush,
                    center = cPos,
                    radius = shaderRadius * 1.15f,
                )
            } else {
                drawStyledOrb(
                    style = orbStyle,
                    primary = mainPrimary,
                    secondary = coreAccent,
                    phase = phase,
                    spin = spin,
                    level = smoothLevel,
                    scale = totalScale,
                    isActive = orbState != OrbState.IDLE,
                    raysPct = hudRaysLengthPct,
                    ringEnabled = hudRingEnabled,
                )
            }
        }

        if (showCenterIcon && !useAgsl) {
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
    hudMain: HudColorOption,
    hudCore: HudColorOption,
    orbState: OrbState,
    errorColor: Color,
    successColor: Color,
): Pair<Color, Color> {
    if (orbState == OrbState.ERROR) return errorColor to Color(0xFFFB7185)
    if (orbState == OrbState.AUTOMATING) return successColor to Color(0xFF22D3EE)

    return when (orbStyle) {
        OrbStyle.JARVIS, OrbStyle.JARVIS_3D -> Color(0xFFF59E0B) to Color(0xFFEA580C)
        OrbStyle.FRIDAY -> Color(0xFFFACC15) to Color(0xFFF97316)
        OrbStyle.RED_HUD -> Color(0xFFEF476F) to Color(0xFFF59E0B)
        OrbStyle.ULTRON -> Color(0xFF38BDF8) to Color(0xFF2563EB)
        else -> Color(hudMain.primaryHex) to Color(hudCore.primaryHex)
    }
}

private fun DrawScope.drawHudTelemetryRing(
    primary: Color,
    secondary: Color,
    spin: Float,
    level: Float,
    raysPct: Float,
    ringEnabled: Boolean,
) {
    val r = size.minDimension * 0.46f
    val cPos = Offset(size.width / 2f, size.height / 2f)
    if (!ringEnabled) return

    val tickCount = 36
    val rayReach = raysPct.coerceIn(0.35f, 1.25f)
    for (i in 0 until tickCount) {
        val a = (i * 10f + spin * 0.35f) * (PI / 180.0)
        val inner = r * 0.80f
        val extra = if (i % 3 == 0) 0.14f * rayReach else 0.07f * rayReach
        val outer = r * (0.82f + extra) + level * 7f
        drawLine(
            color = (if (i % 6 == 0) secondary else primary).copy(alpha = if (i % 3 == 0) 0.72f else 0.25f),
            start = Offset(cPos.x + (cos(a) * inner).toFloat(), cPos.y + (sin(a) * inner).toFloat()),
            end = Offset(cPos.x + (cos(a) * outer).toFloat(), cPos.y + (sin(a) * outer).toFloat()),
            strokeWidth = if (i % 3 == 0) 2.dp.toPx() else 1.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    for (d in 0 until 6) {
        val a = (d * 60f - spin * 0.65f) * (PI / 180.0)
        val orbitR = r * 0.94f
        drawCircle(
            color = if (d % 2 == 0) secondary else primary,
            radius = 2.6.dp.toPx(),
            center = Offset(cPos.x + (cos(a) * orbitR).toFloat(), cPos.y + (sin(a) * orbitR).toFloat()),
        )
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
    raysPct: Float,
    ringEnabled: Boolean,
) {
    val r = size.minDimension * 0.46f
    val cPos = Offset(size.width / 2f, size.height / 2f)

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

    if (ringEnabled && style != OrbStyle.RED_HUD) {
        drawHudTelemetryRing(primary, secondary, spin, level, raysPct, true)
    }

    when (style) {
        OrbStyle.AYSHA_2047 -> {
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
            // Screenshot-exact Neural Constellation Sphere + Red HUD Equalizer Ticks + Jet-Black Sphere Center
            val rayCount = 48
            val reach = raysPct.coerceIn(0.45f, 1.30f)
            val ringRadius = r * 0.62f * scale

            // Faint outer HUD orbital technical ring
            drawCircle(
                color = primary.copy(alpha = 0.22f),
                center = cPos,
                radius = r * 0.96f,
                style = Stroke(width = 1.dp.toPx()),
            )

            // 48 Crisp radial equalizer rays surrounding the core sphere
            for (i in 0 until rayCount) {
                val a = (i * (360f / rayCount) + spin * 0.22f) * (PI / 180.0)
                val barNoise = 0.35f + 0.65f * (sin(phase * 1.4f + i * 0.7f).toFloat() * 0.5f + 0.5f)
                val rayLen = r * (0.06f + (if (isActive) 0.24f else 0.15f) * reach * barNoise + level * 0.16f)
                val startR = ringRadius + 3.dp.toPx()
                val endR = startR + rayLen
                drawLine(
                    color = primary.copy(alpha = if (i % 3 == 0) 0.78f else 0.45f),
                    start = Offset(cPos.x + (cos(a) * startR).toFloat(), cPos.y + (sin(a) * startR).toFloat()),
                    end = Offset(cPos.x + (cos(a) * endR).toFloat(), cPos.y + (sin(a) * endR).toFloat()),
                    strokeWidth = if (i % 2 == 0) 2.dp.toPx() else 1.3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Crisp red/pink rim circle around black sphere
            drawCircle(
                color = primary.copy(alpha = 0.88f),
                center = cPos,
                radius = ringRadius,
                style = Stroke(width = 2.6.dp.toPx()),
            )

            // Deep Obsidian Jet-Black Core Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF19120F), Color(0xFF070709), Color(0xFF040406)),
                    center = cPos,
                    radius = ringRadius - 1.dp.toPx(),
                ),
                center = cPos,
                radius = ringRadius - 1.dp.toPx(),
            )

            // 3D Gold-Orange Orbital Neuron Ellipses & Connected Constellation Mesh inside Black Sphere
            val nodeCount = 24
            val nodes = Array(nodeCount) { idx ->
                val theta = (idx * 2.39996f) + spin * (PI.toFloat() / 180f) * (if (idx % 2 == 0) 0.7f else -0.45f)
                val distFactor = 0.22f + 0.58f * (((idx * 7) % 11) / 10f)
                val nr = ringRadius * 0.78f * distFactor
                Offset(
                    cPos.x + cos(theta) * nr,
                    cPos.y + sin(theta * 0.92f + idx * 0.2f) * nr * 0.86f,
                )
            }

            // Orbital wireframe rings inside sphere
            for (orbIdx in 0..2) {
                rotate(spin * (if (orbIdx % 2 == 0) 0.5f else -0.4f) + orbIdx * 54f, pivot = cPos) {
                    drawOval(
                        color = secondary.copy(alpha = 0.34f),
                        topLeft = Offset(cPos.x - ringRadius * 0.76f, cPos.y - ringRadius * (0.28f + orbIdx * 0.12f)),
                        size = Size(ringRadius * 1.52f, ringRadius * (0.56f + orbIdx * 0.24f)),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }

            // Gold neural filaments between close constellation nodes
            for (i in 0 until nodeCount) {
                val n1 = nodes[i]
                val n2 = nodes[(i + 1) % nodeCount]
                val n3 = nodes[(i + 5) % nodeCount]
                drawLine(
                    color = secondary.copy(alpha = 0.42f),
                    start = n1,
                    end = n2,
                    strokeWidth = 0.9.dp.toPx(),
                )
                drawLine(
                    color = primary.copy(alpha = 0.26f),
                    start = n1,
                    end = n3,
                    strokeWidth = 0.7.dp.toPx(),
                )
            }
            for (i in 0 until nodeCount) {
                val bright = (sin(phase * 1.5f + i) * 0.5f + 0.5f)
                drawCircle(
                    color = if (i % 4 == 0) Color(0xFFFFF3C4) else secondary,
                    radius = (1.6f + bright * 1.5f + level * 1.1f).dp.toPx(),
                    center = nodes[i],
                )
            }
        }

        OrbStyle.PARTICLE_SWARM -> {
            val count = 28
            for (i in 0 until count) {
                val angle = (i * (2f * PI.toFloat() / count)) + (spin * PI.toFloat() / 180f) * (if (i % 2 == 0) 1f else -0.7f)
                val orbit = r * (0.54f + (i % 4) * 0.10f * raysPct + level * 0.12f)
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
