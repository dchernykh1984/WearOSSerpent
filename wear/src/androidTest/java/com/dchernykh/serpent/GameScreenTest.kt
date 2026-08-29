package com.dchernykh.serpent

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

/**
 * What no JVM test can check: that the game actually runs on a watch.
 *
 * Launching the activity exercises the manifest, the theme, the launcher icon, the
 * whole Compose tree and the DataStore-backed record store in one go - the parts
 * excused from the coverage floor precisely because they need a device. The rules
 * themselves are covered far more cheaply by the unit tests, so this walks the
 * screens rather than trying to play.
 *
 * Every label is read from the resources rather than written out, so the test says
 * the same thing on a watch set to any of the eleven languages.
 */
class GameScreenTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int) = rule.activity.getString(id)

    private fun onScreen(label: String) = rule.onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty()

    private val speedLabels
        get() = listOf(R.string.speed_slow, R.string.speed_normal, R.string.speed_fast).map(::text)

    @Test
    fun opensOnTheStartScreen() {
        rule.onNodeWithText(text(R.string.title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.play)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.hint)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.speed)).assertIsDisplayed()
    }

    @Test
    fun walksTheWholeDifficultyCycleAndComesBack() {
        // The button shows the level it will play, which is the only way to tell
        // the setting took. Whichever level the watch was left on, one tap per
        // level walks the cycle right round and lands back on it.
        rule.waitUntil { speedLabels.any(::onScreen) }
        val start = speedLabels.first(::onScreen)

        repeat(speedLabels.size) {
            val current = speedLabels.first(::onScreen)
            rule.onNodeWithText(current).performClick()
            // The level is written to storage before it is shown, so the change
            // arrives a coroutine later than the tap.
            rule.waitUntil { !onScreen(current) }
        }

        rule.onNodeWithText(start).assertIsDisplayed()
    }

    @Test
    fun startsAGameAndShowsTheSteeringControls() {
        rule.onNodeWithText(text(R.string.play)).performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription(text(R.string.steer_up)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.steer_down)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.steer_left)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.steer_right)).assertIsDisplayed()
        rule.onNodeWithContentDescription(text(R.string.pause)).assertIsDisplayed()
    }

    @Test
    fun pausesAndResumesAGame() {
        rule.onNodeWithText(text(R.string.play)).performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription(text(R.string.pause)).performClick()
        rule.onNodeWithText(text(R.string.paused)).assertIsDisplayed()

        rule.onNodeWithText(text(R.string.resume)).performClick()
        rule.waitUntil { !onScreen(text(R.string.paused)) }
        rule.onNodeWithContentDescription(text(R.string.pause)).assertIsDisplayed()
    }
}
