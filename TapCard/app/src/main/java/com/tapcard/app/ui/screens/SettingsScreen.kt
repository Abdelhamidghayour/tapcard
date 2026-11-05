package com.tapcard.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.unit.dp
import com.tapcard.app.LocalNfcWriter
import com.tapcard.app.data.model.BusinessCard
import com.tapcard.app.ui.components.StatusChip
import com.tapcard.app.ui.theme.Success

@Composable
fun SettingsScreen() {
    val writer = LocalNfcWriter.current
    val nfcWrites by CardRepositoryNfcWrites()
    val themeMode by com.tapcard.app.data.ThemeController.mode.collectAsState()
    val authState by com.tapcard.app.data.auth.AuthManager.authState.collectAsState()
    val syncState by com.tapcard.app.data.CardRepository.syncState.collectAsState()
    val migrationState by com.tapcard.app.data.CardRepository.migrationState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---- Appearance ----
            Text(
                "APPEARANCE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        com.tapcard.app.data.ThemeMode.LIGHT to "Light",
                        com.tapcard.app.data.ThemeMode.DARK to "Dark",
                        com.tapcard.app.data.ThemeMode.SYSTEM to "System",
                    ).forEach { (modeOption, label) ->
                        val selected = themeMode == modeOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { com.tapcard.app.data.ThemeController.setMode(modeOption) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ---- Account (cloud mode only) ----
            if (com.tapcard.app.data.cloud.CloudMode.isConfigured) {
                val loggedIn = authState as? com.tapcard.app.data.auth.AuthState.LoggedIn
                SettingsRow(
                    title = "Account",
                    value = loggedIn?.email ?: "Signed out",
                    icon = Icons.Filled.Person,
                    chip = if (loggedIn != null) {
                        { StatusChip("Signed in", Success.copy(alpha = 0.15f), Success) }
                    } else null,
                )
                if (loggedIn != null) {
                    SettingsRow(
                        title = "Cloud sync",
                        value = describeSync(syncState, migrationState),
                        icon = Icons.Filled.Info,
                    )
                    androidx.compose.material3.OutlinedButton(
                        onClick = { com.tapcard.app.data.auth.AuthManager.logout() },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sign out")
                    }
                }
            }
            SettingsRow(
                title = "NFC hardware",
                value = if (!writer.supportsNfc) "Not available on this device"
                    else if (writer.isNfcEnabled) "Enabled" else "Turned off",
                icon = Icons.Filled.Nfc,
                chip = {
                    if (writer.supportsNfc) {
                        if (writer.isNfcEnabled) {
                            StatusChip("Ready", Success.copy(alpha = 0.15f), Success)
                        } else {
                            StatusChip("Off", MaterialTheme.colorScheme.error.copy(alpha = 0.12f), MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
            SettingsRow(
                title = "Successful NFC writes",
                value = nfcWrites.toString(),
                icon = Icons.Filled.Info,
            )
            SettingsRow(
                title = "Public link base",
                value = BusinessCard.PUBLIC_URL_BASE,
                icon = Icons.Filled.Info,
            )
            SettingsRow(
                title = "Version",
                value = "1.0.0 (milestone 1)",
                icon = Icons.Filled.Info,
            )
            Text(
                text = "TapCard writes your public profile link to NFC tags with a standard NDEF URI record, readable by any modern phone — no app required on the receiving side.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun CardRepositoryNfcWrites() = com.tapcard.app.data.CardRepository.nfcWrites.collectAsState()

private fun describeSync(
    sync: com.tapcard.app.data.SyncState,
    migration: com.tapcard.app.data.MigrationState,
): String {
    val migrationText = when (migration) {
        is com.tapcard.app.data.MigrationState.Done -> " · imported ${migration.imported}"
        com.tapcard.app.data.MigrationState.CloudAlreadyHadCards -> " · cloud cards loaded"
        is com.tapcard.app.data.MigrationState.InProgress -> " · importing ${migration.count}…"
        is com.tapcard.app.data.MigrationState.Failed -> " · migration failed"
        else -> ""
    }
    return when (sync) {
        com.tapcard.app.data.SyncState.Synced -> "Synced$migrationText"
        com.tapcard.app.data.SyncState.Syncing -> "Syncing…$migrationText"
        is com.tapcard.app.data.SyncState.Error -> "Error: ${sync.message}"
        com.tapcard.app.data.SyncState.Idle -> "Waiting…"
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    chip: @Composable (() -> Unit)? = null,
) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            chip?.invoke()
        }
    }
}
