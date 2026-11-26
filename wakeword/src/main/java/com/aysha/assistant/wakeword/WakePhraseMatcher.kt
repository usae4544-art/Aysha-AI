package com.aysha.assistant.wakeword

/**
 * Pure Kotlin wake-phrase matcher supporting "Hey Aysha", "Hello Aysha", "Wake up Aysha",
 * custom assistant names, and common Indian/English phonetic transcriptions from offline STT.
 */
object WakePhraseMatcher {

    private val defaultNameVariants = listOf(
        "aysha",
        "aisha",
        "ayesha",
        "asha",
        "asia",
        "isha",
        "eesha",
        "aaysha",
        "ayisha",
    )

    private val greetings = listOf("hey", "hello", "hi", "ok", "okay", "wake up", "wakeup")

    fun parseConfiguredPhrases(csv: String, assistantName: String = "AYSHA"): List<String> {
        val base = csv.split(",", "\n", ";")
            .map { normalize(it) }
            .filter { it.isNotBlank() }
        val cleanName = normalize(assistantName)
        val defaults = if (cleanName.isBlank() || cleanName == "aysha") {
            listOf("hey aysha", "hello aysha", "wake up aysha")
        } else {
            listOf("hey $cleanName", "hello $cleanName", "wake up $cleanName", "hey aysha")
        }
        return (base + defaults).distinct()
    }

    fun matchTranscript(
        transcript: String,
        configuredCsv: String = "Hey Aysha,Hello Aysha,Wake up Aysha",
        assistantName: String = "AYSHA",
    ): String? {
        val text = normalize(transcript)
        if (text.isBlank()) return null

        val configured = parseConfiguredPhrases(configuredCsv, assistantName)
        for (phrase in configured) {
            if (text == phrase || text.contains(phrase)) {
                return phrase
            }
        }

        val nameTokens = buildList {
            addAll(defaultNameVariants)
            val custom = normalize(assistantName)
            if (custom.isNotBlank()) add(custom)
        }.distinct()

        val words = text.split(" ").filter { it.isNotBlank() }
        val hasGreeting = greetings.any { g ->
            text.contains(g)
        }
        val matchedName = words.any { token ->
            nameTokens.any { target ->
                token == target || (token.length >= 4 && levenshtein(token, target) <= 1)
            }
        }
        if (hasGreeting && matchedName) {
            return configured.firstOrNull() ?: "hey aysha"
        }
        return null
    }

    fun normalize(raw: String): String =
        raw.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }
}
