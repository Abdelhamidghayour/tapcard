package com.tapcard.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tapcard.app.data.CardRepository
import com.tapcard.app.ui.components.CardSurface
import com.tapcard.app.ui.theme.BrandGradientColors
import java.util.Calendar
import kotlin.math.abs

/*
 * Dashboard v4 — the business card IS the hero.
 *
 *  Welcome header
 *  MY DIGITAL CARD
 *  ┌──────────────┐   ← premium pager carousel, active card large + centered,
 *  │   (card)     │      neighbours scaled down at the edges
 *  └──────────────┘
 *  [ Edit Card ]  [ Share Profile ]
 *  QR · NFC · New quick tiles
 *  stats
 */

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
    val haptics = LocalHapticFeedback.current
    val context = androidx.compose.ui.platform.LocalContext.current

    // Keep the home-screen QR widget in sync with the active card
    androidx.compose.runtime.DisposableEffect(context) {
        com.tapcard.app.widget.QrWidgetProvider.refreshAll(context)
        onDispose { }
    }

    val initialIndex = cards.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { cards.size },
    )
    val centerIndex by remember(pagerState) {
        derivedStateOf {
            if (cards.isEmpty()) 0
            else pagerState.currentPage.coerceIn(0, cards.lastIndex)
        }
    }
    // Following the carousel selection updates the repository's active card.
    LaunchedEffect(centerIndex, cards.size) {
        if (cards.isNotEmpty()) {
            val id = cards.getOrNull(centerIndex)?.id
            if (id != null && id != activeId) {
                runCatching { CardRepository.setActive(id) }
            }
        }
    }
    val activeCard = cards.getOrNull(centerIndex)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ---- Welcome header (minimal) ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "MY DIGITAL CARD",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = greeting(),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            if (activeCard != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        activeCard.initials.ifBlank { "·" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ---- Card carousel ----
        if (cards.isEmpty()) {
            EmptyCard(onCreateCard)
        } else {
            HorizontalPager(
                state = pagerState,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 44.dp),
                pageSpacing = 14.dp,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                val card = cards[page]
                val dist = abs(pagerState.currentPage - page)
                val scale = when (dist) {
                    0 -> 1f
                    1 -> 0.88f
                    else -> 0.8f
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = if (dist == 0) 1f else 0.75f
                        }
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenCard(card.id)
                        },
                ) {
                    CardSurface(card = card, modifier = Modifier.fillMaxWidth())
                    if (dist == 0 && cards.size > 1) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            repeat(cards.size) { dot ->
                                Box(
                                    modifier = Modifier
                                        .size(if (dot == page) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (dot == page) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                        ),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- Primary actions ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ActionPill(
                    label = "Edit Card",
                    icon = Icons.Filled.Edit,
                    filled = false,
                    modifier = Modifier.weight(1f),
                ) {
                    activeCard?.let { card ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenCard(card.id)
                    }
                }
                ActionPill(
                    label = "Share Profile",
                    icon = null,
                    filled = true,
                    modifier = Modifier.weight(1f),
                ) {
                    activeCard?.let {
                        com.tapcard.app.ui.components.shareVCard(context, it)
                        CardRepository.recordShare()
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- Quick actions ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QuickTile(Icons.Filled.QrCode2, "QR", Modifier.weight(1f)) {
                    activeCard?.let { onShowQr(it.id) }
                }
                QuickTile(Icons.Filled.Nfc, "NFC", Modifier.weight(1f)) {
                    activeCard?.let { onWriteNfc(it.id) }
                }
                QuickTile(Icons.Filled.Edit, "New", Modifier.weight(1f), onClick = onCreateCard)
            }

            Spacer(Modifier.height(16.dp))

            // ---- Setup checklist (Tapni parity) ----
            val checklist = com.tapcard.app.ui.components.rememberChecklist(
                hasCard = cards.isNotEmpty(),
                hasPhoto = activeCard?.profileImageUrl != null,
                hasLogo = activeCard?.logoUrl != null,
                hasSocials = (activeCard?.socials?.isNotEmpty() == true) ||
                    (activeCard?.links?.isNotEmpty() == true),
                hasAbout = !activeCard?.about.isNullOrBlank(),
                nfcWritten = nfcWrites > 0,
                sharedOnce = shares > 0,
            )
            com.tapcard.app.ui.components.SetupChecklistCard(items = checklist)

            Spacer(Modifier.height(16.dp))

            // ---- Stats ----
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(cards.size.toString(), "Cards")
                StatPill(nfcWrites.toString(), "NFC")
                StatPill(shares.toString(), "Shares")
            }
        }

        Spacer(Modifier.height(112.dp)) // floating-nav clearance
    }
}

@Composable
private fun EmptyCard(onCreateCard: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(28.dp),
        ) {
            Text("No card yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Create your digital identity — it takes less than a minute.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(
                if (filled) Brush.linearGradient(BrandGradientColors)
                else Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (filled) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (filled) Color.White else MaterialTheme.colorScheme.onSurface,
            )
        }
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
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
