package com.tapcard.app.data.cloud

import com.tapcard.app.data.model.BusinessCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudMigratorTest {

    private fun card(name: String, slug: String) = BusinessCard(
        id = "id-$name",
        name = name,
        slug = slug,
    )

    @Test
    fun `empty cloud pushes all local cards`() {
        val local = listOf(card("Alex Morgan", "alex-morgan"), card("Sarah Chen", "sarah-chen"))
        val plan = CloudMigrator.plan(local, cloudCards = emptyList(), cloudSlugs = emptySet())
        assertEquals(2, plan.cardsToCreate.size)
        assertFalse(plan.skippedBecauseCloudHasCards)
    }

    @Test
    fun `cloud with existing cards skips migration`() {
        val local = listOf(card("Alex Morgan", "alex-morgan"))
        val cloud = listOf(card("Cloud Card", "cloud-card"))
        val plan = CloudMigrator.plan(local, cloud, cloudSlugs = emptySet())
        assertTrue(plan.cardsToCreate.isEmpty())
        assertTrue(plan.skippedBecauseCloudHasCards)
    }

    @Test
    fun `slug taken by another user gets numeric suffix`() {
        val local = listOf(card("Alex Morgan", "alex-morgan"))
        val plan = CloudMigrator.plan(local, cloudCards = emptyList(), cloudSlugs = setOf("alex-morgan"))
        assertEquals("alex-morgan-2", plan.cardsToCreate.first().slug)
    }

    @Test
    fun `collision cascade increments until free`() {
        val local = listOf(card("Alex Morgan", "alex-morgan"))
        val plan = CloudMigrator.plan(
            local,
            emptyList(),
            cloudSlugs = setOf("alex-morgan", "alex-morgan-2", "alex-morgan-3"),
        )
        assertEquals("alex-morgan-4", plan.cardsToCreate.first().slug)
    }

    @Test
    fun `multiple local cards each get unique slugs`() {
        val local = listOf(
            card("Alex Morgan", "alex-morgan"),
            card("Alex Morgan", "alex-morgan"), // duplicate name locally (shouldn't happen but must stay safe)
        )
        val plan = CloudMigrator.plan(local, emptyList(), cloudSlugs = emptySet())
        assertEquals(setOf("alex-morgan", "alex-morgan-2"), plan.cardsToCreate.map { it.slug }.toSet())
    }

    @Test
    fun `blank slug falls back to slugified name`() {
        val local = listOf(card("Jean-Luc Picard", ""))
        val plan = CloudMigrator.plan(local, emptyList(), cloudSlugs = emptySet())
        assertEquals("jean-luc-picard", plan.cardsToCreate.first().slug)
    }

    @Test
    fun `slug validation matches rules regex`() {
        assertTrue(CloudMigrator.validateSlug("alex-morgan"))
        assertTrue(CloudMigrator.validateSlug("abc"))
        assertFalse(CloudMigrator.validateSlug("ab")) // too short
        assertFalse(CloudMigrator.validateSlug("-bad")) // must start alnum
        assertFalse(CloudMigrator.validateSlug("Bad-Case")) // lowercase only
        assertFalse(CloudMigrator.validateSlug("a".repeat(41))) // too long
    }
}

class CardMappersTest {

    private val card = BusinessCard(
        id = "card-1",
        name = "Alex Morgan",
        jobTitle = "Designer",
        company = "Northwind",
        phone = "+1 415",
        email = "alex@x.io",
        website = "https://northwind.studio",
        socials = listOf(com.tapcard.app.data.model.SocialLink("LinkedIn", "https://linkedin.com/in/alex")),
        accentIndex = 2,
        slug = "alex-morgan",
        createdAt = 1000L,
        updatedAt = 2000L,
    )

    @Test
    fun `card map carries owner and public flag`() {
        val map = CardMappers.toCardMap(card, ownerUid = "uid-1")
        assertEquals("uid-1", map["ownerUid"])
        assertEquals(true, map["isPublic"])
        assertEquals("alex-morgan", map["slug"])
        assertEquals(2L, map["accentIndex"])
    }

    @Test
    fun `public profile map contains only whitelisted keys`() {
        val map = CardMappers.toPublicProfileMap(card, ownerUid = "uid-1")
        val unexpected = map.keys.filter { it !in CardMappers.PUBLIC_FIELDS }
        assertTrue("unexpected keys: $unexpected", unexpected.isEmpty())
        // and never carries the private isPublic/createdAt fields
        assertFalse(map.containsKey("isPublic"))
        assertFalse(map.containsKey("createdAt"))
    }

    @Test
    fun `roundtrip card map preserves fields`() {
        val map = CardMappers.toCardMap(card, ownerUid = "uid-1")
        val restored = CardMappers.fromCardMap("card-1", map)
        assertEquals(card, restored)
    }

    @Test
    fun `fromCardMap tolerates missing fields`() {
        val restored = CardMappers.fromCardMap("card-2", mapOf("name" to "Solo"))
        assertEquals("Solo", restored.name)
        assertEquals("", restored.jobTitle)
        assertEquals(0, restored.accentIndex)
    }
}
