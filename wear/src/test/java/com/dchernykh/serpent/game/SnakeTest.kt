package com.dchernykh.serpent.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * A position built by hand, so a test can start from a board that would take a
 * hundred ticks to reach by playing.
 */
private fun gameAt(
    snake: List<Cell>,
    cols: Int = 5,
    rows: Int = 5,
    direction: Direction = Direction.RIGHT,
    pending: Direction = direction,
    food: Cell? = null,
) = GameState(
    cols = cols,
    rows = rows,
    snake = snake,
    direction = direction,
    pending = pending,
    food = food,
)

/** Always picks the same free cell, so a test never depends on chance. */
private val FIRST_FREE = Random(0)

class NewGameTest {
    @Test
    fun `lays the snake across the middle row facing right`() {
        val game = newGame(15, 15, FIRST_FREE)

        assertEquals(Direction.RIGHT, game.direction)
        assertEquals(listOf(Cell(7, 7), Cell(6, 7), Cell(5, 7)), game.snake)
        assertEquals(0, game.score)
        assertEquals(GameStatus.RUNNING, game.status)
        assertNull(game.reason)
    }

    @Test
    fun `places the first pellet on a free cell`() {
        val game = newGame(15, 15, FIRST_FREE)

        assertNotNull(game.food)
        assertFalse(game.occupies(game.food!!))
    }

    @Test
    fun `shortens the starting body rather than hanging it off a tiny board`() {
        val game = newGame(2, 2, FIRST_FREE)

        assertEquals(2, game.snake.size)
        assertTrue(game.snake.all { it.x in 0 until game.cols && it.y in 0 until game.rows })
    }

    @Test
    fun `raises a board too small for a snake to move on`() {
        val game = newGame(0, -4, FIRST_FREE)

        assertEquals(MIN_BOARD, game.cols)
        assertEquals(MIN_BOARD, game.rows)
    }
}

class DirectionTest {
    @Test
    fun `pairs each direction with its opposite`() {
        assertEquals(Direction.DOWN, Direction.UP.opposite)
        assertEquals(Direction.UP, Direction.DOWN.opposite)
        assertEquals(Direction.LEFT, Direction.RIGHT.opposite)
        assertEquals(Direction.RIGHT, Direction.LEFT.opposite)
    }

    @Test
    fun `is its own opposite twice over`() {
        for (direction in Direction.entries) {
            assertEquals(direction, direction.opposite.opposite)
        }
    }
}

class TurnTest {
    private val game = gameAt(listOf(Cell(2, 2), Cell(1, 2)))

    @Test
    fun `accepts a perpendicular turn`() {
        assertEquals(Direction.UP, game.turned(Direction.UP).pending)
    }

    @Test
    fun `refuses a reversal, which would drive the head into the neck`() {
        assertEquals(Direction.RIGHT, game.turned(Direction.LEFT).pending)
    }

    @Test
    fun `refuses the direction already being travelled`() {
        assertEquals(game, game.turned(Direction.RIGHT))
    }

    @Test
    fun `cannot be tricked into a reversal by two turns inside one tick`() {
        // Up is legal while travelling right, but left is still a reversal of the
        // direction the last tick actually used, so it must not queue behind it.
        val turned = game.turned(Direction.UP).turned(Direction.LEFT)

        assertEquals(Direction.UP, turned.pending)
    }

    @Test
    fun `refuses a turn once the game has ended`() {
        val over = game.copy(status = GameStatus.OVER)

        assertEquals(over, over.turned(Direction.UP))
    }
}

class StepTest {
    @Test
    fun `moves the head one cell and frees the tail`() {
        val game = gameAt(listOf(Cell(1, 1), Cell(0, 1)), food = Cell(4, 4))

        val next = game.stepped(FIRST_FREE)

        assertEquals(listOf(Cell(2, 1), Cell(1, 1)), next.snake)
        assertEquals(0, next.score)
        assertEquals(GameStatus.RUNNING, next.status)
    }

    @Test
    fun `applies the queued turn`() {
        val game = gameAt(listOf(Cell(1, 1)), pending = Direction.DOWN, food = Cell(4, 4))

        val next = game.stepped(FIRST_FREE)

        assertEquals(Direction.DOWN, next.direction)
        assertEquals(listOf(Cell(1, 2)), next.snake)
    }

