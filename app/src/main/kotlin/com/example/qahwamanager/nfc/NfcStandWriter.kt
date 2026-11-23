package com.example.qahwamanager.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface NfcWriteState {
    data object Idle : NfcWriteState
    data class WaitingForTag(val url: String) : NfcWriteState
    data class Success(val url: String) : NfcWriteState
    data class Error(val message: String) : NfcWriteState
}

/**
 * Programs physical NTAG213/NTAG215 NFC table stands and stickers with the café's loyalty URL
 * when [state] is [NfcWriteState.WaitingForTag].
 */
@Singleton
class NfcStandWriter @Inject constructor() {

    private val _state = MutableStateFlow<NfcWriteState>(NfcWriteState.Idle)
    val state: StateFlow<NfcWriteState> = _state.asStateFlow()

    fun startWriting(url: String) {
        _state.value = NfcWriteState.WaitingForTag(url)
    }

    fun cancel() {
        _state.value = NfcWriteState.Idle
    }

    fun onTagDiscovered(tag: Tag) {
        val current = _state.value
        if (current !is NfcWriteState.WaitingForTag) return
        val url = current.url

        runCatching {
            val message = NdefMessage(arrayOf(NdefRecord.createUri(url)))
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    check(ndef.isWritable) { "NFC tag is read-only" }
                    check(ndef.maxSize >= message.toByteArray().size) { "NFC tag capacity too small" }
                    ndef.writeNdefMessage(message)
                } finally {
                    runCatching { ndef.close() }
                }
            } else {
                val formatable = NdefFormatable.get(tag)
                    ?: error("NFC tag does not support NDEF")
                formatable.connect()
                try {
                    formatable.format(message)
                } finally {
                    runCatching { formatable.close() }
                }
            }
            _state.value = NfcWriteState.Success(url)
        }.onFailure { err ->
            _state.value = NfcWriteState.Error(err.localizedMessage ?: "NFC write failed")
        }
    }
}
