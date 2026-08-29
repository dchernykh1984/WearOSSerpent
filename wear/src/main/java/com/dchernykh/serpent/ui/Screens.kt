package com.dchernykh.serpent.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dchernykh.serpent.R
import com.dchernykh.serpent.Screen
import com.dchernykh.serpent.SerpentUiState
import com.dchernykh.serpent.SerpentViewModel
import com.dchernykh.serpent.layout.Board

// The three menus and the choice between them. They live one file away from the
// shell that hosts them because they are what changes when the game gains a
// screen, and the shell is what does not.

/** Whichever menu is in front, or none at all while a game is on. */
@Composable
internal fun Menus(
    screenSize: Int,
    board: Board,
    menu: MenuMetrics,
    state: SerpentUiState,
    viewModel: SerpentViewModel,
) {
    when (state.screen) {
        Screen.PLAYING -> Unit
        Screen.START -> StartMenu(screenSize, board, menu, state, viewModel)
        Screen.PAUSED -> PausedMenu(screenSize, board, menu, viewModel)
        Screen.OVER -> GameOverMenu(screenSize, board, menu, state, viewModel)
    }
}

@Composable
private fun StartMenu(
    screenSize: Int,
    board: Board,
    menu: MenuMetrics,
    state: SerpentUiState,
    viewModel: SerpentViewModel,
) {
    MenuOverlay(
        screenSize = screenSize,
        board = board,
        metrics = menu,
        items =
            listOf(
                MenuItem.Line(menu.big, ColorText, stringResource(R.string.title)),
                MenuItem.Gap(menu.gap),
                MenuItem.Line(menu.row, ColorMuted, stringResource(R.string.best_value, state.best)),
                MenuItem.Gap(menu.gap),
                MenuItem.Line(menu.small, ColorMuted, stringResource(R.string.speed)),
                MenuItem.Action(menu.button, stringResource(state.level.labelRes), viewModel::cycleLevel),
                MenuItem.Gap(menu.gap),
                MenuItem.Action(menu.button, stringResource(R.string.play), viewModel::startGame),
                MenuItem.Line(menu.small, ColorMuted, stringResource(R.string.hint)),
            ),
    )
}

@Composable
private fun PausedMenu(
    screenSize: Int,
    board: Board,
    menu: MenuMetrics,
    viewModel: SerpentViewModel,
) {
    MenuOverlay(
        screenSize = screenSize,
        board = board,
        metrics = menu,
        items =
            listOf(
                MenuItem.Line(menu.big, ColorText, stringResource(R.string.paused)),
                MenuItem.Gap(menu.gap),
                MenuItem.Action(menu.button, stringResource(R.string.resume), viewModel::resumeGame),
            ),
    )
}

@Composable
private fun GameOverMenu(
    screenSize: Int,
    board: Board,
    menu: MenuMetrics,
    state: SerpentUiState,
    viewModel: SerpentViewModel,
) {
    MenuOverlay(
        screenSize = screenSize,
        board = board,
        metrics = menu,
        items =
            listOf(
                MenuItem.Line(menu.big, ColorText, stringResource(R.string.game_over)),
                MenuItem.Gap(menu.gap),
                MenuItem.Line(menu.row, ColorText, stringResource(R.string.score_value, state.score)),
                MenuItem.Line(
                    menu.row,
                    if (state.isRecord) ColorFood else ColorMuted,
                    if (state.isRecord) {
                        stringResource(R.string.new_best)
                    } else {
                        stringResource(R.string.best_value, state.best)
                    },
                ),
                MenuItem.Gap(menu.gap),
                MenuItem.Action(menu.button, stringResource(R.string.again), viewModel::showStart),
            ),
    )
}
