package com.tapcard.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.tapcard.app.nfc.NfcTagWriter
import com.tapcard.app.ui.AppRoot
import com.tapcard.app.ui.theme.TapCardTheme

/** Writer instance shared with NFC screens via composition. */
val LocalNfcWriter = staticCompositionLocalOf<NfcTagWriter> {
    error("NfcTagWriter not provided")
}

class MainActivity : ComponentActivity() {

    val nfcWriter: NfcTagWriter by lazy { NfcTagWriter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TapCardTheme {
                CompositionLocalProvider(LocalNfcWriter provides nfcWriter) {
                    AppRoot()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        nfcWriter.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Covers returning from NFC settings with a pending write queued.
        nfcWriter.refreshCapability()
    }
}
