package com.camerasettings.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.camerasettings.app.BuildConfig
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Checklist
import com.camerasettings.app.data.Shots
import com.camerasettings.app.data.Software
import com.camerasettings.app.data.Storage
import com.camerasettings.app.logic.Advisor
import com.camerasettings.app.logic.EditPreset
import com.camerasettings.app.logic.Presets
import java.util.Locale
import kotlin.math.roundToInt

/** Routes inside the More tab. */
object MoreRoutes {
    const val GEAR = "gear"
    const val FILES = "files"
    const val EDITING = "editing"
    const val CHECKLIST = "checklist"
    const val BASICS = "basics"
    const val ABOUT = "about"
    fun preset(id: String) = "preset:$id"
}

@Composable
fun MoreScreen(state: AppState, route: String?, onRoute: (String?) -> Unit, onGearDone: () -> Unit) {
    val back: () -> Unit = {
        onRoute(if (route?.startsWith("preset:") == true) MoreRoutes.EDITING else null)
    }
    if (route != null) BackHandler(onBack = back)
    when {
        route == MoreRoutes.GEAR -> GearScreen(state, onBack = back, onDone = onGearDone)
        route == MoreRoutes.FILES -> FilesScreen(state, back)
        route == MoreRoutes.EDITING -> EditingScreen(back) { onRoute(MoreRoutes.preset(it)) }
        route?.startsWith("preset:") == true -> PresetScreen(Presets.byId(route.removePrefix("preset:")), back)
        route == MoreRoutes.CHECKLIST -> ChecklistScreen(state, back)
        route == MoreRoutes.BASICS -> BasicsScreen(back)
        route == MoreRoutes.ABOUT -> AboutScreen(back)
        else -> MoreHome(state, onRoute)
    }
}

@Composable
private fun MoreHome(state: AppState, onRoute: (String) -> Unit) {
    val done = Checklist.allIds.count { it in state.checked }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { ScreenTitle("More", "Your gear, files, editing and trip preparation.", eyebrow = "Camera Coach") }
        item { Spacer(Modifier.height(8.dp)) }
        item { NavRow(Icons.Filled.Backpack, "My gear", state.camera?.let { "${it.name} · ${state.lenses.size} lenses" } ?: "Choose your camera and lenses") { onRoute(MoreRoutes.GEAR) } }
        item { NavRow(Icons.Filled.SdCard, "Files & storage", "RAW vs JPEG, ${state.camera?.rawFormat ?: "CR3"} files, how many cards to bring") { onRoute(MoreRoutes.FILES) } }
        item { NavRow(Icons.Filled.AutoFixHigh, "Editing & presets", "Software, workflow and 7 warm Lightroom looks") { onRoute(MoreRoutes.EDITING) } }
        item { NavRow(Icons.Filled.Checklist, "Trip checklist", "$done of ${Checklist.allIds.size} done") { onRoute(MoreRoutes.CHECKLIST) } }
        item { NavRow(Icons.Filled.MenuBook, "Camera basics", "Av, Tv, M, ISO, histogram… in one minute") { onRoute(MoreRoutes.BASICS) } }
        item { NavRow(Icons.Filled.Info, "About", "Version ${BuildConfig.VERSION_NAME}") { onRoute(MoreRoutes.ABOUT) } }
    }
}

// =============================================================================== files

