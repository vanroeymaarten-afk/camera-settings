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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.core.app.ApplicationProvider
import com.camerasettings.app.App
import com.camerasettings.app.R
import com.camerasettings.app.Tab
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Level
import com.camerasettings.app.data.Place
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

    private fun withGear(brand: String = "r50", lenses: List<String> = listOf("rfs18-45", "rfs55-210")) {
        state.selectCamera(brand)
        lenses.forEach { if (it !in state.lensIds) state.toggleLens(it) }
        state.selectLevel(Level.NEW)
        state.selectPlace(Place("Brussels", "Belgium", 50.8505, 4.3488, "Europe/Brussels", 1019022))
        state.finishOnboarding()
    }

    private fun show(
        name: String, tab: Tab = Tab.HOME, scene: String? = null, learn: String? = null,
        photo: String? = null, more: String? = null, guide: Boolean = false, scale: Float = 1f,
    ) {
        compose.setContent { AppTheme(scale) { App(state, tab, scene, learn, photo, more, guide) } }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("screenshots/$name.png")
    }

    @Test fun a_welcome() = show("01_onboarding_welcome")
    @Test fun b_home() { withGear(); show("02_home") }
    @Test fun c_scene() { withGear(); show("03_scene_landscape", scene = "landscape") }
    @Test fun d_guide() { withGear(); show("04_step_by_step_guide", scene = "landscape", guide = true) }
    @Test fun e2_learnTab() { withGear(); show("05_learn", Tab.LEARN) }
    @Test fun f_lesson() { withGear(); show("06_lesson_aperture", Tab.LEARN, learn = "lesson:aperture") }
    @Test fun g_playground() { withGear(); show("07_exposure_playground", Tab.LEARN, learn = "playground") }
    @Test fun h_problem() { withGear(); show("08_problem_blurry", Tab.LEARN, learn = "problem:blurry") }
    @Test fun i_gallery() { withGear(); show("09_gallery", Tab.GALLERY) }
    @Test fun j_photo() { withGear(); show("10_photo_detail", Tab.GALLERY, photo = "water_2") }
    @Test fun k_plan() { withGear(); show("11_plan", Tab.PLAN) }
    @Test fun l_more() { withGear(); show("12_more", Tab.MORE) }
    @Test fun m_settings() { withGear(); show("13_settings", Tab.MORE, more = MoreRoutes.SETTINGS) }
    @Test fun n_nikon() { withGear("z50", listOf("nzdx16-50")); show("14_scene_nikon_wildlife", scene = "wildlife") }
    @Test fun o_bigText() { withGear(); show("15_home_large_text", scale = 1.3f) }
    @Test fun q_manual() { withGear("750d", listOf("efs18-55", "tamron18-270")); show("17_camera_manual", Tab.MORE, more = MoreRoutes.MANUAL) }
    @Test fun p_place() { withGear(); show("16_place_picker", Tab.MORE, more = MoreRoutes.PLACE) }

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
