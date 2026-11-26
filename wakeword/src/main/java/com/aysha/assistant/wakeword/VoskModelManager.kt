package com.aysha.assistant.wakeword

import android.content.Context
import android.content.res.AssetManager
import com.aysha.assistant.core.SafeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

/**
 * Prepares the offline Vosk Indian-English acoustic model from bundled assets (primary, 100% offline)
 * or official Alpha Cephei release zip if asset folder was omitted in a minimal local build.
 */
class VoskModelManager(private val context: Context) {

    private val modelDir = File(context.filesDir, "aysha_vosk_en_in")
    private val readyMarker = File(modelDir, ".model_ready_v1")

    fun isModelReady(): Boolean = readyMarker.exists() && File(modelDir, "am/final.mdl").exists()

    fun getModelPath(): String = modelDir.absolutePath

    suspend fun ensureModelPrepared(): Result<String> = withContext(Dispatchers.IO) {
        if (isModelReady()) {
            return@withContext Result.success(modelDir.absolutePath)
        }
        runCatching {
            if (hasAssetModel(context.assets)) {
                SafeLogger.d(TAG, "Unpacking bundled local Vosk model from APK assets")
                modelDir.deleteRecursively()
                modelDir.mkdirs()
                copyAssetTree(context.assets, ASSET_MODEL_DIR, modelDir)
            } else {
                SafeLogger.d(TAG, "Bundled model absent; fetching open-source Vosk small-en-in model")
                downloadAndExtractFallback()
            }
            if (!File(modelDir, "am/final.mdl").exists()) {
                throw IllegalStateException("Vosk acoustic model files incomplete after extraction")
            }
            readyMarker.writeText("ok")
            modelDir.absolutePath
        }
    }

    private fun hasAssetModel(assets: AssetManager): Boolean {
        val entries = runCatching { assets.list(ASSET_MODEL_DIR) }.getOrNull() ?: return false
        return entries.contains("am") && entries.contains("conf")
    }

    private fun copyAssetTree(assets: AssetManager, assetPath: String, targetDir: File) {
        val children = assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            targetDir.parentFile?.mkdirs()
            assets.open(assetPath).use { input ->
                FileOutputStream(targetDir).use { output ->
                    input.copyTo(output, bufferSize = 16 * 1024)
                }
            }
        } else {
            targetDir.mkdirs()
            for (child in children) {
                copyAssetTree(assets, "$assetPath/$child", File(targetDir, child))
            }
        }
    }

    private fun downloadAndExtractFallback() {
        val client = OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val zipFile = File(context.cacheDir, "vosk-model-small-en-in.zip")
        val req = Request.Builder().url(OFFICIAL_MODEL_URL).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Model download HTTP ${resp.code}")
            }
            val body = resp.body ?: throw IllegalStateException("Empty model download body")
            FileOutputStream(zipFile).use { fos ->
                body.byteStream().copyTo(fos, bufferSize = 32 * 1024)
            }
        }
        modelDir.deleteRecursively()
        modelDir.mkdirs()
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val stripped = entry.name.substringAfter('/')
                if (stripped.isNotBlank()) {
                    val out = File(modelDir, stripped)
                    if (!out.canonicalPath.startsWith(modelDir.canonicalPath)) {
                        throw SecurityException("Invalid zip entry path")
                    }
                    if (entry.isDirectory) {
                        out.mkdirs()
                    } else {
                        out.parentFile?.mkdirs()
                        FileOutputStream(out).use { fos -> zis.copyTo(fos) }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        zipFile.delete()
    }

    companion object {
        private const val TAG = "AyshaVoskModel"
        private const val ASSET_MODEL_DIR = "model-en-in"
        private const val OFFICIAL_MODEL_URL = "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
    }
}
