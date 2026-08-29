package com.dchernykh.serpent.game

// The record decision, kept apart from the storage that holds it so the rule is
// unit tested. This file owns what counts as a record; the store owns the bytes.

/** A best score and whether the game just played is the one that set it. */
data class RecordOutcome(
    val best: Int,
    val isRecord: Boolean,
)

/**
 * A stored value coerced into a usable score.
 *
 * Storage can hand back a negative number left over from an older build, and none
 * of that may crash the game or reach the screen, so anything unusable reads as
 * nothing scored.
 */
fun normalizeScore(value: Int?): Int = value?.coerceAtLeast(0) ?: 0

/**
 * The best score after a finished game, and whether it is new.
 *
 * A game that scored nothing is never a record, so an accidental launch on a fresh
 * install cannot announce one.
 */
fun updateBest(
    previousBest: Int?,
    score: Int?,
): RecordOutcome {
    val best = normalizeScore(previousBest)
    val final = normalizeScore(score)
    return if (final > best && final > 0) {
        RecordOutcome(best = final, isRecord = true)
    } else {
        RecordOutcome(best = best, isRecord = false)
    }
}
