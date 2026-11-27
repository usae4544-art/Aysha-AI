package com.aysha.assistant.ai

import com.aysha.assistant.core.LiveRemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Fetches live Over-The-Air configuration (announcements, Gemini model overrides, system prompt tweaks,
 * and latest APK version metadata) from Firestore `config/app_live` or GitHub Pages `remote-config.json`,
 * falling back to GitHub Releases `/releases/latest` for app update discovery.
 */
class RemoteConfigManager {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    companion object {
        const val FIREBASE_PROJECT_ID = "gemini-494602"
        const val PAGES_REMOTE_CONFIG_URL = "https://usae4544-art.github.io/Aysha-AI/remote-config.json"
        const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/usae4544-art/Aysha-AI/releases/latest"

        fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
            val rParts = remoteVersion.trim().removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
            val cParts = currentVersion.trim().removePrefix("v").split('.').mapNotNull { it.toIntOrNull() }
            if (rParts.isEmpty() || cParts.isEmpty()) return false
            val len = maxOf(rParts.size, cParts.size)
            for (i in 0 until len) {
                val r = rParts.getOrElse(i) { 0 }
                val c = cParts.getOrElse(i) { 0 }
                if (r > c) return true
                if (r < c) return false
            }
            return false
        }
    }

    suspend fun fetchLiveConfig(): LiveRemoteConfig? = withContext(Dispatchers.IO) {
        val fromFirestore = fetchFromFirestore()
        val fromPages = fetchFromPagesJson()
        val base = mergeConfigs(primary = fromFirestore, secondary = fromPages)
        val releaseFallback = fetchLatestGithubRelease()

        val merged = when {
            base != null && releaseFallback != null -> {
                val releaseIsNewer = releaseFallback.latestVersion.isNotBlank() &&
                    (base.latestVersion.isBlank() || isNewerVersion(releaseFallback.latestVersion, base.latestVersion))
                if (!releaseIsNewer) {
                    base
                } else {
                    base.copy(
                        latestVersion = releaseFallback.latestVersion,
                        latestVersionCode = maxOf(base.latestVersionCode, releaseFallback.latestVersionCode),
                        apkDownloadUrl = releaseFallback.apkDownloadUrl.ifBlank { base.apkDownloadUrl },
                        updateNotes = releaseFallback.updateNotes.ifBlank { base.updateNotes },
                    )
                }
            }
            base != null -> base
            else -> releaseFallback
        }
        merged?.copy(fetchedAtMs = System.currentTimeMillis())
    }

    suspend fun downloadApkToFile(url: String, destFile: File): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            destFile.parentFile?.mkdirs()
            val req = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.android.package-archive, application/octet-stream, */*")
                .get()
                .build()
            val apkHttp = http.newBuilder()
                .followRedirects(true)
                .followSslRedirects(true)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
            apkHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("Download failed (HTTP ${resp.code})")
                }
                val body = resp.body ?: error("Empty download response")
                val tmp = File(destFile.parentFile, "${destFile.name}.part")
                body.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output, bufferSize = 32_768)
                    }
                }
                if (tmp.length() < 1_000_000L) {
                    tmp.delete()
                    error("Downloaded file too small (${tmp.length()} bytes)")
                }
                if (destFile.exists()) destFile.delete()
                if (!tmp.renameTo(destFile)) {
                    tmp.copyTo(destFile, overwrite = true)
                    tmp.delete()
                }
                destFile
            }
        }
    }

    private fun fetchFromFirestore(): LiveRemoteConfig? = runCatching {
        val url = "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/config/app_live"
        val req = Request.Builder().url(url).get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching null
            val fields = JSONObject(resp.body?.string().orEmpty()).optJSONObject("fields") ?: return@runCatching null
            LiveRemoteConfig(
                latestVersion = fsStr(fields, "latestVersion"),
                latestVersionCode = fsInt(fields, "latestVersionCode"),
                apkDownloadUrl = fsStr(fields, "apkDownloadUrl").ifBlank {
                    "https://github.com/usae4544-art/Aysha-AI/releases/latest/download/AYSHA-latest-release.apk"
                },
                updateNotes = fsStr(fields, "updateNotes"),
                forceUpdate = fsBool(fields, "forceUpdate"),
                announcementBanner = fsStr(fields, "announcementBanner"),
                geminiTextModelOverride = fsStr(fields, "geminiTextModel"),
                geminiLiveModelOverride = fsStr(fields, "geminiLiveModel"),
                systemPromptExtra = fsStr(fields, "systemPromptExtra"),
                maintenanceMode = fsBool(fields, "maintenanceMode"),
                maintenanceMessage = fsStr(fields, "maintenanceMessage"),
            )
        }
    }.getOrNull()

    private fun fetchFromPagesJson(): LiveRemoteConfig? = runCatching {
        val req = Request.Builder().url("$PAGES_REMOTE_CONFIG_URL?t=${System.currentTimeMillis() / 60000}").get().build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching null
            val obj = JSONObject(resp.body?.string().orEmpty())
            LiveRemoteConfig(
                latestVersion = obj.optString("latestVersion", ""),
                latestVersionCode = obj.optInt("latestVersionCode", 0),
                apkDownloadUrl = obj.optString("apkDownloadUrl", ""),
                updateNotes = obj.optString("updateNotes", ""),
                forceUpdate = obj.optBoolean("forceUpdate", false),
                announcementBanner = obj.optString("announcementBanner", ""),
                geminiTextModelOverride = obj.optString("geminiTextModel", ""),
                geminiLiveModelOverride = obj.optString("geminiLiveModel", ""),
                systemPromptExtra = obj.optString("systemPromptExtra", ""),
                maintenanceMode = obj.optBoolean("maintenanceMode", false),
                maintenanceMessage = obj.optString("maintenanceMessage", ""),
            )
        }
    }.getOrNull()

    private fun fetchLatestGithubRelease(): LiveRemoteConfig? = runCatching {
        val req = Request.Builder()
            .url(GITHUB_LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@runCatching null
            val obj = JSONObject(resp.body?.string().orEmpty())
            val tag = obj.optString("tag_name", "").removePrefix("v").trim()
            if (tag.isBlank()) return@runCatching null
            val assets = obj.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.optJSONObject(i) ?: continue
                    val name = a.optString("name", "")
                    if (name.endsWith("-release.apk") || name == "AYSHA-latest-release.apk") {
                        apkUrl = a.optString("browser_download_url", "")
                        if (name.contains("v$tag")) break
                    }
                }
            }
            LiveRemoteConfig(
                latestVersion = tag,
                apkDownloadUrl = apkUrl.ifBlank {
                    "https://github.com/usae4544-art/Aysha-AI/releases/download/v$tag/AYSHA-v$tag-release.apk"
                },
                updateNotes = obj.optString("name", "").ifBlank { "AYSHA v$tag update available" },
            )
        }
    }.getOrNull()

    internal fun mergeConfigs(primary: LiveRemoteConfig?, secondary: LiveRemoteConfig?): LiveRemoteConfig? {
        if (primary == null) return secondary
        if (secondary == null) return primary
        val primaryIsOlder = primary.latestVersion.isBlank() ||
            (secondary.latestVersion.isNotBlank() && isNewerVersion(secondary.latestVersion, primary.latestVersion))
        val bestVersion = if (primaryIsOlder) secondary.latestVersion else primary.latestVersion
        val bestApkUrl = if (primaryIsOlder) {
            secondary.apkDownloadUrl.ifBlank { primary.apkDownloadUrl }
        } else {
            primary.apkDownloadUrl.ifBlank { secondary.apkDownloadUrl }
        }
        val bestNotes = if (primaryIsOlder) {
            secondary.updateNotes.ifBlank { primary.updateNotes }
        } else {
            primary.updateNotes.ifBlank { secondary.updateNotes }
        }
        return LiveRemoteConfig(
            latestVersion = bestVersion,
            latestVersionCode = maxOf(primary.latestVersionCode, secondary.latestVersionCode),
            apkDownloadUrl = bestApkUrl,
            updateNotes = bestNotes,
            forceUpdate = primary.forceUpdate || secondary.forceUpdate,
            announcementBanner = primary.announcementBanner.ifBlank { secondary.announcementBanner },
            geminiTextModelOverride = primary.geminiTextModelOverride.ifBlank { secondary.geminiTextModelOverride },
            geminiLiveModelOverride = primary.geminiLiveModelOverride.ifBlank { secondary.geminiLiveModelOverride },
            systemPromptExtra = primary.systemPromptExtra.ifBlank { secondary.systemPromptExtra },
            maintenanceMode = primary.maintenanceMode || secondary.maintenanceMode,
            maintenanceMessage = primary.maintenanceMessage.ifBlank { secondary.maintenanceMessage },
        )
    }

    private fun fsStr(fields: JSONObject, key: String): String =
        fields.optJSONObject(key)?.optString("stringValue", "").orEmpty()

    private fun fsBool(fields: JSONObject, key: String): Boolean =
        fields.optJSONObject(key)?.optBoolean("booleanValue", false) ?: false

    private fun fsInt(fields: JSONObject, key: String): Int {
        val obj = fields.optJSONObject(key) ?: return 0
        val strVal = obj.optString("integerValue", "")
        return strVal.toIntOrNull() ?: 0
    }
}
