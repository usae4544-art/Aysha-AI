package com.aysha.assistant.memory

import com.aysha.assistant.core.ChatMessage
import com.aysha.assistant.core.ChatRole
import com.aysha.assistant.core.KnowledgeDocument
import com.aysha.assistant.core.MacroDefinition
import com.aysha.assistant.core.MacroStep
import com.aysha.assistant.core.MemoryEntry
import com.aysha.assistant.core.UserRule
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Semantic TF-IDF + Hinglish/English synonym + character trigram similarity matcher
 * inspired by Maya's on-device memory retrieval.
 */
object MemoryMatcher {

    private val synonymGroups = listOf(
        setOf("study", "padhai", "padhna", "exam", "homework", "pomodoro", "focus", "notes"),
        setOf("music", "gaana", "gana", "song", "songs", "beats", "lofi", "playlist"),
        setOf("friend", "dost", "bhai", "yaar", "buddy", "bestie"),
        setOf("home", "ghar", "address", "city", "shehar", "rehta", "rehti", "live"),
        setOf("work", "kaam", "office", "job", "project", "coding", "code", "dev"),
        setOf("food", "khana", "chai", "coffee", "breakfast", "lunch", "dinner", "pasand"),
        setOf("morning", "subah", "alarm", "wake", "uthna"),
        setOf("night", "raat", "sleep", "sona", "late"),
        setOf("birthday", "janamdin", "bday", "anniversary", "date"),
        setOf("call", "phone", "number", "contact", "mom", "maa", "papa", "dad"),
    )

    private val synonymLookup: Map<String, Set<String>> by lazy {
        val map = mutableMapOf<String, Set<String>>()
        for (group in synonymGroups) {
            for (word in group) {
                map[word] = group
            }
        }
        map
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^a-z0-9\\u0900-\\u097f]+"))
            .filter { it.length >= 2 }

    private fun expandTokens(tokens: List<String>): Set<String> {
        val out = LinkedHashSet<String>(tokens)
        for (t in tokens) {
            synonymLookup[t]?.let { out.addAll(it) }
        }
        return out
    }

    private fun trigrams(text: String): Set<String> {
        val clean = text.lowercase().replace(Regex("\\s+"), " ").trim()
        if (clean.length < 3) return if (clean.isEmpty()) emptySet() else setOf(clean)
        val out = HashSet<String>()
        for (i in 0..(clean.length - 3)) {
            out.add(clean.substring(i, i + 3))
        }
        return out
    }

