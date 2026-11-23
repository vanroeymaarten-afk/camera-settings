package com.camerasettings.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Place
import com.camerasettings.app.logic.Advisor
import com.camerasettings.app.logic.Astro
import com.camerasettings.app.logic.Exposure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
private val dateFmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

private fun ZonedDateTime?.hm(): String = this?.format(timeFmt) ?: "—"

@Composable
fun SkyScreen(state: AppState, place: Place?, onPickPlace: () -> Unit) {
    if (place == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            ScreenTitle("Plan your light", "Choose a place to see golden hour, sunset, moon and stars.")
            Spacer(Modifier.height(16.dp))
            androidx.compose.material3.Button(onClick = onPickPlace) { Text("Choose a place") }
        }
        return
    }
    val zone = place.zoneId
    var dateStr by rememberSaveable(place.zone) { mutableStateOf(LocalDate.now(zone).toString()) }
    val date = LocalDate.parse(dateStr)
    val plan = remember(date, place) { Astro.planDay(date, zone, place.lat, place.lon) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ScreenTitle("Plan your light", "Golden hour, moon and night-sky times anywhere in the world. Calculated on the phone, no internet needed.", eyebrow = "Plan")

        // ------------------------------------------------ place
        SectionCard(null) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onPickPlace), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(place.name, style = MaterialTheme.typography.titleMedium)
                    Text(place.subtitle + " · " + place.zone.substringAfter('/').replace('_', ' ') + " time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Change", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }

        // ------------------------------------------------ date
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { dateStr = date.minusDays(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous day") }
            Text(date.format(dateFmt), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { dateStr = date.plusDays(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next day") }
            TextButton(onClick = { dateStr = LocalDate.now(zone).toString() }) { Text("Today") }
        }

        // ------------------------------------------------ sun timeline
        val s = plan.sun
        SectionCard("Sun", icon = Icons.Filled.WbSunny) {
            TimeRow(Dot.Night, "Night ends", s.astroDawn.hm())
            TimeRow(Dot.Blue, "Blue hour", "${s.blueMorningStart.hm()} – ${s.goldenMorningStart.hm()}")
            TimeRow(Dot.Gold, "Golden hour", "${s.goldenMorningStart.hm()} – ${s.goldenMorningEnd.hm()}")
            TimeRow(Dot.Sun, "Sunrise", s.sunrise.hm(), bold = true)
            HorizontalRule()
            TimeRow(Dot.Gold, "Golden hour", "${s.goldenEveningStart.hm()} – ${s.blueEveningStart.hm()}")
            TimeRow(Dot.Sun, "Sunset", s.sunset.hm(), bold = true)
            TimeRow(Dot.Blue, "Blue hour", "${s.blueEveningStart.hm()} – ${s.blueEveningEnd.hm()}")
            TimeRow(Dot.Night, "Dark sky from", s.astroDusk.hm())
            Spacer(Modifier.height(4.dp))
            Text("Local time at ${place.name}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ------------------------------------------------ moon
        SectionCard("Moon", icon = Icons.Filled.DarkMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MoonDisc(plan.moon.illumination, plan.moon.waxing)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(plan.moon.name, style = MaterialTheme.typography.titleMedium)
                    Text("${(plan.moon.illumination * 100).roundToInt()}% lit · rises ${plan.moonrise.hm()} · sets ${plan.moonset.hm()}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    plan.moon.illumination < 0.25 -> "Great for stars: the moon is thin or new."
                    plan.moon.illumination > 0.75 -> "Bright moon: it washes out the Milky Way while it's up, but it's lovely for moonlit landscapes."
                    else -> "Half moon: shoot the stars while it's below the horizon."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        // ------------------------------------------------ night sky
        val n = plan.night
        SectionCard("Tonight's night sky", container = MaterialTheme.colorScheme.secondaryContainer, icon = Icons.Filled.AutoAwesome) {
            val c = MaterialTheme.colorScheme.onSecondaryContainer
            if (!n.fullyDark) {
                Text("It doesn't get fully dark here tonight; times use 'nearly dark'.", style = MaterialTheme.typography.bodySmall, color = c)
                Spacer(Modifier.height(6.dp))
            }
            WindowRow("Dark sky", n.dark, c)
            WindowRow("Dark and no moon", n.moonFree, c)
            Spacer(Modifier.height(6.dp))
            n.targets.forEach { v ->
                Spacer(Modifier.height(6.dp))
                Text(v.target.name, style = MaterialTheme.typography.titleSmall, color = c)
                if (v.bestTime != null) {
                    Text(
                        "${v.windows.joinToString(", ") { "${it.start.hm()}–${it.end.hm()}" }} · best ${v.bestTime.hm()}, ${v.bestAlt.roundToInt()}° up, look ${Astro.compass(v.bestAz)}",
                        style = MaterialTheme.typography.bodySmall, color = c,
                    )
                } else {
                    Text("Not visible in a dark, moonless sky tonight. ${v.target.hint}", style = MaterialTheme.typography.bodySmall, color = c)
                }
            }
        }

        // ------------------------------------------------ star calendar
        StarCalendar(place.lat, place.lon, zone, onPick = { dateStr = it.toString() })

        // ------------------------------------------------ star shutter table
        val body = state.camera
        if (body != null && state.lenses.isNotEmpty() && !state.simple) {
            SectionCard("Longest shutter before stars streak", icon = Icons.Filled.Timer) {
                state.lenses.forEach { lens ->
                    val f = lens.minFocal
                    val npf = Advisor.npfSeconds(body, lens, f)
                    TimeRow(null, "${lens.name} @ $f mm, ${Exposure.formatAperture(lens.apertureAt(f))}", Exposure.formatShutter(Exposure.shutterAtMost(minOf(npf, 30.0))))
                }
                Text("Sharp-star (NPF) rule for your ${body.name}.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StarCalendar(lat: Double, lon: Double, zone: ZoneId, onPick: (LocalDate) -> Unit) {
    val start = remember(zone) { LocalDate.now(zone) }
    val nights by produceState<List<Astro.StarNight>?>(null, lat, lon, zone) {
        value = withContext(Dispatchers.Default) { Astro.starCalendar(start, 45, zone, lat, lon) }
    }
    SectionCard("Star calendar: next 45 nights", icon = Icons.Filled.CalendarMonth) {
        Text(
            "Hours of dark sky with the moon down. Long bars = best nights for stars. Tap a night to see its details.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        val list = nights
        if (list == null) {
            Text("Calculating…")
        } else {
            val maxMin = (list.maxOfOrNull { it.moonFreeMinutes } ?: 1L).coerceAtLeast(60L)
            list.forEach { night ->
                val frac = night.moonFreeMinutes.toFloat() / maxMin
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onPick(night.date) }.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(night.date.format(shortDate), style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(92.dp))
                    Box(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Box(
                            Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).clip(RoundedCornerShape(7.dp))
                                .background(if (frac > 0.66f) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        String.format(Locale.US, "%.1f h", night.moonFreeMinutes / 60.0),
                        style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(44.dp),
                    )
                    MoonDisc(night.illumination, night.waxing, size = 14)
                }
            }
        }
    }
}

private enum class Dot { Night, Blue, Gold, Sun }

@Composable
private fun TimeRow(dot: Dot?, label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (dot != null) {
            val c = when (dot) {
                Dot.Night -> Color(0xFF2B2F55)
                Dot.Blue -> Color(0xFF4A72C9)
                Dot.Gold -> Color(0xFFF0A040)
                Dot.Sun -> Color(0xFFE9573F)
            }
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(c))
            Spacer(Modifier.width(12.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun HorizontalRule() {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun WindowRow(label: String, windows: List<Astro.Window>, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.weight(1f))
        Text(
            if (windows.isEmpty()) "none" else windows.joinToString(", ") { "${it.start.hm()}–${it.end.hm()}" },
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color,
        )
    }
}

/** A tiny drawn moon showing the lit fraction. */
@Composable
fun MoonDisc(illumination: Double, waxing: Boolean, size: Int = 40) {
    androidx.compose.foundation.Canvas(Modifier.size(size.dp)) {
        val r = this.size.minDimension / 2
        drawCircle(Color(0xFF2A2D45), r)
        val lit = Color(0xFFF4EBD0)
        // Draw lit part: half disc + ellipse
        val k = (illumination * 2 - 1).toFloat() // -1 new .. 1 full
        val path = androidx.compose.ui.graphics.Path().apply {
            val cx = center.x
            val cy = center.y
            // outer limb (semicircle on the lit side)
            val rect = androidx.compose.ui.geometry.Rect(cx - r, cy - r, cx + r, cy + r)
            // Seen from Chile (southern hemisphere) a waxing moon is lit on the left.
            val side = if (waxing) 90f else -90f
            arcTo(rect, side, 180f, true)
            // terminator ellipse
            val ew = r * kotlin.math.abs(k)
            val er = androidx.compose.ui.geometry.Rect(cx - ew, cy - r, cx + ew, cy + r)
            val sweep = if (k > 0) 180f else -180f
            arcTo(er, side + 180f, sweep, false)
            close()
        }
        drawPath(path, lit)
    }
}
