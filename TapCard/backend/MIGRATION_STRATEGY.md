# Local → Cloud Migration Strategy

## Goal
v1.1 users (local DataStore cards) must never lose data when they sign in for
the first time in v2.0 cloud mode.

## Trigger
Runs once per UID, inside `CardRepository` right after the first successful
login, when:
1. DataStore flag `migrated_<uid>` is absent, AND
2. The user has ≥1 local card, AND
3. The cloud query `cards where ownerUid == uid` returns 0 documents

## Algorithm (implemented in `CloudMigrator.plan()` / `FirebaseBackend.migrate()`)

```
localCards   = DataStore cards (v1.1 seed or user-created)
cloudCards   = fetch cards where ownerUid == uid
if cloudCards is empty:
    for each local card:
        keep id, slug (claim via slugs/{slug} transaction; on conflict, suffix -2)
        write cards/{id} + slugs/{slug} + publicProfiles/{slug}  (batched, 500/doc limit)
    write users/{uid}.migratedAt = now
    DataStore becomes a pure offline cache (cards refreshed from cloud)
else:
    # account already has cloud cards (login from another device)
    keep cloud as source of truth, local cache replaced by cloud snapshot
    local-only cards are preserved in DataStore under legacy flag (never deleted)
set DataStore flag migrated_<uid> = true   # never runs twice
```

## Safety properties
- **No deletion ever**: if cloud already has cards, local extras are kept in a
  DataStore "legacy" list the user can import manually later (not auto-merged).
- **Idempotent**: the per-UID flag prevents double migration; cloud-empty check
  prevents clobbering existing cloud data.
- **Slug collisions**: resolved deterministically (`alex-morgan` → `alex-morgan-2`)
  inside the same transaction that claims `slugs/{slug}`, so QR/NFC URLs stay
  valid and unique.
- **Offline**: if the device is offline at login, migration retries on the next
  login/foreground sync (flag only set on success).

## What the user sees
First login → "Importing your N card(s)…" progress line on the dashboard →
cards appear identical, same slugs → existing NFC tags and QR codes continue
to work (URLs unchanged).