    fun rank(entries: List<MemoryEntry>, query: String, limit: Int = 8): List<MemoryEntry> {
        val eligible = entries.filter { it.approvedByUser && !it.archived }
        val q = query.trim().lowercase()
        if (q.isEmpty()) return eligible.take(limit)

        val qTokens = tokenize(q)
        val qExpanded = expandTokens(qTokens)
        val qTrigrams = trigrams(q)
        val docCount = eligible.size.coerceAtLeast(1)

        // Document frequency across eligible memories for IDF weighting
        val df = HashMap<String, Int>()
        val docTokensList = eligible.map { e ->
            val toks = tokenize("${e.title} ${e.content} ${e.tags}")
            toks.distinct().forEach { df[it] = (df[it] ?: 0) + 1 }
            toks
        }

        return eligible
            .mapIndexed { idx, entry ->
                val title = entry.title.lowercase()
                val content = entry.content.lowercase()
                val tags = entry.tags.lowercase()
                val docToks = docTokensList[idx]
                val docSet = docToks.toSet()

                var score = 0.0
                if (title.contains(q)) score += 14.0
                if (content.contains(q)) score += 9.0
                if (tags.contains(q)) score += 7.0

                // TF-IDF cosine-like overlap with synonym expansion
                var dot = 0.0
                var normQ = 0.0
                var normD = 0.0
                for (tok in qExpanded) {
                    val isDirect = tok in qTokens
                    val qWeight = if (isDirect) 1.0 else 0.55
                    val idf = 1.0 + ln((docCount + 1.0) / ((df[tok] ?: 0) + 1.0))
                    val qw = qWeight * idf
                    normQ += qw * qw
                    val tf = docToks.count { it == tok }.toDouble()
                    if (tf > 0.0) {
                        dot += qw * tf * idf
                    }
                }
                for (tok in docSet) {
                    val idf = 1.0 + ln((docCount + 1.0) / ((df[tok] ?: 0) + 1.0))
                    val tf = docToks.count { it == tok }.toDouble()
                    normD += (tf * idf) * (tf * idf)
                }
                val cosine = if (normQ > 0.0 && normD > 0.0) dot / (sqrt(normQ) * sqrt(normD)) else 0.0
                score += cosine * 12.0

                for (tok in qTokens) {
                    if (title.contains(tok)) score += 3.5
                    if (content.contains(tok)) score += 1.8
                    if (tags.contains(tok)) score += 2.0
                }

                // Fuzzy trigram fallback for minor typos / Hinglish spelling variations
                if (qTrigrams.isNotEmpty()) {
                    val dTri = trigrams("$title $content $tags")
                    val inter = qTrigrams.count { it in dTri }
                    val union = (qTrigrams.size + dTri.size - inter).coerceAtLeast(1)
                    val jaccard = inter.toDouble() / union
                    if (jaccard > 0.08) {
                        score += jaccard * 6.0
                    }
                }

                entry to score
            }
            .filter { it.second > 0.35 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    fun chunkDocument(content: String, maxChunkChars: Int = 420): List<String> {
        val clean = content.trim()
        if (clean.isEmpty()) return emptyList()
        val paragraphs = clean.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        for (p in paragraphs) {
            if (current.length + p.length + 2 <= maxChunkChars) {
                if (current.isNotEmpty()) current.append("\n")
                current.append(p)
            } else {
                if (current.isNotEmpty()) {
                    chunks.add(current.toString())
                    current.clear()
                }
                if (p.length <= maxChunkChars) {
                    current.append(p)
                } else {
                    var idx = 0
                    while (idx < p.length) {
                        val end = (idx + maxChunkChars).coerceAtMost(p.length)
                        chunks.add(p.substring(idx, end).trim())
                        idx = end
                    }
                }
            }
        }
        if (current.isNotEmpty()) chunks.add(current.toString())
        return chunks
    }

    fun searchDocuments(docs: List<KnowledgeDocument>, query: String, limit: Int = 3): List<String> {
        val qTokens = tokenize(query)
        if (qTokens.isEmpty()) return emptyList()
        val expanded = expandTokens(qTokens)
        val scored = mutableListOf<Pair<String, Int>>()
        for (doc in docs) {
            for (chunk in chunkDocument(doc.content)) {
                val lower = chunk.lowercase()
                val hits = expanded.count { lower.contains(it) }
                if (hits > 0) {
                    scored.add("[${doc.title}] $chunk" to hits)
                }
            }
        }
        return scored.sortedByDescending { it.second }.take(limit).map { it.first }
    }
}

object ChatExporter {

    fun toMarkdown(messages: List<ChatMessage>, assistantName: String = "AYSHA"): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            appendLine("# $assistantName Conversation Export")
            appendLine()
            for (msg in messages) {
                val speaker = when (msg.role) {
                    ChatRole.USER -> "You"
                    ChatRole.ASSISTANT -> assistantName
                    ChatRole.SYSTEM -> "System"
                }
                val time = fmt.format(Date(msg.timestampMs))
                val tag = if (msg.providerTag.isNotBlank()) " (${msg.providerTag})" else ""
                appendLine("**$speaker$tag** · $time")
                appendLine(msg.text)
                appendLine()
            }
        }.trim()
    }

    fun toJson(messages: List<ChatMessage>, memories: List<MemoryEntry>): String {
        val msgArr = JSONArray()
        for (m in messages) {
            msgArr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("role", m.role.name)
                    put("text", m.text)
                    put("providerTag", m.providerTag)
                    put("timestampMs", m.timestampMs)
                },
            )
        }
        val memArr = JSONArray()
        for (mem in memories) {
            memArr.put(
                JSONObject().apply {
                    put("id", mem.id)
                    put("title", mem.title)
                    put("content", mem.content)
                    put("tags", mem.tags)
                    put("source", mem.source)
                    put("archived", mem.archived)
                    put("approvedByUser", mem.approvedByUser)
                    put("updatedAtMs", mem.updatedAtMs)
                },
            )
        }
        return JSONObject().apply {
            put("app", "AYSHA – Your Personal AI Assistant")
            put("exportedAtMs", System.currentTimeMillis())
            put("messages", msgArr)
            put("memories", memArr)
        }.toString(2)
    }
}

data class FullBackupPayload(
    val settingsJson: JSONObject?,
    val memories: List<MemoryEntry>,
    val rules: List<UserRule>,
    val documents: List<KnowledgeDocument>,
    val customMacros: List<MacroDefinition>,
)

object BackupRestoreHelper {

