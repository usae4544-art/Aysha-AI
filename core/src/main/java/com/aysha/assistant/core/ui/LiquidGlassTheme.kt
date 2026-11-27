package com.aysha.assistant.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.CornerStyle
import com.aysha.assistant.core.MayaPalette
import com.aysha.assistant.core.R
import com.aysha.assistant.core.SurfaceStyle
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.UiFontFamily
import com.aysha.assistant.core.UiTextScale

@Immutable
data class AyshaColors(
    val isDark: Boolean,
    val background: Color,
    val backgroundDeep: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentGlow: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val transparencyFactor: Float,
    val surfaceStyle: SurfaceStyle = SurfaceStyle.GLASS,
    val cornerRadiusDp: Int = 20,
    val textScale: Float = 1.0f,
    val palette: MayaPalette = MayaPalette.OBSIDIAN,
)

val LocalAyshaColors = staticCompositionLocalOf {
    buildAyshaColors(ThemeMode.DARK, AccentPalette.COSMIC_CYAN, 0.72f)
}

fun buildAyshaColors(
    themeMode: ThemeMode,
    accent: AccentPalette,
    glassTransparency: Float,
): AyshaColors {
    val palette = if (themeMode == ThemeMode.DARK) MayaPalette.MIDNIGHT else MayaPalette.DAYLIGHT
    return buildAyshaColors(
        palette = palette,
        accentOverride = accent,
        surfaceStyle = SurfaceStyle.GLASS,
        cornerStyle = CornerStyle.ROUNDED,
        glassTransparency = glassTransparency,
        textScale = 1.0f,
    )
}

fun buildAyshaColors(
    palette: MayaPalette,
    accentOverride: AccentPalette? = null,
    surfaceStyle: SurfaceStyle = SurfaceStyle.GLASS,
    cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    glassTransparency: Float = 0.72f,
    textScale: Float = 1.0f,
): AyshaColors {
    val alphaBase = when (surfaceStyle) {
        SurfaceStyle.FLAT, SurfaceStyle.CLAY -> 0.96f
        SurfaceStyle.SOFT -> 0.88f
        SurfaceStyle.GLASS -> glassTransparency.coerceIn(0.35f, 0.95f)
    }
    val primary = accentOverride?.primaryColor ?: Color(palette.accentHex)
    val secondary = accentOverride?.secondaryColor ?: Color(palette.secondaryHex)
    val glow = accentOverride?.glowColor ?: Color(palette.accentHex)
    val baseSurface = Color(palette.surfaceHex)
    val elevSurface = Color(palette.elevatedHex)

    return AyshaColors(
        isDark = palette.isDark,
        background = Color(palette.bgHex),
        backgroundDeep = Color(palette.bgDeepHex),
        glassSurface = baseSurface.copy(
            alpha = if (surfaceStyle == SurfaceStyle.GLASS) {
                (alphaBase * if (palette.isDark) 0.56f else 0.54f).coerceIn(0.24f, 0.72f)
            } else {
                1f
            },
        ),
        glassSurfaceElevated = elevSurface.copy(
            alpha = if (surfaceStyle == SurfaceStyle.GLASS) {
                (alphaBase * if (palette.isDark) 0.70f else 0.66f).coerceIn(0.32f, 0.80f)
            } else {
                1f
            },
        ),
        glassBorder = if (surfaceStyle == SurfaceStyle.GLASS) {
            if (palette.isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.54f)
        } else {
            Color(palette.borderHex)
        },
        glassHighlight = if (palette.isDark) {
            Color.White.copy(alpha = if (surfaceStyle == SurfaceStyle.GLASS) 0.28f else if (surfaceStyle == SurfaceStyle.CLAY) 0.24f else 0.14f)
        } else {
            Color.White.copy(alpha = if (surfaceStyle == SurfaceStyle.GLASS) 0.62f else 0.84f)
        },
        textPrimary = Color(palette.textPrimaryHex),
        textSecondary = Color(palette.textSecondaryHex),
        accentPrimary = primary,
        accentSecondary = secondary,
        accentGlow = glow,
        success = if (palette.isDark) Color(0xFF22C55E) else Color(0xFF16A34A),
        warning = if (palette.isDark) Color(0xFFF59E0B) else Color(0xFFD97706),
        error = if (palette.isDark) Color(0xFFF43F5E) else Color(0xFFE11D48),
        transparencyFactor = alphaBase,
        surfaceStyle = surfaceStyle,
        cornerRadiusDp = cornerStyle.radiusDp,
        textScale = textScale,
        palette = palette,
    )
}

