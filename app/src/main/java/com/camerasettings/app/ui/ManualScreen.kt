package com.camerasettings.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.camerasettings.app.data.AppState
import com.camerasettings.app.data.CameraManual
import com.camerasettings.app.data.ManualTopic
import com.camerasettings.app.data.Manuals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

private fun manualFile(ctx: Context, cameraId: String?) = File(ctx.filesDir, "manual_${cameraId ?: "camera"}.pdf")

@Composable
fun ManualScreen(state: AppState, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val body = state.camera
    val manual = Manuals.forCamera(body?.id)
    var reader by rememberSaveable { mutableStateOf(false) }
    var hasPdf by remember { mutableStateOf(manualFile(ctx, body?.id).exists()) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openInputStream(uri)!!.use { input -> manualFile(ctx, body?.id).outputStream().use { input.copyTo(it) } }
                    // Check it really is a PDF we can open.
                    ParcelFileDescriptor.open(manualFile(ctx, body?.id), ParcelFileDescriptor.MODE_READ_ONLY).use { PdfRenderer(it).close() }
                }.isSuccess
            }
            if (ok) { hasPdf = true; reader = true; message = null }
            else { manualFile(ctx, body?.id).delete(); hasPdf = false; message = "That file couldn't be opened as a PDF." }
        }
    }

    if (reader && hasPdf) {
        BackHandler { reader = false }
        PdfReader(manualFile(ctx, body?.id), onBack = { reader = false })
        return
    }

    var query by rememberSaveable { mutableStateOf("") }
    val topics = manual?.topics?.filter {
        query.isBlank() || it.title.contains(query, true) || it.group.contains(query, true) || it.steps.any { s -> s.contains(query, true) }
    }.orEmpty()

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BackBar("Camera manual", onBack) }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                ScreenTitle(body?.let { "${it.brand.label} ${it.name}" } ?: "Your camera", if (manual != null) "How do I…? Short steps for your camera, plus the official manual." else "Add the official manual of your camera to read it here, offline.")
            }
        }
        // ---------------------------------------------------------------- official manual
        item {
            SectionCard("Official manual (PDF)", Modifier.padding(horizontal = 16.dp), icon = Icons.AutoMirrored.Filled.MenuBook) {
                if (hasPdf) {
                    Text("Saved in the app. Works offline.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { reader = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Open the manual") }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { picker.launch(arrayOf("application/pdf")) }) { Text("Replace with another PDF") }
                } else {
                    Text(
                        "Canon publishes the full manual for free. Add it once and read it here, also without internet:",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    NumberedSteps(
                        listOf(
                            "Tap 'Download from ${body?.brand?.label ?: "the maker"}', search for your model (${body?.name ?: "your camera"}) and your language, and download the PDF.",
                            "Come back here and tap 'Add the PDF', then choose the file you downloaded (usually in Downloads).",
                        ),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = {
                            runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(manual?.officialUrl ?: Manuals.officialUrl(body?.brand)))) }
                        }) {
                            Icon(Icons.Filled.Download, null); Spacer(Modifier.width(6.dp)); Text("Download from ${body?.brand?.label ?: "the maker"}")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(onClick = { picker.launch(arrayOf("application/pdf")) }) {
                        Icon(Icons.Filled.FileOpen, null); Spacer(Modifier.width(6.dp)); Text("Add the PDF")
                    }
                }
                message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
        if (manual != null) {
            item {
                OutlinedTextField(
                    query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("How do I… (e.g. self-timer, ISO, Wi-Fi)") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, shape = RoundedCornerShape(16.dp),
                )
            }
            if (query.isBlank()) item { ButtonsCard(manual) }
            topics.groupBy { it.group }.forEach { (group, list) ->
                item(key = "g_$group") { SectionLabel(group, Modifier.padding(horizontal = 20.dp)) }
                list.forEach { t -> item(key = t.id) { TopicCard(t) } }
            }
            if (topics.isEmpty()) item { Text("Nothing found. Try another word, or search the official manual.", Modifier.padding(20.dp)) }
            item {
                Text(
                    "These short guides were written for this app. For every detail, see the official manual.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun ButtonsCard(manual: CameraManual) {
    var open by rememberSaveable { mutableStateOf(false) }
    SectionCard("Where are the buttons?", Modifier.padding(horizontal = 16.dp)) {
        val list = if (open) manual.buttons else manual.buttons.take(4)
        list.forEachIndexed { i, (k, v) ->
            if (i > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(k, style = MaterialTheme.typography.titleSmall)
            Text(v, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        androidx.compose.material3.TextButton(onClick = { open = !open }) { Text(if (open) "Show less" else "Show all ${manual.buttons.size} buttons") }
    }
}

@Composable
private fun TopicCard(t: ManualTopic) {
    var open by rememberSaveable(t.id) { mutableStateOf(false) }
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(16.dp))
            .clickable { open = !open }.padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(if (open) "–" else "+", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        if (open) {
            Spacer(Modifier.height(10.dp))
            NumberedSteps(t.steps)
            if (t.tip.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Tip: " + t.tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// =============================================================================== PDF reader

/** Simple offline PDF reader based on Android's built-in PdfRenderer. */
@Composable
private fun PdfReader(file: File, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val renderer = remember(file) {
        runCatching { PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)) }.getOrNull()
    }
    DisposableEffect(renderer) { onDispose { renderer?.close() } }
    if (renderer == null) {
        Column(Modifier.padding(24.dp)) { BackBar("Manual", onBack); Text("The manual couldn't be opened.") }
        return
    }
    val lock = remember { Mutex() }
    val count = renderer.pageCount
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var zoom by rememberSaveable { mutableIntStateOf(1) }
    var pageInput by rememberSaveable { mutableStateOf("") }
    val widthPx = ctx.resources.displayMetrics.widthPixels

    Column(Modifier.fillMaxSize()) {
        BackBar("Manual · ${list.firstVisibleItemIndex + 1} / $count", onBack) {
            IconButton(onClick = { zoom = if (zoom == 1) 2 else 1 }) { Icon(if (zoom == 1) Icons.Filled.ZoomIn else Icons.Filled.ZoomOut, "Zoom") }
        }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                pageInput, { pageInput = it.filter(Char::isDigit).take(4) }, Modifier.width(140.dp),
                placeholder = { Text("Page") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(onClick = {
                pageInput.toIntOrNull()?.let { p -> scope.launch { list.scrollToItem((p - 1).coerceIn(0, count - 1)) } }
            }) { Text("Go") }
            Spacer(Modifier.width(8.dp))
            Text("Tip: the table of contents is on the first pages.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(state = list, modifier = Modifier.weight(1f), contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0 until count).toList(), key = { it }) { index ->
                val bitmap by produceState<Bitmap?>(null, index, zoom) {
                    value = withContext(Dispatchers.IO) {
                        lock.withLock {
                            renderer.openPage(index).use { page ->
                                val w = (widthPx * zoom).coerceAtMost(2600)
                                val h = (w.toFloat() * page.height / page.width).toInt()
                                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
                                    it.eraseColor(AColor.WHITE)
                                    page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                }
                            }
                        }
                    }
                }
                val b = bitmap
                Box(Modifier.fillMaxWidth().then(if (zoom > 1) Modifier.horizontalScroll(rememberScrollState()) else Modifier)) {
                    if (b == null) {
                        Box(Modifier.fillMaxWidth().aspectRatio(0.7f).background(Color.White))
                    } else {
                        Image(
                            b.asImageBitmap(), "Page ${index + 1}",
                            if (zoom > 1) Modifier.width((ctx.resources.configuration.screenWidthDp * zoom).dp) else Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                        )
                    }
                }
            }
        }
    }
}
