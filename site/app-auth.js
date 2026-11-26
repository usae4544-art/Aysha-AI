import { initializeApp } from "https://www.gstatic.com/firebasejs/11.6.1/firebase-app.js";
import {
  getAuth,
  GoogleAuthProvider,
  browserLocalPersistence,
  setPersistence,
  signInWithPopup,
  signInWithRedirect,
  getRedirectResult,
  onAuthStateChanged,
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-auth.js";
import {
  getFirestore,
  doc,
  setDoc,
  serverTimestamp,
} from "https://www.gstatic.com/firebasejs/11.6.1/firebase-firestore.js";

const $ = (id) => document.getElementById(id);

function buildCallbackUrl(user) {
  const params = new URLSearchParams({
    refreshToken: user.refreshToken || "",
    uid: user.uid || "",
    email: user.email || "",
  });
  return `aysha://auth/callback?${params.toString()}`;
}

function handleSignedInUser(db, user) {
  const pill = $("bridge-status");
  pill.className = "status-pill status-ok";
  pill.textContent = "Verified";

  $("bridge-message").textContent = `Signed in as ${user.email}. Syncing profile and opening AYSHA...`;

  // Sync user directory in background so missing Firestore never blocks app return
  setDoc(doc(db, "users", user.uid), {
    uid: user.uid,
    email: String(user.email || "").toLowerCase(),
    emailVerified: Boolean(user.emailVerified),
    displayName: user.displayName || "",
    photoUrl: user.photoURL || "",
    lastLoginAt: serverTimestamp(),
    lastLoginSource: "android",
  }, { merge: true }).catch((err) => {
    console.warn("User upsert error:", err);
  });

  const deepLink = buildCallbackUrl(user);
  const retBtn = $("btn-return-apk");
  retBtn.href = deepLink;
  retBtn.classList.remove("hidden");

  $("fallback-token").value = user.refreshToken || "";
  $("fallback-box").classList.remove("hidden");
  $("btn-google-bridge").textContent = `Switch Google account (${user.email})`;

  setTimeout(() => {
    window.location.href = deepLink;
  }, 450);
}

async function main() {
  $("btn-copy-token").addEventListener("click", () => {
    navigator.clipboard?.writeText($("fallback-token").value || "");
    $("btn-copy-token").textContent = "Copied verification token";
  });

  let cfg;
  try {
    const resp = await fetch(`firebase-config.json?v=${Date.now()}`, { cache: "no-store" });
    cfg = await resp.json();
  } catch (err) {
    $("bridge-status").className = "status-pill status-err";
    $("bridge-status").textContent = "Config Error";
    $("bridge-message").textContent = `Failed to load firebase-config.json: ${err.message}`;
    return;
  }

  if (!cfg.configured || !cfg.apiKey || !cfg.projectId) {
    $("bridge-status").className = "status-pill status-warn";
    $("bridge-status").textContent = "Setup needed";
    $("bridge-message").textContent = "Firebase config is missing in site/firebase-config.json. Complete Firebase setup first.";
    $("btn-google-bridge").disabled = true;
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
  const auth = getAuth(fbApp);
  const db = getFirestore(fbApp);
  setPersistence(auth, browserLocalPersistence).catch(() => {});

  onAuthStateChanged(auth, (user) => {
    if (user) {
      handleSignedInUser(db, user);
    }
  });

  getRedirectResult(auth).catch((err) => {
    if (!err) return;
    $("bridge-status").className = "status-pill status-err";
    $("bridge-status").textContent = "Auth Error";
    $("bridge-message").textContent = `${err.code || "auth/error"}: ${err.message}`;
  });

  $("btn-google-bridge").addEventListener("click", async () => {
    const btn = $("btn-google-bridge");
    btn.disabled = true;
    btn.textContent = "Opening Google Sign-In...";
    const provider = new GoogleAuthProvider();
    provider.setCustomParameters({ prompt: "select_account" });
    try {
      await signInWithPopup(auth, provider);
    } catch (err) {
      if (err.code === "auth/popup-blocked" || err.code === "auth/operation-not-supported-in-this-environment") {
        $("bridge-message").textContent = "Redirecting to Google Sign-In...";
        await signInWithRedirect(auth, provider);
      } else {
        $("bridge-status").className = "status-pill status-err";
        $("bridge-status").textContent = "Error";
        $("bridge-message").textContent = `${err.code || "auth/error"}: ${err.message}`;
      }
    } finally {
      btn.disabled = false;
      if (!auth.currentUser) {
        btn.textContent = "Continue with Google";
      }
    }
  });
}

main();
