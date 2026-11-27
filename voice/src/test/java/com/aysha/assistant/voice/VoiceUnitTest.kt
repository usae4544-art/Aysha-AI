package com.aysha.assistant.voice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class VoiceUnitTest {

    @Test
    fun `speech formatter strips markdown and links`() {
        val cleaned = SpeechTextFormatter.cleanForSpeech("**Hello** _Aysha_! Check https://example.com #1")
        assertEquals("Hello Aysha ! Check link 1", cleaned)
    }

    @Test
    fun `speech chunking splits long text cleanly on boundaries`() {
        val longText = List(40) { "This is sentence number $it for testing chunking." }.joinToString(" ")
        val chunks = SpeechTextFormatter.splitChunks(longText, maxChars = 400)
        assertTrue(chunks.size > 2)
        assertTrue(chunks.all { it.length <= 420 })
    }

    @Test
    fun `gemini interactions tts payload includes real voice and audio l16 config`() {
        val json = GeminiTtsProtocol.buildInteractionsPayload(
            model = "gemini-3.8-flash-lite-tts",
            text = "Namaste friend!",
            voiceName = "Despina",
            style = "warm, natural pacing",
            stream = true,
        )
        assertEquals("gemini-3.8-flash-lite-tts", json.getString("model"))
        assertEquals(true, json.getBoolean("stream"))
        assertEquals("audio/l16", json.getJSONObject("response_format").getString("mime_type"))
        assertEquals(24000, json.getJSONObject("response_format").getInt("sample_rate"))
        val speechCfg = json.getJSONObject("generation_config").getJSONArray("speech_config")
        assertEquals("Despina", speechCfg.getJSONObject(0).getString("voice"))
    }

    @Test
    fun `gemini generateContent tts payload sets prebuiltVoiceConfig voiceName`() {
        val json = GeminiTtsProtocol.buildGenerateContentPayload(
            text = "Main Aysha hoon",
            voiceName = "Aoede",
        )
        val genCfg = json.getJSONObject("generationConfig")
        assertEquals("AUDIO", genCfg.getJSONArray("responseModalities").getString(0))
        val voiceName = genCfg
            .getJSONObject("speechConfig")
            .getJSONObject("voiceConfig")
            .getJSONObject("prebuiltVoiceConfig")
            .getString("voiceName")
        assertEquals("Aoede", voiceName)
    }

    @Test
    fun `unwrapWavIfPresent strips riff wave header and parses sample rate`() {
        val pcmSamples = byteArrayOf(10, 0, 20, 0, 30, 0, 40, 0)
        val wav = ByteBuffer.allocate(44 + pcmSamples.size).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + pcmSamples.size)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1.toShort())
            putShort(1.toShort())
            putInt(24000)
            putInt(48000)
            putShort(2.toShort())
            putShort(16.toShort())
            put("data".toByteArray())
            putInt(pcmSamples.size)
            put(pcmSamples)
        }.array()

        val unwrapped = GeminiTtsProtocol.unwrapWavIfPresent(wav)
        assertEquals(24000, unwrapped.sampleRate)
        assertArrayEquals(pcmSamples, unwrapped.pcmBytes)
    }
}
