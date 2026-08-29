package com.dchernykh.serpent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.dchernykh.serpent.game.Cell
import com.dchernykh.serpent.game.Direction
import com.dchernykh.serpent.game.GameState
import com.dchernykh.serpent.game.GameStatus
import com.dchernykh.serpent.game.SpeedLevel
import com.dchernykh.serpent.game.newGame
import com.dchernykh.serpent.game.stepped
import com.dchernykh.serpent.game.turned
import com.dchernykh.serpent.game.updateBest
import com.dchernykh.serpent.store.RecordStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/** Which of the four screens is in front. */
enum class Screen { START, PLAYING, PAUSED, OVER }

/**
 * Everything the screen draws. The snake is a copy rather than the live body, so
 * Compose sees a new value on every tick and the game cannot mutate what is being
 * painted underneath it.
 */
data class SerpentUiState(
    val screen: Screen = Screen.START,
    val level: SpeedLevel = SpeedLevel.DEFAULT,
    val best: Int = 0,
    val score: Int = 0,
    val snake: List<Cell> = emptyList(),
    val food: Cell? = null,
    val isRecord: Boolean = false,
)

/**
 * The game as the screen sees it: one state to draw and a handful of things a
 * finger can do.
 *
 * The tick loop lives here rather than in the composition. A composition is
 * recreated whenever the watch feels like it - a configuration change, a theme
 * switch - and a game that restarted every time would be unplayable.
 */
class SerpentViewModel(
    private val store: RecordStore,
    private val boardCells: Int,
    private val random: Random = Random.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SerpentUiState())
    val uiState: StateFlow<SerpentUiState> = _uiState.asStateFlow()

    private var game: GameState? = null
    private var loop: Job? = null

    // Every touch of the settings goes through this, each waiting on the one
    // before. Without it the first read - which starts as soon as the view model
    // does - can finish after a tap that changed the level, and quietly put the
    // stored level back over the one the player just chose.
    private var settings: Job = Job().apply { complete() }

    init {
        // The game reopens the way it was left, which means reading the level
        // before anything can be drawn against the wrong record.
        settings =
            viewModelScope.launch {
                val level = store.readLevel()
                _uiState.update { it.copy(level = level, best = store.readBest(level)) }
            }
    }

    /** Walk to the next difficulty, remember it, and load that level's record. */
    fun cycleLevel() {
        val next = _uiState.value.level.next
        val previous = settings
        settings =
            viewModelScope.launch {
                previous.join()
                store.writeLevel(next)
                _uiState.update { it.copy(level = next, best = store.readBest(next)) }
            }
    }

    fun startGame() {
        val fresh = newGame(boardCells, boardCells, random)
        game = fresh
        _uiState.update {
            it.copy(
                screen = Screen.PLAYING,
                score = 0,
                snake = fresh.snake,
                food = fresh.food,
                isRecord = false,
            )
        }
        runLoop()
    }

    /** Steer. A turn the rules refuse simply does nothing. */
    fun turn(direction: Direction) {
        if (_uiState.value.screen != Screen.PLAYING) return
        game = game?.turned(direction)
    }

    fun pauseGame() {
        if (_uiState.value.screen != Screen.PLAYING) return
        stopLoop()
        _uiState.update { it.copy(screen = Screen.PAUSED) }
    }

    fun resumeGame() {
        if (_uiState.value.screen != Screen.PAUSED) return
        _uiState.update { it.copy(screen = Screen.PLAYING) }
        runLoop()
    }

    /** Leave the game and go back to the start screen, dropping the position. */
    fun showStart() {
        stopLoop()
        game = null
        // The board is cleared with it. Without this the wreck of the last game
        // stays on screen behind the start menu, which reads as the next game
        // having already begun.
        _uiState.update {
            it.copy(screen = Screen.START, isRecord = false, snake = emptyList(), food = null)
        }
    }

    override fun onCleared() {
        stopLoop()
        super.onCleared()
    }

    private fun runLoop() {
        stopLoop()
        loop =
            viewModelScope.launch {
                while (isActive) {
                    val before = game ?: return@launch
                    delay(_uiState.value.level.tickInterval(before.score))
                    // Checked after the delay, not before it: a pause during the
                    // wait must not be answered with a tick taken anyway, which
                    // would move the snake behind the pause panel.
                    if (_uiState.value.screen != Screen.PLAYING) return@launch
                    // The turn a finger made during the delay is on `game`, not on
                    // the copy this iteration started with, so the tick is taken
                    // from whatever is current at the moment it fires.
                    val after = (game ?: return@launch).stepped(random)
                    game = after
                    _uiState.update {
                        it.copy(score = after.score, snake = after.snake, food = after.food)
                    }
                    if (after.status != GameStatus.RUNNING) {
                        finish(after)
                        return@launch
                    }
                }
            }
    }

    private fun stopLoop() {
        loop?.cancel()
        loop = null
    }

    /**
     * The crash - or, on a full board, the perfect game - is left on screen under
     * the panel, so you can see what you ran into. A record is written only when
     * there is one, so an ordinary game never touches storage.
     */
    private suspend fun finish(finished: GameState) {
        val outcome = updateBest(_uiState.value.best, finished.score)
        if (outcome.isRecord) store.writeBest(_uiState.value.level, outcome.best)
        _uiState.update {
            it.copy(screen = Screen.OVER, best = outcome.best, isRecord = outcome.isRecord)
        }
    }

    companion object {
        /**
         * The board is square and its size is fixed by the layout, so the factory
         * takes it rather than letting the view model guess at a screen it cannot
         * see.
         */
        fun factory(
            store: RecordStore,
            boardCells: Int,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>,
                    extras: CreationExtras,
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return SerpentViewModel(store, boardCells) as T
                }
            }
    }
}
