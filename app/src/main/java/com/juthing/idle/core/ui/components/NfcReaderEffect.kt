package com.juthing.idle.core.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.juthing.idle.data.system.NfcTagReader

/**
 * Listens for NFC tags while this composable is on screen.
 *
 * Reader mode is bound to the hosting activity and released as soon as the screen leaves, so Idle
 * never intercepts tags the user meant for another app.
 *
 * @param onTag called with the tag's identifier, on the main thread.
 */
@Composable
fun NfcReaderEffect(
    reader: NfcTagReader,
    onTag: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity ?: return
    val currentOnTag by rememberUpdatedState(onTag)

    DisposableEffect(activity) {
        reader.startReading(activity) { tagId ->
            activity.runOnUiThread { currentOnTag(tagId) }
        }
        onDispose { reader.stopReading(activity) }
    }
}
