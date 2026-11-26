package com.aysha.assistant.ai

import com.aysha.assistant.core.AssistantMood

data class ExtractedMemoryFact(
    val title: String,
    val content: String,
    val tags: String,
)

data class ExtractedReminderIntent(
    val minutesFromNow: Int,
    val clockTime24h: String,
    val tomorrow: Boolean,
    val message: String,
)

/**
 * Extracts conversational personal facts, persistent user rules ("ab se hamesha..."),
 * mood/energy shifts, and natural-language reminders in Hindi, English & Hinglish.
 */
object SmartMemoryExtractor {

    fun extractAutoFacts(rawText: String): List<ExtractedMemoryFact> {
        val text = rawText.trim()
        if (text.length < 6 || text.endsWith("?")) return emptyList()
        val lower = text.lowercase()
        val facts = mutableListOf<ExtractedMemoryFact>()

        // Name patterns
        Regex("(?:my name is|mera naam|main)\\s+([A-Za-z][A-Za-z\\s]{1,22}?)(?:\\s+hai|\\s+hoon|[.,!]|$)", RegexOption.IGNORE_CASE)
            .find(text)?.let { m ->
                val candidate = m.groupValues[1].trim()
                if (candidate.split(" ").size <= 2 && !lower.contains("kya") && !lower.contains("kaun")) {
                    facts.add(ExtractedMemoryFact("User Name", "User's name is $candidate", "profile,name"))
                }
            }

        // City / Location patterns
        Regex("(?:i live in|i am from|main)\\s+([A-Za-z][A-Za-z\\s]{2,24}?)\\s*(?:me rehta|me rehti|se hoon|mein rehta|mein rehti|$|[.,!])", RegexOption.IGNORE_CASE)
            .find(text)?.let { m ->
                val city = m.groupValues[1].trim()
                if (lower.contains("live in") || lower.contains("from") || lower.contains("rehta") || lower.contains("rehti") || lower.contains("se hoon")) {
                    facts.add(ExtractedMemoryFact("Location", "Lives in / from $city", "location,city"))
                }
            }

        // Likes / Preferences
        Regex("(?:i like|i love|my favorite is|mujhe|mera favorite)\\s+(.+?)(?:\\s+pasand hai|\\s+accha lagta|\\s+bohot pasand|[.,!]|$)", RegexOption.IGNORE_CASE)
            .find(text)?.let { m ->
                val item = m.groupValues[1].trim()
                if (item.length in 2..60 && (lower.contains("pasand") || lower.contains("i like") || lower.contains("i love") || lower.contains("favorite"))) {
                    facts.add(ExtractedMemoryFact("Preference", text, "preference,likes"))
                }
            }

        // Birthday / Exam / Goal / Meeting dates
        if (lower.contains("my birthday") || lower.contains("mera birthday") || lower.contains("my exam") || lower.contains("mera exam") || lower.contains("mera interview") || lower.contains("my meeting")) {
            facts.add(ExtractedMemoryFact("Important Date / Event", text, "event,schedule"))
        }

        // Explicit remember phrases
        Regex("^(?:yaad rakhna|yaad rakho|save this|note that|remember that|remember)\\s+(.+)$", RegexOption.IGNORE_CASE)
            .find(text)?.let { m ->
                val body = m.groupValues[1].trim()
                if (body.isNotBlank()) {
                    val title = body.split(Regex("\\s+")).take(3).joinToString(" ").replaceFirstChar { it.uppercase() }
                    facts.add(ExtractedMemoryFact(title, body, "personal,saved"))
                }
            }

        return facts.distinctBy { it.title to it.content }
    }

    /**
     * Extracts persistent behavioral rules like:
     * "Ab se hamesha chhote jawab dena", "Always call me boss", "Rule: keep answers short"
     */
    fun extractAutoRule(rawText: String): String? {
        val text = rawText.trim()
        if (text.length < 6) return null
        val patterns = listOf(
            Regex("^(?:ab se hamesha|aaj se hamesha|hamesha|always|from now on)\\s+(.+)$", RegexOption.IGNORE_CASE),
            Regex("^(?:rule:|new rule:|add rule:)\\s*(.+)$", RegexOption.IGNORE_CASE),
            Regex("^(?:call me|mujhe\\s+.*\\s+bolna|mujhe\\s+.*\\s+kaha karo)\\s*(.*)$", RegexOption.IGNORE_CASE),
        )
        for (p in patterns) {
            val m = p.find(text)
            if (m != null) {
                val extracted = if (p.pattern.startsWith("^(?:call me")) text else m.groupValues[1].trim()
                if (extracted.length >= 4 && !extracted.endsWith("?")) {
                    return extracted.replaceFirstChar { it.uppercase() }
                }
            }
        }
        return null
    }

