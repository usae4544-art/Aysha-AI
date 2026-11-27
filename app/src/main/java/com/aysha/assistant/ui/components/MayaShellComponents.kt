package com.aysha.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.R
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.SurfaceStyle
import com.aysha.assistant.core.ui.ClayCircleButton
import com.aysha.assistant.core.ui.ClayRecessedBox
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.core.ui.StaggeredAnimatedEntry
import com.aysha.assistant.ui.screens.SettingsSubRoute

enum class PrimaryBottomTab {
    HOME,
    SCAN,
    YAADEIN,
    CHAT,
}

@Composable
fun MayaTopBar(
    title: String,
    showMenuInsteadOfBack: Boolean,
    unreadNotifications: Boolean = true,
    isDarkMode: Boolean? = null,
    onToggleDarkMode: (() -> Unit)? = null,
    onLeadingClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAyshaColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            ClayCircleButton(
                icon = if (showMenuInsteadOfBack) Icons.Rounded.Menu else Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = if (showMenuInsteadOfBack) "Menu" else "Back",
                onClick = onLeadingClick,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                color = c.textPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isDarkMode != null && onToggleDarkMode != null) {
                ClayCircleButton(
                    icon = if (isDarkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                    contentDescription = if (isDarkMode) "Switch to Light Mode" else "Switch to Dark Mode",
                    onClick = onToggleDarkMode,
                )
            }
            ClayCircleButton(
                icon = Icons.Rounded.Notifications,
                contentDescription = "Notifications",
                showBadgeDot = unreadNotifications,
                onClick = onNotificationsClick,
            )
        }
    }
}

@Composable
fun MayaBottomNavBar(
    activeTab: PrimaryBottomTab,
    orbState: OrbState,
    onSelectTab: (PrimaryBottomTab) -> Unit,
    onMicOrStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalAyshaColors.current
    val isVoiceActive = orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING || orbState == OrbState.CONNECTING

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // Puffy 3D Clay Nav Dock Container
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp),
            cornerRadius = 26.dp,
            elevated = true,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                DockTabItem(
                    label = "Home",
                    icon = Icons.Rounded.Home,
                    selected = activeTab == PrimaryBottomTab.HOME,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(PrimaryBottomTab.HOME) },
                )
                DockTabItem(
                    label = "Scan",
                    icon = Icons.Rounded.QrCodeScanner,
                    selected = activeTab == PrimaryBottomTab.SCAN,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(PrimaryBottomTab.SCAN) },
                )
                Spacer(modifier = Modifier.width(68.dp))
                DockTabItem(
                    label = "Yaadein",
                    icon = Icons.Rounded.Psychology,
                    selected = activeTab == PrimaryBottomTab.YAADEIN,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(PrimaryBottomTab.YAADEIN) },
                )
                DockTabItem(
                    label = "Chat",
                    icon = Icons.Rounded.ChatBubbleOutline,
                    selected = activeTab == PrimaryBottomTab.CHAT,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTab(PrimaryBottomTab.CHAT) },
                )
            }
        }

        // Protruding Glowing Sky-Blue Center Mic / Stop FAB Orb with animated pulse halo
        val micInf = rememberInfiniteTransition(label = "mic_fab_pulse")
        val micPulse by micInf.animateFloat(
            initialValue = 1.0f,
            targetValue = if (isVoiceActive) 1.14f else 1.03f,
            animationSpec = infiniteRepeatable(tween(if (isVoiceActive) 700 else 2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "mic_pulse",
        )
        val fabScale by animateFloatAsState(
            targetValue = if (isVoiceActive) 1.06f else 1f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 450f),
            label = "fab_scale",
        )
        Box(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .scale(fabScale * if (isVoiceActive) micPulse else 1f)
                .size(64.dp)
                .shadow(
                    elevation = if (isVoiceActive) 16.dp else 10.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = c.accentPrimary.copy(alpha = if (isVoiceActive) 0.34f else 0.18f),
                    spotColor = c.accentSecondary.copy(alpha = if (isVoiceActive) 0.38f else 0.22f),
                )
                .clip(CircleShape)
                .background(
                    when {
                        isVoiceActive -> Brush.verticalGradient(listOf(c.accentPrimary.copy(alpha = 0.92f), c.accentSecondary.copy(alpha = 0.94f)))
                        c.surfaceStyle == SurfaceStyle.GLASS && c.isDark -> Brush.linearGradient(
                            listOf(
                                c.glassSurfaceElevated.copy(alpha = 0.72f),
                                c.accentPrimary.copy(alpha = 0.20f),
                                c.glassSurface.copy(alpha = 0.62f),
                            ),
                        )
                        c.surfaceStyle == SurfaceStyle.GLASS -> Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.60f),
                                c.glassSurfaceElevated.copy(alpha = 0.56f),
                                c.accentPrimary.copy(alpha = 0.15f),
                            ),
                        )
                        else -> Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFCDE2FF)))
                    },
                    CircleShape,
                )
                .border(
                    width = if (c.surfaceStyle == SurfaceStyle.GLASS) 2.2.dp else 3.8.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            c.glassHighlight,
                            c.accentPrimary.copy(alpha = 0.52f),
                            c.accentSecondary.copy(alpha = 0.42f),
                        ),
                    ),
                    shape = CircleShape,
                )
                .clickable(onClick = onMicOrStopClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isVoiceActive) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.92f)),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Speak",
                    tint = if (c.isDark) c.textPrimary else c.accentPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

