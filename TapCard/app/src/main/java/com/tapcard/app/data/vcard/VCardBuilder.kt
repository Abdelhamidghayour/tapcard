package com.tapcard.app.data.vcard

import com.tapcard.app.data.model.BusinessCard

/**
 * Builds RFC-6350-style vCard 3.0 text for contact sharing. Used by the
 * share sheet / clipboard and shown verbatim in the card preview.
 */
object VCardBuilder {

    fun build(card: BusinessCard): String = buildString {
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("FN:${escape(card.name)}")
        append("N:")
        val parts = card.name.trim().split(Regex("\\s+"))
        if (parts.isNotEmpty()) {
            append(escape(parts.last()))
            append(';')
            if (parts.size > 1) append(escape(parts.first()))
        }
        appendLine()
        if (card.jobTitle.isNotBlank()) appendLine("TITLE:${escape(card.jobTitle)}")
        if (card.company.isNotBlank()) appendLine("ORG:${escape(card.company)}")
        if (!card.phone.isNullOrBlank()) appendLine("TEL;TYPE=CELL:${escape(card.phone)}")
        if (!card.email.isNullOrBlank()) appendLine("EMAIL;TYPE=INTERNET:${escape(card.email)}")
        if (!card.website.isNullOrBlank()) appendLine("URL:${escape(card.website!!)}")
        card.socials.forEach { social ->
            appendLine("X-SOCIALPROFILE;TYPE=${escape(social.platform.lowercase())}:${escape(social.url)}")
        }
        appendLine("URL:${escape(card.publicUrl)}")
        appendLine("NOTE:Shared with TapCard")
        append("END:VCARD")
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,")
}
