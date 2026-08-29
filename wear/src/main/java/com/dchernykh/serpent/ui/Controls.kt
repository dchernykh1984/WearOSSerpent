package com.dchernykh.serpent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import com.dchernykh.serpent.layout.Stroke
import com.dchernykh.serpent.layout.Box as LayoutBox

/**
 * Place a composable at a box worked out in screen pixels.
 *
 * The layout is computed in whole pixels from the screen diameter, exactly as on
 * the watch this was ported from, so it is placed in pixels too: converting each
 * edge to dp and back would round it twice and pull the board off centre.
 */
fun Modifier.absoluteBox(box: LayoutBox): Modifier =
    this
        .offset { IntOffset(box.x, box.y) }
        .layout { measurable, _ ->
            val placeable = measurable.measure(Constraints.fixed(box.w, box.h))
            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }

/**
 * One icon-shaped control: a box in the margin around the board with a chevron or
 * a pause mark drawn in it, and nothing else.
 *
 * There is no ripple and no background. The controls sit in the segments a round
 * screen leaves over, where a filled button would read as a slab of chrome around
 * the game; the icon brightening under the finger is the whole of the feedback,
 * which is what the Zepp OS original did and is enough to tell a press from a miss.
 *
 * [strokes] arrive in screen coordinates, because that is where the size the four
 * arrows share is worked out, and are shifted into the box here.
 */
@Composable
fun StrokeControl(
    box: LayoutBox,
    strokes: List<Stroke>,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier =
            modifier
                .absoluteBox(box)
                .semantics { contentDescription = label }
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClickLabel = label,
                    onClick = onClick,
                ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = if (pressed) ColorArrowPressed else ColorArrow
            for (stroke in strokes) {
                drawLine(
                    color = color,
                    start = Offset((stroke.x1 - box.x).toFloat(), (stroke.y1 - box.y).toFloat()),
                    end = Offset((stroke.x2 - box.x).toFloat(), (stroke.y2 - box.y).toFloat()),
                    strokeWidth = stroke.width.toFloat(),
                    // The chevron is two strokes meeting at a point; butt ends
                    // would leave a notch at the tip where they cross.
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
