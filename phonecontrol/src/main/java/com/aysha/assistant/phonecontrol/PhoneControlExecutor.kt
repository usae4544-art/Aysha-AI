package com.aysha.assistant.phonecontrol

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.telephony.SmsManager
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.aysha.assistant.core.PendingActionConfirmation
import com.aysha.assistant.core.PendingConfirmationType
import com.aysha.assistant.core.SafeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class PhoneActionResult(
    val ok: Boolean,
    val message: String,
    val data: JSONObject = JSONObject(),
    val confirmationRequired: PendingActionConfirmation? = null,
)

class PhoneControlExecutor(private val context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var torchEnabled = false

    fun openApp(appName: String): PhoneActionResult {
        val clean = appName.trim()
        if (clean.isEmpty()) return PhoneActionResult(false, "Kis app ko open karna hai?")
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(intent, 0)
        val q = clean.lowercase()

        val match = activities.firstOrNull {
            it.loadLabel(pm).toString().equals(clean, ignoreCase = true)
        } ?: activities.firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(q) ||
                it.activityInfo.packageName.lowercase().contains(q)
        } ?: return PhoneActionResult(false, "'$clean' naam ka installed app nahi mila.")

        val pkg = match.activityInfo.packageName
        val label = match.loadLabel(pm).toString()
        val launchIntent = pm.getLaunchIntentForPackage(pkg)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return PhoneActionResult(false, "$label launch nahi ho saka.")

        return runCatching {
            appContext.startActivity(launchIntent)
            PhoneActionResult(true, "$label open kar diya.", JSONObject().put("package", pkg).put("app", label))
        }.getOrElse { err ->
            PhoneActionResult(false, "$label open karne me error aaya: ${err.message}")
        }
    }

    fun openSettings(category: String): PhoneActionResult {
        val action = when (category.trim().lowercase()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth", "bt" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness", "screen" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume", "audio" -> Settings.ACTION_SOUND_SETTINGS
            "location", "gps" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "battery" -> Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            "dnd", "do_not_disturb" -> Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "notifications", "notification_access" -> Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
            "overlay" -> Settings.ACTION_MANAGE_OVERLAY_PERMISSION
            "app", "details" -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        val intent = if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS || action == Settings.ACTION_MANAGE_OVERLAY_PERMISSION) {
            Intent(action, Uri.parse("package:${appContext.packageName}"))
        } else {
            Intent(action)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return runCatching {
            appContext.startActivity(intent)
            PhoneActionResult(true, "Settings (${category.ifBlank { "System" }}) open kar di gayi.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Settings open nahi ho payi: ${err.message}")
        }
    }

    fun setAlarm(hour: Int, minute: Int, message: String = "AYSHA Alarm"): PhoneActionResult {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, h)
            putExtra(AlarmClock.EXTRA_MINUTES, m)
            putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "AYSHA Alarm" })
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            appContext.startActivity(intent)
            val formatted = String.format("%02d:%02d", h, m)
            PhoneActionResult(true, "$formatted ka alarm setup screen open kar diya.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Alarm app available nahi hai: ${err.message}")
        }
    }

    fun createReminder(minutesFromNow: Int, message: String): PhoneActionResult {
        val mins = minutesFromNow.coerceIn(1, 1440)
        val cleanMsg = message.ifBlank { "AYSHA Reminder" }
        val am = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(appContext, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MESSAGE, cleanMsg)
        }
        val pi = PendingIntent.getBroadcast(
            appContext,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val triggerAt = SystemClock.elapsedRealtime() + mins * 60_000L
        runCatching {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi)
        }.onFailure { err ->
            return PhoneActionResult(false, "Reminder schedule failed: ${err.message}")
        }
        return PhoneActionResult(
            true,
            "$mins minute baad ka reminder set kar diya: \"$cleanMsg\"",
            JSONObject().put("minutes", mins).put("message", cleanMsg),
        )
    }

    fun setVolume(streamName: String, percent: Int): PhoneActionResult {
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = when (streamName.trim().lowercase()) {
            "ring", "ringer" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "call", "voice" -> AudioManager.STREAM_VOICE_CALL
            "notification" -> AudioManager.STREAM_NOTIFICATION
            else -> AudioManager.STREAM_MUSIC
        }
        val p = percent.coerceIn(0, 100)
        val max = am.getStreamMaxVolume(stream)
        val target = ((max * p) / 100f).toInt().coerceIn(0, max)
        return runCatching {
            am.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
            PhoneActionResult(true, "${streamName.ifBlank { "Media" }} volume $p% par set kar diya.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Volume change restricted hai (DND permission check karo): ${err.message}")
        }
    }

    fun setTorch(enabled: Boolean): PhoneActionResult {
        val cm = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return runCatching {
            val camId = cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return PhoneActionResult(false, "Is device me flashlight hardware nahi mila.")
            cm.setTorchMode(camId, enabled)
            torchEnabled = enabled
            PhoneActionResult(true, if (enabled) "Flashlight ON kar di hai." else "Flashlight OFF kar di hai.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Flashlight control failed: ${err.message}")
        }
    }

    fun toggleTorch(): PhoneActionResult = setTorch(!torchEnabled)

    fun mediaControl(action: String): PhoneActionResult {
        val am = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyCode = when (action.trim().lowercase()) {
            "next", "skip" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "prev", "back" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "pause" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "play" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        return runCatching {
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            PhoneActionResult(true, "Media command ($action) bhej di gayi.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Media control failed: ${err.message}")
        }
    }

    fun openWebsite(url: String): PhoneActionResult {
        val normalized = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            PhoneActionResult(true, "Website open kar di: $normalized")
        }.getOrElse { err ->
            PhoneActionResult(false, "Browser open nahi ho saka: ${err.message}")
        }
    }

    fun playYouTube(query: String): PhoneActionResult {
        val clean = query.trim()
        val uri = if (clean.isEmpty()) {
            Uri.parse("https://www.youtube.com")
        } else {
            Uri.parse("https://www.youtube.com/results?search_query=${URLEncoder.encode(clean, "UTF-8")}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            PhoneActionResult(true, if (clean.isEmpty()) "YouTube open kar diya." else "YouTube par '$clean' search open kar diya.")
        }.getOrElse { err ->
            PhoneActionResult(false, "YouTube open nahi ho saka: ${err.message}")
        }
    }

    fun openCamera(): PhoneActionResult {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            PhoneActionResult(true, "Camera open kar diya.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Camera app nahi mila: ${err.message}")
        }
    }

    fun openGallery(): PhoneActionResult {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "image/*"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            appContext.startActivity(intent)
            PhoneActionResult(true, "Gallery open kar di.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Gallery open nahi ho saki: ${err.message}")
        }
    }

    fun shareText(text: String): PhoneActionResult {
        val clean = text.trim()
        if (clean.isEmpty()) return PhoneActionResult(false, "Share karne ke liye text khali hai.")
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, clean)
        }
        val chooser = Intent.createChooser(sendIntent, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(chooser)
            PhoneActionResult(true, "Share sheet open kar di.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Share failed: ${err.message}")
        }
    }

    fun copyText(text: String): PhoneActionResult {
        val clean = text.trim()
        if (clean.isEmpty()) return PhoneActionResult(false, "Copy karne ke liye text khali hai.")
        mainHandler.post {
            val cb = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cb.setPrimaryClip(ClipData.newPlainText("AYSHA", clean))
        }
        return PhoneActionResult(true, "Text clipboard me copy kar diya.")
    }

    fun getBatteryInfo(): PhoneActionResult {
        val statusIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = statusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = statusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val plugged = statusIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = plugged != 0
        val tempC = (statusIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val data = JSONObject().apply {
            put("battery_percent", pct)
            put("is_charging", charging)
            put("temperature_c", tempC)
            put("device", "${Build.MANUFACTURER} ${Build.MODEL}")
        }
        val msg = "Battery: $pct% (${if (charging) "Charging" else "Discharging"}), Temp: ${tempC}°C"
        return PhoneActionResult(true, msg, data)
    }

    fun getDeviceInfo(): PhoneActionResult {
        val rt = Runtime.getRuntime()
        val usedMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
        val data = JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("model", Build.MODEL)
            put("android_version", Build.VERSION.RELEASE)
            put("sdk_int", Build.VERSION.SDK_INT)
            put("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
            put("app_heap_mb", usedMb)
        }
        val msg = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}, ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64"})"
        return PhoneActionResult(true, msg, data)
    }

    fun readNotifications(limit: Int = 6): PhoneActionResult {
        if (!AyshaNotificationListener.connected.value) {
            return PhoneActionResult(
                false,
                "Notification Access OFF hai. Privacy / Permissions screen me jakar explicitly ON karo.",
                JSONObject().put("listener_connected", false),
            )
        }
        val items = AyshaNotificationListener.getRecent(limit)
        val arr = JSONArray()
        items.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("app", n.appName)
                    put("title", n.title)
                    put("text", n.text)
                },
            )
        }
        val summary = if (items.isEmpty()) {
            "Abhi koi recent notification capture nahi hui hai."
        } else {
            items.joinToString("\n") { "• [${it.appName}] ${it.title}: ${it.text}" }
        }
        return PhoneActionResult(
            true,
            summary,
            JSONObject().put("listener_connected", true).put("notifications", arr),
        )
    }

    /**
     * Requests explicit user confirmation before initiating a phone call.
     */
    fun preparePhoneCall(targetNameOrNumber: String): PhoneActionResult {
        val resolved = resolvePhoneNumber(targetNameOrNumber)
            ?: return PhoneActionResult(false, "'$targetNameOrNumber' ka phone number nahi mila. Contacts permission check karo ya number bolo.")
        return PhoneActionResult(
            ok = true,
            message = "Call confirmation required for ${resolved.first} (${resolved.second})",
            confirmationRequired = PendingActionConfirmation(
                type = PendingConfirmationType.PHONE_CALL,
                title = "Confirm Phone Call",
                summary = "Call ${resolved.first} (${resolved.second})?",
                target = resolved.second,
            ),
        )
    }

    fun executeConfirmedCall(number: String): PhoneActionResult {
        val hasCall = ContextCompat.checkSelfPermission(appContext, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val intent = Intent(if (hasCall) Intent.ACTION_CALL else Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            appContext.startActivity(intent)
            PhoneActionResult(true, if (hasCall) "$number par call lagaya ja raha hai." else "Dialer me $number open kar diya.")
        }.getOrElse { err ->
            PhoneActionResult(false, "Call initiate failed: ${err.message}")
        }
    }

    fun prepareSms(targetNameOrNumber: String, messageBody: String): PhoneActionResult {
        val resolved = resolvePhoneNumber(targetNameOrNumber)
            ?: return PhoneActionResult(false, "'$targetNameOrNumber' ka number nahi mila.")
        return PhoneActionResult(
            ok = true,
            message = "SMS confirmation required for ${resolved.first}",
            confirmationRequired = PendingActionConfirmation(
                type = PendingConfirmationType.SEND_SMS,
                title = "Confirm SMS",
                summary = "Send SMS to ${resolved.first} (${resolved.second})?\n\"$messageBody\"",
                target = resolved.second,
                messageBody = messageBody,
            ),
        )
    }

    fun executeConfirmedSms(number: String, body: String): PhoneActionResult {
        val hasSms = ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        if (hasSms) {
            val sent = runCatching {
                val mgr = if (Build.VERSION.SDK_INT >= 31) {
                    appContext.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                mgr.sendTextMessage(number, null, body, null, null)
            }
            if (sent.isSuccess) {
                return PhoneActionResult(true, "$number par SMS bhej diya gaya.")
            }
        }
        val compose = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
            putExtra("sms_body", body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            appContext.startActivity(compose)
            PhoneActionResult(true, "SMS composer $number ke liye open kar diya.")
        }.getOrElse { err ->
            PhoneActionResult(false, "SMS open failed: ${err.message}")
        }
    }

    private fun resolvePhoneNumber(query: String): Pair<String, String>? {
        val clean = query.trim()
        if (clean.isEmpty()) return null
        val digits = clean.filter { it.isDigit() || it == '+' }
        if (digits.length >= 6 && clean.count { it.isLetter() } <= 1) {
            return clean to digits
        }
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return null
        }
        return runCatching {
            appContext.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$clean%"),
                null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    val name = c.getString(0).orEmpty()
                    val num = c.getString(1).orEmpty().replace(" ", "")
                    if (num.isNotBlank()) name.ifBlank { clean } to num else null
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    suspend fun getWeather(city: String): PhoneActionResult = withContext(Dispatchers.IO) {
        val cleanCity = city.trim().ifBlank { "Delhi" }
        runCatching {
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${URLEncoder.encode(cleanCity, "UTF-8")}&count=1"
            val geoResp = http.newCall(Request.Builder().url(geoUrl).get().build()).execute().use { r ->
                if (!r.isSuccessful) throw IllegalStateException("Geocoding HTTP ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            val first = geoResp.optJSONArray("results")?.optJSONObject(0)
                ?: return@withContext PhoneActionResult(false, "'$cleanCity' city nahi mili.")
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")
            val name = first.optString("name", cleanCity)
            val country = first.optString("country", "")

            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,wind_speed_10m"
            val wResp = http.newCall(Request.Builder().url(weatherUrl).get().build()).execute().use { r ->
                if (!r.isSuccessful) throw IllegalStateException("Weather HTTP ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            val cur = wResp.optJSONObject("current") ?: JSONObject()
            val temp = cur.optDouble("temperature_2m", 0.0)
            val hum = cur.optInt("relative_humidity_2m", 0)
            val wind = cur.optDouble("wind_speed_10m", 0.0)

            val label = if (country.isNotBlank()) "$name, $country" else name
            val data = JSONObject().apply {
                put("location", label)
                put("temp_c", temp)
                put("humidity", hum)
                put("wind_kmh", wind)
            }
            PhoneActionResult(true, "$label ka mausam: ${temp}°C, Humidity $hum%, Wind $wind km/h", data)
        }.getOrElse { err ->
            SafeLogger.w(TAG, "Weather lookup failed", err)
            PhoneActionResult(false, "Internet ya weather service unavailable hai.")
        }
    }

    suspend fun webSearch(query: String): PhoneActionResult = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext PhoneActionResult(false, "Search query khali hai.")
        runCatching {
            val url = "https://api.duckduckgo.com/?q=${URLEncoder.encode(clean, "UTF-8")}&format=json&no_html=1&skip_disambig=1"
            val obj = http.newCall(Request.Builder().url(url).get().build()).execute().use { r ->
                if (!r.isSuccessful) throw IllegalStateException("Search HTTP ${r.code}")
                JSONObject(r.body?.string().orEmpty())
            }
            val abstractText = obj.optString("AbstractText")
            val heading = obj.optString("Heading")
            val related = obj.optJSONArray("RelatedTopics") ?: JSONArray()
            val bullets = mutableListOf<String>()
            for (i in 0 until related.length()) {
                val item = related.optJSONObject(i) ?: continue
                val t = item.optString("Text")
                if (t.isNotBlank()) bullets.add(t)
                if (bullets.size >= 4) break
            }
            val summary = buildString {
                if (heading.isNotBlank() && abstractText.isNotBlank()) {
                    appendLine("$heading: $abstractText")
                } else if (abstractText.isNotBlank()) {
                    appendLine(abstractText)
                }
                bullets.forEach { appendLine("• $it") }
            }.trim().ifBlank { "'$clean' ke liye instant result nahi mila — browser search use kar sakte ho." }
            PhoneActionResult(true, summary, JSONObject().put("query", clean).put("abstract", abstractText))
        }.getOrElse { err ->
            SafeLogger.w(TAG, "DuckDuckGo search failed", err)
            PhoneActionResult(false, "Web search failed (internet check karo).")
        }
    }

    companion object {
        private const val TAG = "AyshaPhoneControl"
    }
}
