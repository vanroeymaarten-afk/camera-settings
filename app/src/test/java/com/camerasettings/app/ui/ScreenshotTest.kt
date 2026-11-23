package com.camerasettings.app.ui

import android.widget.ImageView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.core.app.ApplicationProvider
import com.camerasettings.app.App
import com.camerasettings.app.R
import com.camerasettings.app.Tab
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
@Config(sdk = [34], qualifiers = "w400dp-h880dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var state: AppState

    @Before
    fun setUp() {
        assumeTrue(System.getProperty("screenshots") == "true")
        state = AppState(ApplicationProvider.getApplicationContext())
    }

    private fun withGear() {
        state.selectCamera("r50")
        if ("rfs18-45" !in state.lensIds) state.toggleLens("rfs18-45")
        if ("rfs55-210" !in state.lensIds) state.toggleLens("rfs55-210")
        state.selectPlace("sanpedro")
    }

    private fun show(name: String, tab: Tab = Tab.SHOOT, shot: String? = null, more: String? = null, scene: String? = null) {
        compose.setContent { AppTheme { App(state, tab, shot, more, scene) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun a_welcome() = show("01_welcome")
    @Test fun b_shoot() { withGear(); show("02_shoot") }
    @Test fun c_sceneAstro() { withGear(); show("03_scene_astro", scene = "astro") }
    @Test fun d_guide() { withGear(); show("04_shot_guide", Tab.GUIDE) }
    @Test fun e_shotOsorno() { withGear(); show("05_shot_osorno", Tab.GUIDE, shot = "osorno") }
    @Test fun f_sky() { withGear(); show("06_sun_and_stars", Tab.SKY) }
    @Test fun f2_starCalendar() {
        withGear()
        compose.setContent { AppTheme { App(state, Tab.SKY) } }
        compose.waitUntil(20_000) { compose.onAllNodesWithText("Star calendar", substring = true).fetchSemanticsNodes().isNotEmpty() }
        compose.waitUntil(60_000) { compose.onAllNodesWithText("Calculating…").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("Longest shutter", substring = true).performScrollTo()
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/06b_star_calendar.png")
    }
    @Test fun g_meter() { withGear(); show("07_meter", Tab.METER) }
    @Test fun h_more() { withGear(); show("08_more", Tab.MORE) }
    @Test fun i_files() { withGear(); show("09_files", Tab.MORE, more = MoreRoutes.FILES) }
    @Test fun j_editing() { withGear(); show("10_editing", Tab.MORE, more = MoreRoutes.EDITING) }
    @Test fun k_preset() { withGear(); show("11_preset", Tab.MORE, more = MoreRoutes.preset("atacama_gold")) }
    @Test fun l_checklist() { withGear(); show("12_checklist", Tab.MORE, more = MoreRoutes.CHECKLIST) }
    @Test fun m_gear() { withGear(); show("13_gear", Tab.MORE, more = MoreRoutes.GEAR) }

    @Test
    fun z_icon() {
        compose.setContent {
            Row(Modifier.background(Color(0xFFEDEAE6)).padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                listOf(R.mipmap.ic_launcher, R.drawable.ic_launcher_monochrome).forEach { res ->
                    Box(Modifier.size(160.dp).background(if (res == R.mipmap.ic_launcher) Color.Transparent else Color(0xFF3B4252))) {
                        AndroidView({ ctx -> ImageView(ctx).apply { setImageResource(res) } }, Modifier.size(160.dp))
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/00_icon.png")
    }
}
