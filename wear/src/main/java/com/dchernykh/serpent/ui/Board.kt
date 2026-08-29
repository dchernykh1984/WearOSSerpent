package com.dchernykh.serpent.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.dchernykh.serpent.game.Cell
import com.dchernykh.serpent.layout.BOARD_EDGE
import com.dchernykh.serpent.layout.Board
import com.dchernykh.serpent.layout.cellRect

/**
 * The board and everything on it, on one canvas.
 *
 * The Zepp OS original redrew only the three cells a tick could change, because
 * there each cell was a widget and repainting the board meant touching two hundred
 * of them. Compose has no such cost: the canvas is one draw pass either way, so
 * the whole board is painted from the state and there is no incremental update to
 * get subtly wrong.
 */
@Composable
fun BoardCanvas(
    board: Board,
    snake: List<Cell>,
    food: Cell?,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val edge = BOARD_EDGE.toFloat()
        drawRoundRect(
            color = ColorBoardEdge,
            topLeft = Offset(board.x - edge, board.y - edge),
            size = Size(board.size + 2 * edge, board.size + 2 * edge),
            cornerRadius = CornerRadius(edge * 3),
        )
        drawRoundRect(
            color = ColorBoard,
            topLeft = Offset(board.x.toFloat(), board.y.toFloat()),
            size = Size(board.size.toFloat(), board.size.toFloat()),
            cornerRadius = CornerRadius(edge * 2),
        )

        // Painted tail first so that the head, the one cell worth finding at a
        // glance, is drawn over its neighbour rather than under it.
        for (i in snake.indices.reversed()) {
            val rect = cellRect(board, snake[i].x, snake[i].y, CELL_INSET)
            drawRoundRect(
                color = if (i == 0) ColorSnakeHead else ColorSnake,
                topLeft = Offset(rect.x.toFloat(), rect.y.toFloat()),
                size = Size(rect.w.toFloat(), rect.h.toFloat()),
                cornerRadius = CornerRadius(edge),
            )
        }

        // A won game has filled every cell, so there is no pellet left to draw.
        food?.let {
            val rect = cellRect(board, it.x, it.y, CELL_INSET)
            drawCircle(
                color = ColorFood,
                radius = rect.w / 2f,
                center = Offset(rect.x + rect.w / 2f, rect.y + rect.h / 2f),
            )
        }
    }
}
