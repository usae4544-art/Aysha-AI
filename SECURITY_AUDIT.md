# AYSHA AI — Security Audit Report

## Scope
- Android modules (`app`, `core`, `settings`, `ai`, `memory`, `automation`, `phonecontrol`, `voice`, `wakeword`, `floatingassistant`), `AndroidManifest.xml`, `site/` Firebase client & Firestore rules.

## Findings & Remediations

| Severity | Finding | Status | Fix Applied |
|---|---|---|---|
| **Medium** | Global `android:usesCleartextTraffic="true"` in `AndroidManifest.xml` allowed plain HTTP to any external host. | **Fixed** | Replaced with `res/xml/network_security_config.xml` (`cleartextTrafficPermitted="false"` globally; HTTP allowed only for `127.0.0.1`, `localhost`, `10.0.2.2` local LLM endpoints). |
| **Medium** | `BrowserScreen` `WebView` had `javaScriptEnabled = true` without explicit `allowFileAccess = false`, `allowContentAccess = false`, or URL scheme filtering. | **Fixed** | Disabled `allowFileAccess` & `allowContentAccess`, and restricted navigation to `https://` / `http://` schemes only. |
| **Low / Info** | `android:allowBackup="false"`, `KeystoreCrypto` AES-256-GCM API key encryption, `SafeLogger` key redaction, `SettingsRepository.buildSafeExportJson` secret exclusion, and `firebase/firestore.rules` server-side enforcement. | **Verified Secure** | No hardcoded private AI/OAuth secrets found in production source. |
