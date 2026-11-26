package com.aysha.assistant.ai

import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.LicenseInfo
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.LicenseStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class DeepLinkAuthResult(
    val refreshToken: String,
    val uid: String = "",
    val email: String = "",
)

data class RemoteFirebaseConfig(
    val configured: Boolean,
    val apiKey: String,
    val projectId: String,
)

data class LicenseVerificationOutcome(
    val license: LicenseInfo,
    val updatedRefreshToken: String? = null,
    val shouldClearSession: Boolean = false,
)

class AyshaLicenseManager {
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun verifyWithSettings(settings: AppSettings): LicenseVerificationOutcome =
        verifyWithRefreshToken(settings.firebaseRefreshToken, settings)

    suspend fun verifyWithRefreshToken(
        rawRefreshToken: String,
        fallbackSettings: AppSettings,
    ): LicenseVerificationOutcome = withContext(Dispatchers.IO) {
        val refreshToken = rawRefreshToken.trim()
        val configRes = fetchPinnedFirebaseConfig()
        if (configRes.isFailure) {
            val cached = fallbackSettings.currentLicenseInfo()
            return@withContext LicenseVerificationOutcome(
                license = if (cached.isUnlocked()) {
                    cached.copy(message = "Offline mode — using cached verified license")
                } else {
                    cached.copy(
                        status = LicenseStatus.NETWORK_ERROR,
                        message = "Unable to reach portal config: ${configRes.exceptionOrNull()?.message}",
                    )
                },
            )
        }

        val cfg = configRes.getOrThrow()
        if (!cfg.configured || cfg.apiKey.isBlank() || cfg.projectId.isBlank()) {
            return@withContext LicenseVerificationOutcome(
                license = LicenseInfo(
                    uid = fallbackSettings.authUid,
                    email = fallbackSettings.authEmail,
                    displayName = fallbackSettings.authDisplayName,
                    status = LicenseStatus.UNCONFIGURED,
                    firebaseConfigured = false,
                    message = "Firebase backend not yet configured in site/firebase-config.json",
                ),
            )
        }

        if (refreshToken.isBlank()) {
            return@withContext LicenseVerificationOutcome(
                license = LicenseInfo(
                    status = LicenseStatus.SIGNED_OUT,
                    firebaseConfigured = true,
                    message = "Sign in with Google via browser to verify your license.",
                ),
            )
        }

        val tokenExchange = exchangeRefreshToken(cfg.apiKey, refreshToken)
        if (tokenExchange.isFailure) {
            val errMsg = tokenExchange.exceptionOrNull()?.message ?: "Token exchange failed"
            val fatalAuth = errMsg.contains("INVALID_REFRESH_TOKEN", ignoreCase = true) ||
                errMsg.contains("TOKEN_EXPIRED", ignoreCase = true) ||
                errMsg.contains("USER_DISABLED", ignoreCase = true) ||
                errMsg.contains("USER_NOT_FOUND", ignoreCase = true)
            val cached = fallbackSettings.currentLicenseInfo()
            return@withContext if (fatalAuth) {
                LicenseVerificationOutcome(
                    license = LicenseInfo(
                        status = LicenseStatus.SIGNED_OUT,
                        firebaseConfigured = true,
                        message = "Session expired ($errMsg). Please sign in again.",
                    ),
                    shouldClearSession = true,
                )
            } else {
                LicenseVerificationOutcome(
                    license = if (cached.isUnlocked()) {
                        cached.copy(firebaseConfigured = true, message = "Offline grace active")
                    } else {
                        cached.copy(
                            status = LicenseStatus.NETWORK_ERROR,
                            firebaseConfigured = true,
                            message = errMsg,
                        )
                    },
                )
            }
        }

        val tokenResult = tokenExchange.getOrThrow()
        val accountRes = lookupGoogleAccount(cfg.apiKey, tokenResult.idToken)
        if (accountRes.isFailure) {
            return@withContext LicenseVerificationOutcome(
                license = LicenseInfo(
                    status = LicenseStatus.NETWORK_ERROR,
                    firebaseConfigured = true,
                    message = "Account check failed: ${accountRes.exceptionOrNull()?.message}",
                ),
            )
        }

        val account = accountRes.getOrThrow()
        val deviceNow = System.currentTimeMillis()
        val serverNow = tokenResult.serverNowMs.takeIf { it > 0L } ?: deviceNow

        // Admin override strictly gated on verified Google account email matching ADMIN_EMAIL
        if (account.emailVerified && account.email.equals(ADMIN_EMAIL, ignoreCase = true)) {
            return@withContext LicenseVerificationOutcome(
                license = LicenseInfo(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    plan = LicensePlan.LIFETIME,
                    isLifetime = true,
                    expiresAtMs = 0L,
                    lastServerNowMs = serverNow,
                    lastVerifiedDeviceMs = deviceNow,
                    status = LicenseStatus.ADMIN_ACTIVE,
                    couponCode = "",
                    firebaseConfigured = true,
                    message = "Owner administrator access verified",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }

        val docRes = fetchFirestoreLicense(cfg.projectId, account.uid, tokenResult.idToken)
        if (docRes.isFailure) {
            return@withContext LicenseVerificationOutcome(
                license = LicenseInfo(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    status = LicenseStatus.NETWORK_ERROR,
                    firebaseConfigured = true,
                    message = "License fetch failed: ${docRes.exceptionOrNull()?.message}",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }

        val fetched = docRes.getOrThrow()
        val lic = if (fetched.rawJson == null) {
            LicenseInfo(
                uid = account.uid,
                email = account.email,
                displayName = account.displayName,
                plan = LicensePlan.NONE,
                isLifetime = false,
                expiresAtMs = 0L,
                lastServerNowMs = fetched.serverNowMs.takeIf { it > 0L } ?: serverNow,
                lastVerifiedDeviceMs = deviceNow,
                status = LicenseStatus.EXPIRED,
                firebaseConfigured = true,
                message = "No active license found for ${account.email}. Redeem a coupon or select a plan on the website.",
            )
        } else {
            parseFirestoreLicenseDocument(
                jsonStr = fetched.rawJson,
                uid = account.uid,
                email = account.email,
                displayName = account.displayName,
                serverNowMs = fetched.serverNowMs.takeIf { it > 0L } ?: serverNow,
                deviceNowMs = deviceNow,
            ).copy(firebaseConfigured = true)
        }

        LicenseVerificationOutcome(
            license = lic,
            updatedRefreshToken = tokenResult.refreshToken,
        )
    }

    private fun fetchPinnedFirebaseConfig(): Result<RemoteFirebaseConfig> = runCatching {
        val req = Request.Builder()
            .url("$CONFIG_URL?ts=${System.currentTimeMillis() / 30000L}")
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                error("HTTP ${resp.code} loading firebase-config.json")
            }
            val body = resp.body?.string().orEmpty()
            val obj = JSONObject(body)
            RemoteFirebaseConfig(
                configured = obj.optBoolean("configured", false),
                apiKey = obj.optString("apiKey", "").trim(),
                projectId = obj.optString("projectId", "").trim(),
            )
        }
    }

    private data class TokenExchangeData(
        val idToken: String,
        val refreshToken: String,
        val serverNowMs: Long,
    )

    private fun exchangeRefreshToken(apiKey: String, refreshToken: String): Result<TokenExchangeData> = runCatching {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()
        val req = Request.Builder()
            .url("https://securetoken.googleapis.com/v1/token?key=$apiKey")
            .post(form)
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            val serverNow = parseHttpServerDateMs(resp.header("Date"))
            if (!resp.isSuccessful) {
                val msg = runCatching { JSONObject(body).optJSONObject("error")?.optString("message") }.getOrNull()
                error(msg ?: "HTTP ${resp.code}")
            }
            val obj = JSONObject(body)
            val idToken = obj.optString("id_token", "")
            val nextRefresh = obj.optString("refresh_token", refreshToken).ifBlank { refreshToken }
            if (idToken.isBlank()) error("Missing id_token in response")
            TokenExchangeData(idToken, nextRefresh, serverNow)
        }
    }

    private data class VerifiedAccount(
        val uid: String,
        val email: String,
        val emailVerified: Boolean,
        val displayName: String,
    )

    private fun lookupGoogleAccount(apiKey: String, idToken: String): Result<VerifiedAccount> = runCatching {
        val payload = JSONObject().put("idToken", idToken).toString()
        val req = Request.Builder()
            .url("https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=$apiKey")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("HTTP ${resp.code}")
            val users = JSONObject(body).optJSONArray("users")
            if (users == null || users.length() == 0) error("No user returned by Identity Toolkit")
            val u = users.getJSONObject(0)
            VerifiedAccount(
                uid = u.optString("localId", ""),
                email = u.optString("email", ""),
                emailVerified = u.optBoolean("emailVerified", false),
                displayName = u.optString("displayName", ""),
            )
        }
    }

