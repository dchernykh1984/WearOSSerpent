package com.dchernykh.serpent.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NormalizeScoreTest {
    @Test
    fun `keeps a real score`() {
        assertEquals(7, normalizeScore(7))
        assertEquals(0, normalizeScore(0))
    }

    @Test
    fun `reads nothing stored as nothing scored`() {
        assertEquals(0, normalizeScore(null))
    }

    @Test
    fun `refuses a negative left over from an older build`() {
        assertEquals(0, normalizeScore(-3))
    }
}

class UpdateBestTest {
    @Test
    fun `records a score that beats the previous best`() {
        val outcome = updateBest(previousBest = 4, score = 9)

        assertEquals(9, outcome.best)
        assertTrue(outcome.isRecord)
    }

    @Test
    fun `keeps the previous best when the game fell short`() {
        val outcome = updateBest(previousBest = 9, score = 4)

        assertEquals(9, outcome.best)
        assertFalse(outcome.isRecord)
    }

    @Test
    fun `does not call an equal score a record`() {
        val outcome = updateBest(previousBest = 9, score = 9)

        assertEquals(9, outcome.best)
        assertFalse(outcome.isRecord)
    }

    @Test
    fun `never announces a record for a game that scored nothing`() {
        // The case that matters: a fresh install where an accidental launch and an
        // immediate crash would otherwise be celebrated.
        val outcome = updateBest(previousBest = 0, score = 0)

        assertEquals(0, outcome.best)
        assertFalse(outcome.isRecord)
    }

    @Test
    fun `takes the first real score as a record`() {
        val outcome = updateBest(previousBest = null, score = 1)

        assertEquals(1, outcome.best)
        assertTrue(outcome.isRecord)
    }
}
