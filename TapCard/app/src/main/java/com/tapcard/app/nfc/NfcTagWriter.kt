package com.tapcard.app.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import androidx.core.content.IntentCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Outcome of a real tag write attempt. Never invented: every failure maps to
 * an actual exception or pre-write check result.
 */
sealed interface NfcWriteOutcome {
    data class Success(val bytesWritten: Int, val tagDescription: String) : NfcWriteOutcome
    data object TagNotWritable : NfcWriteOutcome
    data class CapacityTooSmall(val needed: Int, val available: Int) : NfcWriteOutcome
    data object TagNotNdefCompatible : NfcWriteOutcome
    data object TagLost : NfcWriteOutcome
    data class IoFailure(val detail: String) : NfcWriteOutcome
}

/** High-level UI state machine for the NFC writing screen. */
sealed interface NfcWriteState {
    data object Idle : NfcWriteState
    data object NfcUnavailable : NfcWriteState // device has no NFC hardware
    data object NfcDisabled : NfcWriteState // NFC hardware present but off
    data object WaitingForTag : NfcWriteState // dispatch armed, hold card to phone
    data object Writing : NfcWriteState // talking to the tag
    data class Success(val bytes: Int, val tagDescription: String) : NfcWriteState
    data class Failure(val outcome: NfcWriteOutcome, val retryable: Boolean) : NfcWriteState
}

/**
 * Owns the Android NFC plumbing for tag writing. Kept strictly separate from
 * Compose UI: the screen observes [state] and forwards intents only.
 *
 * Mechanism: foreground dispatch with tech filters for Ndef and NdefFormatable.
 * When a tag is detected, the system re-delivers the launch intent to the
 * activity via onNewIntent; we extract EXTRA_TAG and write the queued message.
 */
class NfcTagWriter(context: Context) {

    private val appContext = context.applicationContext
    val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(appContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<NfcWriteState> = _state.asStateFlow()

    @Volatile
    private var pendingMessage: NfcWriteRequest? = null

    private data class NfcWriteRequest(val message: NdefMessage, val bytes: Int)

    val supportsNfc: Boolean get() = adapter != null
    val isNfcEnabled: Boolean get() = adapter?.isEnabled == true

    private fun initialState(): NfcWriteState = when {
        adapter == null -> NfcWriteState.NfcUnavailable
        !adapter.isEnabled -> NfcWriteState.NfcDisabled
        else -> NfcWriteState.Idle
    }

    /** Queue an NDEF message and enter WaitingForTag. */
    fun startWrite(message: NdefMessage) {
        pendingMessage = NfcWriteRequest(message, message.byteArrayLength)
        _state.value = when {
            adapter == null -> NfcWriteState.NfcUnavailable
            !adapter.isEnabled -> NfcWriteState.NfcDisabled
            else -> NfcWriteState.WaitingForTag
        }
    }

    /** Refresh capability-dependent states (e.g. after user enables NFC). */
    fun refreshCapability() {
        val current = _state.value
        when {
            adapter == null -> {
                if (current !is NfcWriteState.NfcUnavailable) _state.value = NfcWriteState.NfcUnavailable
            }
            !adapter.isEnabled -> {
                // Never disturb an in-flight or completed write with a state flip.
                if (current !is NfcWriteState.NfcDisabled &&
                    current !is NfcWriteState.Writing &&
                    current !is NfcWriteState.Success
                ) {
                    _state.value = NfcWriteState.NfcDisabled
                }
            }
            else -> {
                // NFC just came on: resume a pending write session if there is one.
                if (current is NfcWriteState.NfcDisabled && pendingMessage != null) {
                    _state.value = NfcWriteState.WaitingForTag
                }
            }
        }
    }

    fun reset() {
        pendingMessage = null
        _state.value = initialState()
    }

    /**
     * Arm foreground dispatch while the write screen is visible.
     * Tech lists restrict discovery to NDEF-capable or formatable tags.
     */
    fun attach(activity: Activity) {
        val nfc = adapter ?: return
        if (!nfc.isEnabled) return
        val intent = Intent(appContext, activity.javaClass)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val techLists = arrayOf(
            arrayOf(Ndef::class.java.name),
            arrayOf(NdefFormatable::class.java.name),
        )
        try {
            nfc.enableForegroundDispatch(activity, pendingIntent, null, techLists)
        } catch (_: IllegalStateException) {
            // Foreground dispatch can race activity lifecycle; ignore arming errors.
        }
    }

    fun detach(activity: Activity) {
        try {
            adapter?.disableForegroundDispatch(activity)
        } catch (_: Exception) {
            // Activity may already be destroyed; nothing to disarm.
        }
    }

    /** Called from MainActivity.onNewIntent when the system dispatches a tag. */
    fun onNewIntent(intent: Intent?) {
        val request = pendingMessage ?: return
        val tag = intent?.let {
            IntentCompat.getParcelableExtra(it, NfcAdapter.EXTRA_TAG, Tag::class.java)
        } ?: return
        if (_state.value != NfcWriteState.WaitingForTag) return
        _state.value = NfcWriteState.Writing
        scope.launch {
            val outcome = writeToTag(tag, request.message)
            when (outcome) {
                is NfcWriteOutcome.Success -> {
                    pendingMessage = null
                    _state.value = NfcWriteState.Success(outcome.bytesWritten, outcome.tagDescription)
                }
                else -> _state.value = NfcWriteState.Failure(outcome, retryable = outcome != NfcWriteOutcome.TagNotNdefCompatible)
            }
        }
    }

    /** Synchronous low-level write; invoked on Dispatchers.IO. */
    private fun writeToTag(tag: Tag, message: NdefMessage): NfcWriteOutcome {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) {
                    return NfcWriteOutcome.TagNotWritable
                }
                val needed = message.byteArrayLength
                if (ndef.maxSize < needed) {
                    return NfcWriteOutcome.CapacityTooSmall(needed, ndef.maxSize)
                }
                ndef.writeNdefMessage(message)
                val description = tag.techList.joinToString(", ") { tech ->
                    tech.substringAfterLast('.')
                }
                return NfcWriteOutcome.Success(needed, description)
            } catch (e: android.nfc.TagLostException) {
                return NfcWriteOutcome.TagLost
            } catch (e: java.io.IOException) {
                return NfcWriteOutcome.IoFailure(e.message ?: "I/O error")
            } finally {
                try {
                    ndef.close()
                } catch (_: Exception) {
                }
            }
        }
        val formatable = NdefFormatable.get(tag)
        if (formatable != null) {
            try {
                formatable.connect()
                formatable.format(message)
                val description = tag.techList.joinToString(", ") { tech ->
                    tech.substringAfterLast('.')
                }
                return NfcWriteOutcome.Success(message.byteArrayLength, description)
            } catch (e: android.nfc.TagLostException) {
                return NfcWriteOutcome.TagLost
            } catch (e: java.io.IOException) {
                return NfcWriteOutcome.IoFailure(e.message ?: "Format I/O error")
            } finally {
                try {
                    formatable.close()
                } catch (_: Exception) {
                }
            }
        }
        return NfcWriteOutcome.TagNotNdefCompatible
    }
}
