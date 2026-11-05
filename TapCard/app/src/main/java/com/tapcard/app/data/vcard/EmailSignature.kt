package com.tapcard.app.data.vcard

import com.tapcard.app.data.model.BusinessCard

/**
 * Generates a professional HTML email signature block from a card
 * (same feature class as HiHello's email signatures). Copy-paste ready.
 */
object EmailSignature {

    fun buildHtml(card: BusinessCard): String {
        val role = listOf(card.jobTitle, card.company).filter { it.isNotBlank() }
            .joinToString(" · ")
        val contactLine = listOfNotNull(
            card.phone?.let { "<a href=\"tel:${it.replace(" ", "")}\">$it</a>" },
            card.email?.let { "<a href=\"mailto:$it\">$it</a>" },
        ).joinToString(" &nbsp;·&nbsp; ")
        return buildString {
            append("<div style=\"font-family:Georgia,serif;color:#101828;\">")
            append("<div style=\"border-top:2px solid #C9A961;padding-top:8px;\">")
            append("<span style=\"font-size:15pt;font-weight:bold;\">${escapeHtml(card.name)}</span>")
            if (role.isNotBlank()) {
                append("<br><span style=\"color:#6B6F7A;\">${escapeHtml(role)}</span>")
            }
            if (contactLine.isNotBlank()) {
                append("<br><span style=\"font-size:9pt;\">$contactLine</span>")
            }
            if (!card.website.isNullOrBlank()) {
                append("<br><span style=\"font-size:9pt;\"><a href=\"${card.website}\">${card.website}</a></span>")
            }
            append("<br><span style=\"font-size:9pt;\"><a href=\"${card.publicUrl}\" style=\"color:#8C7038;\">${card.publicUrl}</a></span>")
            append("<br><span style=\"font-size:7.5pt;color:#9AA0AC;letter-spacing:2px;\">TAPCARD</span>")
            append("</div></div>")
        }
    }

    private fun escapeHtml(v: String): String =
        v.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
