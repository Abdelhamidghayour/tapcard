package com.tapcard.app.data.cloud

import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.data.model.CustomLink
import com.tapcard.app.data.model.SocialLink
import com.tapcard.app.data.model.slugify

/**
 * Pure JVM helpers for card ↔ Firestore document mapping and slug planning.
 * No Android/Firebase types here so everything is unit-testable.
 */
object CardMappers {

    /** Fields allowed in publicProfiles (strict subset — see security rules). */
    val PUBLIC_FIELDS = listOf(
        "name", "jobTitle", "company", "email", "phone", "website",
        "socials", "profileImageUrl", "logoUrl", "address", "about",
        "accentIndex", "templateIndex", "landscape", "headerImage", "links",
        "ownerUid", "cardId", "updatedAt",
    )

    fun toCardMap(card: BusinessCard, ownerUid: String): Map<String, Any?> = mapOf(
        "ownerUid" to ownerUid,
        "name" to card.name,
        "jobTitle" to card.jobTitle,
        "company" to card.company,
        "email" to card.email,
        "phone" to card.phone,
        "website" to card.website,
        "socials" to card.socials.map { mapOf("platform" to it.platform, "url" to it.url) },
        "links" to card.links.map { mapOf("label" to it.label, "url" to it.url) },
        "profileImageUrl" to card.profileImageUrl,
        "logoUrl" to card.logoUrl,
        "address" to card.address,
        "about" to card.about,
        "accentIndex" to card.accentIndex.toLong(),
        "templateIndex" to card.templateIndex.toLong(),
        "landscape" to card.landscape,
        "headerImage" to card.headerImage,
        "slug" to card.slug,
        "isPublic" to true,
        "createdAt" to card.createdAt,
        "updatedAt" to card.updatedAt,
    )

    fun toPublicProfileMap(card: BusinessCard, ownerUid: String): Map<String, Any?> = mapOf(
        "name" to card.name,
        "jobTitle" to card.jobTitle,
        "company" to card.company,
        "email" to card.email,
        "phone" to card.phone,
        "website" to card.website,
        "socials" to card.socials.map { mapOf("platform" to it.platform, "url" to it.url) },
        "profileImageUrl" to card.profileImageUrl,
        "accentIndex" to card.accentIndex.toLong(),
        "ownerUid" to ownerUid,
        "cardId" to card.id,
        "updatedAt" to card.updatedAt,
    )

    @Suppress("UNCHECKED_CAST")
    fun fromCardMap(id: String, data: Map<String, Any?>): BusinessCard = BusinessCard(
        id = id,
        name = data["name"] as? String ?: "",
        jobTitle = data["jobTitle"] as? String ?: "",
        company = data["company"] as? String ?: "",
        phone = data["phone"] as? String,
        email = data["email"] as? String,
        website = data["website"] as? String,
        socials = (data["socials"] as? List<Map<String, Any>>)?.map {
            SocialLink(
                platform = it["platform"] as? String ?: "",
                url = it["url"] as? String ?: "",
            )
        } ?: emptyList(),
        links = (data["links"] as? List<Map<String, Any>>)?.map {
            CustomLink(
                label = it["label"] as? String ?: "",
                url = it["url"] as? String ?: "",
            )
        } ?: emptyList(),
        accentIndex = (data["accentIndex"] as? Number)?.toInt() ?: 0,
        slug = data["slug"] as? String ?: slugify(data["name"] as? String ?: ""),
        createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L,
        profileImageUrl = data["profileImageUrl"] as? String,
        logoUrl = data["logoUrl"] as? String,
        address = data["address"] as? String,
        about = data["about"] as? String,
        templateIndex = (data["templateIndex"] as? Number)?.toInt() ?: 0,
        landscape = data["landscape"] as? Boolean ?: false,
        headerImage = data["headerImage"] as? String ?: "auto",
    )
}

/**
 * Pure migration planner: decides which local cards to push to the cloud and
 * how to resolve slug collisions deterministically.
 */
object CloudMigrator {

    data class Plan(
        val cardsToCreate: List<BusinessCard>,
        val skippedBecauseCloudHasCards: Boolean = false,
    )

    /**
     * @param localCards  v1.1 DataStore cards
     * @param cloudCards  existing cloud cards for the authenticated uid
     * @param cloudSlugs  slugs already registered globally by ANY user
     */
    fun plan(
        localCards: List<BusinessCard>,
        cloudCards: List<BusinessCard>,
        cloudSlugs: Set<String>,
    ): Plan {
        if (cloudCards.isNotEmpty()) {
            // Never clobber existing cloud data; local stays as legacy cache.
            return Plan(cardsToCreate = emptyList(), skippedBecauseCloudHasCards = true)
        }
        val used = cloudSlugs.toMutableSet()
        val toCreate = localCards.map { card ->
            var candidate = card.slug.ifBlank { slugify(card.name) }
            var n = 2
            while (candidate in used) {
                candidate = "${card.slug.ifBlank { slugify(card.name) }}-$n"
                n++
            }
            used.add(candidate)
            card.copy(slug = candidate)
        }
        return Plan(cardsToCreate = toCreate)
    }

    fun validateSlug(slug: String): Boolean =
        slug.matches(Regex("^[a-z0-9][a-z0-9-]{2,39}$"))
}
