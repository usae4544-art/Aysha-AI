package com.aysha.assistant.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.aysha.assistant.floatingassistant.AyshaForegroundService
import com.aysha.assistant.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val repo = SettingsRepository(context)
                val settings = repo.settingsFlow.first()
                if (settings.floatingOrbEnabled && Settings.canDrawOverlays(context)) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AyshaForegroundService::class.java).apply {
                            this.action = AyshaForegroundService.ACTION_SHOW_ORB
                        },
                    )
                }
                if (settings.wakeWordEnabled) {
                    ContextCompat.startForegroundService(
                        context,
                        Intent(context, AyshaForegroundService::class.java).apply {
                            this.action = AyshaForegroundService.ACTION_START_WAKEWORD
                        },
                    )
                }
            }
            pendingResult.finish()
        }
    }
}
