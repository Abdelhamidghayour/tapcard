package com.tapcard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.ui.theme.CardTemplate
import com.tapcard.app.ui.theme.cardTemplate

/*
 * Premium digital business card — three visual sections.
 *
 *  ┌───────────────────────┐
 *  │  HEADER (brand)       │  logo / photo / initials (headerImage policy)
 *  │  ───────────────      │  hairline divider
 *  │  NAME (display)       │  serif or sans per template
 *  │  title · company      │
 *  │  ───────────────      │
 *  │  CONTACT (rows)       │  icon + value, only provided fields
 *  │  social dots          │  brand-colored circles
 *  └───────────────────────┘
 *
 * Portrait ratio 0.68, landscape ratio 1.586 (credit-card). Landscape flips to
 * a two-column arrangement.
 */

/** Resolves which image decorates the header, honoring the user's policy. */
private fun headerResolution(card: BusinessCard): HeaderVisual {
    val hasLogo = !card.logoUrl.isNullOrBlank()
    val hasPhoto = !card.profileImageUrl.isNullOrBlank()
    return when (card.headerImage) {
        "logo" -> if (hasLogo) HeaderVisual.Image(card.logoUrl!!, true) else HeaderVisual.Initials
        "photo" -> if (hasPhoto) HeaderVisual.Image(card.profileImageUrl!!, false) else HeaderVisual.Initials
        "initials" -> HeaderVisual.Initials
        else -> when {
            hasLogo -> HeaderVisual.Image(card.logoUrl!!, true)
            hasPhoto -> HeaderVisual.Image(card.profileImageUrl!!, false)
            else -> HeaderVisual.Initials
        }
    }
}

private sealed interface HeaderVisual {
    /** url + isLogo (logo renders in a rounded square, photo in a circle). */
    data class Image(val url: String, val isLogo: Boolean) : HeaderVisual
    data object Initials : HeaderVisual
}

@Composable
fun CardSurface(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (card.landscape && !compact) {
        LandscapeCard(card, modifier)
    } else {
        PortraitCard(card, modifier, compact)
    }
}

// ---------------------------------------------------------------------------
// PORTRAIT
// ---------------------------------------------------------------------------

@Composable
private fun PortraitCard(card: BusinessCard, modifier: Modifier = Modifier, compact: Boolean = false) {
    val t = cardTemplate(card.templateIndex)
    val header = headerResolution(card)
    val ratio = if (compact) 0.72f else 0.68f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(28.dp))
            .background(t.background)
            .border(
                1.dp,
                if (t.isDarkSurface) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                RoundedCornerShape(28.dp),
            ),
    ) {
        // Top hairline accent — the template's signature
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(t.accent),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- HEADER (brand) ----
            if (compact) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp),
                ) {
                    HeaderContent(card, header, t, compact = true)
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 34.dp),
                ) {
                    HeaderContent(card, header, t, compact = false)
                }
            }

            Spacer(Modifier.weight(1f))

            if (!compact) {
                // ---- IDENTITY ----
                Text(
                    text = card.name.ifBlank { "Your name" },
                    color = t.ink,
                    fontFamily = if (t.serifName) FontFamily.Serif else FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val role = listOf(card.jobTitle, card.company).filter { it.isNotBlank() }
                if (role.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = role.joinToString("  ·  "),
                        color = t.subInk,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.42f)
                        .height(1.dp)
                        .background(t.accent.copy(alpha = 0.5f)),
                )
                Spacer(Modifier.height(16.dp))

                // ---- CONTACT ----
                Column(
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 26.dp),
                ) {
                    ContactRow(Icons.Filled.Phone, card.phone, t)
                    ContactRow(Icons.Filled.Email, card.email, t)
                    ContactRow(Icons.Filled.Language, card.website, t)
                    ContactRow(Icons.Filled.LocationOn, card.address, t, maxChars = 34)
                    if (card.socials.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            card.socials.take(5).forEach { social ->
                                SocialIconCircle(platform = social.platform, size = 32.dp)
                            }
                        }
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.padding(top = 12.dp, bottom = 20.dp),
                ) {
                    Text(
                        text = card.name.ifBlank { "Your name" },
                        color = t.ink,
                        fontFamily = if (t.serifName) FontFamily.Serif else FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ContactRow(Icons.Filled.Phone, card.phone, t, small = true)
                    ContactRow(Icons.Filled.Email, card.email, t, small = true)
                }
            }
        }

        // Brand mark
        Text(
            text = "TAPCARD",
            color = t.accent,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.6.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
        )
    }
}

