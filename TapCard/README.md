# TapCard — NFC Digital Business Card Platform

Native Android application (Milestone 1): create digital business cards, preview
them, share the public link, and program real NFC tags with it.

- **Stack:** Kotlin 2.0.21 · Jetpack Compose (BOM 2024.09) · Material 3 · Gradle 8.9 · AGP 8.7.3
- **minSdk 26** · targetSdk 35 · NFC `uses-feature required=false` (installs on non-NFC devices)

## Project location

```
C:\Users\admin\.zcode\workspace\draft-6c528bf0-2807-459f-bb77-b00c5099baaa\TapCard
```

## Build & run

```bash
# from this directory (or open in Android Studio)
gradle assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Architecture

```
app/src/main/java/com/tapcard/app/
├── MainActivity.kt            # single activity; forwards onNewIntent to NfcTagWriter
├── data/
│   ├── model/BusinessCard.kt  # card model, slug, publicUrl, initials
│   ├── CardRepository.kt      # StateFlow store (local sample data this milestone)
│   └── vcard/VCardBuilder.kt  # real vCard 3.0 generation for contact sharing
├── nfc/                       # ALL hardware logic — UI never fakes results
│   ├── NdefPayloadBuilder.kt  # NFC Forum RTD_URI record construction (short-form prefixes)
│   └── NfcTagWriter.kt        # foreground dispatch, connect/capacity/write/format,
│                              # sealed outcomes mapped 1:1 from real exceptions
└── ui/
    ├── theme/                 # premium M3 theme (indigo→violet brand, 24dp shapes)
    ├── components/            # InitialsAvatar, GradientButton, StatusChip, EmptyState
    ├── AppRoot.kt             # NavHost + bottom bar navigation
    └── screens/               # Dashboard, Cards, CreateCard, Preview, NfcWrite, Settings
```

## NFC payload specification (what gets written to the tag)

One NFC Forum **well-known RTD_URI record** containing the public profile URL:

```
TNF: 0x01 (WELL_KNOWN)
TYPE: "U" (RTD_URI)
PAYLOAD: [prefix-code byte][remaining URL bytes, UTF-8]
```

Example for `https://tapcard.app/p/alex-morgan` → payload `[0x04] "tapcard.app/p/alex-morgan"`
(`0x04` = "https://" prefix code → 24 bytes total). This is the same payload
class the HiHello reference writes (see `_out/FINAL_NFC_RESEARCH.md`) and is
readable by Android, iOS background reading and lock screens.

## NFC write flow (real implementation)

1. Screen queues the card's NDEF message → `NfcTagWriter.startWrite()`
2. `enableForegroundDispatch` armed with tech filters `[Ndef], [NdefFormatable]`
3. User taps tag → system re-delivers intent → `MainActivity.onNewIntent` → writer
4. Writer connects, checks `isWritable` and `maxSize ≥ message size`, writes
5. `NdefFormatable.format()` used for blank/unformatted tags
6. Every failure maps to a real exception (`TagLostException`, `IOException`,
   read-only, capacity) and is shown verbatim — success is never simulated

## Milestone status

**Done (M1):** project scaffold, premium theme, navigation, Dashboard,
My Business Cards, Create Business Card (validation + accent picker), Card
Preview (contact rows + real vCard preview + copy link), NFC Writer screen
(full honest state machine), Settings, real NFC engine, APK builds and runs.

**Pending (M2+):** backend (auth, card CRUD API, public profile pages at
`tapcard.app/p/<slug>`), QR code generation & sharing screen, profile photo /
logo upload, vCard file sharing, tag locking (makeReadOnly), Room persistence,
unit/instrumented tests, physical NFC tag test matrix (NTAG213/215, MIFARE).
