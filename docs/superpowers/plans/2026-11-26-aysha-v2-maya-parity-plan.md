# AYSHA v2.0.0 Implementation Plan

1. Add MIT `LICENSE` and bundle SIL OFL fonts (`Inter`, `Nunito`, `JetBrains Mono`) into `:core`.
2. Upgrade `:core` (`CoreModels.kt`, `LiquidGlassTheme.kt`) with Maya palettes, surfaces, corners, scales, font typography, orb styles, moods, sub-agents, rules, documents, and awareness logs.
3. Upgrade `:settings` (`SettingsRepository.kt`) to persist all new settings and support safe Backup/Restore JSON.
4. Upgrade `:memory` (`AyshaDatabase.kt`, `MemoryHelpers.kt`, `MemoryRepository.kt`) with Room v2 migration, `UserRuleEntity`, `KnowledgeDocumentEntity`, synonym+TF-IDF semantic `MemoryMatcher`, archive/restore, and expanded `BuiltInMacros`.
5. Upgrade `:voice` & `:wakeword` (`VoiceController.kt`, `BargeInDetector.kt`, `WakeWordEngine.kt`, `WakeWordStatusBus.kt`) with AEC/NS barge-in monitoring, follow-up window orchestration, awareness sensitivity thresholds, and decision log streaming.
6. Upgrade `:ai`, `:phonecontrol`, `:floatingassistant` (`PromptBuilder.kt`, `SmartMemoryExtractor.kt`, `ToolRegistry.kt`, `ToolExecutor.kt`, `OfflineCommandParser.kt`, `PhoneControlExecutor.kt`, `AyshaForegroundService.kt`) with rules injection, mood tracking, sub-agent routing, whiteboard/SOS tools, and upgraded floating overlay.
7. Upgrade `:app` UI (`AyshaVoiceOrb.kt`, `HomeOrbScreen.kt`, `AppearanceScreen.kt`, `SettingsHubScreen.kt`, `MemoryHistoryScreen.kt`, `VoiceAndLanguageScreen.kt`, `PrivacyDashboardScreen.kt`, plus new `ScannerScreen.kt`, `WhiteboardScreen.kt`, `BrowserScreen.kt`, `RulesAndAgentsScreen.kt`, `LicensesScreen.kt`) and `AyshaViewModel.kt` + `MainActivity.kt`.
8. Build the pure-black professional website (`site/`) and add `.github/workflows/deploy-site.yml` + update `.github/workflows/build-apk.yml` for `v2.0.0`.
9. Expand unit tests across modules, push to GitHub, verify both GitHub Actions workflows succeed, verify GitHub Pages live site, and download verified `v2.0.0` APKs.