@Composable
private fun DockTabItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    val itemScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 460f),
        label = "dock_tab_scale",
    )
    Column(
        modifier = modifier
            .scale(itemScale)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    if (c.surfaceStyle == SurfaceStyle.GLASS) {
                        Brush.linearGradient(
                            listOf(
                                c.accentPrimary.copy(alpha = if (c.isDark) 0.28f else 0.18f),
                                c.accentSecondary.copy(alpha = if (c.isDark) 0.20f else 0.12f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFF6BB3FF), Color(0xFF3E8DFF)))
                    }
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                },
            )
            .then(
                if (selected && c.surfaceStyle == SurfaceStyle.GLASS) {
                    Modifier.border(
                        width = 0.9.dp,
                        brush = Brush.linearGradient(listOf(c.glassHighlight.copy(alpha = 0.65f), c.accentPrimary.copy(alpha = 0.28f))),
                        shape = RoundedCornerShape(18.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) c.textPrimary else c.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            color = c.textPrimary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

data class MayaDrawerEntry(
    val section: String,
    val title: String,
    val icon: ImageVector,
    val route: SettingsSubRoute? = null,
    val isHome: Boolean = false,
    val hasBadge: Boolean = false,
    val activeHighlight: Boolean = false,
)

@Composable
fun MayaDrawerSheetContent(
    assistantName: String,
    versionName: String,
    activeRoute: SettingsSubRoute?,
    isHomeActive: Boolean,
    unreadNotifications: Boolean,
    isDarkMode: Boolean = false,
    isOwnerAdmin: Boolean = false,
    onToggleDarkMode: (Boolean) -> Unit = {},
    onSelectHome: () -> Unit,
    onSelectRoute: (SettingsSubRoute) -> Unit,
) {
    val c = LocalAyshaColors.current
    var search by remember { mutableStateOf("") }

    val allEntries = buildList {
        if (isOwnerAdmin) {
            add(MayaDrawerEntry("OWNER ADMIN", "Owner Admin Control", Icons.Rounded.AdminPanelSettings, route = SettingsSubRoute.OWNER_ADMIN))
        }
        addAll(
            listOf(
                MayaDrawerEntry("HOME", "Home", Icons.Rounded.Home, isHome = true, activeHighlight = isHomeActive),
                MayaDrawerEntry("HOME", "$assistantName Home", Icons.Rounded.HomeWork, route = SettingsSubRoute.SMART_HOME),

                MayaDrawerEntry("KAAM-KAAJ", "Markets", Icons.Rounded.TrendingUp, route = SettingsSubRoute.MARKETS),
                MayaDrawerEntry("KAAM-KAAJ", "Marketing", Icons.Rounded.Storefront, route = SettingsSubRoute.MARKETING),
                MayaDrawerEntry("KAAM-KAAJ", "Documents", Icons.Rounded.Description, route = SettingsSubRoute.DOCUMENTS),
                MayaDrawerEntry("KAAM-KAAJ", "Meri photos", Icons.Rounded.PhotoLibrary, route = SettingsSubRoute.PHOTOS),
                MayaDrawerEntry("KAAM-KAAJ", "Website / Coding", Icons.Rounded.Code, route = SettingsSubRoute.WEBSITE_CODING),
                MayaDrawerEntry("KAAM-KAAJ", "Terminal", Icons.Rounded.Terminal, route = SettingsSubRoute.TERMINAL),
                MayaDrawerEntry("KAAM-KAAJ", "Games with $assistantName", Icons.Rounded.SportsEsports, route = SettingsSubRoute.GAMES),
                MayaDrawerEntry("KAAM-KAAJ", "Browser", Icons.Rounded.Language, route = SettingsSubRoute.BROWSER),
                MayaDrawerEntry("KAAM-KAAJ", "Accessibility", Icons.Rounded.AccessibilityNew, route = SettingsSubRoute.ACCESSIBILITY_FIX),

                MayaDrawerEntry("SYSTEM", "Settings", Icons.Rounded.Settings, route = SettingsSubRoute.HUB),
                MayaDrawerEntry("SYSTEM", "$assistantName ke rules", Icons.Rounded.Shield, route = SettingsSubRoute.RULES_AGENTS),
                MayaDrawerEntry("SYSTEM", "Secret settings (18+)", Icons.Rounded.Favorite, route = SettingsSubRoute.SECRET_SETTINGS),
                MayaDrawerEntry("SYSTEM", "PC ⇄ Phone", Icons.Rounded.SwapHoriz, route = SettingsSubRoute.PC_PHONE),

                MayaDrawerEntry("AUR", "Privacy & SOS", Icons.Rounded.Lock, route = SettingsSubRoute.PRIVACY_SECURITY),
                MayaDrawerEntry("AUR", "About", Icons.Rounded.Info, route = SettingsSubRoute.ABOUT),
                MayaDrawerEntry("AUR", "License / Upgrade", Icons.Rounded.Star, route = SettingsSubRoute.LICENSE_INFO),
                MayaDrawerEntry("AUR", "Notifications", Icons.Rounded.Notifications, route = SettingsSubRoute.NOTIFICATIONS, hasBadge = unreadNotifications),
            ),
        )
    }

    val filtered = allEntries.filter {
        search.isBlank() || it.title.contains(search, ignoreCase = true) || it.section.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(304.dp)
            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(
                when {
                    c.surfaceStyle == SurfaceStyle.GLASS && c.isDark -> Brush.verticalGradient(
                        listOf(c.background.copy(alpha = 0.88f), c.backgroundDeep.copy(alpha = 0.94f)),
                    )
                    c.surfaceStyle == SurfaceStyle.GLASS -> Brush.verticalGradient(
                        listOf(Color(0xF0F4F8FF), Color(0xECE6EEFF)),
                    )
                    c.isDark -> Brush.verticalGradient(listOf(c.background, c.backgroundDeep))
                    else -> Brush.verticalGradient(listOf(Color(0xFFE7EFFD), Color(0xFFD8E5FC)))
                },
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(c.glassHighlight, c.glassBorder, c.accentPrimary.copy(alpha = 0.25f))),
                shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Drawer App Identity Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.aysha_portrait),
                contentDescription = assistantName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.5.dp, Color.White, RoundedCornerShape(14.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(assistantName, color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("Personal AI Companion", color = c.textSecondary, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Recessed Clay Search Pill ("Settings me dhundo")
        ClayRecessedBox(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    textStyle = TextStyle(color = c.textPrimary, fontSize = 13.5.sp),
                    cursorBrush = SolidColor(c.accentPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (search.isEmpty()) Text("Settings me dhundo", color = c.textSecondary, fontSize = 13.5.sp)
                        inner()
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Dark Mode Switch inside Drawer
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 18.dp,
            elevated = false,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                        contentDescription = null,
                        tint = c.accentPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isDarkMode) "Dark Mode ON" else "Dark Mode",
                        color = c.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val grouped = filtered.groupBy { it.section }
            grouped.forEach { (section, rows) ->
                Text(
                    text = section,
                    color = c.textSecondary,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
                rows.forEach { entry ->
                    val isSelected = (entry.isHome && isHomeActive) || (entry.route != null && entry.route == activeRoute)
                    val cardShape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isSelected) 10.dp else 7.dp,
                                shape = cardShape,
                                clip = false,
                                ambientColor = if (isSelected) c.accentPrimary.copy(alpha = 0.45f) else Color(0x45708BB8),
                            )
                            .clip(cardShape)
                            .background(
                                when {
                                    isSelected && c.surfaceStyle == SurfaceStyle.GLASS -> Brush.linearGradient(
                                        listOf(c.accentPrimary.copy(alpha = 0.42f), c.accentSecondary.copy(alpha = 0.34f)),
                                    )
                                    isSelected -> Brush.verticalGradient(listOf(Color(0xFF64B5FF), Color(0xFF2D85FF)))
                                    c.surfaceStyle == SurfaceStyle.GLASS -> Brush.linearGradient(
                                        listOf(c.glassSurfaceElevated, c.glassSurface),
                                    )
                                    c.isDark -> Brush.verticalGradient(listOf(c.glassSurfaceElevated, c.glassSurface))
                                    else -> Brush.verticalGradient(listOf(Color(0xFFFCFDFF), Color(0xFFEEF3FD)))
                                },
                                cardShape,
                            )
                            .border(
                                1.2.dp,
                                if (isSelected) c.glassHighlight else c.glassBorder,
                                cardShape,
                            )
                            .clickable {
                                if (entry.isHome) onSelectHome() else entry.route?.let(onSelectRoute)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = entry.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF0A1428) else c.accentPrimary,
                                    modifier = Modifier.size(19.dp),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = entry.title,
                                    color = if (isSelected) Color(0xFF0A1428) else c.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                )
                            }
                            if (entry.hasBadge) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF97316)),
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "v$versionName · AYSHA AI · Free-First + Licensed Pro",
            color = c.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
