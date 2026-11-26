package com.aysha.assistant.core

import android.util.Log

/**
 * Redacts potential API keys and bearer tokens before writing any diagnostic log.
 */
object SafeLogger {

    private val secretPatterns = listOf(
        Regex("AIza[0-9A-Za-z_\\-]{20,}"),
        Regex("sk-[0-9A-Za-z_\\-]{16,}"),
        Regex("gsk_[0-9A-Za-z_\\-]{16,}"),
        Regex("Bearer\\s+[0-9A-Za-z._\\-]{12,}", RegexOption.IGNORE_CASE),
        Regex("key=[0-9A-Za-z._\\-]{12,}", RegexOption.IGNORE_CASE),
    )

    fun redact(raw: String): String {
        var sanitized = raw
        for (pattern in secretPatterns) {
            sanitized = sanitized.replace(pattern, "[REDACTED]")
        }
        return sanitized
    }

    fun d(tag: String, message: String) {
        Log.d(tag, redact(message))
    }

    fun w(tag: String, message: String, error: Throwable? = null) {
        val safeMsg = redact(message)
        val safeErr = error?.message?.let { redact(it) }
        Log.w(tag, if (safeErr != null) "$safeMsg ($safeErr)" else safeMsg)
    }

    fun e(tag: String, message: String, error: Throwable? = null) {
        val safeMsg = redact(message)
        val safeErr = error?.message?.let { redact(it) }
        Log.e(tag, if (safeErr != null) "$safeMsg ($safeErr)" else safeMsg)
    }
}
