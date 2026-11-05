package com.tapcard.app.data

import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.data.model.slugify
import com.tapcard.app.data.vcard.VCardBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelAndVCardTest {

    @Test
    fun `slugify lowercases and hyphenates`() {
        assertEquals("alex-morgan", slugify("Alex Morgan"))
        assertEquals("sarah-chen-2", slugify("Sarah  Chen 2"))
        assertEquals("card", slugify("!!!"))
        assertEquals("jean-luc-picard", slugify("Jean-Luc Picard"))
    }

    @Test
    fun `public url is base plus slug`() {
        val card = BusinessCard(id = "1", name = "Alex Morgan", slug = "alex-morgan")
        assertEquals("https://tapcard.app/p/alex-morgan", card.publicUrl)
    }

    @Test
    fun `initials takes first two words`() {
        assertEquals("AM", BusinessCard(id = "1", name = "Alex Morgan Jr").initials)
        assertEquals("A", BusinessCard(id = "1", name = "Alex").initials)
    }

    @Test
    fun `vcard contains core fields`() {
        val card = BusinessCard(
            id = "1",
            name = "Alex Morgan",
            jobTitle = "Product Designer",
            company = "Northwind Studio",
            phone = "+1 415 555 0134",
            email = "alex@northwind.studio",
            website = "https://northwind.studio",
            slug = "alex-morgan",
        )
        val v = VCardBuilder.build(card)
        assertTrue(v.startsWith("BEGIN:VCARD"))
        assertTrue(v.trimEnd().endsWith("END:VCARD"))
        assertTrue(v.contains("FN:Alex Morgan"))
        assertTrue(v.contains("TITLE:Product Designer"))
        assertTrue(v.contains("ORG:Northwind Studio"))
        assertTrue(v.contains("TEL;TYPE=CELL:+1 415 555 0134"))
        assertTrue(v.contains("EMAIL;TYPE=INTERNET:alex@northwind.studio"))
        assertTrue(v.contains("https://tapcard.app/p/alex-morgan"))
        assertFalse(v.contains("null"))
    }

    @Test
    fun `vcard escapes special characters`() {
        val card = BusinessCard(
            id = "1",
            name = "Doe, John",
            company = "ACME; Inc",
            slug = "doe-john",
        )
        val v = VCardBuilder.build(card)
        assertTrue(v.contains("FN:Doe\\, John"))
        assertTrue(v.contains("ORG:ACME\\; Inc"))
    }
}
