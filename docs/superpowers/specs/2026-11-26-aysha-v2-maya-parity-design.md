# AYSHA v2.0.0 – Maya-Parity UI & Internal Engine Design Spec

- **App Name:** `AYSHA – Your Personal AI Assistant`
- **Package ID:** `com.aysha.assistant`
- **License:** MIT Open-Source License (100% Free, zero paywalls, zero subscriptions, zero ads)
- **Website:** Pure black (`#000000`) product & download site in `site/` deployed via GitHub Pages.

## Key Upgrades (v2.0.0)
1. **Maya Theme, Surface & Typography Engine (`:core`, `:settings`):**
   - 9 curated palettes (`Midnight`, `Obsidian`, `Abyss`, `Ember`, `Nocturne`, `Rosewood`, `Clay Navy`, `Clay Day`, `Daylight`).
   - 4 surface treatments (`Glass`, `Clay`, `Flat`, `Soft`), 4 corner radii (`Sharp`, `Soft`, `Rounded`, `Pillow`), UI scale (`Compact`..`Larger`), and bundled SIL OFL fonts (`Inter`, `Nunito`, `JetBrains Mono`, `System`, `Serif`).
2. **11 Signature Animated Orbs (`:app`):**
   - `AYSHA 2047`, `Aysha Nova`, `Aysha Nova 3D`, `Liquid Core`, `J.A.R.V.I.S.`, `J.A.R.V.I.S. 3D`, `F.R.I.D.A.Y.`, `Red HUD`, `Particle Swarm`, `Pulse Reactor`, `Ultron` + 9 HUD accent colors.
3. **Maya-Grade Internal ("Andar Ka") Engine (`:ai`, `:memory`, `:voice`, `:wakeword`, `:automation`, `:phonecontrol`):**
   - **Voice & Awareness:** Follow-up listening window (continue conversing without repeating wake word), speaker-mode Barge-in detector (`AcousticEchoCanceler` + `NoiseSuppressor` + RMS), Awareness sensitivity gate (`Reserved`, `Balanced`, `Alert`), media mute toggle, and live Awareness decision log.
   - **Semantic Memory, Rules & Documents:** Hinglish/English synonym-aware TF-IDF cosine ranking, memory source labels (`aapne bataya`, `baaton se samjha`, `document se`), archive/restore (`Purani yaadein`), Documents knowledge base chunking, and persistent **AYSHA Rules** (`ab se hamesha...`).
   - **Sub-Agents, Moods & Macros:** Specialized Sub-Agent modes (`Auto`, `Coding`, `Study`, `Research`, `Phone Control`, `Creative`, `Memory`), live Mood & Energy tracker, and expanded `BuiltInMacros` with execution count tracking.
4. **Maya-Parity Tools & Screens (`:app`):**
   - Camera Vision **Scanner** (*"Kya dikh raha hai?"*), interactive **Study Whiteboard**, in-app **Mini Browser** with AI page summary, **Rules & Sub-Agents Hub**, **Backup & Restore** (excluding API keys), **Emergency SOS**, and **Open-Source Licenses** viewer.
5. **Professional Pure-Black Website (`site/`) + CI/CD:**
   - Responsive black-theme site with interactive Canvas Orb switcher, live GitHub Releases APK download, architecture docs, privacy policy, and MIT license page, deployed automatically via GitHub Actions Pages workflow.
