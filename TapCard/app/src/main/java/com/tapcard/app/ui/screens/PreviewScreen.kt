package com.tapcard.app.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.tapcard.app.data.CardRepository
import com.tapcard.app.data.vcard.VCardBuilder
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.components.InitialsAvatar
import com.tapcard.app.ui.theme.accentBrush

@Composable
fun PreviewScreen(
    cardId: String?,
    onWriteNfc: (String) -> Unit,
    onShowQr: (String) -> Unit,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
) {
    val card = CardRepository.cardById(cardId)
    if (card == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Card not found", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showVCard by rememberSaveable { mutableStateOf(false) }
    var showSignature by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // ---- Hero ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentBrush(card.accentIndex))
                .statusBarsPadding()
                .padding(bottom = 28.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { onEdit(card.id) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit card", tint = Color.White)
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    // Profile image (cloud upload) or initials fallback
                    val uploadState by com.tapcard.app.data.CardRepository.uploadState.collectAsState()
                    val photoPicker = rememberLauncherForActivityResult(
                        ActivityResultContracts.PickVisualMedia(),
                    ) { uri ->
                        if (uri != null) {
                            CardRepository.uploadCardImage(card.id, uri, "profile")
                        }
                    }
                    Box(contentAlignment = Alignment.BottomEnd) {
                        if (card.profileImageUrl != null) {
                            Box(contentAlignment = Alignment.Center) {
                                coil.compose.AsyncImage(
                                    model = card.profileImageUrl,
                                    contentDescription = "Profile photo of ${card.name}",
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape),
                                )
                            }
                        } else {
                            InitialsAvatar(name = card.name, accentIndex = card.accentIndex, size = 84.dp)
                        }
                        if (com.tapcard.app.data.cloud.CloudMode.isConfigured) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                IconButton(
                                    onClick = {
                                        photoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    modifier = Modifier.size(26.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Change profile photo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp),
                                    )
                                }
                            }
                        }
                    }
                    when (val up = uploadState) {
                        is com.tapcard.app.data.UploadState.Uploading ->
                            Text(
                                "Uploading photo… ${up.progressPercent}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                            )
                        is com.tapcard.app.data.UploadState.Failed ->
                            Text(
                                "Photo upload failed: ${up.message}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFFD5D5),
                            )
                        else -> Unit
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(card.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    if (card.jobTitle.isNotBlank() || card.company.isNotBlank()) {
                        Text(
                            text = listOf(card.jobTitle, card.company).filter { it.isNotBlank() }
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            text = card.publicUrl,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // ---- Actions ----
            GradientButton(
                text = "Write to NFC card",
                onClick = { onWriteNfc(card.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            )
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(card.publicUrl))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Copy public link")
            }
            OutlinedButton(
                onClick = { onShowQr(card.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Show QR code")
            }
            OutlinedButton(
                onClick = {
                    com.tapcard.app.ui.components.shareVCard(context, card)
                    com.tapcard.app.data.CardRepository.recordShare()
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Share contact (vCard)")
            }
            OutlinedButton(
                onClick = { com.tapcard.app.ui.components.insertIntoContacts(context, card) },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Save to phone contacts")
            }
            OutlinedButton(
                onClick = { showSignature = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(Icons.Filled.Email, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Email signature")
            }

            Spacer(Modifier.height(8.dp))

            // ---- Contact details ----
            Text("Contact", style = MaterialTheme.typography.titleMedium)
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    ContactRow(Icons.Filled.Phone, "Phone", card.phone)
                    ContactRow(Icons.Filled.Email, "Email", card.email)
                    ContactRow(Icons.Filled.Language, "Website", card.website)
                    card.socials.forEach { social ->
                        ContactRow(Icons.Filled.Share, social.platform, social.url)
                    }
                }
            }

            // ---- vCard data preview (real generated payload) ----
            Text("vCard data", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(
                onClick = { showVCard = !showVCard },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(if (showVCard) "Hide vCard preview" else "Show vCard preview")
            }
            if (showVCard) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
                    Text(
                        text = VCardBuilder.build(card),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(48.dp))
        }
    }

    if (showSignature) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSignature = false },
            title = { Text("Email signature") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        "Copy this HTML into your email client's signature settings:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            com.tapcard.app.data.vcard.EmailSignature.buildHtml(card),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp),
                        )
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    clipboard.setText(AnnotatedString(com.tapcard.app.data.vcard.EmailSignature.buildHtml(card)))
                    Toast.makeText(context, "Signature HTML copied", Toast.LENGTH_SHORT).show()
                    showSignature = false
                }) { Text("Copy HTML") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showSignature = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun ContactRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.size(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
