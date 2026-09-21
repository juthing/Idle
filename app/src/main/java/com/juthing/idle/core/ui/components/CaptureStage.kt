package com.juthing.idle.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juthing.idle.core.ui.confirm
import com.juthing.idle.core.ui.reject

/** Where a capture — a scan, a tap, a position read — currently stands. */
enum class CaptureState {
    /** Nothing has happened yet; the target is waiting and breathing. */
    WAITING,

    /** Work is under way: the camera is up, or a position is being read. */
    BUSY,

    /** The thing was read and accepted. */
    DONE,

    /** The thing was read and refused, or could not be read at all. */
    FAILED,
}

/**
 * The single visual language for "present something to the phone".
 *
 * Registering a QR code, tapping a tag, checking a position and lifting a block are the same
 * moment wearing four hats, and they used to be four different paragraphs of text. Here they are
 * one target that reacts: it breathes while it waits, it snaps shut when it succeeds, and it says
 * so with a vibration at the same instant. That confirmation is the whole reward for the gesture
 * Idle asks for, so it is the one place in the app where an animation earns its keep.
 *
 * @param icon what is being asked for, drawn until the outcome replaces it.
 * @param title the instruction, or the result once there is one.
 * @param subtitle an optional second line; hidden when blank.
 * @param content an optional viewfinder drawn inside the target instead of the icon — the camera
 *   preview, in practice.
 */
@Composable
fun CaptureStage(
    icon: ImageVector,
    title: String,
    state: CaptureState,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val haptics = LocalHapticFeedback.current

    // Fired from the state itself rather than from each call site: every screen that reaches DONE
    // owes the user the same confirmation, and none of them can forget it here.
    LaunchedEffect(state) {
        when (state) {
            CaptureState.DONE -> haptics.confirm()
            CaptureState.FAILED -> haptics.reject()
            else -> Unit
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (content != null && state != CaptureState.DONE) {
            Viewfinder(content = content)
        } else {
            Target(icon = icon, state = state)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(160)) },
                label = "capture-title",
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = when (state) {
                        CaptureState.DONE -> MaterialTheme.colorScheme.primary
                        CaptureState.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The round badge: an icon on a tonal disc, with a halo that breathes while it waits. */
@Composable
private fun Target(icon: ImageVector, state: CaptureState) {
    val container = when (state) {
        CaptureState.DONE -> MaterialTheme.colorScheme.primaryContainer
        CaptureState.FAILED -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val onContainer = when (state) {
        CaptureState.DONE -> MaterialTheme.colorScheme.onPrimaryContainer
        CaptureState.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val waiting = state == CaptureState.WAITING || state == CaptureState.BUSY
    val pulse = rememberInfiniteTransition(label = "halo")
    val haloScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo-scale",
    )
    val haloAlpha by pulse.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo-alpha",
    )

    // The disc itself lands with a short spring, which is what makes a successful scan feel like
    // an event rather than a text change.
    val discScale by animateFloatAsState(
        targetValue = if (state == CaptureState.DONE) 1f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "disc-scale",
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
        if (waiting) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha)),
            )
        }
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(discScale)
                .clip(CircleShape)
                .background(container),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn())
                        .togetherWith(fadeOut(tween(120)))
                },
                label = "capture-icon",
            ) { current ->
                Icon(
                    imageVector = when (current) {
                        CaptureState.DONE -> Icons.Outlined.Check
                        CaptureState.FAILED -> Icons.Outlined.Close
                        else -> icon
                    },
                    contentDescription = null,
                    tint = onContainer,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
    }
}

/** A square viewfinder with a sweep line, so a live camera reads as one. */
@Composable
private fun Viewfinder(content: @Composable () -> Unit) {
    val sweep = rememberInfiniteTransition(label = "sweep")
    val position by sweep.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sweep-position",
    )
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        content()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val band = size.height * 0.16f
            val top = (size.height + band) * position - band
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, accent.copy(alpha = 0.45f), Color.Transparent),
                    startY = top,
                    endY = top + band,
                ),
                topLeft = Offset(0f, top),
                size = Size(size.width, band),
            )

            // A frame rather than a full overlay: it says where to aim without hiding the scene.
            val inset = size.minDimension * 0.12f
            drawRoundRect(
                color = accent.copy(alpha = 0.7f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                cornerRadius = CornerRadius(inset * 0.6f),
                style = Stroke(width = 3.dp.toPx()),
            )
        }
    }
}
