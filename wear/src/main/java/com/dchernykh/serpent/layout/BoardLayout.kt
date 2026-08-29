package com.dchernykh.serpent.layout

import kotlin.math.roundToInt
import kotlin.math.sqrt

// Where the playing grid sits on a round screen, and where one cell of it lands in
// pixels. Pure, so it is unit tested; the screen turns these boxes into paint.

/**
 * The board: [cell] pixels a side, [cells] of them across, occupying a [size]
 * square whose top left corner is at ([x], [y]).
 */
data class Board(
    val cell: Int,
    val size: Int,
    val x: Int,
    val y: Int,
    val cells: Int,
)

/**
 * The playing field is the largest axis-aligned square that fits inside the round
 * screen (a side of diameter / sqrt 2), shrunk to a whole number of equal cells so
 * that no cell is a pixel wider than its neighbour. Centring that square leaves a
 * circular cap above and below it, which is where the score and the controls go.
 */
fun boardLayout(
    screenSize: Int,
    cells: Int,
): Board {
    val columns = maxOf(1, cells)
    val inscribed = (screenSize / sqrt(2f)).toInt()
    val cell = maxOf(1, inscribed / columns)
    val size = cell * columns
    val origin = ((screenSize - size) / 2f).roundToInt()
    return Board(cell = cell, size = size, x = origin, y = origin, cells = columns)
}

/**
 * The pixel box of one grid cell, inset on every side so that neighbours read as
 * separate segments rather than as one solid bar. The inset is capped at a third
 * of the cell, so a small cell can never collapse to nothing.
 */
fun cellRect(
    board: Board,
    column: Int,
    row: Int,
    inset: Int,
): Box {
    val gap = inset.coerceIn(0, board.cell / 3)
    return Box(
        x = board.x + column * board.cell + gap,
        y = board.y + row * board.cell + gap,
        w = board.cell - 2 * gap,
        h = board.cell - 2 * gap,
    )
}