    fun buildBackupJson(
        settingsJson: String,
        memories: List<MemoryEntry>,
        rules: List<UserRule>,
        documents: List<KnowledgeDocument>,
        macros: List<MacroDefinition>,
    ): String {
        val settingsObj = runCatching { JSONObject(settingsJson) }.getOrElse { JSONObject() }
        val memArr = JSONArray().apply {
            memories.forEach { m ->
                put(
                    JSONObject()
                        .put("title", m.title)
                        .put("content", m.content)
                        .put("tags", m.tags)
                        .put("source", m.source)
                        .put("archived", m.archived)
                        .put("approvedByUser", m.approvedByUser),
                )
            }
        }
        val ruleArr = JSONArray().apply {
            rules.forEach { r ->
                put(JSONObject().put("text", r.text).put("enabled", r.enabled))
            }
        }
        val docArr = JSONArray().apply {
            documents.forEach { d ->
                put(JSONObject().put("title", d.title).put("content", d.content))
            }
        }
        val macroArr = JSONArray().apply {
            macros.filter { !it.isBuiltIn }.forEach { macro ->
                val stepsArr = JSONArray().apply {
                    macro.steps.forEach { s ->
                        put(
                            JSONObject()
                                .put("tool", s.tool)
                                .put("optional", s.optional)
                                .put("args", JSONObject().apply { s.args.forEach { (k, v) -> put(k, v) } }),
                        )
                    }
                }
                put(
                    JSONObject()
                        .put("name", macro.name)
                        .put("triggerPhrase", macro.triggerPhrase)
                        .put("intent", macro.intent)
                        .put("steps", stepsArr),
                )
            }
        }

        return JSONObject().apply {
            put("format", "aysha-backup-v2")
            put("exportedAtMs", System.currentTimeMillis())
            put("apiKeysIncluded", false)
            put("settings", settingsObj)
            put("memories", memArr)
            put("rules", ruleArr)
            put("documents", docArr)
            put("macros", macroArr)
        }.toString(2)
    }

    fun parseBackupJson(raw: String): Result<FullBackupPayload> = runCatching {
        val root = JSONObject(raw.trim())
        val settingsObj = root.optJSONObject("settings")

        val memories = mutableListOf<MemoryEntry>()
        val memArr = root.optJSONArray("memories") ?: JSONArray()
        for (i in 0 until memArr.length()) {
            val o = memArr.optJSONObject(i) ?: continue
            val title = o.optString("title").trim()
            val content = o.optString("content").trim()
            if (title.isNotEmpty() && content.isNotEmpty()) {
                memories.add(
                    MemoryEntry(
                        title = title,
                        content = content,
                        tags = o.optString("tags"),
                        source = o.optString("source", "backup restore"),
                        archived = o.optBoolean("archived", false),
                        approvedByUser = o.optBoolean("approvedByUser", true),
                    ),
                )
            }
        }

        val rules = mutableListOf<UserRule>()
        val ruleArr = root.optJSONArray("rules") ?: JSONArray()
        for (i in 0 until ruleArr.length()) {
            val o = ruleArr.optJSONObject(i) ?: continue
            val text = o.optString("text").trim()
            if (text.isNotEmpty()) {
                rules.add(UserRule(text = text, enabled = o.optBoolean("enabled", true)))
            }
        }

        val docs = mutableListOf<KnowledgeDocument>()
        val docArr = root.optJSONArray("documents") ?: JSONArray()
        for (i in 0 until docArr.length()) {
            val o = docArr.optJSONObject(i) ?: continue
            val title = o.optString("title").trim()
            val content = o.optString("content").trim()
            if (title.isNotEmpty() && content.isNotEmpty()) {
                docs.add(
                    KnowledgeDocument(
                        title = title,
                        content = content,
                        chunkCount = MemoryMatcher.chunkDocument(content).size.coerceAtLeast(1),
                    ),
                )
            }
        }

        val macros = mutableListOf<MacroDefinition>()
        val macroArr = root.optJSONArray("macros") ?: JSONArray()
        for (i in 0 until macroArr.length()) {
            val o = macroArr.optJSONObject(i) ?: continue
            val name = o.optString("name").trim()
            if (name.isEmpty()) continue
            val stepsArr = o.optJSONArray("steps") ?: JSONArray()
            val steps = (0 until stepsArr.length()).mapNotNull { idx ->
                val so = stepsArr.optJSONObject(idx) ?: return@mapNotNull null
                val tool = so.optString("tool")
                if (tool.isBlank()) return@mapNotNull null
                val argsObj = so.optJSONObject("args") ?: JSONObject()
                val argsMap = mutableMapOf<String, String>()
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    argsMap[k] = argsObj.opt(k)?.toString().orEmpty()
                }
                MacroStep(tool = tool, args = argsMap, optional = so.optBoolean("optional", false))
            }
            macros.add(
                MacroDefinition(
                    name = name,
                    triggerPhrase = o.optString("triggerPhrase"),
                    intent = o.optString("intent", "Restored macro"),
                    steps = steps,
                    isBuiltIn = false,
                ),
            )
        }

        FullBackupPayload(settingsObj, memories, rules, docs, macros)
    }
}
