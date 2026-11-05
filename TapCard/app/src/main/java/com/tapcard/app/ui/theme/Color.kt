package com.tapcard.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// TapCard "PLASMA" identity — 2026 edition
// Deep void canvas, violet plasma glow, lime punch, glass panels.
// ---------------------------------------------------------------------------

// Plasma core (primary gradient + glow)
val PlasmaStart = Color(0xFF7C3AED)
val PlasmaMid = Color(0xFFA855F7)
val PlasmaEnd = Color(0xFFC084FC)
val PlasmaGlow = Color(0xFFA855F7)

// Lime punch (active / success / highlight)
val Lime = Color(0xFFD7F647)
val LimeDeep = Color(0xFFB7DC26)

// Dark canvas
val VoidBlack = Color(0xFF08070D)
val PanelDark = Color(0xFF12111A)
val PanelDarkElevated = Color(0xFF1A1826)
val BorderViolet = Color(0xFF2A2440)

// Text
val TextBright = Color(0xFFF5F3FA)
val TextDim = Color(0xFFA7A3B8)

// Light theme neutrals (violet-tinted warm white)
val LBg = Color(0xFFF4F2FA)
val LSurface = Color(0xFFFFFFFF)
val LSurfaceVariant = Color(0xFFECE9F6)
val LOutline = Color(0xFFD9D4EA)

// Semantic
val Success = Color(0xFF2F9E77)
val ErrorRed = Color(0xFFC4442A)
val ErrorRedDark = Color(0xFFE27B62)

// Light scheme tokens
val LPrimary = PlasmaStart
val LSecondary = LimeDeep
val LOnBg = Color(0xFF141220)

// Dark scheme tokens
val DPrimary = PlasmaMid
val DSecondary = Lime

// Brand gradient — plasma (used by GradientButton across the app)
val BrandGradient = Brush.linearGradient(listOf(PlasmaStart, PlasmaMid, PlasmaEnd))
val BrandGradientColors = listOf(PlasmaStart, PlasmaMid, PlasmaEnd)

/** Soft radial glow used behind buttons/avatars/cards. */
fun plasmaGlow(alpha: Float = 0.45f): Brush = Brush.radialGradient(
    listOf(PlasmaGlow.copy(alpha = alpha), Color.Transparent),
)

// ---------------------------------------------------------------------------
// Social platform brand colors (colorful icon circles like ProfileTap/Orbix)
// ---------------------------------------------------------------------------
data class PlatformStyle(val color: Color, val label: String)

private val platformMap = mapOf(
    "linkedin" to Color(0xFF0A66C2),
    "whatsapp" to Color(0xFF25D366),
    "instagram" to Color(0xFFE1306C),
    "x" to Color(0xFF202020),
    "github" to Color(0xFF24292F),
    "youtube" to Color(0xFFFF0000),
    "tiktok" to Color(0xFFEE1D52),
    "facebook" to Color(0xFF1877F2),
    "website" to Color(0xFF64748B),
)

fun platformColor(name: String): Color =
    platformMap[name.trim().lowercase()] ?: Color(0xFF64748B)

// ---------------------------------------------------------------------------
// Card templates (index-aligned with data model `templateIndex`)
// ---------------------------------------------------------------------------
data class CardTemplate(
    val name: String,
    val background: Brush,
    val ink: Color,
    val subInk: Color,
    val accent: Color,
    val isDarkSurface: Boolean,
    val serifName: Boolean,
)

val CardTemplates = listOf(
    CardTemplate( // 0 — Midnight (now plasma-coherent: violet accent, sans name)
        name = "Midnight",
        background = Brush.linearGradient(listOf(Color(0xFF0E0C18), Color(0xFF1B1533))),
        ink = TextBright,
        subInk = TextDim,
        accent = PlasmaMid,
        isDarkSurface = true,
        serifName = false,
    ),
    CardTemplate( // 1 — Champagne Ivory (deliberate light alternate)
        name = "Champagne",
        background = Brush.linearGradient(listOf(Color(0xFFF7F3EA), Color(0xFFEFE8D9))),
        ink = Color(0xFF101828),
        subInk = Color(0xFF6E6A5E),
        accent = Color(0xFFA98B4F),
        isDarkSurface = false,
        serifName = true,
    ),
    CardTemplate( // 2 — Plasma Violet (signature 2026 template, stronger glow)
        name = "Plasma",
        background = Brush.linearGradient(listOf(Color(0xFF17101F), Color(0xFF3B1D54))),
        ink = Color(0xFFF3EFFF),
        subInk = Color(0xFFB9A6D6),
        accent = PlasmaEnd,
        isDarkSurface = true,
        serifName = false,
    ),
    CardTemplate( // 3 — Editorial Mono (deliberate light alternate)
        name = "Editorial",
        background = Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF5F4F1))),
        ink = Color(0xFF171717),
        subInk = Color(0xFF71716B),
        accent = Color(0xFF171717),
        isDarkSurface = false,
        serifName = false,
    ),
)

fun cardTemplate(index: Int): CardTemplate =
    CardTemplates[index.coerceIn(0, CardTemplates.lastIndex)]

// Legacy accent palette kept for avatar personalization (5 options)
val CardAccents: List<Pair<Color, Color>> = listOf(
    Color(0xFF7C3AED) to Color(0xFFA855F7), // plasma
    Color(0xFF101828) to Color(0xFF1D2433), // navy
    Color(0xFFA98B4F) to Color(0xFF8C7038), // gold
    Color(0xFF2F9E77) to Color(0xFF1E7A5C), // emerald
    Color(0xFF171717) to Color(0xFF3A3A38), // graphite
)

fun accentBrush(index: Int): Brush {
    val pair = CardAccents[index.coerceIn(0, CardAccents.lastIndex)]
    return Brush.linearGradient(listOf(pair.first, pair.second))
}

fun accentColor(index: Int): Color = CardAccents[index.coerceIn(0, CardAccents.lastIndex)].first

// Social platforms offered in the editor — most popular first (user request:
// WhatsApp / LinkedIn / Instagram / Facebook up front)
val SocialPlatforms: List<Pair<String, String>> = listOf(
    "WhatsApp" to "https://wa.me/",
    "LinkedIn" to "https://linkedin.com/in/",
    "Instagram" to "https://instagram.com/",
    "Facebook" to "https://facebook.com/",
    "X" to "https://x.com/",
    "GitHub" to "https://github.com/",
    "YouTube" to "https://youtube.com/@",
    "TikTok" to "https://tiktok.com/@",
    "Website" to "",
)
