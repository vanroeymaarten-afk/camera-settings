package com.camerasettings.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Photo
import com.camerasettings.app.data.Photos
import com.camerasettings.app.logic.Exposure
import com.camerasettings.app.logic.Recreate
import com.camerasettings.app.logic.Scenes
import kotlin.math.roundToInt

@Composable
fun GalleryScreen(state: AppState, photoId: String?, onOpen: (String?) -> Unit, onOpenScene: (String) -> Unit, onOpenGear: () -> Unit) {
    val photo = Photos.byId(photoId)
    if (photo != null) PhotoDetail(state, photo, onBack = { onOpen(null) }, onOpenScene = onOpenScene, onOpenGear = onOpenGear)
    else GalleryGrid(onOpen)
}

@Composable
private fun GalleryGrid(onOpen: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    val photos = Photos.all.filter { filter == null || it.sceneId == filter }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp,
    ) {
        item(span = StaggeredGridItemSpan.FullLine) {
            Column {
                ScreenTitle(
                    "Inspiration",
                    "${Photos.all.size} award-winning photos, voted 'Featured' or 'Quality' by the Wikimedia Commons community. Tap one to see exactly how it was taken, and how to get it with your camera.",
                    eyebrow = "Gallery",
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                    Scenes.all.filter { s -> Photos.all.any { it.sceneId == s.id } }.forEach { s ->
                        FilterChip(selected = filter == s.id, onClick = { filter = if (filter == s.id) null else s.id }, label = { Text("${s.emoji} ${s.title}") })
                    }
                }
            }
        }
        items(photos, key = { it.id }) { p ->
            Column(Modifier.clip(CardShape).clickable { onOpen(p.id) }) {
                Image(
                    painterResource(p.res), p.title,
                    Modifier.fillMaxWidth().aspectRatio(p.width.toFloat() / p.height).clip(CardShape),
                    contentScale = ContentScale.Crop,
                )
                Text(p.title, style = MaterialTheme.typography.labelLarge, maxLines = 2, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
                Text(
                    "f/${fmtF(p.aperture)} · ${Exposure.formatShutter(p.shutter)} · ISO ${p.iso}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun PhotoDetail(state: AppState, p: Photo, onBack: () -> Unit, onOpenScene: (String) -> Unit, onOpenGear: () -> Unit) {
    val ctx = LocalContext.current
    val scene = Scenes.byId(p.sceneId)
    val body = state.camera
    val plan = remember(p.id, body, state.lenses) { body?.let { Recreate.plan(p, it, state.lenses) } }
    val eq = Recreate.equivalentFocal(p).roundToInt()

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar(scene?.title ?: "Photo", onBack) }
        item { PhotoCard(p, Modifier.padding(horizontal = 16.dp), showCredit = false) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(p.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Pill("⭐ ${p.rating} on Wikimedia Commons")
            }
        }
        item {
            SectionCard("How it was taken", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.PhotoCamera) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueTile("Aperture", "f/${fmtF(p.aperture)}", Modifier.weight(1f))
                    ValueTile("Shutter", Exposure.formatShutter(p.shutter), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ValueTile("ISO", "${p.iso}", Modifier.weight(1f))
                    ValueTile("Zoom", "${fmtFocal(p.focal)} mm" + if (eq.toDouble() != p.focal) " (≈$eq)" else "", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Text("Camera: ${p.camera}" + if (p.lens.isNotBlank()) "\nLens: ${p.lens}" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(explain(p), style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard("Shoot like this with your camera", Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.primaryContainer, icon = Icons.Filled.CameraAlt) {
                val c = MaterialTheme.colorScheme.onPrimaryContainer
                if (body == null || plan == null) {
                    Text("Tell me your camera and lens to translate these settings.", color = c)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenGear) { Text("Choose my camera") }
                } else {
                    Text("Your ${body.name} with ${plan.lens.name}:", style = MaterialTheme.typography.bodySmall, color = c)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValueTile("Mode", "M", Modifier.weight(1f))
                        ValueTile("Zoom", "${plan.focal} mm", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValueTile("Aperture", "f/${fmtF(plan.aperture)}", Modifier.weight(1f))
                        ValueTile("Shutter", Exposure.formatShutter(plan.shutter), Modifier.weight(1f))
                        ValueTile("ISO", "${plan.iso}", Modifier.weight(1f))
                    }
                    if (plan.notes.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        plan.notes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, color = c) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "These are the photographer's exact settings for that moment's light. In different light, start from them and use the screen and histogram to adjust, or use the step-by-step guide below.",
                        style = MaterialTheme.typography.bodySmall, color = c,
                    )
                }
            }
        }
        if (scene != null) item {
            Button(onClick = { onOpenScene(scene.id) }, modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(52.dp)) {
                Text("Open the ${scene.title} guide")
            }
        }
        item {
            SectionCard("Credit & license", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.Info) {
                Text("Photo: ${p.author}", style = MaterialTheme.typography.bodyMedium)
                Text("License: ${p.license}", style = MaterialTheme.typography.bodyMedium)
                Text("Resized for this app. Settings read from the photo's EXIF data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    TextButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.source))) } }) { Text("View original") }
                    if (p.licenseUrl.isNotBlank()) TextButton(onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(p.licenseUrl))) } }) { Text("License") }
                }
            }
        }
    }
}

/** Explain in plain words why the photographer chose these settings. */
private fun explain(p: Photo): String {
    val parts = mutableListOf<String>()
    parts += when {
        p.shutter >= 1.0 -> "A long shutter time (${Exposure.formatShutter(p.shutter)}) turned movement into smooth streaks; that needs a tripod."
        p.shutter >= 1 / 30.0 -> "A slow-ish shutter (${Exposure.formatShutter(p.shutter)}): fine for still scenes, often with something to lean on."
        p.shutter <= 1 / 1000.0 -> "A very fast shutter (${Exposure.formatShutter(p.shutter)}) froze every bit of movement."
        else -> "A normal handheld shutter speed (${Exposure.formatShutter(p.shutter)})."
    }
    parts += when {
        p.aperture <= 2.8 -> "The wide aperture (f/${fmtF(p.aperture)}) let in lots of light" + if (p.shutter < 1.0) " and blurred the background." else "."
        p.aperture >= 8 -> "f/${fmtF(p.aperture)} kept everything sharp from front to back."
        else -> "A middle aperture (f/${fmtF(p.aperture)}) balanced sharpness and light."
    }
    parts += when {
        p.iso <= 200 -> "Low ISO ${p.iso} gave the cleanest possible image."
        p.iso >= 3200 -> "High ISO ${p.iso} made it possible in very little light (grain is the price)."
        else -> "ISO ${p.iso} was raised a little to keep the shutter fast enough."
    }
    return parts.joinToString(" ")
}
