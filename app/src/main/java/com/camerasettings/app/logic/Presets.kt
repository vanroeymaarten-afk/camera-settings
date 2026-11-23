package com.camerasettings.app.logic

import java.util.Locale
import java.util.UUID

/**
 * Editing looks. Each one is shown as a step-by-step recipe in the app
 * and can be exported as an Adobe Lightroom / Camera Raw preset (.xmp).
 */
data class EditPreset(
    val id: String,
    val name: String,
    val tagline: String,
    val useFor: String,
    /** Suggested manual white balance move (white balance can't be relative in a RAW preset). */
    val whiteBalance: String,
    val basic: Basic,
    val hsl: List<Hsl> = emptyList(),
    val grading: Grading = Grading(),
    val curve: List<Pair<Int, Int>> = listOf(0 to 0, 255 to 255),
    val vignette: Int = 0,
    val grain: Int = 0,
    val noiseReduction: Int = 0,
    val calibration: Calibration = Calibration(),
) {
    data class Basic(
        val exposure: Double = 0.0,
        val contrast: Int = 0,
        val highlights: Int = 0,
        val shadows: Int = 0,
        val whites: Int = 0,
        val blacks: Int = 0,
        val texture: Int = 0,
        val clarity: Int = 0,
        val dehaze: Int = 0,
        val vibrance: Int = 0,
        val saturation: Int = 0,
    )

    enum class Channel(val label: String) { Red("Red"), Orange("Orange"), Yellow("Yellow"), Green("Green"), Aqua("Aqua"), Blue("Blue"), Purple("Purple"), Magenta("Magenta") }
    data class Hsl(val channel: Channel, val hue: Int = 0, val sat: Int = 0, val lum: Int = 0)

    data class Grading(
        val shadowHue: Int = 0, val shadowSat: Int = 0,
        val midHue: Int = 0, val midSat: Int = 0,
        val highHue: Int = 0, val highSat: Int = 0,
        val blending: Int = 50, val balance: Int = 0,
    )

    data class Calibration(val redHue: Int = 0, val redSat: Int = 0, val greenHue: Int = 0, val greenSat: Int = 0, val blueHue: Int = 0, val blueSat: Int = 0)

    /** Human-readable steps, in Lightroom panel order. */
    fun steps(): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        out += "White balance" to whiteBalance
        val b = basic
        val basicParts = listOfNotNull(
            b.exposure.takeIf { it != 0.0 }?.let { "Exposure ${sign(it)}" },
            nz("Contrast", b.contrast), nz("Highlights", b.highlights), nz("Shadows", b.shadows),
            nz("Whites", b.whites), nz("Blacks", b.blacks),
        )
        out += "Light" to basicParts.joinToString(" · ")
        val presence = listOfNotNull(nz("Texture", b.texture), nz("Clarity", b.clarity), nz("Dehaze", b.dehaze), nz("Vibrance", b.vibrance), nz("Saturation", b.saturation))
        if (presence.isNotEmpty()) out += "Presence" to presence.joinToString(" · ")
        if (curve.size > 2) out += "Tone curve" to curveDescription()
        hsl.forEach { h ->
            val parts = listOfNotNull(nz("Hue", h.hue), nz("Sat", h.sat), nz("Lum", h.lum))
            out += "Colour mixer: ${h.channel.label}" to parts.joinToString(" · ")
        }
        val g = grading
        if (g.shadowSat + g.midSat + g.highSat > 0) {
            out += "Colour grading" to listOfNotNull(
                if (g.shadowSat > 0) "Shadows ${g.shadowHue}° / ${g.shadowSat}" else null,
                if (g.midSat > 0) "Midtones ${g.midHue}° / ${g.midSat}" else null,
                if (g.highSat > 0) "Highlights ${g.highHue}° / ${g.highSat}" else null,
                "Blending ${g.blending}",
                if (g.balance != 0) "Balance ${sign(g.balance)}" else null,
            ).joinToString(" · ")
        }
        val c = calibration
        val cal = listOfNotNull(nz("Red hue", c.redHue), nz("Red sat", c.redSat), nz("Green hue", c.greenHue), nz("Green sat", c.greenSat), nz("Blue hue", c.blueHue), nz("Blue sat", c.blueSat))
        if (cal.isNotEmpty()) out += "Calibration" to cal.joinToString(" · ")
        if (noiseReduction != 0) out += "Detail" to "Noise reduction $noiseReduction"
        val fx = listOfNotNull(if (vignette != 0) "Vignette ${sign(vignette)}" else null, if (grain != 0) "Grain $grain" else null)
        if (fx.isNotEmpty()) out += "Effects" to fx.joinToString(" · ")
        return out
    }

    private fun curveDescription(): String {
        val lift = curve.first().second
        val roll = 255 - curve.last().second
        val parts = mutableListOf("gentle S-curve")
        if (lift > 0) parts += "lift the black point to $lift (matte look)"
        if (roll > 0) parts += "pull the white point to ${255 - roll}"
        return parts.joinToString(", ")
    }

    private fun nz(label: String, v: Int) = if (v != 0) "$label ${sign(v)}" else null

    companion object {
        fun sign(v: Int) = if (v > 0) "+$v" else "$v"
        fun sign(v: Double) = if (v > 0) String.format(Locale.US, "+%.2f", v) else String.format(Locale.US, "%.2f", v)
    }
}

