package com.juthing.idle.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Root theme for the whole app.
 *
 * Material You dynamic colours are used whenever the device provides them, so Idle
 * blends into the user's system palette; the bundled palettes in [IdleLightColors] and
 * [IdleDarkColors] are the fallback.
 *
 * @param darkTheme whether to render the dark scheme; follows the system by default.
 * @param dynamicColor whether to derive colours from the device wallpaper when available.
 */
@Composable
fun IdleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> IdleDarkColors
        else -> IdleLightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = IdleTypography,
        content = content,
    )
}
