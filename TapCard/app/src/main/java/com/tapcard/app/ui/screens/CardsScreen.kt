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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tapcard.app.data.CardRepository
import com.tapcard.app.ui.components.EmptyState
import com.tapcard.app.ui.components.InitialsAvatar
import com.tapcard.app.ui.components.StatusChip
import com.tapcard.app.ui.theme.Success

@Composable
fun CardsScreen(
    onCreateCard: () -> Unit,
    onOpenCard: (String) -> Unit,
    onWriteNfc: (String) -> Unit,
) {
    val cards by CardRepository.cards.collectAsState()
    val activeId by CardRepository.activeCardId.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateCard) {
                Icon(Icons.Filled.Add, contentDescription = "Create card")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding),
        ) {
            Text(
                text = "My Business Cards",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            )
            if (cards.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.CreditCard,
                    title = "No business cards",
                    subtitle = "Create your first digital card to start sharing and writing NFC tags.",
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 24.dp, end = 24.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardTile(
                            name = card.name,
                            role = listOf(card.jobTitle, card.company)
                                .filter { it.isNotBlank() }
                                .joinToString(" · "),
                            url = card.publicUrl,
                            accentIndex = card.accentIndex,
                            isActive = card.id == activeId,
                            onClick = { onOpenCard(card.id) },
                            onWriteNfc = { onWriteNfc(card.id) },
                            onSetActive = { CardRepository.setActive(card.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTile(
    name: String,
    role: String,
    url: String,
    accentIndex: Int,
    isActive: Boolean,
    onClick: () -> Unit,
    onWriteNfc: () -> Unit,
    onSetActive: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(name = name, accentIndex = accentIndex, size = 48.dp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            if (isActive) {
                StatusChip(text = "Active", container = Success.copy(alpha = 0.15f), content = Success)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onWriteNfc) {
                Icon(Icons.Filled.Nfc, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Write NFC")
            }
            if (!isActive) {
                TextButton(onClick = onSetActive) {
                    Text("Set active")
                }
            }
        }
    }
}
