package com.tapcard.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tapcard.app.ui.theme.Lime

/**
 * Setup checklist (Tapni "Checklist" parity): guides a new user through the
 * key setup steps. Each item derives from real card/account state — no fake
 * progress. Hidden automatically once everything is done.
 */
data class ChecklistItem(val label: String, val done: Boolean)

@Composable
fun rememberChecklist(
    hasCard: Boolean,
    hasPhoto: Boolean,
    hasLogo: Boolean,
    hasSocials: Boolean,
    hasAbout: Boolean,
    nfcWritten: Boolean,
    sharedOnce: Boolean,
): List<ChecklistItem> = listOf(
    ChecklistItem("Create your card", hasCard),
    ChecklistItem("Add your profile photo or logo", hasPhoto || hasLogo),
    ChecklistItem("Add social or bio links", hasSocials),
    ChecklistItem("Write a short about me", hasAbout),
    ChecklistItem("Share your profile once", sharedOnce),
    ChecklistItem("Write it to an NFC card", nfcWritten),
)

@Composable
fun SetupChecklistCard(
    items: List<ChecklistItem>,
    modifier: Modifier = Modifier,
) {
    val done = items.count { it.done }
    if (done == items.size) return // fully set up → hide

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SETUP CHECKLIST",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$done/${items.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(done.toFloat() / items.size.coerceAtLeast(1))
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Lime),
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(
                                    if (item.done) Lime
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.done) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(
                            item.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
