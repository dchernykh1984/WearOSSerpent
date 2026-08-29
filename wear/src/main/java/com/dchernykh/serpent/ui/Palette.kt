package com.dchernykh.serpent.ui

import androidx.compose.ui.graphics.Color

// The colours and the two board constants, carried over unchanged from the Zepp OS
// original so the two versions of the game look like the same game.
//
// Everything is drawn on a black screen: on the OLED a watch uses, black is not a
// colour but pixels that are switched off, which is why a watch game is dark and
// not merely fashionably so.

/** Cells across the inscribed square. Fifteen gives roughly 21px cells at 466. */
const val GRID_CELLS = 15

/** Pixels trimmed off each side of a cell so the body reads as segments. */
const val CELL_INSET = 1

val ColorBackground = Color(0xFF000000)
val ColorBoard = Color(0xFF0C1013)
val ColorBoardEdge = Color(0xFF2B3339)
val ColorSnake = Color(0xFF2FBF71)
val ColorSnakeHead = Color(0xFF8FF0B4)
val ColorFood = Color(0xFFFF5A3C)
val ColorText = Color(0xFFFFFFFF)
val ColorMuted = Color(0xFF9AA4AB)
val ColorButton = Color(0xFF1D262C)
val ColorButtonPressed = Color(0xFF2F3D46)

// The steering arrows and the pause icon, drawn in the segments around the board.
// Bright enough to aim at without competing with the snake for attention, and a
// lift on press so a tap is visibly registered.
val ColorArrow = Color(0xFF7F8C96)
val ColorArrowPressed = Color(0xFFE8F0F5)
