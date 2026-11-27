# /new-component Workflow

1. Inspect target source file completely (layout, state management, handlers, theme tokens).
2. Replicate full visual & functional parity in the target Compose component using shared `LiquidGlassTheme.kt` / `MayaShellComponents.kt` primitives.
3. Wire cleanly into `MainActivity.kt` / `SettingsHubScreen.kt` / `AyshaViewModel.kt`.
4. Never leave TODOs or dead buttons; run full unit tests + lint + release APK build before finishing.
