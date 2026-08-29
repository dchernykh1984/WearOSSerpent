package com.dchernykh.serpent

import com.dchernykh.serpent.game.Direction
import com.dchernykh.serpent.game.SpeedLevel
import com.dchernykh.serpent.store.RecordStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/** An in-memory stand-in for the watch's storage. */
private class FakeRecordStore(
    var level: SpeedLevel = SpeedLevel.DEFAULT,
    private val bests: MutableMap<SpeedLevel, Int> = mutableMapOf(),
) : RecordStore {
    var writes = 0
        private set

    override suspend fun readLevel(): SpeedLevel = level

    override suspend fun writeLevel(level: SpeedLevel) {
        this.level = level
    }

    override suspend fun readBest(level: SpeedLevel): Int = bests[level] ?: 0

    override suspend fun writeBest(
        level: SpeedLevel,
        best: Int,
    ) {
        bests[level] = best
        writes++
    }
}

/**
 * A source of random numbers that names the free cell each pellet goes on, so a
 * test can put one directly in front of the snake and play a scoring game. Free
 * cells are offered in row-major order; once the script runs out every pellet goes
 * on the first free cell, which keeps the rest of a game deterministic without
 * listing it.
 */
private class ScriptedRandom(
    private vararg val picks: Int,
) : Random() {
    private var next = 0

    override fun nextBits(bitCount: Int): Int = 0

    override fun nextInt(until: Int): Int {
        val pick = if (next < picks.size) picks[next] else 0
        next++
        return pick.coerceIn(0, until - 1)
    }
}

private const val BOARD = 15

/**
 * The free-cell index of (8, 7) on a fresh 15x15 board: the cell the head moves
 * into on the very first tick. Row-major position 113, less the three body cells
 * that come before it.
 */
private const val FIRST_CELL_AHEAD = 110

