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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.camerasettings.app.R
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.Learn
import com.camerasettings.app.data.Lesson
import com.camerasettings.app.data.Problem
import com.camerasettings.app.data.Quiz
import com.camerasettings.app.logic.Exposure
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/** Routes in the Learn tab: null = home, "lesson:ID", "problem:ID", "problems", "glossary", "playground". */
@Composable
fun LearnScreen(state: AppState, route: String?, onRoute: (String?) -> Unit, onPractice: (String) -> Unit) {
    when {
        route?.startsWith("lesson:") == true -> Learn.lesson(route.removePrefix("lesson:"))?.let {
            LessonScreen(state, it, onBack = { onRoute(null) }, onNext = { id -> onRoute(if (id == null) null else "lesson:$id") }, onPractice = onPractice)
        }
        route?.startsWith("problem:") == true -> Learn.problems.firstOrNull { it.id == route.removePrefix("problem:") }?.let {
            ProblemScreen(state, it) { onRoute("problems") }
        }
        route == "problems" -> ProblemsList(onBack = { onRoute(null) }) { onRoute("problem:$it") }
        route == "glossary" -> GlossaryScreen(onBack = { onRoute(null) })
        route == "playground" -> Column(Modifier.fillMaxSize()) {
            BackBar("Exposure playground", onBack = { onRoute(null) })
            Column(Modifier.padding(16.dp)) { ExposurePlayground() }
        }
        else -> LearnHome(state, onRoute)
    }
}

@Composable
private fun LearnHome(state: AppState, onRoute: (String?) -> Unit) {
    val done = state.lessonsDone.count { id -> Learn.lessons.any { it.id == id } }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            ScreenTitle("Camera School", "Short lessons in plain language. Each one has something to try with your own camera.", eyebrow = "Learn")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(progress = { done.toFloat() / Learn.lessons.size }, modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(12.dp))
                Text("$done / ${Learn.lessons.size}", style = MaterialTheme.typography.labelLarge)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile(Icons.Filled.TouchApp, "Exposure playground", Modifier.weight(1f)) { onRoute("playground") }
                QuickTile(Icons.Filled.Build, "Fix a problem", Modifier.weight(1f)) { onRoute("problems") }
                QuickTile(Icons.Filled.MenuBook, "Glossary", Modifier.weight(1f)) { onRoute("glossary") }
            }
        }
        Learn.units.forEachIndexed { ui, unit ->
            item { SectionLabel("${ui + 1}. $unit") }
            Learn.lessons.filter { it.unit == unit }.forEach { l ->
                item(key = l.id) {
                    val isDone = l.id in state.lessonsDone
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { onRoute("lesson:${l.id}") }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isDone) Icons.Filled.CheckCircle else Icons.Filled.PlayCircle, null,
                            Modifier.size(30.dp),
                            tint = if (isDone) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(Learn.fill(l.title, state.brand), style = MaterialTheme.typography.titleSmall)
                            Text(Learn.fill(l.summary, state.brand), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("${l.minutes} min", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.secondaryContainer).clickable(onClick = onClick).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 2)
    }
}

