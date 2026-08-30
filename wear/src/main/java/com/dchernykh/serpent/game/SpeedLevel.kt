package com.dchernykh.serpent.game

import androidx.annotation.StringRes
import com.dchernykh.serpent.R

/**
 * The difficulty levels and the pacing they produce. Pure, so the speed curve is
 * unit tested rather than guessed at from how the watch feels.
 *
 * Each level is a starting tick interval and the floor it may accelerate to. The
 * name is the storage key, so a level must never be renamed: a stored record
 * would lose its level and start again from nothing.
 */
enum class SpeedLevel(
    val baseMs: Long,
    val minMs: Long,
    @param:StringRes val labelRes: Int,
) {
    SLOW(baseMs = 340, minMs = 220, labelRes = R.string.speed_slow),
    NORMAL(baseMs = 240, minMs = 150, labelRes = R.string.speed_normal),
    FAST(baseMs = 160, minMs = 100, labelRes = R.string.speed_fast),
    ;

    /** The next level in the cycle, so one button walks through all of them. */
    val next: SpeedLevel get() = entries[(ordinal + 1) % entries.size]

    /**
     * How long to wait before the next tick, at this level and this score.
     *
     * The snake creeping faster as it grows is what turns a long game into a tense
     * one, so the interval sheds [RAMP_STEP_MS] for every [RAMP_EVERY_POINTS]
     * scored - and never drops below the level's floor, so however long a game
     * runs it stays playable.
     */
    fun tickInterval(score: Int): Long {
        val points = maxOf(0, score)
        val ramped = baseMs - (points / RAMP_EVERY_POINTS) * RAMP_STEP_MS
        return maxOf(minMs, ramped)
    }

    companion object {
        /** How many points earn one step of the ramp, and how much a step shaves off. */
        const val RAMP_EVERY_POINTS = 5
        const val RAMP_STEP_MS = 10L

        val DEFAULT = NORMAL

        /**
         * The level a stored name refers to, or the default.
         *
         * Storage can hand back nothing at all (a fresh install), or the name of a
         * level an older build had. Neither may leave the game without pacing, so
         * anything unrecognised reads as the default rather than as the first
         * level - which would silently move everyone to Slow.
         */
        fun fromStoredName(name: String?): SpeedLevel = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
