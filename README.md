# AYSHA – Your Personal AI Assistant (`com.aysha.assistant`)

Free-first, privacy-focused, modular native Android AI Assistant built with Kotlin, Jetpack Compose, Material 3 Liquid Glass UI, Room Database, Android Keystore AES-256-GCM encryption, and on-device Vosk wake-word detection.

## Architecture (10 Modules)
- `:app` — Main shell, Liquid Glass UI screens, ViewModel, adaptive launcher icon
- `:core` — Domain models, `AyshaTheme` Dark/Light Liquid Glass design tokens, `SafeLogger`
- `:settings` — `KeystoreCrypto` (AES-256-GCM), DataStore preferences, `PermissionAudit`
- `:memory` — Room database (`AyshaDatabase`), DAOs, search, `ChatExporter`, `MemoryMatcher`
- `:voice` — `SpeechRecognizer` + multi-personality `TextToSpeech` (Hindi/English/Hinglish)
- `:wakeword` — `AudioRecord` VAD gate + on-device Vosk (`Hey Aysha`, `Hello Aysha`, `Wake up Aysha`)
- `:phonecontrol` — Official intents, confirmation-gated Call/SMS, Notification Listener, Weather & Search
- `:automation` — `AyshaAccessibilityService`, screen inspector, `MacroEngine` & Study Mode workflow
- `:ai` — `OfflineCommandParser`, `MultiProviderAiClient` (Gemini, Groq, OpenAI/Local Ollama + Fallback), `GeminiLiveClient`
- `:floatingassistant` — `AyshaForegroundService` + draggable floating glass orb & quick menu

## Build Instructions
1. Ensure JDK 21 + Android SDK (API 35) + Gradle 8.10.2 are available (or run `.github/workflows/build-apk.yml`).
2. Bundle the Apache-2.0 `vosk-model-small-en-in-0.4` into `wakeword/src/main/assets/model-en-in/` (optional; falls back to in-app download if absent).
3. Run unit tests: `gradle testDebugUnitTest`
4. Build APKs: `gradle assembleDebug assembleRelease`
