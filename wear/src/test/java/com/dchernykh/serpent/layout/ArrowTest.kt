package com.dchernykh.serpent.layout

import com.dchernykh.serpent.game.Direction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val SCREENS = listOf(384, 454, 466, 480)

private fun controlsFor(screen: Int): Controls = controlLayout(screen, boardLayout(screen, 15))

class ArrowMetricsTest {
    @Test
    fun `gives an arrow a stroke and a reach`() {
        val metrics = arrowMetrics(controlsFor(466).arrows)

        assertTrue(metrics.width >= 2)
        assertTrue(metrics.reach > 0)
    }

    @Test
    fun `is decided by the tightest box, so one size fits all four`() {
        val boxes = listOf(Box(0, 0, 100, 100), Box(0, 0, 40, 40))

        assertEquals(arrowMetrics(listOf(Box(0, 0, 40, 40))), arrowMetrics(boxes))
    }

    @Test
    fun `keeps every stroke inside its box, at every screen size`() {
        for (screen in SCREENS) {
            val controls = controlsFor(screen)
            val metrics = arrowMetrics(controls.arrows)
            val drawn =
                Direction.entries.map { direction ->
                    val box = boxFor(controls, direction)
                    box to arrowStrokes(direction, box, metrics)
                } + (controls.pause to pauseStrokes(controls.pause, metrics))

            for ((box, strokes) in drawn) {
                for (stroke in strokes) {
                    assertStrokeInside(box, stroke, "an icon on a $screen screen")
                }
            }
        }
    }

    @Test
    fun `draws nothing in a box with no room in it`() {
        assertEquals(ArrowMetrics(0, 0), arrowMetrics(listOf(Box(0, 0, 1, 1))))
        assertEquals(ArrowMetrics(0, 0), arrowMetrics(emptyList()))
        assertEquals(emptyList<Stroke>(), arrowStrokes(Direction.UP, Box(0, 0, 1, 1), ArrowMetrics(0, 0)))
        assertEquals(emptyList<Stroke>(), pauseStrokes(Box(0, 0, 1, 1), ArrowMetrics(0, 0)))
    }

    private fun boxFor(
        controls: Controls,
        direction: Direction,
    ) = when (direction) {
        Direction.UP -> controls.up
        Direction.DOWN -> controls.down
        Direction.LEFT -> controls.left
        Direction.RIGHT -> controls.right
    }
}

class ArrowStrokesTest {
    private val box = Box(100, 100, 60, 60)
    private val metrics = arrowMetrics(listOf(box))

    @Test
    fun `is a chevron - two strokes meeting at the tip`() {
        val strokes = arrowStrokes(Direction.UP, box, metrics)

        assertEquals(2, strokes.size)
        assertEquals(strokes[0].x2, strokes[1].x1)
        assertEquals(strokes[0].y2, strokes[1].y1)
    }

    @Test
    fun `points its tip the way it steers`() {
        val centreX = box.x + box.w / 2
        val centreY = box.y + box.h / 2

        assertTrue(arrowStrokes(Direction.UP, box, metrics)[0].y2 < centreY)
        assertTrue(arrowStrokes(Direction.DOWN, box, metrics)[0].y2 > centreY)
        assertTrue(arrowStrokes(Direction.LEFT, box, metrics)[0].x2 < centreX)
        assertTrue(arrowStrokes(Direction.RIGHT, box, metrics)[0].x2 > centreX)
    }

    @Test
    fun `spreads its arms either side of the tip`() {
        val strokes = arrowStrokes(Direction.UP, box, metrics)

        assertTrue(strokes[0].x1 < strokes[0].x2)
        assertTrue(strokes[1].x2 > strokes[1].x1)
        assertEquals(strokes[0].y1, strokes[1].y2)
    }

    @Test
    fun `draws pause as two upright bars of the arrows' weight`() {
        val strokes = pauseStrokes(box, metrics)

        assertEquals(2, strokes.size)
        for (stroke in strokes) {
            assertEquals("a pause bar is not upright", stroke.x1, stroke.x2)
            assertEquals(metrics.width, stroke.width)
        }
        assertTrue(strokes[0].x1 < strokes[1].x1)
    }
}
