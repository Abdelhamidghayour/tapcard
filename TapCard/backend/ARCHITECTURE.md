# TapCard Backend Architecture (Milestone 2 Proposal)

## Compatibility verdict with existing v1.1 Android architecture

| Requirement | Current app (v1.1) | Verdict |
|---|---|---|
| Kotlin 2.0.21, coroutines StateFlow data layer | Firebase SDKs are coroutine-first (`kotlinx-coroutines-play-services`) | ✅ Direct fit |
| minSdk 26 | Firebase Auth ≥23, Firestore ≥19, Storage ≥20 | ✅ No change needed |
| `CardRepository` object, 7 public members, 12 UI call sites | Kept as a **facade** over pluggable backends | ✅ Zero UI rewrites |
| DataStore persistence (v1.1) | Becomes local cache + migration source | ✅ Preserved |
| NFC/QR canonical URL `https://tapcard.app/p/{slug}` | Unchanged — cloud slugs live under the same base | ✅ QR/NDEF untouched |

## Stack (as requested)

- **Firebase Authentication** — email/password (+ prepared Google Sign-In extension point)
- **Cloud Firestore** — cards, user profiles, slug registry, public profiles
- **Firebase Storage** — profile images (`users/{uid}/cards/{cardId}/profile.jpg`)
- **Cloud Functions (optional, Node 20)** — server-side public profile HTML with dynamic Open Graph tags + vCard endpoint; hardened slug-claiming callable. Runs on the Blaze plan; a free-tier static fallback is included.
- **Firebase Hosting** — public profile site `https://tapcard.app/p/{slug}`

## Build-safe configuration strategy (important)

The `google-services` Gradle plugin **fails the build without a real `google-services.json`**, which I cannot create (requires your Firebase console). Therefore the Android app:

1. Does **not** apply the google-services plugin.
2. Initializes Firebase **manually** (`FirebaseApp.initializeApp(context, FirebaseOptions...)`) from placeholder values in `res/values/firebase_config.xml`.
3. Runs a runtime check `CloudMode.isConfigured`: placeholder values → **local-only mode (exact v1.1 behavior)**; real values → **cloud mode** (auth-gated, synced).

To activate the backend you paste 5 real values (apiKey, projectId, appId, authDomain, storageBucket) into one XML file. No code changes. Documented in DEPLOYMENT.md.

## Runtime modes

```
CloudMode.isConfigured == false  →  LOCAL  mode: v1.1 behavior, no auth gate
CloudMode.isConfigured == true   →  CLOUD  mode: login required, cards synced to
                                    Firestore, images to Storage, offline cache on
```

## Repository layering (facade preserved)

```
CardRepository (object — same 7 public members as v1.1)
 ├── LocalBackend    (DataStore, v1.1 logic unchanged)
 └── FirebaseBackend (Firestore + Storage + DataStore as offline cache)
      └── Migrator   (one-time: push local cards to cloud on first login)
```

Mode is decided at app start. Switching source of truth happens only in
`CardRepository`; screens keep calling the same members and observing the same
StateFlows.

## Auth flow

1. App start → `AuthManager.authState` flow: `Loading → LoggedOut / LoggedIn(user)`
2. CLOUD mode + LoggedOut → Login screen (email/password; register; forgot password)
3. Sessions are persistent (Firebase's own token store)
4. On first-ever login, `Migrator` runs once (see MIGRATION_STRATEGY.md)
5. Google Sign-In: `AuthManager.signInWithGoogle(idToken)` extension point stubbed,
   Credential Manager wiring documented for future milestone (needs OAuth client IDs)

## Public profile serving (two tiers)

- **Tier 1 (deployable on free plan):** static Hosting page loads
  `publicProfiles/{slug}` client-side via Firestore JS SDK; generic OG tags.
- **Tier 2 (production, Blaze):** Cloud Function renders per-slug HTML with
  dynamic OG/SEO tags + `/p/{slug}/vcard` download; Hosting rewrite
  `/p/** → profilePage` function. Both tiers read ONLY `publicProfiles`
  (explicitly public fields).

## Security model summary (details in security rules file)

- `cards/**` — owner-only read/write (auth.uid == ownerUid)
- `publicProfiles/{slug}` — world-readable, owner-writable, only public fields, immutable ownerUid
- `slugs/{slug}` — create-once registry enforcing global slug uniqueness; owner may delete own
- `users/{uid}` — self only
- Storage — `users/{uid}/**` self-only, ≤ 2 MB, image content-type only
- Service accounts never ship in the APK; the app only holds public Firebase config values (safe by design, they identify the project, they do not authorize access — rules do)
