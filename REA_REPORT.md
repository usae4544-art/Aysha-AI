# REA Recon Report — Reference APK & Screenshot Analysis

- **Reference Binary**: `maya-v5.2.67.apk` (inspected alongside 40 live UI screenshots).
- **Primary Architecture & Design Language**:
  - Jetpack Compose UI using **3D Soft Claymorphism (`Clay Day` default)**: ice-blue ambient background (`#DCE8FD` -> `#E6EEFE`), white puffy bevelled cards (`#F7FAFF`), inset recessed search/chat/key inputs, and sky-blue glowing gradient pill CTAs (`#58ADFF` -> `#2474FF`) with `Nunito` rounded bold typography.
  - **Shared App Chrome**:
    - Top header bar with circular floating Clay Menu (`☰`) / Back (`←`) button + screen title + circular Notification Bell (`🔔`) with pink badge dot.
    - Left sliding Clay Drawer (`HOME`, `KAAM-KAAJ`, `SYSTEM`, `AUR`) with live search filter (`Settings me dhundo`).
    - Floating 5-slot Bottom Navigation Dock (`Home`, `Scan`, central protruding Mic/Stop orb button, `Yaadein`, `Chat`).
  - **Core Screens Mapped**:
    1. **Home**: Greeting (`Abhi tak jaag rahe ho, {name}`), top-right `Energy` tile, Center Neural Constellation Red HUD Orb (AGSL/Canvas + ECG signal & orbiting labels), 3 quick pills (`Gaane`, `Padhai`, `Journal`), 3 telemetry bento cards (`Mausam`, `Aaj`, `Mood`), recessed prompt bar, bottom 5-tab dock.
    2. **3-Step Setup Wizard**: Step 1 (`Apni Gemini key daalo`), Step 2 (`Microphone, Notifications, Camera, Phone, Contacts, SMS, Location, Gallery, Calls, Bluetooth`), Step 3 (`Battery optimisation`, `Overlay`, `Notification access`, `Accessibility`, `Default assistant`).
    3. **Scanner HUD**: Stealth viewfinder (`NEURAL LINK ACTIVE`), animated laser scan sweep over dashed reticle, `FLIP` / capture orb / `REC`.
    4. **Yaadein & Chat**: `Mere baare me`, `Sikhe hue tasks`, `Baat ke baad khud yaad rakhe` toggle; animated Chat bubbles with `Meaning`, `Purani baatein`, `Normal baat karo` quick chips, Clear confirmation.
    5. **Accessibility Recovery**: Status banner, Battery / Autostart (Xiaomi/Oppo/Realme/Vivo) / Restricted Settings fix rows, ADB `WRITE_SECURE_SETTINGS` one-tap helper.
    6. **Games Zone, Notifications Feed, About + App ki bhasha + Open-Source Licences, Secret 18+ Mode**: Terms -> Birth Year (`>=18`) -> R-rated details & 2-step confirmation -> instant `"normal baat karo"` reset.
- **Preserved AYSHA Systems**: Firebase Google Login (`app-auth.html` / `aysha://auth/callback`), unique long license keys, coupon deactivation, amount-locked UPI QR (`shivansh7dubey91@okhdfcbank`), AES-256 Keystore secret storage, Multi-Provider AI (`Gemini`, `Groq`, `OpenAI`, `Local`, `Offline`), Vosk wake-word, and Room memory database.
