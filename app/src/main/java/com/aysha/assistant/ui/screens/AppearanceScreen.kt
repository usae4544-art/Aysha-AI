package com.aysha.assistant.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.CornerStyle
import com.aysha.assistant.core.HudColorOption
import com.aysha.assistant.core.MayaPalette
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.OrbStyle
import com.aysha.assistant.core.SurfaceStyle
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.UiFontFamily
import com.aysha.assistant.core.UiTextScale
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.GlassPill
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel
import com.aysha.assistant.ui.components.AyshaVoiceOrb

@Composable
fun AppearanceScreen(
    viewModel: AyshaViewModel,
    onBack: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Theme & Orb Studio", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Signature Orb Style Picker (11 Maya-style Orbs)
        GlassCard(modifier = Modifier.fillMaxWidth(), elevated = true, contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Voice Orb Style (${OrbStyle.entries.size} Looks)", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                Text(settings.orbStyle.tagline, color = c.accentPrimary, fontSize = 12.sp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OrbStyle.entries.forEach { style ->
                        val active = settings.orbStyle == style
                        GlassCard(
                            modifier = Modifier.width(116.dp),
                            cornerRadius = 16.dp,
                            elevated = active,
                            borderColor = if (active) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(orbStyle = style) } },
                            contentPadding = PaddingValues(8.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                AyshaVoiceOrb(
                                    orbState = if (active) OrbState.LISTENING else OrbState.IDLE,
                                    audioLevel = if (active) 0.4f else 0.1f,
                                    onTap = { viewModel.updateSettings { it.copy(orbStyle = style) } },
                                    orbSize = 54.dp,
                                    orbStyle = style,
                                    hudColor = settings.hudColor,
                                    showCenterIcon = false,
                                )
                                Text(
                                    text = style.label,
                                    color = if (active) c.accentPrimary else c.textPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Text("HUD Orb Color", color = c.textSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HudColorOption.entries.forEach { opt ->
                        val selected = settings.hudColor == opt
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(opt.primaryHex))
                                .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable { viewModel.updateSettings { it.copy(hudColor = opt) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (selected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = opt.label, tint = Color(0xFF0A0A0A), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // Maya 9 Palettes (Obsidian, Midnight, Abyss, Ember, Nocturne, Rosewood, Clay Navy, Clay Day, Daylight)
        Text("Color Palettes (${MayaPalette.entries.size} Themes)", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MayaPalette.entries.forEach { pal ->
                val selected = settings.mayaPalette == pal
                PaletteCardPreview(
                    palette = pal,
                    selected = selected,
                    onClick = {
                        viewModel.updateSettings {
                            it.copy(
                                mayaPalette = pal,
                                themeMode = if (pal.isDark) ThemeMode.DARK else ThemeMode.LIGHT,
                                surfaceStyle = if (pal == MayaPalette.CLAY_NAVY || pal == MayaPalette.CLAY_DAY) SurfaceStyle.CLAY else it.surfaceStyle,
                            )
                        }
                    },
                )
            }
        }

        // Surface Style (Glass / Clay / Flat / Soft)
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Card Surface Style", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(settings.surfaceStyle.blurb, color = c.textSecondary, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SurfaceStyle.entries.forEach { st ->
                        val active = settings.surfaceStyle == st
                        GlassPill(
                            text = st.label,
                            dotColor = if (active) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(surfaceStyle = st) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Corner Radius & Text Scale
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Corner Style", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CornerStyle.entries.forEach { cs ->
                        val active = settings.cornerStyle == cs
                        GlassPill(
                            text = cs.label,
                            dotColor = if (active) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(cornerStyle = cs) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Text("Font Family (${settings.uiFont.label})", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(settings.uiFont.blurb, color = c.textSecondary, fontSize = 11.5.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    UiFontFamily.entries.forEach { ff ->
                        val active = settings.uiFont == ff
                        GlassPill(
                            text = ff.label,
                            dotColor = if (active) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(uiFont = ff) } },
                        )
                    }
                }

                Text("UI Size", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UiTextScale.entries.forEach { sc ->
                        val active = settings.uiScale == sc
                        GlassPill(
                            text = sc.label,
                            dotColor = if (active) c.accentPrimary else null,
                            onClick = { viewModel.updateSettings { it.copy(uiScale = sc) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // Accent Override (for Midnight / Daylight) & Glass Transparency
        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Accent Tint", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AccentPalette.entries.forEach { palette ->
                        val active = settings.accentPalette == palette
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(palette.primaryColor)
                                .border(if (active) 3.dp else 1.dp, if (active) Color.White else Color.White.copy(alpha = 0.25f), CircleShape)
                                .clickable { viewModel.updateSettings { it.copy(accentPalette = palette) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) Icon(Icons.Rounded.CheckCircle, contentDescription = palette.label, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Glass Transparency", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Switch(
                        checked = settings.glassTransparency < 0.90f,
                        onCheckedChange = { enable ->
                            viewModel.updateSettings { it.copy(glassTransparency = if (enable) 0.72f else 0.94f) }
                        },
                    )
                }
                Slider(
                    value = settings.glassTransparency,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(glassTransparency = v) } },
                    valueRange = 0.40f..0.95f,
                )
            }
        }
    }
}

@Composable
private fun PaletteCardPreview(
    palette: MayaPalette,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    GlassCard(
        modifier = Modifier.width(136.dp),
        cornerRadius = 18.dp,
        elevated = selected,
        borderColor = if (selected) Color(palette.accentHex) else null,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(palette.bgHex))
                    .border(1.dp, Color(palette.borderHex), RoundedCornerShape(12.dp))
                    .padding(8.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.55f).height(8.dp).clip(CircleShape).background(Color(palette.accentHex)))
                    Box(modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(8.dp)).background(Color(palette.surfaceHex)))
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(6.dp)).background(Color(palette.elevatedHex)))
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = Color(palette.accentHex),
                        modifier = Modifier.align(Alignment.BottomEnd).size(20.dp),
                    )
                }
            }
            Text(palette.label, color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text(palette.blurb, color = c.textSecondary, fontSize = 10.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
