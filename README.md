# AYSHA – Your Personal AI Assistant (`com.aysha.assistant`)

[![MIT License](https://img.shields.io/badge/License-MIT-22D3EE.svg)](LICENSE)

Free-first, open-source, privacy-focused native Android AI Assistant built with Kotlin, Jetpack Compose, Material 3, Room Database, Android Keystore AES-256-GCM encryption, and on-device Vosk wake-word detection.

- **Website:** [https://usae4544-art.github.io/Aysha-AI/](https://usae4544-art.github.io/Aysha-AI/)
- **Releases (APK Downloads):** [GitHub Releases](https://github.com/usae4544-art/Aysha-AI/releases/latest)
- **License:** [MIT License](LICENSE)

## What's Inside (v2.1.0)
- **Browser Google Login & Synchronized Licensing:** Opens browser Google OAuth (`site/app-auth.html` -> `aysha://auth/callback`) with AES-256-GCM encrypted refresh token storage, Firestore license tiers (`1 Month`, `2 Months`, `1 Year`, `2 Years`, `Lifetime`), time-limited & quota-limited coupon redemption, UPI UTR manual checkout, and owner admin console (`usae4544@gmail.com`). See [`firebase/README.md`](firebase/README.md) for Firebase setup.
- **11 Signature Animated HUD & 3D Orbs:** `AYSHA 2047`, `Aysha Nova`, `Aysha Nova 3D`, `Liquid Core`, `J.A.R.V.I.S.`, `J.A.R.V.I.S. 3D`, `F.R.I.D.A.Y.`, `Red HUD`, `Particle Swarm`, `Pulse Reactor`, `Ultron` + 9 HUD accent colors.
- **Maya-Grade Theme & Typography Engine:** 9 palettes (`Obsidian`, `Midnight`, `Abyss`, `Ember`, `Nocturne`, `Rosewood`, `Clay Navy`, `Clay Day`, `Daylight`), 4 surface styles (`Glass`, `Clay`, `Flat`, `Soft`), 4 corner styles, UI scale, and bundled SIL OFL fonts (`Inter`, `Nunito`, `JetBrains Mono`).
- **Voice, Barge-In & Awareness Engine:** Speaker-mode barge-in with `AcousticEchoCanceler` + `NoiseSuppressor`, configurable Follow-Up listening window, Awareness sensitivity gate (`Reserved`, `Balanced`, `Alert`), media mute guard, and Awareness Decision Log.
- **Semantic Memory, Rules & Documents:** Hinglish/English synonym-aware TF-IDF retrieval, `"Mere baare me"` profile card, `"Purani"` memory archive/restore, pasted Documents chunking, and `"AYSHA ke Rules"` (`ab se hamesha...`).
- **Tools & Companion:** Live Mood & Energy, Sub-Agent specialist roles, Visual Camera Scanner, Study Whiteboard, In-App Mini Browser with AI summary, Vibes, Accessibility Macros, Full Safe Backup/Restore, and confirmation-gated Calls, SMS & Emergency SOS.

## Architecture (10 Modules)
- `:app` — Main shell, Orbs, Theme Studio, Scanner, Whiteboard, Browser, Rules & Hub UI
- `:core` — Domain models, `AyshaTheme` palettes/surfaces/OFL typography, `SafeLogger`
- `:settings` — `KeystoreCrypto` (AES-256-GCM), DataStore preferences, `PermissionAudit`
- `:memory` — Room v2 (`AyshaDatabase`), TF-IDF `MemoryMatcher`, Rules, Docs, `BackupRestoreHelper`
- `:voice` — `SpeechRecognizer`, `BargeInDetector` (AEC/NS), multi-personality `TextToSpeech`
- `:wakeword` — `AudioRecord` VAD Awareness gate + on-device Vosk (`Hey Aysha`, `Hello Aysha`, `Wake up Aysha`)
- `:phonecontrol` — Confirmation-gated Call/SMS/SOS, Notification Listener, Weather & Web Search
- `:automation` — `AyshaAccessibilityService`, screen inspector, `MacroEngine`
- `:ai` — `OfflineCommandParser`, `MultiProviderAiClient` (Gemini, Groq, OpenAI/Local Ollama + Fallback), `GeminiLiveClient`
- `:floatingassistant` — `AyshaForegroundService` + draggable floating glass orb & quick menu

## Build Instructions
1. Ensure JDK 21 + Android SDK (API 35) + Gradle 8.10.2 are available (or run `.github/workflows/build-apk.yml`).
2. Bundle the Apache-2.0 `vosk-model-small-en-in-0.4` into `wakeword/src/main/assets/model-en-in/` (optional; falls back to in-app download if absent).
3. Run unit tests: `gradle testDebugUnitTest`
4. Build APKs: `gradle assembleDebug assembleRelease`
