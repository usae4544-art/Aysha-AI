package com.aysha.assistant.voice

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.PlaybackParams
import android.util.Base64
import com.aysha.assistant.core.SafeLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.sqrt

data class PcmPayload(
    val pcmBytes: ByteArray,
    val sampleRate: Int,
)

/**
 * Pure REST + AudioTrack payload helpers & audio framing math (unit-testable on JVM).
 */
object GeminiTtsProtocol {

    private val RATE_REGEX = Regex("rate=(\\d+)", RegexOption.IGNORE_CASE)

    fun sanitizeKey(raw: String): String = raw.trim().replace(Regex("\\s+"), "")

    fun buildStyleDirective(toneHint: String, rate: Float): String {
        val paceCue = when {
            rate >= 1.35f -> "speaking briskly and lively"
            rate >= 1.14f -> "speaking at an upbeat pace"
            rate <= 0.78f -> "speaking slowly and clearly"
            rate <= 0.90f -> "speaking unhurriedly and gently"
            else -> "natural conversational pacing"
        }
        return if (toneHint.isBlank()) paceCue else "${toneHint.trim().trimEnd('.')}, $paceCue"
    }

    fun buildInteractionsPayload(
        model: String,
        text: String,
        voiceName: String,
        style: String,
        stream: Boolean = true,
    ): JSONObject {
        val textNode = JSONObject()
            .put("type", "text")
            .put("text", text)
            .apply {
                if (style.isNotBlank()) {
                    put(
                        "annotations",
                        JSONArray().put(
                            JSONObject()
                                .put("type", "speech_metadata")
                                .put("style", style),
                        ),
                    )
                }
            }

        return JSONObject()
            .put("model", model)
            .put(
                "input",
                JSONArray().put(
                    JSONObject()
                        .put("type", "user_input")
                        .put("content", JSONArray().put(textNode)),
                ),
            )
            .put(
                "response_format",
                JSONObject()
                    .put("type", "audio")
                    .put("mime_type", "audio/l16")
                    .put("sample_rate", 24000),
            )
            .put(
                "generation_config",
                JSONObject().put(
                    "speech_config",
                    JSONArray().put(JSONObject().put("voice", voiceName)),
                ),
            )
            .put("stream", stream)
    }

