package com.camerasettings.app.ui

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Learn
import com.camerasettings.app.data.Photos
import com.camerasettings.app.data.Place
import com.camerasettings.app.logic.Astro
import com.camerasettings.app.logic.Category
import com.camerasettings.app.logic.Scene
import com.camerasettings.app.logic.Scenes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    state: AppState,
    place: Place?,
    onOpenScene: (String) -> Unit,
    onOpenGear: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenLesson: (String) -> Unit,
    onOpenProblem: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val scenes = Scenes.search(query).filter { category == null || it.category.name == category }
    val nextLesson = Learn.lessons.firstOrNull { it.id !in state.lessonsDone }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(158.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ScreenTitle("What do you want to photograph?", eyebrow = "Camera Coach")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(onClick = onOpenGear).padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CameraAlt, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        state.camera?.let { "${it.brand.label} ${it.name} · ${state.lenses.size} lens" + if (state.lenses.size != 1) "es" else "" } ?: "Choose your camera",
                        style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Filled.Edit, "Change camera", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (place != null) NextLightCard(place, onOpenPlan)
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search: waterfall, dog, fireworks…") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                )
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                    Category.entries.forEach { c ->
                        FilterChip(selected = category == c.name, onClick = { category = if (category == c.name) null else c.name }, label = { Text(c.title) })
                    }
                }
            }
        }
        items(scenes, key = { it.id }) { scene -> SceneCard(scene) { onOpenScene(scene.id) } }
        if (scenes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Nothing found. Try another word, or pick a category.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (nextLesson != null && query.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(Modifier.fillMaxWidth().clickable { onOpenLesson(nextLesson.id) }, shape = CardShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.School, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (state.lessonsDone.isEmpty()) "New to cameras? Start Camera School" else "Continue Camera School",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(Learn.fill(nextLesson.title, state.brand), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("${nextLesson.minutes} min · ${state.lessonsDone.size} of ${Learn.lessons.size} done", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
        if (query.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SectionLabel("Something went wrong?")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Learn.problems.take(6).forEach { p ->
                            FilterChip(selected = false, onClick = { onOpenProblem(p.id) }, label = { Text("${p.emoji} ${p.title}") })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextLightCard(place: Place, onClick: () -> Unit) {
    val zone = place.zoneId
    var now by remember { mutableStateOf(ZonedDateTime.now(zone)) }
    LaunchedEffect(zone) {
        while (true) { now = ZonedDateTime.now(zone); delay(60_000) }
    }
    var next by remember { mutableStateOf<Astro.LightEvent?>(null) }
    LaunchedEffect(place, now.minute / 5) {
        next = withContext(Dispatchers.Default) { Astro.nextEvent(now, place.lat, place.lon) }
    }
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = CardShape, color = MaterialTheme.colorScheme.secondary) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f))
                    Spacer(Modifier.width(4.dp))
                    Text(place.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.8f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.height(4.dp))
                val n = next
                if (n == null) {
                    Text("Calculating the light…", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Text("${n.label} at ${n.time.format(DateTimeFormatter.ofPattern("HH:mm"))}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondary)
                    val mins = Duration.between(now, n.time).toMinutes()
                    Text("in " + if (mins >= 60) "${mins / 60} h ${mins % 60} min" else "$mins min", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.85f))
                }
            }
            Icon(Icons.Filled.WbTwilight, null, Modifier.size(36.dp), tint = Color(0xFFFFC27A))
        }
    }
}

@Composable
private fun SceneCard(scene: Scene, onClick: () -> Unit) {
    val photo = Photos.forScene(scene.id).firstOrNull()
    Column(Modifier.fillMaxWidth().clip(CardShape).clickable(onClick = onClick)) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(CardShape)) {
            if (photo != null) {
                Image(painterResource(photo.res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary))),
                    contentAlignment = Alignment.Center,
                ) { Text(scene.emoji, fontSize = 44.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(scene.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
        Text(scene.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
        Spacer(Modifier.height(4.dp))
    }
}
