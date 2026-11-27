package com.aysha.assistant.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aysha.assistant.AyshaApplication
import com.aysha.assistant.BuildConfig
import com.aysha.assistant.ai.AyshaLicenseManager
import com.aysha.assistant.ai.ConnectivityCheckResult
import com.aysha.assistant.ai.GeminiLiveClient
import com.aysha.assistant.ai.LiveCallbacks
import com.aysha.assistant.ai.MultiProviderAiClient
import com.aysha.assistant.ai.OfflineCommandParser
import com.aysha.assistant.ai.PromptBuilder
import com.aysha.assistant.ai.RemoteConfigManager
import com.aysha.assistant.ai.SmartMemoryExtractor
import com.aysha.assistant.ai.ToolExecutor
import com.aysha.assistant.automation.AyshaAccessibilityService
import com.aysha.assistant.automation.MacroEngine
import com.aysha.assistant.core.AdminUserRecord
import com.aysha.assistant.core.AdultModeGuard
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.AssistantMood
import com.aysha.assistant.core.AwarenessDecisionLog
import com.aysha.assistant.core.BirthYearValidation
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.CompanionIntimacyLevel
import com.aysha.assistant.core.KnowledgeDocument
import com.aysha.assistant.core.LicenseInfo
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.LiveRemoteConfig
import com.aysha.assistant.core.MacroDefinition
import com.aysha.assistant.core.MayaPalette
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.PendingActionConfirmation
import com.aysha.assistant.core.PendingConfirmationType
import com.aysha.assistant.core.StorePricingConfig
import com.aysha.assistant.core.SurfaceStyle
import com.aysha.assistant.core.ThemeMode
import com.aysha.assistant.core.ToolLogEntry
import com.aysha.assistant.core.UserGender
import com.aysha.assistant.core.UserRule
import com.aysha.assistant.core.VibePreset
import com.aysha.assistant.core.VoiceEngineMode
import com.aysha.assistant.core.VoicePersonality
import com.aysha.assistant.core.WhiteboardCard
import com.aysha.assistant.floatingassistant.AyshaForegroundService
import com.aysha.assistant.memory.BackupRestoreHelper
import com.aysha.assistant.memory.ChatExporter
import com.aysha.assistant.phonecontrol.AyshaNotificationListener
import com.aysha.assistant.phonecontrol.ReminderEventBus
import com.aysha.assistant.settings.PermissionAudit
import com.aysha.assistant.settings.PermissionAuditItem
import com.aysha.assistant.settings.SettingsRepository
import com.aysha.assistant.voice.TtsVoiceInfo
import com.aysha.assistant.wakeword.WakeModelState
import com.aysha.assistant.wakeword.WakeWordStatusBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AyshaViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AyshaApplication
    private val settingsRepo = app.settingsRepository
    private val memoryRepo = app.memoryRepository
    private val phoneControl = app.phoneControlExecutor
    private val voice = app.voiceController
    private val aiClient = MultiProviderAiClient()

    private val macroEngine = MacroEngine(phoneControl) { secs, label ->
        startInAppTimer(secs, label)
    }

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _whiteboardCards = MutableStateFlow<List<WhiteboardCard>>(emptyList())
    val whiteboardCards: StateFlow<List<WhiteboardCard>> = _whiteboardCards.asStateFlow()

    private val toolExecutor = ToolExecutor(
        phoneControl = phoneControl,
        memoryRepo = memoryRepo,
        macroEngine = macroEngine,
        onStartTimer = { secs, label -> startInAppTimer(secs, label) },
        onWhiteboardCard = { card ->
            _whiteboardCards.value = listOf(card) + _whiteboardCards.value
        },
        onReadSettings = { settings.value },
    )

    val discoveredModels: StateFlow<List<String>> = settingsRepo.discoveredModels
    val availableTtsVoices: StateFlow<List<TtsVoiceInfo>> = voice.availableVoices
    val accessibilityConnected: StateFlow<Boolean> = AyshaAccessibilityService.connected
    val notificationListenerConnected: StateFlow<Boolean> = AyshaNotificationListener.connected

    val wakeRunning: StateFlow<Boolean> = WakeWordStatusBus.isRunning
    val wakeModelState: StateFlow<WakeModelState> = WakeWordStatusBus.modelState
    val wakeStatusText: StateFlow<String> = WakeWordStatusBus.statusText
    val wakeMicEnergy: StateFlow<Float> = WakeWordStatusBus.micEnergy
    val awarenessDecisionLogs: StateFlow<List<AwarenessDecisionLog>> = WakeWordStatusBus.decisionLogs

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val memories: StateFlow<List<MemoryEntry>> = _memories.asStateFlow()

    private val _archivedMemories = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val archivedMemories: StateFlow<List<MemoryEntry>> = _archivedMemories.asStateFlow()

    private val _rules = MutableStateFlow<List<UserRule>>(emptyList())
    val rules: StateFlow<List<UserRule>> = _rules.asStateFlow()

    private val _documents = MutableStateFlow<List<KnowledgeDocument>>(emptyList())
    val documents: StateFlow<List<KnowledgeDocument>> = _documents.asStateFlow()

    private val _macros = MutableStateFlow<List<MacroDefinition>>(emptyList())
    val macros: StateFlow<List<MacroDefinition>> = _macros.asStateFlow()

    private val _toolLogs = MutableStateFlow<List<ToolLogEntry>>(emptyList())
    val toolLogs: StateFlow<List<ToolLogEntry>> = _toolLogs.asStateFlow()

    private val _permissionAudit = MutableStateFlow<List<PermissionAuditItem>>(emptyList())
    val permissionAudit: StateFlow<List<PermissionAuditItem>> = _permissionAudit.asStateFlow()

    private val _assistantMood = MutableStateFlow(AssistantMood.KHUSH)
    val assistantMood: StateFlow<AssistantMood> = _assistantMood.asStateFlow()

    private val _assistantEnergy = MutableStateFlow(88)
    val assistantEnergy: StateFlow<Int> = _assistantEnergy.asStateFlow()

    private val _agentOrbState = MutableStateFlow(OrbState.IDLE)
    private val _agentStatusText = MutableStateFlow("Tap Orb to talk — 100% Free")
    private val _lastAssistantReply = MutableStateFlow("Hello! Main Aysha hoon — bolo, aaj kya help karoon?")
    val lastAssistantReply: StateFlow<String> = _lastAssistantReply.asStateFlow()

    private val _liveMuted = MutableStateFlow(false)
    val liveMuted: StateFlow<Boolean> = _liveMuted.asStateFlow()

    private val _pendingConfirmation = MutableStateFlow<PendingActionConfirmation?>(null)
    val pendingConfirmation: StateFlow<PendingActionConfirmation?> = _pendingConfirmation.asStateFlow()

    private val _connectivityTesting = MutableStateFlow(false)
    val connectivityTesting: StateFlow<Boolean> = _connectivityTesting.asStateFlow()

    private val _connectivityResult = MutableStateFlow<ConnectivityCheckResult?>(null)
    val connectivityResult: StateFlow<ConnectivityCheckResult?> = _connectivityResult.asStateFlow()

    private val _screenInspectResult = MutableStateFlow<String?>(null)
    val screenInspectResult: StateFlow<String?> = _screenInspectResult.asStateFlow()

    private val _pendingMicPermission = MutableStateFlow(false)
    val pendingMicPermission: StateFlow<Boolean> = _pendingMicPermission.asStateFlow()

    private val _pendingOverlayPermission = MutableStateFlow(false)
    val pendingOverlayPermission: StateFlow<Boolean> = _pendingOverlayPermission.asStateFlow()

    private val _requestedNavTab = MutableStateFlow<String?>(null)
    val requestedNavTab: StateFlow<String?> = _requestedNavTab.asStateFlow()

    private val _sessionElapsedSeconds = MutableStateFlow(0L)
    val sessionElapsedSeconds: StateFlow<Long> = _sessionElapsedSeconds.asStateFlow()

    private val _followUpSecondsLeft = MutableStateFlow(0)
    val followUpSecondsLeft: StateFlow<Int> = _followUpSecondsLeft.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(0L)
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private val _timerLabel = MutableStateFlow("AYSHA Timer")
    val timerLabel: StateFlow<String> = _timerLabel.asStateFlow()

    private val _pendingAttachmentJpeg = MutableStateFlow<ByteArray?>(null)
    val pendingAttachmentJpeg: StateFlow<ByteArray?> = _pendingAttachmentJpeg.asStateFlow()

    private val _scannerReply = MutableStateFlow<String?>(null)
    val scannerReply: StateFlow<String?> = _scannerReply.asStateFlow()

    private val _scannerAnalyzing = MutableStateFlow(false)
    val scannerAnalyzing: StateFlow<Boolean> = _scannerAnalyzing.asStateFlow()

    private val _backupFeedback = MutableStateFlow<String?>(null)
    val backupFeedback: StateFlow<String?> = _backupFeedback.asStateFlow()

    private val licenseManager = AyshaLicenseManager()
    private val remoteConfigManager = RemoteConfigManager()
    private val _liveLicenseOverride = MutableStateFlow<LicenseInfo?>(null)
    private val _licenseChecking = MutableStateFlow(false)
    val licenseChecking: StateFlow<Boolean> = _licenseChecking.asStateFlow()

    private val _remoteConfig = MutableStateFlow(LiveRemoteConfig())
    val remoteConfig: StateFlow<LiveRemoteConfig> = _remoteConfig.asStateFlow()

    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate: StateFlow<Boolean> = _checkingUpdate.asStateFlow()

    private val _storePricing = MutableStateFlow(StorePricingConfig())
    val storePricing: StateFlow<StorePricingConfig> = _storePricing.asStateFlow()

    private val _adminUsers = MutableStateFlow<List<AdminUserRecord>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserRecord>> = _adminUsers.asStateFlow()

    private val _adminBusy = MutableStateFlow(false)
    val adminBusy: StateFlow<Boolean> = _adminBusy.asStateFlow()

    private val _adminFeedback = MutableStateFlow<String?>(null)
    val adminFeedback: StateFlow<String?> = _adminFeedback.asStateFlow()

    val updateAvailableInfo: StateFlow<LiveRemoteConfig?> = _remoteConfig
        .map { cfg ->
            val newerByCode = cfg.latestVersionCode > BuildConfig.VERSION_CODE
            val newerByTag = cfg.latestVersion.isNotBlank() &&
                RemoteConfigManager.isNewerVersion(cfg.latestVersion, BuildConfig.VERSION_NAME)
            if (newerByCode || newerByTag) cfg else null
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val settingsLoaded: StateFlow<Boolean> = settingsRepo.settingsFlow
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val licenseInfo: StateFlow<LicenseInfo> = combine(
        _liveLicenseOverride,
        settings,
    ) { liveOverride, currentSettings ->
        liveOverride ?: currentSettings.currentLicenseInfo()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LicenseInfo())

    private var timerJob: Job? = null
    private var sessionTickerJob: Job? = null
    private var followUpJob: Job? = null

    @Volatile
    private var continuousVoiceActive = false

    @Volatile
    private var consecutiveSilentTimeouts = 0

    @Volatile
    private var lastWakeTriggerHandledMs = 0L

    @Volatile
    private var liveAttemptActive = false

    private val liveClient = GeminiLiveClient(
        toolExecutor = toolExecutor,
        callbacks = object : LiveCallbacks {
            override fun onUserTranscript(text: String) {
                liveAttemptActive = false
                appendMessage(ChatRole.USER, text, "Gemini Live")
            }

            override fun onModelTranscript(text: String) {
                liveAttemptActive = false
                _lastAssistantReply.value = text
                appendMessage(ChatRole.ASSISTANT, text, "Gemini Live")
                refreshData()
            }

            override fun onToolStatus(summary: String) {
                _agentStatusText.value = summary
            }

            override fun onConfirmationRequired(confirmation: PendingActionConfirmation) {
                _pendingConfirmation.value = confirmation
            }

            override fun onError(message: String) {
                val shouldAutoFallback = liveAttemptActive && hasMicPermission()
                liveAttemptActive = false
                appendMessage(ChatRole.SYSTEM, message, "Live")
                if (shouldAutoFallback) {
                    _agentStatusText.value = "Live offline — Smart Voice active"
                    viewModelScope.launch {
                        delay(220L)
                        continuousVoiceActive = true
                        startStandardVoiceListen()
                    }
                } else {
                    _agentStatusText.value = message
                    resumeWakeWordIfEnabled()
                }
            }
        },
        appContext = app,
    )

    val orbState: StateFlow<OrbState> = combine(
        liveClient.orbState,
        _agentOrbState,
        voice.isListening,
        voice.isSpeaking,
    ) { live, agent, listening, speaking ->
        when {
            live != OrbState.IDLE -> live
            listening -> OrbState.LISTENING
            speaking -> OrbState.SPEAKING
            else -> agent
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OrbState.IDLE)

    val audioLevel: StateFlow<Float> = combine(
        liveClient.audioLevel,
        voice.rmsLevel,
    ) { liveLvl, localLvl ->
        maxOf(liveLvl, localLvl)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val statusText: StateFlow<String> = combine(
        liveClient.orbState,
        liveClient.statusText,
        _agentStatusText,
        voice.partialText,
    ) { liveState, liveText, agentText, partial ->
        when {
            partial.isNotBlank() -> partial
            liveState != OrbState.IDLE -> liveText
            else -> agentText
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Tap Orb to talk — 100% Free")

    val partialTranscript: StateFlow<String> = voice.partialText

    init {
        refreshData()
        refreshPermissions()
        startSessionTicker()
        observeFiredReminders()
        refreshRemoteConfig()
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                voice.setGeminiApiKey(s.geminiApiKey)
            }
        }
        viewModelScope.launch {
            settingsRepo.migrateDeprecatedFlashDefault()
            val initial = settingsRepo.settingsFlow.first()
            if (initial.firebaseRefreshToken.isNotBlank()) {
                refreshLicenseStatus(silent = true)
            }
            syncBackgroundServicesWithSettings()
        }
        viewModelScope.launch {
            WakeWordStatusBus.wakeTriggers.collect { matchedPhrase ->
                handleWakeWordTriggered(matchedPhrase)
            }
        }
    }

    fun syncBackgroundServicesWithSettings() {
        val s = settings.value
        if (s.wakeWordEnabled && hasMicPermission() && !WakeWordStatusBus.isRunning.value) {
            runCatching {
                ContextCompat.startForegroundService(
                    app,
                    Intent(app, AyshaForegroundService::class.java).apply {
                        action = AyshaForegroundService.ACTION_START_WAKEWORD
                    },
                )
            }
        }
        if (s.floatingOrbEnabled && Settings.canDrawOverlays(app)) {
            runCatching {
                ContextCompat.startForegroundService(
                    app,
                    Intent(app, AyshaForegroundService::class.java).apply {
                        action = AyshaForegroundService.ACTION_SHOW_ORB
                    },
                )
            }
        }
    }

    private fun handleWakeWordTriggered(phrase: String) {
        val now = System.currentTimeMillis()
        if (now - lastWakeTriggerHandledMs < 1400L) return
        lastWakeTriggerHandledMs = now
        _agentStatusText.value = "Sun rahi hoon ($phrase)..."
        if (!liveClient.isActive() && !voice.isListening.value && !voice.isSpeaking.value) {
            startVoiceFromShortcut()
        }
    }

    fun refreshRemoteConfig(manual: Boolean = false) {
        viewModelScope.launch {
            if (manual) _checkingUpdate.value = true
            try {
                val fetched = remoteConfigManager.fetchLiveConfig()
                if (fetched != null) {
                    _remoteConfig.value = fetched
                }
                licenseManager.fetchStorePricingConfig()?.let { pricing ->
                    _storePricing.value = pricing
                }
            } finally {
                _checkingUpdate.value = false
            }
        }
    }

    fun isVerifiedOwnerAdmin(): Boolean {
        val cur = licenseInfo.value
        return cur.status == LicenseStatus.ADMIN_ACTIVE &&
            cur.email.equals(AyshaLicenseManager.ADMIN_EMAIL, ignoreCase = true)
    }

    fun loadAdminDashboard() {
        if (!isVerifiedOwnerAdmin()) return
        viewModelScope.launch {
            _adminBusy.value = true
            _adminFeedback.value = null
            try {
                val rt = settingsRepo.settingsFlow.first().firebaseRefreshToken
                licenseManager.fetchStorePricingConfig()?.let { _storePricing.value = it }
                val res = licenseManager.fetchAdminUserDirectory(rt)
                if (res.isSuccess) {
                    _adminUsers.value = res.getOrThrow()
                } else {
                    _adminFeedback.value = res.exceptionOrNull()?.message ?: "Failed to load users"
                }
            } finally {
                _adminBusy.value = false
            }
        }
    }

    fun adminToggleBlockUser(uid: String, block: Boolean, reason: String = "") {
        if (!isVerifiedOwnerAdmin()) return
        viewModelScope.launch {
            _adminBusy.value = true
            try {
                val rt = settingsRepo.settingsFlow.first().firebaseRefreshToken
                val res = licenseManager.adminSetUserBlocked(rt, uid, block, reason)
                if (res.isSuccess) {
                    _adminFeedback.value = if (block) "User blocked & locked out." else "User unblocked."
                    val listRes = licenseManager.fetchAdminUserDirectory(rt)
                    if (listRes.isSuccess) _adminUsers.value = listRes.getOrThrow()
                } else {
                    _adminFeedback.value = res.exceptionOrNull()?.message ?: "Block action failed"
                }
            } finally {
                _adminBusy.value = false
            }
        }
    }

    fun adminGrantOrRevokeUser(uid: String, email: String, plan: LicensePlan?) {
        if (!isVerifiedOwnerAdmin()) return
        viewModelScope.launch {
            _adminBusy.value = true
            try {
                val rt = settingsRepo.settingsFlow.first().firebaseRefreshToken
                val res = licenseManager.adminGrantOrRevokeLicense(rt, uid, email, plan)
                if (res.isSuccess) {
                    _adminFeedback.value = if (plan == null || plan == LicensePlan.NONE) {
                        "License revoked."
                    } else {
                        "Granted ${plan.label}."
                    }
                    val listRes = licenseManager.fetchAdminUserDirectory(rt)
                    if (listRes.isSuccess) _adminUsers.value = listRes.getOrThrow()
                } else {
                    _adminFeedback.value = res.exceptionOrNull()?.message ?: "License update failed"
                }
            } finally {
                _adminBusy.value = false
            }
        }
    }

    fun adminSaveStorePricing(nextStore: StorePricingConfig) {
        if (!isVerifiedOwnerAdmin()) return
        viewModelScope.launch {
            _adminBusy.value = true
            try {
                val rt = settingsRepo.settingsFlow.first().firebaseRefreshToken
                val res = licenseManager.adminSaveStorePricing(rt, nextStore)
                if (res.isSuccess) {
                    _storePricing.value = nextStore
                    _adminFeedback.value = "Live prices & UPI config saved to Cloud!"
                } else {
                    _adminFeedback.value = res.exceptionOrNull()?.message ?: "Store save failed"
                }
            } finally {
                _adminBusy.value = false
            }
        }
    }

    fun toggleDarkMode(enableDark: Boolean) {
        updateSettings { cur ->
            val targetPalette = if (enableDark) {
                if (cur.mayaPalette.isDark) cur.mayaPalette else MayaPalette.MIDNIGHT
            } else {
                if (!cur.mayaPalette.isDark && cur.mayaPalette != MayaPalette.CLAY_DAY) {
                    cur.mayaPalette
                } else {
                    MayaPalette.DAYLIGHT
                }
            }
            cur.copy(
                mayaPalette = targetPalette,
                themeMode = if (enableDark) ThemeMode.DARK else ThemeMode.LIGHT,
                surfaceStyle = if (cur.surfaceStyle == SurfaceStyle.CLAY && targetPalette != MayaPalette.CLAY_DAY && targetPalette != MayaPalette.CLAY_NAVY) {
                    SurfaceStyle.GLASS
                } else {
                    cur.surfaceStyle
                },
            )
        }
    }

    fun downloadAndOpenUpdateApk(url: String) {
        val cleanUrl = url.trim().ifBlank {
            "https://github.com/usae4544-art/Aysha-AI/releases/latest/download/AYSHA-latest-release.apk"
        }
        if (!cleanUrl.startsWith("https://")) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !app.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(app, "Pehle AYSHA ke liye 'Install unknown apps' allow karo, phir Update tap karo", Toast.LENGTH_LONG).show()
            runCatching {
                app.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            return
        }
        if (_checkingUpdate.value) return
        viewModelScope.launch {
            _checkingUpdate.value = true
            _agentStatusText.value = "Update APK download ho raha hai..."
            Toast.makeText(app, "Latest AYSHA update download ho raha hai...", Toast.LENGTH_SHORT).show()
            try {
                val destFile = File(app.filesDir, "updates/AYSHA-latest-release.apk")
                val res = remoteConfigManager.downloadApkToFile(cleanUrl, destFile)
                if (res.isSuccess) {
                    val file = res.getOrThrow()
                    val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    _agentStatusText.value = "Install prompt khul raha hai..."
                    runCatching {
                        app.startActivity(installIntent)
                    }.onFailure {
                        runCatching {
                            app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                } else {
                    _agentStatusText.value = "Direct download failed — browser khol raha hoon"
                    Toast.makeText(app, "Browser se update download ho raha hai...", Toast.LENGTH_SHORT).show()
                    runCatching {
                        app.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            } finally {
                _checkingUpdate.value = false
            }
        }
    }

    fun openBrowserGoogleLogin() {
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(AyshaLicenseManager.SITE_AUTH_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun openLicensePortalInBrowser() {
        runCatching {
            app.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(AyshaLicenseManager.SITE_PORTAL_URL))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun handleAuthDeepLink(uriString: String?) {
        val parsed = AyshaLicenseManager.parseDeepLinkCallback(uriString) ?: return
        verifyManualToken(parsed.refreshToken)
    }

    fun verifyManualToken(token: String) {
        val clean = token.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _licenseChecking.value = true
            try {
                val cur = settingsRepo.settingsFlow.first()
                val outcome = licenseManager.verifyWithRefreshToken(clean, cur, BuildConfig.VERSION_NAME)
                applyLicenseOutcome(outcome, clean)
            } finally {
                _licenseChecking.value = false
            }
        }
    }

    fun refreshLicenseStatus(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _licenseChecking.value = true
            try {
                val cur = settingsRepo.settingsFlow.first()
                val outcome = licenseManager.verifyWithSettings(cur, BuildConfig.VERSION_NAME)
                applyLicenseOutcome(outcome, cur.firebaseRefreshToken)
            } finally {
                _licenseChecking.value = false
            }
        }
    }

    fun claimLicenseKey(rawKey: String) {
        val clean = rawKey.trim().uppercase()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _licenseChecking.value = true
            try {
                val cur = settingsRepo.settingsFlow.first()
                val outcome = licenseManager.claimLicenseKey(clean, cur)
                applyLicenseOutcome(outcome, cur.firebaseRefreshToken)
            } finally {
                _licenseChecking.value = false
            }
        }
    }

    private suspend fun applyLicenseOutcome(
        outcome: com.aysha.assistant.ai.LicenseVerificationOutcome,
        tokenFallback: String,
    ) {
        val lic = outcome.license
        _liveLicenseOverride.value = lic
        if (outcome.shouldClearSession) {
            settingsRepo.clearAuthAndLicense()
            return
        }
        val finalToken = outcome.updatedRefreshToken ?: tokenFallback
        if (lic.uid.isNotBlank() && finalToken.isNotBlank()) {
            val hardLocked = lic.blocked || lic.status == LicenseStatus.BLOCKED || lic.status == LicenseStatus.REVOKED
            settingsRepo.update { s ->
                s.copy(
                    firebaseRefreshToken = finalToken,
                    authUid = lic.uid,
                    authEmail = lic.email,
                    authDisplayName = lic.displayName,
                    userName = if (s.userName == "Friend" && lic.displayName.isNotBlank()) lic.displayName else s.userName,
                    cachedLicensePlan = if (hardLocked) LicensePlan.NONE else lic.plan,
                    cachedLicenseStatus = lic.status.name,
                    cachedLicenseLifetime = if (hardLocked) false else lic.isLifetime,
                    cachedLicenseExpiresAtMs = if (hardLocked) 0L else lic.expiresAtMs,
                    cachedLicenseServerNowMs = lic.lastServerNowMs,
                    cachedLicenseVerifiedDeviceMs = lic.lastVerifiedDeviceMs,
                    cachedLicenseCouponCode = lic.couponCode,
                    cachedLicenseKey = if (hardLocked) "" else lic.licenseKey,
                    cachedAccountBlocked = lic.blocked || lic.status == LicenseStatus.BLOCKED,
                    cachedBlockedReason = lic.blockedReason,
                )
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            _liveLicenseOverride.value = LicenseInfo(status = LicenseStatus.SIGNED_OUT)
            settingsRepo.clearAuthAndLicense()
        }
    }

    private fun observeFiredReminders() {
        viewModelScope.launch {
            ReminderEventBus.firedReminders.collect { msg ->
                val text = "🔔 Reminder: $msg"
                _lastAssistantReply.value = text
                appendMessage(ChatRole.ASSISTANT, text, "Reminder")
                speakText("Reminder! $msg", settings.value, null)
            }
        }
    }

    private fun startSessionTicker() {
        sessionTickerJob?.cancel()
        sessionTickerJob = viewModelScope.launch {
            while (isActive) {
                val active = orbState.value != OrbState.IDLE && orbState.value != OrbState.ERROR
                if (active || continuousVoiceActive || _followUpSecondsLeft.value > 0) {
                    delay(1000L)
                    _sessionElapsedSeconds.value += 1L
                } else {
                    if (_sessionElapsedSeconds.value != 0L) _sessionElapsedSeconds.value = 0L
                    delay(350L)
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch(Dispatchers.IO) {
            val msgs = memoryRepo.getRecentMessages(200)
            _messages.value = msgs
            msgs.lastOrNull { it.role == ChatRole.ASSISTANT }?.let {
                _lastAssistantReply.value = it.text
            }
            _memories.value = memoryRepo.getAllMemories(includeArchived = false)
            _archivedMemories.value = memoryRepo.getArchivedMemories()
            _rules.value = memoryRepo.getAllRules()
            _documents.value = memoryRepo.getAllDocuments()
            _macros.value = memoryRepo.getAllMacros()
            _toolLogs.value = memoryRepo.getRecentToolLogs(50)
        }
    }

    fun refreshPermissions() {
        _permissionAudit.value = PermissionAudit.inspectAll(app)
    }

    fun clearMicPermissionFlag() {
        _pendingMicPermission.value = false
    }

    fun clearOverlayPermissionFlag() {
        _pendingOverlayPermission.value = false
    }

    fun clearRequestedTab() {
        _requestedNavTab.value = null
    }

    fun handleLaunchExtras(startVoice: Boolean, targetTab: String?, wakePhrase: String?, reminderSpeak: String? = null) {
        if (!targetTab.isNullOrBlank()) {
            _requestedNavTab.value = targetTab
        }
        if (!reminderSpeak.isNullOrBlank()) {
            val text = "🔔 Reminder: $reminderSpeak"
            _lastAssistantReply.value = text
            _agentStatusText.value = text
            speakText("Reminder! $reminderSpeak", settings.value, null)
        }
        if (!wakePhrase.isNullOrBlank()) {
            val now = System.currentTimeMillis()
            if (now - lastWakeTriggerHandledMs < 1400L) return
            lastWakeTriggerHandledMs = now
            _agentStatusText.value = "Sun rahi hoon ($wakePhrase)..."
        }
        if (startVoice) {
            startVoiceFromShortcut()
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun pauseWakeWordForActiveMic() {
        if (settings.value.wakeWordEnabled) {
            runCatching {
                app.startService(
                    Intent(app, AyshaForegroundService::class.java).apply {
                        action = AyshaForegroundService.ACTION_PAUSE_WAKEWORD
                    },
                )
            }
        }
    }

    private fun resumeWakeWordIfEnabled() {
        if (settings.value.wakeWordEnabled) {
            runCatching {
                ContextCompat.startForegroundService(
                    app,
                    Intent(app, AyshaForegroundService::class.java).apply {
                        action = AyshaForegroundService.ACTION_RESUME_WAKEWORD
                    },
                )
            }
        }
    }

    fun onOrbTap() {
        followUpJob?.cancel()
        _followUpSecondsLeft.value = 0
        val current = settings.value
        if (liveClient.isActive()) {
            liveAttemptActive = false
            liveClient.stop()
            _liveMuted.value = false
            resumeWakeWordIfEnabled()
            return
        }
        if (voice.isListening.value || voice.isSpeaking.value || continuousVoiceActive) {
            stopActiveVoiceOrSpeech()
            return
        }
        if (!hasMicPermission()) {
            _pendingMicPermission.value = true
            _agentStatusText.value = "Microphone permission needed"
            return
        }

        viewModelScope.launch {
            val latest = settingsRepo.settingsFlow.first()
            if (latest.wakeWordEnabled) {
                pauseWakeWordForActiveMic()
                delay(180L) // ensure AudioRecord is fully released before STT/Live claims mic
            }
            consecutiveSilentTimeouts = 0
            if (latest.geminiApiKey.isNotBlank() && latest.voiceEngineMode == VoiceEngineMode.GEMINI_LIVE) {
                startGeminiLiveSession()
            } else {
                continuousVoiceActive = latest.continuousConversation
                startStandardVoiceListen()
            }
        }
    }

    fun startVoiceFromShortcut() {
        if (!liveClient.isActive() && !voice.isListening.value) {
            onOrbTap()
        }
    }

    fun stopActiveVoiceOrSpeech() {
        continuousVoiceActive = false
        followUpJob?.cancel()
        _followUpSecondsLeft.value = 0
        if (liveClient.isActive()) {
            liveClient.interruptPlayback()
            liveClient.stop()
        }
        voice.stopListening()
        voice.stopSpeaking()
        _agentOrbState.value = OrbState.IDLE
        _agentStatusText.value = "Tap Orb to talk"
        resumeWakeWordIfEnabled()
    }

    fun replayLastResponse() {
        val s = settings.value
        if (!voice.replayLast()) {
            speakText(_lastAssistantReply.value, s, onDone = null)
        }
    }

    fun speakMessage(text: String) {
        speakText(text, settings.value, onDone = null)
    }

    private fun startGeminiLiveSession() {
        viewModelScope.launch {
            val raw = settingsRepo.settingsFlow.first()
            val liveCfg = _remoteConfig.value
            val s = raw.copy(
                geminiLiveModel = liveCfg.geminiLiveModelOverride.ifBlank { raw.geminiLiveModel },
            )
            val (mems, activeRules) = withContext(Dispatchers.IO) {
                memoryRepo.getAllMemories().take(12) to memoryRepo.getAllRules()
            }
            val sys = PromptBuilder.buildSystemInstruction(
                settings = s,
                memories = mems,
                accessibilityEnabled = accessibilityConnected.value,
                notificationsEnabled = notificationListenerConnected.value,
                rules = activeRules,
                mood = _assistantMood.value,
                energy = _assistantEnergy.value,
                remoteExtraInstruction = liveCfg.systemPromptExtra,
            )
            _liveMuted.value = false
            liveAttemptActive = true
            liveClient.start(s, sys)
        }
    }

    private fun startFollowUpWindowIfConfigured() {
        val s = settings.value
        val windowSecs = s.followUpWindowSeconds
        if (windowSecs <= 0 && !s.continuousConversation) {
            continuousVoiceActive = false
            resumeWakeWordIfEnabled()
            return
        }
        if (s.continuousConversation) {
            startStandardVoiceListen()
            return
        }
        followUpJob?.cancel()
        followUpJob = viewModelScope.launch {
            _followUpSecondsLeft.value = windowSecs
            startStandardVoiceListen()
            while (isActive && _followUpSecondsLeft.value > 0) {
                delay(1000L)
                _followUpSecondsLeft.value = (_followUpSecondsLeft.value - 1).coerceAtLeast(0)
            }
            if (_followUpSecondsLeft.value == 0 && !voice.isSpeaking.value && _agentOrbState.value == OrbState.IDLE) {
                continuousVoiceActive = false
                voice.stopListening()
                resumeWakeWordIfEnabled()
            }
        }
    }

    private fun startStandardVoiceListen() {
        if (!hasMicPermission()) {
            continuousVoiceActive = false
            _followUpSecondsLeft.value = 0
            _pendingMicPermission.value = true
            _agentOrbState.value = OrbState.IDLE
            return
        }
        val s = settings.value
        _agentStatusText.value = if (_followUpSecondsLeft.value > 0) {
            "Follow-up active (${_followUpSecondsLeft.value}s) — bolo..."
        } else if (continuousVoiceActive) {
            "Continuous listening active..."
        } else {
            "Listening..."
        }

        voice.startListening(
            localeTag = s.language.sttLocaleTag,
            keepContinuousStateOnSoftTimeout = continuousVoiceActive || _followUpSecondsLeft.value > 0,
            onPartialResult = { partial ->
                _agentStatusText.value = partial
            },
            onFinalResult = { spoken ->
                consecutiveSilentTimeouts = 0
                followUpJob?.cancel()
                _followUpSecondsLeft.value = 0
                sendTextMessage(spoken, speakReply = true)
            },
            onSoftTimeout = if (continuousVoiceActive || _followUpSecondsLeft.value > 0) {
                {
                    consecutiveSilentTimeouts++
                    val withinLimit = consecutiveSilentTimeouts <= 3
                    if (withinLimit && ((continuousVoiceActive && settings.value.continuousConversation) || _followUpSecondsLeft.value > 0)) {
                        startStandardVoiceListen()
                    } else {
                        continuousVoiceActive = false
                        followUpJob?.cancel()
                        _followUpSecondsLeft.value = 0
                        voice.stopListening()
                        _agentOrbState.value = OrbState.IDLE
                        _agentStatusText.value = "Tap Orb to talk"
                        resumeWakeWordIfEnabled()
                    }
                }
            } else {
                null
            },
            onError = { err ->
                continuousVoiceActive = false
                followUpJob?.cancel()
                _followUpSecondsLeft.value = 0
                _agentOrbState.value = OrbState.ERROR
                _agentStatusText.value = err
                resumeWakeWordIfEnabled()
            },
        )
    }

    fun toggleLiveMute() {
        val next = !_liveMuted.value
        _liveMuted.value = next
        liveClient.setMuted(next)
    }

    fun attachPendingImage(jpegBytes: ByteArray?) {
        _pendingAttachmentJpeg.value = jpegBytes
    }

    fun sendTextMessage(text: String, speakReply: Boolean? = null) {
        val clean = text.trim()
        val imgBytes = _pendingAttachmentJpeg.value
        if (clean.isEmpty() && imgBytes == null) return
        _pendingAttachmentJpeg.value = null

        val promptText = clean.ifBlank { "Is image me kya dikh raha hai? Explain karo." }

        if (AdultModeGuard.isNormalResetPhrase(promptText)) {
            appendMessage(ChatRole.USER, promptText, "")
            val resetReply = "Theek hai — ab normal baat jaari rakhte hain."
            _assistantMood.value = AssistantMood.KHUSH
            _lastAssistantReply.value = resetReply
            _agentOrbState.value = OrbState.IDLE
            _agentStatusText.value = "Normal Friendly Mode"
            appendMessage(ChatRole.ASSISTANT, resetReply, "Safety Guard")
            viewModelScope.launch {
                settingsRepo.update { it.copy(companionIntimacy = CompanionIntimacyLevel.FRIENDLY) }
                val shouldSpeak = speakReply ?: (settings.value.autoSpeakReplies && settings.value.voiceEngineMode != VoiceEngineMode.TEXT_ONLY)
                if (shouldSpeak) speakText(resetReply, settings.value, null)
            }
            return
        }

        if (liveClient.isActive()) {
            liveClient.sendText(promptText)
            appendMessage(ChatRole.USER, promptText, "Gemini Live")
            return
        }

        appendMessage(ChatRole.USER, if (imgBytes != null) "📷 $promptText" else promptText, "")

        // Update mood & energy naturally
        val (nextMood, nextEnergy) = SmartMemoryExtractor.analyzeMoodShift(promptText, _assistantMood.value, _assistantEnergy.value)
        _assistantMood.value = nextMood
        _assistantEnergy.value = nextEnergy

        viewModelScope.launch {
            val s = settingsRepo.settingsFlow.first()
            _agentOrbState.value = OrbState.PROCESSING
            _agentStatusText.value = "${s.assistantName} is thinking..."

            val (mems, history, activeRules, docSnippets) = withContext(Dispatchers.IO) {
                val ranked = (memoryRepo.searchMemories(promptText, 4) + memoryRepo.getAllMemories().take(3)).distinctBy { it.id }
                val hist = memoryRepo.getRecentMessages(10)
                val allRules = memoryRepo.getAllRules()
                val snippets = memoryRepo.searchDocumentSnippets(promptText, 2)
                Quadruple(ranked, hist, allRules, snippets)
            }
            if (s.autoMemoryEnabled) {
                viewModelScope.launch(Dispatchers.IO) {
                    SmartMemoryExtractor.extractAutoFacts(promptText).forEach { f ->
                        memoryRepo.saveMemory(f.title, f.content, f.tags, approvedByUser = true, source = "baaton se samjha")
                    }
                    SmartMemoryExtractor.extractAutoRule(promptText)?.let { autoRule ->
                        memoryRepo.saveRule(autoRule, enabled = true)
                    }
                }
            }

            val liveCfg = _remoteConfig.value
            val effectiveSettings = s.copy(
                geminiTextModel = liveCfg.geminiTextModelOverride.ifBlank { s.geminiTextModel },
            )
            val sys = PromptBuilder.buildSystemInstruction(
                settings = effectiveSettings,
                memories = mems,
                accessibilityEnabled = accessibilityConnected.value,
                notificationsEnabled = notificationListenerConnected.value,
                rules = activeRules,
                mood = _assistantMood.value,
                energy = _assistantEnergy.value,
                documentSnippets = docSnippets,
                remoteExtraInstruction = liveCfg.systemPromptExtra,
            )

            val turn = runCatching {
                aiClient.runTurnWithFallback(
                    settings = effectiveSettings,
                    systemInstruction = sys,
                    history = history,
                    userText = promptText,
                    attachedImageJpeg = imgBytes,
                    toolExecutor = toolExecutor,
                    onToolEvent = { status ->
                        _agentOrbState.value = OrbState.AUTOMATING
                        _agentStatusText.value = status
                    },
                )
            }.getOrElse { err ->
                com.aysha.assistant.ai.AiTurnResult(
                    text = "API error: ${err.message ?: "Request failed. Internet aur Gemini key check karo."}",
                    providerTag = "Error",
                )
            }

            turn.inAppTimerRequest?.let { (secs, label) ->
                startInAppTimer(secs, label)
            }
            turn.confirmationRequired?.let { confirm ->
                _pendingConfirmation.value = confirm
            }

            _lastAssistantReply.value = turn.text
            appendMessage(ChatRole.ASSISTANT, turn.text, turn.providerTag)
            _agentOrbState.value = OrbState.IDLE
            _agentStatusText.value = if (turn.toolsUsed.isNotEmpty()) {
                "Executed: ${turn.toolsUsed.joinToString(", ")}"
            } else {
                turn.providerTag
            }
            refreshData()

            val shouldSpeak = speakReply ?: (s.autoSpeakReplies && s.voiceEngineMode != VoiceEngineMode.TEXT_ONLY)
            if (shouldSpeak) {
                speakText(turn.text, s) {
                    if (continuousVoiceActive) {
                        startFollowUpWindowIfConfigured()
                    } else {
                        resumeWakeWordIfEnabled()
                    }
                }
            } else if (continuousVoiceActive) {
                startFollowUpWindowIfConfigured()
            } else {
                resumeWakeWordIfEnabled()
            }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun speakText(text: String, s: AppSettings, onDone: (() -> Unit)?) {
        voice.speak(
            text = text,
            localeTag = s.language.ttsLocaleTag,
            rate = s.ttsSpeechRate,
            pitch = s.ttsPitch,
            volume = s.ttsVolume,
            personalityId = s.voicePersonalityId,
            preferredVoiceName = s.selectedTtsVoiceName,
            apiKey = s.geminiApiKey,
            bargeInEnabled = s.bargeInEnabled && hasMicPermission(),
            onBargeIn = {
                continuousVoiceActive = true
                viewModelScope.launch {
                    delay(260L)
                    startStandardVoiceListen()
                }
            },
            onError = { err ->
                _agentStatusText.value = err
            },
            onDone = onDone,
        )
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        val outcome = when (pending.type) {
            PendingConfirmationType.PHONE_CALL -> phoneControl.executeConfirmedCall(pending.target)
            PendingConfirmationType.SEND_SMS -> phoneControl.executeConfirmedSms(pending.target, pending.messageBody)
            PendingConfirmationType.EMERGENCY_SOS -> phoneControl.executeEmergencySos(pending.target, pending.messageBody)
        }
        appendMessage(
            role = if (outcome.ok) ChatRole.ASSISTANT else ChatRole.SYSTEM,
            text = outcome.message,
            providerTag = "Phone Control",
        )
        _agentStatusText.value = outcome.message
    }

    fun dismissPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        appendMessage(ChatRole.SYSTEM, "${pending.title} cancelled by user.", "Safety")
    }

    fun triggerSosConfirmation() {
        val s = settings.value
        val target = s.emergencyContact.ifBlank { "112" }
        _pendingConfirmation.value = PendingActionConfirmation(
            type = PendingConfirmationType.EMERGENCY_SOS,
            title = "Confirm Emergency SOS",
            summary = "Emergency alert/call to $target?\n\"${s.emergencySosMessage}\"",
            target = target,
            messageBody = s.emergencySosMessage,
        )
    }

    fun startInAppTimer(totalSeconds: Int, label: String = "AYSHA Timer") {
        val safe = totalSeconds.coerceIn(1, 86_400).toLong()
        timerJob?.cancel()
        _timerLabel.value = label.ifBlank { "AYSHA Timer" }
        _timerRemainingSeconds.value = safe
        _timerRunning.value = true
        runTimerTicker()
    }

    fun pauseOrResumeTimer() {
        if (_timerRemainingSeconds.value <= 0L) return
        if (_timerRunning.value) {
            timerJob?.cancel()
            _timerRunning.value = false
        } else {
            _timerRunning.value = true
            runTimerTicker()
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerRunning.value = false
        _timerRemainingSeconds.value = 0L
    }

    private fun runTimerTicker() {
        timerJob = viewModelScope.launch {
            while (isActive && _timerRunning.value && _timerRemainingSeconds.value > 0L) {
                delay(1000L)
                if (!_timerRunning.value) break
                val next = (_timerRemainingSeconds.value - 1L).coerceAtLeast(0L)
                _timerRemainingSeconds.value = next
                if (next == 0L) {
                    _timerRunning.value = false
                    val msg = "⏱ ${_timerLabel.value} complete!"
                    appendMessage(ChatRole.ASSISTANT, msg, "Timer")
                    speakText("Time poora ho gaya hai! ${_timerLabel.value} complete.", settings.value, null)
                }
            }
        }
    }

    private fun appendMessage(role: ChatRole, text: String, providerTag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.addMessage(ChatMessage(role = role, text = text, providerTag = providerTag))
            _messages.value = memoryRepo.getRecentMessages(200)
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.update(transform)
        }
    }

    fun completeOnboarding() {
        updateSettings { it.copy(onboardingCompleted = true) }
    }

    fun completeOnboardingWithSetup(
        name: String,
        gender: UserGender,
        language: com.aysha.assistant.core.AssistantLanguage,
        orbStyle: com.aysha.assistant.core.OrbStyle,
    ) {
        updateSettings {
            it.copy(
                userName = name.trim().ifBlank { it.userName },
                userGender = gender,
                language = language,
                orbStyle = orbStyle,
                onboardingCompleted = true,
            )
        }
    }

    fun setAssistantMood(mood: AssistantMood) {
        _assistantMood.value = mood
        _assistantEnergy.value = when (mood) {
            AssistantMood.KHUSH -> 90
            AssistantMood.PYAAR_BHARI -> 95
            AssistantMood.ROOTHI -> 65
            AssistantMood.NARAZ -> 45
        }
    }

    fun activateVibe(preset: VibePreset) {
        updateSettings { it.copy(selectedVibeId = preset.id) }
        viewModelScope.launch(Dispatchers.IO) {
            val res = phoneControl.launchVibe(preset.query, preset.title)
            _agentStatusText.value = res.message
            appendMessage(ChatRole.ASSISTANT, res.message, "Vibes")
        }
    }

    // ---- Camera Scanner Vision ----

    fun analyzeScannerImage(jpegBytes: ByteArray, question: String) {
        val prompt = question.trim().ifBlank { "Is photo me kya dikh raha hai? Hinglish me detail se samjhao." }
        viewModelScope.launch {
            _scannerAnalyzing.value = true
            _scannerReply.value = null
            val s = settingsRepo.settingsFlow.first()
            val turn = aiClient.runTurnWithFallback(
                settings = s,
                systemInstruction = "You are ${s.assistantName}'s Visual Scanner. Describe what you see accurately in ${s.language.label}, solve any question or read text clearly.",
                history = emptyList(),
                userText = prompt,
                attachedImageJpeg = jpegBytes,
                toolExecutor = toolExecutor,
            )
            _scannerAnalyzing.value = false
            _scannerReply.value = turn.text
            appendMessage(ChatRole.USER, "📷 Scanner: $prompt", "")
            appendMessage(ChatRole.ASSISTANT, turn.text, turn.providerTag)
            if (s.autoSpeakReplies) speakText(turn.text, s, null)
        }
    }

    fun addQuickNote(title: String, body: String) {
        val cleanTitle = title.trim().ifBlank { "Quick Note" }
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.saveMemory(
                title = cleanTitle,
                content = cleanBody,
                tags = "note,journal",
                approvedByUser = true,
                source = "notes",
            )
            memoryRepo.saveDocument(title = cleanTitle, content = cleanBody)
            refreshData()
        }
    }

    // ---- Study Whiteboard ----

    fun askWhiteboard(topicPrompt: String) {
        val clean = topicPrompt.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _agentOrbState.value = OrbState.PROCESSING
            val s = settings.value
            val turn = aiClient.runTurnWithFallback(
                settings = s,
                systemInstruction = "You are ${s.assistantName}'s Study Whiteboard Tutor. Always explain step-by-step with numbered lines and call show_whiteboard with title, steps (newline-separated), and optional code/formula.",
                history = emptyList(),
                userText = clean,
                toolExecutor = toolExecutor,
            )
            _agentOrbState.value = OrbState.IDLE
            if (_whiteboardCards.value.none { it.title.equals(clean, ignoreCase = true) }) {
                val lines = turn.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                _whiteboardCards.value = listOf(
                    WhiteboardCard(
                        title = clean.take(60),
                        topic = clean,
                        steps = lines.ifEmpty { listOf(turn.text) },
                    ),
                ) + _whiteboardCards.value
            }
            if (s.autoSpeakReplies) speakText(turn.text, s, null)
        }
    }

    fun clearWhiteboard() {
        _whiteboardCards.value = emptyList()
    }

    // ---- Provider Tests & Voice Previews ----

    fun testProviderConnection(provider: BrainProvider, apiKey: String, baseUrl: String, model: String) {
        val cleanKey = apiKey.trim().replace(Regex("\\s+"), "")
        viewModelScope.launch {
            _connectivityTesting.value = true
            _connectivityResult.value = null
            val res = aiClient.testConnection(provider, cleanKey, baseUrl, model)
            _connectivityTesting.value = false
            _connectivityResult.value = res
            if (res.ok) {
                if (res.models.isNotEmpty()) {
                    settingsRepo.setDiscoveredModels(res.models)
                }
                if (provider == BrainProvider.GEMINI && cleanKey.isNotEmpty()) {
                    settingsRepo.update { cur ->
                        val pickedFlash = listOf(
                            "gemini-2.5-flash-lite",
                            "gemini-3.5-flash-lite",
                            "gemini-3.1-flash-lite",
                            "gemini-2.5-flash",
                            "gemini-3.6-flash",
                            "gemini-3.8-flash",
                            "gemini-3-flash-preview",
                        )
                            .firstOrNull { cand -> res.models.any { it.equals(cand, ignoreCase = true) } }
                            ?: "gemini-2.5-flash-lite"
                        val pickedLive = listOf("gemini-3.8-live", "gemini-3.1-flash-live-preview", "gemini-2.5-flash-native-audio-preview-09-2025", "gemini-2.0-flash-live-001")
                            .firstOrNull { cand -> res.models.any { it.equals(cand, ignoreCase = true) } }
                            ?: cur.geminiLiveModel
                        cur.copy(
                            geminiApiKey = cleanKey,
                            activeProvider = BrainProvider.GEMINI,
                            offlineOnlyMode = false,
                            voiceEngineMode = if (cur.voiceEngineMode == VoiceEngineMode.TEXT_ONLY) VoiceEngineMode.TEXT_ONLY else VoiceEngineMode.GEMINI_LIVE,
                            geminiTextModel = pickedFlash,
                            geminiLiveModel = pickedLive,
                        )
                    }
                }
            }
        }
    }

    fun previewVoicePersonality(personality: VoicePersonality) {
        val s = settings.value
        voice.previewPersonality(
            personality = personality,
            language = s.language,
            rate = s.ttsSpeechRate,
            pitch = s.ttsPitch,
            volume = s.ttsVolume,
            preferredVoiceName = personality.geminiLiveVoice,
            apiKey = s.geminiApiKey,
            onError = { err ->
                _agentStatusText.value = err
            },
        )
    }

    // ---- Chats, Memories, Rules, Documents, Macros ----

    fun deleteMessage(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.deleteMessage(id)
            _messages.value = memoryRepo.getRecentMessages(200)
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.clearMessages()
            _messages.value = emptyList()
        }
    }

    fun saveMemory(title: String, content: String, tags: String) {
        if (title.isBlank() || content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.saveMemory(title, content, tags, approvedByUser = true, source = "aapne bataya")
            refreshData()
        }
    }

    fun updateMemory(entry: MemoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.updateMemory(entry)
            refreshData()
        }
    }

    fun archiveMemory(id: Long, archived: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.setMemoryArchived(id, archived)
            refreshData()
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.deleteMemory(id)
            refreshData()
        }
    }

    fun saveRule(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.saveRule(text, enabled = true)
            _rules.value = memoryRepo.getAllRules()
        }
    }

    fun toggleRule(rule: UserRule) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.updateRule(rule.copy(enabled = !rule.enabled))
            _rules.value = memoryRepo.getAllRules()
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.deleteRule(id)
            _rules.value = memoryRepo.getAllRules()
        }
    }

    fun saveDocument(title: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.saveDocument(title, content)
            _documents.value = memoryRepo.getAllDocuments()
        }
    }

    fun deleteDocument(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.deleteDocument(id)
            _documents.value = memoryRepo.getAllDocuments()
        }
    }

    fun saveMacro(macro: MacroDefinition) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.upsertMacro(macro)
            _macros.value = memoryRepo.getAllMacros()
        }
    }

    fun deleteMacro(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.deleteMacro(name)
            _macros.value = memoryRepo.getAllMacros()
        }
    }

    fun runMacro(macro: MacroDefinition, params: Map<String, String> = emptyMap()) {
        viewModelScope.launch(Dispatchers.IO) {
            _agentOrbState.value = OrbState.AUTOMATING
            _agentStatusText.value = "Running '${macro.name}'..."
            val res = macroEngine.execute(macro, params)
            val msg = res.getOrElse { it.message ?: "Workflow failed" }
            if (res.isSuccess) memoryRepo.markMacroPlayed(macro.name)
            memoryRepo.logTool("macro:${macro.name}", msg, res.isSuccess)
            _agentOrbState.value = if (res.isSuccess) OrbState.IDLE else OrbState.ERROR
            _agentStatusText.value = msg
            refreshData()
        }
    }

    fun clearAwarenessLogs() {
        WakeWordStatusBus.clearLogs()
    }

    fun inspectCurrentScreen() {
        val acc = AyshaAccessibilityService.instance
        if (acc == null) {
            _screenInspectResult.value = "Accessibility Service OFF hai. Permissions tab me ON karo."
            return
        }
        val elements = runCatching { acc.readScreenElements(35) }.getOrDefault(emptyList())
        _screenInspectResult.value = if (elements.isEmpty()) {
            "Current screen par readable element nahi mila."
        } else {
            elements.joinToString("\n") { el ->
                val txt = listOf(el.text, el.contentDescription).filter { it.isNotBlank() }.joinToString(" / ")
                "• ${txt.ifBlank { el.className }} (${el.bounds.centerX()},${el.bounds.centerY()})"
            }
        }
    }

    fun runPhoneQuickAction(actionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = when (actionId) {
                "camera" -> phoneControl.openCamera()
                "gallery" -> phoneControl.openGallery()
                "alarm" -> phoneControl.setAlarm(7, 0, "AYSHA Morning Alarm")
                "reminder" -> phoneControl.createReminder(10, "AYSHA Quick Reminder")
                "torch" -> phoneControl.toggleTorch()
                "battery" -> phoneControl.getBatteryInfo()
                "device" -> phoneControl.getDeviceInfo()
                "media" -> phoneControl.mediaControl("play_pause")
                "notifications" -> phoneControl.readNotifications(6)
                "youtube" -> phoneControl.playYouTube("")
                "chrome" -> phoneControl.openWebsite("https://www.google.com")
                else -> return@launch
            }
            _agentStatusText.value = res.message
            appendMessage(if (res.ok) ChatRole.ASSISTANT else ChatRole.SYSTEM, res.message, "Phone Control")
            refreshData()
        }
    }

    fun requestCallConfirmation(target: String) {
        val res = phoneControl.preparePhoneCall(target)
        if (res.confirmationRequired != null) {
            _pendingConfirmation.value = res.confirmationRequired
        } else {
            _agentStatusText.value = res.message
        }
    }

    fun requestSmsConfirmation(target: String, message: String) {
        val res = phoneControl.prepareSms(target, message)
        if (res.confirmationRequired != null) {
            _pendingConfirmation.value = res.confirmationRequired
        } else {
            _agentStatusText.value = res.message
        }
    }

    fun toggleFloatingOrb(enable: Boolean) {
        if (enable && !Settings.canDrawOverlays(app)) {
            updateSettings { it.copy(floatingOrbEnabled = false) }
            _pendingOverlayPermission.value = true
            _agentStatusText.value = "Floating Orb ke liye Display Over Other Apps permission ON karo"
            return
        }
        updateSettings { it.copy(floatingOrbEnabled = enable) }
        val intent = Intent(app, AyshaForegroundService::class.java).apply {
            action = if (enable) AyshaForegroundService.ACTION_SHOW_ORB else AyshaForegroundService.ACTION_HIDE_ORB
        }
        runCatching {
            if (enable) ContextCompat.startForegroundService(app, intent) else app.startService(intent)
        }
    }

    fun applyWakePhrasesAndRestart(rawCsv: String) {
        val cleanCsv = rawCsv.ifBlank { "Hey Aysha,Hello Aysha,Wake up Aysha" }
        val firstHint = cleanCsv.split(",", "\n", ";").map { it.trim() }.firstOrNull { it.isNotBlank() } ?: "Hey Aysha"
        viewModelScope.launch {
            val enableNow = hasMicPermission()
            settingsRepo.update { it.copy(wakeWordsCsv = cleanCsv, wakeWordEnabled = enableNow || it.wakeWordEnabled) }
            if (enableNow) {
                _agentStatusText.value = "Background wake listening: '$firstHint'"
                val startOrUpdate = Intent(app, AyshaForegroundService::class.java).apply {
                    action = if (WakeWordStatusBus.isRunning.value) {
                        AyshaForegroundService.ACTION_UPDATE_WAKE_CONFIG
                    } else {
                        AyshaForegroundService.ACTION_START_WAKEWORD
                    }
                }
                runCatching { ContextCompat.startForegroundService(app, startOrUpdate) }
            } else {
                _pendingMicPermission.value = true
                _agentStatusText.value = "Wake Word ke liye Microphone permission chahiye"
            }
        }
    }

    fun toggleWakeWord(enable: Boolean) {
        if (enable && !hasMicPermission()) {
            updateSettings { it.copy(wakeWordEnabled = false) }
            _pendingMicPermission.value = true
            _agentStatusText.value = "Wake Word ke liye Microphone permission chahiye"
            return
        }
        updateSettings { it.copy(wakeWordEnabled = enable) }
        _agentStatusText.value = if (enable) "Wake Word chalu — 'Hey Aysha' boliye" else "Wake Word band kiya gaya"
        val intent = Intent(app, AyshaForegroundService::class.java).apply {
            action = if (enable) AyshaForegroundService.ACTION_START_WAKEWORD else AyshaForegroundService.ACTION_STOP_WAKEWORD
        }
        runCatching {
            if (enable) ContextCompat.startForegroundService(app, intent) else app.startService(intent)
        }
    }

    fun openSystemSettings(category: String) {
        phoneControl.openSettings(category)
    }

    fun exportAndShareChats(asJson: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val msgs = memoryRepo.getRecentMessages(500)
            val mems = memoryRepo.getAllMemories(includeArchived = true)
            val content = if (asJson) ChatExporter.toJson(msgs, mems) else ChatExporter.toMarkdown(msgs, settings.value.assistantName)
            val ext = if (asJson) "json" else "md"
            val file = File(app.cacheDir, "aysha_export_${System.currentTimeMillis()}.$ext")
            file.writeText(content)
            val uri: Uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (asJson) "application/json" else "text/markdown"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching {
                app.startActivity(Intent.createChooser(shareIntent, "Export AYSHA Chats").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun exportFullBackupAndShare() {
        viewModelScope.launch(Dispatchers.IO) {
            val backupJson = BackupRestoreHelper.buildBackupJson(
                settingsJson = settingsRepo.exportSafeConfigJson(),
                memories = memoryRepo.getAllMemories(includeArchived = true),
                rules = memoryRepo.getAllRules(),
                documents = memoryRepo.getAllDocuments(),
                macros = memoryRepo.getAllMacros(),
            )
            val file = File(app.cacheDir, "aysha_backup_v2_${System.currentTimeMillis()}.json")
            file.writeText(backupJson)
            val uri: Uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            _backupFeedback.value = "Full safe backup ready (API keys excluded)."
            runCatching {
                app.startActivity(Intent.createChooser(shareIntent, "Save AYSHA Full Backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }

    fun restoreFullBackup(rawJson: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parsed = BackupRestoreHelper.parseBackupJson(rawJson)
            if (parsed.isFailure) {
                _backupFeedback.value = "Invalid backup JSON: ${parsed.exceptionOrNull()?.message}"
                return@launch
            }
            val payload = parsed.getOrThrow()
            payload.settingsJson?.let { settingsRepo.restoreSafeSettings(it) }
            payload.memories.forEach { m ->
                memoryRepo.saveMemory(m.title, m.content, m.tags, m.approvedByUser, m.source)
            }
            payload.rules.forEach { r -> memoryRepo.saveRule(r.text, r.enabled) }
            payload.documents.forEach { d -> memoryRepo.saveDocument(d.title, d.content) }
            payload.customMacros.forEach { macro -> memoryRepo.upsertMacro(macro) }
            refreshData()
            _backupFeedback.value = "Restored ${payload.memories.size} memories, ${payload.rules.size} rules, ${payload.documents.size} docs!"
        }
    }

    fun shareSafeConfigBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val safeJson = settingsRepo.exportSafeConfigJson()
            phoneControl.shareText(safeJson)
        }
    }

    fun clearAllApiKeys() {
        viewModelScope.launch {
            settingsRepo.clearAllApiKeys()
            _connectivityResult.value = null
        }
    }

    fun wipeAllLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.clearAllLocalData()
            refreshData()
        }
    }

    fun formatClock(seconds: Long): String = OfflineCommandParser.formatClock(seconds)

    fun setCompanionIntimacy(level: CompanionIntimacyLevel) {
        if (level == CompanionIntimacyLevel.INTENSE_18_PLUS) {
            val s = settings.value
            val licUnlocked = licenseInfo.value.isUnlocked()
            if (!licUnlocked || !s.adultAgeVerified || !s.adultTermsAccepted || !s.adultRatedConfirmed) {
                return
            }
        }
        viewModelScope.launch {
            settingsRepo.update { it.copy(companionIntimacy = level) }
        }
    }

    fun recordAdultTermsAccepted() {
        viewModelScope.launch {
            settingsRepo.update { it.copy(adultTermsAccepted = true) }
        }
    }

    fun verifyAdultBirthYear(rawYear: String): BirthYearValidation {
        val result = AdultModeGuard.validateBirthYear(rawYear)
        if (result is BirthYearValidation.Valid) {
            viewModelScope.launch {
                settingsRepo.update {
                    it.copy(
                        adultBirthYear = result.birthYear,
                        adultAgeVerified = true,
                        adultVerifiedAtMs = System.currentTimeMillis(),
                    )
                }
            }
        }
        return result
    }

    fun activateIntense18PlusMode() {
        val s = settings.value
        if (!licenseInfo.value.isUnlocked() || !s.adultAgeVerified || s.userGender == UserGender.FEMALE) {
            return
        }
        viewModelScope.launch {
            settingsRepo.update {
                it.copy(
                    adultTermsAccepted = true,
                    adultRatedConfirmed = true,
                    companionIntimacy = CompanionIntimacyLevel.INTENSE_18_PLUS,
                    adultVerifiedAtMs = System.currentTimeMillis(),
                )
            }
        }
    }

    fun withdrawAdultConfirmation() {
        viewModelScope.launch {
            settingsRepo.withdrawAdultConfirmation()
            _agentStatusText.value = "Age confirmation removed · Friendly mode active"
        }
    }

    override fun onCleared() {
        continuousVoiceActive = false
        followUpJob?.cancel()
        liveClient.destroy()
        super.onCleared()
    }
}
