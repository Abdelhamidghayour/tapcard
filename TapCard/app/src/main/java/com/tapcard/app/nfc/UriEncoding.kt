package com.tapcard.app.nfc

/**
 * Pure JVM encoding of NFC Forum URI records (RTD_URI 1.0) — separated from
 * android.nfc types so it is unit-testable on the JVM.
 *
 * Payload layout: byte 0 = URI identifier code (index into [PREFIXES]),
 * bytes 1.. = remainder of the URI, UTF-8.
 */
object UriEncoding {

    /** NFC Forum URI identifier codes 0x00–0x23. Index == identifier code. */
    val PREFIXES: List<String> = listOf(
        "", // 0x00 no prefix
        "http://www.", // 0x01
        "https://www.", // 0x02
        "http://", // 0x03
        "https://", // 0x04
        "tel:", // 0x05
        "mailto:", // 0x06
        "ftp://anonymous:anonymous@", // 0x07
        "ftp://ftp.", // 0x08
        "ftps://", // 0x09
        "sftp://", // 0x0A
        "smb://", // 0x0B
        "nfs://", // 0x0C
        "ftp://", // 0x0D
        "dav://", // 0x0E
        "news:", // 0x0F
        "telnet://", // 0x10
        "imap:", // 0x11
        "rtsp://", // 0x12
        "urn:", // 0x13
        "pop:", // 0x14
        "sip:", // 0x15
        "sips:", // 0x16
        "tftp:", // 0x17
        "bt:", // 0x18
        "btspp:", // 0x19
        "btl2cap:", // 0x1A
        "btgoep:", // 0x1B
        "tcpobex:", // 0x1C
        "irdaobex:", // 0x1D
        "file:", // 0x1E
        "urn:epc:id:", // 0x1F
        "urn:epc:tag:", // 0x20
        "urn:epc:pat:", // 0x21
        "urn:epc:raw:", // 0x22
        "urn:epc:", // 0x23
        "urn:nfc:", // 0x24
    )

    /**
     * Splits [uri] into (identifier code, remainder). Chooses the longest
     * matching well-known prefix (index 0 = "no prefix" never matches a
     * shorter alternative).
     */
    fun split(uri: String): Pair<Int, String> {
        var bestIndex = 0
        var bestLength = 0
        PREFIXES.forEachIndexed { index, prefix ->
            if (index > 0 && prefix.length > bestLength && uri.startsWith(prefix)) {
                bestIndex = index
                bestLength = prefix.length
            }
        }
        return bestIndex to uri.substring(bestLength)
    }
}