    fun buildGenerateContentPayload(
        text: String,
        voiceName: String,
    ): JSONObject {
        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", text)),
                    ),
                ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put(
                                "prebuiltVoiceConfig",
                                JSONObject().put("voiceName", voiceName),
                            ),
                        ),
                    ),
            )
    }

    fun extractSampleRate(mimeType: String?, defaultRate: Int = 24000): Int {
        if (mimeType.isNullOrBlank()) return defaultRate
        val m = RATE_REGEX.find(mimeType)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return if (m != null && m in 8000..48000) m else defaultRate
    }

    fun unwrapWavIfPresent(bytes: ByteArray, defaultSampleRate: Int = 24000): PcmPayload {
        if (bytes.size >= 44 &&
            bytes[0] == 'R'.code.toByte() &&
            bytes[1] == 'I'.code.toByte() &&
            bytes[2] == 'F'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[8] == 'W'.code.toByte() &&
            bytes[9] == 'A'.code.toByte() &&
            bytes[10] == 'V'.code.toByte() &&
            bytes[11] == 'E'.code.toByte()
        ) {
            var rate = defaultSampleRate
            var offset = 12
            while (offset + 8 <= bytes.size) {
                val id0 = bytes[offset].toInt().toChar()
                val id1 = bytes[offset + 1].toInt().toChar()
                val id2 = bytes[offset + 2].toInt().toChar()
                val id3 = bytes[offset + 3].toInt().toChar()
                val chunkId = "$id0$id1$id2$id3"
                val chunkSize = ByteBuffer.wrap(bytes, offset + 4, 4)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int
                val payloadStart = offset + 8
                if (chunkId == "fmt " && chunkSize >= 16 && payloadStart + 8 <= bytes.size) {
                    val parsedRate = ByteBuffer.wrap(bytes, payloadStart + 4, 4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .int
                    if (parsedRate in 8000..48000) {
                        rate = parsedRate
                    }
                } else if (chunkId == "data" && chunkSize >= 0) {
                    val end = (payloadStart + chunkSize).coerceAtMost(bytes.size)
                    if (payloadStart <= end) {
                        return PcmPayload(bytes.copyOfRange(payloadStart, end), rate)
                    }
                }
                if (chunkSize < 0) break
                val padded = chunkSize + (chunkSize and 1)
                offset = payloadStart + padded
            }
            return PcmPayload(bytes.copyOfRange(44, bytes.size), rate)
        }
        return PcmPayload(bytes, defaultSampleRate)
    }

    fun applyFadeIn(pcm16Le: ByteArray, fadeSamples: Int = 240) {
        val availableSamples = pcm16Le.size / 2
        val count = fadeSamples.coerceAtMost(availableSamples)
        if (count <= 1) return
        for (i in 0 until count) {
            val idx = i * 2
            val lo = pcm16Le[idx].toInt() and 0xFF
            val hi = pcm16Le[idx + 1].toInt()
            val sample = ((hi shl 8) or lo).toShort().toInt()
            val scaled = (sample * i) / count
            pcm16Le[idx] = (scaled and 0xFF).toByte()
            pcm16Le[idx + 1] = ((scaled shr 8) and 0xFF).toByte()
        }
    }

    fun applyFadeOut(pcm16Le: ByteArray, fadeSamples: Int = 240) {
        val availableSamples = pcm16Le.size / 2
        val count = fadeSamples.coerceAtMost(availableSamples)
        if (count <= 1) return
        val startSample = availableSamples - count
        for (i in 0 until count) {
            val sampleIdx = startSample + i
            val idx = sampleIdx * 2
            val lo = pcm16Le[idx].toInt() and 0xFF
            val hi = pcm16Le[idx + 1].toInt()
            val sample = ((hi shl 8) or lo).toShort().toInt()
            val gain = (count - 1 - i).toFloat() / count.toFloat()
            val scaled = (sample * gain).toInt()
            pcm16Le[idx] = (scaled and 0xFF).toByte()
            pcm16Le[idx + 1] = ((scaled shr 8) and 0xFF).toByte()
        }
    }

    fun computeRms(pcm16Le: ByteArray, len: Int = pcm16Le.size): Float {
        if (len < 2) return 0f
        var sumSq = 0.0
        var count = 0
        var i = 0
        while (i + 1 < len) {
            val sample = ((pcm16Le[i + 1].toInt() shl 8) or (pcm16Le[i].toInt() and 0xFF)).toShort()
            val norm = sample / 32768.0
            sumSq += norm * norm
            count++
            i += 2
        }
        if (count == 0) return 0f
        return (sqrt(sumSq / count).toFloat() * 4.5f).coerceIn(0f, 1f)
    }
}

/**
 * Pure Gemini Neural Text-to-Speech engine using streaming Interactions API & generateContent Audio fallback.
 * Plays 24kHz HD PCM audio via hardware AudioTrack — zero dependency on Android Google TextToSpeech.
 */
class GeminiNeuralTtsEngine {

    private enum class TtsBackend(val model: String, val isInteractions: Boolean) {
        INTERACTIONS_LITE("gemini-3.8-flash-lite-tts", true),
        INTERACTIONS_FLASH("gemini-3.8-flash-tts", true),
        GENERATE_CONTENT_25_FLASH("gemini-2.5-flash-preview-tts", false),
        GENERATE_CONTENT_31_FLASH("gemini-3.1-flash-tts-preview", false),
        GENERATE_CONTENT_25_PRO("gemini-2.5-pro-preview-tts", false),
    }

