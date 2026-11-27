package com.aysha.assistant.floatingassistant

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aysha.assistant.core.SafeLogger
import com.aysha.assistant.settings.SettingsRepository
import com.aysha.assistant.wakeword.WakeWordEngine
import com.aysha.assistant.wakeword.WakeWordStatusBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class AyshaForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var settingsRepo: SettingsRepository
    private lateinit var wakeEngine: WakeWordEngine

    private var windowManager: WindowManager? = null
    private var orbView: View? = null
    private var menuView: View? = null
    private var wakeCollectorJob: Job? = null
    private var wakeResumeWatchdogJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var orbParams: WindowManager.LayoutParams? = null

    @Volatile
    private var orbVisible = false

    @Volatile
    private var wakeActive = false

    @Volatile
    private var activeWakeHint = "Hey Aysha"

    override fun onCreate() {
        super.onCreate()
        settingsRepo = SettingsRepository(this)
        wakeEngine = WakeWordEngine(this)
        createNotificationChannel()
        observeWakeEvents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_ALL -> {
                wakeActive = false
                orbVisible = false
                wakeResumeWatchdogJob?.cancel()
                wakeEngine.stop()
                releaseWakeLock()
                removeMenu()
                removeOrb()
                scope.launch {
                    settingsRepo.update { it.copy(wakeWordEnabled = false, floatingOrbEnabled = false) }
                }
                runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW_ORB -> {
                orbVisible = true
                promoteForeground()
                showOrbIfPermitted()
            }
            ACTION_HIDE_ORB -> {
                orbVisible = false
                removeMenu()
                removeOrb()
                if (!wakeActive) {
                    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                    stopSelf()
                    return START_NOT_STICKY
                } else {
                    promoteForeground()
                }
            }
            ACTION_START_WAKEWORD -> {
                wakeActive = true
                acquireWakeLock()
                scope.launch {
                    val s = settingsRepo.settingsFlow.first()
                    activeWakeHint = firstPhraseFromCsv(s.wakeWordsCsv, s.assistantName)
                    promoteForeground()
                    wakeEngine.start(
                        wakeWordsCsv = s.wakeWordsCsv,
                        assistantName = s.assistantName,
                        awareness = s.awarenessEnabled,
                        sens = s.awarenessSensitivity,
                        muteMedia = s.muteDuringMedia,
                    )
                }
            }
            ACTION_UPDATE_WAKE_CONFIG -> {
                scope.launch {
                    val s = settingsRepo.settingsFlow.first()
                    activeWakeHint = firstPhraseFromCsv(s.wakeWordsCsv, s.assistantName)
                    wakeEngine.updateConfig(
                        wakeWordsCsv = s.wakeWordsCsv,
                        name = s.assistantName,
                        awareness = s.awarenessEnabled,
                        sens = s.awarenessSensitivity,
                        muteMedia = s.muteDuringMedia,
                    )
                    if (wakeActive) promoteForeground()
                }
            }
            ACTION_STOP_WAKEWORD -> {
                wakeActive = false
                wakeResumeWatchdogJob?.cancel()
                wakeEngine.stop()
                releaseWakeLock()
                if (!orbVisible) {
                    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                    stopSelf()
                    return START_NOT_STICKY
                } else {
                    promoteForeground()
                }
            }
            ACTION_PAUSE_WAKEWORD -> {
                wakeEngine.pauseForActiveSession()
            }
            ACTION_RESUME_WAKEWORD -> {
                wakeResumeWatchdogJob?.cancel()
                if (wakeActive) wakeEngine.resumeAfterActiveSession()
            }
            else -> {
                // Service restarted by Android (START_STICKY): restore persisted wake & orb state
                scope.launch {
                    val s = settingsRepo.settingsFlow.first()
                    activeWakeHint = firstPhraseFromCsv(s.wakeWordsCsv, s.assistantName)
                    orbVisible = s.floatingOrbEnabled && Settings.canDrawOverlays(this@AyshaForegroundService)
                    wakeActive = s.wakeWordEnabled &&
                        ContextCompat.checkSelfPermission(this@AyshaForegroundService, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    promoteForeground()
                    if (orbVisible) showOrbIfPermitted()
                    if (wakeActive) {
                        acquireWakeLock()
                        wakeEngine.start(
                            wakeWordsCsv = s.wakeWordsCsv,
                            assistantName = s.assistantName,
                            awareness = s.awarenessEnabled,
                            sens = s.awarenessSensitivity,
                            muteMedia = s.muteDuringMedia,
                        )
                    } else if (!orbVisible) {
                        releaseWakeLock()
                        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                        stopSelf()
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun observeWakeEvents() {
        wakeCollectorJob?.cancel()
        wakeCollectorJob = scope.launch {
            wakeEngine.wakeEvents.collect { matchedPhrase ->
                SafeLogger.d(TAG, "Wake trigger received in service: $matchedPhrase")
                wakeEngine.pauseForActiveSession()
                delay(140L) // allow OS AudioRecord release before handoff to STT/Gemini Live
                WakeWordStatusBus.emitWakeTrigger(matchedPhrase)
                postWakeActivationAlert(matchedPhrase)
                launchAssistant(startVoice = true, quickTab = null, wakePhrase = matchedPhrase)
                // Failsafe: if Android blocked background activity launch and no session claimed mic, auto-resume!
                wakeResumeWatchdogJob?.cancel()
                wakeResumeWatchdogJob = scope.launch {
                    delay(4500L)
                    if (wakeActive && !wakeEngine.micEnergy.value.isNaN()) {
                        wakeEngine.resumeAfterActiveSession()
                    }
                }
            }
        }
    }

    private fun firstPhraseFromCsv(csv: String, assistantName: String): String =
        csv.split(",", "\n", ";")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: "Hey ${assistantName.ifBlank { "Aysha" }}"

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) return
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "aysha:wake_word_lock").apply {
                setReferenceCounted(false)
                acquire(6 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    private fun promoteForeground() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) {
            val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            val type = if (wakeActive && hasMic) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            val started = runCatching { startForeground(NOTIFICATION_ID, notif, type) }
            if (started.isFailure) {
                runCatching { startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) }
            }
        } else {
            runCatching { startForeground(NOTIFICATION_ID, notif) }
        }
    }

    private fun createNotificationChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AYSHA Assistant Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Keeps AYSHA floating orb and optional on-device wake word ready"
        }
        val wakeAlertChannel = NotificationChannel(
            WAKE_ALERT_CHANNEL_ID,
            "AYSHA Background Wake Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Opens AYSHA hands-free voice when wake phrase triggers in background"
        }
        nm.createNotificationChannel(channel)
        nm.createNotificationChannel(wakeAlertChannel)
    }

    private fun postWakeActivationAlert(matchedPhrase: String) {
        runCatching {
            val wakeIntent = Intent(ACTION_OPEN_ASSISTANT).apply {
                setPackage(packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_START_VOICE, true)
                putExtra(EXTRA_WAKE_PHRASE, matchedPhrase)
            }
            val wakePi = PendingIntent.getActivity(
                this,
                105,
                wakeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notif = NotificationCompat.Builder(this, WAKE_ALERT_CHANNEL_ID)
                .setSmallIcon(applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_btn_speak_now)
                .setContentTitle("AYSHA Activated · '$matchedPhrase'")
                .setContentText("Tap to speak with AYSHA")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setTimeoutAfter(5000L)
                .setFullScreenIntent(wakePi, true)
                .setContentIntent(wakePi)
                .build()
            nm.notify(WAKE_ALERT_NOTIFICATION_ID, notif)
        }
    }

    private fun buildNotification(): Notification {
        val openVoiceIntent = Intent(ACTION_OPEN_ASSISTANT).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_START_VOICE, true)
        }
        val talkPi = PendingIntent.getActivity(
            this,
            101,
            openVoiceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopPi = PendingIntent.getService(
            this,
            102,
            Intent(this, AyshaForegroundService::class.java).apply { action = ACTION_STOP_ALL },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = when {
            wakeActive && orbVisible -> "AYSHA Active · '$activeWakeHint' + Orb"
            wakeActive -> "AYSHA Listening: '$activeWakeHint'"
            orbVisible -> "AYSHA Floating Orb Ready"
            else -> "AYSHA Assistant Ready"
        }
        val subtitle = if (wakeActive) "Background wake + noise filter active · Tap to talk" else "Tap to open AYSHA"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon.takeIf { it != 0 } ?: android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setOngoing(true)
            .setContentIntent(talkPi)
            .addAction(0, "Talk", talkPi)
            .addAction(0, "Stop", stopPi)
            .build()
    }

    private fun showOrbIfPermitted() {
        if (!Settings.canDrawOverlays(this) || orbView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm

        val density = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels
        val sizePx = (62 * density).toInt()

        val orbContainer = FrameLayout(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#B822D3EE"), Color.parseColor("#B88B5CF6"), Color.parseColor("#B8EC4899")),
            ).apply {
                shape = GradientDrawable.OVAL
                setStroke((2.2f * density).toInt(), Color.parseColor("#D8FFFFFF"))
            }
            elevation = 20f * density
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
        }

        val iconView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            val pad = (3 * density).toInt()
            setPadding(pad, pad, pad, pad)
            runCatching {
                val portraitId = resources.getIdentifier("aysha_portrait", "drawable", packageName)
                if (portraitId != 0) {
                    setImageResource(portraitId)
                } else {
                    setImageDrawable(packageManager.getApplicationIcon(packageName))
                }
            }
        }
        orbContainer.addView(iconView, FrameLayout.LayoutParams(sizePx, sizePx))

        val params = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenW - sizePx - (18 * density).toInt()).coerceAtLeast(0)
            y = (220 * density).toInt()
        }
        orbParams = params

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        var longPressed = false

        val longPressRunnable = Runnable {
            if (!moved) {
                longPressed = true
                removeMenu()
                launchAssistant(startVoice = true, quickTab = null)
            }
        }

        orbContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    longPressed = false
                    mainHandler.postDelayed(longPressRunnable, 480L)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        moved = true
                        mainHandler.removeCallbacks(longPressRunnable)
                        removeMenu()
                    }
                    params.x = (startX + dx).coerceIn(0, (screenW - sizePx).coerceAtLeast(0))
                    params.y = (startY + dy).coerceIn(0, (screenH - sizePx).coerceAtLeast(0))
                    runCatching { wm.updateViewLayout(orbContainer, params) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mainHandler.removeCallbacks(longPressRunnable)
                    if (!moved && !longPressed && event.action == MotionEvent.ACTION_UP) {
                        toggleQuickMenu()
                    }
                    true
                }
                else -> false
            }
        }

        runCatching {
            wm.addView(orbContainer, params)
            orbView = orbContainer
        }
    }

    private fun toggleQuickMenu() {
        if (menuView != null) {
            removeMenu()
            return
        }
        val wm = windowManager ?: return
        val op = orbParams ?: return
        val density = resources.displayMetrics.density
        val screenW = resources.displayMetrics.widthPixels
        val menuW = (210 * density).toInt()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (14 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22 * density
                setColor(Color.parseColor("#CC0D1834"))
                setStroke((1.5f * density).toInt(), Color.parseColor("#B0FFFFFF"))
            }
            elevation = 22f * density
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titleTv = TextView(this).apply {
            text = "AYSHA"
            setTextColor(Color.WHITE)
            textSize = 15f
            setTypeface(Typeface.DEFAULT_BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeTv = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 16f
            setPadding((8 * density).toInt(), 0, (4 * density).toInt(), 0)
            setOnClickListener { removeMenu() }
        }
        headerRow.addView(titleTv)
        headerRow.addView(closeTv)
        card.addView(headerRow)

        fun addMenuItem(label: String, accentHex: String, onClick: () -> Unit) {
            val btn = TextView(this).apply {
                text = label
                setTextColor(Color.WHITE)
                textSize = 13.5f
                val vPad = (10 * density).toInt()
                val hPad = (12 * density).toInt()
                setPadding(hPad, vPad, hPad, vPad)
                background = GradientDrawable().apply {
                    cornerRadius = 12 * density
                    setColor(Color.parseColor("#24FFFFFF"))
                    setStroke((1 * density).toInt(), Color.parseColor(accentHex))
                }
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (8 * density).toInt()
                }
                layoutParams = lp
                setOnClickListener {
                    removeMenu()
                    onClick()
                }
            }
            card.addView(btn)
        }

        addMenuItem("✦ Open Assistant", "#5522D3EE") {
            launchAssistant(startVoice = false, quickTab = null)
        }
        addMenuItem("🎙 Voice Command", "#558B5CF6") {
            launchAssistant(startVoice = true, quickTab = null)
        }
        addMenuItem("📷 Camera Scanner", "#550EA5E9") {
            launchAssistant(startVoice = false, quickTab = "scanner")
        }
        addMenuItem("✏ Study Whiteboard", "#5510B981") {
            launchAssistant(startVoice = false, quickTab = "whiteboard")
        }
        addMenuItem("⚡ Phone Control", "#5514B8A6") {
            launchAssistant(startVoice = false, quickTab = "phone_control")
        }
        addMenuItem("✕ Hide Floating", "#44F43F5E") {
            scope.launch {
                settingsRepo.update { it.copy(floatingOrbEnabled = false) }
                orbVisible = false
                removeOrb()
                if (!wakeActive) {
                    runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
                    stopSelf()
                } else {
                    promoteForeground()
                }
            }
        }

        val menuParams = WindowManager.LayoutParams(
            menuW,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (op.x + menuW < screenW - 20) op.x else (op.x - menuW + (62 * density).toInt()).coerceAtLeast(16)
            y = op.y + (70 * density).toInt()
        }

        runCatching {
            wm.addView(card, menuParams)
            menuView = card
        }
    }

    private fun launchAssistant(startVoice: Boolean, quickTab: String?, wakePhrase: String? = null) {
        val intent = Intent(ACTION_OPEN_ASSISTANT).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_START_VOICE, startVoice)
            if (quickTab != null) putExtra(EXTRA_TARGET_TAB, quickTab)
            if (wakePhrase != null) putExtra(EXTRA_WAKE_PHRASE, wakePhrase)
        }
        runCatching { startActivity(intent) }
    }

    private fun removeOrb() {
        orbView?.let { v -> runCatching { windowManager?.removeView(v) } }
        orbView = null
    }

    private fun removeMenu() {
        menuView?.let { v -> runCatching { windowManager?.removeView(v) } }
        menuView = null
    }

    override fun onDestroy() {
        wakeResumeWatchdogJob?.cancel()
        wakeCollectorJob?.cancel()
        wakeEngine.destroy()
        releaseWakeLock()
        removeMenu()
        removeOrb()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "AyshaFgService"
        const val CHANNEL_ID = "aysha_service_channel"
        const val WAKE_ALERT_CHANNEL_ID = "aysha_wake_alert_channel"
        const val NOTIFICATION_ID = 909
        const val WAKE_ALERT_NOTIFICATION_ID = 910

        const val ACTION_OPEN_ASSISTANT = "com.aysha.assistant.ACTION_OPEN_ASSISTANT"
        const val EXTRA_START_VOICE = "extra_start_voice"
        const val EXTRA_TARGET_TAB = "extra_target_tab"
        const val EXTRA_WAKE_PHRASE = "extra_wake_phrase"

        const val ACTION_SHOW_ORB = "com.aysha.assistant.SHOW_ORB"
        const val ACTION_HIDE_ORB = "com.aysha.assistant.HIDE_ORB"
        const val ACTION_START_WAKEWORD = "com.aysha.assistant.START_WAKEWORD"
        const val ACTION_UPDATE_WAKE_CONFIG = "com.aysha.assistant.UPDATE_WAKE_CONFIG"
        const val ACTION_STOP_WAKEWORD = "com.aysha.assistant.STOP_WAKEWORD"
        const val ACTION_PAUSE_WAKEWORD = "com.aysha.assistant.PAUSE_WAKEWORD"
        const val ACTION_RESUME_WAKEWORD = "com.aysha.assistant.RESUME_WAKEWORD"
        const val ACTION_STOP_ALL = "com.aysha.assistant.STOP_ALL"
    }
}
