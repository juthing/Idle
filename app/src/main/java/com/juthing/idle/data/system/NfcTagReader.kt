package com.juthing.idle.data.system

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Whether the device can read NFC tags at all, and whether it is switched on. */
enum class NfcAvailability { AVAILABLE, DISABLED, UNSUPPORTED }

/**
 * Reads NFC tags through the reader-mode API.
 *
 * Reader mode is used rather than foreground dispatch: it keeps tag delivery inside the screen
 * that asked for it, and it does not bounce the user through a new intent while they are holding
 * a tag against the phone.
 *
 * Only the tag's hardware identifier is read. Idle never writes to a tag and never reads its
 * contents, so any tag already in use for something else keeps working.
 */
@Singleton
class NfcTagReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val adapter: NfcAdapter? get() = NfcAdapter.getDefaultAdapter(context)

    fun availability(): NfcAvailability = when {
        adapter == null -> NfcAvailability.UNSUPPORTED
        adapter?.isEnabled != true -> NfcAvailability.DISABLED
        else -> NfcAvailability.AVAILABLE
    }

    /**
     * Starts delivering tags to [onTag] while [activity] is in the foreground.
     *
     * @param onTag called on a binder thread with the tag's identifier as lowercase hex.
     */
    fun startReading(activity: Activity, onTag: (String) -> Unit) {
        adapter?.enableReaderMode(
            activity,
            { tag: Tag -> onTag(tag.id.toHexString()) },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null,
        )
    }

    fun stopReading(activity: Activity) {
        adapter?.disableReaderMode(activity)
    }

    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { "%02x".format(it) }
}
