package com.tapcard.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tapcard.app.data.ocr.OcrFieldParser
import com.tapcard.app.data.ocr.OcrPrefillStore
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.theme.PlasmaGlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Paper business card scanning (same ML Kit engine class HiHello uses).
 * Pick a photo of a card → on-device OCR → parse fields → hand off to the
 * editor as a prefill. Nothing leaves the device; user reviews everything.
 */
@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onParsed: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        error = null
        status = "Reading the card…"
        scope.launch {
            try {
                val image = InputImage.fromFilePath(context, uri)
                val result = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).await()
                val parsed = OcrFieldParser.parse(result.text)
                if (parsed.name.isNullOrBlank() && parsed.email.isNullOrBlank() && parsed.phone.isNullOrBlank()) {
                    status = null
                    error = "Couldn't find readable details. Try a sharper, well-lit photo of the card."
                } else {
                    OcrPrefillStore.offer(parsed)
                    status = null
                    onParsed()
                }
            } catch (e: Exception) {
                status = null
                error = e.message ?: "Scanning failed"
            } finally {
                busy = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Scan a paper card", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .size(140.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.DocumentScanner,
                contentDescription = null,
                tint = PlasmaGlow,
                modifier = Modifier.size(96.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Photograph a paper business card",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Everything is read on this device. The scanned details open in the editor so you can review and fix anything before saving.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (busy) {
            CircularProgressIndicator()
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        GradientButton(
            text = "Choose card photo",
            enabled = !busy,
            onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .height(54.dp),
        )
        Spacer(Modifier.height(40.dp))
    }
}
