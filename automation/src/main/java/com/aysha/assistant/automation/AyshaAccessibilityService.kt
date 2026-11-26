package com.aysha.assistant.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenUiElement(
    val text: String,
    val contentDescription: String,
    val className: String,
    val clickable: Boolean,
    val editable: Boolean,
    val bounds: Rect,
)

class AyshaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _connected.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // On-demand traversal only; never uploads screen data passively.
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        _connected.value = false
        return super.onUnbind(intent)
    }

    fun readScreenElements(limit: Int = 50): List<ScreenUiElement> {
        val root = rootInActiveWindow ?: return emptyList()
        val list = mutableListOf<ScreenUiElement>()
        traverse(root, list, limit)
        return list
    }

    fun readScreenText(maxLines: Int = 40): String {
        val elements = readScreenElements(limit = maxLines * 2)
        return elements
            .mapNotNull { el ->
                val txt = listOf(el.text, el.contentDescription)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                txt.takeIf { it.isNotBlank() }
            }
            .distinct()
            .take(maxLines)
            .joinToString("\n")
    }

    fun tapText(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return false
        val root = rootInActiveWindow ?: return false
        val target = findNodeByText(root, q) ?: return false

        var clickableNode: AccessibilityNodeInfo? = target
        while (clickableNode != null && !clickableNode.isClickable) {
            clickableNode = clickableNode.parent
        }
        if (clickableNode != null && clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            return true
        }
        val rect = Rect()
        target.getBoundsInScreen(rect)
        if (rect.width() > 0 && rect.height() > 0) {
            return tapCoordinates(rect.centerX().toFloat(), rect.centerY().toFloat())
        }
        return false
    }

    fun tapCoordinates(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x.coerceAtLeast(1f), y.coerceAtLeast(1f)) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 60L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        val target = if (focused != null && focused.isEditable) focused else findFirstEditable(root)
        target ?: return false
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun scroll(forward: Boolean = true): Boolean {
        val root = rootInActiveWindow ?: return false
        val scrollable = findFirstScrollable(root) ?: root
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return scrollable.performAction(action)
    }

    fun goBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)

    suspend fun waitForScreenText(
        text: String,
        appear: Boolean = true,
        timeoutSeconds: Int = 20,
    ): Boolean {
        val q = text.trim().lowercase()
        val deadline = System.currentTimeMillis() + timeoutSeconds.coerceIn(1, 300) * 1000L
        while (System.currentTimeMillis() < deadline) {
            val found = readScreenText(60).lowercase().contains(q)
            if (appear && found) return true
            if (!appear && !found) return true
            delay(800L)
        }
        return false
    }

    private fun traverse(node: AccessibilityNodeInfo?, out: MutableList<ScreenUiElement>, limit: Int) {
        if (node == null || out.size >= limit) return
        val txt = node.text?.toString().orEmpty().trim()
        val desc = node.contentDescription?.toString().orEmpty().trim()
        if (txt.isNotBlank() || desc.isNotBlank() || node.isClickable || node.isEditable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            out.add(
                ScreenUiElement(
                    text = txt,
                    contentDescription = desc,
                    className = node.className?.toString().orEmpty().substringAfterLast('.'),
                    clickable = node.isClickable,
                    editable = node.isEditable,
                    bounds = rect,
                ),
            )
        }
        for (i in 0 until node.childCount) {
            traverse(node.getChild(i), out, limit)
            if (out.size >= limit) break
        }
    }

    private fun findNodeByText(node: AccessibilityNodeInfo?, query: String): AccessibilityNodeInfo? {
        node ?: return null
        val txt = node.text?.toString().orEmpty()
        val desc = node.contentDescription?.toString().orEmpty()
        if (txt.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val hit = findNodeByText(node.getChild(i), query)
            if (hit != null) return hit
        }
        return null
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val hit = findFirstEditable(node.getChild(i))
            if (hit != null) return hit
        }
        return null
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        node ?: return null
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val hit = findFirstScrollable(node.getChild(i))
            if (hit != null) return hit
        }
        return null
    }

    companion object {
        @Volatile
        var instance: AyshaAccessibilityService? = null
            private set

        private val _connected = MutableStateFlow(false)
        val connected: StateFlow<Boolean> = _connected.asStateFlow()
    }
}
