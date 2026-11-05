package com.tapcard.app.data.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OcrFieldParserTest {

    @Test
    fun `typical business card parses all fields`() {
        val text = """
            ALEX MORGAN
            Product Designer
            Northwind Studio LLC
            120 Market Street, Suite 400
            San Francisco, CA 94105
            +1 415 555 0134
            alex@northwind.studio
            www.northwind.studio
        """.trimIndent()
        val f = OcrFieldParser.parse(text)
        assertEquals("ALEX MORGAN", f.name)
        assertEquals("Product Designer", f.jobTitle)
        assertEquals("Northwind Studio LLC", f.company)
        assertEquals("+1 415 555 0134", f.phone)
        assertEquals("alex@northwind.studio", f.email)
        assertEquals("https://www.northwind.studio", f.website)
        assertEquals(true, f.address?.contains("Market Street"))
    }

    @Test
    fun `phone is not confused with url digits`() {
        val f = OcrFieldParser.parse("CALL +1 (206) 555-0177\nbrightline.io")
        assertEquals("+1 (206) 555-0177", f.phone)
        assertEquals("https://brightline.io", f.website)
    }

    @Test
    fun `role keyword line becomes job title`() {
        val f = OcrFieldParser.parse("Sarah Chen\nFounder & CEO\nBrightline Labs")
        assertEquals("Sarah Chen", f.name)
        assertEquals("Founder & CEO", f.jobTitle)
        assertEquals("Brightline Labs", f.company)
    }

    @Test
    fun `email takes precedence over url on same line`() {
        val f = OcrFieldParser.parse("hello@company.com")
        assertEquals("hello@company.com", f.email)
        assertNull(f.website)
    }

    @Test
    fun `blank text yields empty fields`() {
        val f = OcrFieldParser.parse("")
        assertNull(f.name)
        assertNull(f.email)
    }

    @Test
    fun `garbage digits do not become phone`() {
        val f = OcrFieldParser.parse("123456789012345678901234567890")
        assertNull(f.phone)
    }

    @Test
    fun `parse is stateless`() {
        val a = OcrFieldParser.parse("Ann Lee\nDesigner")
        val b = OcrFieldParser.parse("Bob Ray\nEngineer")
        assertEquals("Ann Lee", a.name)
        assertEquals("Bob Ray", b.name)
    }
}
