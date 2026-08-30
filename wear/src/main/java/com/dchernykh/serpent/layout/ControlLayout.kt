package com.dchernykh.serpent.layout

import kotlin.math.roundToInt

// Where the on-screen controls sit on a round screen.
//
// The board is the square inscribed in the circle, which leaves four segments
// around it - one at each edge, thick in the middle and tapering to nothing at the
// corners. On a round watch those segments are dead space, and they are exactly
// where the four direction arrows go: the board keeps its whole area, and steering
// never covers the snake.
//
// The top segment carries the score above the up arrow. The bottom one carries the
// down arrow with the pause control beside it.
//
// All of it is pure arithmetic, so a test can ask where a control lands without a
// watch in the room.

/** The thickness of the frame drawn around the board. */
const val BOARD_EDGE = 3

/** How much clearance every centred box keeps from the bezel. */
const val SCREEN_PADDING = 8

// How much of a segment a control fills. The arrows are generous - they are the
// thing being aimed at constantly - and everything is kept clear of the bezel.
private const val ARROW_SPAN = 0.72f
private const val ARROW_HEIGHT = 0.34f

// How the top segment is split between the score and the up arrow. The score does
// not start at the very top: a round screen is barely seventy pixels across up
// there, and a couple of digits want more than that.
private const val SCORE_TOP = 0.14f
private const val SCORE_FILL = 0.35f
private const val UP_FILL = 0.5f

// The bottom row sits just under the board rather than centred in its segment. A
// round screen narrows fast towards the bottom: centred, the row would be only as
// wide as the chord at its lowest edge. Moved up against the board it has far more
// width to share, and that width is what buys the gap between the two controls.
//
// One pixel clear of the frame, so tightening the frame cannot slide the row
// underneath it.
private const val ROW_TOP_GAP = BOARD_EDGE + 1
private const val ROW_FILL = 0.55f

// The row is not two equal buttons. The down arrow is steering, pressed
// constantly; pause is pressed once or twice a game and is an unwelcome surprise
// mid-run. So the arrow is centred under the board and much the wider of the two,
// pause is pushed out to the end, and what is left between them belongs to nobody:
// a thumb landing wide of the arrow does nothing at all rather than stopping the
// game it was steering.
private const val DOWN_SHARE = 0.4f
private const val PAUSE_SHARE = 0.24f

/** Every box the play screen draws into. */
data class Controls(
    val board: Box,
    val score: Box,
    val up: Box,
    val down: Box,
    val pause: Box,
    val left: Box,
    val right: Box,
) {
    /** The four steering boxes, which share one arrow size so they read as a set. */
    val arrows: List<Box> get() = listOf(up, down, left, right)
}

private fun box(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
) = Box(x.roundToInt(), y.roundToInt(), w.roundToInt(), h.roundToInt())

/** Every control, given the screen size and where the board sits on it. */
fun controlLayout(
    screenSize: Int,
    board: Board,
): Controls {
    val top = board.y
    val bottom = screenSize - (board.y + board.size)
    val middle = board.y + board.size / 2f

    // The board is centred to the nearest whole pixel, so on some screens its left
    // and right margins differ by one. The narrower of the two sizes both arrows,
    // and the right one is placed as the mirror image of the left about the middle
    // of the SCREEN - centring each in its own margin would leave the pair visibly
    // off-centre by that pixel.
    val side = minOf(board.x, screenSize - (board.x + board.size))
    val armWidth = (side * ARROW_SPAN).roundToInt()
    val armHeight = (board.size * ARROW_HEIGHT).roundToInt()
    val armLeft = ((side - armWidth) / 2f).roundToInt()

    val scoreHeight = (top * SCORE_FILL).roundToInt()
    val scoreTop = (top * SCORE_TOP).roundToInt()
    val scoreRow = centeredBox(screenSize, scoreTop, scoreHeight, board.size.toFloat(), SCREEN_PADDING)

    // The up arrow stops clear of the frame around the board, so that painting it
    // can never rub the frame's top edge out.
    val upTop = scoreTop + scoreHeight
    val upHeight = minOf((top * UP_FILL).roundToInt(), top - BOARD_EDGE - 1 - upTop)
    val upRow = centeredBox(screenSize, upTop, upHeight, board.size * 0.5f, SCREEN_PADDING)

    val rowHeight = (bottom * ROW_FILL).roundToInt()
    val rowTop = board.y + board.size + ROW_TOP_GAP
    val row = centeredBox(screenSize, rowTop, rowHeight, board.size.toFloat(), SCREEN_PADDING)
    val downWidth = (row.w * DOWN_SHARE).roundToInt()
    val pauseWidth = (row.w * PAUSE_SHARE).roundToInt()

    return Controls(
        board = Box(board.x, board.y, board.size, board.size),
        score = scoreRow,
        up = upRow,
        down = box(row.x + (row.w - downWidth) / 2f, row.y.toFloat(), downWidth.toFloat(), row.h.toFloat()),
        pause = box((row.x + row.w - pauseWidth).toFloat(), row.y.toFloat(), pauseWidth.toFloat(), row.h.toFloat()),
        left = box(armLeft.toFloat(), middle - armHeight / 2f, armWidth.toFloat(), armHeight.toFloat()),
        right =
            box(
                (screenSize - armLeft - armWidth).toFloat(),
                middle - armHeight / 2f,
                armWidth.toFloat(),
                armHeight.toFloat(),
            ),
    )
}
