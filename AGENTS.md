# AGENTS.md — AYSHA AI Master Rules (Ultra-Low Token + Skill Routing + Security)

## 1. Ultra-Low Token Execution
- **Targeted Reads Only:** Read specific line ranges (`StartLine` to `EndLine`) instead of whole large files.
- **Ignore Heavy Folders:** Never scan `node_modules/`, `dist/`, `build/`, `.next/`, `.git/`, `*.lock`, `*.log`, `*.apk`.
- **Concise Output:** Do not repeat unchanged code or write verbose summaries.

## 2. Smart Skill Routing
- **UI & Frontend:** Prioritize `ui-ux-pro-max-skill` + Soft 3D Claymorphism (`CLAY_DAY` default + 9 switchable themes).
- **App Rebuilding & Cloning:** Use `REA` / `REA_Skill` (`mcp_config.json` `rea` + `REA_REPORT.md`).
- **Deep Reasoning:** Use `ultrathink` / `Gravitas` for multi-file architecture & complex bugs.
- **Security Audit:** Use `security-core` + `ai-security-testing-agent` when auditing secrets, `AndroidManifest.xml`, auth, or network calls.

## 3. Code Quality, Safety & Verification
- Preserve 100% of existing working features (Firebase Google Login, Unique License Keys, Coupons, UPI checkout, Secret 18+ age verification & `"normal baat karo"` reset guard).
- Keep `PromptBuilder.kt` strictly `AYSHA`-branded.
- Never leave `// TODO` placeholders; always verify unit tests, lint, and signed release APK before finishing.
