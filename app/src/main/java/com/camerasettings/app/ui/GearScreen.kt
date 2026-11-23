package com.camerasettings.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Gear
import com.camerasettings.app.data.Lens

@Composable
fun GearScreen(state: AppState, onBack: () -> Unit, onDone: () -> Unit) {
    var cameraOpen by rememberSaveable { mutableStateOf(state.camera == null) }
    var showAllLenses by rememberSaveable { mutableStateOf(false) }
    var customOpen by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackBar("My gear", onBack) }
        item {
            ScreenTitle("What's in your bag?", "Pick your Canon camera and the lenses you're bringing. All advice is tailored to them.")
        }

        // ------------------------------------------------ camera
        item {
            SectionCard("1. Your Canon camera") {
                val cam = state.camera
                Row(
                    Modifier.fillMaxWidth().clickable { cameraOpen = !cameraOpen }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(cam?.name ?: "Tap to choose…", style = MaterialTheme.typography.titleMedium)
                        if (cam != null) {
                            Text(
                                "${cam.sensor.label} · ${cam.megapixels.toInt()} MP" +
                                    (if (cam.ibis) " · in-body stabilizer" else "") +
                                    (if (cam.animalAf) " · animal AF" else ""),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Icon(if (cameraOpen) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                }
                if (cameraOpen) {
                    Text(
                        "The model name is printed on the front of the camera, e.g. \"EOS R50\" or \"EOS 250D\".",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                    Gear.cameras.forEach { body ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                state.selectCamera(body.id)
                                cameraOpen = false
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = body.id == state.cameraId, onClick = {
                                state.selectCamera(body.id)
                                cameraOpen = false
                            })
                            Text(body.name)
                        }
                    }
                }
            }
        }

        // ------------------------------------------------ lenses
        item {
            SectionCard("2. Your lenses (tick all you'll bring)") {
                Text(
                    "The lens name is printed on the lens barrel or front ring, e.g. \"RF-S 18-45mm\" or \"EF-S 18-55mm\".",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = showAllLenses, onCheckedChange = { showAllLenses = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (showAllLenses) "Showing all lenses" else "Showing lenses for your camera")
                }
            }
        }
        val mounts = compatibleMounts(state.camera?.id, state.camera?.mirrorless, state.camera?.sensor?.crop)
        val shown = Gear.lenses.filter { showAllLenses || it.mount in mounts || it.id in state.lensIds }
        items(shown, key = { it.id }) { lens ->
            Row(
                Modifier.fillMaxWidth().clickable { state.toggleLens(lens.id) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = lens.id in state.lensIds, onCheckedChange = { state.toggleLens(lens.id) })
                Column {
                    Text(lens.name)
                    Text(lens.mount, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // ------------------------------------------------ custom lens
        item {
            HorizontalDivider()
            TextButton(onClick = { customOpen = !customOpen }) {
                Text(if (customOpen) "Hide custom lens" else "My lens isn't listed: add it")
            }
            if (customOpen) CustomLensForm(state.customLens) { state.saveCustomLens(it); customOpen = false }
            state.customLens?.let { c ->
                Row(Modifier.fillMaxWidth().clickable { state.toggleLens(c.id) }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = c.id in state.lensIds, onCheckedChange = { state.toggleLens(c.id) })
                    Text(c.name)
                }
            }
        }

        item {
            Spacer(Modifier.padding(4.dp))
            Button(onClick = onDone, enabled = state.hasGear, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (state.hasGear) "Done: show me settings" else "Pick a camera and at least one lens")
            }
        }
    }
}

private fun compatibleMounts(cameraId: String?, mirrorless: Boolean?, crop: Double?): Set<String> = when {
    cameraId == null -> setOf("RF", "RF-S", "EF", "EF-S", "EF-M", "Other", "Built-in")
    cameraId.startsWith("g") -> setOf("Built-in")
    cameraId.startsWith("m") -> setOf("EF-M", "EF", "EF-S", "Other")
    mirrorless == true -> setOf("RF", "RF-S", "EF", "EF-S", "Other") // EF/EF-S via adapter
    crop != null && crop > 1 -> setOf("EF", "EF-S", "Other")
    else -> setOf("EF", "Other")
}

@Composable
private fun CustomLensForm(existing: Lens?, onSave: (Lens) -> Unit) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var minF by remember { mutableStateOf(existing?.minFocal?.toString() ?: "") }
    var maxF by remember { mutableStateOf(existing?.maxFocal?.toString() ?: "") }
    var apW by remember { mutableStateOf(existing?.apWide?.toString() ?: "") }
    var apT by remember { mutableStateOf(existing?.apTele?.toString() ?: "") }
    var hasIs by remember { mutableStateOf(existing?.hasIs ?: true) }

    val num = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Read it off the lens: e.g. \"18-55mm 1:3.5-5.6 IS\" means 18 to 55 mm, f/3.5 to f/5.6, with stabilizer.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(name, { name = it }, label = { Text("Name (anything)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(minF, { minF = it }, label = { Text("Shortest mm") }, keyboardOptions = num, modifier = Modifier.weight(1f))
            OutlinedTextField(maxF, { maxF = it }, label = { Text("Longest mm") }, keyboardOptions = num, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(apW, { apW = it }, label = { Text("f/ at short end") }, keyboardOptions = num, modifier = Modifier.weight(1f))
            OutlinedTextField(apT, { apT = it }, label = { Text("f/ at long end") }, keyboardOptions = num, modifier = Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(hasIs, { hasIs = it })
            Text("Has image stabilizer (IS)")
        }
        val lo = minF.toIntOrNull()
        val hi = (maxF.toIntOrNull() ?: lo)
        val aw = apW.replace(',', '.').toDoubleOrNull()
        val at = apT.replace(',', '.').toDoubleOrNull() ?: aw
        val valid = lo != null && hi != null && hi >= lo && lo > 0 && aw != null && at != null && aw >= 0.95
        OutlinedButton(
            enabled = valid,
            onClick = {
                onSave(
                    Lens(
                        AppState.CUSTOM_LENS_ID,
                        name.ifBlank { "My lens $lo-${hi}mm" },
                        lo!!, hi!!, aw!!, at!!, hasIs, "Custom",
                    ),
                )
            },
        ) { Text("Save lens", fontWeight = FontWeight.SemiBold) }
    }
}
