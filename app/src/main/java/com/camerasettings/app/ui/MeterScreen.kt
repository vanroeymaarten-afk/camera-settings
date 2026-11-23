package com.camerasettings.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.logic.Exposure
import java.util.Locale
import kotlin.math.roundToInt

private val evPresets = listOf(
    16.0 to "Bright sun on snow / salt",
    15.0 to "Sunny",
    14.0 to "Hazy sun",
    13.0 to "Bright overcast",
    12.0 to "Overcast / open shade",
    11.0 to "Sunset",
    9.0 to "Just after sunset",
    7.0 to "Blue hour",
    5.0 to "City at night",
    2.0 to "Bright indoor",
    -2.0 to "Full-moon landscape",
    -6.0 to "Milky Way",
)

private val ndFilters = listOf(3 to "ND8", 6 to "ND64", 10 to "ND1000")

@Composable
private fun rememberLux(): Pair<Boolean, Float?> {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_LIGHT) }
    var lux by remember { mutableStateOf<Float?>(null) }
    DisposableEffect(sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) { lux = e.values[0] }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm.unregisterListener(listener) }
    }
    return (sensor != null) to lux
}

@Composable
fun MeterScreen(state: AppState) {
    val (hasSensor, liveLux) = rememberLux()
    var useSensor by rememberSaveable { mutableStateOf(true) }
    var frozenLux by rememberSaveable { mutableStateOf<Float?>(null) }
    var manualEv by rememberSaveable { mutableFloatStateOf(15f) }
    var isoIdx by rememberSaveable { mutableIntStateOf(Exposure.isoStops.indexOf(100)) }
    var aperturePriority by rememberSaveable { mutableStateOf(true) }
    var apIdx by rememberSaveable { mutableIntStateOf(Exposure.apertureStops.indexOf(8.0)) }
    var shIdx by rememberSaveable { mutableIntStateOf(Exposure.shutterStops.indexOfFirst { it == 1 / 250.0 }) }
    var ndStops by rememberSaveable { mutableIntStateOf(6) }

    val sensorActive = hasSensor && useSensor
    val lux = frozenLux ?: liveLux
    val ev = if (sensorActive && lux != null) Exposure.ev100FromLux(lux.toDouble()) else manualEv.toDouble()
    val iso = Exposure.isoStops[isoIdx]

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle("Light meter", "Use your phone to measure light, or pick a situation. Handy for manual mode and ND filters.")

        // ------------------------------------------------ light reading
        SectionCard("1. How bright is it?") {
            if (hasSensor) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = useSensor, onClick = { useSensor = true }, label = { Text("Phone sensor") })
                    FilterChip(selected = !useSensor, onClick = { useSensor = false }, label = { Text("Pick situation") })
                }
            } else {
                Text("Your phone has no light sensor, so pick the situation instead.", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            if (sensorActive) {
                Text(
                    "Hold the phone in the same light as your subject, screen facing towards where you'll shoot from. " +
                        "The sensor is near the selfie camera.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (lux == null) "Waiting for sensor…" else String.format(Locale.US, "%.0f lux  ·  %s", lux, Exposure.formatEv(ev)),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                )
                OutlinedButton(onClick = { frozenLux = if (frozenLux == null) liveLux else null }) {
                    Text(if (frozenLux == null) "Hold this reading" else "Release (live reading)")
                }
            } else {
                Text(Exposure.formatEv(ev), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Slider(value = manualEv, onValueChange = { manualEv = it.roundToInt().toFloat() }, valueRange = -6f..16f, steps = 21)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    evPresets.forEach { (v, label) ->
                        FilterChip(selected = manualEv.toDouble() == v, onClick = { manualEv = v.toFloat() }, label = { Text(label) })
                    }
                }
            }
        }

        // ------------------------------------------------ settings
        SectionCard("2. Your settings") {
            Text("ISO ${iso}", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = isoIdx.toFloat(),
                onValueChange = { isoIdx = it.roundToInt() },
                valueRange = 0f..(Exposure.isoStops.lastIndex).toFloat(),
                steps = Exposure.isoStops.size - 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = aperturePriority, onClick = { aperturePriority = true }, label = { Text("I choose aperture") })
                FilterChip(selected = !aperturePriority, onClick = { aperturePriority = false }, label = { Text("I choose shutter") })
            }
            if (aperturePriority) {
                val n = Exposure.apertureStops[apIdx]
                Text("Aperture ${Exposure.formatAperture(n)}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = apIdx.toFloat(), onValueChange = { apIdx = it.roundToInt() },
                    valueRange = 0f..Exposure.apertureStops.lastIndex.toFloat(), steps = Exposure.apertureStops.size - 2,
                )
            } else {
                val t = Exposure.shutterStops[shIdx]
                Text("Shutter ${Exposure.formatShutter(t)}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = shIdx.toFloat(), onValueChange = { shIdx = it.roundToInt() },
                    valueRange = 0f..Exposure.shutterStops.lastIndex.toFloat(), steps = Exposure.shutterStops.size - 2,
                )
            }
        }

        // ------------------------------------------------ result
        val resultShutter: Double
        SectionCard("3. Set your camera to (M mode)", container = MaterialTheme.colorScheme.primaryContainer) {
            if (aperturePriority) {
                val n = Exposure.apertureStops[apIdx]
                val t = Exposure.shutterFor(ev, n, iso)
                val tooLong = t > 30.5
                val tooShort = t < 1 / 8000.0
                Text(
                    "${Exposure.formatAperture(n)}  ·  " +
                        (if (tooLong) "${t.roundToInt()} s (use BULB)" else Exposure.formatShutter(Exposure.nearestShutter(t))) +
                        "  ·  ISO $iso",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                )
                if (tooShort) Text("Too bright: choose a lower ISO or a smaller aperture (bigger f-number).")
                state.camera?.let { body ->
                    state.lenses.firstOrNull()?.let { lens ->
                        val hh = com.camerasettings.app.logic.Advisor.handheldShutter(body, lens, lens.minFocal)
                        if (t > hh && !tooLong) Text("Slower than ${Exposure.formatShutter(hh)}: use a tripod or raise ISO.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                val t = Exposure.shutterStops[shIdx]
                val n = Exposure.apertureFor(ev, t, iso)
                val ok = n in 1.0..22.5
                Text(
                    "${if (ok) Exposure.formatAperture(Exposure.nearestAperture(n)) else String.format(Locale.US, "f/%.1f", n)}  ·  " +
                        "${Exposure.formatShutter(t)}  ·  ISO $iso",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                )
                if (n < 1.0) Text("Too dark for this shutter speed: raise ISO or use a slower shutter.")
                if (n > 22.5) Text("Too bright: lower ISO, use a faster shutter, or an ND filter.")
            }
            Text(
                "Tip: take a test shot and check the histogram, then adjust by ⅓–⅔ stop.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        resultShutter = if (aperturePriority) {
            Exposure.shutterFor(ev, Exposure.apertureStops[apIdx], iso)
        } else {
            Exposure.shutterStops[shIdx]
        }

        // ------------------------------------------------ ND calculator
        SectionCard("ND filter calculator") {
            Text(
                "Meter WITHOUT the filter first (above). Then screw on the filter and use this shutter speed:",
                style = MaterialTheme.typography.bodySmall,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ndFilters.forEach { (stops, name) ->
                    FilterChip(selected = ndStops == stops, onClick = { ndStops = stops }, label = { Text("$name ($stops stops)") })
                }
            }
            val withNd = resultShutter * (1 shl ndStops)
            Spacer(Modifier.height(4.dp))
            Text(
                "${Exposure.formatShutter(Exposure.nearestShutter(resultShutter))}  →  " +
                    if (withNd > 30.5) formatLong(withNd) + " (BULB + timer)" else Exposure.formatShutter(Exposure.nearestShutter(withNd)),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
            )
            Text("Autofocus before attaching dark filters, then switch the lens to MF.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatLong(t: Double): String {
    val s = t.roundToInt()
    return if (s >= 60) "${s / 60} min ${s % 60} s" else "$s s"
}
