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

## 4. On-Demand Multi-Agent Team (Dormant = 0 Tokens Until Triggered)
Never wake up an agent that isn't needed for the current step. Keep all inter-agent handoff notes under 5 lines.

| Agent | Assigned Skill | Trigger (When Active) | Role |
|---|---|---|---|
| `@Boss-Atlas` | `oh-my-antigravity` / `/swarm` | Multi-agent task coordination | Splits complex tasks, calls only required specialist, verifies final output |
| `@Pixel-UI` | `ui-ux-pro-max-skill` | UI, theme, HUD orb, Compose screens, animations | Builds responsive modern UI & animations |
| `@Rea-Cloner` | `REA_Skill` | Reverse engineering or copying app/APK features | Inspects codebase/APK and rebuilds with parity |
| `@Brain-Deep` | `ultrathink` / `Gravitas` | Complex bugs, crashes, multi-file architecture | Deep planning & zero-bug implementation |
| `@Shield-Sec` | `security-core` + `ai-security-testing-agent` | Pre-release checks or security audits | Fixes API keys, WebView, and APK vulnerabilities |
