package com.tapcard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PersonAdd

import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tapcard.app.data.CardRepository
import com.tapcard.app.ui.components.CardSurface
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.components.SocialIconCircle
import com.tapcard.app.ui.theme.BrandGradientColors
import com.tapcard.app.ui.theme.Lime
import com.tapcard.app.ui.theme.PlasmaGlow
import com.tapcard.app.ui.theme.accentBrush
import com.tapcard.app.ui.theme.plasmaGlow

@Composable
fun DashboardScreen(
    onCreateCard: () -> Unit,
    onOpenCard: (String) -> Unit,
    onWriteNfc: (String) -> Unit,
    onShowQr: (String) -> Unit,
    onManageCards: () -> Unit,
    onScanCard: () -> Unit,
) {
    val cards by CardRepository.cards.collectAsState()
    val activeId by CardRepository.activeCardId.collectAsState()
    val nfcWrites by CardRepository.nfcWrites.collectAsState()
    val shares by CardRepository.shares.collectAsState()
    val activeCard = cards.firstOrNull { it.id == activeId } ?: cards.firstOrNull()
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(26.dp))

        // ---- Profile hero (ProfileTap style) ----
        Box(contentAlignment = Alignment.BottomEnd) {
            if (activeCard?.profileImageUrl != null) {
                AsyncImage(
                    model = activeCard.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface),
                            ),
                        )
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = (activeCard?.initials ?: "").ifBlank { "·" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            // NFC badge dot
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, PlasmaGlow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = activeCard?.name ?: "Create your identity",
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = listOf(activeCard?.jobTitle, activeCard?.company)
                .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                .ifBlank { "Your digital identity, one tap away" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ---- Colorful social circles ----
        if (!activeCard?.socials.isNullOrEmpty() || !activeCard?.email.isNullOrBlank()) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                activeCard?.socials?.take(5)?.forEach { social ->
                    SocialIconCircle(platform = social.platform, size = 44.dp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- Lime Save Contact + Plasma Share Profile ----
        val context = androidx.compose.ui.platform.LocalContext.current
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Lime)
                    .clickable(enabled = activeCard != null) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        activeCard?.let {
                            com.tapcard.app.ui.components.insertIntoContacts(context, it)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFF12160A),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Save Contact",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF12160A),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(BrandGradientColors))
                    .clickable(enabled = activeCard != null) {
                        activeCard?.let {
                            com.tapcard.app.ui.components.shareVCard(context, it)
                            CardRepository.recordShare()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Share profile",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // ---- The digital business card itself ----
        if (activeCard != null) {
            CardSurface(
                card = activeCard,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .clickable { onOpenCard(activeCard.id) },
            )
        } else {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Text("No card yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Create your digital identity — it takes less than a minute.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- About me card (ProfileTap style) ----
        if (!activeCard?.about.isNullOrBlank()) {
            Spacer(Modifier.height(18.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("About me", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        activeCard!!.about!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ---- Quick actions grid ----
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickTile(Icons.Filled.QrCode2, "QR", Modifier.weight(1f)) {
                activeCard?.let { onShowQr(it.id) }
            }
            QuickTile(Icons.Filled.Nfc, "NFC", Modifier.weight(1f)) {
                activeCard?.let { onWriteNfc(it.id) }
            }
            QuickTile(Icons.Filled.Add, "New", Modifier.weight(1f), onClick = onCreateCard)
            QuickTile(Icons.Filled.DocumentScanner, "Scan", Modifier.weight(1f), onClick = onScanCard)
        }

        Spacer(Modifier.height(16.dp))

        // ---- Real stats ----
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatPill(cards.size.toString(), "Cards")
            StatPill(nfcWrites.toString(), "NFC writes")
            StatPill(shares.toString(), "Shares")
        }

        Spacer(Modifier.height(20.dp))

        // ---- Recent cards ----
        Column(Modifier.fillMaxWidth()) {
            Text(
                "RECENT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(8.dp))
            cards.take(3).forEach { card ->
                RecentRow(card.initials, card.name, card.accentIndex) { onOpenCard(card.id) }
            }
        }

        // ---- Create CTA ----
        GradientButton(
            text = "Create new card",
            onClick = onCreateCard,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(120.dp)) // clearance for the floating nav
    }
}

@Composable
private fun QuickTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatPill(value: String, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecentRow(
    initials: String,
    name: String,
    accent: Int,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accentBrush(accent)),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials.ifBlank { "·" }, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.titleMedium)
    }
}