@OptIn(ExperimentalCoroutinesApi::class)
class SerpentViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope is pinned to the main dispatcher, so the tick loop only
        // runs under a test dispatcher that stands in for it. The virtual clock is
        // what lets a whole game finish in milliseconds.
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        store: RecordStore = FakeRecordStore(),
        seed: Int = 0,
    ) = SerpentViewModel(store, BOARD, Random(seed))

    @Test
    fun `opens on the start screen, at the level and record it was left on`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = SpeedLevel.FAST)
            store.writeBest(SpeedLevel.FAST, 12)
            val model = viewModel(store)

            advanceUntilIdle()

            assertEquals(Screen.START, model.uiState.value.screen)
            assertEquals(SpeedLevel.FAST, model.uiState.value.level)
            assertEquals(12, model.uiState.value.best)
        }

    @Test
    fun `walks to the next level, remembers it, and shows that level's record`() =
        runTest(dispatcher) {
            val store = FakeRecordStore(level = SpeedLevel.SLOW)
            store.writeBest(SpeedLevel.NORMAL, 30)
            val model = viewModel(store)
            advanceUntilIdle()

            model.cycleLevel()
            advanceUntilIdle()

            assertEquals(SpeedLevel.NORMAL, model.uiState.value.level)
            assertEquals(SpeedLevel.NORMAL, store.level)
            assertEquals(30, model.uiState.value.best)
        }

    @Test
    fun `deals a fresh board when a game starts`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()

            model.startGame()

            val state = model.uiState.value
            assertEquals(Screen.PLAYING, state.screen)
            assertEquals(0, state.score)
            assertEquals(3, state.snake.size)
            assertNotEquals(null, state.food)
        }

    @Test
    fun `moves the snake on every tick`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val head =
                model.uiState.value.snake
                    .first()

            advanceTimeBy(SpeedLevel.DEFAULT.baseMs + 1)

            assertNotEquals(
                head,
                model.uiState.value.snake
                    .first(),
            )
        }

    @Test
    fun `steers, and the next tick takes the turn`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            val head =
                model.uiState.value.snake
                    .first()

            model.turn(Direction.UP)
            advanceTimeBy(SpeedLevel.DEFAULT.baseMs + 1)

            assertEquals(
                head.y - 1,
                model.uiState.value.snake
                    .first()
                    .y,
            )
            assertEquals(
                head.x,
                model.uiState.value.snake
                    .first()
                    .x,
            )
        }

    @Test
    fun `paces the game by the level that was chosen`() =
        runTest(dispatcher) {
            // The whole point of the difficulty setting: on Fast the snake has
            // already moved by the time Normal would still be waiting.
            val store = FakeRecordStore(level = SpeedLevel.FAST)
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()
            val head =
                model.uiState.value.snake
                    .first()

            advanceTimeBy(SpeedLevel.FAST.baseMs + 1)
            val movedOnFast =
                model.uiState.value.snake
                    .first()

            assertNotEquals(head, movedOnFast)
            assertTrue(SpeedLevel.FAST.baseMs < SpeedLevel.NORMAL.baseMs)
        }

    @Test
    fun `ignores steering when no game is on`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()

            model.turn(Direction.UP)

            assertEquals(Screen.START, model.uiState.value.screen)
        }

    @Test
    fun `stops the clock while paused and starts it again on resume`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            advanceTimeBy(SpeedLevel.DEFAULT.baseMs + 1)
            model.pauseGame()
            val frozen = model.uiState.value.snake

            advanceTimeBy(SpeedLevel.DEFAULT.baseMs * 5)

            assertEquals(Screen.PAUSED, model.uiState.value.screen)
            assertEquals(frozen, model.uiState.value.snake)

            model.resumeGame()
            advanceTimeBy(SpeedLevel.DEFAULT.baseMs + 1)

            assertEquals(Screen.PLAYING, model.uiState.value.screen)
            assertNotEquals(frozen, model.uiState.value.snake)
        }

    @Test
    fun `refuses to pause or resume out of turn`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()

            model.pauseGame()
            assertEquals(Screen.START, model.uiState.value.screen)

            model.resumeGame()
            assertEquals(Screen.START, model.uiState.value.screen)
        }

    @Test
    fun `ends the game at the wall and leaves the wreck on screen`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()

            // Straight ahead from the middle of the board into the right-hand wall.
            advanceTimeBy(SpeedLevel.DEFAULT.baseMs * (BOARD + 2))

            assertEquals(Screen.OVER, model.uiState.value.screen)
            assertTrue(
                model.uiState.value.snake
                    .isNotEmpty(),
            )
        }

    @Test
    fun `writes nothing to storage for a game that scored nothing`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()

            advanceTimeBy(SpeedLevel.DEFAULT.baseMs * (BOARD + 2))

            assertEquals(Screen.OVER, model.uiState.value.screen)
            assertEquals(0, model.uiState.value.score)
            assertFalse(model.uiState.value.isRecord)
            assertEquals(0, store.writes)
        }

    @Test
    fun `goes back to the start screen and drops the position`() =
        runTest(dispatcher) {
            val model = viewModel()
            advanceUntilIdle()
            model.startGame()
            advanceTimeBy(SpeedLevel.DEFAULT.baseMs + 1)

            model.showStart()
            advanceTimeBy(SpeedLevel.DEFAULT.baseMs * 5)

            assertEquals(Screen.START, model.uiState.value.screen)
            assertFalse(model.uiState.value.isRecord)
            // The board goes with it, or the wreck of the last game sits behind
            // the start menu looking like a game already in progress.
            assertTrue(
                model.uiState.value.snake
                    .isEmpty(),
            )
            assertEquals(null, model.uiState.value.food)
        }

    @Test
    fun `scores, announces the record and writes it exactly once`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            val model = SerpentViewModel(store, BOARD, ScriptedRandom(FIRST_CELL_AHEAD))
            advanceUntilIdle()
            model.startGame()

            advanceTimeBy(SpeedLevel.DEFAULT.baseMs * (BOARD + 2))

            assertEquals(Screen.OVER, model.uiState.value.screen)
            assertEquals(1, model.uiState.value.score)
            assertTrue(model.uiState.value.isRecord)
            assertEquals(1, model.uiState.value.best)
            assertEquals(1, store.writes)
            assertEquals(1, store.readBest(SpeedLevel.DEFAULT))
        }

    @Test
    fun `keeps the old record when a later game falls short`() =
        runTest(dispatcher) {
            val store = FakeRecordStore()
            store.writeBest(SpeedLevel.DEFAULT, 5)
            val model = viewModel(store)
            advanceUntilIdle()
            model.startGame()

            advanceTimeBy(SpeedLevel.DEFAULT.baseMs * (BOARD + 2))

            assertFalse(model.uiState.value.isRecord)
            assertEquals(5, model.uiState.value.best)
            assertEquals(1, store.writes)
        }
}
