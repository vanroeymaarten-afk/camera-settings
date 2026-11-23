package com.camerasettings.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.World
import com.camerasettings.app.logic.Scenes
import com.camerasettings.app.ui.AppTheme
import com.camerasettings.app.ui.GalleryScreen
import com.camerasettings.app.ui.GuideStepper
import com.camerasettings.app.ui.HomeScreen
import com.camerasettings.app.ui.LearnScreen
import com.camerasettings.app.ui.MoreRoutes
import com.camerasettings.app.ui.MoreScreen
import com.camerasettings.app.ui.Onboarding
import com.camerasettings.app.ui.PlacePicker
import com.camerasettings.app.ui.SceneScreen
import com.camerasettings.app.ui.SkyScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val state = AppState(applicationContext)
        setContent {
            AppTheme(textScale = state.textScale) { App(state) }
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    LEARN("Learn", Icons.Filled.School),
    GALLERY("Gallery", Icons.Filled.PhotoLibrary),
    PLAN("Plan", Icons.Filled.WbTwilight),
    MORE("More", Icons.Filled.MoreHoriz),
}

/** Start arguments are used by the screenshot tests to open a specific screen. */
@Composable
fun App(
    state: AppState,
    startTab: Tab = Tab.HOME,
    startScene: String? = null,
    startLearn: String? = null,
    startPhoto: String? = null,
    startMore: String? = null,
    startGuide: Boolean = false,
) {
    val ctx = LocalContext.current
    var tab by rememberSaveable { mutableStateOf(startTab) }
    var scene by rememberSaveable { mutableStateOf(startScene) }
    var guide by rememberSaveable { mutableStateOf(startGuide) }
    var learn by rememberSaveable { mutableStateOf(startLearn) }
    var photo by rememberSaveable { mutableStateOf(startPhoto) }
    var more by rememberSaveable { mutableStateOf(startMore) }
    var planPicker by rememberSaveable { mutableStateOf(false) }

    // Default location: the biggest city in the phone's time zone.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { World.load(ctx.applicationContext) }
        if (state.place == null) World.defaultFor(ZoneId.systemDefault())?.let { state.selectPlace(it) }
    }

    if (!state.onboarded) {
        Onboarding(state) { tab = Tab.HOME }
        return
    }

    val sceneObj = Scenes.byId(scene)
    if (guide && sceneObj != null) {
        BackHandler { guide = false }
        GuideStepper(state, sceneObj) { guide = false }
        return
    }

    val openGear = { tab = Tab.MORE; more = MoreRoutes.GEAR }
    val openScene = { id: String -> scene = id; tab = Tab.HOME }

    BackHandler(enabled = tab == Tab.HOME && scene != null) { scene = null }
    BackHandler(enabled = tab == Tab.LEARN && learn != null) { learn = null }
    BackHandler(enabled = tab == Tab.GALLERY && photo != null) { photo = null }
    BackHandler(enabled = tab == Tab.PLAN && planPicker) { planPicker = false }
    BackHandler(enabled = tab != Tab.HOME && learn == null && photo == null && more == null && !planPicker) { tab = Tab.HOME }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = {
                            if (tab == t) when (t) {
                                Tab.HOME -> scene = null
                                Tab.LEARN -> learn = null
                                Tab.GALLERY -> photo = null
                                Tab.PLAN -> planPicker = false
                                Tab.MORE -> more = null
                            }
                            tab = t
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.HOME -> if (sceneObj != null) {
                    SceneScreen(
                        state, sceneObj,
                        onBack = { scene = null },
                        onGuide = { guide = true },
                        onOpenPhoto = { photo = it; tab = Tab.GALLERY },
                        onOpenGear = openGear,
                    )
                } else {
                    HomeScreen(
                        state, state.place,
                        onOpenScene = { scene = it },
                        onOpenGear = openGear,
                        onOpenPlan = { tab = Tab.PLAN },
                        onOpenLesson = { learn = "lesson:$it"; tab = Tab.LEARN },
                        onOpenProblem = { learn = "problem:$it"; tab = Tab.LEARN },
                    )
                }
                Tab.LEARN -> LearnScreen(state, learn, onRoute = { learn = it }, onPractice = openScene)
                Tab.GALLERY -> GalleryScreen(state, photo, onOpen = { photo = it }, onOpenScene = openScene, onOpenGear = openGear)
                Tab.PLAN -> if (planPicker) PlacePicker(state) { planPicker = false }
                    else SkyScreen(state, state.place) { planPicker = true }
                Tab.MORE -> MoreScreen(state, more, onRoute = { more = it }, onGearDone = { more = null; tab = Tab.HOME })
            }
        }
    }
}
