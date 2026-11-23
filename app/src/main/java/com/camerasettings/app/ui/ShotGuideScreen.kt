package com.camerasettings.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.BestTime
import com.camerasettings.app.data.Region
import com.camerasettings.app.data.Shot
import com.camerasettings.app.data.Shots
import com.camerasettings.app.logic.Advisor
import com.camerasettings.app.logic.Astro
import com.camerasettings.app.logic.Presets
import com.camerasettings.app.logic.Recommendation
import com.camerasettings.app.logic.Scenes
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val hm = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ShotGuideScreen(state: AppState, shotId: String?, onOpen: (String?) -> Unit, onOpenGear: () -> Unit) {
    val shot = Shots.byId(shotId)
    if (shot == null) ShotList(onOpen) else ShotDetail(state, shot, onBack = { onOpen(null) }, onOpenGear = onOpenGear)
}

@Composable
private fun ShotList(onOpen: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ScreenTitle(
                    "Shot guide",
                    "13 classic photos on your route. Each shows the flat RAW look next to the finished edit, " +
                        "with the camera settings and the editing recipe.",
                    eyebrow = "Inspiration",
                )
            }
        }
        Region.entries.forEach { region ->
            val shots = Shots.all.filter { it.region == region }
            item(key = region.name) {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(12.dp))
                    Text(region.title, style = MaterialTheme.typography.titleLarge)
                    Text(region.blurb, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(10.dp))
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shots, key = { it.id }) { s ->
                        Column(Modifier.width(260.dp).clickable { onOpen(s.id) }) {
                            ArtImage(Art[s.id]!!.thumb)
                            Spacer(Modifier.height(8.dp))
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${s.place.name} · ${s.bestTime.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(
                "The pictures are illustrations painted for this app, not photos. They show what the light, " +
                    "composition and edit should feel like.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}

@Composable
private fun ShotDetail(state: AppState, shot: Shot, onBack: () -> Unit, onOpenGear: () -> Unit) {
    val ctx = LocalContext.current
    val art = Art[shot.id]!!
    val preset = Presets.byId(shot.presetId)
    var recipeOpen by rememberSaveable(shot.id) { mutableStateOf(false) }
    val scene = Scenes.byId(shot.sceneId)!!
    val body = state.camera
    val rec: Recommendation? = remember(shot.id, body, state.lenses, state.support) {
        val lens = body?.let { Advisor.pickLens(state.lenses, scene.lensPref, it.sensor.crop) }
        if (body == null || lens == null) null else {
            val base = Advisor.recommend(scene, body, lens, Advisor.focalFor(lens, scene.lensPref, body.sensor.crop), state.support)
            val merged = base.settings.map { s -> shot.overrides.firstOrNull { it.name == s.name } ?: s } +
                shot.overrides.filter { o -> base.settings.none { it.name == o.name } }
            base.copy(settings = merged)
        }
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar(shot.region.title, onBack) }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                BeforeAfter(art.before, art.after)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Drag the handle: left is the flat RAW file, right is the finished edit. Painted illustration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(shot.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(shot.place.name)
                    Pill(shot.bestTime.label, container = MaterialTheme.colorScheme.tertiaryContainer, content = MaterialTheme.colorScheme.onTertiaryContainer)
                    Pill("Look: ${preset.name}", container = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(10.dp))
                Text(shot.story, style = MaterialTheme.typography.bodyLarge)
            }
        }

        item { BestLightCard(shot, Modifier.padding(horizontal = 16.dp)) }

        item {
            SectionCard("Camera settings", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.CameraAlt) {
                if (rec == null) {
                    Text("Add your camera and lenses to see settings for your gear.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = onOpenGear) { Text("Set up my gear") }
                } else {
                    Text("For your ${body!!.name}" + if (state.support == com.camerasettings.app.logic.Support.TRIPOD) " on a tripod" else ", handheld", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    KeyValue("Mode dial", rec.mode, rec.modeWhy, emphasize = true)
                    rec.settings.forEach { s ->
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        KeyValue(s.name, s.value, s.why)
                    }
                }
            }
        }

        item {
            SectionCard("How to get the shot", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.Lightbulb) {
                NumberedSteps(shot.howTo)
            }
        }

        item {
            SectionCard("Straight-out-of-camera look (JPEG)", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.PhotoFilter) {
                Text(shot.inCamera, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "These only change the JPEG. The RAW file keeps everything, so you can still edit it any way you like later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            SectionCard("Edit it later: “${preset.name}”", Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.primaryContainer, icon = Icons.Filled.AutoFixHigh) {
                Text(preset.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(10.dp))
                if (recipeOpen) {
                    preset.steps().forEach { (k, v) ->
                        KeyValue(k, v)
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { recipeOpen = !recipeOpen }) { Text(if (recipeOpen) "Hide recipe" else "Show recipe") }
                    FilledTonalButton(onClick = { Share.preset(ctx, preset) }) {
                        Icon(Icons.Filled.Share, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Lightroom preset")
                    }
                }
            }
        }

        item {
            SectionCard("Good to know", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.Info) {
                Text(shot.practical, style = MaterialTheme.typography.bodyMedium)
                shot.place.note.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BestLightCard(shot: Shot, modifier: Modifier) {
    val place = shot.place
    val zone = remember(place.zone) { ZoneId.of(place.zone) }
    val now = remember { ZonedDateTime.now(zone) }
    val today = remember(place.id) { Astro.planDay(LocalDate.now(zone), zone, place.lat, place.lon) }
    val tomorrow = remember(place.id) { Astro.planDay(LocalDate.now(zone).plusDays(1), zone, place.lat, place.lon) }

    SectionCard("Best light", modifier, container = MaterialTheme.colorScheme.secondaryContainer, icon = Icons.Filled.Schedule) {
        val color = MaterialTheme.colorScheme.onSecondaryContainer
        if (shot.bestTime == BestTime.SOFT_LIGHT) {
            Text("Any time of day. Overcast skies or shade are ideal: soft light, rich colours.", color = color)
        } else {
            val t = shot.bestTime.window(today)
            val useToday = t != null && t.second.isAfter(now)
            val w = if (useToday) t else shot.bestTime.window(tomorrow)
            val dayLabel = if (useToday) "Today" else "Tomorrow"
            if (w != null) {
                Text("$dayLabel at ${place.name}", style = MaterialTheme.typography.labelMedium, color = color)
                Text("${w.first.format(hm)} – ${w.second.format(hm)}", style = MaterialTheme.typography.headlineSmall, color = color)
                Text(shot.bestTime.label + " (local time, offline calculation)", style = MaterialTheme.typography.bodySmall, color = color)
            } else if (shot.bestTime == BestTime.DARK_NIGHT) {
                Text("The moon is up during the dark hours tonight and tomorrow.", color = color)
                Text("Open Sun & Stars → star calendar to find a moonless night.", style = MaterialTheme.typography.bodySmall, color = color)
            } else {
                Text("Not available for this date.", color = color)
            }
        }
    }
}
