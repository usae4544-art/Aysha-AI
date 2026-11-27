# /new-feature Workflow

Use UI/UX Pro Max & Compose Claymorphism principles to design and build new screens in AYSHA AI:

1. Inspect existing `AyshaColors`, `GlassCard`, `ClayTopBar`, `ClayBottomNavBar`, `AppSettings`, and `AyshaViewModel` APIs before writing code.
2. Preserve default `CLAY_DAY` (soft ice-blue `#DCE8FD` canvas + sculpted white `#F7FAFF` clay surfaces + vivid `#2E86FF` pill buttons + Nunito bold headings) as well as all 9 switchable themes.
3. Follow Plan -> Build -> Review -> Verify (`gradle testDebugUnitTest`, `:app:lintDebug`, `assembleRelease`, `apksigner verify`).
