package com.dchernykh.serpent

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The one thing no JVM test can check: that the app actually starts on a watch.
 *
 * Launching the activity exercises the manifest, the theme, the launcher icon and
 * the whole Compose entry point at once, so a missing resource or a theme a watch
 * refuses shows up here rather than on someone's wrist.
 */
@RunWith(AndroidJUnit4::class)
class LaunchTest {
    @Test
    fun theActivityReachesResumed() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