@Composable
private fun HeaderContent(
    card: BusinessCard,
    header: HeaderVisual,
    t: CardTemplate,
    compact: Boolean,
) {
    when (header) {
        is HeaderVisual.Image -> {
            val shape = if (header.isLogo) RoundedCornerShape(18.dp) else CircleShape
            AsyncImage(
                model = header.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(if (compact) 46.dp else if (header.isLogo) 74.dp else 88.dp)
                    .clip(shape)
                    .border(1.5.dp, t.accent.copy(alpha = 0.55f), shape),
            )
            if (!compact && header.isLogo && card.company.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    card.company,
                    color = t.subInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1,
                )
            }
        }
        HeaderVisual.Initials -> {
            if (compact) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(t.accent.copy(alpha = 0.16f))
                        .border(1.dp, t.accent.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        card.initials.ifBlank { "·" },
                        color = t.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        card.name.ifBlank { "Your name" },
                        color = t.ink,
                        fontFamily = if (t.serifName) FontFamily.Serif else FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                    )
                    val role = listOf(card.jobTitle, card.company).filter { it.isNotBlank() }
                    if (role.isNotEmpty()) {
                        Text(role.joinToString(" · "), color = t.subInk, fontSize = 11.5.sp, maxLines = 1)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(t.accent.copy(alpha = 0.14f))
                        .border(1.5.dp, t.accent.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        card.initials.ifBlank { "·" },
                        color = t.accent,
                        fontFamily = if (t.serifName) FontFamily.Serif else FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactRow(
    icon: ImageVector,
    value: String?,
    t: CardTemplate,
    small: Boolean = false,
    maxChars: Int = 30,
) {
    if (value.isNullOrBlank()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(if (small) 22.dp else 26.dp)
                .clip(CircleShape)
                .background(t.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = t.accent,
                modifier = Modifier.size(if (small) 13.dp else 15.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (value.length > maxChars) value.take(maxChars - 1) + "…" else value,
            color = t.ink,
            fontSize = if (small) 11.5.sp else 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// LANDSCAPE (credit-card ratio 1.586)
// ---------------------------------------------------------------------------

@Composable
private fun LandscapeCard(card: BusinessCard, modifier: Modifier = Modifier) {
    val t = cardTemplate(card.templateIndex)
    val header = headerResolution(card)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(24.dp))
            .background(t.background)
            .border(
                1.dp,
                if (t.isDarkSurface) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                RoundedCornerShape(24.dp),
            ),
    ) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 0.dp)
                .width(64.dp)
                .height(4.dp)
                .background(t.accent),
        )
        Row(
            Modifier
                .fillMaxSize()
                .padding(22.dp),
        ) {
            // Left column — brand
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (header) {
                    is HeaderVisual.Image -> {
                        val shape = CircleShape
                        AsyncImage(
                            model = header.url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(if (header.isLogo) 62.dp else 72.dp)
                                .clip(shape)
                                .border(1.5.dp, t.accent.copy(alpha = 0.55f), shape),
                        )
                    }
                    HeaderVisual.Initials -> Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(t.accent.copy(alpha = 0.14f))
                            .border(1.5.dp, t.accent.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            card.initials.ifBlank { "·" },
                            color = t.accent,
                            fontFamily = if (t.serifName) FontFamily.Serif else FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
            Box(
                Modifier
                    .width(1.dp)
                    .height(96.dp)
                    .align(Alignment.CenterVertically)
                    .background(t.accent.copy(alpha = 0.4f)),
            )
            Spacer(Modifier.width(20.dp))
            // Right column — identity + contacts
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    card.name.ifBlank { "Your name" },
                    color = t.ink,
                    fontFamily = if (t.serifName) FontFamily.Serif else FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val role = listOf(card.jobTitle, card.company).filter { it.isNotBlank() }
                if (role.isNotEmpty()) {
                    Text(
                        role.joinToString("  ·  "),
                        color = t.subInk,
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.height(10.dp))
                ContactRow(Icons.Filled.Phone, card.phone, t, small = true, maxChars = 22)
                ContactRow(Icons.Filled.Email, card.email, t, small = true, maxChars = 22)
                ContactRow(Icons.Filled.Language, card.website, t, small = true, maxChars = 22)
                if (card.socials.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        card.socials.take(4).forEach { social ->
                            SocialIconCircle(platform = social.platform, size = 26.dp)
                        }
                    }
                }
            }
        }
        Text(
            text = "TAPCARD",
            color = t.accent,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.4.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp),
        )
    }
}
