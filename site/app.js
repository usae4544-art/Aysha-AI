(() => {
  const canvas = document.getElementById("orb-canvas");
  if (!canvas) return;
  const ctx = canvas.getContext("2d");

  const ORBS = {
    aysha2047: { label: "AYSHA 2047", primary: "#22d3ee", secondary: "#6366f1", line: "Namaste! Main Aysha hoon — bolo, aaj phone pe kya kaam karoon?" },
    liquid: { label: "Liquid Core", primary: "#14b8a6", secondary: "#22d3ee", line: "Living plasma core — continuous follow-up & barge-in active." },
    jarvis: { label: "J.A.R.V.I.S.", primary: "#f59e0b", secondary: "#ea580c", line: "HUD tactical circuits calibrated. Ready for commands." },
    reactor: { label: "Pulse Reactor", primary: "#ec4899", secondary: "#8b5cf6", line: "All 10 modular engines online. Zero subscriptions." },
    ultron: { label: "Ultron", primary: "#38bdf8", secondary: "#2563eb", line: "Accessibility macros & semantic memory vault unlocked." }
  };

  let activeOrb = "aysha2047";
  let audioLevel = 0.14;
  let pulseUntil = 0;
  let pointerX = 0;
  let pointerY = 0;
  let t = 0;

  const orbStageName = document.getElementById("orb-stage-name");
  const orbDemoLine = document.getElementById("orb-demo-line");
  const orbStatus = document.getElementById("orb-stage-status");

  document.querySelectorAll(".orb-tab").forEach((btn) => {
    btn.addEventListener("click", () => {
      document.querySelectorAll(".orb-tab").forEach((b) => b.classList.remove("active"));
      btn.classList.add("active");
      activeOrb = btn.dataset.orb || "aysha2047";
      const cfg = ORBS[activeOrb] || ORBS.aysha2047;
      if (orbStageName) orbStageName.textContent = cfg.label;
      if (orbDemoLine) orbDemoLine.textContent = cfg.line;
    });
  });

  function triggerPulse() {
    pulseUntil = performance.now() + 2600;
    if (orbStatus) orbStatus.textContent = "Listening · Barge-In Ready";
    setTimeout(() => {
      if (performance.now() >= pulseUntil - 60 && orbStatus) {
        orbStatus.textContent = "Ready · Obsidian";
      }
    }, 2650);
  }

  document.getElementById("sim-voice-btn")?.addEventListener("click", triggerPulse);
  canvas.addEventListener("click", triggerPulse);
  canvas.addEventListener("mousemove", (e) => {
    const r = canvas.getBoundingClientRect();
    pointerX = ((e.clientX - r.left) / r.width - 0.5) * 14;
    pointerY = ((e.clientY - r.top) / r.height - 0.5) * 14;
  });
  canvas.addEventListener("mouseleave", () => {
    pointerX = 0;
    pointerY = 0;
  });

  function render(now) {
    t = now * 0.001;
    const pulsing = now < pulseUntil;
    const targetAudio = pulsing ? 0.45 + 0.42 * Math.abs(Math.sin(t * 7.5)) : 0.12 + 0.05 * Math.sin(t * 2);
    audioLevel += (targetAudio - audioLevel) * 0.16;

    const w = canvas.width;
    const h = canvas.height;
    const cx = w / 2 + pointerX * 0.5;
    const cy = h / 2 + pointerY * 0.5;
    const r = 128;
    const cfg = ORBS[activeOrb] || ORBS.aysha2047;

    ctx.clearRect(0, 0, w, h);

    // Subtle radial ambient behind orb inside canvas only
    const halo = ctx.createRadialGradient(cx, cy, r * 0.2, cx, cy, r * 1.15);
    halo.addColorStop(0, cfg.primary + "44");
    halo.addColorStop(0.6, cfg.secondary + "1a");
    halo.addColorStop(1, "transparent");
    ctx.fillStyle = halo;
    ctx.beginPath();
    ctx.arc(cx, cy, r * 1.15, 0, Math.PI * 2);
    ctx.fill();

    if (activeOrb === "aysha2047") {
      for (let i = 0; i < 36; i++) {
        const a = (i * 10 * Math.PI) / 180 + t * 0.35;
        const inner = r * 0.84;
        const outer = r * (i % 3 === 0 ? 0.96 : 0.90) + audioLevel * 10;
        ctx.strokeStyle = i % 3 === 0 ? cfg.primary : "rgba(255,255,255,0.22)";
        ctx.lineWidth = i % 3 === 0 ? 2.4 : 1.2;
        ctx.beginPath();
        ctx.moveTo(cx + Math.cos(a) * inner, cy + Math.sin(a) * inner);
        ctx.lineTo(cx + Math.cos(a) * outer, cy + Math.sin(a) * outer);
        ctx.stroke();
      }
      ctx.strokeStyle = cfg.primary;
      ctx.lineWidth = 3.5;
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.74, t * 1.1, t * 1.1 + Math.PI * 1.45);
      ctx.stroke();

      ctx.strokeStyle = cfg.secondary;
      ctx.lineWidth = 2.2;
      ctx.beginPath();
      ctx.arc(cx, cy, r * 0.64, -t * 1.5, -t * 1.5 + Math.PI * 1.0);
      ctx.stroke();
    } else if (activeOrb === "liquid") {
      ctx.beginPath();
      const pts = 64;
      for (let i = 0; i <= pts; i++) {
        const ang = (i / pts) * Math.PI * 2;
        const wob = Math.sin(ang * 3 + t * 2.5) * 7 + Math.cos(ang * 5 - t * 3.1) * (5 + audioLevel * 18);
        const rad = r * 0.68 + wob;
        const x = cx + Math.cos(ang) * rad;
        const y = cy + Math.sin(ang) * rad;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.strokeStyle = cfg.primary;
      ctx.lineWidth = 3;
      ctx.stroke();
    } else if (activeOrb === "jarvis" || activeOrb === "reactor") {
      for (let ring = 0; ring < 3; ring++) {
        const factor = 0.86 - ring * 0.14;
        const dir = ring % 2 === 0 ? 1 : -1.3;
        ctx.strokeStyle = ring === 0 ? cfg.primary : cfg.secondary;
        ctx.lineWidth = 3.2 - ring * 0.6;
        for (let seg = 0; seg < 3; seg++) {
          const start = t * dir + (seg * Math.PI * 2) / 3 + ring * 0.6;
          ctx.beginPath();
          ctx.arc(cx, cy, r * factor, start, start + 1.25);
          ctx.stroke();
        }
      }
    } else if (activeOrb === "ultron") {
      ctx.beginPath();
      const sides = 8;
      for (let i = 0; i <= sides; i++) {
        const a = (i / sides) * Math.PI * 2 + t * 0.8;
        const rad = r * (i % 2 === 0 ? 0.82 : 0.72) + audioLevel * 10;
        const x = cx + Math.cos(a) * rad;
        const y = cy + Math.sin(a) * rad;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.strokeStyle = cfg.primary;
      ctx.lineWidth = 3;
      ctx.stroke();
    }

    // Core Sphere
    const coreRad = r * (0.48 + audioLevel * 0.08);
    const coreGrad = ctx.createRadialGradient(cx - 14, cy - 16, 4, cx, cy, coreRad);
    coreGrad.addColorStop(0, "#ffffffcc");
    coreGrad.addColorStop(0.35, cfg.primary);
    coreGrad.addColorStop(0.85, cfg.secondary);
    coreGrad.addColorStop(1, "#05070c");
    ctx.fillStyle = coreGrad;
    ctx.beginPath();
    ctx.arc(cx, cy, coreRad, 0, Math.PI * 2);
    ctx.fill();

    requestAnimationFrame(render);
  }
  requestAnimationFrame(render);

  // Hydrate live GitHub Release APK link & version
  fetch("https://api.github.com/repos/usae4544-art/Aysha-AI/releases/latest")
    .then((res) => (res.ok ? res.json() : null))
    .then((release) => {
      if (!release) return;
      const tag = release.tag_name || "v2.0.0";
      const assets = Array.isArray(release.assets) ? release.assets : [];
      const releaseApk = assets.find((a) => a.name && a.name.endsWith("-release.apk")) || assets.find((a) => a.name && a.name.endsWith(".apk"));

      const navVersion = document.getElementById("nav-version");
      const tagText = document.getElementById("release-tag-text");
      const sizeText = document.getElementById("release-size-text");
      const dlLabel = document.getElementById("hero-download-label");
      if (navVersion) navVersion.textContent = tag;
      if (tagText) tagText.textContent = tag;

      if (releaseApk && releaseApk.browser_download_url) {
        const mb = (releaseApk.size / (1024 * 1024)).toFixed(1);
        if (sizeText) sizeText.textContent = `${releaseApk.name} (${mb} MB)`;
        if (dlLabel) dlLabel.textContent = `Download ${tag} APK (${mb} MB)`;
        ["nav-download", "hero-download-btn", "footer-download-btn"].forEach((id) => {
          const el = document.getElementById(id);
          if (el) el.setAttribute("href", releaseApk.browser_download_url);
        });
      }
    })
    .catch(() => {});
})();