    @Test
    fun `grows and scores on food, and puts the next pellet somewhere free`() {
        val game = gameAt(listOf(Cell(1, 1)), food = Cell(2, 1))

        val next = game.stepped(FIRST_FREE)

        assertEquals(1, next.score)
        assertEquals(listOf(Cell(2, 1), Cell(1, 1)), next.snake)
        assertNotNull(next.food)
        assertFalse(next.occupies(next.food!!))
    }

    @Test
    fun `ends the game at a wall and leaves the wreck in place`() {
        val game = gameAt(listOf(Cell(0, 2)), direction = Direction.LEFT, food = Cell(4, 4))

        val next = game.stepped(FIRST_FREE)

        assertEquals(GameStatus.OVER, next.status)
        assertEquals(EndReason.HIT_WALL, next.reason)
        assertEquals(game.snake, next.snake)
    }

    @Test
    fun `ends the game on the snake's own body`() {
        val game =
            gameAt(
                snake = listOf(Cell(1, 1), Cell(2, 1), Cell(2, 2), Cell(1, 2), Cell(0, 2)),
                direction = Direction.LEFT,
                pending = Direction.DOWN,
                food = Cell(4, 4),
            )

        val next = game.stepped(FIRST_FREE)

        assertEquals(GameStatus.OVER, next.status)
        assertEquals(EndReason.HIT_SELF, next.reason)
    }

    @Test
    fun `lets the head take the tail cell it vacates in the same tick`() {
        val game =
            gameAt(
                snake = listOf(Cell(1, 1), Cell(2, 1), Cell(2, 2), Cell(1, 2)),
                direction = Direction.LEFT,
                pending = Direction.DOWN,
                food = Cell(4, 4),
            )

        val next = game.stepped(FIRST_FREE)

        assertEquals(GameStatus.RUNNING, next.status)
        assertEquals(Cell(1, 2), next.snake.first())
    }

    @Test
    fun `is a no-op once the game is over`() {
        val game = gameAt(listOf(Cell(1, 1)), food = Cell(4, 4)).copy(status = GameStatus.OVER)

        assertEquals(game, game.stepped(FIRST_FREE))
    }

    @Test
    fun `wins rather than crashing when the snake fills the board`() {
        // Three of the four cells are snake and the fourth is the pellet, so
        // taking it leaves nowhere for the next one.
        val game =
            gameAt(
                snake = listOf(Cell(0, 0), Cell(0, 1), Cell(1, 1)),
                cols = 2,
                rows = 2,
                food = Cell(1, 0),
            )

        val next = game.stepped(FIRST_FREE)

        assertEquals(GameStatus.WON, next.status)
        assertNull(next.food)
        assertEquals(next.cols * next.rows, next.snake.size)
        assertNull(next.reason)
    }
}

class PlaceFoodTest {
    @Test
    fun `never lands on the snake, whatever the random value`() {
        val game = gameAt(listOf(Cell(1, 1), Cell(1, 2)), cols = 4, rows = 4)

        for (seed in 0 until 200) {
            val food = game.placeFood(Random(seed))
            assertNotNull(food)
            assertFalse("pellet landed on the snake for seed $seed", game.occupies(food!!))
            assertTrue(food.x in 0 until game.cols && food.y in 0 until game.rows)
        }
    }

    @Test
    fun `returns nothing when there is no free cell left`() {
        val full =
            gameAt(
                snake = listOf(Cell(0, 0), Cell(1, 0), Cell(0, 1), Cell(1, 1)),
                cols = 2,
                rows = 2,
            )

        assertNull(full.placeFood(FIRST_FREE))
    }

    @Test
    fun `reaches every free cell over enough games`() {
        val game = gameAt(listOf(Cell(0, 0)), cols = 2, rows = 2)

        val seen = (0 until 200).mapNotNull { game.placeFood(Random(it)) }.toSet()

        assertEquals(setOf(Cell(1, 0), Cell(0, 1), Cell(1, 1)), seen)
    }
}

class OccupiesTest {
    private val game = gameAt(listOf(Cell(1, 1), Cell(2, 1), Cell(3, 1)))

    @Test
    fun `reports body cells`() {
        assertTrue(game.occupies(Cell(1, 1)))
        assertTrue(game.occupies(Cell(2, 1)))
        assertFalse(game.occupies(Cell(4, 4)))
    }

    @Test
    fun `skips the tail on request`() {
        assertTrue(game.occupies(Cell(3, 1)))
        assertFalse(game.occupies(Cell(3, 1), ignoreTail = true))
        assertTrue(game.occupies(Cell(1, 1), ignoreTail = true))
    }
}
