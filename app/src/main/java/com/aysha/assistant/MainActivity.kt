package com.aysha.assistant

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aysha.assistant.core.ui.AyshaTheme
import com.aysha.assistant.core.ui.LiquidAuroraBackground
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.floatingassistant.AyshaForegroundService
import com.aysha.assistant.ui.AyshaViewModel
import com.aysha.assistant.ui.screens.AboutAyshaScreen
import com.aysha.assistant.ui.screens.AiProvidersScreen
import com.aysha.assistant.ui.screens.AppearanceScreen
import com.aysha.assistant.ui.screens.AutomationScreen
import com.aysha.assistant.ui.screens.ChatScreen
import com.aysha.assistant.ui.screens.HomeOrbScreen
import com.aysha.assistant.ui.screens.MemoryHistoryScreen
import com.aysha.assistant.ui.screens.PhoneControlScreen
import com.aysha.assistant.ui.screens.PrivacyDashboardScreen
import com.aysha.assistant.ui.screens.SettingsHubScreen
import com.aysha.assistant.ui.screens.SettingsSubRoute
import com.aysha.assistant.ui.screens.SplashOnboardingScreen
import com.aysha.assistant.ui.screens.VoiceAndLanguageScreen
import com.aysha.assistant.ui.screens.WakeWordScreen

private data class MainTab(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {

    private val viewModel: AyshaViewModel by viewModels()

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.refreshPermissions()
        if (granted) {
            viewModel.startVoiceFromShortcut()
        } else {
            Toast.makeText(this, "Microphone permission voice aur wake word ke liye chahiye", Toast.LENGTH_SHORT).show()
        }
    }

    private val genericPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        viewModel.refreshPermissions()
    }

    private val startupPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        viewModel.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialPerms = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        startupPermissionsLauncher.launch(initialPerms.toTypedArray())
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            AyshaTheme(
                themeMode = settings.themeMode,
                accentPalette = settings.accentPalette,
                glassTransparency = settings.glassTransparency,
            ) {
                if (!settings.onboardingCompleted) {
                    SplashOnboardingScreen(
                        onGetStarted = { viewModel.completeOnboarding() },
                    )
                } else {
                    AyshaMainShell(
                        viewModel = viewModel,
                        onRequestMic = { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        onRequestSpecificPermission = { permId -> handlePermissionRequest(permId) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        intent ?: return
        runCatching {
            val startVoice = intent.getBooleanExtra(AyshaForegroundService.EXTRA_START_VOICE, false)
            val targetTab = intent.getStringExtra(AyshaForegroundService.EXTRA_TARGET_TAB)
            val wakePhrase = intent.getStringExtra(AyshaForegroundService.EXTRA_WAKE_PHRASE)
            viewModel.handleLaunchExtras(startVoice, targetTab, wakePhrase)

            if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
                val shared = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!shared.isNullOrBlank()) {
                    viewModel.sendTextMessage(shared)
                }
            }
        }
    }

    private fun handlePermissionRequest(permId: String) {
        when (permId) {
            "mic" -> genericPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            "notifications" -> if (Build.VERSION.SDK_INT >= 33) {
                genericPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            "contacts" -> genericPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            "call" -> genericPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            "sms" -> genericPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            "camera" -> genericPermissionLauncher.launch(Manifest.permission.CAMERA)
            "overlay" -> viewModel.openSystemSettings("overlay")
            "accessibility" -> viewModel.openSystemSettings("accessibility")
            "notif_listener" -> viewModel.openSystemSettings("notifications")
            "battery" -> runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName"),
                    ),
                )
            }.onFailure { viewModel.openSystemSettings("battery") }
        }
    }
}

