package com.aysha.assistant.ui

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aysha.assistant.AyshaApplication
import com.aysha.assistant.ai.ConnectivityCheckResult
import com.aysha.assistant.ai.GeminiLiveClient
import com.aysha.assistant.ai.LiveCallbacks
import com.aysha.assistant.ai.MultiProviderAiClient
import com.aysha.assistant.ai.OfflineCommandParser
import com.aysha.assistant.ai.PromptBuilder
import com.aysha.assistant.ai.ToolExecutor
import com.aysha.assistant.automation.AyshaAccessibilityService
import com.aysha.assistant.automation.MacroEngine
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.BrainProvider
import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.MacroDefinition
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.OrbState
import com.aysha.assistant.core.PendingActionConfirmation
import com.aysha.assistant.core.PendingConfirmationType
import com.aysha.assistant.core.ToolLogEntry
import com.aysha.assistant.core.VoiceEngineMode
import com.aysha.assistant.core.VoicePersonality
import com.aysha.assistant.floatingassistant.AyshaForegroundService
import com.aysha.assistant.memory.ChatExporter
import com.aysha.assistant.phonecontrol.AyshaNotificationListener
import com.aysha.assistant.settings.PermissionAudit
import com.aysha.assistant.settings.PermissionAuditItem
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

    private val toolExecutor = ToolExecutor(
        phoneControl = phoneControl,
        memoryRepo = memoryRepo,
        macroEngine = macroEngine,
        onStartTimer = { secs, label -> startInAppTimer(secs, label) },
    )

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val discoveredModels: StateFlow<List<String>> = settingsRepo.discoveredModels
    val availableTtsVoices: StateFlow<List<TtsVoiceInfo>> = voice.availableVoices
    val accessibilityConnected: StateFlow<Boolean> = AyshaAccessibilityService.connected
    val notificationListenerConnected: StateFlow<Boolean> = AyshaNotificationListener.connected

    val wakeRunning: StateFlow<Boolean> = WakeWordStatusBus.isRunning
    val wakeModelState: StateFlow<WakeModelState> = WakeWordStatusBus.modelState
    val wakeStatusText: StateFlow<String> = WakeWordStatusBus.statusText
    val wakeMicEnergy: StateFlow<Float> = WakeWordStatusBus.micEnergy

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _memories = MutableStateFlow<List<MemoryEntry>>(emptyList())
    val memories: StateFlow<List<MemoryEntry>> = _memories.asStateFlow()

    private val _macros = MutableStateFlow<List<MacroDefinition>>(emptyList())
    val macros: StateFlow<List<MacroDefinition>> = _macros.asStateFlow()

    private val _toolLogs = MutableStateFlow<List<ToolLogEntry>>(emptyList())
    val toolLogs: StateFlow<List<ToolLogEntry>> = _toolLogs.asStateFlow()

    private val _permissionAudit = MutableStateFlow<List<PermissionAuditItem>>(emptyList())
    val permissionAudit: StateFlow<List<PermissionAuditItem>> = _permissionAudit.asStateFlow()

    private val _agentOrbState = MutableStateFlow(OrbState.IDLE)
    private val _agentStatusText = MutableStateFlow("Tap Orb to talk — 100% Free")
    private val _lastAssistantReply = MutableStateFlow("Hello! I am Aysha. How can I help you today?")
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

    private val _timerRemainingSeconds = MutableStateFlow(0L)
    val timerRemainingSeconds: StateFlow<Long> = _timerRemainingSeconds.asStateFlow()

    private val _timerRunning = MutableStateFlow(false)
    val timerRunning: StateFlow<Boolean> = _timerRunning.asStateFlow()

    private val _timerLabel = MutableStateFlow("AYSHA Timer")
    val timerLabel: StateFlow<String> = _timerLabel.asStateFlow()

    private var timerJob: Job? = null
    private var sessionTickerJob: Job? = null

    @Volatile
    private var continuousVoiceActive = false

    private val liveClient = GeminiLiveClient(
        toolExecutor = toolExecutor,
        callbacks = object : LiveCallbacks {
            override fun onUserTranscript(text: String) {
                appendMessage(ChatRole.USER, text, "Gemini Live")
            }

            override fun onModelTranscript(text: String) {
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
                appendMessage(ChatRole.SYSTEM, message, "Live")
                _agentStatusText.value = message
                resumeWakeWordIfEnabled()
            }
        },
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
    }

    private fun startSessionTicker() {
        sessionTickerJob?.cancel()
        sessionTickerJob = viewModelScope.launch {
            while (isActive) {
                val active = orbState.value != OrbState.IDLE && orbState.value != OrbState.ERROR
                if (active || continuousVoiceActive) {
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
            _memories.value = memoryRepo.getAllMemories()
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

    fun handleLaunchExtras(startVoice: Boolean, targetTab: String?, wakePhrase: String?) {
        if (!targetTab.isNullOrBlank()) {
            _requestedNavTab.value = targetTab
        }
        if (!wakePhrase.isNullOrBlank()) {
            _agentStatusText.value = "Wake word: $wakePhrase"
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
        val current = settings.value
        if (liveClient.isActive()) {
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

        pauseWakeWordForActiveMic()
        if (current.voiceEngineMode == VoiceEngineMode.GEMINI_LIVE && current.geminiApiKey.isNotBlank() && !current.offlineOnlyMode) {
            startGeminiLiveSession()
        } else {
            continuousVoiceActive = current.continuousConversation
            startStandardVoiceListen()
        }
    }

    fun startVoiceFromShortcut() {
        if (!liveClient.isActive() && !voice.isListening.value) {
            onOrbTap()
        }
    }

    fun stopActiveVoiceOrSpeech() {
        continuousVoiceActive = false
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
            val s = settings.value
            val mems = withContext(Dispatchers.IO) { memoryRepo.getAllMemories().take(12) }
            val sys = PromptBuilder.buildSystemInstruction(
                settings = s,
                memories = mems,
                accessibilityEnabled = accessibilityConnected.value,
                notificationsEnabled = notificationListenerConnected.value,
            )
            _liveMuted.value = false
            liveClient.start(s, sys)
        }
    }

    private fun startStandardVoiceListen() {
        if (!hasMicPermission()) {
            continuousVoiceActive = false
            _pendingMicPermission.value = true
            _agentOrbState.value = OrbState.IDLE
            return
        }
        val s = settings.value
        _agentStatusText.value = if (continuousVoiceActive) "Continuous listening active..." else "Listening..."
        voice.startListening(
            localeTag = s.language.sttLocaleTag,
            onPartialResult = { partial ->
                _agentStatusText.value = partial
            },
            onFinalResult = { spoken ->
                sendTextMessage(spoken, speakReply = true)
            },
            onSoftTimeout = if (continuousVoiceActive) {
                {
                    if (continuousVoiceActive && settings.value.continuousConversation) {
                        startStandardVoiceListen()
                    } else {
                        continuousVoiceActive = false
                        _agentOrbState.value = OrbState.IDLE
                        resumeWakeWordIfEnabled()
                    }
                }
            } else {
                null
            },
            onError = { err ->
                continuousVoiceActive = false
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

    fun sendTextMessage(text: String, speakReply: Boolean? = null) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        if (liveClient.isActive()) {
            liveClient.sendText(clean)
            appendMessage(ChatRole.USER, clean, "Gemini Live")
            return
        }

        appendMessage(ChatRole.USER, clean, "")

        viewModelScope.launch {
            val s = settings.value
            _agentOrbState.value = OrbState.PROCESSING
            _agentStatusText.value = "${s.assistantName} is thinking..."

            val (mems, history) = withContext(Dispatchers.IO) {
                val ranked = (memoryRepo.searchMemories(clean, 6) + memoryRepo.getAllMemories().take(4)).distinctBy { it.id }
                val hist = memoryRepo.getRecentMessages(20)
                ranked to hist
            }
            val sys = PromptBuilder.buildSystemInstruction(
                settings = s,
                memories = mems,
                accessibilityEnabled = accessibilityConnected.value,
                notificationsEnabled = notificationListenerConnected.value,
            )

            val turn = aiClient.runTurnWithFallback(
                settings = s,
                systemInstruction = sys,
                history = history,
                userText = clean,
                toolExecutor = toolExecutor,
                onToolEvent = { status ->
                    _agentOrbState.value = OrbState.AUTOMATING
                    _agentStatusText.value = status
                },
            )

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
                    if (continuousVoiceActive && settings.value.continuousConversation) {
                        startStandardVoiceListen()
                    } else {
                        resumeWakeWordIfEnabled()
                    }
                }
            } else if (continuousVoiceActive && s.continuousConversation) {
                startStandardVoiceListen()
            } else {
                resumeWakeWordIfEnabled()
            }
        }
    }

    private fun speakText(text: String, s: AppSettings, onDone: (() -> Unit)?) {
        voice.speak(
            text = text,
            localeTag = s.language.ttsLocaleTag,
            rate = s.ttsSpeechRate,
            pitch = s.ttsPitch,
            volume = s.ttsVolume,
            personalityId = s.voicePersonalityId,
            preferredVoiceName = s.selectedTtsVoiceName,
            onDone = onDone,
        )
    }

    fun confirmPendingAction() {
        val pending = _pendingConfirmation.value ?: return
        _pendingConfirmation.value = null
        val outcome = when (pending.type) {
            PendingConfirmationType.PHONE_CALL -> phoneControl.executeConfirmedCall(pending.target)
            PendingConfirmationType.SEND_SMS -> phoneControl.executeConfirmedSms(pending.target, pending.messageBody)
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

    fun testProviderConnection(provider: BrainProvider, apiKey: String, baseUrl: String, model: String) {
        viewModelScope.launch {
            _connectivityTesting.value = true
            _connectivityResult.value = null
            val res = aiClient.testConnection(provider, apiKey, baseUrl, model)
            _connectivityTesting.value = false
            _connectivityResult.value = res
            if (res.ok && res.models.isNotEmpty()) {
                settingsRepo.setDiscoveredModels(res.models)
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
            preferredVoiceName = s.selectedTtsVoiceName,
        )
    }

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
            memoryRepo.saveMemory(title, content, tags, approvedByUser = true)
            _memories.value = memoryRepo.getAllMemories()
        }
    }

    fun updateMemory(entry: MemoryEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.updateMemory(entry)
            _memories.value = memoryRepo.getAllMemories()
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.deleteMemory(id)
            _memories.value = memoryRepo.getAllMemories()
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
            memoryRepo.logTool("macro:${macro.name}", msg, res.isSuccess)
            _agentOrbState.value = if (res.isSuccess) OrbState.IDLE else OrbState.ERROR
            _agentStatusText.value = msg
            refreshData()
        }
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

    fun toggleWakeWord(enable: Boolean) {
        if (enable && !hasMicPermission()) {
            updateSettings { it.copy(wakeWordEnabled = false) }
            _pendingMicPermission.value = true
            _agentStatusText.value = "Wake Word ke liye Microphone permission chahiye"
            return
        }
        updateSettings { it.copy(wakeWordEnabled = enable) }
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
            val mems = memoryRepo.getAllMemories()
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

    override fun onCleared() {
        continuousVoiceActive = false
        liveClient.destroy()
        super.onCleared()
    }
}
