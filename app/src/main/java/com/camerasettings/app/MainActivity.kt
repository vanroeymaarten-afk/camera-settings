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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import com.camerasettings.app.data.AppState
import com.camerasettings.app.ui.AppTheme
import com.camerasettings.app.ui.MeterScreen
import com.camerasettings.app.ui.MoreRoutes
import com.camerasettings.app.ui.MoreScreen
import com.camerasettings.app.ui.ShootScreen
import com.camerasettings.app.ui.ShotGuideScreen
import com.camerasettings.app.ui.SkyScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val state = AppState(applicationContext)
        setContent {
            AppTheme { App(state) }
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    SHOOT("Shoot", Icons.Filled.PhotoCamera),
    GUIDE("Shot guide", Icons.Filled.AutoAwesome),
    SKY("Sun & Stars", Icons.Filled.NightsStay),
    METER("Meter", Icons.Filled.Exposure),
    MORE("More", Icons.Filled.MoreHoriz),
}

@Composable
fun App(state: AppState, startTab: Tab = Tab.SHOOT, startShot: String? = null, startMore: String? = null, startScene: String? = null) {
    var tab by rememberSaveable { mutableStateOf(startTab) }
    var sceneRoute by rememberSaveable { mutableStateOf(startScene) }
    var shotRoute by rememberSaveable { mutableStateOf(startShot) }
    var moreRoute by rememberSaveable { mutableStateOf(startMore) }
    val openGear = { tab = Tab.MORE; moreRoute = MoreRoutes.GEAR }

    // System back: leave a sub-screen first, then go back to the Shoot tab.
    BackHandler(enabled = tab != Tab.SHOOT && (tab != Tab.GUIDE || shotRoute == null) && (tab != Tab.MORE || moreRoute == null)) {
        tab = Tab.SHOOT
    }
    BackHandler(enabled = tab == Tab.GUIDE && shotRoute != null) { shotRoute = null }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = {
                            // Tapping the active tab again returns to its top level.
                            if (tab == t) when (t) {
                                Tab.SHOOT -> sceneRoute = null
                                Tab.GUIDE -> shotRoute = null
                                Tab.MORE -> moreRoute = null
                                else -> Unit
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
                Tab.SHOOT -> ShootScreen(
                    state, sceneRoute,
                    onOpenScene = { sceneRoute = it },
                    onOpenGear = openGear,
                    onOpenSky = { tab = Tab.SKY },
                )
                Tab.GUIDE -> ShotGuideScreen(state, shotRoute, onOpen = { shotRoute = it }, onOpenGear = openGear)
                Tab.SKY -> SkyScreen(state)
                Tab.METER -> MeterScreen(state)
                Tab.MORE -> MoreScreen(
                    state, moreRoute,
                    onRoute = { moreRoute = it },
                    onGearDone = { moreRoute = null; tab = Tab.SHOOT },
                )
            }
        }
    }
}
