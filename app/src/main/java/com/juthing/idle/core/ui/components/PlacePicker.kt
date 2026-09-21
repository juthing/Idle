package com.juthing.idle.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.juthing.idle.R
import androidx.compose.ui.res.stringResource
import com.juthing.idle.core.ui.step
import kotlin.math.cos

/** Metres in one degree of latitude. Close enough everywhere; longitude is scaled by latitude. */
private const val METERS_PER_DEGREE_LATITUDE = 111_320.0

/** How wide the view is, in metres, at each zoom step: a room, a building, a neighbourhood. */
private val SPANS_METERS = listOf(60, 250, 1_000)

/**
 * Places the centre of an unlock area by hand.
 *
 * Idle does not declare the internet permission, so there are no map tiles to show and there
 * never will be: a place picker that needed a tile server would trade the app's central promise
 * for a prettier screen. What it draws instead is the only thing that actually matters here —
 * the distance and bearing from where the phone is standing to the point being chosen, with the
 * unlock radius drawn to scale around it.
 *
 * Dragging moves the point; the scale chips change how much ground the square covers.
 *
 * @param anchorLatitude the position the device reported, drawn as the reference dot.
 * @param latitude the point currently chosen, which starts equal to the anchor.
 * @param onMove called with a new latitude and longitude as the point is dragged.
 */
@Composable
fun PlacePicker(
    anchorLatitude: Double,
    anchorLongitude: Double,
    latitude: Double,
    longitude: Double,
    radiusMeters: Int,
    onMove: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    var spanIndex by remember { mutableIntStateOf(1) }
    val spanMeters = SPANS_METERS[spanIndex]

    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    val grid = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val anchorColor = MaterialTheme.colorScheme.tertiary

    // One degree of longitude shrinks towards the poles; without this the picked point would
    // drift east or west of where the user put their finger.
    val metersPerDegreeLongitude = METERS_PER_DEGREE_LATITUDE * cos(Math.toRadians(anchorLatitude))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .pointerInput(spanMeters, anchorLatitude, anchorLongitude) {
                    val metersPerPixel = spanMeters.toFloat() / size.width
                    detectDragGestures(
                        onDragStart = { haptics.step() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val east = dragAmount.x * metersPerPixel
                            // Screen y grows downwards; north is up.
                            val north = -dragAmount.y * metersPerPixel
                            onMove(
                                latitude + north / METERS_PER_DEGREE_LATITUDE,
                                longitude + east / metersPerDegreeLongitude,
                            )
                        },
                    )
                }
                .pointerInput(spanMeters, anchorLatitude, anchorLongitude) {
                    val metersPerPixel = spanMeters.toFloat() / size.width
                    detectTapGestures { tap ->
                        haptics.step()
                        val east = (tap.x - size.width / 2f) * metersPerPixel
                        val north = -(tap.y - size.height / 2f) * metersPerPixel
                        onMove(
                            anchorLatitude + north / METERS_PER_DEGREE_LATITUDE,
                            anchorLongitude + east / metersPerDegreeLongitude,
                        )
                    }
                },
        ) {
            val metersPerPixel = spanMeters / size.width
            val centre = Offset(size.width / 2f, size.height / 2f)

            drawRect(color = surface)

            // A grid at a round interval, so the scale is legible without a ruler.
            val gridStep = size.width / 4f
            val dashes = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
            for (i in 1..3) {
                drawLine(grid, Offset(gridStep * i, 0f), Offset(gridStep * i, size.height), pathEffect = dashes)
                drawLine(grid, Offset(0f, gridStep * i), Offset(size.width, gridStep * i), pathEffect = dashes)
            }

            val east = (longitude - anchorLongitude) * metersPerDegreeLongitude
            val north = (latitude - anchorLatitude) * METERS_PER_DEGREE_LATITUDE
            val point = Offset(
                centre.x + (east / metersPerPixel).toFloat(),
                centre.y - (north / metersPerPixel).toFloat(),
            )

            // The unlock area, to scale: the user can see at a glance whether their radius covers
            // the whole flat or half the street.
            val radiusPixels = radiusMeters / metersPerPixel
            drawCircle(color = accent.copy(alpha = 0.16f), radius = radiusPixels, center = point)
            drawCircle(
                color = accent,
                radius = radiusPixels,
                center = point,
                style = Stroke(width = 2.dp.toPx()),
            )

            // Where the phone actually is, for reference.
            drawCircle(color = anchorColor, radius = 5.dp.toPx(), center = centre)

            // The chosen point.
            drawCircle(color = accent, radius = 9.dp.toPx(), center = point)
            drawCircle(
                color = surface,
                radius = 4.dp.toPx(),
                center = point,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SPANS_METERS.forEachIndexed { index, span ->
                FilterChip(
                    selected = index == spanIndex,
                    onClick = { spanIndex = index },
                    label = {
                        Text(
                            text = stringResource(R.string.place_span, span),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text(
            text = stringResource(R.string.place_picker_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}