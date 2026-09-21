package com.juthing.idle.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.juthing.idle.R
import com.juthing.idle.domain.model.UnlockMethodType

/**
 * The single source of what each unlock method looks like and is called.
 *
 * The three methods appear in six different places — the list, the creation flow, the rule
 * editor, the picker sheet, the block screen and the in-app unlock — and a method that is a
 * camera here and a key there is a method the user has to re-learn on every screen.
 */

/** The icon that stands for [UnlockMethodType] everywhere it is shown. */
val UnlockMethodType.icon: ImageVector
    get() = when (this) {
        UnlockMethodType.QR -> Icons.Outlined.QrCodeScanner
        UnlockMethodType.NFC -> Icons.Outlined.Nfc
        UnlockMethodType.LOCATION -> Icons.Outlined.Place
    }

/** The localised name of a method type. */
fun UnlockMethodType.labelRes(): Int = when (this) {
    UnlockMethodType.QR -> R.string.unlock_type_qr
    UnlockMethodType.NFC -> R.string.unlock_type_nfc
    UnlockMethodType.LOCATION -> R.string.unlock_type_location
}

/** The one-line explanation of a method type, shown when choosing one. */
fun UnlockMethodType.summaryRes(): Int = when (this) {
    UnlockMethodType.QR -> R.string.unlock_type_qr_summary
    UnlockMethodType.NFC -> R.string.unlock_type_nfc_summary
    UnlockMethodType.LOCATION -> R.string.unlock_type_location_summary
}

/** The instruction shown while Idle is waiting for this kind of proof. */
fun UnlockMethodType.promptRes(): Int = when (this) {
    UnlockMethodType.QR -> R.string.capture_prompt_qr
    UnlockMethodType.NFC -> R.string.capture_prompt_nfc
    UnlockMethodType.LOCATION -> R.string.capture_prompt_location
}
