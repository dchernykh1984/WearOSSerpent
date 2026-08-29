package com.dchernykh.serpent.game

import kotlin.random.Random

// The whole rule set, with nothing Android in it, so every rule is exercised by a
// unit test rather than by squinting at a watch. This file owns truth; the screen
// owns pixels and input.
//
// A position is an immutable value and a tick is a function from one to the next.
// That is what lets a test start from any board it likes - a snake curled back on
// itself, a board one pellet from full - rather than only from positions it can
// reach by playing. The cost is a new body list per tick, which for a board of
// two hundred cells at ten ticks a second is nothing worth complicating the rules
// over.

/** A cell of the grid, counted from the top left. */
data class Cell(
    val x: Int,
    val y: Int,
)

/**
 * The four ways the snake can travel. Declared clockwise, so the reverse of a
 * direction is the one two places along.
 */
enum class Direction(
    val dx: Int,
    val dy: Int,
) {
    UP(0, -1),
    RIGHT(1, 0),
    DOWN(0, 1),
    LEFT(-1, 0),
    ;

    val opposite: Direction get() = entries[(ordinal + 2) % entries.size]
}

/**
 * WON is reachable only by filling every cell of the board. It is a win rather
 * than a crash, so a perfect game ends gracefully instead of looking like a bug.
 */
enum class GameStatus { RUNNING, OVER, WON }

/** Why a game ended, so the screen can say something better than "over". */
enum class EndReason { HIT_WALL, HIT_SELF }

/** How long the snake is at the start, before it is clamped to fit its row. */
const val START_LENGTH = 3

/** The smallest board a snake has anywhere to go on. */
const val MIN_BOARD = 2

/**
 * A position: the board, the snake on it head first, where it is going, and what
 * has happened to it so far.
 *
 * [pending] is the turn the next tick will take. It is separate from [direction]
 * so that two quick turns inside one tick cannot add up to a reversal - see
 * [turned].
 */
data class GameState(
    val cols: Int,
    val rows: Int,
    val snake: List<Cell>,
    val direction: Direction,
    val pending: Direction,
    val food: Cell?,
    val score: Int = 0,
    val status: GameStatus = GameStatus.RUNNING,
    val reason: EndReason? = null,
)

/**
 * A fresh game: the snake lies across the middle row facing right, with the first
 * pellet already placed.
 *
 * A board smaller than [MIN_BOARD] a side has nowhere for a snake to go, so the
 * dimensions are raised; the body is shortened rather than hung off the edge of a
 * board too narrow to hold all of it.
 */
fun newGame(
    cols: Int,
    rows: Int,
    random: Random = Random.Default,
): GameState {
    val width = maxOf(MIN_BOARD, cols)
    val height = maxOf(MIN_BOARD, rows)
    val row = height / 2
    val headColumn = width / 2
    val length = maxOf(1, minOf(START_LENGTH, headColumn + 1))
    val snake = List(length) { i -> Cell(headColumn - i, row) }

    val started =
        GameState(
            cols = width,
            rows = height,
            snake = snake,
            direction = Direction.RIGHT,
            pending = Direction.RIGHT,
            food = null,
        )
    return started.copy(food = started.placeFood(random))
}

/** Whether any part of the snake is on this cell, optionally ignoring the tail. */
fun GameState.occupies(
    cell: Cell,
    ignoreTail: Boolean = false,
): Boolean {
    val last = snake.size - 1
    for (i in snake.indices) {
        if (ignoreTail && i == last) continue
        if (snake[i] == cell) return true
    }
    return false
}

/**
 * Queue a turn for the next tick, or return the position unchanged when the rules
 * refuse it.
 *
 * A reversal is refused rather than fatal: the head would run straight into the
 * neck, and losing a game to a stray swipe is not a rule anyone enjoys. The check
 * is against the direction the last tick actually used, not the one already
 * queued, so two quick turns inside a single tick cannot add up to a reversal.
 */
fun GameState.turned(direction: Direction): GameState {
    if (status != GameStatus.RUNNING) return this
    if (direction == this.direction || direction == this.direction.opposite) return this
    return copy(pending = direction)
}

/**
 * The position one tick later.
 *
 * A finished game is left exactly as it is, wreck and all, so the screen can leave
 * it under the game-over panel instead of clearing the board out from under it.
 */
fun GameState.stepped(random: Random = Random.Default): GameState {
    if (status != GameStatus.RUNNING) return this

    val moved = copy(direction = pending)
    val head = moved.snake.first()
    val next = Cell(head.x + moved.direction.dx, head.y + moved.direction.dy)

    if (moved.isOutside(next)) {
        return moved.copy(status = GameStatus.OVER, reason = EndReason.HIT_WALL)
    }

    val ate = next == moved.food
    // On a tick that does not grow the snake the tail leaves its cell in the same
    // tick, so the head may legally take it.
    if (moved.occupies(next, ignoreTail = !ate)) {
        return moved.copy(status = GameStatus.OVER, reason = EndReason.HIT_SELF)
    }

    val grown = ArrayList<Cell>(moved.snake.size + 1)
    grown.add(next)
    grown.addAll(if (ate) moved.snake else moved.snake.subList(0, moved.snake.size - 1))

    return if (ate) moved.fed(grown, random) else moved.copy(snake = grown)
}

/** One point, one cell longer, and a pellet dealt somewhere still free. */
private fun GameState.fed(
    grown: List<Cell>,
    random: Random,
): GameState {
    val eaten = copy(snake = grown, score = score + 1)
    val pellet = eaten.placeFood(random)
    // No free cell left means every cell is snake. That is the perfect game, not
    // a failure.
    val status = if (pellet == null) GameStatus.WON else GameStatus.RUNNING
    return eaten.copy(food = pellet, status = status)
}

/**
 * A uniformly random free cell, or null when the snake fills the board.
 *
 * Choosing from the free cells rather than retrying random ones keeps the cost
 * bounded and stays fair on a nearly full board, where guessing would take ever
 * longer to stumble on the last gap.
 */
fun GameState.placeFood(random: Random): Cell? {
    val taken = snake.toHashSet()
    val free = ArrayList<Cell>(cols * rows - taken.size)
    for (y in 0 until rows) {
        for (x in 0 until cols) {
            val cell = Cell(x, y)
            if (cell !in taken) free.add(cell)
        }
    }
    return if (free.isEmpty()) null else free[random.nextInt(free.size)]
}

private fun GameState.isOutside(cell: Cell): Boolean = cell.x < 0 || cell.y < 0 || cell.x >= cols || cell.y >= rows
