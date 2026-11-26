package com.aysha.assistant.ai

import com.aysha.assistant.core.AssistantLanguage
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface OfflineResolution {
    data class DirectReply(val reply: String) : OfflineResolution
    data class InAppTimer(val seconds: Int, val label: String) : OfflineResolution
    data class ToolCall(val toolName: String, val args: JSONObject, val statusLabel: String) : OfflineResolution
}

object OfflineCommandParser {

    fun parse(
        rawText: String,
        assistantName: String = "AYSHA",
        userName: String = "Friend",
        language: AssistantLanguage = AssistantLanguage.HINGLISH,
    ): OfflineResolution {
        val text = rawText.trim()
        val lower = text.lowercase()

        // 1. Identity / greeting
        if (lower.matches(Regex("^(hi|hello|hey|namaste|namaskar|salam|hii+|heyy+)(\\s+${assistantName.lowercase()})?[!?.]*$"))) {
            val reply = when (language) {
                AssistantLanguage.HINDI -> "नमस्ते $userName! मैं $assistantName हूँ। बताइए, आज आपकी क्या मदद करूँ?"
                AssistantLanguage.ENGLISH -> "Hello $userName! I am $assistantName, your personal assistant. How can I help you today?"
                AssistantLanguage.HINGLISH -> "Hello $userName! Main $assistantName hoon — 100% free aur offline-ready. Batao aaj kya help karoon?"
            }
            return OfflineResolution.DirectReply(reply)
        }

        if (lower.contains("who are you") || lower.contains("tum kaun ho") || lower.contains("tumhara naam") || lower.contains("your name")) {
            return OfflineResolution.DirectReply(
                "Main $assistantName hoon — aapki personal Android AI assistant! Main voice, phone control, alarms, timers, macros, weather, web search aur memory bina kisi subscription ke handle karti hoon.",
            )
        }

        // 2. Time & Date
        if (lower.contains("time kya") || lower.contains("kitne baje") || lower.contains("what time") || lower == "time") {
            val now = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return OfflineResolution.DirectReply("Abhi $now ho rahe hain.")
        }
        if (lower.contains("aaj ki date") || lower.contains("today's date") || lower.contains("what date") || lower.contains("aaj ka din")) {
            val date = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
            return OfflineResolution.DirectReply("Aaj $date hai.")
        }

        // 3. Calculator (pure offline arithmetic)
        val mathCandidate = lower
            .replace("kitna hota hai", "")
            .replace("kitna hoga", "")
            .replace("calculate", "")
            .replace("what is", "")
            .replace("=", "")
            .trim()
        if (mathCandidate.matches(Regex("^[0-9.+\\-*/()\\s]{3,}$")) &&
            mathCandidate.any { it in listOf('+', '-', '*', '/') }
        ) {
            val evaluated = evaluateSimpleMath(mathCandidate)
            if (evaluated != null) {
                val formatted = if (evaluated % 1.0 == 0.0) evaluated.toLong().toString() else String.format(Locale.US, "%.4f", evaluated).trimEnd('0').trimEnd('.')
                return OfflineResolution.DirectReply("$mathCandidate = $formatted")
            }
        }

        // 4. Built-in Study Mode / custom workflow trigger
        if (lower.contains("study mode") || lower.contains("start study")) {
            return OfflineResolution.ToolCall("run_macro", JSONObject().put("name", "Study Mode"), "Starting Study Mode...")
        }

        // 5. Timers
        val timerMatch = Regex("(\\d+)\\s*(sec|second|seconds|min|minute|minutes|hr|hour|hours)\\b").find(lower)
        if (timerMatch != null && (lower.contains("timer") || lower.contains("countdown"))) {
            val qty = timerMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            val unit = timerMatch.groupValues[2]
            val secs = when {
                unit.startsWith("hr") || unit.startsWith("hour") -> qty * 3600
                unit.startsWith("min") -> qty * 60
                else -> qty
            }
            return OfflineResolution.InAppTimer(secs, "$assistantName Timer")
        }

        // 6. Reminders
        if (lower.contains("remind") || lower.contains("reminder")) {
            val mins = Regex("(\\d+)\\s*(min|minute|minutes)").find(lower)?.groupValues?.get(1)?.toIntOrNull() ?: 5
            val msg = text
                .replace(Regex("(?i)remind me( in \\d+ minutes?)?( to)?"), "")
                .replace(Regex("(?i)\\d+\\s*minute(s)?\\s*(baad|me)?\\s*(ka)?\\s*reminder( lagao| set karo)?"), "")
                .trim()
                .ifBlank { "AYSHA Reminder" }
            return OfflineResolution.ToolCall(
                "create_reminder",
                JSONObject().put("minutes", mins).put("message", msg),
                "Setting reminder...",
            )
        }

        // 7. Alarm
        if (lower.contains("alarm")) {
            val hm = Regex("(\\d{1,2})(?::(\\d{2}))?").find(lower)
            var hour = hm?.groupValues?.get(1)?.toIntOrNull() ?: 7
            val min = hm?.groupValues?.get(2)?.toIntOrNull() ?: 0
            if ((lower.contains("pm") || lower.contains("shaam") || lower.contains("raat")) && hour < 12) hour += 12
            if ((lower.contains("am") || lower.contains("subah")) && hour == 12) hour = 0
            return OfflineResolution.ToolCall(
                "set_alarm",
                JSONObject().put("hour", hour).put("minute", min).put("message", "$assistantName Alarm"),
                "Setting alarm...",
            )
        }

        // 8. Torch / Flashlight
        if (lower.contains("torch") || lower.contains("flash")) {
            val off = lower.contains("off") || lower.contains("band")
            return OfflineResolution.ToolCall("set_torch", JSONObject().put("enabled", !off), "Toggling flashlight...")
        }

        // 9. Battery & Device info
        if (lower.contains("battery") || lower.contains("charge")) {
            return OfflineResolution.ToolCall("get_battery_info", JSONObject(), "Checking battery...")
        }
        if (lower.contains("device info") || lower.contains("phone info") || lower.contains("kaun sa phone")) {
            return OfflineResolution.ToolCall("get_device_info", JSONObject(), "Checking device info...")
        }

        // 10. Volume
        if (lower.contains("volume")) {
            val pct = when {
                lower.contains("mute") || lower.contains("silent") -> 0
                lower.contains("full") || lower.contains("max") -> 100
                else -> Regex("(\\d{1,3})").find(lower)?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100) ?: 60
            }
            val stream = if (lower.contains("ring")) "ring" else if (lower.contains("alarm")) "alarm" else "media"
            return OfflineResolution.ToolCall("set_volume", JSONObject().put("stream", stream).put("percent", pct), "Adjusting volume...")
        }

