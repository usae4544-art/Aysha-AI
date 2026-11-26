const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp, FieldValue } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();

const ADMIN_EMAIL = "usae4544@gmail.com";

const PLAN_DAYS = {
  ONE_MONTH: 30,
  TWO_MONTHS: 60,
  ONE_YEAR: 365,
  TWO_YEARS: 730,
  LIFETIME: 0,
};

function assertSignedIn(request) {
  if (!request.auth || !request.auth.uid) {
    throw new HttpsError("unauthenticated", "Sign in with Google first.");
  }
  return request.auth;
}

function assertAdmin(request) {
  const auth = assertSignedIn(request);
  const email = String(auth.token?.email || "").toLowerCase();
  if (!auth.token?.email_verified || email !== ADMIN_EMAIL) {
    throw new HttpsError("permission-denied", "Admin access is restricted to usae4544@gmail.com.");
  }
  return auth;
}

function computeLicensePayload({ uid, email, planId, source, couponCode = null, existingData = null, updatedBy = uid, note = "" }) {
  if (!(planId in PLAN_DAYS)) {
    throw new HttpsError("invalid-argument", `Invalid planId: ${planId}`);
  }
  const nowMs = Date.now();
  const isLifetime = planId === "LIFETIME";

  let expiresAtMs = 0;
  let expiresAt = null;
  if (!isLifetime) {
    const existingEnd = existingData?.status === "active" && existingData?.expiresAtMs > nowMs
      ? existingData.expiresAtMs
      : nowMs;
    const baseMs = existingData?.isLifetime ? nowMs : existingEnd;
    expiresAtMs = baseMs + PLAN_DAYS[planId] * 24 * 60 * 60 * 1000;
    expiresAt = Timestamp.fromMillis(expiresAtMs);
  }

  return {
    uid,
    email: String(email || existingData?.email || "").toLowerCase(),
    planId,
    status: "active",
    isLifetime,
    expiresAt,
    expiresAtMs,
    source,
    couponCode,
    note,
    updatedAt: FieldValue.serverTimestamp(),
    updatedBy,
  };
}

exports.redeemFreeCoupon = onCall({ cors: true }, async (request) => {
  const auth = assertSignedIn(request);
  const rawCode = String(request.data?.code || "").trim().toUpperCase();
  const requestedPlanId = String(request.data?.planId || "").trim().toUpperCase();

  if (!rawCode) {
    throw new HttpsError("invalid-argument", "Coupon code is required.");
  }

  const couponRef = db.collection("coupons").doc(rawCode);
  const redemptionRef = db.collection("redemptions").doc(`${rawCode}_${auth.uid}`);
  const licenseRef = db.collection("licenses").doc(auth.uid);

  const result = await db.runTransaction(async (tx) => {
    const [couponSnap, redemptionSnap, licenseSnap] = await Promise.all([
      tx.get(couponRef),
      tx.get(redemptionRef),
      tx.get(licenseRef),
    ]);

    if (!couponSnap.exists) {
      throw new HttpsError("not-found", "Coupon code does not exist.");
    }
    const c = couponSnap.data();
    if (!c.active) {
      throw new HttpsError("failed-precondition", "Coupon has been disabled.");
    }
    const expiresMs = c.expiresAt?.toMillis ? c.expiresAt.toMillis() : Number(c.expiresAtMs || 0);
    if (!expiresMs || Date.now() >= expiresMs) {
      throw new HttpsError("deadline-exceeded", "Coupon validity timer has expired.");
    }
    if (Number(c.usedCount || 0) >= Number(c.maxUses || 0)) {
      throw new HttpsError("resource-exhausted", "Coupon redemption limit reached.");
    }
    if (redemptionSnap.exists) {
      throw new HttpsError("already-exists", "You have already redeemed this coupon.");
    }

    const isFree = c.rewardType === "free_license" || Number(c.discountPercent || 0) >= 100;
    if (!isFree) {
      throw new HttpsError("failed-precondition", "This is a percentage discount coupon. Apply it during checkout.");
    }

    const finalPlanId = c.rewardPlanId && c.rewardPlanId !== "ANY" ? c.rewardPlanId : requestedPlanId;
    if (!(finalPlanId in PLAN_DAYS)) {
      throw new HttpsError("invalid-argument", "Invalid target plan.");
    }

    const nextLicense = computeLicensePayload({
      uid: auth.uid,
      email: auth.token?.email || "",
      planId: finalPlanId,
      source: "coupon",
      couponCode: rawCode,
      existingData: licenseSnap.exists ? licenseSnap.data() : null,
      updatedBy: auth.uid,
      note: `Redeemed coupon ${rawCode}`,
    });

    tx.update(couponRef, { usedCount: FieldValue.increment(1) });
    tx.set(redemptionRef, {
      code: rawCode,
      uid: auth.uid,
      email: String(auth.token?.email || "").toLowerCase(),
      rewardType: c.rewardType,
      rewardPlanId: finalPlanId,
      discountPercent: Number(c.discountPercent || 100),
      redeemedAt: FieldValue.serverTimestamp(),
    });
    tx.set(licenseRef, nextLicense, { merge: true });

    return {
      ok: true,
      planId: finalPlanId,
      isLifetime: nextLicense.isLifetime,
      expiresAtMs: nextLicense.expiresAtMs,
      serverNowMs: Date.now(),
    };
  });

  return result;
});

