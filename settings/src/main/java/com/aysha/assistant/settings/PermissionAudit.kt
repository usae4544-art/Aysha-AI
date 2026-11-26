package com.aysha.assistant.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionAuditItem(
    val id: String,
    val title: String,
    val purpose: String,
    val isGranted: Boolean,
    val isSpecialSettings: Boolean = false,
)

object PermissionAudit {

    fun inspectAll(context: Context): List<PermissionAuditItem> {
        val pm = context.packageManager
        val pkg = context.packageName

        fun hasRuntime(perm: String): Boolean =
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

        val accessibilityEnabled = isAccessibilityEnabled(context, pkg)
        val notificationEnabled = isNotificationListenerEnabled(context, pkg)
        val overlayGranted = Settings.canDrawOverlays(context)
        val batteryExempt = runCatching {
            val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            power.isIgnoringBatteryOptimizations(pkg)
        }.getOrDefault(false)

        return buildList {
            add(
                PermissionAuditItem(
                    id = "mic",
                    title = "Microphone",
                    purpose = "Voice chat, continuous listening & on-device wake word detection",
                    isGranted = hasRuntime(Manifest.permission.RECORD_AUDIO),
                ),
            )
            if (Build.VERSION.SDK_INT >= 33) {
                add(
                    PermissionAuditItem(
                        id = "notifications",
                        title = "Post Notifications",
                        purpose = "Persistent foreground control, reminders & timer alerts",
                        isGranted = hasRuntime(Manifest.permission.POST_NOTIFICATIONS),
                    ),
                )
            }
            add(
                PermissionAuditItem(
                    id = "overlay",
                    title = "Floating Overlay (Draw over apps)",
                    purpose = "Draggable floating AYSHA glass orb & quick action menu",
                    isGranted = overlayGranted,
                    isSpecialSettings = true,
                ),
            )
            add(
                PermissionAuditItem(
                    id = "accessibility",
                    title = "Accessibility Service",
                    purpose = "Screen text reading & user-configured automation macros",
                    isGranted = accessibilityEnabled,
                    isSpecialSettings = true,
                ),
            )
            add(
                PermissionAuditItem(
                    id = "notif_listener",
                    title = "Notification Access",
                    purpose = "Read recent notifications only when you explicitly ask AYSHA",
                    isGranted = notificationEnabled,
                    isSpecialSettings = true,
                ),
            )
            add(
                PermissionAuditItem(
                    id = "contacts",
                    title = "Contacts",
                    purpose = "Resolve contact names before confirmation-gated call or SMS",
                    isGranted = hasRuntime(Manifest.permission.READ_CONTACTS),
                ),
            )
            add(
                PermissionAuditItem(
                    id = "call",
                    title = "Phone Calls",
                    purpose = "Place calls strictly after you tap Confirm",
                    isGranted = hasRuntime(Manifest.permission.CALL_PHONE),
                ),
            )
            add(
                PermissionAuditItem(
                    id = "sms",
                    title = "SMS Messages",
                    purpose = "Send SMS strictly after you tap Confirm (or compose in SMS app)",
                    isGranted = hasRuntime(Manifest.permission.SEND_SMS),
                ),
            )
            add(
                PermissionAuditItem(
                    id = "camera",
                    title = "Camera",
                    purpose = "Hardware flashlight toggle & camera launch",
                    isGranted = hasRuntime(Manifest.permission.CAMERA),
                ),
            )
            add(
                PermissionAuditItem(
                    id = "battery",
                    title = "Battery Optimization Exemption",
                    purpose = "Keep optional wake-word or floating orb uninterrupted",
                    isGranted = batteryExempt,
                    isSpecialSettings = true,
                ),
            )
        }
    }

    fun isAccessibilityEnabled(context: Context, packageName: String): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.split(':').any { it.startsWith("$packageName/") }
    }

    fun isNotificationListenerEnabled(context: Context, packageName: String): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty()
        return enabled.split(':').any { it.startsWith("$packageName/") }
    }
}
