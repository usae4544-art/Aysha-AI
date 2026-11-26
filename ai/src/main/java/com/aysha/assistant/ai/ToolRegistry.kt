package com.aysha.assistant.ai

import org.json.JSONArray
import org.json.JSONObject

data class ToolParamSpec(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = false,
)

data class ToolSpec(
    val name: String,
    val description: String,
    val params: List<ToolParamSpec>,
)

object ToolRegistry {

    val allTools: List<ToolSpec> = listOf(
        ToolSpec(
            name = "open_app",
            description = "Open an installed Android app by name (e.g. YouTube, WhatsApp, Chrome, Settings).",
            params = listOf(ToolParamSpec("name", "string", "App name to open", required = true)),
        ),
        ToolSpec(
            name = "open_settings",
            description = "Open a phone settings category (wifi, bluetooth, display, sound, location, battery, dnd, accessibility, notifications, overlay, general).",
            params = listOf(ToolParamSpec("category", "string", "Settings category", required = true)),
        ),
        ToolSpec(
            name = "set_alarm",
            description = "Open alarm setup for a specific 24-hour time.",
            params = listOf(
                ToolParamSpec("hour", "integer", "Hour 0-23", required = true),
                ToolParamSpec("minute", "integer", "Minute 0-59", required = true),
                ToolParamSpec("message", "string", "Alarm label"),
            ),
        ),
        ToolSpec(
            name = "create_reminder",
            description = "Schedule an in-app notification reminder minutes from now.",
            params = listOf(
                ToolParamSpec("minutes", "integer", "Minutes from now", required = true),
                ToolParamSpec("message", "string", "Reminder text", required = true),
            ),
        ),
        ToolSpec(
            name = "start_timer",
            description = "Start AYSHA's built-in spoken countdown timer.",
            params = listOf(
                ToolParamSpec("seconds", "integer", "Duration in seconds", required = true),
                ToolParamSpec("label", "string", "Timer label"),
            ),
        ),
        ToolSpec(
            name = "set_volume",
            description = "Adjust media, ring, alarm, or call volume percentage (0-100).",
            params = listOf(
                ToolParamSpec("stream", "string", "media, ring, alarm, call, or notification"),
                ToolParamSpec("percent", "integer", "0 to 100", required = true),
            ),
        ),
        ToolSpec(
            name = "set_torch",
            description = "Turn the phone flashlight on or off.",
            params = listOf(ToolParamSpec("enabled", "boolean", "true for ON, false for OFF", required = true)),
        ),
        ToolSpec(
            name = "media_control",
            description = "Control music/media playback (play_pause, play, pause, next, previous, stop).",
            params = listOf(ToolParamSpec("action", "string", "Media action", required = true)),
        ),
        ToolSpec(
            name = "open_website",
            description = "Open a URL in the default web browser.",
            params = listOf(ToolParamSpec("url", "string", "Website URL", required = true)),
        ),
        ToolSpec(
            name = "play_youtube",
            description = "Search or open a video/song on YouTube.",
            params = listOf(ToolParamSpec("query", "string", "Video or song search query", required = true)),
        ),
        ToolSpec(
            name = "open_camera",
            description = "Open the device camera app.",
            params = emptyList(),
        ),
        ToolSpec(
            name = "open_gallery",
            description = "Open the device photo gallery.",
            params = emptyList(),
        ),
        ToolSpec(
            name = "share_text",
            description = "Open the Android share sheet with text.",
            params = listOf(ToolParamSpec("text", "string", "Text to share", required = true)),
        ),
        ToolSpec(
            name = "copy_text",
            description = "Copy text to the clipboard.",
            params = listOf(ToolParamSpec("text", "string", "Text to copy", required = true)),
        ),
        ToolSpec(
            name = "get_battery_info",
            description = "Check current phone battery percentage, charging status, and temperature.",
            params = emptyList(),
        ),
        ToolSpec(
            name = "get_device_info",
            description = "Get Android device manufacturer, model, OS version, and ABI.",
            params = emptyList(),
        ),
        ToolSpec(
            name = "read_notifications",
            description = "Read recent device notifications if user enabled Notification Access.",
            params = listOf(ToolParamSpec("limit", "integer", "Max notifications to read")),
        ),
        ToolSpec(
            name = "prepare_phone_call",
            description = "Prepare a phone call (always prompts user for confirmation before calling).",
            params = listOf(ToolParamSpec("target", "string", "Contact name or phone number", required = true)),
        ),
        ToolSpec(
            name = "prepare_sms",
            description = "Prepare an SMS message (always prompts user for confirmation before sending).",
            params = listOf(
                ToolParamSpec("target", "string", "Contact name or phone number", required = true),
                ToolParamSpec("message", "string", "SMS text content", required = true),
            ),
        ),
        ToolSpec(
            name = "get_weather",
            description = "Get live current weather temperature, humidity, and wind for a city.",
            params = listOf(ToolParamSpec("city", "string", "City name", required = true)),
        ),
        ToolSpec(
            name = "web_search",
            description = "Search live public facts on the web via DuckDuckGo.",
            params = listOf(ToolParamSpec("query", "string", "Search query", required = true)),
        ),
        ToolSpec(
            name = "save_memory",
            description = "Save a personal fact or preference to AYSHA's local memory vault.",
            params = listOf(
                ToolParamSpec("title", "string", "Short topic title", required = true),
                ToolParamSpec("content", "string", "Fact to remember", required = true),
                ToolParamSpec("tags", "string", "Comma-separated keywords"),
            ),
        ),
        ToolSpec(
            name = "recall_memories",
            description = "Search user-approved saved memories.",
            params = listOf(ToolParamSpec("query", "string", "Topic to search")),
        ),
        ToolSpec(
            name = "run_macro",
            description = "Run a saved automation workflow or macro by name (e.g., Study Mode).",
            params = listOf(ToolParamSpec("name", "string", "Macro or workflow name", required = true)),
        ),
        ToolSpec(
            name = "read_screen_text",
            description = "Read visible UI text from the current screen using Accessibility Service.",
            params = emptyList(),
        ),
        ToolSpec(
            name = "tap_screen_text",
            description = "Tap an on-screen button or label via Accessibility Service.",
            params = listOf(ToolParamSpec("text", "string", "Visible label to tap", required = true)),
        ),
        ToolSpec(
            name = "type_on_screen",
            description = "Type text into the focused input field via Accessibility Service.",
            params = listOf(ToolParamSpec("text", "string", "Text to type", required = true)),
        ),
        ToolSpec(
            name = "scroll_screen",
            description = "Scroll down or up on the current screen via Accessibility Service.",
            params = listOf(ToolParamSpec("direction", "string", "down or up")),
        ),
        ToolSpec(
            name = "go_back",
            description = "Press system back via Accessibility Service.",
            params = emptyList(),
        ),
    )

