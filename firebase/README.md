# AYSHA AI — Firebase Backend Setup

This directory provides server-enforced **Google Authentication**, **License Entitlements**, **Time-Limited / Use-Limited Coupon Codes**, **UPI UTR Payment Requests**, and an **Admin Console strictly locked to `usae4544@gmail.com`**.

## Option A — 100% Free Firebase Spark Plan (No Credit Card Required)

The Firestore rules in `firestore.rules` enforce all security invariants atomically at the database layer using server `request.time` and `getAfter(...)` batch checks:
- Only `usae4544@gmail.com` (verified Google email) can list users, list coupons, create/edit/delete coupons, change store pricing/UPI ID, grant/extend/revoke licenses, or approve/reject UPI requests.
- Signed-in users can only look up an individual coupon by exact code (`get`), NEVER list all coupons.
- Coupon redemption atomically creates `/redemptions/{CODE}_{UID}`, increments `usedCount` by `1` (blocking if `request.time >= expiresAt` or `usedCount >= maxUses`), and either activates `/licenses/{UID}` (for free-license / 100%-off coupons) or attaches to a `/payment_requests/{REQ_ID}` submission.

### Steps
1. Open [Firebase Console](https://console.firebase.google.com/) and create a project (e.g., `aysha-ai-prod`).
2. **Authentication → Sign-in method**:
   - Enable **Google** provider and set project support email to `usae4544@gmail.com`.
   - Under **Authentication → Settings → Authorized domains**, add:
     `usae4544-art.github.io`
3. **Cloud Firestore → Create database** (Production mode, e.g., region `asia-south1`).
4. Deploy `firestore.rules` and `firestore.indexes.json`:
   ```bash
   npm i -g firebase-tools
   firebase login
   firebase use --add <YOUR_PROJECT_ID>
   firebase deploy --only firestore
   ```
   (Or paste the contents of `firebase/firestore.rules` directly into **Firestore → Rules** in the Firebase Console and click **Publish**.)
5. **Project Settings → General → Your apps → Web app (`</>`)**:
   Copy your `firebaseConfig` JSON into `site/firebase-config.json` in this repository and push to `main`:
   ```json
   {
     "configured": true,
     "useCloudFunctions": false,
     "apiKey": "AIza...",
     "authDomain": "your-project.firebaseapp.com",
     "projectId": "your-project",
     "storageBucket": "your-project.firebasestorage.app",
     "messagingSenderId": "1234567890",
     "appId": "1:1234567890:web:abcdef"
   }
   ```
   Both the live GitHub Pages site (`https://usae4544-art.github.io/Aysha-AI/`) and the Android APK automatically read `site/firebase-config.json`.

## Option B — Blaze Plan Cloud Functions (Optional)

If your Firebase project is on the Blaze plan, you can also deploy `firebase/functions`:
```bash
cd firebase/functions && npm install && cd ..
firebase deploy --only functions,firestore
```
Then set `"useCloudFunctions": true` in `site/firebase-config.json`.
