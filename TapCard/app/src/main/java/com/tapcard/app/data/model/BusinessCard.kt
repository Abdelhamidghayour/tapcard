package com.tapcard.app.data.model

import kotlinx.serialization.Serializable

/**
 * A digital business card. The public URL below is what gets written to NFC
 * tags (a single NFC Forum RTD_URI record) and encoded into QR codes.
 */
@Serializable
data class BusinessCard(
    val id: String,
    val name: String,
    val jobTitle: String = "",
    val company: String = "",
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val socials: List<SocialLink> = emptyList(),
    val accentIndex: Int = 0,
    val slug: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val profileImageUrl: String? = null,
    val logoUrl: String? = null,
    val address: String? = null,
    /** Short "About me" bio shown on the profile and public page. */
    val about: String? = null,
    /** Index into CardTemplates (0 = Midnight Executive). */
    val templateIndex: Int = 0,
) {
    /** Public profile page. Backend replaces the host once deployed. */
    val publicUrl: String get() = "$PUBLIC_URL_BASE$slug"
    val initials: String
        get() = name.trim().split(Regex("\\s+")).take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

    companion object {
        const val PUBLIC_URL_BASE = "https://tapcard.app/p/"
    }
}

@Serializable
data class SocialLink(
    val platform: String, // e.g. LinkedIn, X, Instagram, GitHub
    val url: String,
)

fun slugify(name: String): String = name.trim()
    .lowercase()
    .replace(Regex("[^a-z0-9]+"), "-")
    .trim('-')
    .take(32)
    .ifEmpty { "card" }
