package com.tapcard.app.data

import android.content.Context
import android.util.Log
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FieldValue
import com.tapcard.app.data.auth.AuthManager
import com.tapcard.app.data.cloud.CardMappers
import com.tapcard.app.data.cloud.CloudMode
import com.tapcard.app.data.cloud.CloudMigrator
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.data.model.SocialLink
import com.tapcard.app.data.model.slugify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.util.UUID

private val Context.cardStore by preferencesDataStore(name = "tapcard_store")

@Serializable
private data class StoreDocument(
    val cards: List<BusinessCard> = emptyList(),
    val activeCardId: String? = null,
    val nfcWrites: Int = 0,
    val shares: Int = 0,
    val legacyCards: List<BusinessCard> = emptyList(),
)

/** Cloud sync status surfaced in the UI. */
sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data object Synced : SyncState
    data class Error(val message: String) : SyncState
}

/** One-time local→cloud migration status. */
sealed interface MigrationState {
    data object NotStarted : MigrationState
    data class InProgress(val count: Int) : MigrationState
    data class Done(val imported: Int) : MigrationState
    data object CloudAlreadyHadCards : MigrationState
    data class Failed(val message: String) : MigrationState
}

/** Profile image upload status. */
sealed interface UploadState {
    data object Idle : UploadState
    data class Uploading(val progressPercent: Int) : UploadState
    data object Done : UploadState
    /** Image saved on the device; cloud mirror unavailable (Storage not provisioned). */
    data object SavedOnDevice : UploadState
    data class Failed(val message: String) : UploadState
}

/**
 * Single source of truth for business cards.
 *
 * v1.1 API is preserved 1:1 (DataStore-backed, offline-first, all mutations
 * optimistic). In CLOUD mode every mutation is additionally written through
 * to Firestore; on login a one-time migration imports local cards, and a
 * snapshot listener adopts the cloud as the authoritative snapshot.
 */
object CardRepository {

    private val KEY_DOC = stringPreferencesKey("document_json")
    private val KEY_SCHEMA = intPreferencesKey("schema_version")
    private fun migratedKey(uid: String) = booleanPreferencesKey("migrated_$uid")

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var appContext: Context? = null

    private val _cards = MutableStateFlow<List<BusinessCard>>(emptyList())
    val cards: StateFlow<List<BusinessCard>> = _cards.asStateFlow()

    private val _activeCardId = MutableStateFlow<String?>(null)
    val activeCardId: StateFlow<String?> = _activeCardId.asStateFlow()

    private val _nfcWrites = MutableStateFlow(0)
    val nfcWrites: StateFlow<Int> = _nfcWrites.asStateFlow()

    /** Real counter: incremented on QR shares and vCard shares. */
    private val _shares = MutableStateFlow(0)
    val shares: StateFlow<Int> = _shares.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _migrationState = MutableStateFlow<MigrationState>(MigrationState.NotStarted)
    val migrationState: StateFlow<MigrationState> = _migrationState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    @Volatile
    private var initialized = false

    private var cardListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    /** True when Firebase is configured AND the user is authenticated. */
    val cloudActive: Boolean
        get() = CloudMode.isConfigured && AuthManager.currentUid != null

