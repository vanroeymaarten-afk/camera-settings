package com.camerasettings.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Exposure
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
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
import com.camerasettings.app.data.AppState
import com.camerasettings.app.ui.AppTheme
import com.camerasettings.app.ui.GearScreen
import com.camerasettings.app.ui.MeterScreen
import com.camerasettings.app.ui.ShootScreen
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
    METER("Meter", Icons.Filled.Exposure),
    SKY("Sun & Stars", Icons.Filled.NightsStay),
    GEAR("My gear", Icons.Filled.Backpack),
}

@Composable
fun App(state: AppState) {
    var tab by rememberSaveable { mutableStateOf(if (state.hasGear) Tab.SHOOT else Tab.GEAR) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                Tab.SHOOT -> ShootScreen(state, onOpenGear = { tab = Tab.GEAR })
                Tab.METER -> MeterScreen(state)
                Tab.SKY -> SkyScreen(state)
                Tab.GEAR -> GearScreen(state, onDone = { tab = Tab.SHOOT })
            }
        }
    }
}
