package com.camerasettings.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.camerasettings.app.R

// =========================================================================== theme

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val Light = lightColorScheme(
    primary = Color(0xFFB8471F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF3A0B00),
    secondary = Color(0xFF34407A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE1FF),
    onSecondaryContainer = Color(0xFF131C4A),
    tertiary = Color(0xFF8A5A00),
    tertiaryContainer = Color(0xFFFFE3B0),
    onTertiaryContainer = Color(0xFF2C1900),
    background = Color(0xFFFCF8F5),
    onBackground = Color(0xFF1F1A17),
    surface = Color(0xFFFCF8F5),
    onSurface = Color(0xFF1F1A17),
    surfaceVariant = Color(0xFFF2E6DF),
    onSurfaceVariant = Color(0xFF5A4C45),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F1EC),
    surfaceContainer = Color(0xFFF3EBE5),
    surfaceContainerHigh = Color(0xFFEDE4DE),
    surfaceContainerHighest = Color(0xFFE7DED8),
    outline = Color(0xFF8D7B72),
    outlineVariant = Color(0xFFE0D2CA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB596),
    onPrimary = Color(0xFF5A1B00),
    primaryContainer = Color(0xFF8A3210),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFBAC3FF),
    onSecondary = Color(0xFF1F2A60),
    secondaryContainer = Color(0xFF2F3A73),
    onSecondaryContainer = Color(0xFFDDE1FF),
    tertiary = Color(0xFFF7BD52),
    tertiaryContainer = Color(0xFF5F4100),
    onTertiaryContainer = Color(0xFFFFE3B0),
    background = Color(0xFF14121A),
    onBackground = Color(0xFFEDE6E1),
    surface = Color(0xFF14121A),
    onSurface = Color(0xFFEDE6E1),
    surfaceVariant = Color(0xFF2A2630),
    onSurfaceVariant = Color(0xFFCFC3BC),
    surfaceContainerLowest = Color(0xFF0F0D14),
    surfaceContainerLow = Color(0xFF1C1A22),
    surfaceContainer = Color(0xFF211E27),
    surfaceContainerHigh = Color(0xFF2B2832),
    surfaceContainerHighest = Color(0xFF36323D),
    outline = Color(0xFF9A8E88),
    outlineVariant = Color(0xFF3E3842),
)

private fun appTypography(): Typography {
    val base = Typography()
    fun TextStyle.inter(weight: FontWeight? = null, tracking: Float? = null) =
        copy(fontFamily = Inter, fontWeight = weight ?: fontWeight, letterSpacing = tracking?.sp ?: letterSpacing)
    return Typography(
        displayLarge = base.displayLarge.inter(FontWeight.Bold, -1f),
        displayMedium = base.displayMedium.inter(FontWeight.Bold, -0.8f),
        displaySmall = base.displaySmall.inter(FontWeight.Bold, -0.5f),
        headlineLarge = base.headlineLarge.inter(FontWeight.Bold, -0.6f),
        headlineMedium = base.headlineMedium.inter(FontWeight.Bold, -0.5f),
        headlineSmall = base.headlineSmall.inter(FontWeight.Bold, -0.3f),
        titleLarge = base.titleLarge.inter(FontWeight.SemiBold, -0.2f),
        titleMedium = base.titleMedium.inter(FontWeight.SemiBold, -0.1f),
        titleSmall = base.titleSmall.inter(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.inter(),
        bodyMedium = base.bodyMedium.inter(),
        bodySmall = base.bodySmall.inter(),
        labelLarge = base.labelLarge.inter(FontWeight.SemiBold),
        labelMedium = base.labelMedium.inter(FontWeight.SemiBold, 0.3f),
        labelSmall = base.labelSmall.inter(FontWeight.SemiBold, 0.6f),
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = remember { appTypography() },
        content = content,
    )
}

// =========================================================================== building blocks

val CardShape = RoundedCornerShape(20.dp)

@Composable
fun SectionCard(
    title: String?,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    icon: ImageVector? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        color = container,
        border = if (container == MaterialTheme.colorScheme.surfaceContainerLow) {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        } else null,
    ) {
        Column(Modifier.padding(18.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, eyebrow: String? = null) {
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        if (eyebrow != null) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineMedium)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
fun BackBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        actions()
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, container: Color = MaterialTheme.colorScheme.secondaryContainer, content: Color = MaterialTheme.colorScheme.onSecondaryContainer) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        modifier = modifier.clip(RoundedCornerShape(50)).background(container).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun KeyValue(key: String, value: String, note: String? = null, emphasize: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(key, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
        if (!note.isNullOrBlank()) {
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NumberedSteps(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        steps.forEachIndexed { i, s ->
            Row {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Text(s, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun Bullets(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { t ->
            Row {
                Box(Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                Spacer(Modifier.width(10.dp))
                Text(t, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun NavRow(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Illustration with rounded corners and an optional caption over a dark gradient. */
@Composable
fun ArtImage(@DrawableRes res: Int, modifier: Modifier = Modifier, title: String? = null, subtitle: String? = null, ratio: Float = 1.5f) {
    Box(modifier.fillMaxWidth().aspectRatio(ratio).clip(CardShape)) {
        Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (title != null) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.72f)),
                ),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

/**
 * Before/after comparison: drag the handle to reveal the flat RAW look (left)
 * versus the finished edit (right).
 */
@Composable
fun BeforeAfter(@DrawableRes before: Int, @DrawableRes after: Int, modifier: Modifier = Modifier, start: Float = 0.5f) {
    var fraction by remember(before) { mutableFloatStateOf(start) }
    BoxWithConstraints(
        modifier.fillMaxWidth().aspectRatio(1.5f).clip(CardShape)
            .pointerInput(Unit) { detectTapGestures { fraction = (it.x / size.width).coerceIn(0f, 1f) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                }
            },
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        Image(painterResource(after), "Edited", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Image(
            painterResource(before), "Straight out of camera",
            Modifier.fillMaxSize().drawWithContent {
                clipRect(right = size.width * fraction) { this@drawWithContent.drawContent() }
            },
            contentScale = ContentScale.Crop,
        )
        // divider + handle
        val x = with(LocalDensity.current) { (widthPx * fraction).toDp() }
        Box(Modifier.offset(x = x - 1.dp).width(2.dp).fillMaxHeight().background(Color.White))
        Box(
            Modifier.align(Alignment.CenterStart).offset(x = x - 18.dp).size(36.dp).clip(CircleShape)
                .background(Color.White).border(2.dp, Color.Black.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.UnfoldMore, "Drag to compare", Modifier.rotate(90f), tint = Color(0xFF1F1A17))
        }
        Text(
            "RAW", style = MaterialTheme.typography.labelSmall, color = Color.White,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        Text(
            "EDITED", style = MaterialTheme.typography.labelSmall, color = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