    private data class FirestoreDocResult(
        val rawJson: String?,
        val serverNowMs: Long,
    )

    private fun fetchFirestoreLicense(
        projectId: String,
        uid: String,
        idToken: String,
    ): Result<FirestoreDocResult> = runCatching {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/licenses/$uid"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $idToken")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val serverNow = parseHttpServerDateMs(resp.header("Date"))
            if (resp.code == 404) {
                return@runCatching FirestoreDocResult(null, serverNow)
            }
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("Firestore HTTP ${resp.code}")
            }
            FirestoreDocResult(body, serverNow)
        }
    }

    companion object {
        const val ADMIN_EMAIL = "usae4544@gmail.com"
        const val SITE_BASE_URL = "https://usae4544-art.github.io/Aysha-AI"
        const val SITE_AUTH_URL = "$SITE_BASE_URL/app-auth.html?mode=apk"
        const val SITE_PORTAL_URL = "$SITE_BASE_URL/#licensing"
        const val CONFIG_URL = "$SITE_BASE_URL/firebase-config.json"

        fun parseDeepLinkCallback(uriString: String?): DeepLinkAuthResult? {
            if (uriString.isNullOrBlank()) return null
            if (!uriString.startsWith("aysha://auth/callback", ignoreCase = true)) return null
            val qIdx = uriString.indexOf('?')
            if (qIdx < 0 || qIdx == uriString.lastIndex) return null
            val query = uriString.substring(qIdx + 1)
            val params = mutableMapOf<String, String>()
            for (pair in query.split('&')) {
                val eq = pair.indexOf('=')
                if (eq > 0) {
                    val k = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8.name())
                    val v = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8.name())
                    params[k] = v
                }
            }
            val rt = params["refreshToken"].orEmpty().trim()
            if (rt.isBlank()) return null
            return DeepLinkAuthResult(
                refreshToken = rt,
                uid = params["uid"].orEmpty().trim(),
                email = params["email"].orEmpty().trim(),
            )
        }

        fun parseHttpServerDateMs(dateHeader: String?): Long {
            if (dateHeader.isNullOrBlank()) return 0L
            return runCatching {
                ZonedDateTime.parse(dateHeader.trim(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant()
                    .toEpochMilli()
            }.getOrDefault(0L)
        }

        fun parseFirestoreLicenseDocument(
            jsonStr: String,
            uid: String,
            email: String,
            displayName: String,
            serverNowMs: Long,
            deviceNowMs: Long = System.currentTimeMillis(),
        ): LicenseInfo {
            val root = JSONObject(jsonStr)
            val fields = root.optJSONObject("fields") ?: JSONObject()

            val docStatus = fields.optJSONObject("status")?.optString("stringValue", "").orEmpty().lowercase()
            val plan = LicensePlan.fromId(fields.optJSONObject("planId")?.optString("stringValue", ""))
            val isLifetime = fields.optJSONObject("isLifetime")?.optBoolean("booleanValue", false) == true ||
                plan == LicensePlan.LIFETIME
            val couponCode = fields.optJSONObject("couponCode")?.optString("stringValue", "").orEmpty()

            val expiresFromTimestamp = fields.optJSONObject("expiresAt")
                ?.optString("timestampValue", "")
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrDefault(0L) } ?: 0L

            val expiresFromInt = fields.optJSONObject("expiresAtMs")
                ?.optString("integerValue", "0")
                ?.toLongOrNull() ?: 0L

            val expiresAtMs = maxOf(expiresFromTimestamp, expiresFromInt)

            if (docStatus == "revoked") {
                return LicenseInfo(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    plan = plan,
                    isLifetime = false,
                    expiresAtMs = 0L,
                    lastServerNowMs = serverNowMs,
                    lastVerifiedDeviceMs = deviceNowMs,
                    status = LicenseStatus.REVOKED,
                    couponCode = couponCode,
                    message = "Your license was revoked by the administrator.",
                )
            }

            val isValidNow = docStatus == "active" && (isLifetime || (expiresAtMs > serverNowMs))
            return if (isValidNow) {
                LicenseInfo(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    plan = plan,
                    isLifetime = isLifetime,
                    expiresAtMs = if (isLifetime) 0L else expiresAtMs,
                    lastServerNowMs = serverNowMs,
                    lastVerifiedDeviceMs = deviceNowMs,
                    status = LicenseStatus.ACTIVE,
                    couponCode = couponCode,
                    message = if (isLifetime) "Lifetime license active" else "${plan.label} license active",
                )
            } else {
                LicenseInfo(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    plan = plan,
                    isLifetime = false,
                    expiresAtMs = expiresAtMs,
                    lastServerNowMs = serverNowMs,
                    lastVerifiedDeviceMs = deviceNowMs,
                    status = LicenseStatus.EXPIRED,
                    couponCode = couponCode,
                    message = "License expired. Renew or redeem a coupon on the website.",
                )
            }
        }
    }
}
