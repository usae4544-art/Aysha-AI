package com.aysha.assistant

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aysha.assistant.core.ui.AuroraGradientButton
import com.aysha.assistant.core.ui.AyshaTheme
import com.aysha.assistant.core.ui.ColoredIconBadge
import com.aysha.assistant.core.ui.GlassCard
import com.aysha.assistant.core.ui.LiquidAuroraBackground
import com.aysha.assistant.core.ui.LocalAyshaColors
import com.aysha.assistant.floatingassistant.AyshaForegroundService
import com.aysha.assistant.phonecontrol.ReminderReceiver
import com.aysha.assistant.ui.AyshaViewModel
import com.aysha.assistant.ui.screens.AboutAyshaScreen
import com.aysha.assistant.ui.screens.AiProvidersScreen
import com.aysha.assistant.ui.screens.AppearanceScreen
import com.aysha.assistant.ui.screens.AutomationScreen
import com.aysha.assistant.ui.screens.BrowserScreen
import com.aysha.assistant.ui.screens.ChatScreen
import com.aysha.assistant.ui.screens.HomeOrbScreen
import com.aysha.assistant.ui.screens.LicensesScreen
import com.aysha.assistant.ui.screens.MemoryHistoryScreen
import com.aysha.assistant.ui.screens.PhoneControlScreen
import com.aysha.assistant.ui.screens.PrivacyDashboardScreen
import com.aysha.assistant.ui.screens.RulesAndAgentsScreen
import com.aysha.assistant.ui.screens.ScannerScreen
import com.aysha.assistant.ui.screens.SettingsHubScreen
import com.aysha.assistant.ui.screens.SettingsSubRoute
import com.aysha.assistant.ui.screens.SplashOnboardingScreen
import com.aysha.assistant.ui.screens.VoiceAndLanguageScreen
import com.aysha.assistant.ui.screens.WakeWordScreen
import com.aysha.assistant.ui.screens.WhiteboardScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private enum class AppScreenRoute {
    HOME,
    CHAT,
    SETTINGS,
}

class MainActivity : ComponentActivity() {

    private val viewModel: AyshaViewModel by viewModels()
    private var appUnlockedState = mutableStateOf(true)

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

    private val deviceUnlockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            appUnlockedState.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialSettings = runBlocking { (application as AyshaApplication).settingsRepository.settingsFlow.first() }
        if (initialSettings.appLockEnabled) {
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (km.isDeviceSecure) {
                appUnlockedState.value = false
                @Suppress("DEPRECATION")
                val authIntent = km.createConfirmDeviceCredentialIntent("Unlock AYSHA", "Confirm screen lock to open AYSHA")
                if (authIntent != null) {
                    deviceUnlockLauncher.launch(authIntent)
                } else {
                    appUnlockedState.value = true
                }
            }
        }

        val initialPerms = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        startupPermissionsLauncher.launch(initialPerms.toTypedArray())
        handleIncomingIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsState()
            val unlocked by appUnlockedState

