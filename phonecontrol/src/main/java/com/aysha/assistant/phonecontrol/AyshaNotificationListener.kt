package com.aysha.assistant.phonecontrol

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.aysha.assistant.core.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

class AyshaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
    }

    override fun onListenerDisconnected() {
        _connected.value = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (sbn.packageName == packageName) return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val text = (
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
            )?.toString().orEmpty().trim()
        if (title.isBlank() && text.isBlank()) return

        val appName = runCatching {
            val ai = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(ai).toString()
        }.getOrDefault(sbn.packageName)

        recent.add(
            0,
            CapturedNotification(
                packageName = sbn.packageName,
                appName = appName,
                title = title,
                text = text,
                postedAtMs = sbn.postTime,
            ),
        )
        while (recent.size > MAX_ITEMS) {
            runCatching { recent.removeAt(recent.lastIndex) }
        }
    }

    companion object {
        private const val MAX_ITEMS = 40
        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        private val recent = CopyOnWriteArrayList<CapturedNotification>()

        fun getRecent(limit: Int = 10): List<CapturedNotification> =
            recent.take(limit.coerceIn(1, MAX_ITEMS))

        fun clearCaptured() {
            recent.clear()
        }
    }
}
