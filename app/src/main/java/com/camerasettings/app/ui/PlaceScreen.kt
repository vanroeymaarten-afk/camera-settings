package com.camerasettings.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Place
import com.camerasettings.app.data.World
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Search 34,000 cities and famous photo spots, or use GPS. */
@Composable
fun PlacePicker(state: AppState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val loaded by produceState(World.loaded) { if (!value) { withContext(Dispatchers.IO) { World.load(ctx.applicationContext) }; value = true } }
    val results by produceState(emptyList<Place>(), query, loaded) {
        value = if (loaded) withContext(Dispatchers.Default) { World.search(query) } else emptyList()
    }

    fun choose(p: Place) { state.selectPlace(p); onBack() }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            message = "Finding you… (works best outdoors)"
            locate(ctx) { loc ->
                if (loc == null) {
                    message = "Couldn't get a location. Go outside, or search for a place."
                } else {
                    val near = World.nearest(loc.latitude, loc.longitude)
                    val name = near?.let { "Near ${it.name}" } ?: "My location"
                    choose(
                        Place(
                            name, near?.country ?: String.format(Locale.US, "%.3f, %.3f", loc.latitude, loc.longitude),
                            loc.latitude, loc.longitude, near?.zone ?: java.time.ZoneId.systemDefault().id,
                        ),
                    )
                }
            }
        } else {
            message = "No location permission. Search for a place instead."
        }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { BackBar("Choose a place", onBack) }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Filled.MyLocation, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use where I am now (GPS)")
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(
                    query, { query = it }, Modifier.fillMaxWidth(),
                    placeholder = { Text("Search any city or photo spot…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                )
                if (!loaded) Text("Loading world map…", style = MaterialTheme.typography.bodySmall)
            }
        }
        if (query.length < 2) {
            if (state.recentPlaces.isNotEmpty()) {
                item { SectionLabel("Recent", Modifier.padding(horizontal = 20.dp)) }
                items(state.recentPlaces, key = { "r" + it.encode() }) { p -> PlaceRow(p, Icons.Filled.History) { choose(p) } }
            }
            item { SectionLabel("Famous photo spots", Modifier.padding(horizontal = 20.dp)) }
            items(if (loaded) World.featured else emptyList(), key = { "f" + it.encode() }) { p -> PlaceRow(p, Icons.Filled.Photo) { choose(p) } }
        } else {
            if (results.isEmpty() && loaded) item { Text("No places found.", Modifier.padding(20.dp)) }
            items(results, key = { "s" + it.encode() }) { p -> PlaceRow(p, if (p.landmark) Icons.Filled.Photo else Icons.Filled.LocationCity) { choose(p) } }
        }
        item {
            Text(
                "Cities: GeoNames (geonames.org), CC BY 4.0. Works offline.",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun PlaceRow(p: Place, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(p.name, style = MaterialTheme.typography.bodyLarge)
            Text(p.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@SuppressLint("MissingPermission")
fun locate(ctx: Context, onResult: (Location?) -> Unit) {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    try {
        val providers = lm.getProviders(true)
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 6 * 3600_000L) { onResult(last); return }
        val provider = when {
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
            else -> providers.firstOrNull()
        }
        if (provider == null) { onResult(last); return }
        if (Build.VERSION.SDK_INT >= 30) {
            lm.getCurrentLocation(provider, null, ctx.mainExecutor) { onResult(it ?: last) }
        } else {
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(provider, { onResult(it) }, android.os.Looper.getMainLooper())
        }
    } catch (e: SecurityException) {
        onResult(null)
    }
}
