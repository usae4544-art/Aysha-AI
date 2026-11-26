package com.aysha.assistant.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import androidx.core.content.ContextCompat
import com.aysha.assistant.core.SafeLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Lightweight speaker-mode barge-in monitor using VOICE_COMMUNICATION + hardware AEC/NS.
 * Lets the user naturally interrupt AYSHA mid-sentence without putting the device in call mode.
 */
class BargeInDetector(private val context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var monitorJob: Job? = null
    private var recorder: AudioRecord? = null
    private var aec: AcousticEchoCanceler? = null
    private var ns: NoiseSuppressor? = null

    @SuppressLint("MissingPermission")
    fun start(
        threshold: Float = 0.25f,
        onBargeInDetected: () -> Unit,
    ) {
        stop()
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        monitorJob = scope.launch {
            // Allow initial TTS buffer to start before arming barge-in to avoid onset echo spikes.
            delay(500L)
            if (!isActive) return@launch

            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            )
            if (minBuf <= 0) return@launch

            val ar = runCatching {
                AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(minBuf.coerceAtLeast(3200) * 2)
                    .build()
            }.getOrNull() ?: return@launch

            if (ar.state != AudioRecord.STATE_INITIALIZED) {
                runCatching { ar.release() }
                return@launch
            }
            recorder = ar
            if (AcousticEchoCanceler.isAvailable()) {
                aec = runCatching { AcousticEchoCanceler.create(ar.audioSessionId)?.apply { enabled = true } }.getOrNull()
            }
            if (NoiseSuppressor.isAvailable()) {
                ns = runCatching { NoiseSuppressor.create(ar.audioSessionId)?.apply { enabled = true } }.getOrNull()
            }

            if (runCatching { ar.startRecording() }.isFailure) {
                cleanup()
                return@launch
            }

            val buf = ByteArray(1920) // ~60ms window @ 16kHz PCM16
            var consecutiveHits = 0
            while (isActive) {
                val read = runCatching { ar.read(buf, 0, buf.size) }.getOrDefault(-1)
                if (read <= 0) {
                    delay(30L)
                    continue
                }
                val rms = computeRms(buf, read)
                if (rms >= threshold) {
                    consecutiveHits++
                    if (consecutiveHits >= 3) {
                        SafeLogger.d(TAG, "Barge-in triggered (rms=$rms)")
                        cleanup()
                        onBargeInDetected()
                        return@launch
                    }
                } else {
                    consecutiveHits = 0
                }
            }
            cleanup()
        }
    }

    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        cleanup()
    }

    private fun cleanup() {
        runCatching { aec?.release() }
        runCatching { ns?.release() }
        aec = null
        ns = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
    }

    private fun computeRms(pcm: ByteArray, len: Int): Float {
        if (len < 2) return 0f
        var sumSq = 0.0
        var count = 0
        var i = 0
        while (i + 1 < len) {
            val sample = ((pcm[i + 1].toInt() shl 8) or (pcm[i].toInt() and 0xFF)).toShort()
            val norm = sample / 32768.0
            sumSq += norm * norm
            count++
            i += 2
        }
        if (count == 0) return 0f
        return (sqrt(sumSq / count).toFloat() * 4.0f).coerceIn(0f, 1f)
    }

    companion object {
        private const val TAG = "AyshaBargeIn"
    }
}
