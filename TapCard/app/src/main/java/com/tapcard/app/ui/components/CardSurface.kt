package com.tapcard.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.ui.theme.cardTemplate

/**
 * The premium digital business card surface.
 * Template-driven visual identity shared by Dashboard, Preview, Create (live
 * preview) and the QR screen — changes to the card render immediately.
 */
@Composable
fun CardSurface(
    card: BusinessCard,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val template = cardTemplate(card.templateIndex)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(template.background),
    ) {
        // Plasma aura on dark templates — ties the card to the app identity
        if (template.isDarkSurface) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                com.tapcard.app.ui.theme.PlasmaGlow.copy(alpha = 0.22f),
                                Color.Transparent,
                            ),
                            center = androidx.compose.ui.geometry.Offset(
                                Float.POSITIVE_INFINITY,
                                0f,
                            ),
                            radius = 620f,
                        ),
                    ),
            )
        }
        // Signature hairline accent along the top edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(template.accent),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 18.dp else 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (card.profileImageUrl != null) {
                    AsyncImage(
                        model = card.profileImageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(if (compact) 46.dp else 62.dp)
                            .clip(CircleShape)
                            .background(template.accent.copy(alpha = 0.25f)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 46.dp else 62.dp)
                            .clip(CircleShape)
                            .background(template.accent.copy(alpha = 0.18f))
                            .border(1.dp, template.accent.copy(alpha = 0.55f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = card.initials.ifBlank { "·" },
                            color = template.accent,
                            fontFamily = if (template.serifName) FontFamily.Serif else FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (compact) 16.sp else 21.sp,
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = card.name.ifBlank { "Your name" },
                        color = template.ink,
                        fontFamily = if (template.serifName) FontFamily.Serif else FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 17.sp else 21.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val role = listOf(card.jobTitle, card.company)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (role.isNotBlank()) {
                        Text(
                            text = role,
                            color = template.subInk,
                            fontSize = if (compact) 12.sp else 13.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Company logo (when provided)
                if (card.logoUrl != null) {
                    Spacer(Modifier.width(10.dp))
                    AsyncImage(
                        model = card.logoUrl,
                        contentDescription = "Company logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(if (compact) 26.dp else 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (template.isDarkSurface) Color.White.copy(alpha = 0.92f) else Color.Transparent)
                            .padding(3.dp),
                    )
                }
            }

            if (!compact) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(template.accent.copy(alpha = 0.30f)),
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (!card.email.isNullOrBlank()) {
                            ContactLine("EMAIL", card.email, template.subInk, template.ink)
                        }
                        if (!card.phone.isNullOrBlank()) {
                            ContactLine("PHONE", card.phone, template.subInk, template.ink)
                        }
                        if (!card.website.isNullOrBlank()) {
                            ContactLine("WEB", card.website, template.subInk, template.ink)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "TAPCARD",
                        color = template.accent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                        fontSize = 10.sp,
                    )
                }
                // Colorful social circles row (WhatsApp/LinkedIn/IG/FB…)
                if (card.socials.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        card.socials.take(6).forEach { social ->
                            SocialIconCircle(platform = social.platform, size = 28.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactLine(label: String, value: String, labelColor: Color, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(58.dp),
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