            AyshaTheme(settings = settings) {
                if (!unlocked) {
                    LiquidAuroraBackground {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text("AYSHA Locked", color = LocalAyshaColors.current.textPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            AuroraGradientButton(
                                text = "Unlock with Device PIN / Biometric",
                                onClick = {
                                    val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                                    @Suppress("DEPRECATION")
                                    val authIntent = km.createConfirmDeviceCredentialIntent("Unlock AYSHA", "Verify identity")
                                    if (authIntent != null) deviceUnlockLauncher.launch(authIntent) else appUnlockedState.value = true
                                },
                            )
                        }
                    }
                } else if (!settings.onboardingCompleted) {
                    SplashOnboardingScreen(
                        onGetStarted = { viewModel.completeOnboarding() },
                    )
                } else {
                    AyshaMainRouter(
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
            val reminderSpeak = intent.getStringExtra(ReminderReceiver.EXTRA_REMINDER_SPEAK)
            viewModel.handleLaunchExtras(startVoice, targetTab, wakePhrase, reminderSpeak)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AyshaMainRouter(
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

    var currentScreen by remember { mutableStateOf(AppScreenRoute.HOME) }
    var settingsRoute by remember { mutableStateOf(SettingsSubRoute.HUB) }
    var showMoreSheet by remember { mutableStateOf(false) }

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

    LaunchedEffect(requestedTab) {
        when (requestedTab) {
            "phone_control" -> {
                currentScreen = AppScreenRoute.SETTINGS
                settingsRoute = SettingsSubRoute.PHONE_CONTROL
                viewModel.clearRequestedTab()
            }
            "scanner" -> {
                currentScreen = AppScreenRoute.SETTINGS
                settingsRoute = SettingsSubRoute.SCANNER
                viewModel.clearRequestedTab()
            }
            "whiteboard" -> {
                currentScreen = AppScreenRoute.SETTINGS
                settingsRoute = SettingsSubRoute.WHITEBOARD
                viewModel.clearRequestedTab()
            }
            "chat" -> {
                currentScreen = AppScreenRoute.CHAT
                viewModel.clearRequestedTab()
            }
        }
    }

    BackHandler(enabled = currentScreen != AppScreenRoute.HOME) {
        if (currentScreen == AppScreenRoute.SETTINGS && settingsRoute != SettingsSubRoute.HUB) {
            settingsRoute = SettingsSubRoute.HUB
        } else {
            currentScreen = AppScreenRoute.HOME
        }
    }

    LiquidAuroraBackground {
        Crossfade(
            targetState = currentScreen to settingsRoute,
            animationSpec = tween(200),
            label = "aysha_router",
        ) { (screen, sub) ->
            when (screen) {
                AppScreenRoute.HOME -> HomeOrbScreen(
                    viewModel = viewModel,
                    onOpenChat = { currentScreen = AppScreenRoute.CHAT },
                    onOpenSettings = {
                        settingsRoute = SettingsSubRoute.HUB
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                    onOpenMoreSheet = { showMoreSheet = true },
                    onOpenProviders = {
                        settingsRoute = SettingsSubRoute.AI_PROVIDERS
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                    onOpenAppearance = {
                        settingsRoute = SettingsSubRoute.APPEARANCE
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                    onOpenScanner = {
                        settingsRoute = SettingsSubRoute.SCANNER
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                    onOpenWhiteboard = {
                        settingsRoute = SettingsSubRoute.WHITEBOARD
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                    onOpenBrowser = {
                        settingsRoute = SettingsSubRoute.BROWSER
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                    onOpenRulesAgents = {
                        settingsRoute = SettingsSubRoute.RULES_AGENTS
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                    onOpenAutomation = {
                        settingsRoute = SettingsSubRoute.AUTOMATION
                        currentScreen = AppScreenRoute.SETTINGS
                    },
                )
                AppScreenRoute.CHAT -> ChatScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = AppScreenRoute.HOME },
                    onSwitchToVoiceOrb = { currentScreen = AppScreenRoute.HOME },
                )
                AppScreenRoute.SETTINGS -> when (sub) {
                    SettingsSubRoute.HUB -> SettingsHubScreen(
                        viewModel = viewModel,
                        onBackToOrb = { currentScreen = AppScreenRoute.HOME },
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
                    SettingsSubRoute.RULES_AGENTS -> RulesAndAgentsScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                    SettingsSubRoute.SCANNER -> ScannerScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                    SettingsSubRoute.WHITEBOARD -> WhiteboardScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                    SettingsSubRoute.BROWSER -> BrowserScreen(viewModel) { settingsRoute = SettingsSubRoute.HUB }
                    SettingsSubRoute.LICENSES -> LicensesScreen { settingsRoute = SettingsSubRoute.HUB }
                    SettingsSubRoute.ABOUT -> AboutAyshaScreen(
                        onOpenLicenses = { settingsRoute = SettingsSubRoute.LICENSES },
                        onBack = { settingsRoute = SettingsSubRoute.HUB },
                    )
                }
            }
        }
    }

    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false },
            containerColor = c.backgroundDeep,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("AYSHA Quick Hub", color = c.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreTile("Rules & Mood", Icons.Rounded.Tune, Color(0xFFEC4899), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.RULES_AGENTS
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                    MoreTile("Scanner", Icons.Rounded.CameraAlt, Color(0xFF0EA5E9), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.SCANNER
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreTile("Whiteboard", Icons.Rounded.EditNote, Color(0xFF10B981), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.WHITEBOARD
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                    MoreTile("Browser", Icons.Rounded.Language, Color(0xFF3B82F6), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.BROWSER
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreTile("Phone Control", Icons.Rounded.PhoneAndroid, Color(0xFF14B8A6), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.PHONE_CONTROL
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                    MoreTile("Automation", Icons.Rounded.Bolt, Color(0xFFEA580C), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.AUTOMATION
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreTile("Yaadein & Docs", Icons.Rounded.History, Color(0xFF6366F1), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.MEMORY_HISTORY
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                    MoreTile("Wake Word", Icons.Rounded.Mic, Color(0xFF06B6D4), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.WAKE_WORD
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreTile("AI Providers", Icons.Rounded.Psychology, Color(0xFF7C3AED), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.AI_PROVIDERS
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                    MoreTile("Orbs & Theme", Icons.Rounded.ColorLens, Color(0xFF10B981), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.APPEARANCE
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MoreTile("Privacy & SOS", Icons.Rounded.Lock, Color(0xFF8B5CF6), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.PRIVACY_SECURITY
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                    MoreTile("All Settings", Icons.Rounded.History, Color(0xFF2563EB), Modifier.weight(1f)) {
                        showMoreSheet = false
                        settingsRoute = SettingsSubRoute.HUB
                        currentScreen = AppScreenRoute.SETTINGS
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    pendingConfirm?.let { confirm ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPendingAction() },
            title = { Text(confirm.title) },
            text = { Text(confirm.summary) },
            confirmButton = {
                Button(onClick = { viewModel.confirmPendingAction() }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissPendingAction() }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MoreTile(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = LocalAyshaColors.current
    GlassCard(
        modifier = modifier,
        cornerRadius = 16.dp,
        elevated = true,
        onClick = onClick,
        contentPadding = PaddingValues(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColoredIconBadge(icon = icon, tintColor = color, size = 34.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = c.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
