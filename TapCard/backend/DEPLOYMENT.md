# TapCard Deployment Guide

> Nothing in this project connects to Firebase until YOU create the project and
> paste its config values. Every step below is one-time.

## 0. Prerequisites
- Google account, Node.js 20+, Firebase CLI: `npm i -g firebase-tools`
- The `tapcard.app` domain (only needed when you want the final public URLs;
  until then pages work at `https://<project>.web.app/p/<slug>`)

## 1. Create the Firebase project
1. https://console.firebase.google.com → **Add project** → name `tapcard`
2. **Authentication → Sign-in method → enable Email/Password**
3. **Firestore Database → Create** (production mode; region near your users)
4. **Storage → Get started** (same region)

## 2. Register the Android app
1. Project settings → **Add app → Android** → package `com.tapcard.app`
2. Download `google-services.json` **only to read values from it** (we do NOT
   add the file to the build). Copy into `firebase_config.xml` (next section):
   - `project_info.project_id` → `firebase_project_id`
   - `client[0].api_key[0].current_key` → `firebase_api_key`
   - `client[0].client_info.appinvites...app_id` → `firebase_app_id`
3. Rebuild + install the app → it now runs in **cloud mode** (login required).

## 3. Wire the config into the app
Edit `TapCard/app/src/main/res/values/firebase_config.xml`:
```xml
<string name="firebase_api_key">PASTE_REAL_KEY</string>
<string name="firebase_project_id">PASTE_REAL_PROJECT_ID</string>
<string name="firebase_app_id">PASTE_REAL_APP_ID</string>
<bool name="cloud_mode_enabled">true</bool>
```
`cloud_mode_enabled=false` (current state) keeps v1.1 local behavior for testing.

## 4. Deploy backend (CLI)
```bash
cd TapCard/backend
firebase login
firebase use --add               # select your project, alias "default"
firebase deploy --only firestore:rules,storage
# Hosting + functions (Tier 2 public pages):
cd functions && npm install && cd ..
firebase deploy --only hosting,functions
```

## 5. Hosting domain + DNS (only for tapcard.app)
1. Firebase Console → Hosting → **Add custom domain** → `tapcard.app`
   (and `www.tapcard.app`)
2. Firebase gives you a TXT record to verify domain ownership at your DNS
   provider, then **A records** to point the apex at Firebase Hosting IPs:
   - Add the A records it shows (currently two IPs, e.g. `199.36.158.100` /
     `199.36.158.101` — use exactly what the console shows; they can change)
3. SSL certificates are auto-provisioned (can take up to 24h)
4. The app's canonical URL base is already `https://tapcard.app/p/{slug}` —
   no app change needed once DNS is live. Until then NFC/QR keep encoding
   `tapcard.app` URLs which only resolve after DNS is set up (or test with
   `<project>.web.app` URLs by temporarily changing
   `BusinessCard.PUBLIC_URL_BASE`).

## 6. Public profile tiers
- **Tier 1 (free)**: deploy hosting only → `web.app/p/{slug}` static page reads
  `publicProfiles/{slug}` (works instantly, generic OG tags)
- **Tier 2 (Blaze)**: `firebase deploy --only functions,hosting` → Hosting
  rewrites `/p/**` to the `profilePage` Cloud Function → per-slug SEO + OG
  tags + `/p/{slug}/vcard` contact download

## 7. Verify checklist (run after deploy)
```bash
# rules: anonymous write must FAIL, owner write must PASS
firebase emulators:start --only firestore,auth,storage   # local test first
# then in the app: register → create card → check Firestore console shows
# cards/, slugs/, publicProfiles/ docs for your uid
```
