package com.aysha.assistant.automation

import com.aysha.assistant.core.MacroDefinition
import com.aysha.assistant.phonecontrol.PhoneControlExecutor
import kotlinx.coroutines.delay

object MacroTemplate {
    fun substitute(template: String, params: Map<String, String>): String {
        var out = template
        params.forEach { (k, v) ->
            out = out.replace("{$k}", v).replace("{${k.replace(' ', '_')}}", v)
        }
        return out
    }
}

class MacroEngine(
    private val phoneControl: PhoneControlExecutor,
    private val onStartTimer: (seconds: Int, label: String) -> Unit = { _, _ -> },
) {
    suspend fun execute(
        macro: MacroDefinition,
        params: Map<String, String> = emptyMap(),
    ): Result<String> {
        if (macro.steps.isEmpty()) {
            return Result.failure(IllegalStateException("Macro '${macro.name}' has no steps."))
        }

        val logs = mutableListOf<String>()
        for ((index, step) in macro.steps.withIndex()) {
            val resolvedArgs = step.args.mapValues { (_, v) -> MacroTemplate.substitute(v, params) }
            val stepOk: Boolean
            val stepMsg: String

            when (step.tool) {
                "open_app", "open_app_by_name" -> {
                    val res = phoneControl.openApp(resolvedArgs["name"].orEmpty())
                    stepOk = res.ok
                    stepMsg = res.message
                }
                "wait" -> {
                    val secs = resolvedArgs["seconds"]?.toIntOrNull()?.coerceIn(1, 30) ?: 1
                    delay(secs * 1000L)
                    stepOk = true
                    stepMsg = "Waited ${secs}s"
                }
                "start_timer" -> {
                    val secs = resolvedArgs["seconds"]?.toIntOrNull()?.coerceIn(5, 86_400) ?: 300
                    val label = resolvedArgs["label"].orEmpty().ifBlank { macro.name }
                    onStartTimer(secs, label)
                    stepOk = true
                    stepMsg = "Started timer '$label' (${secs}s)"
                }
                "set_volume" -> {
                    val res = phoneControl.setVolume(
                        resolvedArgs["stream"].orEmpty().ifBlank { "media" },
                        resolvedArgs["percent"]?.toIntOrNull() ?: 50,
                    )
                    stepOk = res.ok
                    stepMsg = res.message
                }
                "set_torch" -> {
                    val enabled = resolvedArgs["enabled"]?.toBooleanStrictOrNull() ?: true
                    val res = phoneControl.setTorch(enabled)
                    stepOk = res.ok
                    stepMsg = res.message
                }
                "media_control" -> {
                    val res = phoneControl.mediaControl(resolvedArgs["action"].orEmpty().ifBlank { "play_pause" })
                    stepOk = res.ok
                    stepMsg = res.message
                }
                "open_settings" -> {
                    val res = phoneControl.openSettings(resolvedArgs["category"].orEmpty())
                    stepOk = res.ok
                    stepMsg = res.message
                }
                "open_website" -> {
                    val res = phoneControl.openWebsite(resolvedArgs["url"].orEmpty())
                    stepOk = res.ok
                    stepMsg = res.message
                }
                "tap_text" -> {
                    val acc = AyshaAccessibilityService.instance
                    if (acc == null) {
                        stepOk = false
                        stepMsg = "Accessibility Service OFF hai"
                    } else {
                        stepOk = acc.tapText(resolvedArgs["text"].orEmpty())
                        stepMsg = if (stepOk) "Tapped '${resolvedArgs["text"]}'" else "Text '${resolvedArgs["text"]}' not found"
                    }
                }
                "type_text" -> {
                    val acc = AyshaAccessibilityService.instance
                    if (acc == null) {
                        stepOk = false
                        stepMsg = "Accessibility Service OFF hai"
                    } else {
                        stepOk = acc.typeText(resolvedArgs["text"].orEmpty())
                        stepMsg = if (stepOk) "Typed text" else "Editable field not found"
                    }
                }
                "scroll" -> {
                    val acc = AyshaAccessibilityService.instance
                    if (acc == null) {
                        stepOk = false
                        stepMsg = "Accessibility Service OFF hai"
                    } else {
                        val forward = !resolvedArgs["direction"].equals("up", ignoreCase = true)
                        stepOk = acc.scroll(forward)
                        stepMsg = if (stepOk) "Scrolled" else "Scroll failed"
                    }
                }
                "go_back" -> {
                    val acc = AyshaAccessibilityService.instance
                    stepOk = acc?.goBack() ?: false
                    stepMsg = if (stepOk) "Navigated back" else "Accessibility Service OFF hai"
                }
                "wait_for_screen_text" -> {
                    val acc = AyshaAccessibilityService.instance
                    if (acc == null) {
                        stepOk = false
                        stepMsg = "Accessibility Service OFF hai"
                    } else {
                        val appear = !resolvedArgs["mode"].equals("disappear", ignoreCase = true)
                        val timeout = resolvedArgs["timeout_seconds"]?.toIntOrNull() ?: 20
                        stepOk = acc.waitForScreenText(resolvedArgs["text"].orEmpty(), appear, timeout)
                        stepMsg = if (stepOk) "Condition met for '${resolvedArgs["text"]}'" else "Timeout waiting for '${resolvedArgs["text"]}'"
                    }
                }
                else -> {
                    stepOk = false
                    stepMsg = "Unknown macro tool '${step.tool}'"
                }
            }

            logs.add("Step ${index + 1} (${step.tool}): $stepMsg")
            if (!stepOk && !step.optional) {
                return Result.failure(IllegalStateException(logs.joinToString(" -> ")))
            }
        }
        return Result.success("Workflow '${macro.name}' complete: ${logs.joinToString(" | ")}")
    }
}