    fun analyzeMoodShift(
        userText: String,
        currentMood: AssistantMood,
        currentEnergy: Int,
    ): Pair<AssistantMood, Int> {
        val lower = userText.lowercase()
        val warmWords = listOf("love", "pyaar", "thank", "shukriya", "awesome", "best", "cute", "sweet", "sorry", "maaf", "miss you", "good job", "mast", "shabash")
        val harshWords = listOf("stupid", "idiot", "bakwas", "bewakoof", "useless", "chup", "shut up", "pagal", "hate")

        return when {
            harshWords.any { lower.contains(it) } -> {
                val nextEnergy = (currentEnergy - 15).coerceAtLeast(35)
                val nextMood = if (currentMood == AssistantMood.ROOTHI) AssistantMood.NARAZ else AssistantMood.ROOTHI
                nextMood to nextEnergy
            }
            warmWords.any { lower.contains(it) } -> {
                val nextEnergy = (currentEnergy + 10).coerceAtMost(100)
                val nextMood = if (lower.contains("pyaar") || lower.contains("love") || lower.contains("miss you")) {
                    AssistantMood.PYAAR_BHARI
                } else {
                    AssistantMood.KHUSH
                }
                nextMood to nextEnergy
            }
            else -> currentMood to (currentEnergy + 1).coerceIn(40, 100)
        }
    }

    fun extractNaturalReminder(rawText: String): ExtractedReminderIntent? {
        val text = rawText.trim()
        val lower = text.lowercase()
        val looksLikeReminder = lower.contains("remind") ||
            lower.contains("reminder") ||
            lower.contains("yaad dila") ||
            lower.contains("yad dila") ||
            lower.contains("yaad dilana") ||
            lower.contains("yad delana") ||
            lower.contains("bhool na jaun")
        if (!looksLikeReminder) return null

        val tomorrow = lower.contains("kal ") || lower.startsWith("kal") || lower.contains("tomorrow")

        val relMatch = Regex("(\\d+)\\s*(min|minute|minutes|hr|hour|hours|ghante)\\b").find(lower)
        if (relMatch != null) {
            val qty = relMatch.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 5
            val unit = relMatch.groupValues[2]
            val mins = if (unit.startsWith("hr") || unit.startsWith("hour") || unit.startsWith("ghant")) qty * 60 else qty
            val cleaned = cleanReminderMessage(text)
            return ExtractedReminderIntent(
                minutesFromNow = mins,
                clockTime24h = "",
                tomorrow = false,
                message = cleaned,
            )
        }

        val clockMatch = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|baje)?").find(lower)
        if (clockMatch != null && (lower.contains("baje") || lower.contains("am") || lower.contains("pm") || lower.contains("at "))) {
            var h = clockMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 8
            val m = clockMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
            val amPm = clockMatch.groupValues[3]
            val isEvening = amPm == "pm" || lower.contains("shaam") || lower.contains("sham") || lower.contains("raat") || lower.contains("evening") || lower.contains("night")
            val isMorning = amPm == "am" || lower.contains("subah") || lower.contains("morning")
            if (isEvening && h < 12) h += 12
            if (isMorning && h == 12) h = 0
            val clock24 = String.format(java.util.Locale.US, "%02d:%02d", h, m)
            return ExtractedReminderIntent(
                minutesFromNow = 10,
                clockTime24h = clock24,
                tomorrow = tomorrow,
                message = cleanReminderMessage(text),
            )
        }

        return ExtractedReminderIntent(
            minutesFromNow = 10,
            clockTime24h = "",
            tomorrow = false,
            message = cleanReminderMessage(text),
        )
    }

    private fun cleanReminderMessage(raw: String): String {
        val stripped = raw
            .replace(Regex("(?i)(hey|hello|ok)?\\s*aysha[, ]*"), "")
            .replace(Regex("(?i)\\b(remind me|set a? ?reminder|create reminder|yaad dila\\s*(?:dena|na|o)?|yad dila\\s*(?:dena|na|o)?|yad delana|mujhe)\\b"), "")
            .replace(Regex("(?i)\\b(kal|tomorrow|aaj|today|subah|morning|shaam|sham|evening|raat|night)\\b"), "")
            .replace(Regex("(?i)\\b(in|at|after|par|pe|me|baad|ko|ke liye|to|that)\\b"), " ")
            .replace(Regex("(?i)\\b\\d{1,2}(:\\d{2})?\\s*(am|pm|baje|min|minute|minutes|hr|hour|hours|ghante)?\\b"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return stripped.ifBlank { "AYSHA Reminder" }
    }
}
