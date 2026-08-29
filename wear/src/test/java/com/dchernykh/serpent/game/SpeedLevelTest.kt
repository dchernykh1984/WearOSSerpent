package com.dchernykh.serpent.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedLevelTest {
    @Test
    fun `starts at its base interval`() {
        for (level in SpeedLevel.entries) {
            assertEquals(level.baseMs, level.tickInterval(0))
        }
    }

    @Test
    fun `quickens by one step for every five points`() {
        val level = SpeedLevel.SLOW

        assertEquals(level.baseMs, level.tickInterval(4))
        assertEquals(level.baseMs - SpeedLevel.RAMP_STEP_MS, level.tickInterval(5))
        assertEquals(level.baseMs - SpeedLevel.RAMP_STEP_MS, level.tickInterval(9))
        assertEquals(level.baseMs - 2 * SpeedLevel.RAMP_STEP_MS, level.tickInterval(10))
    }

    @Test
    fun `never drops below the floor, however long the game runs`() {
        for (level in SpeedLevel.entries) {
            assertEquals(level.minMs, level.tickInterval(10_000))
            assertTrue(level.tickInterval(1_000) >= level.minMs)
        }
    }

    @Test
    fun `treats a negative score as no score at all`() {
        assertEquals(SpeedLevel.FAST.baseMs, SpeedLevel.FAST.tickInterval(-20))
    }

    @Test
    fun `is ordered from slowest to fastest, with room to ramp`() {
        val ordered = SpeedLevel.entries.sortedByDescending { it.baseMs }
        assertEquals(SpeedLevel.entries.toList(), ordered)
        for (level in SpeedLevel.entries) {
            assertTrue("${level.name} cannot ramp", level.minMs < level.baseMs)
        }
    }

    @Test
    fun `cycles through every level and back`() {
        var level = SpeedLevel.entries.first()
        val walked = mutableListOf(level)
        repeat(SpeedLevel.entries.size - 1) {
            level = level.next
            walked.add(level)
        }

        assertEquals(SpeedLevel.entries.toList(), walked)
        assertEquals(SpeedLevel.entries.first(), level.next)
    }

    @Test
    fun `reads back a stored level`() {
        for (level in SpeedLevel.entries) {
            assertEquals(level, SpeedLevel.fromStoredName(level.name))
        }
    }

    @Test
    fun `falls back to the default rather than the first level`() {
        // Not the same thing: the first level is Slow, and a fresh install that
        // quietly started on Slow would be a bug nobody would ever report.
        assertNotEquals(SpeedLevel.entries.first(), SpeedLevel.DEFAULT)
        assertEquals(SpeedLevel.DEFAULT, SpeedLevel.fromStoredName(null))
        assertEquals(SpeedLevel.DEFAULT, SpeedLevel.fromStoredName(""))
        assertEquals(SpeedLevel.DEFAULT, SpeedLevel.fromStoredName("BLISTERING"))
    }

    @Test
    fun `gives every level its own label`() {
        val labels = SpeedLevel.entries.map { it.labelRes }.toSet()
        assertEquals(SpeedLevel.entries.size, labels.size)
    }
}
