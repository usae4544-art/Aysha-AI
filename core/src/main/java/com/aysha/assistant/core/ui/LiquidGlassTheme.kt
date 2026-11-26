package com.aysha.assistant.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
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
        glassSurface = baseSurface.copy(alpha = if (surfaceStyle == SurfaceStyle.GLASS) (alphaBase * 0.82f).coerceIn(0.32f, 0.92f) else 1f),
        glassSurfaceElevated = elevSurface.copy(alpha = if (surfaceStyle == SurfaceStyle.GLASS) (alphaBase * 0.94f).coerceIn(0.46f, 0.97f) else 1f),
        glassBorder = Color(palette.borderHex),
        glassHighlight = if (palette.isDark) Color.White.copy(alpha = if (surfaceStyle == SurfaceStyle.CLAY) 0.28f else 0.16f) else Color.White.copy(alpha = 0.90f),
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
fun LiquidAuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalAyshaColors.current
    val isPureMinimal = c.palette == MayaPalette.OBSIDIAN || c.surfaceStyle == SurfaceStyle.FLAT
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(c.background, c.backgroundDeep, c.background),
                ),
            ),
    ) {
        if (!isPureMinimal) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.accentPrimary.copy(alpha = if (c.isDark) 0.20f else 0.12f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.20f, h * 0.18f),
                        radius = w * 0.85f,
                    ),
                    center = Offset(w * 0.20f, h * 0.18f),
                    radius = w * 0.85f,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.accentSecondary.copy(alpha = if (c.isDark) 0.20f else 0.11f),
                            Color.Transparent,
                        ),
                        center = Offset(w * 0.86f, h * 0.56f),
                        radius = w * 0.88f,
                    ),
                    center = Offset(w * 0.86f, h * 0.56f),
                    radius = w * 0.88f,
                )

                val wave1 = Path().apply {
                    moveTo(-w * 0.1f, h * 0.78f)
                    cubicTo(w * 0.25f, h * 0.62f, w * 0.62f, h * 0.88f, w * 1.1f, h * 0.68f)
                    lineTo(w * 1.1f, h * 1.05f)
                    lineTo(-w * 0.1f, h * 1.05f)
                    close()
                }
                drawPath(
                    path = wave1,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            c.accentPrimary.copy(alpha = if (c.isDark) 0.15f else 0.08f),
                            c.accentSecondary.copy(alpha = if (c.isDark) 0.19f else 0.11f),
                            c.accentGlow.copy(alpha = if (c.isDark) 0.15f else 0.08f),
                        ),
                    ),
                )

                val ribbonStroke = Path().apply {
                    moveTo(-w * 0.05f, h * 0.75f)
                    cubicTo(w * 0.28f, h * 0.59f, w * 0.64f, h * 0.86f, w * 1.05f, h * 0.65f)
                }
                drawPath(
                    path = ribbonStroke,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            c.accentPrimary.copy(alpha = if (c.isDark) 0.45f else 0.22f),
                            c.accentSecondary.copy(alpha = if (c.isDark) 0.50f else 0.24f),
                            c.accentGlow.copy(alpha = if (c.isDark) 0.35f else 0.18f),
                        ),
                    ),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
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
        SurfaceStyle.CLAY -> Brush.verticalGradient(
            listOf(
                baseFill.copy(alpha = 1f),
                c.backgroundDeep.copy(alpha = if (c.isDark) 0.98f else 0.92f),
            ),
        )
        SurfaceStyle.SOFT -> Brush.verticalGradient(listOf(baseFill, baseFill.copy(alpha = 0.92f)))
        SurfaceStyle.GLASS -> Brush.linearGradient(
            colors = listOf(
                baseFill,
                baseFill.copy(alpha = (c.transparencyFactor * 0.66f).coerceAtLeast(0.30f)),
            ),
        )
    }

    val actualBorder = borderColor ?: c.glassBorder
    val borderWidth = when (c.surfaceStyle) {
        SurfaceStyle.CLAY -> 1.5.dp
        SurfaceStyle.FLAT -> 1.dp
        SurfaceStyle.SOFT -> 0.8.dp
        SurfaceStyle.GLASS -> 1.dp
    }
    val borderBrush = if (c.surfaceStyle == SurfaceStyle.FLAT) {
        Brush.linearGradient(listOf(actualBorder, actualBorder))
    } else {
        Brush.linearGradient(listOf(c.glassHighlight, actualBorder, c.accentSecondary.copy(alpha = 0.22f)))
    }
    val clayShadowMod = if (c.surfaceStyle == SurfaceStyle.CLAY || c.surfaceStyle == SurfaceStyle.SOFT) {
        Modifier.shadow(elevation = if (c.surfaceStyle == SurfaceStyle.CLAY) 8.dp else 4.dp, shape = shape, clip = false)
    } else {
        Modifier
    }
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .then(clayShadowMod)
            .clip(shape)
            .background(fillBrush, shape)
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
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .then(clickMod),
        shape = CircleShape,
        color = c.glassSurfaceElevated,
        border = BorderStroke(1.dp, c.glassBorder),
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
    height: Dp = 50.dp,
    cornerRadius: Dp = 25.dp,
) {
    val c = LocalAyshaColors.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(c.accentPrimary, c.accentSecondary)), shape)
            .border(1.dp, Color.White.copy(alpha = 0.28f), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun ColoredIconBadge(
    icon: ImageVector,
    tintColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    listOf(tintColor.copy(alpha = 0.85f), tintColor.copy(alpha = 0.55f)),
                ),
            )
            .border(1.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.54f))
    }
}