    fun toGeminiFunctionDeclarations(nonBlocking: Boolean = false): JSONArray {
        return JSONArray().apply {
            for (spec in allTools) {
                put(
                    JSONObject().apply {
                        put("name", spec.name)
                        put("description", spec.description)
                        if (nonBlocking) {
                            put("behavior", "NON_BLOCKING")
                        }
                        put("parameters", buildSchemaObject(spec.params))
                    },
                )
            }
        }
    }

    fun toOpenAiTools(): JSONArray {
        return JSONArray().apply {
            for (spec in allTools) {
                put(
                    JSONObject().apply {
                        put("type", "function")
                        put(
                            "function",
                            JSONObject().apply {
                                put("name", spec.name)
                                put("description", spec.description)
                                put("parameters", buildSchemaObject(spec.params))
                            },
                        )
                    },
                )
            }
        }
    }

    private fun buildSchemaObject(params: List<ToolParamSpec>): JSONObject {
        val props = JSONObject()
        val req = JSONArray()
        for (p in params) {
            props.put(
                p.name,
                JSONObject()
                    .put("type", p.type)
                    .put("description", p.description),
            )
            if (p.required) req.put(p.name)
        }
        return JSONObject().apply {
            put("type", "object")
            put("properties", props)
            if (req.length() > 0) put("required", req)
        }
    }
}