    fun init(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            val ctx = appContext!!
            val doc: StoreDocument = runBlocking {
                try {
                    val raw = ctx.cardStore.data.first()[KEY_DOC]
                    if (raw != null) json.decodeFromString<StoreDocument>(raw) else StoreDocument()
                } catch (e: Exception) {
                    StoreDocument()
                }
            }
            if (doc.cards.isEmpty()) {
                val seeded = seed()
                _cards.value = seeded
                _activeCardId.value = seeded.firstOrNull()?.id
                persist()
            } else {
                // Deduplicate by slug on load (keeps newest) — guards against
                // duplicate artifacts from earlier migration runs.
                val cleaned = doc.cards
                    .sortedByDescending { it.updatedAt }
                    .distinctBy { it.slug.ifBlank { it.id } }
                _cards.value = cleaned
                _activeCardId.value =
                    if (cleaned.any { it.id == doc.activeCardId }) doc.activeCardId
                    else cleaned.firstOrNull()?.id
                _nfcWrites.value = doc.nfcWrites
                _shares.value = doc.shares
            }
            initialized = true
        }
    }

    fun cardById(id: String?): BusinessCard? = _cards.value.firstOrNull { it.id == id }

    fun activeCard(): BusinessCard? = cardById(_activeCardId.value)

    fun create(card: BusinessCard): BusinessCard {
        val withSlug = card.copy(slug = uniqueSlugFor(card.name))
        _cards.update { it + withSlug }
        if (_activeCardId.value == null) _activeCardId.value = withSlug.id
        persist()
        pushCard(withSlug)
        return withSlug
    }

    fun update(card: BusinessCard) {
        val stamped = card.copy(updatedAt = System.currentTimeMillis())
        _cards.update { list -> list.map { if (it.id == stamped.id) stamped else it } }
        persist()
        pushCard(stamped)
    }

    fun delete(id: String) {
        val removed = _cards.value.firstOrNull { it.id == id }
        _cards.update { list -> list.filterNot { it.id == id } }
        if (_activeCardId.value == id) _activeCardId.value = _cards.value.firstOrNull()?.id
        persist()
        removed?.let { pushDelete(it) }
    }

    fun setActive(id: String) {
        require(_cards.value.any { it.id == id }) { "Unknown card id" }
        _activeCardId.value = id
        persist()
        val uid = AuthManager.currentUid
        val fs = CloudMode.firestore
        if (uid != null && fs != null) {
            scope.launch {
                runCatching {
                    fs.collection("users").document(uid)
                        .set(mapOf("activeCardId" to id), com.google.firebase.firestore.SetOptions.merge())
                        .await()
                }
            }
        }
    }

    fun recordNfcWriteSuccess() {
        _nfcWrites.value += 1
        persist()
        val card = activeCard()
        val uid = AuthManager.currentUid
        val fs = CloudMode.firestore
        if (card != null && uid != null && fs != null) {
            scope.launch {
                runCatching {
                    fs.collection("cards").document(card.id)
                        .update("nfcWrites", FieldValue.increment(1)).await()
                }
            }
        }
    }

    fun recordShare() {
        _shares.value += 1
        persist()
    }

    // ---------------------------------------------------------------------
    // Profile image upload (Storage)
    // ---------------------------------------------------------------------

    /** kind: "profile" or "logo" */
    fun uploadCardImage(cardId: String, imageUri: Uri, kind: String = "profile") {
        val context = appContext ?: return

        // 1) ALWAYS persist locally first — the card shows the image instantly
        //    and it survives restarts, with or without cloud.
        val card = cardById(cardId)
        val storedLocally = card?.let { setLocalImage(cardId, imageUri, kind) } ?: false

        // 2) Cloud mirror is best-effort: a missing bucket or signed-out state
        //    must never surface as a user-facing error.
        val storage = CloudMode.storage
        val uid = AuthManager.currentUid
        if (storage == null || uid == null || card == null || !storedLocally) {
            _uploadState.value = UploadState.SavedOnDevice
            return
        }
        scope.launch {
            try {
                _uploadState.value = UploadState.Uploading(5)
                val bytes = compressForUpload(context, imageUri, maxDim = 1024, quality = 85)
                _uploadState.value = UploadState.Uploading(35)
                val fileName = if (kind == "logo") "logo.jpg" else "profile.jpg"
                val ref = storage.reference.child("users/$uid/cards/$cardId/$fileName")
                ref.putBytes(bytes).await()
                _uploadState.value = UploadState.Uploading(80)
                val url = ref.downloadUrl.await().toString()
                _uploadState.value = UploadState.Uploading(95)
                update(
                    if (kind == "logo") card.copy(logoUrl = url)
                    else card.copy(profileImageUrl = url),
                )
                _uploadState.value = UploadState.Done
            } catch (e: Exception) {
                // Local copy already in place — degrade quietly.
                _uploadState.value = UploadState.SavedOnDevice
            }
        }
    }

    /** Local-mode image persistence: saves into internal storage, updates card. */
    fun setLocalImage(cardId: String, imageUri: Uri, kind: String = "profile"): Boolean {
        val context = appContext ?: return false
        val card = cardById(cardId) ?: return false
        val stored = ImageStore.persist(context, imageUri) ?: return false
        update(
            if (kind == "logo") card.copy(logoUrl = stored)
            else card.copy(profileImageUrl = stored),
        )
        return true
    }

    private suspend fun compressForUpload(
        context: Context,
        uri: Uri,
        maxDim: Int,
        quality: Int,
    ): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val src = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        } ?: throw IllegalArgumentException("Cannot read selected image")
        val scaled = if (maxOf(src.width, src.height) > maxDim) {
            val scale = maxDim.toFloat() / maxOf(src.width, src.height)
            Bitmap.createScaledBitmap(
                src,
                (src.width * scale).toInt().coerceAtLeast(1),
                (src.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else src
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val bytes = out.toByteArray()
        require(bytes.size <= 2 * 1024 * 1024) { "Image exceeds 2 MB after compression" }
        return bytes
    }

    // ---------------------------------------------------------------------
    // Cloud sync (write-through + migration + snapshot listener)
    // ---------------------------------------------------------------------

    /** Called by AuthManager consumers when auth state becomes LoggedIn. */
    fun onLoggedIn(uid: String) {
        scope.launch { ensureCloudSession(uid) }
    }

    /** Called on logout: detach listener; local cache remains (read-only cloud-wise). */
    fun onLoggedOut() {
        cardListenerRegistration?.remove()
        cardListenerRegistration = null
        _syncState.value = SyncState.Idle
        _migrationState.value = MigrationState.NotStarted
    }

    private suspend fun ensureCloudSession(uid: String) {
        Log.d("TapCardSync", "ensureCloudSession uid=$uid")
        val fs = CloudMode.firestore ?: run { Log.d("TapCardSync", "firestore NULL"); return }
        val ctx = appContext ?: return
        try {
            val alreadyMigrated = ctx.cardStore.data.first()[migratedKey(uid)] == true
            Log.d("TapCardSync", "alreadyMigrated=$alreadyMigrated")
            val cloudCards = fs.collection("cards").whereEqualTo("ownerUid", uid).get().await()
                .documents.mapNotNull { CardMappers.fromCardMap(it.id, it.data ?: emptyMap()) }

            Log.d("TapCardSync", "cloudCards=${cloudCards.size}")
            if (cloudCards.isNotEmpty()) {
                // Account already has cloud cards (another device / earlier run):
                // cloud is the source of truth, local stays as offline cache.
                _migrationState.value = MigrationState.CloudAlreadyHadCards
                adoptCloudSnapshot(cloudCards)
                markMigrated(uid)
            } else if (!alreadyMigrated) {
                // First login: import the local cards (v1.x → cloud).
                val local = _cards.value
                _migrationState.value = MigrationState.InProgress(local.size)
                var imported = 0
                for (card in local) {
                    val ok = pushWithSlugRetry(card, uid)
                    if (ok) imported++
                }
                Log.d("TapCardSync", "migration imported=$imported of ${local.size}")
                _migrationState.value = MigrationState.Done(imported)
                markMigrated(uid)
            } else {
                // Flag set but cloud is empty (a previous run failed before
                // persisting): retry the migration — idempotent because the
                // slug claims and card ids are stable.
                Log.d("TapCardSync", "flag set but cloud empty — retrying migration")
                val local = _cards.value
                _migrationState.value = MigrationState.InProgress(local.size)
                var imported = 0
                for (card in local) {
                    val ok = pushWithSlugRetry(card, uid)
                    if (ok) imported++
                }
                Log.d("TapCardSync", "retry migration imported=$imported of ${local.size}")
                _migrationState.value = MigrationState.Done(imported)
                markMigrated(uid)
            }
            // Write account doc + adopt live listener regardless of branch.
            fs.collection("users").document(uid)
                .set(
                    mapOf("email" to authEmail(), "createdAt" to System.currentTimeMillis()),
                    com.google.firebase.firestore.SetOptions.merge(),
                )
                .await()
            attachCardsListener(uid)
            _syncState.value = SyncState.Synced
        } catch (e: Exception) {
            Log.d("TapCardSync", "ensureCloudSession FAILED: ${e.javaClass.simpleName}: ${e.message}")
            _migrationState.value = MigrationState.Failed(e.message ?: "Migration failed")
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    private fun authEmail(): String? =
        (AuthManager.authState.value as? com.tapcard.app.data.auth.AuthState.LoggedIn)?.email

    private suspend fun markMigrated(uid: String) {
        val ctx = appContext ?: return
        ctx.cardStore.edit { it[migratedKey(uid)] = true }
    }

    private fun attachCardsListener(uid: String) {
        val fs = CloudMode.firestore ?: return
        cardListenerRegistration?.remove()
        cardListenerRegistration = fs.collection("cards")
            .whereEqualTo("ownerUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncState.value = SyncState.Error(error.message ?: "Live sync error")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val remote = snapshot.documents.mapNotNull {
                        CardMappers.fromCardMap(it.id, it.data ?: emptyMap())
                    }
                    adoptCloudSnapshot(remote)
                    _syncState.value = SyncState.Synced
                }
            }
    }

    private fun adoptCloudSnapshot(remote: List<BusinessCard>) {
        // 1) Deduplicate remote docs by slug (double-migration artifacts) —
        //    keep the most recently updated doc per slug.
        val deduped = remote
            .sortedByDescending { it.updatedAt }
            .distinctBy { it.slug.ifBlank { it.id } }
        // 2) Keep optimistic local cards that haven't reached the cloud yet,
        //    dropping locals whose slug already exists in the cloud.
        val remoteSlugs = deduped.map { it.slug.ifBlank { it.id } }.toSet()
        val remoteIds = deduped.map { it.id }.toSet()
        val pending = _cards.value
            .filter { it.id !in remoteIds }
            .filter { it.slug !in remoteSlugs }
            .distinctBy { it.slug.ifBlank { it.id } }
        _cards.value = deduped + pending
        if (_activeCardId.value !in _cards.value.map { it.id }) {
            _activeCardId.value = _cards.value.firstOrNull()?.id
        }
        persist()
    }

    private fun pushCard(card: BusinessCard) {
        val uid = AuthManager.currentUid ?: return
        if (CloudMode.firestore == null) return
        scope.launch { pushWithSlugRetry(card, uid, mutateLocalOnRename = true) }
    }

    private suspend fun pushWithSlugRetry(
        card: BusinessCard,
        uid: String,
        mutateLocalOnRename: Boolean = false,
    ): Boolean {
        val fs = CloudMode.firestore ?: return false
        var candidate = card.slug.ifBlank { slugify(card.name) }
        repeat(6) { attempt ->
            try {
                _syncState.value = SyncState.Syncing
                val current = if (attempt == 0) card else card.copy(slug = candidate)
                fs.runTransaction { tx ->
                    val slugRef = fs.collection("slugs").document(current.slug)
                    val existing = tx.get(slugRef)
                    if (existing.exists() && existing.getString("ownerUid") != uid) {
                        throw IllegalStateException("SLUG_TAKEN")
                    }
                    tx.set(
                        fs.collection("cards").document(current.id),
                        CardMappers.toCardMap(current, uid),
                    )
                    tx.set(slugRef, mapOf("cardId" to current.id, "ownerUid" to uid))
                    tx.set(
                        fs.collection("publicProfiles").document(current.slug),
                        CardMappers.toPublicProfileMap(current, uid),
                    )
                    null
                }.await()
                if (attempt > 0 && mutateLocalOnRename) {
                    val renamed = card.copy(slug = candidate)
                    _cards.update { list -> list.map { if (it.id == renamed.id) renamed else it } }
                    persist()
                }
                _syncState.value = SyncState.Synced
                return true
            } catch (e: Exception) {
                Log.d("TapCardSync", "push attempt ${attempt + 1} FAILED: ${e.javaClass.simpleName}: ${e.message}")
                if (e.message?.contains("SLUG_TAKEN") == true) {
                    candidate = "${card.slug.ifBlank { slugify(card.name) }}-${attempt + 2}"
                } else {
                    _syncState.value = SyncState.Error(e.message ?: "Cloud write failed")
                    return false
                }
            }
        }
        return false
    }

    private fun pushDelete(card: BusinessCard) {
        val uid = AuthManager.currentUid ?: return
        val fs = CloudMode.firestore ?: return
        scope.launch {
            try {
                _syncState.value = SyncState.Syncing
                fs.runBatch { batch ->
                    batch.delete(fs.collection("cards").document(card.id))
                    batch.delete(fs.collection("slugs").document(card.slug))
                    batch.delete(fs.collection("publicProfiles").document(card.slug))
                }.await()
                _syncState.value = SyncState.Synced
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Cloud delete failed")
            }
        }
    }

    // ---------------------------------------------------------------------
    // Persistence (v1.1 logic unchanged)
    // ---------------------------------------------------------------------

    private fun persist() {
        val ctx = appContext ?: return
        val doc = StoreDocument(
            cards = _cards.value,
            activeCardId = _activeCardId.value,
            nfcWrites = _nfcWrites.value,
            shares = _shares.value,
        )
        scope.launch {
            try {
                ctx.cardStore.edit { prefs ->
                    prefs[KEY_DOC] = json.encodeToString(StoreDocument.serializer(), doc)
                    prefs[KEY_SCHEMA] = 1
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun uniqueSlugFor(name: String): String {
        val base = slugify(name)
        val existing = _cards.value.map { it.slug }.toSet()
        if (base !in existing) return base
        var n = 2
        while ("$base-$n" in existing) n++
        return "$base-$n"
    }

    private fun seed(): List<BusinessCard> = listOf(
        BusinessCard(
            id = UUID.randomUUID().toString(),
            name = "Alex Morgan",
            jobTitle = "Product Designer",
            company = "Northwind Studio",
            phone = "+1 415 555 0134",
            email = "alex@northwind.studio",
            website = "https://northwind.studio",
            socials = listOf(
                SocialLink("LinkedIn", "https://linkedin.com/in/alexmorgan"),
                SocialLink("Dribbble", "https://dribbble.com/alexmorgan"),
            ),
            accentIndex = 0,
            slug = "alex-morgan",
            templateIndex = 0,
        ),
        BusinessCard(
            id = UUID.randomUUID().toString(),
            name = "Sarah Chen",
            jobTitle = "Founder & CEO",
            company = "Brightline Labs",
            phone = "+1 206 555 0177",
            email = "sarah@brightline.io",
            website = "https://brightline.io",
            socials = listOf(
                SocialLink("LinkedIn", "https://linkedin.com/in/sarahchen"),
                SocialLink("X", "https://x.com/sarahchen"),
            ),
            accentIndex = 3,
            slug = "sarah-chen",
            templateIndex = 2,
        ),
    )
}
