# AYSHA – Personal AI Assistant (Android) Design Specification

- **App Name:** `AYSHA – Your Personal AI Assistant`
- **Package Name:** `com.aysha.assistant`
- **Target Platforms:** Android 8.0+ (`minSdk = 26`) up to Android 15 (`compileSdk = 35`, `targetSdk = 35`), ARM64 (`arm64-v8a`, `armeabi-v7a` supported, 4 GB RAM optimized).
- **Build Pipeline:** Multi-module Gradle project (`app`, `core`, `ai`, `voice`, `wakeword`, `automation`, `phonecontrol`, `memory`, `settings`, `floatingassistant`) built & unit-tested with Gradle 8.10.2 + AGP 8.7.3 + Kotlin 2.1.0 + Jetpack Compose + Room + Android Keystore.

## Core Principles
1. **100% Free-First:** Zero subscriptions, zero paywalls, zero ads, zero hidden limits, zero mandatory account sign-in.
2. **Offline-Capable by Default:** Works out-of-the-box with zero API keys via local command parsing, Android SpeechRecognizer / TextToSpeech, local Room database, and on-device Vosk wake-word detection.
3. **BYOK Multi-Provider + Auto Fallback:** Supports Google Gemini (REST + Live WebSocket), Groq, OpenAI-Compatible (OpenAI, OpenRouter, DeepSeek, Ollama/local endpoints), automatic fallback chain across configured providers, and graceful fallback to Offline Mode.
4. **Security & Privacy:** All API keys stored via AES-256-GCM wrapped in `AndroidKeyStore`. Never logged or included in exports. Sensitive actions (phone calls, SMS) require explicit user confirmation. Notifications and screen automation operate strictly after user-granted system permissions.
5. **Liquid Glass UI & Voice Orb:** Dark & Light Liquid Glass themes, customizable accent colors and transparency, keyboard-safe insets, live state transitions (`IDLE -> LISTENING -> PROCESSING -> SPEAKING -> ERROR`), and full multi-screen parity with the 15-screen visual specification.
