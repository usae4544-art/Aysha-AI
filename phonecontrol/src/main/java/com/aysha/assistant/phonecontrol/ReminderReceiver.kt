package com.aysha.assistant.phonecontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ReminderEventBus {
    private val _firedReminders = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val firedReminders: SharedFlow<String> = _firedReminders.asSharedFlow()

    fun emit(message: String) {
        _firedReminders.tryEmit(message)
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val message = intent?.getStringExtra(EXTRA_MESSAGE)?.ifBlank { "AYSHA Reminder" } ?: "AYSHA Reminder"
        ReminderEventBus.emit(message)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AYSHA Reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Scheduled reminders & spoken alerts from AYSHA"
        }
        nm.createNotificationChannel(channel)

        val openIntent = Intent("com.aysha.assistant.ACTION_OPEN_ASSISTANT").apply {
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_REMINDER_SPEAK, message)
        }
        val pi = PendingIntent.getActivity(
            context,
            (System.currentTimeMillis() % 100_000).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("AYSHA Reminder 🔔")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()

        nm.notify((System.currentTimeMillis() % 100_000).toInt(), notif)
    }

    companion object {
        const val CHANNEL_ID = "aysha_reminders"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_REMINDER_SPEAK = "extra_reminder_speak"
    }
}
