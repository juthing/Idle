package com.juthing.idle.core.ui

import androidx.annotation.StringRes
import com.juthing.idle.R
import com.juthing.idle.domain.usecase.UnlockFailure

/**
 * What to tell the user about a refused attempt.
 *
 * Shared by the block screen and by the in-app unlock: the same refusal has to be worded the same
 * way wherever it happens, or the user starts wondering whether it is the same refusal at all.
 */
@StringRes
fun UnlockFailure.messageRes(): Int = when (this) {
    UnlockFailure.MISMATCH -> R.string.block_wrong_code
    UnlockFailure.OUT_OF_AREA -> R.string.block_out_of_area
    UnlockFailure.WRONG_KIND -> R.string.block_wrong_tag
    UnlockFailure.METHOD_INCOMPLETE -> R.string.block_method_broken
}
