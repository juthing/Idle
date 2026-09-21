package com.juthing.idle.core.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * The haptic vocabulary the whole app speaks.
 *
 * Every vibration in Idle goes through one of these five verbs rather than through
 * [HapticFeedbackType] directly, so that the same gesture never feels different from one screen
 * to the next, and so that the mapping can be tuned in one place.
 *
 * The rule is deliberately narrow: a touch that only navigates stays silent, because a phone that
 * buzzes at everything stops saying anything.
 */

/** A choice was registered — a chip, a row, a picker entry. */
fun HapticFeedback.tap() = performHapticFeedback(HapticFeedbackType.ContextClick)

/** A switch or checkbox moved, with the direction it moved in. */
fun HapticFeedback.toggle(on: Boolean) =
    performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

/** Something worked: a code scanned, a tag read, a rule saved, a block lifted. */
fun HapticFeedback.confirm() = performHapticFeedback(HapticFeedbackType.Confirm)

/** Something was refused: the wrong code, the wrong place, an edit the lock forbids. */
fun HapticFeedback.reject() = performHapticFeedback(HapticFeedbackType.Reject)

/** One notch along a continuous control, so a value can be felt as well as read. */
fun HapticFeedback.step() = performHapticFeedback(HapticFeedbackType.SegmentTick)
