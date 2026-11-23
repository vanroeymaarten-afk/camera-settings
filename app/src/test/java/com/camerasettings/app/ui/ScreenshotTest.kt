package com.camerasettings.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.camerasettings.app.App
import com.camerasettings.app.data.AppState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders the real screens to PNG files. Run with: ./gradlew testDebugUnitTest -Pscreenshots */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var state: AppState

    @Before
    fun setUp() {
        assumeTrue(System.getProperty("screenshots") == "true")
        state = AppState(ApplicationProvider.getApplicationContext())
        state.selectCamera("r50")
        state.toggleLens("rfs18-45")
        state.toggleLens("rfs55-210")
        state.selectPlace("sanpedro")
        compose.setContent { AppTheme { App(state) } }
    }

    private fun shot(name: String) = compose.onRoot().captureRoboImage("screenshots/$name.png")

    @Test fun scenes() = shot("1_scenes")

    @Test fun astro() {
        compose.onNodeWithText("Milky Way & stars").performClick()
        shot("2_astro")
    }

    @Test fun wildlife() {
        compose.onNodeWithText("Wildlife").performClick()
        shot("3_wildlife")
    }

    @Test fun meter() {
        compose.onNodeWithText("Meter").performClick()
        shot("4_meter")
    }

    @Test fun sky() {
        compose.onNodeWithText("Sun & Stars").performClick()
        shot("5_sky")
    }

    @Test fun gear() {
        compose.onNodeWithText("My gear").performClick()
        shot("6_gear")
    }
}
