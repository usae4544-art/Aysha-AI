package com.aysha.assistant.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.AccentPalette
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.ui.AyshaViewModel

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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = c.textPrimary)
            }
            Text("Theme & Appearance", color = c.textPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        // Side-by-side Dark Mode vs Light Mode Preview Cards matching Screen 11
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemePreviewCard(
                title = "Dark Mode",
                isDarkPreview = true,
                selected = settings.themeMode == ThemeMode.DARK,
                accent = settings.accentPalette.primaryColor,
                onClick = { viewModel.updateSettings { it.copy(themeMode = ThemeMode.DARK) } },
                modifier = Modifier.weight(1f),
            )
            ThemePreviewCard(
                title = "Light Mode",
                isDarkPreview = false,
                selected = settings.themeMode == ThemeMode.LIGHT,
                accent = settings.accentPalette.primaryColor,
                onClick = { viewModel.updateSettings { it.copy(themeMode = ThemeMode.LIGHT) } },
                modifier = Modifier.weight(1f),
            )
        }

        // Accent Color Swatches
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Accent Color", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AccentPalette.entries.forEach { palette ->
                        val active = settings.accentPalette == palette
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.primaryColor)
                                .border(
                                    width = if (active) 3.dp else 1.dp,
                                    color = if (active) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = CircleShape,
                                )
                                .clickable { viewModel.updateSettings { it.copy(accentPalette = palette) } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (active) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = palette.label, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                Text(settings.accentPalette.label, color = c.accentPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Glass Transparency Toggle & Slider
        GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp, contentPadding = PaddingValues(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Liquid Glass Transparency", color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Switch(
                        checked = settings.glassTransparency < 0.90f,
                        onCheckedChange = { enable ->
                            viewModel.updateSettings { it.copy(glassTransparency = if (enable) 0.72f else 0.94f) }
                        },
                    )
                }
                Text("Frosted Glass Depth: ${(settings.glassTransparency * 100).toInt()}%", color = c.textSecondary, fontSize = 12.sp)
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
private fun ThemePreviewCard(
    title: String,
    isDarkPreview: Boolean,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAyshaColors.current
    val bg = if (isDarkPreview) Color(0xFF0A1026) else Color(0xFFF1F5F9)
    val cardBg = if (isDarkPreview) Color(0xFF18224A) else Color.White
    val barText = if (isDarkPreview) Color(0xFF94A3B8) else Color(0xFF475569)

    GlassCard(
        modifier = modifier,
        cornerRadius = 18.dp,
        elevated = selected,
        borderColor = if (selected) c.accentPrimary else null,
        onClick = onClick,
        contentPadding = PaddingValues(10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .padding(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.6f).height(10.dp).clip(CircleShape).background(accent))
                    Box(modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).background(cardBg))
                    Box(modifier = Modifier.fillMaxWidth(0.75f).height(18.dp).clip(RoundedCornerShape(8.dp)).background(barText.copy(alpha = 0.3f)))
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = c.accentPrimary,
                        modifier = Modifier.align(Alignment.BottomEnd).size(22.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
        }
    }
}