object Presets {
    private val S_CURVE = listOf(0 to 0, 64 to 58, 128 to 128, 192 to 198, 255 to 255)

    val all: List<EditPreset> = listOf(
        EditPreset(
            id = "golden_warm",
            name = "Golden Warmth",
            tagline = "Glowing desert sunsets with rich orange rock and soft purple shadows.",
            useFor = "Sunsets, deserts, autumn colours, any golden hour",
            whiteBalance = "Start from As Shot, then warm it: Temp +300 to +500 K (RAW) or +8 on the slider (JPEG). Tint +5.",
            basic = EditPreset.Basic(contrast = 15, highlights = -45, shadows = 30, whites = 10, blacks = -12, texture = 10, clarity = 12, dehaze = 8, vibrance = 20),
            hsl = listOf(
                EditPreset.Hsl(EditPreset.Channel.Red, hue = 5, sat = 5),
                EditPreset.Hsl(EditPreset.Channel.Orange, sat = 12, lum = 5),
                EditPreset.Hsl(EditPreset.Channel.Yellow, hue = -8, sat = 8),
                EditPreset.Hsl(EditPreset.Channel.Aqua, sat = -10),
                EditPreset.Hsl(EditPreset.Channel.Blue, hue = -5, sat = -10, lum = -8),
            ),
            grading = EditPreset.Grading(shadowHue = 250, shadowSat = 12, midHue = 40, midSat = 8, highHue = 42, highSat = 18, blending = 60, balance = 10),
            curve = listOf(0 to 6, 64 to 58, 128 to 130, 192 to 200, 255 to 252),
            vignette = -12,
            calibration = EditPreset.Calibration(redHue = 8, blueSat = 15),
        ),
        EditPreset(
            id = "desert_night",
            name = "Desert Night",
            tagline = "Clean, deep night skies with natural star colours and controlled noise.",
            useFor = "Milky Way, star fields, northern lights",
            whiteBalance = "Set Temp to about 3900–4300 K and Tint +5 to +10 for a neutral blue-black sky.",
            basic = EditPreset.Basic(exposure = 0.3, contrast = 20, highlights = -30, shadows = 20, whites = 15, blacks = -20, texture = 15, clarity = 25, dehaze = 20, vibrance = 15, saturation = -5),
            hsl = listOf(
                EditPreset.Hsl(EditPreset.Channel.Orange, sat = -20),
                EditPreset.Hsl(EditPreset.Channel.Purple, sat = -15),
                EditPreset.Hsl(EditPreset.Channel.Blue, sat = 5, lum = -5),
            ),
            grading = EditPreset.Grading(shadowHue = 230, shadowSat = 8, highHue = 40, highSat = 6, blending = 50),
            curve = S_CURVE,
            vignette = -15,
            noiseReduction = 30,
        ),
        EditPreset(
            id = "andes_clear",
            name = "Andes Clear",
            tagline = "Deep high-altitude blue skies against warm earth and golden grass.",
            useFor = "Mountains, lakes, high-altitude blue skies, birds against the sky",
            whiteBalance = "As Shot (Daylight). If the sky looks cyan, Temp +200 K.",
            basic = EditPreset.Basic(contrast = 20, highlights = -35, shadows = 25, whites = 8, blacks = -10, texture = 15, clarity = 15, dehaze = 12, vibrance = 18),
            hsl = listOf(
                EditPreset.Hsl(EditPreset.Channel.Orange, sat = 10),
                EditPreset.Hsl(EditPreset.Channel.Yellow, hue = -5, sat = 12),
                EditPreset.Hsl(EditPreset.Channel.Aqua, sat = 10),
                EditPreset.Hsl(EditPreset.Channel.Blue, sat = 10, lum = -15),
            ),
            grading = EditPreset.Grading(shadowHue = 220, shadowSat = 8, highHue = 45, highSat = 10, blending = 50),
            curve = S_CURVE,
            vignette = -10,
        ),
        EditPreset(
            id = "city_blue",
            name = "City Blue Hour",
            tagline = "Blue sky, warm city lights: the classic teal-and-amber night look.",
            useFor = "Skylines, lit buildings, light trails, fireworks",
            whiteBalance = "Temp about 4000–4500 K keeps the sky blue while lights stay warm.",
            basic = EditPreset.Basic(contrast = 18, highlights = -50, shadows = 25, whites = 12, blacks = -15, clarity = 10, dehaze = 5, vibrance = 15),
            hsl = listOf(
                EditPreset.Hsl(EditPreset.Channel.Orange, sat = 15, lum = 8),
                EditPreset.Hsl(EditPreset.Channel.Yellow, hue = -10),
                EditPreset.Hsl(EditPreset.Channel.Blue, hue = -8, sat = 15),
                EditPreset.Hsl(EditPreset.Channel.Purple, sat = -10),
            ),
            grading = EditPreset.Grading(shadowHue = 220, shadowSat = 18, highHue = 40, highSat = 15, blending = 55),
            curve = S_CURVE,
            vignette = -15,
            noiseReduction = 20,
        ),
        EditPreset(
            id = "puerto_color",
            name = "Colour Port",
            tagline = "Bright, joyful colour that keeps skin and white walls natural.",
            useFor = "Markets, colourful streets, festivals, food",
            whiteBalance = "As Shot, then Temp +150 K for a little sunshine.",
            basic = EditPreset.Basic(contrast = 15, highlights = -30, shadows = 20, whites = 10, blacks = -8, texture = 12, clarity = 10, vibrance = 25, saturation = 5),
            hsl = listOf(
                EditPreset.Hsl(EditPreset.Channel.Red, sat = 10),
                EditPreset.Hsl(EditPreset.Channel.Orange, sat = 5),
                EditPreset.Hsl(EditPreset.Channel.Yellow, hue = -5, sat = 10),
                EditPreset.Hsl(EditPreset.Channel.Green, sat = -10),
                EditPreset.Hsl(EditPreset.Channel.Blue, sat = 10),
            ),
            grading = EditPreset.Grading(shadowHue = 215, shadowSat = 6, highHue = 45, highSat = 10, blending = 50),
            curve = S_CURVE,
            vignette = -8,
        ),
        EditPreset(
            id = "lakes_volcanoes",
            name = "Lakes & Volcanoes",
            tagline = "Fresh greens, turquoise water and a warm glow on the snow.",
            useFor = "Waterfalls, rivers, green landscapes, lakes",
            whiteBalance = "As Shot, Temp +200 to +300 K. At sunset use Cloudy (6500 K) for pink snow.",
            basic = EditPreset.Basic(contrast = 15, highlights = -40, shadows = 30, whites = 10, blacks = -10, texture = 12, clarity = 15, dehaze = 10, vibrance = 20),
            hsl = listOf(
                EditPreset.Hsl(EditPreset.Channel.Orange, sat = 5),
                EditPreset.Hsl(EditPreset.Channel.Green, hue = 10, sat = -10),
                EditPreset.Hsl(EditPreset.Channel.Aqua, sat = 15, lum = 5),
                EditPreset.Hsl(EditPreset.Channel.Blue, sat = 10, lum = -8),
            ),
            grading = EditPreset.Grading(shadowHue = 200, shadowSat = 10, highHue = 40, highSat = 12, blending = 50),
            curve = S_CURVE,
            vignette = -10,
        ),
        EditPreset(
            id = "moody_south",
            name = "Moody South",
            tagline = "Soft, matte, film-like: olive greens, warm highlights and a little grain.",
            useFor = "Forests, fog, rainy days, moody portraits",
            whiteBalance = "Cloudy (6500 K) or As Shot +400 K to take the chill out of rain light.",
            basic = EditPreset.Basic(contrast = 10, highlights = -40, shadows = 20, whites = -5, blacks = 5, texture = 10, clarity = 15, dehaze = 10, vibrance = -5, saturation = -10),
            hsl = listOf(
                EditPreset.Hsl(EditPreset.Channel.Orange, sat = 5),
                EditPreset.Hsl(EditPreset.Channel.Yellow, hue = -10, sat = -15),
                EditPreset.Hsl(EditPreset.Channel.Green, hue = 20, sat = -25, lum = -10),
                EditPreset.Hsl(EditPreset.Channel.Blue, sat = -20),
            ),
            grading = EditPreset.Grading(shadowHue = 160, shadowSat = 10, midHue = 40, midSat = 6, highHue = 45, highSat = 14, blending = 60),
            curve = listOf(0 to 22, 60 to 58, 128 to 128, 190 to 195, 255 to 245),
            vignette = -20,
            grain = 15,
        ),
    )