    private val http = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(4, 5, TimeUnit.MINUTES))
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cachedBackend: TtsBackend? = null

    private var activeJob: Job? = null
    private val activeCall = AtomicReference<Call?>(null)
    private val activeTrack = AtomicReference<AudioTrack?>(null)

    fun speak(
        apiKey: String,
        chunks: List<String>,
        voiceName: String,
        styleHint: String,
        rate: Float,
        volume: Float,
        onStarted: () -> Unit,
        onRms: (Float) -> Unit,
        onFinished: () -> Unit,
        onError: (String) -> Unit,
    ) {
        stop()
        val cleanKey = GeminiTtsProtocol.sanitizeKey(apiKey)
        if (cleanKey.isEmpty()) {
            onError("Gemini API Key missing hai — Settings → AI Provider Settings me Gemini key daalo.")
            return
        }
        val cleanVoice = voiceName.trim().ifBlank { "Aoede" }
        val safeRate = rate.coerceIn(0.6f, 1.8f)
        val safeVolume = volume.coerceIn(0.05f, 1.0f)

        activeJob = scope.launch {
            var notifiedStart = false
            try {
                for (chunk in chunks) {
                    ensureActive()
                    val ok = playChunkWithCascade(
                        apiKey = cleanKey,
                        text = chunk,
                        voiceName = cleanVoice,
                        style = styleHint,
                        rate = safeRate,
                        volume = safeVolume,
                        onAudioStarted = {
                            if (!notifiedStart) {
                                notifiedStart = true
                                onStarted()
                            }
                        },
                        onRms = onRms,
                    )
                    if (!ok) {
                        throw IllegalStateException("Gemini Neural TTS audio response nahi aaya.")
                    }
                }
                onRms(0f)
                onFinished()
            } catch (_: CancellationException) {
                onRms(0f)
            } catch (err: Throwable) {
                SafeLogger.w(TAG, "Gemini TTS failed: ${err.message}")
                onRms(0f)
                onError(SafeLogger.redact(err.message ?: "Gemini Voice failed"))
            } finally {
                releaseActiveAudio()
            }
        }
    }

    fun stop() {
        activeJob?.cancel()
        activeJob = null
        activeCall.getAndSet(null)?.let { runCatching { it.cancel() } }
        releaseActiveAudio()
    }

    private suspend fun playChunkWithCascade(
        apiKey: String,
        text: String,
        voiceName: String,
        style: String,
        rate: Float,
        volume: Float,
        onAudioStarted: () -> Unit,
        onRms: (Float) -> Unit,
    ): Boolean {
        val candidates = buildList {
            cachedBackend?.let { add(it) }
            TtsBackend.entries.forEach { if (it !in this) add(it) }
        }

        var lastError: Throwable? = null
        for (backend in candidates) {
            currentCoroutineContext().ensureActive()
            val result = runCatching {
                if (backend.isInteractions) {
                    streamInteractionsTts(
                        apiKey = apiKey,
                        model = backend.model,
                        text = text,
                        voiceName = voiceName,
                        style = style,
                        rate = rate,
                        volume = volume,
                        onAudioStarted = onAudioStarted,
                        onRms = onRms,
                    )
                } else {
                    playGenerateContentTts(
                        apiKey = apiKey,
                        model = backend.model,
                        text = text,
                        voiceName = voiceName,
                        rate = rate,
                        volume = volume,
                        onAudioStarted = onAudioStarted,
                        onRms = onRms,
                    )
                }
            }
            if (result.isSuccess && result.getOrNull() == true) {
                cachedBackend = backend
                return true
            }
            val err = result.exceptionOrNull()
            if (err is CancellationException) throw err
            if (err is AuthOrFatalApiException) throw err
            if (cachedBackend == backend) cachedBackend = null
            lastError = err
            SafeLogger.d(TAG, "TTS backend ${backend.model} skipped: ${err?.message}")
        }
        if (lastError != null) throw lastError
        return false
    }

    private suspend fun streamInteractionsTts(
        apiKey: String,
        model: String,
        text: String,
        voiceName: String,
        style: String,
        rate: Float,
        volume: Float,
        onAudioStarted: () -> Unit,
        onRms: (Float) -> Unit,
    ): Boolean {
        val payload = GeminiTtsProtocol.buildInteractionsPayload(
            model = model,
            text = text,
            voiceName = voiceName,
            style = style,
            stream = true,
        )
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/interactions")
            .header("x-goog-api-key", apiKey)
            .header("Accept", "text/event-stream")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()

        val call = http.newCall(req)
        activeCall.set(call)

        var track: AudioTrack? = null
        val preBuffer = ByteArrayOutputStream()
        var trackStarted = false
        var sampleRate = 24000
        var audioBytesWritten = 0L

        try {
            call.execute().use { resp ->
                if (!resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    val msg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                        ?: body.take(180)
                    if (resp.code == 401 || resp.code == 403 || (resp.code == 400 && msg.contains("API key", ignoreCase = true))) {
                        throw AuthOrFatalApiException("Gemini Auth Error (${resp.code}): $msg")
                    }
                    throw IllegalStateException("Interactions HTTP ${resp.code}: $msg")
                }

                val source = resp.body?.source() ?: return false
                while (!source.exhausted()) {
                    currentCoroutineContext().ensureActive()
                    val line = source.readUtf8Line() ?: break
                    if (!line.startsWith("data:")) continue
                    val jsonStr = line.removePrefix("data:").trim()
                    if (jsonStr.isEmpty() || jsonStr == "[DONE]") continue
                    val evt = runCatching { JSONObject(jsonStr) }.getOrNull() ?: continue
                    val eventType = evt.optString("event_type")
                    when (eventType) {
                        "error" -> {
                            val errMsg = evt.optJSONObject("error")?.optString("message").orEmpty().ifBlank { "TTS stream error" }
                            if (audioBytesWritten == 0L) {
                                throw IllegalStateException(errMsg)
                            }
                        }
                        "step.delta" -> {
                            val delta = evt.optJSONObject("delta") ?: continue
                            if (delta.optString("type") == "audio") {
                                val b64 = delta.optString("data")
                                if (b64.isEmpty()) continue
                                val raw = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull() ?: continue
                                if (raw.isEmpty()) continue
                                val mime = delta.optString("mime_type")
                                val unwrapped = if (audioBytesWritten == 0L && preBuffer.size() == 0) {
                                    val parsedRate = GeminiTtsProtocol.extractSampleRate(mime, 24000)
                                    GeminiTtsProtocol.unwrapWavIfPresent(raw, parsedRate)
                                } else {
                                    PcmPayload(raw, sampleRate)
                                }
                                sampleRate = unwrapped.sampleRate
                                if (unwrapped.pcmBytes.isEmpty()) continue

                                if (!trackStarted) {
                                    preBuffer.write(unwrapped.pcmBytes)
                                    // Wait until ~100ms (4800 bytes @ 24kHz mono 16-bit) before starting hardware track -> zero underrun clicks
                                    if (preBuffer.size() >= 4800) {
                                        val firstBuf = preBuffer.toByteArray()
                                        preBuffer.reset()
                                        GeminiTtsProtocol.applyFadeIn(firstBuf)
                                        val created = createOutputTrack(sampleRate, volume, rate)
                                            ?: throw IllegalStateException("AudioTrack unavailable.")
                                        track = created
                                        activeTrack.set(created)
                                        onAudioStarted()
                                        created.play()
                                        trackStarted = true
                                        writeAlignedPcm(created, firstBuf, onRms)
                                        audioBytesWritten += firstBuf.size
                                    }
                                } else {
                                    writeAlignedPcm(track, unwrapped.pcmBytes, onRms)
                                    audioBytesWritten += unwrapped.pcmBytes.size
                                }
                            }
                        }
                    }
                }
            }

            if (!trackStarted && preBuffer.size() > 0) {
                val remainder = preBuffer.toByteArray()
                GeminiTtsProtocol.applyFadeIn(remainder)
                GeminiTtsProtocol.applyFadeOut(remainder)
                val created = createOutputTrack(sampleRate, volume, rate)
                    ?: throw IllegalStateException("AudioTrack unavailable.")
                track = created
                activeTrack.set(created)
                onAudioStarted()
                created.play()
                trackStarted = true
                writeAlignedPcm(created, remainder, onRms)
                audioBytesWritten += remainder.size
            }

            if (trackStarted && track != null) {
                val silence = ByteArray(960)
                runCatching {
                    track.write(silence, 0, silence.size)
                    audioBytesWritten += silence.size
                }
                awaitTrackDrain(track, audioBytesWritten / 2L, sampleRate)
            }
            return audioBytesWritten > 0L
        } finally {
            activeCall.compareAndSet(call, null)
            releaseTrack(track)
        }
    }

    private suspend fun playGenerateContentTts(
        apiKey: String,
        model: String,
        text: String,
        voiceName: String,
        rate: Float,
        volume: Float,
        onAudioStarted: () -> Unit,
        onRms: (Float) -> Unit,
    ): Boolean {
        val payload = GeminiTtsProtocol.buildGenerateContentPayload(text = text, voiceName = voiceName)
        val req = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            .post(payload.toString().toRequestBody(jsonMedia))
            .build()

        val call = http.newCall(req)
        activeCall.set(call)
        var track: AudioTrack? = null

        try {
            val audioChunks = mutableListOf<PcmPayload>()
            var totalBytesWritten = 0L
            call.execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                        ?: body.take(180)
                    if (resp.code == 401 || resp.code == 403 || (resp.code == 400 && msg.contains("API key", ignoreCase = true))) {
                        throw AuthOrFatalApiException("Gemini Auth Error (${resp.code}): $msg")
                    }
                    throw IllegalStateException("Gemini TTS HTTP ${resp.code}: $msg")
                }
                val root = JSONObject(body)
                val parts = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?: JSONArray()
                for (i in 0 until parts.length()) {
                    val inline = parts.optJSONObject(i)?.optJSONObject("inlineData") ?: continue
                    val b64 = inline.optString("data")
                    if (b64.isBlank()) continue
                    val decoded = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull() ?: continue
                    val mime = inline.optString("mimeType")
                    val rateHint = GeminiTtsProtocol.extractSampleRate(mime, 24000)
                    val unwrapped = GeminiTtsProtocol.unwrapWavIfPresent(decoded, rateHint)
                    if (unwrapped.pcmBytes.isNotEmpty()) {
                        audioChunks.add(unwrapped)
                    }
                }
            }

            if (audioChunks.isEmpty()) return false
            val sampleRate = audioChunks.first().sampleRate
            track = createOutputTrack(sampleRate, volume, rate) ?: return false
            activeTrack.set(track)
            onAudioStarted()
            track.play()

            audioChunks.forEachIndexed { idx, chunk ->
                currentCoroutineContext().ensureActive()
                val pcm = chunk.pcmBytes
                if (idx == 0) GeminiTtsProtocol.applyFadeIn(pcm)
                if (idx == audioChunks.lastIndex) GeminiTtsProtocol.applyFadeOut(pcm)
                val sliceSize = 4800
                var offset = 0
                while (offset < pcm.size) {
                    currentCoroutineContext().ensureActive()
                    val count = (pcm.size - offset).coerceAtMost(sliceSize) and 0x7FFFFFFE
                    if (count <= 0) break
                    val slice = pcm.copyOfRange(offset, offset + count)
                    onRms(GeminiTtsProtocol.computeRms(slice, count))
                    val written = runCatching { track.write(slice, 0, count) }.getOrDefault(-1)
                    if (written < 0) break
                    totalBytesWritten += written
                    offset += count
                }
            }

            val silence = ByteArray(960)
            runCatching {
                track.write(silence, 0, silence.size)
                totalBytesWritten += silence.size
            }
            awaitTrackDrain(track, totalBytesWritten / 2L, sampleRate)
            return true
        } finally {
            activeCall.compareAndSet(call, null)
            releaseTrack(track)
        }
    }

    private fun writeAlignedPcm(track: AudioTrack?, bytes: ByteArray, onRms: (Float) -> Unit) {
        if (track == null || bytes.size < 2) return
        val alignedLen = bytes.size and 0x7FFFFFFE
        if (alignedLen <= 0) return
        onRms(GeminiTtsProtocol.computeRms(bytes, alignedLen))
        runCatching { track.write(bytes, 0, alignedLen) }
    }

    private fun createOutputTrack(sampleRate: Int, volume: Float, rate: Float): AudioTrack? {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(9600)

        val track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(minBuf * 3)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        }.getOrNull()

        if (track == null || track.state != AudioTrack.STATE_INITIALIZED) {
            runCatching { track?.release() }
            return null
        }
        runCatching { track.setVolume(volume.coerceIn(0.05f, 1.0f)) }
        val clampedSpeed = rate.coerceIn(0.8f, 1.35f)
        if (kotlin.math.abs(clampedSpeed - 1.0f) >= 0.04f) {
            runCatching {
                track.playbackParams = PlaybackParams().setSpeed(clampedSpeed)
            }
        }
        return track
    }

    private suspend fun awaitTrackDrain(track: AudioTrack, totalFrames: Long, sampleRate: Int) {
        if (totalFrames <= 0L) return
        val maxWaitMs = ((totalFrames * 1000L) / sampleRate.coerceAtLeast(8000) + 650L).coerceAtMost(8000L)
        val deadline = System.currentTimeMillis() + maxWaitMs
        var stagnantPolls = 0
        var lastHead = -1L
        while (currentCoroutineContext().isActive) {
            val head = runCatching { track.playbackHeadPosition.toLong() and 0xFFFFFFFFL }.getOrDefault(totalFrames)
            if (head >= totalFrames - 240L || System.currentTimeMillis() >= deadline) break
            if (head == lastHead) {
                stagnantPolls++
                if (stagnantPolls > 12) break
            } else {
                stagnantPolls = 0
                lastHead = head
            }
            kotlinx.coroutines.delay(25L)
        }
        runCatching { track.stop() }
    }

    private fun releaseTrack(track: AudioTrack?) {
        if (track == null) return
        activeTrack.compareAndSet(track, null)
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
                track.flush()
            }
            track.release()
        }
    }

    private fun releaseActiveAudio() {
        val track = activeTrack.getAndSet(null) ?: return
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.pause()
                track.flush()
            }
            track.release()
        }
    }

    private class AuthOrFatalApiException(message: String) : IllegalStateException(message)

    companion object {
        private const val TAG = "AyshaGeminiNeuralTts"
    }
}