fun resolveComposeFontFamily(uiFont: UiFontFamily): FontFamily {
    return when (uiFont) {
        UiFontFamily.INTER -> FontFamily(
            Font(R.font.inter_regular, FontWeight.Normal),
            Font(R.font.inter_medium, FontWeight.Medium),
            Font(R.font.inter_semibold, FontWeight.SemiBold),
            Font(R.font.inter_bold, FontWeight.Bold),
        )
        UiFontFamily.NUNITO -> FontFamily(
            Font(R.font.nunito, FontWeight.Normal),
            Font(R.font.nunito, FontWeight.Medium),
            Font(R.font.nunito, FontWeight.SemiBold),
            Font(R.font.nunito, FontWeight.Bold),
        )
        UiFontFamily.JETBRAINS_MONO -> FontFamily(
            Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
            Font(R.font.jetbrains_mono_regular, FontWeight.Medium),
            Font(R.font.jetbrains_mono_bold, FontWeight.SemiBold),
            Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
        )
        UiFontFamily.SERIF -> FontFamily.Serif
        UiFontFamily.SYSTEM -> FontFamily.Default
    }
}

private fun scaledTypography(fontFamily: FontFamily, scale: Float): Typography {
    val s = scale.coerceIn(0.85f, 1.25f)
    return Typography(
        displaySmall = TextStyle(fontFamily = fontFamily, fontSize = (34 * s).sp, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontSize = (24 * s).sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontFamily = fontFamily, fontSize = (20 * s).sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontFamily = fontFamily, fontSize = (16 * s).sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontSize = (15 * s).sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontSize = (14 * s).sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontFamily = fontFamily, fontSize = (12 * s).sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontFamily = fontFamily, fontSize = (14 * s).sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontFamily = fontFamily, fontSize = (12 * s).sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontFamily = fontFamily, fontSize = (11 * s).sp, fontWeight = FontWeight.Medium),
    )
}

@Composable
fun AyshaTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val palette = settings.mayaPalette
    val accentForPalette = if (palette == MayaPalette.MIDNIGHT || palette == MayaPalette.DAYLIGHT) {
        settings.accentPalette
    } else {
        null
    }
    val ayshaColors = buildAyshaColors(
        palette = palette,
        accentOverride = accentForPalette,
        surfaceStyle = settings.surfaceStyle,
        cornerStyle = settings.cornerStyle,
        glassTransparency = settings.glassTransparency,
        textScale = settings.uiScale.factor,
    )
    AyshaThemeInternal(
        ayshaColors = ayshaColors,
        uiFont = settings.uiFont,
        uiScale = settings.uiScale,
        content = content,
    )
}

@Composable
fun AyshaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentPalette: AccentPalette = AccentPalette.COSMIC_CYAN,
    glassTransparency: Float = 0.72f,
    content: @Composable () -> Unit,
) {
    val ayshaColors = buildAyshaColors(themeMode, accentPalette, glassTransparency)
    AyshaThemeInternal(
        ayshaColors = ayshaColors,
        uiFont = UiFontFamily.INTER,
        uiScale = UiTextScale.NORMAL,
        content = content,
    )
}

@Composable
private fun AyshaThemeInternal(
    ayshaColors: AyshaColors,
    uiFont: UiFontFamily,
    uiScale: UiTextScale,
    content: @Composable () -> Unit,
) {
    val bgAnim by animateColorAsState(ayshaColors.background, tween(280), label = "bg_anim")
    val primaryAnim by animateColorAsState(ayshaColors.accentPrimary, tween(280), label = "pri_anim")
    val secondaryAnim by animateColorAsState(ayshaColors.accentSecondary, tween(280), label = "sec_anim")
    val fontFamily = remember(uiFont) { resolveComposeFontFamily(uiFont) }
    val typography = remember(fontFamily, uiScale) { scaledTypography(fontFamily, uiScale.factor) }

    val materialScheme = if (ayshaColors.isDark) {
        darkColorScheme(
            primary = primaryAnim,
            secondary = secondaryAnim,
            background = bgAnim,
            surface = ayshaColors.glassSurfaceElevated,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = ayshaColors.textPrimary,
            onSurface = ayshaColors.textPrimary,
            error = ayshaColors.error,
        )
    } else {
        lightColorScheme(
            primary = primaryAnim,
            secondary = secondaryAnim,
            background = bgAnim,
            surface = ayshaColors.glassSurfaceElevated,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = ayshaColors.textPrimary,
            onSurface = ayshaColors.textPrimary,
            error = ayshaColors.error,
        )
    }

    CompositionLocalProvider(LocalAyshaColors provides ayshaColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = typography,
            content = content,
        )
    }
}

