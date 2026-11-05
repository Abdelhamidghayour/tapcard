package com.tapcard.app.nfc

import org.junit.Assert.assertEquals
import org.junit.Test

class UriEncodingTest {

    @Test
    fun `https url uses prefix code 4`() {
        val (code, rest) = UriEncoding.split("https://tapcard.app/p/alex-morgan")
        assertEquals(0x04, code)
        assertEquals("tapcard.app/p/alex-morgan", rest)
    }

    @Test
    fun `https www url prefers longer prefix 2`() {
        val (code, rest) = UriEncoding.split("https://www.example.com/card")
        assertEquals(0x02, code)
        assertEquals("example.com/card", rest)
    }

    @Test
    fun `http url uses prefix code 3`() {
        val (code, rest) = UriEncoding.split("http://example.com")
        assertEquals(0x03, code)
        assertEquals("example.com", rest)
    }

    @Test
    fun `tel url uses prefix code 5`() {
        val (code, rest) = UriEncoding.split("tel:+14155550134")
        assertEquals(0x05, code)
        assertEquals("+14155550134", rest)
    }

    @Test
    fun `mailto url uses prefix code 6`() {
        val (code, rest) = UriEncoding.split("mailto:alex@northwind.studio")
        assertEquals(0x06, code)
        assertEquals("alex@northwind.studio", rest)
    }

    @Test
    fun `unknown scheme falls back to no prefix`() {
        val (code, rest) = UriEncoding.split("ftp://files.example.com/x")
        assertEquals(0x0D, code) // "ftp://" is a defined prefix (0x0D)
        assertEquals("files.example.com/x", rest)
    }

    @Test
    fun `custom scheme has no prefix`() {
        val (code, rest) = UriEncoding.split("myapp://open/card/42")
        assertEquals(0x00, code)
        assertEquals("myapp://open/card/42", rest)
    }

    @Test
    fun `payload roundtrip reconstructs uri`() {
        val urls = listOf(
            "https://tapcard.app/p/alex-morgan",
            "https://www.hihello.com/p/some-id",
            "tel:+14155550134",
            "mailto:a@b.co",
            "myapp://x",
        )
        urls.forEach { url ->
            val (code, rest) = UriEncoding.split(url)
            assertEquals(url, UriEncoding.PREFIXES[code] + rest)
        }
    }
}
