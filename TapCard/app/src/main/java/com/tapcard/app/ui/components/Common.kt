package com.tapcard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import com.tapcard.app.ui.theme.BrandGradient
import com.tapcard.app.ui.theme.TextBright
import com.tapcard.app.ui.theme.accentBrush
import com.tapcard.app.ui.theme.plasmaGlow
import com.tapcard.app.ui.theme.platformColor

/** Circular avatar with the person's initials over their card accent gradient. */
@Composable
fun InitialsAvatar(name: String, accentIndex: Int, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accentBrush(accentIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initialsOf(name),
            color = Color.White,
            fontSize = (size.value * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

private fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")

/** Primary CTA — plasma gradient pill with a soft violet aura (2026 look). */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        // Aura behind the pill
        Box(
            Modifier
                .matchParentSize()
                .padding(6.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(plasmaGlow(if (enabled) 0.55f else 0.12f)),
        )
        Button(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(22.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = if (enabled) TextBright else Color(0xFF8888A0),
                disabledContainerColor = Color.Transparent,
            ),
            modifier = Modifier
                .height(56.dp)
                .background(
                    brush = if (enabled) BrandGradient else Brush.linearGradient(
                        listOf(Color(0xFF2A2833), Color(0xFF2A2833)),
                    ),
                    shape = RoundedCornerShape(22.dp),
                ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}

/** Colorful social icon circle with a recognizable platform glyph. */
@Composable
fun SocialIconCircle(platform: String, size: Dp = 44.dp, onClick: (() -> Unit)? = null) {
    val color = platformColor(platform)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        PlatformGlyph(platform, glyphSize = (size.value * 0.5f).sp)
    }
}

/** Recognizable glyph per platform: material icon or styled letter. */
@Composable
fun PlatformGlyph(platform: String, glyphSize: androidx.compose.ui.unit.TextUnit) {
    val key = platform.trim().lowercase()
    when (key) {
        "phone", "tel" -> Icon(
            Icons.Filled.Phone, null,
            tint = Color.White, modifier = Modifier.size(glyphSize.value.dp * 1.1f),
        )
        "whatsapp" -> Icon(
            Icons.Filled.ChatBubble, null,
            tint = Color.White, modifier = Modifier.size(glyphSize.value.dp * 1.1f),
        )
        "instagram" -> Icon(
            Icons.Filled.PhotoCamera, null,
            tint = Color.White, modifier = Modifier.size(glyphSize.value.dp * 1.1f),
        )
        "youtube" -> Icon(
            Icons.Filled.PlayArrow, null,
            tint = Color.White, modifier = Modifier.size(glyphSize.value.dp * 1.3f),
        )
        "website", "web" -> Icon(
            Icons.Filled.Language, null,
            tint = Color.White, modifier = Modifier.size(glyphSize.value.dp * 1.1f),
        )
        else -> Text(
            text = when (key) {
                "linkedin" -> "in"
                "facebook" -> "f"
                "x" -> "𝕏"
                "tiktok" -> "♪"
                "github" -> "G"
                else -> platform.take(1).uppercase()
            },
            color = Color.White,
            fontSize = glyphSize,
            fontWeight = FontWeight.Black,
            fontFamily = if (key == "linkedin") FontFamily.SansSerif else FontFamily.Default,
        )
    }
}

/** Small rounded status badge (e.g. "Active", "NFC off"). */
@Composable
fun StatusChip(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
        )
    }
}

/** Section header with optional trailing action text. */
@Composable
fun SectionTitle(title: String, trailing: (@Composable () -> Unit)? = null) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}
