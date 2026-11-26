package com.aysha.assistant.ai

import com.aysha.assistant.automation.AyshaAccessibilityService
import com.aysha.assistant.automation.MacroEngine
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.PendingActionConfirmation
import com.aysha.assistant.core.PendingConfirmationType
import com.aysha.assistant.core.WhiteboardCard
import com.aysha.assistant.memory.MemoryRepository
import com.aysha.assistant.phonecontrol.PhoneActionResult
import com.aysha.assistant.phonecontrol.PhoneControlExecutor
import org.json.JSONArray
import org.json.JSONObject

data class ToolOutcome(
    val ok: Boolean,
    val output: JSONObject,
    val message: String,
    val confirmationRequired: PendingActionConfirmation? = null,
)

class ToolExecutor(
    private val phoneControl: PhoneControlExecutor,
    private val memoryRepo: MemoryRepository,
    private val macroEngine: MacroEngine,
    private val onStartTimer: (seconds: Int, label: String) -> Unit,
    private val onWhiteboardCard: (WhiteboardCard) -> Unit = {},
    private val onReadSettings: () -> AppSettings = { AppSettings() },
) {

    suspend fun execute(name: String, args: JSONObject): ToolOutcome {
        val outcome = when (name) {
            "open_app" -> phoneControl.openApp(args.optString("name")).toOutcome()
            "open_settings" -> phoneControl.openSettings(args.optString("category")).toOutcome()
            "set_alarm" -> phoneControl.setAlarm(
                hour = args.optInt("hour", 7),
                minute = args.optInt("minute", 0),
                message = args.optString("message", "AYSHA Alarm"),
            ).toOutcome()
            "create_reminder" -> phoneControl.createReminder(
                minutesFromNow = args.optInt("minutes", 5),
                message = args.optString("message", "Reminder"),
                clockTime24h = args.optString("clock_time", ""),
                tomorrow = args.optBoolean("tomorrow", false),
            ).toOutcome()
            "start_timer" -> {
                val secs = args.optInt("seconds", 60).coerceIn(5, 86_400)
                val label = args.optString("label", "AYSHA Timer").ifBlank { "AYSHA Timer" }
                onStartTimer(secs, label)
                ToolOutcome(
                    ok = true,
                    output = JSONObject().put("status", "Timer started").put("seconds", secs).put("label", label),
                    message = "Timer started for ${OfflineCommandParser.formatClock(secs.toLong())} ($label).",
                )
            }
            "set_volume" -> phoneControl.setVolume(
                streamName = args.optString("stream", "media"),
                percent = args.optInt("percent", 50),
            ).toOutcome()
            "set_torch" -> phoneControl.setTorch(args.optBoolean("enabled", true)).toOutcome()
            "media_control" -> phoneControl.mediaControl(args.optString("action", "play_pause")).toOutcome()
            "open_website" -> phoneControl.openWebsite(args.optString("url")).toOutcome()
            "play_youtube" -> phoneControl.playYouTube(args.optString("query")).toOutcome()
            "open_camera" -> phoneControl.openCamera().toOutcome()
            "open_gallery" -> phoneControl.openGallery().toOutcome()
            "share_text" -> phoneControl.shareText(args.optString("text")).toOutcome()
            "copy_text" -> phoneControl.copyText(args.optString("text")).toOutcome()
            "get_battery_info" -> phoneControl.getBatteryInfo().toOutcome()
            "get_device_info" -> phoneControl.getDeviceInfo().toOutcome()
            "read_notifications" -> phoneControl.readNotifications(args.optInt("limit", 6)).toOutcome()
            "prepare_phone_call" -> phoneControl.preparePhoneCall(args.optString("target")).toOutcome()
            "prepare_sms" -> phoneControl.prepareSms(
                targetNameOrNumber = args.optString("target"),
                messageBody = args.optString("message"),
            ).toOutcome()
            "trigger_sos" -> {
                val s = onReadSettings()
                val contact = s.emergencyContact.ifBlank { "112" }
                val note = args.optString("note").ifBlank { s.emergencySosMessage }
                ToolOutcome(
                    ok = true,
                    output = JSONObject().put("sos_pending", true).put("target", contact),
                    message = "Emergency SOS confirmation ready for $contact.",
                    confirmationRequired = PendingActionConfirmation(
                        type = PendingConfirmationType.EMERGENCY_SOS,
                        title = "Confirm Emergency SOS",
                        summary = "Call / SMS $contact?\n$note",
                        target = contact,
                        messageBody = note,
                    ),
                )
            }
            "get_weather" -> phoneControl.getWeather(args.optString("city", "Delhi")).toOutcome()
            "web_search" -> phoneControl.webSearch(args.optString("query")).toOutcome()
            "save_memory" -> {
                val title = args.optString("title").ifBlank { "Note" }
                val content = args.optString("content")
                val tags = args.optString("tags")
                if (content.isBlank()) {
                    ToolOutcome(false, JSONObject().put("error", "Memory content empty"), "Kya yaad rakhna hai?")
                } else {
                    memoryRepo.saveMemory(title, content, tags, approvedByUser = true, source = "aapne bataya")
                    ToolOutcome(
                        ok = true,
                        output = JSONObject().put("saved", true).put("title", title),
                        message = "Yaad rakh liya: $title — $content",
                    )
                }
            }
            "recall_memories" -> {
                val query = args.optString("query")
                val hits = if (query.isBlank()) memoryRepo.getAllMemories().take(8) else memoryRepo.searchMemories(query, 8)
                val arr = JSONArray()
                hits.forEach { m ->
                    arr.put(JSONObject().put("title", m.title).put("content", m.content).put("source", m.source))
                }
                val text = if (hits.isEmpty()) {
                    "Koi saved memory nahi mili."
                } else {
                    hits.joinToString("\n") { "• ${it.title}: ${it.content}" }
                }
                ToolOutcome(true, JSONObject().put("memories", arr), text)
            }
            "save_rule" -> {
                val ruleText = args.optString("rule").trim()
                if (ruleText.isBlank()) {
                    ToolOutcome(false, JSONObject().put("error", "Rule empty"), "Kya rule save karna hai?")
                } else {
                    memoryRepo.saveRule(ruleText, enabled = true)
                    ToolOutcome(
                        ok = true,
                        output = JSONObject().put("saved", true).put("rule", ruleText),
                        message = "AYSHA rule save ho gaya: \"$ruleText\"",
                    )
                }
            }
            "list_rules" -> {
                val rules = memoryRepo.getAllRules()
                val arr = JSONArray()
                rules.forEach { r -> arr.put(JSONObject().put("text", r.text).put("enabled", r.enabled)) }
                val msg = if (rules.isEmpty()) "Abhi koi custom rule set nahi hai." else rules.joinToString("\n") { "• ${it.text}" }
                ToolOutcome(true, JSONObject().put("rules", arr), msg)
            }
            "search_documents" -> {
                val query = args.optString("query")
                val hits = memoryRepo.searchDocumentSnippets(query, 4)
                val arr = JSONArray(hits)
                val msg = if (hits.isEmpty()) "Documents me isse milti julti jaankari nahi mili." else hits.joinToString("\n")
                ToolOutcome(true, JSONObject().put("snippets", arr), msg)
            }
            "show_whiteboard" -> {
                val title = args.optString("title", "Study Note")
                val stepsRaw = args.optString("steps", "")
                val code = args.optString("code", "")
                val steps = stepsRaw.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
                onWhiteboardCard(
                    WhiteboardCard(
                        title = title,
                        topic = title,
                        steps = steps.ifEmpty { listOf(stepsRaw.ifBlank { "Concept explained." }) },
                        codeSnippet = code,
                    ),
                )
                ToolOutcome(
                    ok = true,
                    output = JSONObject().put("displayed", true).put("title", title),
                    message = "Whiteboard par '$title' dikha diya hai.",
                )
            }
            "run_macro" -> {
                val macroName = args.optString("name")
                val macro = memoryRepo.getMacroByNameOrTrigger(macroName)
                if (macro == null) {
                    ToolOutcome(false, JSONObject().put("error", "Macro not found"), "'$macroName' workflow nahi mila.")
                } else {
                    val res = macroEngine.execute(macro)
                    val ok = res.isSuccess
                    if (ok) memoryRepo.markMacroPlayed(macro.name)
                    val msg = res.getOrElse { it.message ?: "Workflow failed" }
                    ToolOutcome(ok, JSONObject().put(if (ok) "result" else "error", msg), msg)
                }
            }
            "read_screen_text" -> {
                val acc = AyshaAccessibilityService.instance
                if (acc == null) {
                    ToolOutcome(false, JSONObject().put("error", "Accessibility Service OFF"), "Accessibility Service OFF hai. Phone Control / Permissions me ON karo.")
                } else {
                    val txt = acc.readScreenText()
                    val out = txt.ifBlank { "Screen par koi readable text nahi mila." }
                    ToolOutcome(true, JSONObject().put("text", out), out)
                }
            }
            "tap_screen_text" -> {
                val acc = AyshaAccessibilityService.instance
                if (acc == null) {
                    ToolOutcome(false, JSONObject().put("error", "Accessibility Service OFF"), "Accessibility Service OFF hai.")
                } else {
                    val ok = acc.tapText(args.optString("text"))
                    val msg = if (ok) "Tapped '${args.optString("text")}'" else "Element '${args.optString("text")}' nahi mila."
                    ToolOutcome(ok, JSONObject().put("tapped", ok), msg)
                }
            }
            "type_on_screen" -> {
                val acc = AyshaAccessibilityService.instance
                if (acc == null) {
                    ToolOutcome(false, JSONObject().put("error", "Accessibility Service OFF"), "Accessibility Service OFF hai.")
                } else {
                    val ok = acc.typeText(args.optString("text"))
                    val msg = if (ok) "Text type kar diya." else "Screen par editable box nahi mila."
                    ToolOutcome(ok, JSONObject().put("typed", ok), msg)
                }
            }
            "scroll_screen" -> {
                val acc = AyshaAccessibilityService.instance
                if (acc == null) {
                    ToolOutcome(false, JSONObject().put("error", "Accessibility Service OFF"), "Accessibility Service OFF hai.")
                } else {
                    val forward = !args.optString("direction").equals("up", ignoreCase = true)
                    val ok = acc.scroll(forward)
                    ToolOutcome(ok, JSONObject().put("scrolled", ok), if (ok) "Screen scroll kar di." else "Scroll nahi ho saka.")
                }
            }
            "go_back" -> {
                val acc = AyshaAccessibilityService.instance
                val ok = acc?.goBack() ?: false
                ToolOutcome(ok, JSONObject().put("back", ok), if (ok) "Back action executed." else "Accessibility Service OFF hai.")
            }
            else -> ToolOutcome(false, JSONObject().put("error", "Unknown tool: $name"), "Unsupported action: $name")
        }

        memoryRepo.logTool(name, outcome.message, outcome.ok)
        return outcome
    }

    private fun PhoneActionResult.toOutcome(): ToolOutcome {
        val merged = JSONObject(data.toString()).apply {
            put("ok", ok)
            put("message", message)
            if (!ok && !has("error")) put("error", message)
        }
        return ToolOutcome(
            ok = ok,
            output = merged,
            message = message,
            confirmationRequired = confirmationRequired,
        )
    }
}
