package com.camerasettings.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.logic.Advisor
import com.camerasettings.app.logic.Scene
import com.camerasettings.app.logic.Scenes
import com.camerasettings.app.logic.Support
import kotlin.math.roundToInt

@Composable
fun ShootScreen(state: AppState, onOpenGear: () -> Unit) {
    var sceneId by rememberSaveable { mutableStateOf<String?>(null) }
    val scene = Scenes.byId(sceneId)

    if (!state.hasGear) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            ScreenTitle("Welcome! 👋", "First tell me which Canon camera and lenses you're taking to Chile.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onOpenGear) { Text("Set up my gear") }
        }
        return
    }

    if (scene != null) {
        BackHandler { sceneId = null }
        SceneDetail(state, scene, onBack = { sceneId = null })
    } else {
        SceneGrid(state, onPick = { sceneId = it.id })
    }
}

@Composable
private fun SupportChips(state: AppState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("I'm shooting:", style = MaterialTheme.typography.bodyMedium)
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
private fun SceneGrid(state: AppState, onPick: (Scene) -> Unit) {
    var basicsOpen by rememberSaveable { mutableStateOf(false) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                ScreenTitle("What are you shooting?", state.camera?.name)
                Spacer(Modifier.height(4.dp))
                SupportChips(state)
            }
        }
        items(Scenes.all, key = { it.id }) { scene ->
            Card(Modifier.fillMaxWidth().clickable { onPick(scene) }) {
                Column(Modifier.padding(14.dp)) {
                    Text(scene.emoji, fontSize = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(scene.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(scene.where, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionCard(null) {
                Row(
                    Modifier.fillMaxWidth().clickable { basicsOpen = !basicsOpen },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📖  Camera basics in 1 minute", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Icon(if (basicsOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                }
                if (basicsOpen) {
                    Advisor.basics.forEach { (term, text) ->
                        Spacer(Modifier.height(8.dp))
                        Text(term, fontWeight = FontWeight.SemiBold)
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                Text("${scene.emoji}  ${scene.title}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            SupportChips(state)
        }

        if (bag.size > 1) {
            item {
                Text("Lens", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    bag.forEach { l ->
                        FilterChip(selected = l.id == lens.id, onClick = { lensId = l.id }, label = { Text(l.name) })
                    }
                }
            }
        }
        if (lens.isZoom) {
            item {
                Text("Zoom: $focal mm", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = focal.toFloat(),
                    onValueChange = { focal = it.roundToInt() },
                    valueRange = lens.minFocal.toFloat()..lens.maxFocal.toFloat(),
                )
            }
        }

        item {
            SectionCard(null, container = MaterialTheme.colorScheme.primaryContainer) {
                Text("Mode dial", style = MaterialTheme.typography.labelLarge)
                Text(rec.mode, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(rec.modeWhy, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (rec.warnings.isNotEmpty()) {
            items(rec.warnings) { w ->
                SectionCard(null, container = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row {
                        Icon(Icons.Filled.Warning, null)
                        Spacer(Modifier.width(8.dp))
                        Text(w)
                    }
                }
            }
        }

        item {
            SectionCard("Settings") {
                rec.settings.forEachIndexed { i, s ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Text(s.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (s.why.isNotBlank()) Text(s.why, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            SectionCard("Tips for Chile", container = MaterialTheme.colorScheme.secondaryContainer) {
                rec.tips.forEach { t ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•  ")
                        Text(t, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
