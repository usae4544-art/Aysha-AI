package com.aysha.assistant.ui.components

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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HomeWork
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
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
import com.aysha.assistant.core.ui.ClayCircleButton
import com.aysha.assistant.core.ui.ClayRecessedBox
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LocalAyshaColors
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
        ClayCircleButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = "Notifications",
            showBadgeDot = unreadNotifications,
            onClick = onNotificationsClick,
        )
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

        // Protruding Glowing Sky-Blue Center Mic / Stop FAB Orb
        Box(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .size(64.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Color(0xFF2E85FF),
                    spotColor = Color(0xFF2474FF),
                )
                .clip(CircleShape)
                .background(
                    if (isVoiceActive) Brush.verticalGradient(listOf(Color(0xFF60B2FF), Color(0xFF2563EB)))
                    else Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFCDE2FF))),
                    CircleShape,
                )
                .border(
                    width = 5.dp,
                    brush = Brush.verticalGradient(listOf(Color(0xFF5CAEFF), Color(0xFF2D85FF))),
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
                        .background(Color.White),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Speak",
                    tint = Color(0xFF1D4ED8),
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(Color(0xFF6BB3FF), Color(0xFF3E8DFF)))
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
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
    onSelectHome: () -> Unit,
    onSelectRoute: (SettingsSubRoute) -> Unit,
) {
    val c = LocalAyshaColors.current
    var search by remember { mutableStateOf("") }

    val allEntries = listOf(
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
    )

    val filtered = allEntries.filter {
        search.isBlank() || it.title.contains(search, ignoreCase = true) || it.section.contains(search, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(304.dp)
            .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
            .background(
                if (c.isDark) Brush.verticalGradient(listOf(c.background, c.backgroundDeep))
                else Brush.verticalGradient(listOf(Color(0xFFE7EFFD), Color(0xFFD8E5FC))),
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

        Spacer(modifier = Modifier.height(12.dp))

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
                                if (isSelected) {
                                    Brush.verticalGradient(listOf(Color(0xFF64B5FF), Color(0xFF2D85FF)))
                                } else if (c.isDark) {
                                    Brush.verticalGradient(listOf(c.glassSurfaceElevated, c.glassSurface))
                                } else {
                                    Brush.verticalGradient(listOf(Color(0xFFFCFDFF), Color(0xFFEEF3FD)))
                                },
                                cardShape,
                            )
                            .border(
                                1.2.dp,
                                if (isSelected) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = if (c.isDark) 0.16f else 0.9f),
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
