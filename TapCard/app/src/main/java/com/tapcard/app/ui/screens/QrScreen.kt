package com.tapcard.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tapcard.app.data.CardRepository
import com.tapcard.app.data.qr.QrCodeGenerator
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.components.InitialsAvatar
import com.tapcard.app.ui.theme.cardTemplate

/**
 * QR sharing screen: renders the QR for the card's public URL (generated with
 * ZXing — never a placeholder image) and offers share / save / copy actions.
 */
@Composable
fun QrScreen(
    cardId: String?,
    onBack: () -> Unit,
) {
    val card = CardRepository.cardById(cardId)
    if (card == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Card not found", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var qrBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Generate off the first composition; 720px is fast (<50 ms) but still
    // kept off the main thread for smoothness.
    LaunchedEffect(card.id) {
        qrBitmap = QrCodeGenerator.generate(card.publicUrl, sizePx = 720)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("QR Code", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ---- Brand card frame around the QR ----
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        com.tapcard.app.ui.theme.cardTemplate(card.templateIndex).background,
                        RoundedCornerShape(28.dp),
                    )
                    .padding(10.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialsAvatar(name = card.name, accentIndex = card.accentIndex, size = 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(card.name, style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A))
                            Text(
                                listOf(card.jobTitle, card.company).filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    val bmp = qrBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "QR code for ${card.publicUrl}",
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.86f)
                                .height(240.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("Generating…", color = Color(0xFF64748B))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Scan to open my digital card",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF64748B),
                    )
                }
            }

            Text(
                text = card.publicUrl,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            // ---- Actions ----
            GradientButton(
                text = "Share QR code",
                onClick = {
                    val bmp = qrBitmap ?: return@GradientButton
                    QrCodeGenerator.share(context, bmp, card.slug, card.publicUrl)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            )
            OutlinedButton(
                onClick = {
                    val bmp = qrBitmap ?: return@OutlinedButton
                    val saved = QrCodeGenerator.saveToGallery(context, bmp, card.slug)
                    Toast.makeText(
                        context,
                        if (saved) "Saved to DCIM/TapCard" else "Saving needs Android 10+ — use Share instead",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Save to photos")
            }
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(card.publicUrl))
                    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Copy link")
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
