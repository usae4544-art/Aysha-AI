package com.aysha.assistant.ai

import android.os.Build
import com.aysha.assistant.core.AdminUserRecord
import com.aysha.assistant.core.AppSettings
import com.aysha.assistant.core.LicenseInfo
import com.aysha.assistant.core.LicensePlan
import com.aysha.assistant.core.LicenseStatus
import com.aysha.assistant.core.SafeLogger
import com.aysha.assistant.core.StorePricingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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

    suspend fun verifyWithSettings(
        settings: AppSettings,
        appVersion: String = "",
    ): LicenseVerificationOutcome =
        verifyWithRefreshToken(settings.firebaseRefreshToken, settings, appVersion)

    suspend fun verifyWithRefreshToken(
        rawRefreshToken: String,
        fallbackSettings: AppSettings,
        appVersion: String = "",
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

        // Check user record for block status first (even before license check)
        val userDocRes = fetchFirestoreDoc(cfg.projectId, "users/${account.uid}", tokenResult.idToken)
        val userFields = userDocRes.getOrNull()?.rawJson?.let {
            runCatching { JSONObject(it).optJSONObject("fields") }.getOrNull()
        }
        val isBlockedByAdmin = userFields?.optJSONObject("blocked")?.optBoolean("booleanValue", false) == true
        val blockedReason = userFields?.optJSONObject("blockedReason")?.optString("stringValue", "").orEmpty()

        // Admin override strictly gated on verified Google account email matching ADMIN_EMAIL
        if (account.emailVerified && account.email.equals(ADMIN_EMAIL, ignoreCase = true)) {
            upsertAndroidUserLogin(cfg.projectId, tokenResult.idToken, account, serverNow, appVersion)
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
                    blocked = false,
                    message = "Owner administrator access verified",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }

        if (isBlockedByAdmin) {
            return@withContext LicenseVerificationOutcome(
                license = LicenseInfo(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    plan = LicensePlan.NONE,
                    isLifetime = false,
                    expiresAtMs = 0L,
                    lastServerNowMs = serverNow,
                    lastVerifiedDeviceMs = deviceNow,
                    status = LicenseStatus.BLOCKED,
                    firebaseConfigured = true,
                    blocked = true,
                    blockedReason = blockedReason,
                    message = blockedReason.ifBlank { "Your account has been blocked by the administrator." },
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }

        // Log APK login metadata for non-blocked user (never touches admin/block fields)
        upsertAndroidUserLogin(cfg.projectId, tokenResult.idToken, account, serverNow, appVersion)

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

    suspend fun claimLicenseKey(
        rawLicenseKey: String,
        fallbackSettings: AppSettings,
    ): LicenseVerificationOutcome = withContext(Dispatchers.IO) {
        val cleanKey = rawLicenseKey.trim().uppercase()
        if (cleanKey.length < 12) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    firebaseConfigured = true,
                    message = "Enter a valid full License Key (e.g. AYSHA-XXXX-...).",
                ),
            )
        }

        val refreshToken = fallbackSettings.firebaseRefreshToken.trim()
        val configRes = fetchPinnedFirebaseConfig()
        if (configRes.isFailure) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    message = "Unable to reach server config: ${configRes.exceptionOrNull()?.message}",
                ),
            )
        }
        val cfg = configRes.getOrThrow()
        if (!cfg.configured || cfg.apiKey.isBlank() || cfg.projectId.isBlank()) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    status = LicenseStatus.UNCONFIGURED,
                    message = "Firebase backend is not configured.",
                ),
            )
        }
        if (refreshToken.isBlank()) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    status = LicenseStatus.SIGNED_OUT,
                    firebaseConfigured = true,
                    message = "First tap Sign in with Google (Browser), then enter your License Key.",
                ),
            )
        }

        val tokenExchange = exchangeRefreshToken(cfg.apiKey, refreshToken)
        if (tokenExchange.isFailure) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    firebaseConfigured = true,
                    message = "Auth session check failed: ${tokenExchange.exceptionOrNull()?.message}",
                ),
            )
        }
        val tokenResult = tokenExchange.getOrThrow()
        val accountRes = lookupGoogleAccount(cfg.apiKey, tokenResult.idToken)
        if (accountRes.isFailure) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    firebaseConfigured = true,
                    message = "Account verification failed: ${accountRes.exceptionOrNull()?.message}",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }
        val account = accountRes.getOrThrow()
        val deviceNow = System.currentTimeMillis()
        val serverNow = tokenResult.serverNowMs.takeIf { it > 0L } ?: deviceNow

        val keyDocRes = fetchFirestoreDoc(cfg.projectId, "license_keys/$cleanKey", tokenResult.idToken)
        if (keyDocRes.isFailure) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    firebaseConfigured = true,
                    message = "Key lookup failed: ${keyDocRes.exceptionOrNull()?.message}",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }
        val keyFetched = keyDocRes.getOrThrow()
        if (keyFetched.rawJson == null) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    firebaseConfigured = true,
                    message = "Invalid License Key: $cleanKey not found.",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }

        val fields = JSONObject(keyFetched.rawJson).optJSONObject("fields") ?: JSONObject()
        val kActive = fields.optJSONObject("active")?.optBoolean("booleanValue", false) == true
        val kStatus = fields.optJSONObject("status")?.optString("stringValue", "").orEmpty().lowercase()
        val kUid = fields.optJSONObject("uid")?.optString("stringValue", "").orEmpty()
        val kPlan = LicensePlan.fromId(fields.optJSONObject("planId")?.optString("stringValue", ""))

        if (kUid == account.uid && kStatus == "active") {
            return@withContext verifyWithRefreshToken(tokenResult.refreshToken, fallbackSettings)
        }
        if (!kActive || kStatus == "revoked") {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    firebaseConfigured = true,
                    message = "This License Key has been deactivated by the administrator.",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }
        if (kStatus != "available" || kPlan == LicensePlan.NONE) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    firebaseConfigured = true,
                    message = "This License Key was already claimed or is not valid for activation.",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }

        val commitServerNow = keyFetched.serverNowMs.takeIf { it > 0L } ?: serverNow
        val isLifetime = kPlan == LicensePlan.LIFETIME
        val expiresAtMs = if (isLifetime) 0L else commitServerNow + kPlan.durationDays * 86_400_000L

        val commitRes = commitKeyClaimBatch(
            projectId = cfg.projectId,
            idToken = tokenResult.idToken,
            keyId = cleanKey,
            uid = account.uid,
            email = account.email,
            plan = kPlan,
            isLifetime = isLifetime,
            serverNowMs = commitServerNow,
            expiresAtMs = expiresAtMs,
        )
        if (commitRes.isFailure) {
            return@withContext LicenseVerificationOutcome(
                license = fallbackSettings.currentLicenseInfo().copy(
                    uid = account.uid,
                    email = account.email,
                    displayName = account.displayName,
                    firebaseConfigured = true,
                    message = "Key activation error: ${commitRes.exceptionOrNull()?.message}",
                ),
                updatedRefreshToken = tokenResult.refreshToken,
            )
        }

        LicenseVerificationOutcome(
            license = LicenseInfo(
                uid = account.uid,
                email = account.email,
                displayName = account.displayName,
                plan = kPlan,
                isLifetime = isLifetime,
                expiresAtMs = expiresAtMs,
                lastServerNowMs = commitServerNow,
                lastVerifiedDeviceMs = deviceNow,
                status = LicenseStatus.ACTIVE,
                licenseKey = cleanKey,
                firebaseConfigured = true,
                message = "Activated ${kPlan.label} via License Key!",
            ),
            updatedRefreshToken = tokenResult.refreshToken,
        )
    }

    private fun fetchFirestoreDoc(
        projectId: String,
        docPath: String,
        idToken: String,
    ): Result<FirestoreDocResult> = runCatching {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$docPath"
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

    private fun commitKeyClaimBatch(
        projectId: String,
        idToken: String,
        keyId: String,
        uid: String,
        email: String,
        plan: LicensePlan,
        isLifetime: Boolean,
        serverNowMs: Long,
        expiresAtMs: Long,
    ): Result<Unit> = runCatching {
        val baseDocPath = "projects/$projectId/databases/(default)/documents"
        val nowIso = Instant.ofEpochMilli(serverNowMs).toString()

        val keyUpdate = JSONObject().apply {
            put("updateMask", JSONObject().put("fieldPaths", JSONArray(listOf("status", "claimedByUid", "claimedByEmail", "claimedAt"))))
            put("currentDocument", JSONObject().put("exists", true))
            put(
                "update",
                JSONObject().apply {
                    put("name", "$baseDocPath/license_keys/$keyId")
                    put(
                        "fields",
                        JSONObject().apply {
                            put("status", JSONObject().put("stringValue", "claimed"))
                            put("claimedByUid", JSONObject().put("stringValue", uid))
                            put("claimedByEmail", JSONObject().put("stringValue", email.lowercase()))
                            put("claimedAt", JSONObject().put("timestampValue", nowIso))
                        },
                    )
                },
            )
        }

        val licFields = JSONObject().apply {
            put("uid", JSONObject().put("stringValue", uid))
            put("email", JSONObject().put("stringValue", email.lowercase()))
            put("planId", JSONObject().put("stringValue", plan.id))
            put("status", JSONObject().put("stringValue", "active"))
            put("isLifetime", JSONObject().put("booleanValue", isLifetime))
            if (isLifetime) {
                put("expiresAt", JSONObject().put("nullValue", JSONObject.NULL))
            } else {
                put("expiresAt", JSONObject().put("timestampValue", Instant.ofEpochMilli(expiresAtMs).toString()))
            }
            put("expiresAtMs", JSONObject().put("integerValue", expiresAtMs.toString()))
            put("source", JSONObject().put("stringValue", "license_key"))
            put("licenseKey", JSONObject().put("stringValue", keyId))
            put("updatedAt", JSONObject().put("timestampValue", nowIso))
            put("updatedBy", JSONObject().put("stringValue", uid))
        }
        val licWrite = JSONObject().apply {
            put(
                "update",
                JSONObject().apply {
                    put("name", "$baseDocPath/licenses/$uid")
                    put("fields", licFields)
                },
            )
        }

        val body = JSONObject().put("writes", JSONArray().put(keyUpdate).put(licWrite)).toString()
        val req = Request.Builder()
            .url("https://firestore.googleapis.com/v1/$baseDocPath:commit")
            .header("Authorization", "Bearer $idToken")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        http.newCall(req).execute().use { resp ->
            val respBody = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                error("Commit HTTP ${resp.code}: $respBody")
            }
        }
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

    private fun upsertAndroidUserLogin(
        projectId: String,
        idToken: String,
        account: VerifiedAccount,
        serverNowMs: Long,
        appVersion: String,
    ) {
        runCatching {
            val nowIso = Instant.ofEpochMilli(serverNowMs.takeIf { it > 0L } ?: System.currentTimeMillis()).toString()
            val deviceLabel = "${Build.MANUFACTURER.orEmpty()} ${Build.MODEL.orEmpty()}".trim().take(110)
            val fields = JSONObject().apply {
                put("uid", JSONObject().put("stringValue", account.uid))
                put("email", JSONObject().put("stringValue", account.email.lowercase()))
                put("emailVerified", JSONObject().put("booleanValue", account.emailVerified))
                put("displayName", JSONObject().put("stringValue", account.displayName.take(110)))
                put("lastLoginSource", JSONObject().put("stringValue", "android"))
                put("deviceModel", JSONObject().put("stringValue", deviceLabel.ifBlank { "Android Device" }))
                if (appVersion.isNotBlank()) {
                    put("appVersion", JSONObject().put("stringValue", appVersion.take(36)))
                }
                put("lastLoginAt", JSONObject().put("timestampValue", nowIso))
            }
            val maskParams = buildList {
                add("uid"); add("email"); add("emailVerified"); add("displayName")
                add("lastLoginSource"); add("deviceModel"); add("lastLoginAt")
                if (appVersion.isNotBlank()) add("appVersion")
            }.joinToString("&") { "updateMask.fieldPaths=$it" }

            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/users/${account.uid}?$maskParams"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $idToken")
                .patch(JSONObject().put("fields", fields).toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            http.newCall(req).execute().close()
        }.onFailure { err ->
            SafeLogger.w("AyshaLicenseManager", "User login sync skipped: ${err.message}")
        }
    }

    suspend fun fetchStorePricingConfig(): StorePricingConfig? = withContext(Dispatchers.IO) {
        runCatching {
            val cfg = fetchPinnedFirebaseConfig().getOrNull() ?: return@runCatching null
            if (!cfg.configured || cfg.projectId.isBlank()) return@runCatching null
            val url = "https://firestore.googleapis.com/v1/projects/${cfg.projectId}/databases/(default)/documents/config/store"
            val req = Request.Builder().url(url).get().build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val fields = JSONObject(resp.body?.string().orEmpty()).optJSONObject("fields") ?: return@runCatching null
                val priceFields = fields.optJSONObject("prices")?.optJSONObject("mapValue")?.optJSONObject("fields") ?: JSONObject()
                fun readPrice(id: String, fallback: Int): Int {
                    val obj = priceFields.optJSONObject(id) ?: return fallback
                    val iVal = obj.optString("integerValue", "").toIntOrNull()
                    if (iVal != null) return iVal.coerceAtLeast(0)
                    val dVal = obj.optDouble("doubleValue", Double.NaN)
                    return if (!dVal.isNaN()) dVal.toInt().coerceAtLeast(0) else fallback
                }
                val defaults = StorePricingConfig()
                StorePricingConfig(
                    upiId = fields.optJSONObject("upiId")?.optString("stringValue", "").orEmpty().ifBlank { defaults.upiId },
                    upiName = fields.optJSONObject("upiName")?.optString("stringValue", "").orEmpty().ifBlank { defaults.upiName },
                    currency = fields.optJSONObject("currency")?.optString("stringValue", "").orEmpty().ifBlank { "INR" },
                    prices = mapOf(
                        LicensePlan.ONE_MONTH to readPrice("ONE_MONTH", 149),
                        LicensePlan.TWO_MONTHS to readPrice("TWO_MONTHS", 249),
                        LicensePlan.ONE_YEAR to readPrice("ONE_YEAR", 999),
                        LicensePlan.TWO_YEARS to readPrice("TWO_YEARS", 1599),
                        LicensePlan.LIFETIME to readPrice("LIFETIME", 2999),
                    ),
                )
            }
        }.getOrNull()
    }

    private suspend fun verifyAdminSession(refreshToken: String): Result<Triple<RemoteFirebaseConfig, TokenExchangeData, VerifiedAccount>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cfg = fetchPinnedFirebaseConfig().getOrThrow()
                require(cfg.configured && cfg.apiKey.isNotBlank() && cfg.projectId.isNotBlank()) { "Firebase not configured" }
                val tok = exchangeRefreshToken(cfg.apiKey, refreshToken.trim()).getOrThrow()
                val acc = lookupGoogleAccount(cfg.apiKey, tok.idToken).getOrThrow()
                require(acc.emailVerified && acc.email.equals(ADMIN_EMAIL, ignoreCase = true)) {
                    "Admin privileges restricted to verified $ADMIN_EMAIL only"
                }
                Triple(cfg, tok, acc)
            }
        }

    suspend fun fetchAdminUserDirectory(refreshToken: String): Result<List<AdminUserRecord>> = withContext(Dispatchers.IO) {
        runCatching {
            val (cfg, tok, _) = verifyAdminSession(refreshToken).getOrThrow()
            val usersJson = listCollectionDocs(cfg.projectId, "users", tok.idToken)
            val licensesJson = listCollectionDocs(cfg.projectId, "licenses", tok.idToken)

            val licMap = mutableMapOf<String, JSONObject>()
            for (docObj in licensesJson) {
                val docId = docObj.optString("name", "").substringAfterLast('/')
                if (docId.isNotBlank()) licMap[docId] = docObj.optJSONObject("fields") ?: JSONObject()
            }

            val out = mutableListOf<AdminUserRecord>()
            val seenUids = mutableSetOf<String>()

            fun parseTsMs(fields: JSONObject?, key: String): Long {
                val ts = fields?.optJSONObject(key)?.optString("timestampValue", "").orEmpty()
                if (ts.isNotBlank()) {
                    return runCatching { Instant.parse(ts).toEpochMilli() }.getOrDefault(0L)
                }
                return fields?.optJSONObject("${key}Ms")?.optString("integerValue", "0")?.toLongOrNull() ?: 0L
            }

            for (uDoc in usersJson) {
                val fields = uDoc.optJSONObject("fields") ?: JSONObject()
                val docUid = uDoc.optString("name", "").substringAfterLast('/')
                val uid = fields.optJSONObject("uid")?.optString("stringValue", "").orEmpty().ifBlank { docUid }
                if (uid.isBlank()) continue
                seenUids.add(uid)
                val licFields = licMap[uid]
                val plan = LicensePlan.fromId(licFields?.optJSONObject("planId")?.optString("stringValue", ""))
                val expFromTs = parseTsMs(licFields, "expiresAt")
                val expFromInt = licFields?.optJSONObject("expiresAtMs")?.optString("integerValue", "0")?.toLongOrNull() ?: 0L
                out.add(
                    AdminUserRecord(
                        uid = uid,
                        email = fields.optJSONObject("email")?.optString("stringValue", "").orEmpty(),
                        displayName = fields.optJSONObject("displayName")?.optString("stringValue", "").orEmpty().ifBlank { "User" },
                        lastLoginSource = fields.optJSONObject("lastLoginSource")?.optString("stringValue", "").orEmpty().ifBlank { "unknown" },
                        deviceModel = fields.optJSONObject("deviceModel")?.optString("stringValue", "").orEmpty(),
                        appVersion = fields.optJSONObject("appVersion")?.optString("stringValue", "").orEmpty(),
                        lastLoginAtMs = parseTsMs(fields, "lastLoginAt"),
                        blocked = fields.optJSONObject("blocked")?.optBoolean("booleanValue", false) == true,
                        blockedReason = fields.optJSONObject("blockedReason")?.optString("stringValue", "").orEmpty(),
                        plan = plan,
                        licenseStatus = licFields?.optJSONObject("status")?.optString("stringValue", "").orEmpty(),
                        isLifetime = licFields?.optJSONObject("isLifetime")?.optBoolean("booleanValue", false) == true || plan == LicensePlan.LIFETIME,
                        expiresAtMs = maxOf(expFromTs, expFromInt),
                        licenseKey = licFields?.optJSONObject("licenseKey")?.optString("stringValue", "").orEmpty(),
                    ),
                )
            }

            // Include any user who has a license doc even before visiting users directory
            for ((uid, licFields) in licMap) {
                if (uid in seenUids) continue
                val plan = LicensePlan.fromId(licFields.optJSONObject("planId")?.optString("stringValue", ""))
                val expFromTs = parseTsMs(licFields, "expiresAt")
                val expFromInt = licFields.optJSONObject("expiresAtMs")?.optString("integerValue", "0")?.toLongOrNull() ?: 0L
                out.add(
                    AdminUserRecord(
                        uid = uid,
                        email = licFields.optJSONObject("email")?.optString("stringValue", "").orEmpty(),
                        displayName = "User",
                        lastLoginSource = licFields.optJSONObject("source")?.optString("stringValue", "").orEmpty().ifBlank { "license" },
                        lastLoginAtMs = parseTsMs(licFields, "updatedAt"),
                        plan = plan,
                        licenseStatus = licFields.optJSONObject("status")?.optString("stringValue", "").orEmpty(),
                        isLifetime = licFields.optJSONObject("isLifetime")?.optBoolean("booleanValue", false) == true || plan == LicensePlan.LIFETIME,
                        expiresAtMs = maxOf(expFromTs, expFromInt),
                        licenseKey = licFields.optJSONObject("licenseKey")?.optString("stringValue", "").orEmpty(),
                    ),
                )
            }

            out.sortedByDescending { it.lastLoginAtMs }
        }
    }

    private fun listCollectionDocs(projectId: String, collection: String, idToken: String): List<JSONObject> {
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$collection?pageSize=200"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $idToken")
            .get()
            .build()
        http.newCall(req).execute().use { resp ->
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) error("Firestore list $collection HTTP ${resp.code}: $body")
            val arr = JSONObject(body).optJSONArray("documents") ?: return emptyList()
            return (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }
        }
    }

    suspend fun adminSetUserBlocked(
        refreshToken: String,
        targetUid: String,
        block: Boolean,
        reason: String = "",
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(targetUid.isNotBlank()) { "Invalid target UID" }
            val (cfg, tok, acc) = verifyAdminSession(refreshToken).getOrThrow()
            require(targetUid != acc.uid) { "Admin cannot block own owner account" }
            val nowIso = Instant.ofEpochMilli(tok.serverNowMs.takeIf { it > 0L } ?: System.currentTimeMillis()).toString()
            val cleanReason = reason.trim().take(280).ifBlank { if (block) "Blocked by administrator" else "" }
            val fields = JSONObject().apply {
                put("uid", JSONObject().put("stringValue", targetUid))
                put("blocked", JSONObject().put("booleanValue", block))
                put("blockedReason", JSONObject().put("stringValue", cleanReason))
                put("blockedBy", JSONObject().put("stringValue", if (block) acc.email else ""))
                if (block) {
                    put("blockedAt", JSONObject().put("timestampValue", nowIso))
                } else {
                    put("blockedAt", JSONObject().put("nullValue", JSONObject.NULL))
                }
            }
            val mask = "updateMask.fieldPaths=uid&updateMask.fieldPaths=blocked&updateMask.fieldPaths=blockedReason&updateMask.fieldPaths=blockedBy&updateMask.fieldPaths=blockedAt"
            val url = "https://firestore.googleapis.com/v1/projects/${cfg.projectId}/databases/(default)/documents/users/$targetUid?$mask"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${tok.idToken}")
                .patch(JSONObject().put("fields", fields).toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("Block update HTTP ${resp.code}: ${resp.body?.string().orEmpty().take(180)}")
                }
            }
        }
    }

    suspend fun adminSaveStorePricing(
        refreshToken: String,
        store: StorePricingConfig,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val (cfg, tok, acc) = verifyAdminSession(refreshToken).getOrThrow()
            val cleanUpi = store.upiId.trim().take(120)
            require(cleanUpi.isNotBlank()) { "UPI ID cannot be empty" }
            val nowIso = Instant.ofEpochMilli(tok.serverNowMs.takeIf { it > 0L } ?: System.currentTimeMillis()).toString()

            val priceFields = JSONObject().apply {
                listOf(
                    LicensePlan.ONE_MONTH,
                    LicensePlan.TWO_MONTHS,
                    LicensePlan.ONE_YEAR,
                    LicensePlan.TWO_YEARS,
                    LicensePlan.LIFETIME,
                ).forEach { plan ->
                    val amount = (store.prices[plan] ?: 0).coerceIn(0, 10_000_000)
                    put(plan.id, JSONObject().put("integerValue", amount.toString()))
                }
            }

            val fields = JSONObject().apply {
                put("upiId", JSONObject().put("stringValue", cleanUpi))
                put("upiName", JSONObject().put("stringValue", store.upiName.trim().ifBlank { "AYSHA AI" }.take(100)))
                put("currency", JSONObject().put("stringValue", store.currency.ifBlank { "INR" }))
                put("prices", JSONObject().put("mapValue", JSONObject().put("fields", priceFields)))
                put("updatedAt", JSONObject().put("timestampValue", nowIso))
                put("updatedBy", JSONObject().put("stringValue", acc.email))
            }

            val mask = "updateMask.fieldPaths=upiId&updateMask.fieldPaths=upiName&updateMask.fieldPaths=currency&updateMask.fieldPaths=prices&updateMask.fieldPaths=updatedAt&updateMask.fieldPaths=updatedBy"
            val url = "https://firestore.googleapis.com/v1/projects/${cfg.projectId}/databases/(default)/documents/config/store?$mask"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${tok.idToken}")
                .patch(JSONObject().put("fields", fields).toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("Store save HTTP ${resp.code}: ${resp.body?.string().orEmpty().take(180)}")
                }
            }
        }
    }

    suspend fun adminGrantOrRevokeLicense(
        refreshToken: String,
        targetUid: String,
        targetEmail: String,
        plan: LicensePlan?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            require(targetUid.isNotBlank()) { "Invalid target UID" }
            val (cfg, tok, acc) = verifyAdminSession(refreshToken).getOrThrow()
            val nowMs = tok.serverNowMs.takeIf { it > 0L } ?: System.currentTimeMillis()
            val nowIso = Instant.ofEpochMilli(nowMs).toString()

            val fields = JSONObject().apply {
                put("uid", JSONObject().put("stringValue", targetUid))
                put("email", JSONObject().put("stringValue", targetEmail.lowercase()))
                if (plan == null || plan == LicensePlan.NONE) {
                    put("planId", JSONObject().put("stringValue", "NONE"))
                    put("status", JSONObject().put("stringValue", "revoked"))
                    put("isLifetime", JSONObject().put("booleanValue", false))
                    put("expiresAt", JSONObject().put("nullValue", JSONObject.NULL))
                    put("expiresAtMs", JSONObject().put("integerValue", "0"))
                    put("source", JSONObject().put("stringValue", "admin_revoke"))
                } else {
                    val isLife = plan == LicensePlan.LIFETIME
                    val expMs = if (isLife) 0L else nowMs + plan.durationDays * 86_400_000L
                    put("planId", JSONObject().put("stringValue", plan.id))
                    put("status", JSONObject().put("stringValue", "active"))
                    put("isLifetime", JSONObject().put("booleanValue", isLife))
                    if (isLife) {
                        put("expiresAt", JSONObject().put("nullValue", JSONObject.NULL))
                    } else {
                        put("expiresAt", JSONObject().put("timestampValue", Instant.ofEpochMilli(expMs).toString()))
                    }
                    put("expiresAtMs", JSONObject().put("integerValue", expMs.toString()))
                    put("source", JSONObject().put("stringValue", "admin_grant"))
                }
                put("updatedAt", JSONObject().put("timestampValue", nowIso))
                put("updatedBy", JSONObject().put("stringValue", acc.email))
            }

            val url = "https://firestore.googleapis.com/v1/projects/${cfg.projectId}/databases/(default)/documents/licenses/$targetUid"
            val req = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${tok.idToken}")
                .patch(JSONObject().put("fields", fields).toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    error("License update HTTP ${resp.code}: ${resp.body?.string().orEmpty().take(180)}")
                }
            }
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
            val licenseKey = fields.optJSONObject("licenseKey")?.optString("stringValue", "").orEmpty()

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
                    licenseKey = licenseKey,
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
                    licenseKey = licenseKey,
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
                    licenseKey = licenseKey,
                    message = "License expired. Renew or redeem a coupon on the website.",
                )
            }
        }
    }
}
