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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Places
import com.camerasettings.app.logic.Advisor
import com.camerasettings.app.logic.Astro
import com.camerasettings.app.logic.Exposure
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)

private fun ZonedDateTime?.hm(): String = this?.format(timeFmt) ?: "—"

@Composable
fun SkyScreen(state: AppState) {
    val ctx = LocalContext.current
    val place = state.place
    val zone = remember(place.zone) { runCatching { ZoneId.of(place.zone) }.getOrDefault(ZoneId.systemDefault()) }
    var dateStr by rememberSaveable { mutableStateOf(LocalDate.now(zone).toString()) }
    val date = LocalDate.parse(dateStr)
    var placesOpen by rememberSaveable { mutableStateOf(false) }
    var gpsMessage by remember { mutableStateOf<String?>(null) }

    val plan = remember(date, place) { Astro.planDay(date, zone, place.lat, place.lon) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        if (granted.values.any { it }) {
            gpsMessage = "Finding you… (outdoors works best)"
            locate(ctx) { loc ->
                if (loc != null) {
                    state.setGps(loc.latitude, loc.longitude)
                    gpsMessage = null
                    placesOpen = false
                } else {
                    gpsMessage = "Couldn't get a location. Go outside, or pick a place from the list."
                }
            }
        } else {
            gpsMessage = "No location permission: pick a place from the list instead."
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Sun & Stars", "Golden hour, moon and Milky Way times. Works offline.")

        // ------------------------------------------------ place
        SectionCard(null) {
            Row(Modifier.fillMaxWidth().clickable { placesOpen = !placesOpen }, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("📍 ${place.name}", style = MaterialTheme.typography.titleMedium)
                    Text(place.region, style = MaterialTheme.typography.bodySmall)
                }
                Icon(if (placesOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
            }
            if (placesOpen) {
                OutlinedButton(onClick = {
                    permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) {
                    Icon(Icons.Filled.MyLocation, null)
                    Text("  Use where I am now")
                }
                Places.chile.forEach { p ->
                    Row(Modifier.fillMaxWidth().clickable { state.selectPlace(p.id); placesOpen = false }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.placeId == p.id, onClick = { state.selectPlace(p.id); placesOpen = false })
                        Column {
                            Text(p.name)
                            Text(p.region, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            gpsMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }

        // ------------------------------------------------ date
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { dateStr = date.minusDays(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day") }
            Text(date.format(dateFmt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { dateStr = date.plusDays(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day") }
            TextButton(onClick = { dateStr = LocalDate.now(zone).toString() }) { Text("Today") }
        }

        // ------------------------------------------------ sun
        val s = plan.sun
        SectionCard("☀️ Sun  (local time)") {
            TimeRow("🌌", "Darkness ends", s.astroDawn.hm())
            TimeRow("🔵", "Blue hour", "${s.blueMorningStart.hm()} – ${s.goldenMorningStart.hm()}")
            TimeRow("🟠", "Golden hour", "${s.goldenMorningStart.hm()} – ${s.goldenMorningEnd.hm()}")
            TimeRow("🌄", "Sunrise", s.sunrise.hm(), bold = true)
            Spacer(Modifier.height(6.dp))
            TimeRow("🟠", "Golden hour", "${s.goldenEveningStart.hm()} – ${s.blueEveningStart.hm()}")
            TimeRow("🌇", "Sunset", s.sunset.hm(), bold = true)
            TimeRow("🔵", "Blue hour", "${s.blueEveningStart.hm()} – ${s.blueEveningEnd.hm()}")
            TimeRow("🌌", "Full darkness from", s.astroDusk.hm())
        }

        // ------------------------------------------------ moon
        SectionCard("🌙 Moon") {
            TimeRow(moonEmoji(plan.moon), plan.moon.name, "${(plan.moon.illumination * 100).roundToInt()}% lit", bold = true)
            TimeRow("⬆️", "Moonrise", plan.moonrise.hm())
            TimeRow("⬇️", "Moonset", plan.moonset.hm())
            Text(
                when {
                    plan.moon.illumination < 0.25 -> "Great for stars: the moon is thin or new."
                    plan.moon.illumination > 0.75 -> "Bright moon: the Milky Way will be washed out while it's up. Great for moonlit landscapes, though."
                    else -> "Half moon: shoot stars while the moon is below the horizon."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // ------------------------------------------------ night sky
        val n = plan.night
        SectionCard("✨ Tonight's night sky", container = MaterialTheme.colorScheme.secondaryContainer) {
            if (!n.fullyDark) {
                Text(
                    "It never gets fully dark here tonight (sun only ${(-n.lowestSun).roundToInt()}° below the horizon). " +
                        "Stars are visible but the sky keeps a glow. Times below use 'nearly dark'.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
            }
            WindowRow("Dark sky", n.dark)
            WindowRow("Dark + no moon", n.moonFree)
            Text("Dark sky with the moon down: the moments for star photos.", style = MaterialTheme.typography.bodySmall)
        }

        n.targets.forEach { v ->
            SectionCard("🌌 ${v.target.name}") {
                Text(v.target.hint, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                if (v.bestTime != null) {
                    WindowRow("Visible (dark, no moon)", v.windows)
                    Text(
                        "Best: ${v.bestTime.hm()}, ${v.bestAlt.roundToInt()}° high, look ${Astro.compass(v.bestAz)}.",
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text("Not visible in a dark, moonless sky tonight.", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ------------------------------------------------ star shutter table
        val body = state.camera
        if (body != null && state.lenses.isNotEmpty()) {
            SectionCard("⏱️ Max shutter before stars streak") {
                state.lenses.forEach { lens ->
                    val f = lens.minFocal
                    val npf = Advisor.npfSeconds(body, lens, f)
                    TimeRow(
                        "",
                        "${lens.name} @ $f mm, ${Exposure.formatAperture(lens.apertureAt(f))}",
                        Exposure.formatShutter(Exposure.shutterAtMost(minOf(npf, 30.0))),
                    )
                }
                Text("Sharp-star (NPF) rule for your ${body.name}.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TimeRow(icon: String, label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        if (icon.isNotEmpty()) Text("$icon  ")
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun WindowRow(label: String, windows: List<Astro.Window>) {
    TimeRow("", label, if (windows.isEmpty()) "none" else windows.joinToString(", ") { "${it.start.hm()}–${it.end.hm()}" })
}

private fun moonEmoji(p: Astro.MoonPhase): String = when {
    p.illumination < 0.03 -> "🌑"
    p.illumination > 0.97 -> "🌕"
    p.illumination < 0.4 -> if (p.waxing) "🌒" else "🌘"
    p.illumination < 0.6 -> if (p.waxing) "🌓" else "🌗"
    else -> if (p.waxing) "🌔" else "🌖"
}

@SuppressLint("MissingPermission")
private fun locate(ctx: Context, onResult: (Location?) -> Unit) {
    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    try {
        val providers = lm.getProviders(true)
        val last = providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < 6 * 3600_000L) {
            onResult(last)
            return
        }
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
