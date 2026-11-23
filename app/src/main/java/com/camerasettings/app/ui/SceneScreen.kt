package com.camerasettings.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Level
import com.camerasettings.app.data.Photos
import com.camerasettings.app.logic.Advisor
import com.camerasettings.app.logic.Exposure
import com.camerasettings.app.logic.Recommendation
import com.camerasettings.app.logic.Scene
import com.camerasettings.app.logic.Support
import kotlin.math.roundToInt

@Composable
fun SupportChips(state: AppState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        FilterChip(selected = state.support == Support.HANDHELD, onClick = { state.selectSupport(Support.HANDHELD) }, label = { Text("In my hands") })
        FilterChip(selected = state.support == Support.TRIPOD, onClick = { state.selectSupport(Support.TRIPOD) }, label = { Text("On a tripod") })
    }
}

@Composable
fun SceneScreen(state: AppState, scene: Scene, onBack: () -> Unit, onGuide: () -> Unit, onOpenPhoto: (String) -> Unit, onOpenGear: () -> Unit) {
    val body = state.camera
    val bag = state.lenses
    if (body == null || bag.isEmpty()) {
        Column(Modifier.padding(24.dp)) {
            BackBar(scene.title, onBack)
            Text("First tell me which camera and lens you have.", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenGear) { Text("Choose my camera") }
        }
        return
    }
    val crop = body.sensor.crop
    var lensId by rememberSaveable(scene.id) { mutableStateOf(Advisor.pickLens(bag, scene.lensPref, crop)?.id) }
    val lens = bag.firstOrNull { it.id == lensId } ?: bag.first()
    var focal by remember(scene.id, lens.id) { mutableIntStateOf(Advisor.focalFor(lens, scene.lensPref, crop)) }
    val rec = remember(scene.id, lens, focal, state.support, body) { Advisor.recommend(scene, body, lens, focal, state.support) }
    val photos = Photos.forScene(scene.id)
    val showHow = state.level != Level.PRO
    val settings = if (state.simple) rec.settings.filter { !it.advanced } else rec.settings

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar(scene.title, onBack) }
        if (photos.isNotEmpty()) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(photos, key = { it.id }) { p ->
                        PhotoCard(p, Modifier.width(300.dp), ratio = 1.4f, onClick = { onOpenPhoto(p.id) })
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("${scene.emoji}  ${scene.title}", style = MaterialTheme.typography.headlineSmall)
                Text(scene.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                SupportChips(state)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                SectionCard("Your settings", container = MaterialTheme.colorScheme.primaryContainer, icon = Icons.Filled.Tune) {
                    Text("For your ${body.name} with ${lens.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValueTile("Mode", rec.modeLabel.substringBefore(" ("), Modifier.weight(1f))
                        ValueTile("Aperture", rec.aperture.substringBefore(" ("), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValueTile("Shutter", rec.shutter.substringBefore(" ("), Modifier.weight(1f))
                        ValueTile("ISO", rec.iso, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onGuide, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guide me step by step", style = MaterialTheme.typography.titleSmall)
                    }
                }
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
        if (bag.size > 1 || lens.isZoom) {
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    if (bag.size > 1) {
                        Text("Lens", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            bag.forEach { l -> FilterChip(selected = l.id == lens.id, onClick = { lensId = l.id }, label = { Text(l.name) }) }
                        }
                    }
                    if (lens.isZoom) {
                        Text("Zoom: $focal mm", style = MaterialTheme.typography.labelLarge)
                        Slider(focal.toFloat(), { focal = it.roundToInt() }, valueRange = lens.minFocal.toFloat()..lens.maxFocal.toFloat())
                    }
                }
            }
        }
        item {
            SectionCard("All settings" + if (state.simple) " (simple)" else "", Modifier.padding(horizontal = 16.dp)) {
                KeyValue("Mode", rec.modeLabel, rec.modeWhy)
                if (showHow) Text("How: " + rec.modeHow, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                settings.forEach { s ->
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    KeyValue(s.name, s.value, s.why)
                    if (showHow && s.how.isNotBlank()) Text("How: " + s.how, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (state.simple && rec.settings.any { it.advanced }) {
                    Spacer(Modifier.height(8.dp))
                    Text("Some extra settings are hidden in Simple mode (More → Settings).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (rec.tips.isNotEmpty()) item {
            SectionCard("Tips", Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.secondaryContainer, icon = Icons.Filled.Lightbulb) { Bullets(rec.tips) }
        }
        if (rec.mistakes.isNotEmpty()) item {
            SectionCard("If it goes wrong", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.ReportProblem) { Bullets(rec.mistakes) }
        }
        if (photos.isNotEmpty()) {
            item { SectionLabel("How these photos were taken", Modifier.padding(horizontal = 20.dp)) }
            items(photos, key = { "exif_" + it.id }) { p ->
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(p.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "f/${fmtF(p.aperture)} · ${Exposure.formatShutter(p.shutter)} · ISO ${p.iso} · ${fmtFocal(p.focal)} mm · ${p.camera}",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { onOpenPhoto(p.id) }) { Text("Shoot like this with my camera →") }
                }
            }
        }
    }
}

fun fmtF(f: Double): String = if (f >= 10) f.roundToInt().toString() else String.format(java.util.Locale.US, "%.1f", f).removeSuffix(".0")
fun fmtFocal(f: Double): String = if (f == f.roundToInt().toDouble()) f.roundToInt().toString() else String.format(java.util.Locale.US, "%.1f", f)

// =============================================================================== step-by-step guide

private data class Step(val title: String, val value: String, val how: String, val why: String)

@Composable
fun GuideStepper(state: AppState, scene: Scene, onClose: () -> Unit) {
    val body = state.camera ?: return onClose()
    val bag = state.lenses
    if (bag.isEmpty()) return onClose()
    val crop = body.sensor.crop
    val lens = Advisor.pickLens(bag, scene.lensPref, crop)!!
    val focal = Advisor.focalFor(lens, scene.lensPref, crop)
    val rec = remember(scene.id, state.support, body, lens) { Advisor.recommend(scene, body, lens, focal, state.support) }
    val steps = remember(rec, state.simple) {
        val shown = if (state.simple) rec.settings.filter { !it.advanced } else rec.settings
        buildList {
            add(Step("Get ready", if (state.support == Support.TRIPOD) "Camera on the tripod" else "Camera in your hands",
                "Switch the camera on. Check the switch on the lens is on AF (autofocus), unless a step tells you otherwise.",
                "We'll set ${shown.size + 1} things, one at a time. Tap Next after each one."))
            add(Step("Set the mode", rec.modeLabel, rec.modeHow, rec.modeWhy))
            shown.forEach { s ->
                add(Step(s.name, s.value, s.how.ifBlank { "Set this on your camera." }, s.why))
            }
            add(Step("Take the photo", "Half-press, then press",
                "Press the shutter button halfway to focus, wait for the beep or green mark, then press it all the way.",
                "Then press ▶ and zoom in to check it's sharp. Too dark? Use + exposure compensation. Too bright? Use −."))
        }
    }
    var i by rememberSaveable(scene.id) { mutableIntStateOf(0) }
    val step = steps[i]
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "Close guide") }
            Text("${scene.emoji} ${scene.title}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${i + 1} / ${steps.size}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 12.dp))
        }
        LinearProgressIndicator(progress = { (i + 1f) / steps.size }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(6.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(3.dp)))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("STEP ${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(step.title, style = MaterialTheme.typography.headlineMedium)
            Box(Modifier.fillMaxWidth().clip(CardShape).background(MaterialTheme.colorScheme.primaryContainer).padding(20.dp)) {
                Text(step.value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
            }
            Text(step.how, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal))
            if (step.why.isNotBlank()) {
                Text("Why: " + step.why, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            SectionLabel("All steps")
            steps.forEachIndexed { n, st ->
                Row(
                    Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                        .background(if (n == i) MaterialTheme.colorScheme.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { i = n }.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (n < i) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                        Modifier.size(20.dp),
                        tint = if (n < i) androidx.compose.ui.graphics.Color(0xFF2E9E5B) else MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(st.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(st.value.substringBefore(" ("), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { if (i > 0) i-- else onClose() }, modifier = Modifier.weight(1f).height(56.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                Spacer(Modifier.width(6.dp))
                Text(if (i > 0) "Back" else "Close")
            }
            Button(onClick = { if (i < steps.lastIndex) i++ else onClose() }, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(if (i < steps.lastIndex) "Next" else "Done")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }
    }
}
