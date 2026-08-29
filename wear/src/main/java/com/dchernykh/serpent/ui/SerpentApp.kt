package com.dchernykh.serpent.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.dchernykh.serpent.R
import com.dchernykh.serpent.Screen
import com.dchernykh.serpent.SerpentViewModel
import com.dchernykh.serpent.game.Direction
import com.dchernykh.serpent.layout.Controls
import com.dchernykh.serpent.layout.GRID_CELLS
import com.dchernykh.serpent.layout.arrowMetrics
import com.dchernykh.serpent.layout.arrowStrokes
import com.dchernykh.serpent.layout.boardLayout
import com.dchernykh.serpent.layout.controlLayout
import com.dchernykh.serpent.layout.pauseStrokes
import kotlin.math.abs

/**
 * The whole screen: the board, the controls around it, and whichever menu is in
 * front.
 *
 * The layout is worked out once from the screen diameter and then everything is
 * placed at absolute pixels, which is what keeps the port looking like the game it
 * was ported from on any round watch.
 */
@Composable
fun SerpentApp(viewModel: SerpentViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The screen is a circle, so one diameter drives every measurement. Taking the
    // smaller side keeps that true even if a watch reports a pixel of slop between
    // width and height.
    val container = LocalWindowInfo.current.containerSize
    val screenSize = minOf(container.width, container.height)
    val board = remember(screenSize) { boardLayout(screenSize, GRID_CELLS) }
    val controls = remember(screenSize, board) { controlLayout(screenSize, board) }
    // The four arrows share one size so they read as a set rather than as four
    // unrelated marks, and pause is drawn at the same weight for the same reason.
    val metrics = remember(controls) { arrowMetrics(controls.arrows) }
    val menu = remember(board) { MenuMetrics(board) }

    KeepScreenOnWhile(state.screen == Screen.PLAYING)

    // Wear OS reads a swipe from the left edge as Back. During a game that must
    // not leave the app - a hard turn would end the run - so it pauses instead,
    // which is also the one thing a player pressing Back mid-game could want. From
    // a menu it steps back towards the start screen, and from the start screen it
    // is left alone so that the watch closes the app as it does any other.
    BackHandler(enabled = state.screen != Screen.START) { goBack(state.screen, viewModel) }

    MaterialTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(ColorBackground)
                    .swipes(state.screen) { steer(state.screen, it, viewModel) },
        ) {
            BoardCanvas(board = board, snake = state.snake, food = state.food, modifier = Modifier.fillMaxSize())

            if (state.screen == Screen.PLAYING) {
                Score(controls, state.score)
                SteeringControls(controls, metrics, viewModel)
            }

            Menus(screenSize, board, menu, state, viewModel)
        }
    }
}

/** What Back does, which depends on where it was pressed. */
private fun goBack(
    screen: Screen,
    viewModel: SerpentViewModel,
) {
    when (screen) {
        Screen.PLAYING -> viewModel.pauseGame()
        Screen.PAUSED, Screen.OVER -> viewModel.showStart()
        Screen.START -> Unit
    }
}

/**
 * What a swipe does, which also depends on where it was made: it steers during a
 * game, and up or down on the start screen walks the difficulty - the second way
 * the original offered to change it, and the one that needs no aiming.
 */
private fun steer(
    screen: Screen,
    direction: Direction,
    viewModel: SerpentViewModel,
) {
    when (screen) {
        Screen.PLAYING -> viewModel.turn(direction)
        Screen.START ->
            if (direction == Direction.UP || direction == Direction.DOWN) {
                viewModel.cycleLevel()
            }
        Screen.PAUSED, Screen.OVER -> Unit
    }
}

/**
 * A game outlasts the watch's display timeout by a wide margin, and a screen that
 * blacks out mid-run is a lost game. Only while a game is actually on: a menu left
 * open on the wrist has nothing worth burning the battery for.
 */
@Composable
private fun KeepScreenOnWhile(playing: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Swipes, over the whole screen: steering during a game, and the difficulty on the
 * start screen.
 *
 * A drag is read once, on the first movement that clears the touch slop, and the
 * longer axis decides: a swipe is never exactly straight, and waiting for the
 * finger to lift would turn the snake a moment too late to be any use.
 *
 * Keyed on [screen] so that [onSwipe] is read afresh whenever the screen changes.
 * A pointerInput block keyed on Unit would hold on to the very first one for the
 * life of the app, and every swipe would go on being handled as though the start
 * screen were still in front.
 */
private fun Modifier.swipes(
    screen: Screen,
    onSwipe: (Direction) -> Unit,
): Modifier =
    pointerInput(screen) {
        var handled = false
        detectDragGestures(
            onDragStart = { handled = false },
            onDragEnd = { handled = false },
            onDragCancel = { handled = false },
        ) { change, drag ->
            change.consume()
            if (!handled) {
                handled = true
                onSwipe(
                    if (abs(drag.x) > abs(drag.y)) {
                        if (drag.x > 0) Direction.RIGHT else Direction.LEFT
                    } else {
                        if (drag.y > 0) Direction.DOWN else Direction.UP
                    },
                )
            }
        }
    }

@Composable
private fun Score(
    controls: Controls,
    score: Int,
) {
    Box(modifier = Modifier.absoluteBox(controls.score), contentAlignment = Alignment.Center) {
        Text(
            text = score.toString(),
            color = ColorText,
            fontSize = with(LocalDensity.current) { (controls.score.h * 0.8f).toSp() },
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SteeringControls(
    controls: Controls,
    metrics: com.dchernykh.serpent.layout.ArrowMetrics,
    viewModel: SerpentViewModel,
) {
    val arrows =
        listOf(
            Triple(Direction.UP, controls.up, R.string.steer_up),
            Triple(Direction.DOWN, controls.down, R.string.steer_down),
            Triple(Direction.LEFT, controls.left, R.string.steer_left),
            Triple(Direction.RIGHT, controls.right, R.string.steer_right),
        )
    for ((direction, box, label) in arrows) {
        StrokeControl(
            box = box,
            strokes = arrowStrokes(direction, box, metrics),
            label = stringResource(label),
            onClick = { viewModel.turn(direction) },
        )
    }
    StrokeControl(
        box = controls.pause,
        strokes = pauseStrokes(controls.pause, metrics),
        label = stringResource(R.string.pause),
        onClick = viewModel::pauseGame,
    )
}
