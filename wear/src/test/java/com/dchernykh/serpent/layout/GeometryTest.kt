package com.dchernykh.serpent.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two round sizes the game is built for, and a small one for good measure. */
private val SCREENS = listOf(384, 454, 466, 480)

class RoundGeometryTest {
    @Test
    fun `is widest on the centre line and narrows towards the edge`() {
        assertEquals(100f, safeHalfWidth(100f, 0f), 0.001f)
        assertEquals(80f, safeHalfWidth(100f, 60f), 0.001f)
        assertEquals(0f, safeHalfWidth(100f, 100f), 0.001f)
    }

    @Test
    fun `has no width at all past the edge of the circle`() {
        assertEquals(0f, safeHalfWidth(100f, 140f), 0.001f)
    }

    @Test
    fun `measures the same either side of the centre line`() {
        assertEquals(safeHalfWidth(100f, 42f), safeHalfWidth(100f, -42f), 0.001f)
    }

    @Test
    fun `binds a line by whichever of its edges is further out`() {
        // A line just below the centre is bound by its lower edge, and one just
        // above it by its upper edge, so the two are mirror images.
        val below = safeLineWidth(466, 300f, 40f, 8)
        val above = safeLineWidth(466, 166f, 40f, 8)

        assertEquals(above, below, 0.001f)
        assertTrue(below < safeLineWidth(466, 233f, 40f, 8))
    }

    @Test
    fun `gives nothing to a line pushed off the screen`() {
        assertEquals(0f, safeLineWidth(466, -50f, 40f, 8), 0.001f)
        assertEquals(0f, safeLineWidth(466, 520f, 40f, 8), 0.001f)
    }

    @Test
    fun `keeps every corner of a centred box on the screen`() {
        // The property that matters: wherever the box is put, the bezel never
        // slices a corner off it.
        for (screen in SCREENS) {
            var top = 0
            while (top < screen - 30) {
                val box = centeredBox(screen, top, 30, screen.toFloat(), SCREEN_PADDING)
                if (box.w > 0) assertCornersOnScreen(screen, box, "a box at $top")
                top += 7
            }
        }
    }

    @Test
    fun `keeps the padding clear of the bezel across the box, not around it`() {
        // The padding is horizontal: it is measured along the row that binds the
        // box, which is whichever of its edges is further from the centre line. A
        // corner may therefore sit closer to the bezel than the padding without
        // the box being any narrower than it should be - a diagonal distance is
        // not what a chord is inset by.
        val screen = 466
        val radius = screen / 2f
        val box = centeredBox(screen, 40, 30, screen.toFloat(), SCREEN_PADDING)
        val bindingRow = box.y.toFloat()
        val half = safeHalfWidth(radius, bindingRow - radius)

        assertEquals(radius - half + SCREEN_PADDING, box.x.toFloat(), 1f)
        assertEquals(radius + half - SCREEN_PADDING, (box.x + box.w).toFloat(), 1f)
    }

    @Test
    fun `never exceeds the width it was asked for`() {
        val box = centeredBox(466, 200, 40, 100f, SCREEN_PADDING)

        assertEquals(100, box.w)
        assertEquals(200, box.y)
        assertEquals(40, box.h)
    }

    @Test
    fun `centres what it places`() {
        for (screen in SCREENS) {
            val box = centeredBox(screen, screen / 2, 40, 120f, SCREEN_PADDING)
            assertEquals(screen - box.x - box.w, box.x)
        }
    }

    @Test
    fun `knows what it contains`() {
        val box = Box(10, 20, 30, 40)

        assertTrue((10 to 20) in box)
        assertTrue((39 to 59) in box)
        assertTrue((9 to 20) !in box)
        assertTrue((40 to 20) !in box)
        assertTrue((10 to 60) !in box)
    }
}

class BoardLayoutTest {
    @Test
    fun `fits inside the square inscribed in the round screen`() {
        for (screen in SCREENS) {
            val board = boardLayout(screen, 15)
            val inscribed = (screen / 1.41421356f).toInt()

            assertTrue("board of ${board.size} overflows $inscribed", board.size <= inscribed)
        }
    }

    @Test
    fun `divides into cells of exactly equal size`() {
        for (screen in SCREENS) {
            val board = boardLayout(screen, 15)

            assertEquals(board.cell * 15, board.size)
            assertEquals(15, board.cells)
        }
    }

    @Test
    fun `is centred on the screen`() {
        for (screen in SCREENS) {
            val board = boardLayout(screen, 15)

            assertEquals(board.x, board.y)
            // Centring lands on a whole pixel, so the two margins may differ by one.
            assertTrue(kotlin.math.abs(screen - board.x - board.size - board.x) <= 1)
        }
    }

    @Test
    fun `keeps a cell even when asked for more cells than pixels`() {
        val board = boardLayout(40, 400)

        assertTrue(board.cell >= 1)
    }

    @Test
    fun `refuses a board of no columns`() {
        assertEquals(1, boardLayout(466, 0).cells)
        assertEquals(1, boardLayout(466, -5).cells)
    }

    @Test
    fun `places every cell inside the board, in order`() {
        val board = boardLayout(466, 15)

        val first = cellRect(board, 0, 0, CELL_INSET_FOR_TEST)
        val last = cellRect(board, 14, 14, CELL_INSET_FOR_TEST)

        assertTrue(first.x >= board.x && first.y >= board.y)
        assertTrue(last.x + last.w <= board.x + board.size)
        assertTrue(last.y + last.h <= board.y + board.size)
        assertEquals(first.w, last.w)
    }

    @Test
    fun `leaves neighbouring cells a gap on every side`() {
        val board = boardLayout(466, 15)

        val left = cellRect(board, 3, 3, 1)
        val right = cellRect(board, 4, 3, 1)

        assertTrue(left.x + left.w < right.x)
        assertEquals(board.cell - 2, left.w)
    }

    @Test
    fun `refuses to let the inset swallow a small cell`() {
        val board = boardLayout(40, 15)
        val rect = cellRect(board, 0, 0, 50)

        assertTrue("a cell collapsed to ${rect.w}", rect.w > 0)
    }

    @Test
    fun `treats a negative inset as none`() {
        val board = boardLayout(466, 15)

        assertEquals(board.cell, cellRect(board, 0, 0, -4).w)
    }

    private companion object {
        const val CELL_INSET_FOR_TEST = 1
    }
}
