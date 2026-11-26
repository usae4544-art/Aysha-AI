package com.aysha.assistant.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.ThemeMode

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
)

val LocalAyshaColors = staticCompositionLocalOf {
    buildAyshaColors(ThemeMode.DARK, AccentPalette.COSMIC_CYAN, 0.72f)
}

fun buildAyshaColors(
    themeMode: ThemeMode,
    accent: AccentPalette,
    glassTransparency: Float,
): AyshaColors {
    val alphaBase = glassTransparency.coerceIn(0.35f, 0.95f)
    return if (themeMode == ThemeMode.DARK) {
        AyshaColors(
            isDark = true,
            background = Color(0xFF060918),
            backgroundDeep = Color(0xFF0B112B),
            glassSurface = Color(0xFF101938).copy(alpha = (alphaBase * 0.78f).coerceIn(0.30f, 0.90f)),
            glassSurfaceElevated = Color(0xFF17224A).copy(alpha = (alphaBase * 0.88f).coerceIn(0.42f, 0.94f)),
            glassBorder = Color(0xFF93C5FD).copy(alpha = 0.24f),
            glassHighlight = Color.White.copy(alpha = 0.14f),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFF94A3B8),
            accentPrimary = accent.primaryColor,
            accentSecondary = accent.secondaryColor,
            accentGlow = accent.glowColor,
            success = Color(0xFF10B981),
            warning = Color(0xFFF59E0B),
            error = Color(0xFFF43F5E),
            transparencyFactor = alphaBase,
        )
    } else {
        AyshaColors(
            isDark = false,
            background = Color(0xFFEEF4FF),
            backgroundDeep = Color(0xFFDFE9FC),
            glassSurface = Color.White.copy(alpha = (alphaBase * 0.82f).coerceIn(0.45f, 0.94f)),
            glassSurfaceElevated = Color.White.copy(alpha = (alphaBase * 0.94f).coerceIn(0.65f, 0.98f)),
            glassBorder = accent.primaryColor.copy(alpha = 0.28f),
            glassHighlight = Color.White.copy(alpha = 0.80f),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            accentPrimary = accent.primaryColor,
            accentSecondary = accent.secondaryColor,
            accentGlow = accent.glowColor,
            success = Color(0xFF059669),
            warning = Color(0xFFD97706),
            error = Color(0xFFE11D48),
            transparencyFactor = alphaBase,
        )
    }
}

@Composable
fun AyshaTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentPalette: AccentPalette = AccentPalette.COSMIC_CYAN,
    glassTransparency: Float = 0.72f,
    content: @Composable () -> Unit,
) {
    val ayshaColors = buildAyshaColors(themeMode, accentPalette, glassTransparency)
    val bgAnim by animateColorAsState(ayshaColors.background, tween(280), label = "bg_anim")
    val primaryAnim by animateColorAsState(ayshaColors.accentPrimary, tween(280), label = "pri_anim")
    val secondaryAnim by animateColorAsState(ayshaColors.accentSecondary, tween(280), label = "sec_anim")

    val materialScheme = if (themeMode == ThemeMode.DARK) {
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(c.background, c.backgroundDeep, c.background),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c.accentPrimary.copy(alpha = if (c.isDark) 0.22f else 0.14f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.14f),
                    radius = size.width * 0.72f,
                ),
                center = Offset(size.width * 0.18f, size.height * 0.14f),
                radius = size.width * 0.72f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c.accentSecondary.copy(alpha = if (c.isDark) 0.20f else 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.48f),
                    radius = size.width * 0.78f,
                ),
                center = Offset(size.width * 0.85f, size.height * 0.48f),
                radius = size.width * 0.78f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        c.accentGlow.copy(alpha = if (c.isDark) 0.14f else 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.35f, size.height * 0.88f),
                    radius = size.width * 0.65f,
                ),
                center = Offset(size.width * 0.35f, size.height * 0.88f),
                radius = size.width * 0.65f,
            )
        }
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    elevated: Boolean = false,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalAyshaColors.current
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val fillBrush = Brush.linearGradient(
        colors = listOf(
            if (elevated) c.glassSurfaceElevated else c.glassSurface,
            (if (elevated) c.glassSurfaceElevated else c.glassSurface).copy(alpha = (c.transparencyFactor * 0.65f).coerceAtLeast(0.28f)),
        ),
    )
    val actualBorder = borderColor ?: c.glassBorder
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .background(fillBrush, shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(c.glassHighlight, actualBorder, actualBorder.copy(alpha = 0.5f)),
                ),
                shape = shape,
            )
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}