@Composable
private fun FilesScreen(state: AppState, onBack: () -> Unit) {
    val body = state.camera
    var perDay by rememberSaveable { mutableStateOf(state.shotsPerDay.toFloat()) }
    var days by rememberSaveable { mutableStateOf(state.tripDays.toFloat()) }
    var withJpeg by rememberSaveable { mutableStateOf(true) }
    var craw by rememberSaveable { mutableStateOf(false) }
    val fmt = body?.rawFormat ?: "CR3"

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar("Files & storage", onBack) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ScreenTitle("Shoot RAW + JPEG", "The single most important setting for editing your photos later.")
            }
        }
        item {
            SectionCard("What's the difference?", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.Storage) {
                KeyValue("JPEG (.JPG)", "Finished photo, small, shareable", "The camera has already decided colours, contrast and white balance and thrown the rest away. Perfect for sending to your phone during the trip.")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                KeyValue("RAW (.$fmt)", "Everything the sensor saw", "Canon's RAW format${body?.let { " on your ${it.name}" } ?: ""}. It looks flat and dull at first, but holds far more detail in bright skies and dark shadows, and white balance can be changed freely. This is what you'll edit for warmth and feel after the trip.")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                KeyValue("Set it on your camera", "Menu (red camera tab) → Image quality → RAW + L", if (body?.craw == true) "Your camera also offers C-RAW (compressed RAW): about 40% smaller files with almost no visible difference. A good choice if you're short on cards." else "Use the large, fine JPEG (L) alongside RAW.")
            }
        }
        if (body != null) {
            item {
                SectionCard("How much space do you need?", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.SdCard) {
                    Text("Photos per day: ${perDay.roundToInt()}", style = MaterialTheme.typography.labelLarge)
                    Slider(perDay, { perDay = (it / 25).roundToInt() * 25f }, valueRange = 25f..800f, onValueChangeFinished = { state.setStoragePlan(perDay.roundToInt(), days.roundToInt()) })
                    Text("Days: ${days.roundToInt()}", style = MaterialTheme.typography.labelLarge)
                    Slider(days, { days = it.roundToInt().toFloat() }, valueRange = 3f..45f, onValueChangeFinished = { state.setStoragePlan(perDay.roundToInt(), days.roundToInt()) })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = withJpeg, onClick = { withJpeg = !withJpeg }, label = { Text("+ JPEG") })
                        if (body.craw) FilterChip(selected = craw, onClick = { craw = !craw }, label = { Text("C-RAW") })
                    }
                    val est = Storage.estimate(body, perDay.roundToInt(), days.roundToInt(), withJpeg, craw)
                    Spacer(Modifier.height(10.dp))
                    Text(String.format(Locale.US, "≈ %.0f GB", est.gbTotal), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                    Text(String.format(Locale.US, "≈ %.0f MB per photo · %s", est.perShotMb, est.cards), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Use several smaller cards rather than one huge one: a lost or broken card then costs you days, not the whole trip.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionCard("Back up every evening", Modifier.padding(horizontal = 16.dp), icon = Icons.Filled.Download) {
                NumberedSteps(
                    listOf(
                        "Keep two copies of every photo, on two different devices, before you re-use a card.",
                        "Easiest: a laptop or a portable SSD with a card reader. With a phone, use a USB-C card reader to copy the folder.",
                        "Camera Connect can send JPEGs to your phone over Wi-Fi, a handy extra copy of your favourites.",
                        "Upload to cloud storage (Google Photos, Drive, iCloud) when you have good Wi-Fi at a hotel.",
                    ),
                )
            }
        }
        item { AfterTripCard(Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
fun AfterTripCard(modifier: Modifier = Modifier) {
    SectionCard("After the trip: editing with Claude", modifier, container = MaterialTheme.colorScheme.secondaryContainer, icon = Icons.Filled.SmartToy) {
        val c = MaterialTheme.colorScheme.onSecondaryContainer
        Text("When you're home, I can help give your photos warmth and feel. To make that easy:", style = MaterialTheme.typography.bodyMedium, color = c)
        Spacer(Modifier.height(10.dp))
        NumberedSteps(
            listOf(
                "Keep the original RAW files (.CR3 / .CR2) untouched. Don't convert or delete them.",
                "Pick your favourites first (for example 20–50 photos). Star them in Lightroom or copy them to one folder.",
                "Start a new Claude Code session and share that folder: connect your computer, or upload the files.",
                "Tell me the feel you want (\"warm Atacama gold\", \"soft and moody\") or point to a look in this app.",
                "I can batch-develop the RAWs with open-source tools, fine-tune these Lightroom presets to your taste, and export JPEGs for printing, Instagram (4:5) or a photo book.",
            ),
        )
    }
}

// =============================================================================== editing

@Composable
private fun EditingScreen(onBack: () -> Unit, onOpenPreset: (String) -> Unit) {
    val ctx = LocalContext.current
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar("Editing & presets", onBack) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ScreenTitle("Warmth & feel", "RAW files are meant to be finished on a computer. These recipes turn them into the warm, rich look of the Shot guide.")
            }
        }
        item {
            SectionCard("The 5-step workflow", Modifier.padding(horizontal = 16.dp)) {
                NumberedSteps(
                    listOf(
                        "Import the RAW files into your editor (Lightroom, or free: Canon DPP / darktable).",
                        "Pick the best shot of each scene. Be strict: one great photo beats five similar ones.",
                        "Apply the matching look below (one click with the Lightroom presets).",
                        "Fine-tune per photo: white balance, exposure, straighten the horizon, crop.",
                        "Export as JPEG, sRGB, quality 85–90%. Long edge 2048 px for sharing, full size for prints.",
                    ),
                )
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Looks", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { Share.allPresets(ctx) }) {
                    Icon(Icons.Filled.Share, null)
                    Spacer(Modifier.width(6.dp))
                    Text("All presets (.zip)")
                }
            }
        }
        Presets.all.forEach { p ->
            item(key = p.id) {
                val example = Shots.all.firstOrNull { it.presetId == p.id }
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(CardShape).clickable { onOpenPreset(p.id) }
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (example != null) {
                        Image(
                            painterResource(Art[example.id]!!.thumb), null,
                            Modifier.size(width = 96.dp, height = 64.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(Modifier.width(14.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.titleSmall)
                        Text(p.useFor, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { SoftwareCard(Modifier.padding(horizontal = 16.dp)) }
        item { AfterTripCard(Modifier.padding(horizontal = 16.dp)) }
    }
}

@Composable
private fun SoftwareCard(modifier: Modifier) {
    SectionCard("Which software?", modifier) {
        Software.apps.forEachIndexed { i, a ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(a.name, style = MaterialTheme.typography.titleSmall)
            Text("${a.platform} · ${a.price}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(a.bestFor, style = MaterialTheme.typography.bodyMedium)
            val flags = listOfNotNull(
                if (a.readsRaw) "Reads RAW" else "JPEG only",
                if (a.usesPresets) "Imports the presets" else null,
            ).joinToString(" · ")
            Text("$flags. ${a.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Recommendation: Lightroom Classic if you'll edit regularly; Canon DPP 4 (free) if you just want to try.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PresetScreen(p: EditPreset, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val examples = Shots.all.filter { it.presetId == p.id }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar("Look", onBack) }
        examples.firstOrNull()?.let { ex ->
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    BeforeAfter(Art[ex.id]!!.before, Art[ex.id]!!.after)
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("Chile · ${p.name}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(p.tagline, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(4.dp))
                Text("Use for: ${p.useFor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { Share.preset(ctx, p) }) {
                    Icon(Icons.Filled.Share, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Share Lightroom preset (.xmp)")
                }
            }
        }
        item {
            SectionCard("Recipe (Lightroom panel by panel)", Modifier.padding(horizontal = 16.dp)) {
                p.steps().forEachIndexed { i, (k, v) ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    KeyValue(k, v)
                }
            }
        }
        item {
            SectionCard("Installing the preset", Modifier.padding(horizontal = 16.dp)) {
                Bullets(
                    listOf(
                        "Send the file to your computer (e-mail, Drive, WhatsApp…).",
                        "Lightroom Classic: Develop module → Presets panel → + → Import Presets → pick the .xmp or .zip.",
                        "Lightroom (cloud/desktop): Edit → Presets → ⋯ → Import Presets. It then syncs to Lightroom on your phone.",
                        "Photoshop / Camera Raw: open Camera Raw → Presets → ⋯ → Import Profiles & Presets.",
                        "Other editors: follow the recipe above by hand; the names are nearly identical everywhere.",
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text("A preset is a starting point. Always adjust white balance and exposure per photo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// =============================================================================== checklist

@Composable
private fun ChecklistScreen(state: AppState, onBack: () -> Unit) {
    val total = Checklist.allIds.size
    val done = Checklist.allIds.count { it in state.checked }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { BackBar("Trip checklist", onBack) }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                ScreenTitle("$done of $total ready", "Ticks are saved on your phone.")
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            }
        }
        Checklist.groups.forEach { g ->
            item(key = g.title) { SectionLabel(g.title, Modifier.padding(horizontal = 20.dp)) }
            g.items.forEach { it ->
                item(key = it.id) {
                    Row(
                        Modifier.fillMaxWidth().clickable { state.toggleChecked(it.id) }.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = it.id in state.checked, onCheckedChange = { _ -> state.toggleChecked(it.id) })
                        Column(Modifier.weight(1f)) {
                            Text(it.text, style = MaterialTheme.typography.bodyLarge)
                            if (it.detail.isNotBlank()) Text(it.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================== basics & about

@Composable
private fun BasicsScreen(onBack: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackBar("Camera basics", onBack) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ScreenTitle("Camera basics in one minute", "Everything the Shoot tab talks about, explained simply.")
            }
        }
        item {
            SectionCard(null, Modifier.padding(horizontal = 16.dp)) {
                Advisor.basics.forEachIndexed { i, (term, text) ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(term, style = MaterialTheme.typography.titleSmall)
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            SectionCard("The exposure triangle", Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.secondaryContainer) {
                Text(
                    "Aperture, shutter speed and ISO all control brightness. Change one by a 'stop' and you must change another " +
                        "by a stop the other way to keep the same brightness. Example: f/8 · 1/250 s · ISO 100 is just as bright as " +
                        "f/5.6 · 1/500 s · ISO 100, but with a blurrier background and sharper motion.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackBar("About", onBack) }
        item {
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().aspectRatio(2f).clip(CardShape)) {
                Image(painterResource(com.camerasettings.app.R.drawable.ex_osorno_after), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ScreenTitle("Camera Coach · Chile", "Version ${BuildConfig.VERSION_NAME}")
            }
        }
        item {
            SectionCard(null, Modifier.padding(horizontal = 16.dp)) {
                Bullets(
                    listOf(
                        "Works completely offline: settings, sun, moon and star times are all calculated on the phone.",
                        "Sun and moon positions use standard low-precision astronomical formulas (accurate to a minute or two).",
                        "Settings are sensible starting points. Always check the result on the screen and histogram, and adjust.",
                        "All illustrations were painted by code for this app. They are not photographs.",
                        "Typeface: Inter by Rasmus Andersson, SIL Open Font License 1.1.",
                        "Not affiliated with Canon or Adobe. Canon, EOS, PowerShot, Lightroom and Photoshop are trademarks of their owners.",
                    ),
                )
            }
        }
    }
}
