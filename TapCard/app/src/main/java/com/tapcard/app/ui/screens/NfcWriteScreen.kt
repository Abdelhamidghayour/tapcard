package com.tapcard.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import com.tapcard.app.LocalNfcWriter
import com.tapcard.app.data.CardRepository
import com.tapcard.app.nfc.NdefPayloadBuilder
import com.tapcard.app.nfc.NfcWriteOutcome
import com.tapcard.app.nfc.NfcWriteState
import com.tapcard.app.ui.components.GradientButton
import com.tapcard.app.ui.theme.ErrorRed
import com.tapcard.app.ui.theme.Success

/**
 * NFC writing screen. Pure presentation over NfcTagWriter's state machine;
 * all hardware I/O lives in the writer (never faked in the UI).
 */
@Composable
fun NfcWriteScreen(
    cardId: String?,
    onBack: () -> Unit,
) {
    val card = CardRepository.cardById(cardId)
    val writer = LocalNfcWriter.current
    val state by writer.state.collectAsState()
    val activity = LocalContext.current as ComponentActivity

    // Arm/disarm foreground dispatch with the screen's visibility.
    DisposableEffect(cardId) {
        writer.attach(activity)
        onDispose { writer.detach(activity) }
    }

    // Queue the payload for the active card as soon as the screen appears.
    LaunchedEffect(cardId) {
        if (card != null) {
            writer.startWrite(NdefPayloadBuilder.cardMessage(card))
        }
    }

    // Count verified writes exactly once per successful state.
    LaunchedEffect(state) {
        if (state is NfcWriteState.Success) {
            CardRepository.recordNfcWriteSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Write NFC Card", style = MaterialTheme.typography.titleLarge)
        }

        if (card == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Card not found", style = MaterialTheme.typography.bodyLarge)
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = card.name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = card.publicUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))

            when (val s = state) {
                is NfcWriteState.NfcUnavailable -> NfcProblemState(
                    title = "NFC is not available",
                    body = "This device has no NFC hardware, so cards cannot be written here. " +
                        "Everything else in the app works — try it on an NFC-enabled phone.",
                )
                is NfcWriteState.NfcDisabled -> {
                    NfcProblemState(
                        title = "NFC is turned off",
                        body = "Turn on NFC to program your card, then come back and hold the card against the phone.",
                    )
                    OutlinedButton(
                        onClick = {
                            activity.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Open NFC settings")
                    }
                }
                is NfcWriteState.WaitingForTag -> PulsingTarget()
                is NfcWriteState.Writing -> {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Writing…", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is NfcWriteState.Success -> {
                    ResultCircle(color = com.tapcard.app.ui.theme.PlasmaGlow, icon = { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(48.dp)) })
                    Text("Card written successfully", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        text = "${s.bytes} bytes · ${s.tagDescription}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = { writer.startWrite(NdefPayloadBuilder.cardMessage(card)) },
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Write another card")
                    }
                }
                is NfcWriteState.Failure -> {
                    ResultCircle(color = ErrorRed, icon = { Icon(Icons.Filled.Close, null, tint = Color.White, modifier = Modifier.size(48.dp)) })
                    Text("Write failed", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        text = describeFailure(s.outcome),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    if (s.retryable) {
                        GradientButton(
                            text = "Try again",
                            onClick = { writer.startWrite(NdefPayloadBuilder.cardMessage(card)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is NfcWriteState.Idle -> Unit
            }

            Spacer(Modifier.weight(1f))
            Text(
                text = "Hold the NFC card flat against the back of the phone until writing finishes. Keep the card still — moving it too early is the most common cause of failed writes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun PulsingTarget() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Box(
        modifier = Modifier
            .size(200.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.scale(scale)) {
            Text("NFC", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                "Hold your card near the back of the phone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun ResultCircle(color: Color, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}

@Composable
private fun NfcProblemState(title: String, body: String) {
    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text("NFC", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private fun describeFailure(outcome: NfcWriteOutcome): String = when (outcome) {
    is NfcWriteOutcome.TagNotWritable ->
        "This tag is read-only (locked). Use a writable NFC card such as NTAG213/215/216."
    is NfcWriteOutcome.CapacityTooSmall ->
        "Tag too small: it can store ${outcome.available} bytes but the card needs ${outcome.needed}. Use a larger tag."
    is NfcWriteOutcome.TagNotNdefCompatible ->
        "This tag does not support NDEF data. Use an NDEF-formatable card (e.g. NTAG or MIFARE Ultralight)."
    is NfcWriteOutcome.TagLost ->
        "The tag was moved away before writing finished. Keep the card still and try again."
    is NfcWriteOutcome.IoFailure ->
        "Communication error: ${outcome.detail}. Hold the card steady and try again."
    is NfcWriteOutcome.Success -> "Written"
}
