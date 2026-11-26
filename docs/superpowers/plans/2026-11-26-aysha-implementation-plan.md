# AYSHA Implementation Plan

> **For agentic workers:** Native execution in this session with verification-before-completion and unit tests across all modules.

**Goal:** Build the complete 10-module native Android assistant `AYSHA – Your Personal AI Assistant` (`com.aysha.assistant`), verify via Gradle unit tests + lint + release/debug APK compilation, and produce installable APKs.

**Architecture:** Clean modular Android app (`:core`, `:settings`, `:memory`, `:voice`, `:wakeword`, `:phonecontrol`, `:automation`, `:ai`, `:floatingassistant`, `:app`).

**Tech Stack:** Kotlin 2.1.0, Jetpack Compose Material 3, Room 2.6.1 (KSP), DataStore Preferences, Android Keystore AES-GCM, OkHttp 4.12, Coroutines 1.9, Vosk Android 0.3.75 (`vosk-model-small-en-in-0.4`), Android SpeechRecognizer/TextToSpeech, AccessibilityService, NotificationListenerService, WindowManager overlay.

## Global Constraints
- Package ID: `com.aysha.assistant`
- App Name: `AYSHA – Your Personal AI Assistant`
- Target SDK 35 (`compileSdk = 35`, `minSdk = 26`)
- Zero ads, zero subscriptions, zero artificial limits, zero hardcoded API keys
- Every module must have real implementation (no stub/dead buttons) and compile cleanly in release ProGuard/R8 mode.

## Review Focus
1. Missing permissions (Mic, Overlay, Call, SMS, Notification Access, Accessibility) never crash — always surface clean rationale + direct permission/settings recovery.
2. API key encryption via AndroidKeyStore handles keystore reset/corruption safely and never leaks keys in logs, exceptions, or JSON exports.
3. AudioRecord / SpeechRecognizer / TTS / GeminiLiveClient never leak mic/audio threads or attach duplicate listeners across screen rotation or rapid taps.
4. Vosk wake-word model unpacks once safely on IO thread, handles unknown/phonetic variants (`aysha`, `aisha`, `asha`, `ayesha`), and pauses during active TTS/SpeechRecognizer to avoid mic contention.
5. Soft keyboard (`IME`) and Android 15 edge-to-edge navigation bars never obscure text composer inputs or bottom action buttons.

## Tasks
- [x] Task 0: Design spec, implementation plan, and original AYSHA visual artwork generation.
- [ ] Task 1: Root Gradle multi-module setup + `:core` module (models, Liquid Glass theme & components, safe utilities).
- [ ] Task 2: `:settings` module (AndroidKeyStore AES-256-GCM key vault + DataStore preferences + Privacy/Permission snapshots).
- [ ] Task 3: `:memory` module (Room database, DAOs, entities, search, export, MemoryMatcher, unit tests).
- [ ] Task 4: `:voice` & `:wakeword` modules (SpeechRecognizer, multi-personality TTS engine, AudioRecord VAD + Vosk KWS, WakePhraseMatcher, unit tests).
- [ ] Task 5: `:phonecontrol` & `:automation` modules (Confirmation-gated Phone/SMS, Apps/Settings/Alarms/Timers/Media/Camera/Gallery/Battery/Weather/Search, AccessibilityService, MacroEngine, unit tests).
- [ ] Task 6: `:ai` module (ToolRegistry, OfflineCommandParser, MultiProviderAiClient with fallback chain, GeminiLiveClient, PromptBuilder, unit tests).
- [ ] Task 7: `:floatingassistant` module (AyshaForegroundService, draggable floating orb + expanded quick action glass menu).
- [ ] Task 8: `:app` module (AyshaApplication, MainActivity, AyshaViewModel, all 15 Liquid Glass screens, Adaptive Launcher Icons, AndroidManifest, ProGuard rules).
- [ ] Task 9: GitHub Actions CI build (`testDebugUnitTest`, `lintDebug`, `assembleDebug`, `assembleRelease` with release keystore + bundled Vosk model) & verification of final installable APKs.