        // 11. Media controls
        if (lower.contains("play music") || lower.contains("gana chalao") || lower.contains("pause music") || lower.contains("pause song")) {
            val action = if (lower.contains("pause")) "pause" else "play_pause"
            return OfflineResolution.ToolCall("media_control", JSONObject().put("action", action), "Controlling media...")
        }
        if (lower.contains("next song") || lower.contains("agla gana")) {
            return OfflineResolution.ToolCall("media_control", JSONObject().put("action", "next"), "Skipping track...")
        }
        if (lower.contains("previous song") || lower.contains("pichla gana")) {
            return OfflineResolution.ToolCall("media_control", JSONObject().put("action", "previous"), "Previous track...")
        }

        // 12. Notifications
        if (lower.contains("notification") && (lower.contains("read") || lower.contains("padh") || lower.contains("check") || lower.contains("show"))) {
            return OfflineResolution.ToolCall("read_notifications", JSONObject().put("limit", 6), "Reading notifications...")
        }

        // 13. Confirmation-gated Call & SMS
        val callMatch = Regex("^(?:call|dial|phone lagao)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
        if (callMatch != null) {
            val target = callMatch.groupValues[1].trim()
            return OfflineResolution.ToolCall("prepare_phone_call", JSONObject().put("target", target), "Preparing call...")
        }
        val smsMatch = Regex("^(?:send sms to|sms|message)\\s+([^\\s]+)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
        if (smsMatch != null) {
            return OfflineResolution.ToolCall(
                "prepare_sms",
                JSONObject().put("target", smsMatch.groupValues[1]).put("message", smsMatch.groupValues[2]),
                "Preparing SMS...",
            )
        }

        // 14. Camera / Gallery / Settings
        if (lower.contains("open camera") || lower.contains("camera kholo")) {
            return OfflineResolution.ToolCall("open_camera", JSONObject(), "Opening Camera...")
        }
        if (lower.contains("open gallery") || lower.contains("gallery kholo")) {
            return OfflineResolution.ToolCall("open_gallery", JSONObject(), "Opening Gallery...")
        }
        if (lower.contains("wifi settings")) return OfflineResolution.ToolCall("open_settings", JSONObject().put("category", "wifi"), "Opening Wi-Fi Settings...")
        if (lower.contains("bluetooth settings")) return OfflineResolution.ToolCall("open_settings", JSONObject().put("category", "bluetooth"), "Opening Bluetooth Settings...")
        if (lower.contains("open settings")) return OfflineResolution.ToolCall("open_settings", JSONObject().put("category", "general"), "Opening Settings...")

        // 15. YouTube / Open App
        if (lower.contains("youtube") && (lower.contains("pe ") || lower.contains("par ") || lower.contains("play ") || lower.contains("search "))) {
            val query = text
                .replace(Regex("(?i).*?youtube\\s*(pe|par|on)?"), "")
                .replace(Regex("(?i)(chalao|play|search|karo|dikhao)"), "")
                .trim()
            return OfflineResolution.ToolCall("play_youtube", JSONObject().put("query", query), "Opening YouTube...")
        }
        val openAppMatch = Regex("^(?:open|launch|kholo|start)\\s+([a-zA-Z0-9 ._-]+?)(?:\\s+kholo|\\s+open)?$", RegexOption.IGNORE_CASE).find(text)
        if (openAppMatch != null) {
            val target = openAppMatch.groupValues[1].trim()
            if (target.contains(".")) {
                return OfflineResolution.ToolCall("open_website", JSONObject().put("url", target), "Opening website...")
            }
            return OfflineResolution.ToolCall("open_app", JSONObject().put("name", target), "Opening $target...")
        }

        // 16. Weather & Web Search
        if (lower.contains("weather") || lower.contains("mausam")) {
            val city = Regex("(?:in|me|of|at)\\s+([a-zA-Z\\s]{2,30})", RegexOption.IGNORE_CASE)
                .find(text)?.groupValues?.get(1)?.trim() ?: "Delhi"
            return OfflineResolution.ToolCall("get_weather", JSONObject().put("city", city), "Fetching weather...")
        }
        if (lower.startsWith("search ") || lower.startsWith("google ")) {
            val q = text.substringAfter(' ').trim()
            return OfflineResolution.ToolCall("web_search", JSONObject().put("query", q), "Searching web...")
        }

        // 17. Memory save / recall
        val memSave = Regex("^(?:yaad rakho|remember that|remember)\\s+(.+)$", RegexOption.IGNORE_CASE).find(text)
        if (memSave != null) {
            val fact = memSave.groupValues[1].trim()
            val title = fact.split(Regex("\\s+")).take(3).joinToString(" ").replaceFirstChar { it.uppercase() }
            return OfflineResolution.ToolCall(
                "save_memory",
                JSONObject().put("title", title).put("content", fact).put("tags", "personal"),
                "Saving memory...",
            )
        }
        if (lower.contains("memories") || lower.contains("kya yaad hai") || lower.contains("recall")) {
            return OfflineResolution.ToolCall("recall_memories", JSONObject().put("query", ""), "Checking memories...")
        }

        // 18. Friendly Offline Conversational Guide
        return OfflineResolution.DirectReply(
            "Main abhi **Offline Mode** me hoon (zero API key required!). Aap mujhse ye sab bolkar ya likhkar karwa sakte ho:\n" +
                "• Apps & Controls: \"Open YouTube\", \"Torch on\", \"Volume 70\", \"Play music\", \"Open Camera\"\n" +
                "• Time & Math: \"Time kya hua\", \"25 * 48\", \"5 minute ka timer lagao\", \"7 AM alarm lagao\"\n" +
                "• Workflows & Memory: \"Start Study Mode\", \"Yaad rakho mera favourite color blue hai\"\n" +
                "• Calls/SMS & Info: \"Call Mom\", \"Battery check karo\", \"Weather in Mumbai\"\n\n" +
                "Full AI conversation ke liye Settings → AI Provider Settings me apni free **Google Gemini** ya **Groq** key add karein!",
        )
    }

    fun formatClock(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s) else String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun evaluateSimpleMath(expr: String): Double? {
        return runCatching {
            object {
                private var pos = -1
                private var ch = 0

                private fun nextChar() {
                    ch = if (++pos < expr.length) expr[pos].code else -1
                }

                private fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < expr.length) throw IllegalArgumentException("Unexpected char")
                    return x
                }

                private fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        when {
                            eat('+'.code) -> x += parseTerm()
                            eat('-'.code) -> x -= parseTerm()
                            else -> return x
                        }
                    }
                }

                private fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        when {
                            eat('*'.code) -> x *= parseFactor()
                            eat('/'.code) -> {
                                val d = parseFactor()
                                if (d == 0.0) throw ArithmeticException("Division by zero")
                                x /= d
                            }
                            else -> return x
                        }
                    }
                }

                private fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return -parseFactor()
                    val startPos = pos
                    val x: Double
                    if (eat('('.code)) {
                        x = parseExpression()
                        if (!eat(')'.code)) throw IllegalArgumentException("Missing ')'")
                    } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                        while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                        x = expr.substring(startPos, pos).toDouble()
                    } else {
                        throw IllegalArgumentException("Unexpected token")
                    }
                    return x
                }
            }.parse()
        }.getOrNull()
    }
}
