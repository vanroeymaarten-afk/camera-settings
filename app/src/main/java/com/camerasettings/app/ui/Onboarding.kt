package com.camerasettings.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.camerasettings.app.R
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Brand
import com.camerasettings.app.data.Gear
import com.camerasettings.app.data.Level

@Composable
fun Onboarding(state: AppState, onDone: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val total = 5
    when (step) {
        0 -> Welcome { step = 1 }
        else -> Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { step-- }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                LinearProgressIndicator(progress = { step.toFloat() / (total - 1) }, modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)))
                Spacer(Modifier.width(16.dp))
            }
            Box(Modifier.weight(1f)) {
                when (step) {
                    1 -> LevelStep(state) { step = 2 }
                    2 -> BrandStep(state) { step = 3 }
                    3 -> ModelStep(state) { step = if (state.camera?.fixedLens != null) 5 else 4 }
                    4 -> LensStep(state) { step = 5 }
                    else -> ReadyStep(state) { state.finishOnboarding(); onDone() }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onNext: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.photo_landscape_2), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.15f to Color.Transparent, 0.7f to Color.Black.copy(alpha = 0.88f))))
        Column(Modifier.align(Alignment.BottomStart).padding(28.dp).navigationBarsPadding()) {
            Text("CAMERA COACH", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFFC9A8))
            Spacer(Modifier.height(8.dp))
            Text("Take photos you're proud of.", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(
                "Tell me which camera you have. I'll tell you exactly which buttons to press for every situation, " +
                    "and teach you the basics along the way. No experience needed.",
                style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.88f),
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Let's start", style = MaterialTheme.typography.titleSmall) }
            Text("Photo: Krishansar Lake, Kashmir · Wikimedia Commons", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun StepScaffold(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun BigChoice(title: String, subtitle: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
            .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LevelStep(state: AppState, onNext: () -> Unit) {
    StepScaffold("How much do you know about cameras?", "Be honest: the app adapts how much it explains. You can change this later.") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Level.entries.forEach { l ->
                BigChoice(l.label, l.blurb, state.level == l) { state.selectLevel(l); onNext() }
            }
        }
    }
}

@Composable
private fun BrandStep(state: AppState, onNext: () -> Unit) {
    StepScaffold("Which brand is your camera?", "It's written on the front or the top of the camera.") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(Brand.entries) { b ->
                BigChoice(if (b == Brand.OTHER) "Other brand / I don't know" else b.label, null, state.brand == b) { state.selectBrand(b); onNext() }
            }
        }
    }
}

@Composable
private fun ModelStep(state: AppState, onNext: () -> Unit) {
    val brand = state.brand ?: Brand.OTHER
    val models = Gear.cameras.filter { it.brand == brand } + Gear.cameras.filter { it.brand == Brand.OTHER && brand != Brand.OTHER }
    StepScaffold(
        "Which model?",
        "Look for the name near the lens or on top, for example \"EOS R50\", \"Z50\" or \"A6400\". Not listed? Pick the sensor size option at the bottom.",
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(models, key = { it.id }) { c ->
                BigChoice(c.name, c.sensor.label, state.cameraId == c.id) { state.selectCamera(c.id); onNext() }
            }
        }
    }
}

@Composable
private fun LensStep(state: AppState, onNext: () -> Unit) {
    val lenses = Gear.lensesFor(state.camera)
    val mine = lenses.filter { it.mount != "any" }
    val generic = lenses.filter { it.mount == "any" }
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            StepScaffold("Which lenses do you have?", "Tick all you'll use. The name is printed on the lens, for example \"18-55mm\". Not sure? Pick the closest generic one.") {
                LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (mine.isNotEmpty()) item { SectionLabel("Lenses for your camera") }
                    items(mine, key = { it.id }) { l -> LensRow(state, l.id, l.name) }
                    item { SectionLabel("Generic (any brand)") }
                    items(generic, key = { it.id }) { l -> LensRow(state, l.id, l.name) }
                }
            }
        }
        Button(
            onClick = onNext, enabled = state.lenses.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        ) { Text(if (state.lenses.isEmpty()) "Tick at least one lens" else "Continue") }
    }
}

@Composable
private fun LensRow(state: AppState, id: String, name: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { state.toggleLens(id) }.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(id in state.lensIds, { state.toggleLens(id) })
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ReadyStep(state: AppState, onDone: () -> Unit) {
    StepScaffold("You're all set!", "Here's how the app works:") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                "📸  Home" to "Pick what you want to photograph. You get the exact settings, and a step-by-step guide that tells you which buttons to press.",
                "🎓  Learn" to "Camera School: short lessons with exercises. Start here if you're new. Also: a problem solver and a glossary.",
                "🖼️  Gallery" to "Beautiful photos with their real settings, and how to take them with your camera.",
                "🌅  Plan" to "Golden hour, sunset, moon and stars for any place in the world. Works offline.",
                "⚙️  More" to "Light meter, tools, your gear, bigger text and Simple mode.",
            ).forEach { (t, d) ->
                Column {
                    Text(t, style = MaterialTheme.typography.titleMedium)
                    Text(d, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Start taking photos") }
            if (state.level == Level.NEW) {
                Text("Tip: begin with Learn → Lesson 1 \"Meet your camera\" (3 minutes).", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
