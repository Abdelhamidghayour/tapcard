# Firestore Schema (Milestone 2)

## Collections

### `users/{uid}` — private account document
| Field | Type | Notes |
|---|---|---|
| email | string | from Auth |
| displayName | string | editable later |
| createdAt | timestamp | serverTimestamp |
| migratedAt | timestamp? | set after local→cloud migration completes |

### `cards/{autoId}` — owner-private card documents
| Field | Type | Notes |
|---|---|---|
| ownerUid | string | immutable |
| name | string | required, ≤120 |
| jobTitle, company | string | ≤120 |
| email, phone, website | string? | validated client+rule length |
| socials[] | array<{platform,url}> | ≤10 items |
| profileImageUrl | string? | Storage download URL |
| accentIndex | number | 0..4 |
| slug | string | unique, `[a-z0-9-]{3,40}` |
| isPublic | boolean | default true |
| createdAt / updatedAt | timestamp | serverTimestamp |
| nfcWrites | number | denormalized counter |

**Index:** composite `(ownerUid ASC, updatedAt DESC)` — listed in firestore.indexes.json.

### `slugs/{slug}` — global uniqueness registry
| Field | Type |
|---|---|
| cardId | string |
| ownerUid | string |
| createdAt | timestamp |

Doc-id IS the slug. Created inside the same client `runTransaction` that
creates/renames a card → guarantees no duplicate slugs across all users.
Rename = create new slug doc + delete old (both inside one transaction).

### `publicProfiles/{slug}` — explicitly-public projection (world-readable)
| Field | Type | Notes |
|---|---|---|
| name | string | |
| jobTitle, company | string | |
| email, phone, website | string? | ONLY if user chooses to expose |
| socials[] | array<{platform,url}> | |
| profileImageUrl | string? | |
| accentIndex | number | |
| ownerUid | string | used by rules for write authz |
| cardId | string | |
| updatedAt | timestamp | |

Rules force writes to be a strict subset of public fields — private data
(e.g. auth email) can never leak here. Deleting the card deletes this doc.

## Data flow for one save (CLOUD mode)

```
Client runTransaction:
  1. slugs/{newSlug} must not exist (or owned by me)  → else ABORT (slug taken)
  2. upsert cards/{cardId}
  3. upsert publicProfiles/{slug} (public projection)
  4. if slug changed: delete slugs/{oldSlug} + publicProfiles/{oldSlug}
```

## v1.1 field mapping (migration)

Local `BusinessCard` → Firestore `cards` doc is 1:1 (same field names).
Local `publicUrl` is **derived**, never stored. `nfcWrites` counter migrates.

## Offline behavior (CLOUD mode)

Firestore offline persistence enabled (100 MB cache). Reads serve from cache;
writes queue and sync on reconnect. UI exposes `SyncState`:
`Synced / Syncing / Offline Queued(n) / Error(msg)`.