@Composable
fun StaggeredAnimatedEntry(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay((index.coerceIn(0, 10) * 32L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(260, easing = FastOutSlowInEasing)) +
            slideInVertically(
                initialOffsetY = { (it * 0.14f).toInt().coerceAtLeast(18) },
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            ) +
            scaleIn(initialScale = 0.96f, animationSpec = tween(260, easing = FastOutSlowInEasing)),
    ) {
        content()
    }
}

@Composable
fun LiquidAuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalAyshaColors.current
    val isPureMinimal = c.palette == MayaPalette.OBSIDIAN || c.surfaceStyle == SurfaceStyle.FLAT
    val inf = rememberInfiniteTransition(label = "aurora_drift")
    val drift by inf.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val ribbonShift by inf.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(tween(5200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ribbon_shift",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(c.background, c.backgroundDeep, c.background),
                ),
            ),
    ) {
        if (!isPureMinimal && c.isDark) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.accentPrimary.copy(alpha = 0.17f * drift),
                            Color.Transparent,
                        ),
                        center = Offset(w * (0.20f + ribbonShift), h * 0.16f),
                        radius = w * 0.90f * drift,
                    ),
                    center = Offset(w * (0.20f + ribbonShift), h * 0.16f),
                    radius = w * 0.90f * drift,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.accentSecondary.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                        center = Offset(w * (0.86f - ribbonShift), h * 0.56f),
                        radius = w * 0.92f,
                    ),
                    center = Offset(w * (0.86f - ribbonShift), h * 0.56f),
                    radius = w * 0.92f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.accentGlow.copy(alpha = 0.11f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.45f, h * 0.92f),
                        radius = w * 0.72f * drift,
                    ),
                    center = Offset(w * 0.45f, h * 0.92f),
                    radius = w * 0.72f * drift,
                )

                val wave1 = Path().apply {
                    moveTo(-w * 0.1f, h * (0.76f + ribbonShift * 0.4f))
                    cubicTo(w * 0.25f, h * 0.60f, w * 0.62f, h * 0.88f, w * 1.1f, h * (0.66f - ribbonShift * 0.4f))
                    lineTo(w * 1.1f, h * 1.05f)
                    lineTo(-w * 0.1f, h * 1.05f)
                    close()
                }
                drawPath(
                    path = wave1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            c.accentPrimary.copy(alpha = 0.10f),
                            c.accentSecondary.copy(alpha = 0.14f),
                            c.accentGlow.copy(alpha = 0.10f),
                        ),
                    ),
                )

                val ribbonStroke = Path().apply {
                    moveTo(-w * 0.05f, h * (0.73f + ribbonShift * 0.35f))
                    cubicTo(w * 0.28f, h * 0.57f, w * 0.64f, h * 0.86f, w * 1.05f, h * (0.64f - ribbonShift * 0.35f))
                }
                drawPath(
                    path = ribbonStroke,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            c.accentPrimary.copy(alpha = 0.24f),
                            c.accentSecondary.copy(alpha = 0.26f),
                            c.accentGlow.copy(alpha = 0.20f),
                            Color.Transparent,
                        ),
                    ),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        } else if (!isPureMinimal) {
            // Calm Mist Liquid Glass Refractive Mesh (soft atmospheric refractions, zero white glare)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accentPrimary.copy(alpha = 0.16f * drift), Color.Transparent),
                        center = Offset(w * (0.16f + ribbonShift), h * 0.15f),
                        radius = w * 0.88f * drift,
                    ),
                    center = Offset(w * (0.16f + ribbonShift), h * 0.15f),
                    radius = w * 0.88f * drift,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.accentSecondary.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(w * (0.86f - ribbonShift), h * 0.48f),
                        radius = w * 0.86f,
                    ),
                    center = Offset(w * (0.86f - ribbonShift), h * 0.48f),
                    radius = w * 0.86f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(c.glassSurfaceElevated.copy(alpha = 0.36f), Color.Transparent),
                        center = Offset(w * 0.48f, h * 0.28f),
                        radius = w * 0.90f * drift,
                    ),
                    center = Offset(w * 0.48f, h * 0.28f),
                    radius = w * 0.90f * drift,
                )
                val mistWave = Path().apply {
                    moveTo(-w * 0.1f, h * (0.78f + ribbonShift * 0.4f))
                    cubicTo(w * 0.25f, h * 0.64f, w * 0.64f, h * 0.89f, w * 1.1f, h * (0.68f - ribbonShift * 0.4f))
                    lineTo(w * 1.1f, h * 1.05f)
                    lineTo(-w * 0.1f, h * 1.05f)
                    close()
                }
                drawPath(
                    path = mistWave,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            c.accentPrimary.copy(alpha = 0.08f),
                            c.accentSecondary.copy(alpha = 0.10f),
                            c.accentGlow.copy(alpha = 0.08f),
                        ),
                    ),
                )
                val silkPath = Path().apply {
                    moveTo(-w * 0.05f, h * (0.77f + ribbonShift * 0.4f))
                    cubicTo(w * 0.26f, h * 0.64f, w * 0.68f, h * 0.88f, w * 1.05f, h * (0.69f - ribbonShift * 0.4f))
                }
                drawPath(
                    path = silkPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            c.accentPrimary.copy(alpha = 0.18f),
                            Color.White.copy(alpha = 0.34f),
                            c.accentSecondary.copy(alpha = 0.17f),
                            Color.Transparent,
                        ),
                    ),
                    style = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevated: Boolean = false,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalAyshaColors.current
    val resolvedRadius = if (cornerRadius == 20.dp) c.cornerRadiusDp.dp else cornerRadius
    val shape: Shape = RoundedCornerShape(resolvedRadius)
    val baseFill = if (elevated) c.glassSurfaceElevated else c.glassSurface
    val fillBrush = when (c.surfaceStyle) {
        SurfaceStyle.FLAT -> Brush.linearGradient(listOf(baseFill, baseFill))
        SurfaceStyle.CLAY -> if (c.isDark) {
            Brush.verticalGradient(listOf(baseFill.copy(alpha = 1f), c.backgroundDeep.copy(alpha = 0.98f)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFCFDFF), Color(0xFFEFF4FE)))
        }
        SurfaceStyle.SOFT -> Brush.verticalGradient(listOf(baseFill, baseFill.copy(alpha = 0.92f)))
        SurfaceStyle.GLASS -> Brush.linearGradient(
            colors = if (c.isDark) {
                listOf(
                    Color.White.copy(alpha = 0.08f),
                    baseFill.copy(alpha = (c.transparencyFactor * 0.74f).coerceIn(0.28f, 0.64f)),
                    c.accentPrimary.copy(alpha = 0.07f),
                    Color.Black.copy(alpha = 0.24f),
                )
            } else {
                listOf(
                    Color.White.copy(alpha = (c.transparencyFactor * 0.76f).coerceIn(0.36f, 0.66f)),
                    baseFill.copy(alpha = (c.transparencyFactor * 0.68f).coerceIn(0.32f, 0.60f)),
                    c.accentPrimary.copy(alpha = 0.06f),
                    c.backgroundDeep.copy(alpha = 0.22f),
                )
            },
        )
    }

    val actualBorder = borderColor ?: c.glassBorder
    val borderWidth = when (c.surfaceStyle) {
        SurfaceStyle.CLAY -> 1.4.dp
        SurfaceStyle.FLAT -> 1.dp
        SurfaceStyle.SOFT -> 0.8.dp
        SurfaceStyle.GLASS -> if (elevated) 1.25.dp else 1.05.dp
    }
    val borderBrush = when {
        c.surfaceStyle == SurfaceStyle.FLAT -> Brush.linearGradient(listOf(actualBorder, actualBorder))
        c.surfaceStyle == SurfaceStyle.CLAY && !c.isDark -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.88f), Color(0x66B7CBEB)),
        )
        c.surfaceStyle == SurfaceStyle.GLASS -> Brush.linearGradient(
            listOf(
                c.glassHighlight,
                actualBorder.copy(alpha = 0.56f),
                c.accentPrimary.copy(alpha = if (elevated) 0.26f else 0.16f),
                if (c.isDark) Color.White.copy(alpha = 0.08f) else c.backgroundDeep.copy(alpha = 0.45f),
            ),
        )
        else -> Brush.linearGradient(listOf(c.glassHighlight, actualBorder, c.accentSecondary.copy(alpha = 0.22f)))
    }
    val clayShadowMod = when (c.surfaceStyle) {
        SurfaceStyle.CLAY -> Modifier.shadow(
            elevation = if (elevated) 14.dp else 10.dp,
            shape = shape,
            clip = false,
            ambientColor = if (c.isDark) Color.Black else Color(0x5A6F8BB8),
            spotColor = if (c.isDark) Color.Black else Color(0x6B5878AE),
        )
        SurfaceStyle.GLASS -> Modifier.shadow(
            elevation = if (elevated) 10.dp else 5.dp,
            shape = shape,
            clip = false,
            ambientColor = if (c.isDark) Color.Black.copy(alpha = 0.28f) else Color(0xFF475E82).copy(alpha = 0.16f),
            spotColor = if (c.isDark) c.accentPrimary.copy(alpha = 0.16f) else Color(0xFF3E5579).copy(alpha = 0.18f),
        )
        SurfaceStyle.SOFT -> Modifier.shadow(elevation = 4.dp, shape = shape, clip = false)
        else -> Modifier
    }
    val opticalGlintMod = if (c.surfaceStyle == SurfaceStyle.GLASS) {
        Modifier.drawBehind {
            val radiusPx = resolvedRadius.toPx()
            // Top diagonal frosted depth mist band
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (c.isDark) 0.06f else 0.18f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.48f,
                ),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )
            // Delicate top-edge optical specular catch-light
            val edgeInset = (radiusPx * 0.45f).coerceAtMost(size.width * 0.35f)
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        c.glassHighlight.copy(alpha = if (c.isDark) 0.24f else 0.42f),
                        c.accentPrimary.copy(alpha = 0.14f),
                        Color.Transparent,
                    ),
                ),
                start = Offset(edgeInset, 1.dp.toPx()),
                end = Offset((size.width - edgeInset).coerceAtLeast(edgeInset), 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
    } else {
        Modifier
    }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "card_press_scale",
    )
    val clickMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(pressScale)
            .then(clayShadowMod)
            .clip(shape)
            .background(fillBrush, shape)
            .then(opticalGlintMod)
            .border(width = borderWidth, brush = borderBrush, shape = shape)
            .then(clickMod)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassPill(
    text: String,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = LocalAyshaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pillScale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 540f),
        label = "pill_press",
    )
    val clickMod = if (onClick != null) {
        Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .scale(pillScale)
            .clip(CircleShape)
            .then(clickMod),
        shape = CircleShape,
        color = if (c.surfaceStyle == SurfaceStyle.GLASS) {
            c.glassSurfaceElevated.copy(alpha = if (c.isDark) 0.58f else 0.62f)
        } else {
            c.glassSurfaceElevated
        },
        border = BorderStroke(
            width = if (c.surfaceStyle == SurfaceStyle.GLASS) 0.95.dp else 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    c.glassHighlight,
                    c.glassBorder.copy(alpha = 0.65f),
                    c.accentPrimary.copy(alpha = 0.20f),
                ),
            ),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                color = c.textPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun AuroraGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    cornerRadius: Dp = 26.dp,
    enabled: Boolean = true,
) {
    val c = LocalAyshaColors.current
    val shape = RoundedCornerShape(cornerRadius)
    val startCol = if (c.surfaceStyle == SurfaceStyle.CLAY && !c.isDark) Color(0xFF60B2FF) else c.accentPrimary
    val endCol = if (c.surfaceStyle == SurfaceStyle.CLAY && !c.isDark) Color(0xFF2778FF) else c.accentSecondary
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 520f),
        label = "btn_press_scale",
    )
    val sheenInf = rememberInfiniteTransition(label = "btn_sheen")
    val sheenX by sheenInf.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "sheen_x",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .height(height)
            .shadow(
                elevation = if (enabled) 9.dp else 2.dp,
                shape = shape,
                clip = false,
                ambientColor = endCol.copy(alpha = 0.26f),
                spotColor = endCol.copy(alpha = 0.34f),
            )
            .clip(shape)
            .background(
                if (enabled) Brush.verticalGradient(listOf(startCol.copy(alpha = 0.90f), endCol.copy(alpha = 0.92f)))
                else Brush.verticalGradient(listOf(c.glassSurface, c.backgroundDeep)),
                shape,
            )
            .border(1.1.dp, Color.White.copy(alpha = if (enabled) 0.30f else 0.16f), shape)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val x = size.width * sheenX
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        startX = x - size.width * 0.28f,
                        endX = x + size.width * 0.28f,
                    ),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else c.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = if (enabled) Color.White else c.textSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ClayCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    showBadgeDot: Boolean = false,
    tint: Color? = null,
) {
    val c = LocalAyshaColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 550f),
        label = "clay_circle_press",
    )
    Box(
        modifier = modifier
            .scale(pressScale)
            .size(size)
            .shadow(
                elevation = if (c.isDark) 4.dp else 7.dp,
                shape = CircleShape,
                clip = false,
                ambientColor = if (c.isDark) Color.Black.copy(alpha = 0.35f) else Color(0x2E4B648E),
                spotColor = if (c.isDark) Color.Black.copy(alpha = 0.38f) else Color(0x33405982),
            )
            .clip(CircleShape)
            .background(
                when {
                    c.surfaceStyle == SurfaceStyle.GLASS && c.isDark -> Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.10f),
                            c.glassSurfaceElevated.copy(alpha = 0.60f),
                            c.accentPrimary.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.24f),
                        ),
                    )
                    c.surfaceStyle == SurfaceStyle.GLASS -> Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.58f),
                            c.glassSurface.copy(alpha = 0.54f),
                            c.backgroundDeep.copy(alpha = 0.24f),
                        ),
                    )
                    c.isDark -> Brush.verticalGradient(listOf(c.glassSurfaceElevated, c.backgroundDeep))
                    else -> Brush.verticalGradient(listOf(Color(0xFFFCFDFF), Color(0xFFE7EFFD)))
                },
                CircleShape,
            )
            .border(
                width = 1.05.dp,
                brush = Brush.linearGradient(
                    listOf(
                        c.glassHighlight,
                        c.glassBorder.copy(alpha = 0.60f),
                        c.accentPrimary.copy(alpha = 0.20f),
                    ),
                ),
                shape = CircleShape,
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: c.textPrimary,
            modifier = Modifier.size(size * 0.48f),
        )
        if (showBadgeDot) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEC4899)),
            )
        }
    }
}

