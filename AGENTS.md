# AGENTS.md — AYSHA AI Skill Management, Priority Routing & Engineering Rules

1. **Single Skill Directory:** Manage and check all installed skills inside `~/.gemini/antigravity/skills/` and `.agent/skills/`. Use `/antigravity-skills-manager` whenever asked to find, install, or update a skill (after Termux user confirmation for external package/repo installs).
2. **UI Priority Rule:** Whenever a task involves UI, UX, styling, layouts, animations, or frontend components, prioritize Soft 3D Claymorphism (`CLAY_DAY` default palette + `SurfaceStyle.CLAY` + `Nunito` bold headings + 9 switchable themes) and `ui-ux-pro-max-skill` principles as the primary design authority.
3. **Task-Specific Routing:**
   - App cloning or reverse engineering -> use `REA` / `REA_Skill` (`mcp_config.json` `rea` server + static APK inspection) and document in `REA_REPORT.md`.
   - Complex debugging or architecture -> trace all dependencies, edge cases, and build checks step-by-step before editing.
   - Copying or modifying features -> always inspect exact target files and imports first; preserve 100% of existing working functionality (Firebase Google Login, Unique License Keys, Coupons, UPI checkout, Secret 18+ age verification & `"normal baat karo"` reset guard).
4. **Zero Shortcuts & Verification Rule:** Never write placeholders or TODOs, keep `PromptBuilder.kt` strictly `AYSHA`-branded, and never claim completion until `gradle testDebugUnitTest`, `:app:lintDebug`, `assembleRelease`, and `apksigner verify` pass with zero errors.