@Composable
private fun AyshaMainShell(
    viewModel: AyshaViewModel,
    onRequestMic: () -> Unit,
    onRequestSpecificPermission: (String) -> Unit,
) {
    val c = LocalAyshaColors.current
    val context = LocalContext.current
    val pendingMic by viewModel.pendingMicPermission.collectAsState()
    val pendingOverlay by viewModel.pendingOverlayPermission.collectAsState()
    val pendingConfirm by viewModel.pendingConfirmation.collectAsState()
    val requestedTab by viewModel.requestedNavTab.collectAsState()

    LaunchedEffect(pendingMic) {
        if (pendingMic) {
            viewModel.clearMicPermissionFlag()
            onRequestMic()
        }
    }

    LaunchedEffect(pendingOverlay) {
        if (pendingOverlay) {
            viewModel.clearOverlayPermissionFlag()
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }.onFailure {
                Toast.makeText(context, "Overlay permission settings me allow karo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val tabs = listOf(
        MainTab("Orb", Icons.Rounded.AutoAwesome),
        MainTab("Chat", Icons.Rounded.ChatBubbleOutline),
        MainTab("Control", Icons.Rounded.PhoneAndroid),
        MainTab("Memory", Icons.Rounded.History),
        MainTab("Settings", Icons.Rounded.Settings),
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var settingsRoute by remember { mutableStateOf(SettingsSubRoute.HUB) }

    LaunchedEffect(requestedTab) {
        when (requestedTab) {
            "phone_control" -> {
                selectedTab = 2
                viewModel.clearRequestedTab()
            }
            "chat" -> {
                selectedTab = 1
                viewModel.clearRequestedTab()
            }
        }
    }

    BackHandler(enabled = selectedTab == 4 && settingsRoute != SettingsSubRoute.HUB) {
        settingsRoute = SettingsSubRoute.HUB
    }

    LiquidAuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(c.glassBorder),
                    )
                    NavigationBar(containerColor = c.glassSurfaceElevated) {
                        tabs.forEachIndexed { idx, tab ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = {
                                    if (idx == 4) settingsRoute = SettingsSubRoute.HUB
                                    selectedTab = idx
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = c.accentPrimary,
                                    selectedTextColor = c.accentPrimary,
                                    unselectedIconColor = c.textSecondary,
                                    unselectedTextColor = c.textSecondary,
                                    indicatorColor = c.accentPrimary.copy(alpha = 0.16f),
                                ),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)) {
                Crossfade(
                    targetState = selectedTab to settingsRoute,
                    animationSpec = tween(180),
                    label = "aysha_nav",
                ) { (tab, sub) ->
                    when (tab) {
                        0 -> HomeOrbScreen(
                            viewModel = viewModel,
                            onOpenChat = { selectedTab = 1 },
                            onOpenSettings = {
                                settingsRoute = SettingsSubRoute.HUB
                                selectedTab = 4
                            },
                            onOpenMore = {
                                settingsRoute = SettingsSubRoute.AUTOMATION
                                selectedTab = 4
                            },
                            onOpenFloating = {
                                selectedTab = 2
                            },
                        )
                        1 -> ChatScreen(
                            viewModel = viewModel,
                            onSwitchToVoiceOrb = { selectedTab = 0 },
                        )
                        2 -> PhoneControlScreen(
                            viewModel = viewModel,
                            onRequestRuntimePermission = onRequestSpecificPermission,
                            onOpenAutomation = {
                                settingsRoute = SettingsSubRoute.AUTOMATION
                                selectedTab = 4
                            },
                        )
                        3 -> MemoryHistoryScreen(viewModel = viewModel)
                        4 -> when (sub) {
                            SettingsSubRoute.HUB -> SettingsHubScreen(
                                viewModel = viewModel,
                                onNavigate = { settingsRoute = it },
                            )
                            SettingsSubRoute.AI_PROVIDERS -> AiProvidersScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                            SettingsSubRoute.VOICE_LANGUAGE -> VoiceAndLanguageScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                            SettingsSubRoute.APPEARANCE -> AppearanceScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                            SettingsSubRoute.WAKE_WORD -> WakeWordScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                            SettingsSubRoute.AUTOMATION -> AutomationScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                            SettingsSubRoute.MEMORY_HISTORY -> MemoryHistoryScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                            SettingsSubRoute.PHONE_CONTROL -> PhoneControlScreen(
                                viewModel = viewModel,
                                onRequestRuntimePermission = onRequestSpecificPermission,
                                onOpenAutomation = { settingsRoute = SettingsSubRoute.AUTOMATION },
                                onBack = { settingsRoute = SettingsSubRoute.HUB },
                            )
                            SettingsSubRoute.PRIVACY_SECURITY -> PrivacyDashboardScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                            SettingsSubRoute.ABOUT -> AboutAyshaScreen { settingsRoute = SettingsSubRoute.HUB }
                        }
                    }
                }
            }
        }
    }

    pendingConfirm?.let { confirm ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingAction() },
            title = { Text(confirm.title) },
            text = { Text(confirm.summary) },
            confirmButton = {
                Button(onClick = { viewModel.confirmPendingAction() }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingAction() }) {
                    Text("Cancel")
                }
            },
        )
    }
}