@Composable
fun ClayRecessedBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalAyshaColors.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                when {
                    c.surfaceStyle == SurfaceStyle.GLASS && c.isDark -> Brush.linearGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.28f),
                            c.backgroundDeep.copy(alpha = 0.62f),
                            c.glassSurface.copy(alpha = 0.50f),
                        ),
                    )
                    c.surfaceStyle == SurfaceStyle.GLASS -> Brush.linearGradient(
                        listOf(
                            c.backgroundDeep.copy(alpha = 0.48f),
                            Color.White.copy(alpha = 0.46f),
                            c.glassSurfaceElevated.copy(alpha = 0.56f),
                        ),
                    )
                    c.isDark -> Brush.verticalGradient(listOf(c.backgroundDeep, c.glassSurface))
                    else -> Brush.verticalGradient(listOf(Color(0xFFD6E2F6), Color(0xFFEDF3FE)))
                },
                shape,
            )
            .border(
                width = 1.05.dp,
                brush = Brush.linearGradient(
                    listOf(
                        c.glassHighlight,
                        c.glassBorder.copy(alpha = 0.60f),
                        c.accentPrimary.copy(alpha = 0.20f),
                    ),
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun ColoredIconBadge(
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val c = LocalAyshaColors.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (c.surfaceStyle == SurfaceStyle.CLAY && !c.isDark) {
                    Brush.linearGradient(listOf(Color(0xFFEAF2FF), Color(0xFFDCE8FD)))
                } else {
                    Brush.linearGradient(listOf(tintColor.copy(alpha = 0.85f), tintColor.copy(alpha = 0.55f)))
                },
            )
            .border(1.dp, c.glassHighlight.copy(alpha = if (c.isDark) 0.85f else 0.75f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (c.surfaceStyle == SurfaceStyle.CLAY && !c.isDark) tintColor else Color.White,
            modifier = Modifier.size(size * 0.54f),
        )
    }
}