@Composable
private fun LessonScreen(state: AppState, lesson: Lesson, onBack: () -> Unit, onNext: (String?) -> Unit, onPractice: (String) -> Unit) {
    val b = state.brand
    val idx = Learn.lessons.indexOf(lesson)
    val next = Learn.lessons.getOrNull(idx + 1)
    val answers = remember(lesson.id) { mutableStateMapOf<Int, Int>() }
    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackBar("Camera School · ${lesson.unit}", onBack) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("LESSON ${idx + 1} OF ${Learn.lessons.size} · ${lesson.minutes} MIN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(Learn.fill(lesson.title, b), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(Learn.fill(lesson.summary, b), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SectionCard(null, Modifier.padding(horizontal = 16.dp)) {
                lesson.sections.forEachIndexed { i, (h, t) ->
                    if (i > 0) HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Text(Learn.fill(h, b), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(Learn.fill(t, b), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        if (lesson.playground) item { Column(Modifier.padding(horizontal = 16.dp)) { ExposurePlayground() } }
        item {
            SectionCard("Try it with your camera", Modifier.padding(horizontal = 16.dp), container = MaterialTheme.colorScheme.secondaryContainer, icon = Icons.Filled.TouchApp) {
                NumberedSteps(lesson.tryIt.map { Learn.fill(it, b) })
                lesson.practiceScene?.let { sid ->
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { onPractice(sid) }) { Text("Open the matching guide →") }
                }
            }
        }
        item { SectionLabel("Quick check", Modifier.padding(horizontal = 20.dp)) }
        lesson.quiz.forEachIndexed { qi, q ->
            item(key = "q$qi") { QuizCard(q, b, answers[qi]) { answers[qi] = it } }
        }
        item {
            val allRight = lesson.quiz.indices.all { answers[it] == lesson.quiz[it].answer }
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { state.markLesson(lesson.id); onNext(next?.id) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Text(
                        when {
                            next == null -> "Finish Camera School 🎉"
                            allRight -> "Done! Next: ${Learn.fill(next.title, b)}"
                            else -> "Mark as done and continue"
                        },
                        maxLines = 1,
                    )
                }
                if (lesson.id in state.lessonsDone) {
                    Text("✓ You finished this lesson.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E9E5B))
                }
            }
        }
    }
}

@Composable
private fun QuizCard(q: Quiz, brand: com.camerasettings.app.data.Brand?, chosen: Int?, onChoose: (Int) -> Unit) {
    SectionCard(Learn.fill(q.question, brand), Modifier.padding(horizontal = 16.dp)) {
        q.options.forEachIndexed { i, o ->
            val isChosen = chosen == i
            val right = i == q.answer
            val bg = when {
                chosen == null -> MaterialTheme.colorScheme.surfaceContainerHigh
                isChosen && right -> Color(0xFFCFEFD9)
                isChosen -> Color(0xFFF7D4CF)
                right -> Color(0xFFCFEFD9).copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                    .clickable(enabled = chosen == null) { onChoose(i) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    when { chosen != null && right -> Icons.Filled.CheckCircle; else -> Icons.Filled.RadioButtonUnchecked }, null,
                    tint = if (chosen != null && right) Color(0xFF2E9E5B) else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Text(Learn.fill(o, brand), style = MaterialTheme.typography.bodyLarge, color = if (chosen == null) MaterialTheme.colorScheme.onSurface else Color(0xFF1F1A17))
            }
        }
        if (chosen != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                (if (chosen == q.answer) "Correct! " else "Not quite. ") + Learn.fill(q.explain, brand),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            )
        }
    }
}

// =============================================================================== problems & glossary

@Composable
private fun ProblemsList(onBack: () -> Unit, onOpen: (String) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { BackBar("Fix a problem", onBack) }
        item { Column(Modifier.padding(horizontal = 20.dp)) { ScreenTitle("What went wrong?", "Tap what you see in your photo.") } }
        Learn.problems.forEach { p ->
            item(key = p.id) {
                Row(
                    Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).clickable { onOpen(p.id) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(p.emoji, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(14.dp))
                    Text(p.title, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ProblemScreen(state: AppState, p: Problem, onBack: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackBar("Fix a problem", onBack) }
        item { Column(Modifier.padding(horizontal = 20.dp)) { ScreenTitle("${p.emoji} ${p.title}", "Find the description that matches your photo.") } }
        p.fixes.forEach { (cause, fix) ->
            item {
                SectionCard(Learn.fill(cause, state.brand), Modifier.padding(horizontal = 16.dp)) {
                    Text(Learn.fill(fix, state.brand), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun GlossaryScreen(onBack: () -> Unit) {
    var q by rememberSaveable { mutableStateOf("") }
    val items = Learn.glossary.filter { q.isBlank() || it.first.contains(q, true) || it.second.contains(q, true) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item { BackBar("Glossary", onBack) }
        item {
            OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search a word…") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        }
        items.forEach { (term, def) ->
            item(key = term) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(term, style = MaterialTheme.typography.titleSmall)
                    Text(def, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// =============================================================================== exposure playground

/**
 * Move aperture, shutter and ISO and see brightness, background blur, motion blur and grain
 * change on a real photo.
 */
@Composable
fun ExposurePlayground() {
    val apertures = listOf(1.8, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0)
    val shutters = listOf(1 / 2000.0, 1 / 1000.0, 1 / 500.0, 1 / 250.0, 1 / 125.0, 1 / 60.0, 1 / 30.0, 1 / 15.0)
    val isos = listOf(100, 200, 400, 800, 1600, 3200, 6400)
    var ai by rememberSaveable { mutableFloatStateOf(3f) }
    var si by rememberSaveable { mutableFloatStateOf(3f) }
    var ii by rememberSaveable { mutableFloatStateOf(1f) }
    val f = apertures[ai.roundToInt()]
    val t = shutters[si.roundToInt()]
    val iso = isos[ii.roundToInt()]
    // The scene is correctly exposed at the starting values f/5.6, 1/250 s, ISO 200.
    val ev = log2(f * f / t) - log2(iso / 100.0)
    val stopsOver = log2(5.6 * 5.6 * 250) - 1 - ev
    val brightness = 2.0.pow(stopsOver).toFloat()
    val bgBlur = ((8.0 / f - 1) * 3).coerceIn(0.0, 12.0)
    val motion = ((t * 250) - 1).coerceAtLeast(0.0)

    SectionCard("Exposure playground", container = MaterialTheme.colorScheme.surfaceContainerHigh, icon = Icons.Filled.TouchApp) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f).clip(RoundedCornerShape(14.dp))) {
            // Background (blurs with small f-numbers)
            Image(
                painterResource(R.drawable.photo_street_2), null,
                Modifier.fillMaxSize().blur(bgBlur.dp),
                contentScale = ContentScale.Crop,
                colorFilter = brightnessFilter(brightness),
            )
            // Subject: a cyclist dot blurred by slow shutter speeds
            Box(
                Modifier.align(Alignment.Center).size(width = (46 + motion * 40).dp.coerceAtMost(220.dp), height = 46.dp)
                    .clip(CircleShape).background(Color(0xFFE94F37).copy(alpha = (1f / (1f + motion.toFloat())).coerceIn(0.25f, 1f)))
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
            )
            if (iso >= 1600) {
                Box(Modifier.fillMaxSize().background(Color.Gray.copy(alpha = ((log2(iso / 800.0)) * 0.06).toFloat())))
            }
        }
        Spacer(Modifier.height(10.dp))
        val verdict = when {
            abs(stopsOver) < 0.5 -> "✅ Perfectly exposed"
            stopsOver > 0 -> "⚠️ Too bright (${"%.0f".format(stopsOver)} stop${if (stopsOver >= 1.5) "s" else ""} over)"
            else -> "⚠️ Too dark (${"%.0f".format(-stopsOver)} stop${if (-stopsOver >= 1.5) "s" else ""} under)"
        }
        Text(verdict, style = MaterialTheme.typography.titleMedium)
        Text(
            buildList {
                if (f <= 2.8) add("Background nicely blurred.")
                if (f >= 11) add("Everything sharp, background too.")
                if (motion > 0.5) add("The moving red ball is blurred: shutter too slow.") else add("Movement frozen.")
                if (iso >= 3200) add("High ISO: more grain.")
            }.joinToString(" "),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Text("Aperture  f/${fmtF(f)}", style = MaterialTheme.typography.labelLarge)
        Slider(ai, { ai = it }, valueRange = 0f..(apertures.size - 1f), steps = apertures.size - 2)
        Text("Shutter  ${Exposure.formatShutter(t)}", style = MaterialTheme.typography.labelLarge)
        Slider(si, { si = it }, valueRange = 0f..(shutters.size - 1f), steps = shutters.size - 2)
        Text("ISO  $iso", style = MaterialTheme.typography.labelLarge)
        Slider(ii, { ii = it }, valueRange = 0f..(isos.size - 1f), steps = isos.size - 2)
        Text("Challenge: blurred background AND frozen ball AND perfect exposure.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

private fun brightnessFilter(gain: Float): ColorFilter {
    val g = gain.coerceIn(0.12f, 4f)
    return ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                g, 0f, 0f, 0f, 0f,
                0f, g, 0f, 0f, 0f,
                0f, 0f, g, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )
}
