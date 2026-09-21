package com.juthing.idle.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Fallback palettes used when the device cannot provide a dynamic Material You
 * scheme (Android 11 and below).
 *
 * The palette is deliberately desaturated: Idle should feel calm, and colour is
 * reserved for the few places that carry meaning (an active rule, a blocked app).
 */
private val Slate = Color(0xFF4A5B6B)
private val SlateLight = Color(0xFFD3E4F5)
private val Sand = Color(0xFF6B5D4A)
private val SandLight = Color(0xFFF5E4D3)
private val Clay = Color(0xFFBA1A1A)

internal val IdleLightColors = lightColorScheme(
    primary = Slate,
    onPrimary = Color.White,
    primaryContainer = SlateLight,
    onPrimaryContainer = Color(0xFF071A29),
    secondary = Sand,
    onSecondary = Color.White,
    secondaryContainer = SandLight,
    onSecondaryContainer = Color(0xFF241A0C),
    error = Clay,
    onError = Color.White,
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72777F),
)

internal val IdleDarkColors = darkColorScheme(
    primary = SlateLight,
    onPrimary = Color(0xFF213040),
    primaryContainer = Color(0xFF334757),
    onPrimaryContainer = SlateLight,
    secondary = SandLight,
    onSecondary = Color(0xFF3B2F20),
    secondaryContainer = Color(0xFF534635),
    onSecondaryContainer = SandLight,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199),
)