exports.adminGrantLicense = onCall({ cors: true }, async (request) => {
  const admin = assertAdmin(request);
  const targetUid = String(request.data?.uid || "").trim();
  const action = String(request.data?.action || "grant").trim();
  const planId = String(request.data?.planId || "ONE_MONTH").trim().toUpperCase();
  const note = String(request.data?.note || "").trim();

  if (!targetUid) {
    throw new HttpsError("invalid-argument", "Target uid is required.");
  }

  const licenseRef = db.collection("licenses").doc(targetUid);
  const userRef = db.collection("users").doc(targetUid);
  const [licenseSnap, userSnap] = await Promise.all([licenseRef.get(), userRef.get()]);
  const targetEmail = userSnap.exists ? userSnap.data().email : (licenseSnap.data()?.email || "");

  if (action === "revoke") {
    await licenseRef.set({
      uid: targetUid,
      email: targetEmail,
      planId: "NONE",
      status: "revoked",
      isLifetime: false,
      expiresAt: null,
      expiresAtMs: 0,
      source: "admin_revoke",
      note: note || "Revoked by admin",
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: admin.token.email,
    }, { merge: true });
    return { ok: true, status: "revoked" };
  }

  const payload = computeLicensePayload({
    uid: targetUid,
    email: targetEmail,
    planId,
    source: "admin_grant",
    existingData: action === "extend" && licenseSnap.exists ? licenseSnap.data() : null,
    updatedBy: admin.token.email,
    note: note || `Admin ${action}`,
  });

  await licenseRef.set(payload, { merge: true });
  return { ok: true, planId, expiresAtMs: payload.expiresAtMs, isLifetime: payload.isLifetime };
});

exports.adminResolvePaymentRequest = onCall({ cors: true }, async (request) => {
  const admin = assertAdmin(request);
  const requestId = String(request.data?.requestId || "").trim();
  const decision = String(request.data?.decision || "").trim();
  const adminNote = String(request.data?.adminNote || "").trim();

  if (!requestId || !["approved", "rejected"].includes(decision)) {
    throw new HttpsError("invalid-argument", "requestId and decision (approved|rejected) are required.");
  }

  const reqRef = db.collection("payment_requests").doc(requestId);
  await db.runTransaction(async (tx) => {
    const reqSnap = await tx.get(reqRef);
    if (!reqSnap.exists) {
      throw new HttpsError("not-found", "Payment request not found.");
    }
    const reqData = reqSnap.data();
    tx.update(reqRef, {
      status: decision,
      adminNote,
      resolvedAt: FieldValue.serverTimestamp(),
      resolvedBy: admin.token.email,
    });

    if (decision === "approved") {
      const licenseRef = db.collection("licenses").doc(reqData.uid);
      const licSnap = await tx.get(licenseRef);
      const payload = computeLicensePayload({
        uid: reqData.uid,
        email: reqData.email || "",
        planId: reqData.planId,
        source: "upi_utr",
        couponCode: reqData.couponCode || null,
        existingData: licSnap.exists ? licSnap.data() : null,
        updatedBy: admin.token.email,
        note: `UTR ${reqData.utr} approved`,
      });
      tx.set(licenseRef, payload, { merge: true });
    }
  });

  return { ok: true, decision };
});