    fun byId(id: String) = all.first { it.id == id }

    // ------------------------------------------------------------------ XMP export

    private fun v(x: Int) = if (x > 0) "+$x" else "$x"

    /** Adobe Lightroom / Camera Raw preset file contents. */
    fun toXmp(p: EditPreset): String {
        val uuid = UUID.nameUUIDFromBytes("camera-coach-${p.id}".toByteArray()).toString().replace("-", "").uppercase(Locale.US)
        val b = p.basic
        val attrs = linkedMapOf(
            "PresetType" to "Normal",
            "Cluster" to "",
            "UUID" to uuid,
            "SupportsAmount" to "False",
            "SupportsColor" to "True",
            "SupportsMonochrome" to "False",
            "SupportsHighDynamicRange" to "True",
            "SupportsNormalDynamicRange" to "True",
            "SupportsSceneReferred" to "True",
            "SupportsOutputReferred" to "True",
            "CameraModelRestrictive" to "False",
            "Copyright" to "",
            "ContactInfo" to "",
            "Version" to "15.0",
            "ProcessVersion" to "11.0",
            "Exposure2012" to String.format(Locale.US, "%+.2f", b.exposure),
            "Contrast2012" to v(b.contrast),
            "Highlights2012" to v(b.highlights),
            "Shadows2012" to v(b.shadows),
            "Whites2012" to v(b.whites),
            "Blacks2012" to v(b.blacks),
            "Texture" to v(b.texture),
            "Clarity2012" to v(b.clarity),
            "Dehaze" to v(b.dehaze),
            "Vibrance" to v(b.vibrance),
            "Saturation" to v(b.saturation),
        )
        EditPreset.Channel.entries.forEach { c ->
            val h = p.hsl.firstOrNull { it.channel == c }
            attrs["HueAdjustment${c.name}"] = v(h?.hue ?: 0)
            attrs["SaturationAdjustment${c.name}"] = v(h?.sat ?: 0)
            attrs["LuminanceAdjustment${c.name}"] = v(h?.lum ?: 0)
        }
        val g = p.grading
        attrs["SplitToningShadowHue"] = "${g.shadowHue}"
        attrs["SplitToningShadowSaturation"] = "${g.shadowSat}"
        attrs["SplitToningHighlightHue"] = "${g.highHue}"
        attrs["SplitToningHighlightSaturation"] = "${g.highSat}"
        attrs["SplitToningBalance"] = v(g.balance)
        attrs["ColorGradeMidtoneHue"] = "${g.midHue}"
        attrs["ColorGradeMidtoneSat"] = "${g.midSat}"
        attrs["ColorGradeShadowLum"] = "0"
        attrs["ColorGradeMidtoneLum"] = "0"
        attrs["ColorGradeHighlightLum"] = "0"
        attrs["ColorGradeBlending"] = "${g.blending}"
        attrs["ColorGradeGlobalHue"] = "0"
        attrs["ColorGradeGlobalSat"] = "0"
        attrs["ColorGradeGlobalLum"] = "0"
        val c = p.calibration
        attrs["RedHue"] = v(c.redHue)
        attrs["RedSaturation"] = v(c.redSat)
        attrs["GreenHue"] = v(c.greenHue)
        attrs["GreenSaturation"] = v(c.greenSat)
        attrs["BlueHue"] = v(c.blueHue)
        attrs["BlueSaturation"] = v(c.blueSat)
        if (p.noiseReduction != 0) {
            attrs["LuminanceSmoothing"] = "${p.noiseReduction}"
            attrs["ColorNoiseReduction"] = "25"
        }
        attrs["PostCropVignetteAmount"] = v(p.vignette)
        if (p.vignette != 0) {
            attrs["PostCropVignetteMidpoint"] = "50"
            attrs["PostCropVignetteFeather"] = "60"
            attrs["PostCropVignetteRoundness"] = "0"
            attrs["PostCropVignetteStyle"] = "1"
            attrs["PostCropVignetteHighlightContrast"] = "0"
        }
        attrs["GrainAmount"] = "${p.grain}"
        if (p.grain != 0) {
            attrs["GrainSize"] = "25"
            attrs["GrainFrequency"] = "50"
        }
        attrs["ToneCurveName2012"] = "Custom"
        attrs["HasSettings"] = "True"

        val sb = StringBuilder()
        sb.append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Adobe XMP Core 7.0-c000 1.000000, 0000/00/00-00:00:00        \">\n")
        sb.append(" <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n")
        sb.append("  <rdf:Description rdf:about=\"\"\n")
        sb.append("    xmlns:crs=\"http://ns.adobe.com/camera-raw-settings/1.0/\"")
        attrs.forEach { (k, value) -> sb.append("\n   crs:").append(k).append("=\"").append(esc(value)).append('"') }
        sb.append(">\n")
        fun alt(tag: String, text: String) {
            sb.append("   <crs:$tag>\n    <rdf:Alt>\n     <rdf:li xml:lang=\"x-default\">${esc(text)}</rdf:li>\n    </rdf:Alt>\n   </crs:$tag>\n")
        }
        alt("Name", "Camera Coach - ${p.name}")
        alt("ShortName", p.name)
        alt("SortName", "Camera Coach - ${p.name}")
        alt("Group", "Camera Coach")
        alt("Description", p.tagline + " White balance: " + p.whiteBalance)
        sb.append("   <crs:ToneCurvePV2012>\n    <rdf:Seq>\n")
        p.curve.forEach { (x, y) -> sb.append("     <rdf:li>$x, $y</rdf:li>\n") }
        sb.append("    </rdf:Seq>\n   </crs:ToneCurvePV2012>\n")
        listOf("Red", "Green", "Blue").forEach { ch ->
            sb.append("   <crs:ToneCurvePV2012$ch>\n    <rdf:Seq>\n     <rdf:li>0, 0</rdf:li>\n     <rdf:li>255, 255</rdf:li>\n    </rdf:Seq>\n   </crs:ToneCurvePV2012$ch>\n")
        }
        sb.append("  </rdf:Description>\n </rdf:RDF>\n</x:xmpmeta>\n")
        return sb.toString()
    }

    fun fileName(p: EditPreset) = "Camera Coach - ${p.name}.xmp"

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
