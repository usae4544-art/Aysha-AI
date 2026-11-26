import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.1/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  browserLocalPersistence,
  setPersistence,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  signOut,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-auth.js";
import {
  getFirestore,
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  collection,
  query,
  where,
  getDocs,
  writeBatch,
  serverTimestamp,
  Timestamp,
  increment,
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-firestore.js";
import {
  getFunctions,
  httpsCallable,
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-functions.js";

const ADMIN_EMAIL = "usae4544@gmail.com";

const PLANS = [
  { id: "ONE_MONTH", name: "1 Month", days: 30, periodLabel: "30 days", badge: "" },
  { id: "TWO_MONTHS", name: "2 Months", days: 60, periodLabel: "60 days", badge: "" },
  { id: "ONE_YEAR", name: "1 Year", days: 365, periodLabel: "365 days", badge: "Popular" },
  { id: "TWO_YEARS", name: "2 Years", days: 730, periodLabel: "730 days", badge: "" },
  { id: "LIFETIME", name: "Lifetime", days: 0, periodLabel: "All-time access", badge: "All-time" },
];

const DEFAULT_STORE = {
  upiId: "shivansh7dubey91@okhdfcbank",
  upiName: "Shivansh Pandit",
  currency: "INR",
  prices: {
    ONE_MONTH: 149,
    TWO_MONTHS: 249,
    ONE_YEAR: 999,
    TWO_YEARS: 1599,
    LIFETIME: 2999,
  },
};

const state = {
  config: null,
  firebaseReady: false,
  authBusy: false,
  auth: null,
  db: null,
  functions: null,
  user: null,
  isAdmin: false,
  license: null,
  store: structuredClone(DEFAULT_STORE),
  appliedCoupon: null,
  pendingLicenseKeyDoc: null,
  checkoutPlan: null,
  adminUsers: [],
  adminLicensesMap: new Map(),
  adminCoupons: [],
  adminKeys: [],
  adminPayments: [],
  myPayments: [],
  myRedemptions: [],
};

function generateLicenseKey() {
  const alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const bytes = new Uint8Array(24);
  crypto.getRandomValues(bytes);
  let raw = "";
  for (let i = 0; i < bytes.length; i++) {
    raw += alphabet[bytes[i] % alphabet.length];
  }
  return `AYSHA-${raw.slice(0, 4)}-${raw.slice(4, 8)}-${raw.slice(8, 12)}-${raw.slice(12, 16)}-${raw.slice(16, 20)}-${raw.slice(20, 24)}`;
}

const $ = (id) => document.getElementById(id);

function esc(str) {
  return String(str ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function showToast(msg) {
  const el = $("toast");
  el.textContent = msg;
  el.classList.remove("hidden");
  clearTimeout(showToast._t);
  showToast._t = setTimeout(() => el.classList.add("hidden"), 4500);
}

function showAuthError(msg) {
  const box = $("auth-error-box");
  if (!box) return;
  box.textContent = msg;
  box.classList.remove("hidden");
}

function clearAuthError() {
  const box = $("auth-error-box");
  if (!box) return;
  box.textContent = "";
  box.classList.add("hidden");
}

function friendlyAuthError(err) {
  const code = err?.code || "";
  if (code === "auth/unauthorized-domain") {
    return "Add usae4544-art.github.io in Firebase Console -> Authentication -> Settings -> Authorized domains.";
  }
  if (code === "auth/operation-not-allowed") {
    return "Enable Google provider in Firebase Console -> Authentication -> Sign-in method.";
  }
  if (code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request") {
    return "Google sign-in popup was closed before completing. Tap Sign in with Google again.";
  }
  if (code === "auth/popup-blocked") {
    return "Browser blocked the popup. Redirecting to Google Sign-In...";
  }
  if (code === "auth/web-storage-unsupported" || code === "auth/operation-not-supported-in-this-environment") {
    return "In-app browser blocked storage. Open https://usae4544-art.github.io/Aysha-AI/ in Chrome.";
  }
  return `${code ? code + ": " : ""}${err?.message || "Google sign-in failed."}`;
}

function withTimeout(promise, ms = 4000) {
  return Promise.race([
    promise,
    new Promise((_, reject) => setTimeout(() => reject(new Error("timeout")), ms)),
  ]);
}

function planById(planId) {
  return PLANS.find((p) => p.id === planId) || { id: planId, name: planId, days: 30, periodLabel: "" };
}

function toMs(val) {
  if (!val) return 0;
  if (typeof val === "number") return val;
  if (typeof val.toMillis === "function") return val.toMillis();
  if (val.seconds) return val.seconds * 1000;
  return 0;
}

function formatCountdown(targetMs) {
  const diff = targetMs - Date.now();
  if (diff <= 0) return "Expired";
  const sec = Math.floor(diff / 1000);
  const d = Math.floor(sec / 86400);
  const h = Math.floor((sec % 86400) / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (d > 0) return `${d}d ${String(h).padStart(2, "0")}h ${String(m).padStart(2, "0")}m`;
  return `${String(h).padStart(2, "0")}h ${String(m).padStart(2, "0")}m ${String(s).padStart(2, "0")}s`;
}

function formatDate(ms) {
  if (!ms) return "—";
  return new Date(ms).toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function isUserAdmin(user) {
  return Boolean(
    user &&
    user.emailVerified === true &&
    String(user.email || "").toLowerCase() === ADMIN_EMAIL
  );
}

function isLicenseActive(lic) {
  if (!lic || lic.status !== "active") return false;
  if (lic.isLifetime || lic.planId === "LIFETIME") return true;
  const expMs = Math.max(toMs(lic.expiresAt), Number(lic.expiresAtMs || 0));
  return expMs > Date.now();
}

function licenseExpiryMs(lic) {
  if (!lic) return 0;
  return Math.max(toMs(lic.expiresAt), Number(lic.expiresAtMs || 0));
}

function discountedPrice(planId) {
  const base = Number(state.store.prices?.[planId] ?? DEFAULT_STORE.prices[planId] ?? 0);
  const c = state.appliedCoupon;
  if (!c) return { base, final: base, discountPercent: 0, code: null };
  const applies = c.rewardPlanId === "ANY" || c.rewardPlanId === planId;
  if (!applies) return { base, final: base, discountPercent: 0, code: null };
  const pct = c.rewardType === "free_license" ? 100 : Math.min(100, Math.max(0, Number(c.discountPercent || 0)));
  const final = Math.max(0, Math.round(base * (100 - pct) / 100));
  return { base, final, discountPercent: pct, code: c.code };
}

/* --- Subtle monochrome/cyan HUD Orb Canvas --- */
function startOrbLoop() {
  const canvas = $("orb-canvas");
  if (!canvas) return;
  const ctx = canvas.getContext("2d");
  if (!ctx) return;

  let t = 0;
  const draw = () => {
    const w = canvas.width;
    const h = canvas.height;
    const cx = w / 2;
    const cy = h / 2;
    ctx.clearRect(0, 0, w, h);

    const active = state.isAdmin || isLicenseActive(state.license);
    const accent = active ? "#10b981" : state.user ? "#22d3ee" : "#52525b";

    // Outer calibrated rings
    ctx.strokeStyle = "rgba(255,255,255,0.08)";
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.arc(cx, cy, 82, 0, Math.PI * 2);
    ctx.stroke();

    ctx.strokeStyle = accent;
    ctx.lineWidth = 1.6;
    for (let i = 0; i < 3; i++) {
      const a0 = t * (0.4 + i * 0.25) + (i * Math.PI * 2) / 3;
      ctx.beginPath();
      ctx.arc(cx, cy, 62 + i * 10, a0, a0 + 1.35);
      ctx.stroke();
    }

    // Inner waveform ticks
    const tickCount = 36;
    for (let i = 0; i < tickCount; i++) {
      const ang = (i / tickCount) * Math.PI * 2 + t * 0.15;
      const amp = 4 + Math.sin(t * 2.4 + i * 0.65) * (active ? 6 : 3);
      const r1 = 38;
      const r2 = r1 + amp;
      ctx.strokeStyle = i % 3 === 0 ? accent : "rgba(237,237,237,0.35)";
      ctx.lineWidth = 1.4;
      ctx.beginPath();
      ctx.moveTo(cx + Math.cos(ang) * r1, cy + Math.sin(ang) * r1);
      ctx.lineTo(cx + Math.cos(ang) * r2, cy + Math.sin(ang) * r2);
      ctx.stroke();
    }

    // Core dot
    ctx.fillStyle = accent;
    ctx.beginPath();
    ctx.arc(cx, cy, 4, 0, Math.PI * 2);
    ctx.fill();

    t += 0.016;
    requestAnimationFrame(draw);
  };
  requestAnimationFrame(draw);
}

/* --- Render functions --- */
function renderAccountPanel() {
  const user = state.user;
  const authBtn = $("btn-auth-action");
  const heroBtn = $("btn-hero-signin");
  const appSyncBtn = $("btn-open-app-auth");
  const badge = $("status-badge");
  const orbLabel = $("orb-mode-label");

  if (!user) {
    authBtn.querySelector("span").textContent = "Sign in with Google";
    heroBtn.textContent = "Sign in with Google";
    $("btn-copy-my-key").classList.add("hidden");
    $("acc-key-row").classList.add("hidden");
    appSyncBtn.classList.add("hidden");
    badge.className = "status-pill status-neutral";
    badge.textContent = "Signed out";
    $("acc-email").textContent = "Not signed in";
    $("acc-plan").textContent = "—";
    $("acc-timer").textContent = "—";
    $("acc-key").textContent = "—";
    orbLabel.textContent = "AYSHA 2047 · Signed out";
  } else {
    authBtn.querySelector("span").textContent = "Sign out";
    heroBtn.textContent = `Signed in (${user.email})`;
    appSyncBtn.classList.remove("hidden");
    $("acc-email").textContent = user.email || user.uid;

    const activeKey = state.license?.licenseKey || "";
    if (activeKey && (state.isAdmin || isLicenseActive(state.license))) {
      $("acc-key").textContent = activeKey;
      $("acc-key-row").classList.remove("hidden");
      $("btn-copy-my-key").classList.remove("hidden");
    } else {
      $("acc-key").textContent = "—";
      $("acc-key-row").classList.add("hidden");
      $("btn-copy-my-key").classList.add("hidden");
    }

    if (state.isAdmin) {
      badge.className = "status-pill status-ok";
      badge.textContent = "Owner Admin";
      $("acc-plan").textContent = "Lifetime (Admin)";
      $("acc-timer").textContent = "Unlimited";
      orbLabel.textContent = "AYSHA 2047 · Owner active";
    } else if (isLicenseActive(state.license)) {
      const plan = planById(state.license.planId);
      const isLife = state.license.isLifetime || state.license.planId === "LIFETIME";
      badge.className = "status-pill status-ok";
      badge.textContent = "License Active";
      $("acc-plan").textContent = plan.name;
      $("acc-timer").textContent = isLife ? "All-time" : formatCountdown(licenseExpiryMs(state.license));
      orbLabel.textContent = `AYSHA 2047 · ${plan.name}`;
    } else if (state.license?.status === "revoked") {
      badge.className = "status-pill status-err";
      badge.textContent = "Revoked";
      $("acc-plan").textContent = "None";
      $("acc-timer").textContent = "Access revoked";
      orbLabel.textContent = "AYSHA 2047 · Locked";
    } else {
      badge.className = "status-pill status-warn";
      badge.textContent = "No active license";
      $("acc-plan").textContent = "None";
      $("acc-timer").textContent = "Select a plan or redeem coupon";
      orbLabel.textContent = "AYSHA 2047 · License required";
    }
  }

  // Strictly hide or reveal Admin UI based ONLY on verified usae4544@gmail.com
  const adminNav = $("nav-admin-link");
  const adminSec = $("admin-console");
  if (state.isAdmin) {
    adminNav.classList.remove("hidden");
    adminSec.classList.remove("hidden");
    $("admin-email-badge").textContent = user.email;
  } else {
    adminNav.classList.add("hidden");
    adminSec.classList.add("hidden");
  }
}

function renderPlans() {
  const grid = $("plans-grid");
  grid.innerHTML = PLANS.map((p) => {
    const pricing = discountedPrice(p.id);
    const hasDiscount = pricing.discountPercent > 0;
    const isFreeNow = hasDiscount && pricing.final === 0;
    const featured = p.id === "ONE_YEAR" ? " plan-featured" : "";

    return `
      <div class="plan-card${featured}">
        <div>
          <div class="plan-name">${esc(p.name)}</div>
          <div class="plan-duration">${esc(p.periodLabel)}</div>
        </div>
        <div>
          <span class="plan-price mono">&#8377;${pricing.final}</span>
          ${hasDiscount ? `<span class="plan-strike mono">&#8377;${pricing.base}</span>` : ""}
        </div>
        <ul class="plan-meta">
          <li>Google OAuth APK sync</li>
          <li>All 11 HUD orbs &amp; themes</li>
          <li>Full voice, memory &amp; automation</li>
        </ul>
        <button type="button" class="btn ${p.id === "ONE_YEAR" ? "btn-primary" : "btn-secondary"} btn-full btn-select-plan" data-plan="${esc(p.id)}">
          ${isFreeNow ? "Activate Free" : "Choose " + esc(p.name)}
        </button>
      </div>
    `;
  }).join("");

  grid.querySelectorAll(".btn-select-plan").forEach((btn) => {
    btn.addEventListener("click", () => handleSelectPlan(btn.dataset.plan));
  });
}

function renderMyHistory() {
  const panel = $("my-requests-panel");
  const tbody = $("my-requests-tbody");
  if (!state.user || (state.myPayments.length === 0 && state.myRedemptions.length === 0)) {
    panel.classList.add("hidden");
    tbody.innerHTML = "";
    return;
  }

  panel.classList.remove("hidden");
  const rows = [];

  for (const r of state.myRedemptions) {
    const ts = toMs(r.redeemedAt);
    const desc = r.rewardType === "free_license" || Number(r.discountPercent) >= 100
      ? "Free license"
      : `${r.discountPercent}% off coupon`;
    rows.push({
      ts,
      html: `<tr>
        <td class="mono">${esc(formatDate(ts))}</td>
        <td>${esc(planById(r.rewardPlanId).name)}</td>
        <td class="mono">${esc(desc)}</td>
        <td class="mono">${esc(r.code)}</td>
        <td><span class="status-pill status-ok">Redeemed</span></td>
      </tr>`,
    });
  }

  for (const p of state.myPayments) {
    const ts = toMs(p.createdAt);
    const pillCls = p.status === "approved" ? "status-ok" : p.status === "rejected" ? "status-err" : "status-warn";
    rows.push({
      ts,
      html: `<tr>
        <td class="mono">${esc(formatDate(ts))}</td>
        <td>${esc(planById(p.planId).name)}</td>
        <td class="mono">&#8377;${Number(p.finalPriceInr || 0)}</td>
        <td class="mono">${esc(p.utr || "—")}</td>
        <td><span class="status-pill ${pillCls}">${esc(p.status)}</span></td>
      </tr>`,
    });
  }

  rows.sort((a, b) => b.ts - a.ts);
  tbody.innerHTML = rows.map((r) => r.html).join("");
}

function renderAdminTables() {
  if (!state.isAdmin) return;

  // Stats
  $("stat-users").textContent = String(state.adminUsers.length);
  const activeLicCount = Array.from(state.adminLicensesMap.values()).filter(isLicenseActive).length;
  $("stat-active").textContent = String(activeLicCount);
  const liveCoupons = state.adminCoupons.filter((c) => c.active && toMs(c.expiresAt) > Date.now() && Number(c.usedCount || 0) < Number(c.maxUses || 0));
  $("stat-coupons").textContent = String(liveCoupons.length);
  const pendingReqs = state.adminPayments.filter((p) => p.status === "pending");
  $("stat-pending").textContent = String(pendingReqs.length);

  // Coupons table
  const cBody = $("admin-coupons-tbody");
  cBody.innerHTML = state.adminCoupons.map((c) => {
    const expMs = Math.max(toMs(c.expiresAt), Number(c.expiresAtMs || 0));
    const expired = expMs <= Date.now();
    const used = Number(c.usedCount || 0);
    const max = Number(c.maxUses || 0);
    const full = used >= max;
    const rewardText = c.rewardType === "free_license"
      ? `Free · ${planById(c.rewardPlanId).name}`
      : `${c.discountPercent}% off · ${c.rewardPlanId === "ANY" ? "Any plan" : planById(c.rewardPlanId).name}`;

    let statusPill = `<span class="status-pill status-ok">Live</span>`;
    if (!c.active) statusPill = `<span class="status-pill status-neutral">Deactivated</span>`;
    else if (expired) statusPill = `<span class="status-pill status-err">Expired</span>`;
    else if (full) statusPill = `<span class="status-pill status-warn">Quota full</span>`;

    return `<tr>
      <td class="mono"><strong>${esc(c.code)}</strong></td>
      <td>${esc(rewardText)}</td>
      <td class="mono">${used} / ${max}</td>
      <td class="mono" data-countdown-ms="${expMs}">${esc(formatCountdown(expMs))}</td>
      <td>${statusPill}</td>
      <td>
        <div class="row-actions">
          <button type="button" class="btn btn-ghost btn-sm" data-c-copy="${esc(c.code)}">Copy</button>
          <button type="button" class="btn btn-ghost btn-sm" data-c-toggle="${esc(c.code)}">${c.active ? "Deactivate" : "Activate"}</button>
          <button type="button" class="btn btn-danger btn-sm" data-c-del="${esc(c.code)}">Delete</button>
        </div>
      </td>
    </tr>`;
  }).join("") || `<tr><td colspan="6" class="muted-text">No coupons created yet.</td></tr>`;

  cBody.querySelectorAll("[data-c-copy]").forEach((b) => {
    b.addEventListener("click", () => {
      navigator.clipboard?.writeText(b.dataset.cCopy);
      showToast(`Copied code ${b.dataset.cCopy}`);
    });
  });
  cBody.querySelectorAll("[data-c-toggle]").forEach((b) => {
    b.addEventListener("click", () => toggleCouponActive(b.dataset.cToggle));
  });
  cBody.querySelectorAll("[data-c-del]").forEach((b) => {
    b.addEventListener("click", () => deleteCoupon(b.dataset.cDel));
  });

  // License Keys table
  const kBody = $("admin-keys-tbody");
  if (kBody) {
    kBody.innerHTML = state.adminKeys.map((k) => {
      const kStatus = !k.active ? "revoked" : (k.status || "available");
      const pill = kStatus === "available"
        ? `<span class="status-pill status-ok">Available</span>`
        : (kStatus === "active" || kStatus === "claimed")
          ? `<span class="status-pill status-neutral">${esc(kStatus)}</span>`
          : `<span class="status-pill status-err">Deactivated</span>`;
      const ownerText = k.claimedByEmail || k.email || (k.note ? `(${k.note})` : "Unclaimed");
      return `<tr>
        <td class="mono" style="font-size:11.5px;">${esc(k.key)}</td>
        <td class="mono">${esc(planById(k.planId).name)}</td>
        <td class="mono">${esc(ownerText)}</td>
        <td>${pill}</td>
        <td>
          <div class="row-actions">
            <button type="button" class="btn btn-ghost btn-sm" data-k-copy="${esc(k.key)}">Copy</button>
            <button type="button" class="btn btn-ghost btn-sm" data-k-toggle="${esc(k.key)}">${k.active ? "Deactivate" : "Activate"}</button>
            <button type="button" class="btn btn-danger btn-sm" data-k-del="${esc(k.key)}">Delete</button>
          </div>
        </td>
      </tr>`;
    }).join("") || `<tr><td colspan="5" class="muted-text">No license keys generated yet.</td></tr>`;

    kBody.querySelectorAll("[data-k-copy]").forEach((b) => {
      b.addEventListener("click", () => {
        navigator.clipboard?.writeText(b.dataset.kCopy);
        showToast("Copied license key.");
      });
    });
    kBody.querySelectorAll("[data-k-toggle]").forEach((b) => {
      b.addEventListener("click", () => toggleKeyActive(b.dataset.kToggle));
    });
    kBody.querySelectorAll("[data-k-del]").forEach((b) => {
      b.addEventListener("click", () => deleteLicenseKey(b.dataset.kDel));
    });
  }

  // Users table
  const filter = ($("admin-user-search").value || "").trim().toLowerCase();
  const uBody = $("admin-users-tbody");
  const filteredUsers = state.adminUsers.filter((u) => {
    if (!filter) return true;
    return String(u.email || "").toLowerCase().includes(filter) || String(u.uid || "").toLowerCase().includes(filter);
  });

  uBody.innerHTML = filteredUsers.map((u) => {
    const lic = state.adminLicensesMap.get(u.uid);
    const active = isLicenseActive(lic);
    const isLife = lic && (lic.isLifetime || lic.planId === "LIFETIME");
    const expMs = licenseExpiryMs(lic);
    const planLabel = lic?.planId && lic.planId !== "NONE" ? planById(lic.planId).name : "None";
    const keyShort = lic?.licenseKey ? esc(lic.licenseKey) : "—";
    const statusPill = active
      ? `<span class="status-pill status-ok">Active</span>`
      : lic?.status === "revoked"
        ? `<span class="status-pill status-err">Revoked</span>`
        : `<span class="status-pill status-neutral">Inactive</span>`;

    return `<tr>
      <td>
        <div>${esc(u.displayName || "User")}</div>
        <div class="mono muted-text">${esc(u.email || u.uid)}</div>
      </td>
      <td class="mono">${esc(planLabel)}</td>
      <td class="mono" style="font-size:11px;">
        ${keyShort !== "—" ? `${keyShort} <button type="button" class="btn btn-ghost btn-sm" data-u-copykey="${keyShort}">Copy</button>` : "—"}
      </td>
      <td class="mono" data-countdown-ms="${isLife || !active ? 0 : expMs}">${isLife && active ? "All-time" : active ? esc(formatCountdown(expMs)) : "—"}</td>
      <td>${statusPill}</td>
      <td>
        <div class="row-actions">
          <select class="grant-select" data-u-select="${esc(u.uid)}">
            <option value="ONE_MONTH">1 Month</option>
            <option value="TWO_MONTHS">2 Months</option>
            <option value="ONE_YEAR">1 Year</option>
            <option value="TWO_YEARS">2 Years</option>
            <option value="LIFETIME">Lifetime</option>
          </select>
          <button type="button" class="btn btn-secondary btn-sm" data-u-grant="${esc(u.uid)}">Grant</button>
          <button type="button" class="btn btn-danger btn-sm" data-u-revoke="${esc(u.uid)}">Revoke</button>
        </div>
      </td>
    </tr>`;
  }).join("") || `<tr><td colspan="6" class="muted-text">No users found.</td></tr>`;

  uBody.querySelectorAll("[data-u-copykey]").forEach((b) => {
    b.addEventListener("click", () => {
      navigator.clipboard?.writeText(b.dataset.uCopykey);
      showToast("Copied user license key.");
    });
  });

  uBody.querySelectorAll("[data-u-grant]").forEach((b) => {
    b.addEventListener("click", () => {
      const uid = b.dataset.uGrant;
      const sel = uBody.querySelector(`[data-u-select="${CSS.escape(uid)}"]`);
      adminSetLicense(uid, sel ? sel.value : "ONE_MONTH");
    });
  });
  uBody.querySelectorAll("[data-u-revoke]").forEach((b) => {
    b.addEventListener("click", () => adminRevokeLicense(b.dataset.uRevoke));
  });

  // Payment requests table
  const pBody = $("admin-payments-tbody");
  pBody.innerHTML = state.adminPayments.map((p) => {
    const pill = p.status === "approved" ? "status-ok" : p.status === "rejected" ? "status-err" : "status-warn";
    return `<tr>
      <td class="mono">${esc(p.email || p.uid)}</td>
      <td>${esc(planById(p.planId).name)}</td>
      <td class="mono">&#8377;${Number(p.finalPriceInr || 0)}</td>
      <td class="mono">${esc(p.couponCode || "—")}</td>
      <td class="mono">${esc(p.utr || "—")}</td>
      <td><span class="status-pill ${pill}">${esc(p.status)}</span></td>
      <td>
        ${p.status === "pending" ? `
          <div class="row-actions">
            <button type="button" class="btn btn-primary btn-sm" data-p-approve="${esc(p.id)}">Approve</button>
            <button type="button" class="btn btn-danger btn-sm" data-p-reject="${esc(p.id)}">Reject</button>
          </div>` : `<span class="muted-text">Resolved</span>`}
      </td>
    </tr>`;
  }).join("") || `<tr><td colspan="7" class="muted-text">No payment submissions yet.</td></tr>`;

  pBody.querySelectorAll("[data-p-approve]").forEach((b) => {
    b.addEventListener("click", () => adminResolvePayment(b.dataset.pApprove, "approved"));
  });
  pBody.querySelectorAll("[data-p-reject]").forEach((b) => {
    b.addEventListener("click", () => adminResolvePayment(b.dataset.pReject, "rejected"));
  });
}

/* --- Data loading --- */
async function loadStoreConfig() {
  if (!state.db) return;
  try {
    const snap = await withTimeout(getDoc(doc(state.db, "config", "store")), 4000);
    if (snap.exists()) {
      const data = snap.data();
      state.store = {
        upiId: data.upiId || state.store.upiId,
        upiName: data.upiName || state.store.upiName,
        currency: data.currency || "INR",
        prices: { ...state.store.prices, ...(data.prices || {}) },
      };
    }
  } catch (err) {
    console.warn("Store config fetch skipped:", err);
  }
  syncStoreFormInputs();
  renderPlans();
}

function syncStoreFormInputs() {
  if (!$("s-upi-id")) return;
  $("s-upi-id").value = state.store.upiId || "";
  $("s-upi-name").value = state.store.upiName || "";
  $("s-p-1m").value = state.store.prices.ONE_MONTH ?? 149;
  $("s-p-2m").value = state.store.prices.TWO_MONTHS ?? 249;
  $("s-p-1y").value = state.store.prices.ONE_YEAR ?? 999;
  $("s-p-2y").value = state.store.prices.TWO_YEARS ?? 1599;
  $("s-p-life").value = state.store.prices.LIFETIME ?? 2999;
}

async function syncUserRecords(user) {
  if (!state.db || !user) return;
  let firestoreIssue = null;

  const userWrite = setDoc(doc(state.db, "users", user.uid), {
    uid: user.uid,
    email: String(user.email || "").toLowerCase(),
    emailVerified: Boolean(user.emailVerified),
    displayName: user.displayName || "",
    photoUrl: user.photoURL || "",
    lastLoginAt: serverTimestamp(),
    lastLoginSource: "web",
  }, { merge: true });

  const [writeRes, licRes, payRes, redRes] = await Promise.allSettled([
    withTimeout(userWrite, 4500),
    withTimeout(getDoc(doc(state.db, "licenses", user.uid)), 4500),
    withTimeout(getDocs(query(collection(state.db, "payment_requests"), where("uid", "==", user.uid))), 4500),
    withTimeout(getDocs(query(collection(state.db, "redemptions"), where("uid", "==", user.uid))), 4500),
  ]);

  if (writeRes.status === "rejected") {
    firestoreIssue = writeRes.reason;
  }
  if (licRes.status === "fulfilled" && licRes.value.exists()) {
    state.license = licRes.value.data();
  } else {
    state.license = null;
  }
  state.myPayments = payRes.status === "fulfilled"
    ? payRes.value.docs.map((d) => ({ id: d.id, ...d.data() }))
    : [];
  state.myRedemptions = redRes.status === "fulfilled"
    ? redRes.value.docs.map((d) => ({ id: d.id, ...d.data() }))
    : [];

  if (firestoreIssue) {
    const msg = String(firestoreIssue?.message || firestoreIssue || "");
    if (msg.includes("does not exist") || msg.includes("not-found") || msg.includes("timeout") || msg.includes("offline")) {
      showAuthError("Signed in, but Cloud Firestore DB is not ready yet. Create Firestore Database + publish firebase/firestore.rules in Firebase Console.");
    } else if (msg.includes("permission-denied")) {
      showAuthError("Firestore rules denied access. Publish firebase/firestore.rules in Firebase Console -> Firestore -> Rules.");
    }
  } else {
    clearAuthError();
  }

  renderAccountPanel();
  renderMyHistory();

  if (state.isAdmin && !firestoreIssue) {
    await loadAdminAll();
  }
}

async function loadAdminAll() {
  if (!state.db || !state.isAdmin) return;
  try {
    const [uSnaps, lSnaps, cSnaps, pSnaps, kSnaps] = await withTimeout(Promise.all([
      getDocs(collection(state.db, "users")),
      getDocs(collection(state.db, "licenses")),
      getDocs(collection(state.db, "coupons")),
      getDocs(collection(state.db, "payment_requests")),
      getDocs(collection(state.db, "license_keys")),
    ]), 7000);

    state.adminUsers = uSnaps.docs.map((d) => d.data())
      .sort((a, b) => toMs(b.lastLoginAt) - toMs(a.lastLoginAt));
    state.adminLicensesMap = new Map();
    lSnaps.docs.forEach((d) => state.adminLicensesMap.set(d.id, d.data()));
    state.adminCoupons = cSnaps.docs.map((d) => ({ code: d.id, ...d.data() }))
      .sort((a, b) => toMs(b.createdAt) - toMs(a.createdAt));
    state.adminPayments = pSnaps.docs.map((d) => ({ id: d.id, ...d.data() }))
      .sort((a, b) => toMs(b.createdAt) - toMs(a.createdAt));
    state.adminKeys = kSnaps.docs.map((d) => ({ key: d.id, ...d.data() }))
      .sort((a, b) => toMs(b.createdAt) - toMs(a.createdAt));

    renderAdminTables();
  } catch (err) {
    showAuthError(`Admin load failed: ${err.message}`);
  }
}

/* --- Auth Actions --- */
async function triggerGoogleAuth() {
  if (!state.firebaseReady) {
    showAuthError("Still loading Firebase config or config is invalid.");
    $("setup-guide-panel").classList.remove("hidden");
    return;
  }
  if (state.authBusy) return;
  clearAuthError();

  if (state.user) {
    await signOut(state.auth);
    state.appliedCoupon = null;
    showToast("Signed out.");
    return;
  }

  state.authBusy = true;
  const heroBtn = $("btn-hero-signin");
  const prevHeroText = heroBtn.textContent;
  heroBtn.disabled = true;
  heroBtn.textContent = "Opening Google Sign-In...";

  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  try {
    await signInWithPopup(state.auth, provider);
  } catch (err) {
    const msg = friendlyAuthError(err);
    if (err.code === "auth/popup-blocked") {
      showAuthError(msg);
      try {
        await signInWithRedirect(state.auth, provider);
      } catch (redirErr) {
        showAuthError(friendlyAuthError(redirErr));
      }
    } else {
      showAuthError(msg);
      showToast(msg);
    }
  } finally {
    state.authBusy = false;
    heroBtn.disabled = false;
    if (!state.user) {
      heroBtn.textContent = prevHeroText;
    }
  }
}

/* --- Coupon lookup & redemption --- */
function showCouponFeedback(ok, htmlText) {
  const box = $("coupon-feedback");
  box.classList.remove("hidden", "ok", "err");
  box.classList.add(ok ? "ok" : "err");
  box.innerHTML = htmlText;
}

async function handleCheckCoupon(e) {
  e.preventDefault();
  if (!state.firebaseReady) {
    showToast("Configure Firebase first.");
    return;
  }
  if (!state.user) {
    showToast("Sign in with Google first to check or redeem a coupon.");
    await triggerGoogleAuth();
    return;
  }
  const raw = ($("coupon-input").value || "").trim().toUpperCase();
  if (!raw) {
    showCouponFeedback(false, "Enter a coupon code or license key.");
    return;
  }
  const freeBtn = $("btn-redeem-free");
  freeBtn.classList.add("hidden");
  state.appliedCoupon = null;
  state.pendingLicenseKeyDoc = null;
  renderPlans();

  try {
    const snap = await getDoc(doc(state.db, "coupons", raw));
    if (!snap.exists()) {
      if (raw.length >= 12) {
        const keySnap = await getDoc(doc(state.db, "license_keys", raw));
        if (keySnap.exists()) {
          const k = { key: raw, ...keySnap.data() };
          if (!k.active || k.status === "revoked") {
            showCouponFeedback(false, `License key <code>${esc(raw)}</code> has been deactivated.`);
            return;
          }
          if (k.status !== "available") {
            showCouponFeedback(false, `License key <code>${esc(raw)}</code> is already ${esc(k.status)}.`);
            return;
          }
          state.pendingLicenseKeyDoc = k;
          freeBtn.textContent = `Activate ${planById(k.planId).name} key`;
          freeBtn.classList.remove("hidden");
          showCouponFeedback(true, `Valid unique license key for <strong>${esc(planById(k.planId).name)}</strong>. Click Activate to unlock.`);
          return;
        }
      }
      showCouponFeedback(false, `Code or key <code>${esc(raw)}</code> does not exist.`);
      return;
    }
    const c = { code: raw, ...snap.data() };
    const expMs = Math.max(toMs(c.expiresAt), Number(c.expiresAtMs || 0));
    const left = Math.max(0, Number(c.maxUses || 0) - Number(c.usedCount || 0));

    if (!c.active) {
      showCouponFeedback(false, `Coupon <code>${esc(raw)}</code> was deactivated by admin.`);
      return;
    }
    if (Date.now() >= expMs) {
      showCouponFeedback(false, `Coupon <code>${esc(raw)}</code> expired on ${esc(formatDate(expMs))}.`);
      return;
    }
    if (left <= 0) {
      showCouponFeedback(false, `Coupon <code>${esc(raw)}</code> has reached its maximum user limit.`);
      return;
    }

    const redSnap = await getDoc(doc(state.db, "redemptions", `${raw}_${state.user.uid}`));
    if (redSnap.exists()) {
      showCouponFeedback(false, `You have already redeemed <code>${esc(raw)}</code> on this account.`);
      return;
    }

    state.appliedCoupon = c;
    renderPlans();

    const isFree = c.rewardType === "free_license" || Number(c.discountPercent || 0) >= 100;
    if (isFree) {
      const targetName = c.rewardPlanId === "ANY" ? "1 Month" : planById(c.rewardPlanId).name;
      showCouponFeedback(
        true,
        `Valid free license coupon <code>${esc(raw)}</code> (${esc(targetName)}) · ${left} uses left · Expires in <span class="mono">${esc(formatCountdown(expMs))}</span>.`
      );
      freeBtn.textContent = "Activate free license";
      freeBtn.classList.remove("hidden");
    } else {
      const targetText = c.rewardPlanId === "ANY" ? "any plan" : planById(c.rewardPlanId).name;
      showCouponFeedback(
        true,
        `Applied <strong>${Number(c.discountPercent)}% discount</strong> for ${esc(targetText)} · ${left} uses left · Expires in <span class="mono">${esc(formatCountdown(expMs))}</span>. Choose your plan below.`
      );
    }
  } catch (err) {
    showCouponFeedback(false, `Lookup error: ${esc(err.message)}`);
  }
}

async function handleRedeemFreeCoupon() {
  if (!state.user || !state.db) return;
  const btn = $("btn-redeem-free");

  if (state.pendingLicenseKeyDoc) {
    const k = state.pendingLicenseKeyDoc;
    btn.disabled = true;
    try {
      const uid = state.user.uid;
      const email = String(state.user.email || "").toLowerCase();
      const plan = planById(k.planId);
      const isLife = k.planId === "LIFETIME";
      const curActiveEnd = isLicenseActive(state.license) && !state.license?.isLifetime
        ? licenseExpiryMs(state.license)
        : Date.now();
      const newExpiryMs = isLife ? 0 : curActiveEnd + plan.days * 86400000;

      const batch = writeBatch(state.db);
      batch.update(doc(state.db, "license_keys", k.key), {
        status: "claimed",
        claimedByUid: uid,
        claimedByEmail: email,
        claimedAt: serverTimestamp(),
      });
      batch.set(doc(state.db, "licenses", uid), {
        uid,
        email,
        planId: k.planId,
        status: "active",
        isLifetime: isLife,
        expiresAt: isLife ? null : Timestamp.fromMillis(newExpiryMs),
        expiresAtMs: newExpiryMs,
        source: "license_key",
        licenseKey: k.key,
        note: `Activated license key ${k.key}`,
        updatedAt: serverTimestamp(),
        updatedBy: uid,
      }, { merge: true });
      await batch.commit();

      state.pendingLicenseKeyDoc = null;
      $("coupon-input").value = "";
      btn.classList.add("hidden");
      showCouponFeedback(true, `License key <code>${esc(k.key)}</code> activated (${esc(plan.name)})! Open the AYSHA APK to unlock.`);
      await syncUserRecords(state.user);
    } catch (err) {
      showCouponFeedback(false, `Key activation failed: ${esc(err.message)}`);
    } finally {
      btn.disabled = false;
    }
    return;
  }

  const c = state.appliedCoupon;
  if (!c) return;

  const targetPlanId = c.rewardPlanId && c.rewardPlanId !== "ANY" ? c.rewardPlanId : "ONE_MONTH";
  btn.disabled = true;

  try {
    if (state.config?.useCloudFunctions && state.functions) {
      const fn = httpsCallable(state.functions, "redeemFreeCoupon");
      await fn({ code: c.code, planId: targetPlanId });
    } else {
      const uid = state.user.uid;
      const email = String(state.user.email || "").toLowerCase();
      const plan = planById(targetPlanId);
      const isLife = targetPlanId === "LIFETIME";
      const curActiveEnd = isLicenseActive(state.license) && !state.license?.isLifetime
        ? licenseExpiryMs(state.license)
        : Date.now();
      const newExpiryMs = isLife ? 0 : curActiveEnd + plan.days * 86400000;
      const newKey = generateLicenseKey();

      const batch = writeBatch(state.db);
      batch.update(doc(state.db, "coupons", c.code), {
        usedCount: increment(1),
      });
      batch.set(doc(state.db, "redemptions", `${c.code}_${uid}`), {
        code: c.code,
        uid,
        email,
        rewardType: c.rewardType,
        rewardPlanId: targetPlanId,
        discountPercent: Number(c.discountPercent || 100),
        redeemedAt: serverTimestamp(),
      });
      batch.set(doc(state.db, "licenses", uid), {
        uid,
        email,
        planId: targetPlanId,
        status: "active",
        isLifetime: isLife,
        expiresAt: isLife ? null : Timestamp.fromMillis(newExpiryMs),
        expiresAtMs: newExpiryMs,
        source: "coupon",
        couponCode: c.code,
        licenseKey: newKey,
        note: `Redeemed ${c.code}`,
        updatedAt: serverTimestamp(),
        updatedBy: uid,
      }, { merge: true });
      batch.set(doc(state.db, "license_keys", newKey), {
        key: newKey,
        uid,
        email,
        planId: targetPlanId,
        status: "active",
        active: true,
        isLifetime: isLife,
        expiresAt: isLife ? null : Timestamp.fromMillis(newExpiryMs),
        expiresAtMs: newExpiryMs,
        createdAt: serverTimestamp(),
        updatedBy: uid,
      });

      await batch.commit();
    }

    state.appliedCoupon = null;
    $("coupon-input").value = "";
    btn.classList.add("hidden");
    showCouponFeedback(true, `Activated ${esc(planById(targetPlanId).name)} license with your unique license key. Open the AYSHA APK to unlock.`);
    await syncUserRecords(state.user);
    renderPlans();
  } catch (err) {
    showCouponFeedback(false, `Activation failed: ${esc(err.message)}`);
  } finally {
    btn.disabled = false;
  }
}

/* --- Checkout & UPI UTR --- */
async function handleSelectPlan(planId) {
  if (!state.firebaseReady) {
    showToast("Configure Firebase first.");
    return;
  }
  if (!state.user) {
    showToast("Sign in with Google to activate a plan.");
    await triggerGoogleAuth();
    return;
  }
  const pricing = discountedPrice(planId);
  if (pricing.code && pricing.final === 0) {
    await handleRedeemFreeCoupon();
    return;
  }

  state.checkoutPlan = planId;
  const plan = planById(planId);
  const exactAmt = Number(pricing.final).toFixed(2);
  const orderRef = `AYSHA${Date.now()}`;
  $("co-plan-name").textContent = `${plan.name} (${plan.periodLabel})`;
  $("co-base-price").textContent = `INR ${pricing.base}`;
  $("co-coupon-label").textContent = pricing.code ? `${pricing.code} (-${pricing.discountPercent}%)` : "None";
  $("co-final-price").textContent = `INR ${exactAmt}`;
  $("co-upi-name").textContent = state.store.upiName || "Shivansh Pandit";
  $("co-upi-id").textContent = state.store.upiId;
  $("co-feedback").classList.add("hidden");
  $("co-utr-input").value = "";

  // Lock exact payable amount using both am= and mam= per NPCI UPI spec
  const upiUri = `upi://pay?pa=${encodeURIComponent(state.store.upiId)}&pn=${encodeURIComponent(state.store.upiName)}&am=${exactAmt}&mam=${exactAmt}&cu=INR&tr=${encodeURIComponent(orderRef)}&tn=${encodeURIComponent("AYSHA " + plan.name)}`;
  $("co-upi-deep-link").href = upiUri;

  const qrCanvas = $("upi-qr-canvas");
  const saveQrBtn = $("btn-save-qr");
  qrCanvas.classList.add("hidden");
  saveQrBtn.classList.add("hidden");
  import("https://cdn.jsdelivr.net/npm/qrcode@1.5.4/+esm")
    .then(async (mod) => {
      const QRCode = mod.default || mod;
      await QRCode.toCanvas(qrCanvas, upiUri, {
        width: 200,
        margin: 2,
        color: { dark: "#000000", light: "#ffffff" },
      });
      qrCanvas.classList.remove("hidden");
      saveQrBtn.classList.remove("hidden");
    })
    .catch(() => {
      qrCanvas.classList.add("hidden");
      saveQrBtn.classList.add("hidden");
    });

  $("checkout-modal").classList.remove("hidden");
}

async function handleUtrSubmit(e) {
  e.preventDefault();
  if (!state.user || !state.db || !state.checkoutPlan) return;
  const utr = ($("co-utr-input").value || "").trim();
  if (utr.length < 6) {
    showToast("Enter a valid UTR / transaction ID (6+ characters).");
    return;
  }

  const btn = $("btn-submit-utr");
  btn.disabled = true;
  const pricing = discountedPrice(state.checkoutPlan);
  const reqId = `${state.user.uid}_${Date.now()}`;

  try {
    const reqRef = doc(state.db, "payment_requests", reqId);
    const reqPayload = {
      uid: state.user.uid,
      email: String(state.user.email || "").toLowerCase(),
      displayName: state.user.displayName || "",
      planId: state.checkoutPlan,
      basePriceInr: pricing.base,
      discountPercent: pricing.discountPercent,
      finalPriceInr: pricing.final,
      couponCode: pricing.code || null,
      utr,
      status: "pending",
      createdAt: serverTimestamp(),
    };

    if (pricing.code && state.appliedCoupon) {
      const batch = writeBatch(state.db);
      batch.set(reqRef, reqPayload);
      batch.update(doc(state.db, "coupons", pricing.code), {
        usedCount: increment(1),
      });
      batch.set(doc(state.db, "redemptions", `${pricing.code}_${state.user.uid}`), {
        code: pricing.code,
        uid: state.user.uid,
        email: String(state.user.email || "").toLowerCase(),
        rewardType: "percent_off",
        rewardPlanId: state.checkoutPlan,
        discountPercent: pricing.discountPercent,
        paymentRequestId: reqId,
        redeemedAt: serverTimestamp(),
      });
      await batch.commit();
      state.appliedCoupon = null;
      renderPlans();
    } else {
      await setDoc(reqRef, reqPayload);
    }

    $("co-feedback").textContent = "UTR submitted. Waiting for administrator approval.";
    $("co-feedback").classList.remove("hidden");
    showToast("Payment request submitted for approval.");
    await syncUserRecords(state.user);
  } catch (err) {
    showToast(`Submission failed: ${err.message}`);
  } finally {
    btn.disabled = false;
  }
}

/* --- Admin Actions --- */
async function handleCreateCoupon(e) {
  e.preventDefault();
  if (!state.isAdmin || !state.db) return;

  const code = ($("c-code").value || "").trim().toUpperCase().replace(/[^A-Z0-9_-]/g, "");
  const rewardType = $("c-type").value;
  const rewardPlanId = $("c-plan").value;
  const discountPercent = rewardType === "free_license" ? 100 : Math.min(100, Math.max(1, Number($("c-discount").value || 10)));
  const maxUses = Math.max(1, Number($("c-max-uses").value || 1));
  const expInput = $("c-expires-at").value;

  if (!code || code.length < 3) {
    showToast("Coupon code must be at least 3 alphanumeric characters.");
    return;
  }
  if (rewardType === "free_license" && rewardPlanId === "ANY") {
    showToast("Choose a specific plan (1 Month, 2 Months, 1 Year, 2 Years, or Lifetime) for free-license coupons.");
    return;
  }
  const expiresAtMs = new Date(expInput).getTime();
  if (!Number.isFinite(expiresAtMs) || expiresAtMs <= Date.now()) {
    showToast("Expiration date/time must be in the future.");
    return;
  }

  try {
    await setDoc(doc(state.db, "coupons", code), {
      code,
      rewardType,
      rewardPlanId,
      discountPercent,
      maxUses,
      usedCount: 0,
      active: true,
      expiresAt: Timestamp.fromMillis(expiresAtMs),
      expiresAtMs,
      createdAt: serverTimestamp(),
      createdBy: state.user.email,
    });
    showToast(`Published coupon ${code}`);
    $("c-code").value = "";
    await loadAdminAll();
  } catch (err) {
    showToast(`Create failed: ${err.message}`);
  }
}

async function toggleCouponActive(code) {
  if (!state.isAdmin || !state.db) return;
  const existing = state.adminCoupons.find((c) => c.code === code);
  if (!existing) return;
  try {
    await updateDoc(doc(state.db, "coupons", code), { active: !existing.active });
    await loadAdminAll();
  } catch (err) {
    showToast(err.message);
  }
}

async function deleteCoupon(code) {
  if (!state.isAdmin || !state.db) return;
  try {
    await deleteDoc(doc(state.db, "coupons", code));
    showToast(`Deleted coupon ${code}`);
    await loadAdminAll();
  } catch (err) {
    showToast(err.message);
  }
}

async function handleCreateLicenseKey(e) {
  e.preventDefault();
  if (!state.isAdmin || !state.db) return;
  const planId = $("k-plan").value;
  const note = ($("k-note").value || "").trim();
  const key = generateLicenseKey();
  try {
    await setDoc(doc(state.db, "license_keys", key), {
      key,
      planId,
      status: "available",
      active: true,
      note,
      createdAt: serverTimestamp(),
      updatedBy: state.user.email,
    });
    $("k-note").value = "";
    navigator.clipboard?.writeText(key);
    showToast(`Generated & copied key: ${key}`);
    await loadAdminAll();
  } catch (err) {
    showToast(`Key create failed: ${err.message}`);
  }
}

async function toggleKeyActive(key) {
  if (!state.isAdmin || !state.db) return;
  const existing = state.adminKeys.find((k) => k.key === key);
  if (!existing) return;
  try {
    const nextActive = !existing.active;
    await updateDoc(doc(state.db, "license_keys", key), {
      active: nextActive,
      status: nextActive ? (existing.claimedByUid ? "claimed" : existing.uid ? "active" : "available") : "revoked",
    });
    showToast(nextActive ? "License key activated." : "License key deactivated.");
    await loadAdminAll();
  } catch (err) {
    showToast(err.message);
  }
}

async function deleteLicenseKey(key) {
  if (!state.isAdmin || !state.db) return;
  try {
    await deleteDoc(doc(state.db, "license_keys", key));
    showToast("License key deleted.");
    await loadAdminAll();
  } catch (err) {
    showToast(err.message);
  }
}

async function adminSetLicense(uid, planId) {
  if (!state.isAdmin || !state.db) return;
  try {
    if (state.config?.useCloudFunctions && state.functions) {
      await httpsCallable(state.functions, "adminGrantLicense")({ uid, planId, action: "grant" });
    } else {
      const plan = planById(planId);
      const isLife = planId === "LIFETIME";
      const expiresAtMs = isLife ? 0 : Date.now() + plan.days * 86400000;
      const userRec = state.adminUsers.find((u) => u.uid === uid);
      const userEmail = String(userRec?.email || "").toLowerCase();
      const newKey = generateLicenseKey();

      const batch = writeBatch(state.db);
      batch.set(doc(state.db, "licenses", uid), {
        uid,
        email: userEmail,
        planId,
        status: "active",
        isLifetime: isLife,
        expiresAt: isLife ? null : Timestamp.fromMillis(expiresAtMs),
        expiresAtMs,
        source: "admin_grant",
        licenseKey: newKey,
        note: `Granted ${plan.name} by admin`,
        updatedAt: serverTimestamp(),
        updatedBy: state.user.email,
      }, { merge: true });
      batch.set(doc(state.db, "license_keys", newKey), {
        key: newKey,
        uid,
        email: userEmail,
        planId,
        status: "active",
        active: true,
        isLifetime: isLife,
        expiresAt: isLife ? null : Timestamp.fromMillis(expiresAtMs),
        expiresAtMs,
        createdAt: serverTimestamp(),
        updatedBy: state.user.email,
      });
      await batch.commit();
    }
    showToast(`Granted ${planById(planId).name} + unique key`);
    await loadAdminAll();
  } catch (err) {
    showToast(err.message);
  }
}

async function adminRevokeLicense(uid) {
  if (!state.isAdmin || !state.db) return;
  try {
    if (state.config?.useCloudFunctions && state.functions) {
      await httpsCallable(state.functions, "adminGrantLicense")({ uid, action: "revoke" });
    } else {
      await setDoc(doc(state.db, "licenses", uid), {
        uid,
        planId: "NONE",
        status: "revoked",
        isLifetime: false,
        expiresAt: null,
        expiresAtMs: 0,
        source: "admin_revoke",
        updatedAt: serverTimestamp(),
        updatedBy: state.user.email,
      }, { merge: true });
    }
    showToast("License revoked.");
    await loadAdminAll();
  } catch (err) {
    showToast(err.message);
  }
}

async function adminResolvePayment(reqId, decision) {
  if (!state.isAdmin || !state.db) return;
  try {
    if (state.config?.useCloudFunctions && state.functions) {
      await httpsCallable(state.functions, "adminResolvePaymentRequest")({ requestId: reqId, decision });
    } else {
      const reqItem = state.adminPayments.find((p) => p.id === reqId);
      if (!reqItem) return;
      const batch = writeBatch(state.db);
      batch.update(doc(state.db, "payment_requests", reqId), {
        status: decision,
        resolvedAt: serverTimestamp(),
        resolvedBy: state.user.email,
      });

      if (decision === "approved") {
        const plan = planById(reqItem.planId);
        const isLife = reqItem.planId === "LIFETIME";
        const prev = state.adminLicensesMap.get(reqItem.uid);
        const baseMs = isLicenseActive(prev) && !prev?.isLifetime ? licenseExpiryMs(prev) : Date.now();
        const expiresAtMs = isLife ? 0 : baseMs + plan.days * 86400000;
        const userEmail = String(reqItem.email || "").toLowerCase();
        const newKey = generateLicenseKey();

        batch.set(doc(state.db, "licenses", reqItem.uid), {
          uid: reqItem.uid,
          email: userEmail,
          planId: reqItem.planId,
          status: "active",
          isLifetime: isLife,
          expiresAt: isLife ? null : Timestamp.fromMillis(expiresAtMs),
          expiresAtMs,
          source: "upi_utr",
          couponCode: reqItem.couponCode || null,
          licenseKey: newKey,
          note: `UTR ${reqItem.utr} approved`,
          updatedAt: serverTimestamp(),
          updatedBy: state.user.email,
        }, { merge: true });
        batch.set(doc(state.db, "license_keys", newKey), {
          key: newKey,
          uid: reqItem.uid,
          email: userEmail,
          planId: reqItem.planId,
          status: "active",
          active: true,
          isLifetime: isLife,
          expiresAt: isLife ? null : Timestamp.fromMillis(expiresAtMs),
          expiresAtMs,
          createdAt: serverTimestamp(),
          updatedBy: state.user.email,
        });
      }
      await batch.commit();
    }
    showToast(`Payment ${decision}.`);
    await loadAdminAll();
  } catch (err) {
    showToast(err.message);
  }
}

async function handleSaveStoreConfig(e) {
  e.preventDefault();
  if (!state.isAdmin || !state.db) return;
  const next = {
    upiId: ($("s-upi-id").value || "").trim(),
    upiName: ($("s-upi-name").value || "AYSHA AI").trim(),
    currency: "INR",
    prices: {
      ONE_MONTH: Number($("s-p-1m").value || 0),
      TWO_MONTHS: Number($("s-p-2m").value || 0),
      ONE_YEAR: Number($("s-p-1y").value || 0),
      TWO_YEARS: Number($("s-p-2y").value || 0),
      LIFETIME: Number($("s-p-life").value || 0),
    },
    updatedAt: serverTimestamp(),
    updatedBy: state.user.email,
  };
  try {
    await setDoc(doc(state.db, "config", "store"), next, { merge: true });
    state.store = next;
    renderPlans();
    showToast("Store configuration saved.");
  } catch (err) {
    showToast(err.message);
  }
}

/* --- Live 1s countdown loop --- */
function startCountdowns() {
  setInterval(() => {
    document.querySelectorAll("[data-countdown-ms]").forEach((el) => {
      const ms = Number(el.getAttribute("data-countdown-ms") || 0);
      if (ms > 0) el.textContent = formatCountdown(ms);
    });
    if (state.user && !state.isAdmin && isLicenseActive(state.license) && !state.license?.isLifetime) {
      $("acc-timer").textContent = formatCountdown(licenseExpiryMs(state.license));
    }
  }, 1000);
}

/* --- Default coupon expiry helper --- */
function setQuickExpiry(minutesFromNow) {
  const dt = new Date(Date.now() + minutesFromNow * 60000);
  const pad = (n) => String(n).padStart(2, "0");
  const localStr = `${dt.getFullYear()}-${pad(dt.getMonth() + 1)}-${pad(dt.getDate())}T${pad(dt.getHours())}:${pad(dt.getMinutes())}`;
  $("c-expires-at").value = localStr;
}

/* --- Init --- */
async function init() {
  startOrbLoop();
  startCountdowns();
  renderPlans();
  setQuickExpiry(1440);

  $("btn-toggle-setup-guide").addEventListener("click", () => {
    $("setup-guide-panel").classList.toggle("hidden");
  });
  $("btn-auth-action").addEventListener("click", triggerGoogleAuth);
  $("btn-hero-signin").addEventListener("click", triggerGoogleAuth);
  $("coupon-form").addEventListener("submit", handleCheckCoupon);
  $("btn-redeem-free").addEventListener("click", handleRedeemFreeCoupon);
  $("btn-close-modal").addEventListener("click", () => $("checkout-modal").classList.add("hidden"));
  $("btn-copy-upi").addEventListener("click", () => {
    navigator.clipboard?.writeText(state.store.upiId);
    showToast("UPI ID copied.");
  });
  $("btn-save-qr").addEventListener("click", () => {
    const c = $("upi-qr-canvas");
    if (!c) return;
    const link = document.createElement("a");
    link.download = `aysha-upi-qr-${state.checkoutPlan || "pay"}.png`;
    link.href = c.toDataURL("image/png");
    link.click();
  });
  $("btn-copy-my-key").addEventListener("click", () => {
    const k = state.license?.licenseKey || "";
    if (!k) return;
    navigator.clipboard?.writeText(k);
    showToast("License key copied.");
  });
  $("utr-form").addEventListener("submit", handleUtrSubmit);
  $("btn-refresh-user-data").addEventListener("click", () => state.user && syncUserRecords(state.user));

  // Admin listeners
  $("btn-admin-refresh").addEventListener("click", loadAdminAll);
  $("admin-coupon-form").addEventListener("submit", handleCreateCoupon);
  $("admin-key-form").addEventListener("submit", handleCreateLicenseKey);
  $("admin-store-form").addEventListener("submit", handleSaveStoreConfig);
  $("admin-user-search").addEventListener("input", renderAdminTables);
  $("btn-gen-code").addEventListener("click", () => {
    const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    let code = "AYSHA-";
    for (let i = 0; i < 6; i++) code += chars[Math.floor(Math.random() * chars.length)];
    $("c-code").value = code;
  });
  $("c-type").addEventListener("change", () => {
    const isFree = $("c-type").value === "free_license";
    $("c-discount-wrap").classList.toggle("hidden", isFree);
    if (isFree && $("c-plan").value === "ANY") $("c-plan").value = "ONE_MONTH";
  });
  $("c-type").dispatchEvent(new Event("change"));

  document.querySelectorAll(".quick-timers button[data-minutes]").forEach((b) => {
    b.addEventListener("click", () => setQuickExpiry(Number(b.dataset.minutes)));
  });

  document.querySelectorAll(".tab-btn").forEach((b) => {
    b.addEventListener("click", () => {
      document.querySelectorAll(".tab-btn").forEach((t) => t.classList.remove("active"));
      document.querySelectorAll(".tab-pane").forEach((p) => p.classList.remove("active"));
      b.classList.add("active");
      $(b.dataset.tab).classList.add("active");
    });
  });

  try {
    const resp = await fetch(`firebase-config.json?v=${Date.now()}`, { cache: "no-store" });
    const cfg = await resp.json();
    state.config = cfg;
    if (cfg.defaultStore) {
      state.store = structuredClone(cfg.defaultStore);
      renderPlans();
      syncStoreFormInputs();
    }
    if (cfg.latestApkDownloadUrl) {
      $("btn-download-release").href = cfg.latestApkDownloadUrl;
    }
    if (cfg.latestReleaseUrl) {
      $("btn-view-releases").href = cfg.latestReleaseUrl;
    }

    if (!cfg.configured || !cfg.apiKey || !cfg.projectId) {
      $("setup-notice").classList.remove("hidden");
      renderAccountPanel();
      return;
    }

    const fbApp = initializeApp({
      apiKey: cfg.apiKey,
      authDomain: cfg.authDomain,
      projectId: cfg.projectId,
      storageBucket: cfg.storageBucket,
      messagingSenderId: cfg.messagingSenderId,
      appId: cfg.appId,
    });
    state.auth = getAuth(fbApp);
    state.db = getFirestore(fbApp);
    state.functions = getFunctions(fbApp);
    state.firebaseReady = true;

    setPersistence(state.auth, browserLocalPersistence).catch(() => {});

    onAuthStateChanged(state.auth, (user) => {
      state.user = user;
      state.isAdmin = isUserAdmin(user);
      if (user) {
        clearAuthError();
        renderAccountPanel();
        void syncUserRecords(user);
      } else {
        state.license = null;
        state.myPayments = [];
        state.myRedemptions = [];
        renderAccountPanel();
        renderMyHistory();
      }
    });

    getRedirectResult(state.auth).catch((err) => {
      if (err) showAuthError(friendlyAuthError(err));
    });

    void loadStoreConfig();
  } catch (err) {
    console.error("Initialization error:", err);
    showAuthError(`Init error: ${err.message}`);
    $("setup-notice").classList.remove("hidden");
  }
}

init();
