package com.tapcard.app.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import com.tapcard.app.data.model.BusinessCard

/**
 * Builds the NDEF message written to business card tags.
 *
 * Payload design (informed by the HiHello reference analysis, FINAL_NFC_RESEARCH.md):
 * one NFC Forum well-known RTD_URI record containing the public profile URL.
 * URI records work on Android (NFC tag dispatch), iOS background reading and
 * locked screens — maximum compatibility for a business card tap.
 */
object NdefPayloadBuilder {

    fun uriRecord(uri: String): NdefRecord {
        require(uri.isNotBlank()) { "URI must not be blank" }
        val (prefixCode, remainder) = UriEncoding.split(uri)
        val payload = byteArrayOf(prefixCode.toByte()) +
            remainder.toByteArray(Charsets.UTF_8)
        return NdefRecord(
            NdefRecord.TNF_WELL_KNOWN,
            NdefRecord.RTD_URI,
            ByteArray(0),
            payload,
        )
    }

    /**
     * The message programmed into a TapCard NFC tag: a single URI record.
     * Kept explicit and minimal — one record is the most universally
     * readable payload.
     */
    fun cardMessage(card: BusinessCard): NdefMessage =
        NdefMessage(arrayOf(uriRecord(card.publicUrl)))
}
