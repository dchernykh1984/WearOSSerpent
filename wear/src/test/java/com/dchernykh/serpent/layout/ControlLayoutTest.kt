package com.dchernykh.serpent.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

private val SCREENS = listOf(384, 454, 466, 480)

private fun layoutFor(screen: Int): Pair<Board, Controls> {
    val board = boardLayout(screen, 15)
    return board to controlLayout(screen, board)
}

class ControlLayoutTest {
    @Test
    fun `reports the board it was given`() {
        val (board, controls) = layoutFor(466)

        assertEquals(Box(board.x, board.y, board.size, board.size), controls.board)
    }

    @Test
    fun `puts every control in the margin, never over the board`() {
        for (screen in SCREENS) {
            val (_, controls) = layoutFor(screen)
            val board = controls.board

            for (control in controls.arrows + controls.pause + controls.score) {
                val clear =
                    control.x + control.w <= board.x ||
                        control.x >= board.x + board.w ||
                        control.y + control.h <= board.y ||
                        control.y >= board.y + board.h
                assertTrue("$control overlaps the board on a $screen screen", clear)
            }
        }
    }

    @Test
    fun `mirrors the side arrows about the middle of the screen`() {
        for (screen in SCREENS) {
            val (_, controls) = layoutFor(screen)

            assertEquals(controls.left.w, controls.right.w)
            assertEquals(controls.left.h, controls.right.h)
            assertEquals(controls.left.y, controls.right.y)
            // Mirrored about the screen, not each centred in its own margin: the
            // board is centred to the nearest whole pixel, so the two margins can
            // differ by one and centring separately would show.
            assertEquals(controls.left.x, screen - controls.right.x - controls.right.w)
        }
    }

    @Test
    fun `centres the side arrows on the board`() {
        for (screen in SCREENS) {
            val (board, controls) = layoutFor(screen)
            val middle = board.y + board.size / 2

            assertTrue(abs(controls.left.y + controls.left.h / 2 - middle) <= 1)
        }
    }

    @Test
    fun `stacks the score above the up arrow without overlapping it`() {
        for (screen in SCREENS) {
            val (_, controls) = layoutFor(screen)

            assertTrue(controls.score.y + controls.score.h <= controls.up.y)
            assertTrue("no room for the up arrow on $screen", controls.up.h > 0)
        }
    }

    @Test
    fun `keeps the up arrow clear of the frame around the board`() {
        for (screen in SCREENS) {
            val (board, controls) = layoutFor(screen)

            assertTrue(controls.up.y + controls.up.h <= board.y - BOARD_EDGE)
        }
    }

    @Test
    fun `leaves dead space between the down arrow and pause`() {
        // Deliberate: pause is pressed twice a game and the arrow constantly, so a
        // thumb landing wide of the arrow does nothing rather than stopping the
        // game it was steering.
        for (screen in SCREENS) {
            val (_, controls) = layoutFor(screen)

            assertTrue(
                "down and pause touch on a $screen screen",
                controls.down.x + controls.down.w < controls.pause.x,
            )
        }
    }

    @Test
    fun `gives the down arrow far more room than pause`() {
        val (_, controls) = layoutFor(466)

        assertTrue(controls.down.w > controls.pause.w)
    }

    @Test
    fun `keeps the bottom row below the board`() {
        for (screen in SCREENS) {
            val (board, controls) = layoutFor(screen)

            assertTrue(controls.down.y > board.y + board.size + BOARD_EDGE)
            assertEquals(controls.down.y, controls.pause.y)
        }
    }

    @Test
    fun `keeps every control on the round screen`() {
        for (screen in SCREENS) {
            val (_, controls) = layoutFor(screen)

            for (control in controls.arrows + controls.pause + controls.score) {
                assertCornersOnScreen(screen, control, "control $control")
            }
        }
    }

    @Test
    fun `lists exactly the four steering boxes`() {
        val (_, controls) = layoutFor(466)

        assertEquals(listOf(controls.up, controls.down, controls.left, controls.right), controls.arrows)
    }
}
