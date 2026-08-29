package com.dchernykh.serpent.layout

import com.dchernykh.serpent.game.Direction
import kotlin.math.floor
import kotlin.math.roundToInt

// The control icons, as line segments.
//
// A chevron is two thick strokes meeting at a tip rather than a filled triangle,
// which is how the Zepp OS original drew them and how they stay legible at watch
// size: a filled arrowhead small enough to fit in the margin reads as a blob.
//
// Pure, so a test can ask what an arrow looks like without a screen in the room;
// the canvas only executes what these return.

/** One stroke of an icon, in screen pixels. */
data class Stroke(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val width: Int,
)

/** How thick a stroke is and how far an icon reaches from its centre. */
data class ArrowMetrics(
    val reach: Int,
    val width: Int,
)

// Both as fractions of the smallest control, so the set scales with the screen.
private const val STROKE_RATIO = 0.14f
private const val REACH_RATIO = 0.42f
private const val MIN_STROKE = 2

/**
 * One size shared by every icon, so the five read as one set of controls rather
 * than as five unrelated marks. The smallest box decides, so the shared size fits
 * every one of them, stroke included.
 */
fun arrowMetrics(boxes: List<Box>): ArrowMetrics {
    if (boxes.isEmpty()) return ArrowMetrics(reach = 0, width = 0)

    var shortest = Int.MAX_VALUE
    // How far a centre can travel before it leaves the tightest box. Measured from
    // the centre the icon is actually drawn around, which is rounded to a whole
    // pixel and so is not exactly half way across an odd-sized box.
    var room = Int.MAX_VALUE
    for (b in boxes) {
        shortest = minOf(shortest, b.w, b.h)
        val midX = (b.x + b.w / 2f).roundToInt()
        val midY = (b.y + b.h / 2f).roundToInt()
        room = minOf(room, midX - b.x, b.x + b.w - midX, midY - b.y, b.y + b.h - midY)
    }
    if (shortest <= 0 || room <= 0) return ArrowMetrics(reach = 0, width = 0)

    val width = maxOf(MIN_STROKE, (shortest * STROKE_RATIO).roundToInt())
    // Half the stroke hangs outside the endpoint it is drawn from, so the reach has
    // to leave room for it or the icon overhangs its box.
    val reach = maxOf(0, floor(minOf(shortest * REACH_RATIO, room - width / 2f)).toInt())
    return ArrowMetrics(reach = reach, width = width)
}

/** An arrow, as the two strokes of its chevron. */
fun arrowStrokes(
    direction: Direction,
    area: Box,
    metrics: ArrowMetrics,
): List<Stroke> {
    val reach = metrics.reach
    if (reach <= 0) return emptyList()
    val midX = (area.x + area.w / 2f).roundToInt()
    val midY = (area.y + area.h / 2f).roundToInt()

    val tipX: Int
    val tipY: Int
    val armAX: Int
    val armAY: Int
    val armBX: Int
    val armBY: Int
    when (direction) {
        Direction.UP -> {
            tipX = midX
            tipY = midY - reach
            armAX = midX - reach
            armAY = midY + reach
            armBX = midX + reach
            armBY = midY + reach
        }
        Direction.DOWN -> {
            tipX = midX
            tipY = midY + reach
            armAX = midX - reach
            armAY = midY - reach
            armBX = midX + reach
            armBY = midY - reach
        }
        Direction.LEFT -> {
            tipX = midX - reach
            tipY = midY
            armAX = midX + reach
            armAY = midY - reach
            armBX = midX + reach
            armBY = midY + reach
        }
        Direction.RIGHT -> {
            tipX = midX + reach
            tipY = midY
            armAX = midX - reach
            armAY = midY - reach
            armBX = midX - reach
            armBY = midY + reach
        }
    }
    return listOf(
        Stroke(armAX, armAY, tipX, tipY, metrics.width),
        Stroke(tipX, tipY, armBX, armBY, metrics.width),
    )
}

/**
 * Pause: the two upright bars everything else in the world uses for it, drawn at
 * the same stroke weight as the arrows because it sits in the same row and a
 * hairline icon beside a thick chevron looks like a different app drew it.
 */
fun pauseStrokes(
    area: Box,
    metrics: ArrowMetrics,
): List<Stroke> {
    val reach = metrics.reach
    if (reach <= 0) return emptyList()
    val midX = (area.x + area.w / 2f).roundToInt()
    val midY = (area.y + area.h / 2f).roundToInt()
    val gap = maxOf(metrics.width, (reach * 0.55f).roundToInt())
    return listOf(
        Stroke(midX - gap, midY - reach, midX - gap, midY + reach, metrics.width),
        Stroke(midX + gap, midY - reach, midX + gap, midY + reach, metrics.width),
    )
}
