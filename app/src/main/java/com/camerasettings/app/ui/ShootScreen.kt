package com.camerasettings.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camerasettings.app.R
import com.camerasettings.app.data.AppState
import com.camerasettings.app.logic.Advisor
import com.camerasettings.app.logic.Astro
import com.camerasettings.app.logic.Scene
import com.camerasettings.app.logic.Scenes
import com.camerasettings.app.logic.Support
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ShootScreen(state: AppState, sceneId: String?, onOpenScene: (String?) -> Unit, onOpenGear: () -> Unit, onOpenSky: () -> Unit) {
    val scene = Scenes.byId(sceneId)
    if (!state.hasGear) {
        Welcome(onOpenGear)
        return
    }
    if (scene != null) {
        BackHandler { onOpenScene(null) }
        SceneDetail(state, scene, onBack = { onOpenScene(null) })
    } else {
        SceneGrid(state, onPick = { onOpenScene(it.id) }, onOpenGear = onOpenGear, onOpenSky = onOpenSky)
    }
}

@Composable
private fun Welcome(onOpenGear: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.ex_valle_luna_after), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.2f to Color.Transparent, 0.75f to Color.Black.copy(alpha = 0.85f))))
        Column(Modifier.align(Alignment.BottomStart).padding(28.dp)) {
            Text("CAMERA COACH · CHILE", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC9A8))
            Spacer(Modifier.height(8.dp))
            Text("Great photos from the Atacama to the Lakes.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(10.dp))
            Text(
                "Tell me which Canon camera and lenses you're bringing, and I'll give you the exact settings " +
                    "for every situation on your trip.",
                style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onOpenGear, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Set up my gear") }
        }
    }
}

@Composable
fun SupportChips(state: AppState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(
            selected = state.support == Support.HANDHELD,
            onClick = { state.selectSupport(Support.HANDHELD) },
            label = { Text("Handheld") },
        )
        FilterChip(
            selected = state.support == Support.TRIPOD,
            onClick = { state.selectSupport(Support.TRIPOD) },
            label = { Text("On a tripod") },
        )
    }
}

@Composable
private fun TodayCard(state: AppState, onOpenSky: () -> Unit) {
    val place = state.place
    val zone = remember(place.zone) { runCatching { ZoneId.of(place.zone) }.getOrDefault(ZoneId.systemDefault()) }
    var now by remember { mutableStateOf(ZonedDateTime.now(zone)) }
    LaunchedEffect(zone) {
        while (true) {
            now = ZonedDateTime.now(zone)
            delay(60_000)
        }
    }
    var next by remember { mutableStateOf<Astro.LightEvent?>(null) }
    LaunchedEffect(place, now.minute / 5) {
        next = withContext(Dispatchers.Default) { Astro.nextEvent(now, place.lat, place.lon) }
    }
    Surface(
        Modifier.fillMaxWidth().clickable(onClick = onOpenSky),
        shape = CardShape,
        color = MaterialTheme.colorScheme.secondary,
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("NEXT LIGHT · ${place.name.uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                val n = next
                if (n == null) {
                    Text("Calculating…", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("${n.label} at ${n.time.format(DateTimeFormatter.ofPattern("HH:mm"))}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondary)
                    val mins = Duration.between(now, n.time).toMinutes()
                    Text(
                        "in " + if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f),
                    )
                }
            }
            Icon(Icons.Filled.WbTwilight, null, Modifier.size(36.dp), tint = Color(0xFFFFC27A))
        }
    }
}

@Composable
private fun SceneGrid(state: AppState, onPick: (Scene) -> Unit, onOpenGear: () -> Unit, onOpenSky: () -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.weight(1f)) { ScreenTitle("What are you shooting?", eyebrow = "Shoot") }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onOpenGear).padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "${state.camera?.name} · ${state.lenses.size} lens" + if (state.lenses.size != 1) "es" else "",
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.Edit, "Edit gear", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TodayCard(state, onOpenSky)
                SupportChips(state)
            }
        }
        items(Scenes.all, key = { it.id }) { scene ->
            SceneCard(scene) { onPick(scene) }
        }
    }
}

@Composable
private fun SceneCard(scene: Scene, onClick: () -> Unit) {
    val art = Art[scene.art]
    Column(Modifier.fillMaxWidth().clip(CardShape).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(CardShape)) {
            if (art != null) {
                Image(painterResource(art.thumb), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)),
                    ),
                    contentAlignment = Alignment.Center,
                ) { Text(scene.emoji, fontSize = 44.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(scene.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
        Text(scene.where, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SceneDetail(state: AppState, scene: Scene, onBack: () -> Unit) {
    val body = state.camera ?: return
    val bag = state.lenses
    val crop = body.sensor.crop
    var lensId by rememberSaveable(scene.id) { mutableStateOf(Advisor.pickLens(bag, scene.lensPref, crop)?.id) }
    val lens = bag.firstOrNull { it.id == lensId } ?: bag.first()
    var focal by remember(scene.id, lens.id) { mutableIntStateOf(Advisor.focalFor(lens, scene.lensPref, crop)) }
    val rec = remember(scene.id, lens, focal, state.support, body) {
        Advisor.recommend(scene, body, lens, focal, state.support)
    }
    val art = Art[scene.art]

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar(scene.title, onBack) }
        if (art != null) {
            item { ArtImage(art.after, Modifier.padding(horizontal = 16.dp), title = scene.title, subtitle = scene.where, ratio = 2f) }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SupportChips(state)
                if (bag.size > 1) {
                    Text("Lens", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        bag.forEach { l ->
                            FilterChip(selected = l.id == lens.id, onClick = { lensId = l.id }, label = { Text(l.name) })
                        }
                    }
                }
                if (lens.isZoom) {
                    Text("Zoom: $focal mm", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = focal.toFloat(),
                        onValueChange = { focal = it.roundToInt() },
                        valueRange = lens.minFocal.toFloat()..lens.maxFocal.toFloat(),
                    )
                }
            }
        }

        item {
            SectionCard(null, Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.primaryContainer) {
                Text("MODE DIAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(rec.mode, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(rec.modeWhy, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        items(rec.warnings) { w ->
            SectionCard(null, Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.tertiaryContainer) {
                Row {
                    Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(w, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        item {
            SectionCard("Settings", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.Tune) {
                rec.settings.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    KeyValue(s.name, s.value, s.why)
                }
            }
        }

        item {
            SectionCard("Tips for Chile", Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.secondaryContainer, icon = Icons.Filled.Lightbulb) {
                Bullets(rec.tips)
            }
        }
    }
}
