package com.camerasettings.app.logic

import com.camerasettings.app.data.Brand
import com.camerasettings.app.data.CameraBody
import com.camerasettings.app.data.Lens
import com.camerasettings.app.logic.Exposure.formatAperture
import com.camerasettings.app.logic.Exposure.formatShutter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

enum class Support { TRIPOD, HANDHELD }

enum class LensPref { WIDE, ASTRO, LONG, PORTRAIT, NORMAL }

enum class Category(val title: String) {
    NATURE("Landscapes & nature"),
    NIGHT("Night & low light"),
    MOTION("Animals & action"),
    PEOPLE("People & places"),
}

enum class Mode { AV, TV, M }

enum class Kind { FOCAL, APERTURE, SHUTTER, ISO, MIN_SHUTTER, EXP_COMP, WHITE_BALANCE, FOCUS, METERING, DRIVE, STABILIZER, QUALITY, FILTER, STYLE, FLASH, OTHER }

data class Scene(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val category: Category,
    val lensPref: LensPref,
    val keywords: String = "",
)

/**
 * One camera setting. [how] says, in plain words, how to set it on the user's camera;
 * [advanced] settings are hidden in Simple mode.
 */
data class Setting(
    val kind: Kind,
    val name: String,
    val value: String,
    val why: String,
    val how: String = "",
    val advanced: Boolean = false,
)

data class Recommendation(
    val mode: Mode,
    val modeLabel: String,
    val modeWhy: String,
    val modeHow: String,
    val aperture: String,
    val shutter: String,
    val iso: String,
    val settings: List<Setting>,
    val tips: List<String>,
    val mistakes: List<String>,
    val warnings: List<String>,
)

object Scenes {
    val all = listOf(
        Scene("landscape", "🏔️", "Landscapes", "Mountains, lakes and wide views", Category.NATURE, LensPref.WIDE, "mountain lake view hills nature scenery"),
        Scene("golden", "🌅", "Sunrise & sunset", "Golden hour and colourful skies", Category.NATURE, LensPref.WIDE, "golden hour sky clouds dusk dawn"),
        Scene("bright", "🏖️", "Snow, beach & desert", "Very bright scenes that come out grey", Category.NATURE, LensPref.WIDE, "snow ski beach sea sand desert salt white"),
        Scene("water", "💧", "Waterfalls & rivers", "Silky or frozen water", Category.NATURE, LensPref.WIDE, "waterfall river stream sea waves long exposure"),
        Scene("forest", "🌲", "Forest, fog & rain", "Soft light and moody days", Category.NATURE, LensPref.WIDE, "forest woods fog mist rain cloudy trees"),
        Scene("macro", "🌸", "Flowers & close-ups", "Insects, flowers, small details", Category.NATURE, LensPref.LONG, "macro flower insect butterfly close detail garden"),
        Scene("astro", "🌌", "Stars & Milky Way", "Dark skies away from towns", Category.NIGHT, LensPref.ASTRO, "stars milky way night sky astro"),
        Scene("aurora", "🟢", "Northern lights", "Aurora in the far north or south", Category.NIGHT, LensPref.ASTRO, "aurora borealis northern lights australis"),
        Scene("city", "🌃", "City at night", "Blue hour, skylines, lit buildings", Category.NIGHT, LensPref.WIDE, "city night skyline blue hour lights evening"),
        Scene("trails", "🚗", "Light trails", "Streaks of car and boat lights", Category.NIGHT, LensPref.WIDE, "light trails traffic cars long exposure"),
        Scene("fireworks", "🎆", "Fireworks", "New Year, festivals, celebrations", Category.NIGHT, LensPref.NORMAL, "fireworks new year festival"),
        Scene("indoor", "⛪", "Indoors & museums", "Churches, museums, restaurants", Category.NIGHT, LensPref.NORMAL, "indoor church museum restaurant room dark no flash"),
        Scene("wildlife", "🦊", "Wildlife", "Animals in nature, safaris, zoos", Category.MOTION, LensPref.LONG, "animals wildlife zoo safari deer fox"),
        Scene("birds", "🦅", "Birds in flight", "Fast-moving birds against the sky", Category.MOTION, LensPref.LONG, "birds flying flight seagull eagle"),
        Scene("action", "🏄", "Sports & action", "Surfing, cycling, children playing", Category.MOTION, LensPref.LONG, "sport action running kids football surf bike moving"),
        Scene("pets", "🐶", "Pets", "Dogs and cats at home or outside", Category.MOTION, LensPref.PORTRAIT, "pet dog cat"),
        Scene("portrait", "🙂", "Portraits", "Family, friends, people you meet", Category.PEOPLE, LensPref.PORTRAIT, "portrait people family person face"),
        Scene("street", "🛍️", "Travel & street", "Markets, streets, everyday life", Category.PEOPLE, LensPref.NORMAL, "travel street market town walk city day"),
        Scene("architecture", "🏛️", "Buildings & architecture", "Churches, facades, monuments", Category.PEOPLE, LensPref.WIDE, "building architecture church castle monument"),
        Scene("food", "🍝", "Food", "Restaurant plates and markets", Category.PEOPLE, LensPref.NORMAL, "food restaurant dinner plate meal"),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }

    fun search(q: String): List<Scene> {
        val s = q.trim().lowercase(Locale.ROOT)
        if (s.isEmpty()) return all
        return all.filter { it.title.lowercase().contains(s) || it.subtitle.lowercase().contains(s) || it.keywords.contains(s) }
    }
}

object Advisor {

    // ------------------------------------------------------------------ lens choice

    fun pickLens(bag: List<Lens>, pref: LensPref, crop: Double): Lens? {
        if (bag.isEmpty()) return null
        return when (pref) {
            LensPref.WIDE -> bag.minWith(compareBy<Lens> { it.minFocal }.thenBy { if (it.isZoom) 0 else 1 })
            LensPref.ASTRO -> {
                val wide = bag.filter { it.minFocal * crop <= 35 }
                (wide.ifEmpty { bag }).minWith(compareBy<Lens> { it.apWide }.thenBy { it.minFocal })
            }
            LensPref.LONG -> bag.maxBy { it.maxFocal }
            LensPref.PORTRAIT -> {
                val target = (70 / crop).roundToInt()
                bag.minWith(compareBy<Lens> { it.apertureAt(focalFor(it, pref, crop)) + penalty(it, target) })
            }
            LensPref.NORMAL -> {
                val target = (35 / crop).roundToInt()
                bag.minWith(compareBy<Lens> { penalty(it, target) }.thenBy { if (it.isZoom) 0 else 1 })
            }
        }
    }

    private fun penalty(lens: Lens, target: Int): Double = when {
        target < lens.minFocal -> (lens.minFocal - target) / 10.0
        target > lens.maxFocal -> (target - lens.maxFocal) / 10.0
        else -> 0.0
    }

    fun focalFor(lens: Lens, pref: LensPref, crop: Double): Int = when (pref) {
        LensPref.WIDE, LensPref.ASTRO -> lens.minFocal
        LensPref.LONG -> lens.maxFocal
        LensPref.PORTRAIT -> (70 / crop).roundToInt().coerceIn(lens.minFocal, lens.maxFocal)
        LensPref.NORMAL -> (35 / crop).roundToInt().coerceIn(lens.minFocal, lens.maxFocal)
    }

    // ------------------------------------------------------------------ helpers

    private fun stops(body: CameraBody, lens: Lens): Double = when {
        body.ibis && lens.hasIs -> 4.0
        body.ibis -> 3.0
        lens.hasIs -> 2.5
        else -> 0.0
    }

    fun handheldShutter(body: CameraBody, lens: Lens, focal: Int): Double {
        val eq = focal * body.sensor.crop
        return Exposure.shutterAtMost(min((1.0 / eq) * 2.0.pow(stops(body, lens)), 1 / 15.0))
    }

    fun npfSeconds(body: CameraBody, lens: Lens, focal: Int): Double =
        (35 * lens.apertureAt(focal) + 30 * body.pixelPitchUm) / focal

    private fun clampF(target: Double, lens: Lens, focal: Int) = Exposure.nearestAperture(max(target, lens.apertureAt(focal)))

    // ------------------------------------------------------------------ plain-language "how to" per brand

    private class How(val brand: Brand, val body: CameraBody) {
        val v = brand.vocab
        val q = v.quickMenu
        fun mode(m: Mode): String = when {
            brand == Brand.FUJIFILM && m == Mode.AV -> "Set the shutter speed dial to A, then choose the aperture with the aperture ring (or the front dial if the lens has no ring)."
            brand == Brand.FUJIFILM && m == Mode.TV -> "Set the aperture ring to A (or the lens switch to A), then pick the shutter speed on the dial on top."
            brand == Brand.FUJIFILM -> "Pick the shutter speed on the dial on top and the aperture on the ring; set ISO yourself."
            else -> "${v.dialMode} ${label(m)}."
        }
        fun label(m: Mode) = when (m) { Mode.AV -> v.av; Mode.TV -> v.tv; Mode.M -> "M" }
        fun aperture(value: String, manual: Boolean) =
            if (manual && brand == Brand.CANON) "Turn the rear wheel (or hold Av± and turn the main dial) until the screen shows $value."
            else "Turn the ${v.apertureDial} until the screen shows $value."
        fun shutter(value: String) = "Turn the ${v.shutterDial} until the screen shows $value."
        fun iso(value: String) = if (brand == Brand.FUJIFILM) "Use the ISO dial (or press $q → ISO) and set $value." else "Press the ISO button (or the $q) and set $value."
        fun quick(what: String, value: String) = "Press the $q, select $what and choose $value."
        fun expComp(value: String) = "To set $value: ${v.expComp}."
        fun stabilizer(on: Boolean) = "Set ${v.stabilizer} to ${if (on) "ON" else "OFF"} (a switch on the lens, or in the camera menu)."
        val quality = "Menu → Image quality → RAW + JPEG (large/fine). Your camera saves RAW as .${body.rawFormat} files."
        val timer = "Press the drive button (or the $q) and choose the 2-second self-timer."
        val burst = "Press the drive button (or the $q) and choose high-speed continuous shooting."
        val mf = "Switch the lens (or the camera's focus mode) to MF and turn the focus ring."
        val minShutter = "Menu → ${v.minShutter}."
    }

    // ------------------------------------------------------------------ the advice

    fun recommend(scene: Scene, body: CameraBody, lens: Lens, focal: Int, support: Support): Recommendation {
        val h = How(body.brand, body)
        val v = body.brand.vocab
        val crop = body.sensor.crop
        val tripod = support == Support.TRIPOD
        val wideOpen = lens.apertureAt(focal)
        val hh = handheldShutter(body, lens, focal)
        val autoIsoLabel = "Auto (max ${body.maxGoodIso})"
        val hasIs = lens.hasIs || body.ibis
        val eq = (focal * crop).roundToInt()

        val s = mutableListOf<Setting>()
        val tips = mutableListOf<String>()
        val mistakes = mutableListOf<String>()
        val warn = mutableListOf<String>()

        var mode = Mode.AV
        var ap = "Camera chooses"
        var sh = "Camera chooses"
        var iso = autoIsoLabel
        var modeWhy = ""

        fun focal() {
            s += Setting(
                Kind.FOCAL, "Zoom", "$focal mm" + if (crop != 1.0) " (looks like $eq mm on full frame)" else "",
                when (scene.lensPref) {
                    LensPref.WIDE -> "Zoom out to fit in the whole view."
                    LensPref.ASTRO -> "Zoom out fully: more sky and longer exposures before stars blur."
                    LensPref.LONG -> "Zoom in fully: it lets you stay further away."
                    LensPref.PORTRAIT -> "A little zoomed in flatters faces and blurs the background."
                    LensPref.NORMAL -> "A natural angle, close to what your eyes see."
                },
                if (lens.isZoom) "Turn the zoom ring on the lens until the mark points at about $focal." else "",
            )
        }
        fun aperture(f: Double, why: String, manual: Boolean = false) {
            ap = formatAperture(f)
            s += Setting(Kind.APERTURE, "Aperture", ap, why, h.aperture(ap, manual))
        }
        fun wideOpenAperture(why: String, manual: Boolean = false) {
            ap = formatAperture(wideOpen)
            s += Setting(Kind.APERTURE, "Aperture", "$ap (smallest number your lens allows)", why, h.aperture(ap, manual))
        }
        fun shutter(t: String, why: String) {
            sh = t
            s += Setting(Kind.SHUTTER, "Shutter speed", t, why, h.shutter(t))
        }
        fun isoFixed(n: Int, why: String) {
            iso = "ISO $n"
            s += Setting(Kind.ISO, "ISO", iso, why, h.iso("ISO $n"))
        }
        fun isoAuto(why: String, minShutter: Double? = null) {
            iso = autoIsoLabel
            s += Setting(Kind.ISO, "ISO", "AUTO", why, h.iso("AUTO") + " Optional: limit Auto ISO to ${body.maxGoodIso} in the menu.")
            if (minShutter != null) {
                s += Setting(Kind.MIN_SHUTTER, "Auto ISO slowest shutter", formatShutter(minShutter), "Stops the camera from choosing a shutter speed that's too slow to hold steady.", h.minShutter, advanced = true)
            }
        }
        fun expComp(value: String, why: String) {
            s += Setting(Kind.EXP_COMP, "Exposure compensation (+/–)", value, why, h.expComp(value))
        }
        fun wb(value: String, why: String) {
            s += Setting(Kind.WHITE_BALANCE, "White balance", value, why, h.quick("white balance (WB)", value))
        }
        fun af(single: Boolean, extra: String = "", why: String = "") {
            val value = (if (single) v.afSingle else v.afContinuous) + if (extra.isNotEmpty()) ", $extra" else ""
            s += Setting(Kind.FOCUS, "Focus", value, why.ifEmpty { if (single) "Locks focus once when you half-press the shutter button." else "Keeps refocusing while the subject moves." }, h.quick("the focus mode", if (single) v.afSingle else v.afContinuous))
        }
        fun manualFocus(why: String) {
            s += Setting(Kind.FOCUS, "Focus", "Manual (MF)", why, h.mf)
        }
        fun metering() {
            s += Setting(Kind.METERING, "Metering", v.metering, "The camera's all-round light measurement works well here.", h.quick("metering", v.metering), advanced = true)
        }
        fun timer() {
            s += Setting(Kind.DRIVE, "Drive", "2-second self-timer", "Pressing the button shakes the camera; the timer lets it settle first.", h.timer)
        }
        fun burst(why: String = "Take a few frames in a row and keep the sharpest.") {
            s += Setting(Kind.DRIVE, "Drive", "Continuous (burst)", why, h.burst)
        }
        fun stabilizer() {
            if (!hasIs) return
            s += if (tripod) Setting(Kind.STABILIZER, "Stabilizer (${v.stabilizer})", "OFF", "On a tripod the stabilizer can cause blur.", h.stabilizer(false))
            else Setting(Kind.STABILIZER, "Stabilizer (${v.stabilizer})", "ON", "Helps you hold the camera steady.", h.stabilizer(true))
        }
        fun quality() {
            s += Setting(Kind.QUALITY, "Image quality", "RAW + JPEG", "JPEG to share right away; RAW keeps extra detail for editing later.", h.quality, advanced = true)
        }
        fun style(value: String, why: String) {
            s += Setting(Kind.STYLE, v.pictureStyle, value, why, h.quick(v.pictureStyle, value), advanced = true)
        }

        focal()
        when (scene.id) {
            "landscape" -> {
                mode = Mode.AV; modeWhy = "You choose the aperture so everything is sharp; the camera picks the shutter speed."
                aperture(clampF(if (crop >= 2.0) 5.6 else 8.0, lens, focal), "Most lenses are sharpest here, and both near and far are in focus.")
                if (tripod) { isoFixed(100, "Lowest ISO = cleanest picture. The tripod handles slow shutter speeds."); timer() }
                else isoAuto("The camera raises ISO only when the light drops.", Exposure.shutterAtMost(min(hh, 1 / 125.0)))
                af(true, "single point", "Put the focus point about a third into the scene, then half-press.")
                expComp("0 (−⅓ with a lot of bright sky)", "Keeps bright clouds from turning pure white.")
                wb("Daylight ☀", "Keeps colours natural and consistent.")
                metering(); stabilizer(); style(v.styleLandscape, "Punchier blues and greens in the JPEG."); quality()
                tips += "Put something in the foreground (a rock, flowers, a person) to show how big the view is."
                tips += "Keep the horizon straight: most cameras can show a level or grid on the screen."
                tips += "A polarising filter makes skies deeper blue and removes reflections on water."
                mistakes += "Everything blurry? You probably focused on nothing in particular. Aim the focus point at something a third into the scene."
                mistakes += "Washed-out sky: dial in −⅓ or −⅔ exposure compensation."
            }
            "golden" -> {
                mode = Mode.AV; modeWhy = "You fix the aperture; the camera keeps up with the fast-changing light."
                aperture(clampF(8.0, lens, focal), "Front-to-back sharpness. f/11–f/16 turns the sun into a star.")
                if (tripod) { isoFixed(100, "Clean images; the tripod handles slow speeds."); timer() } else isoAuto("The light fades fast; Auto ISO keeps up.", hh)
                expComp("−⅔", "Keeps the sky's colours rich instead of washed out.")
                wb("Cloudy ☁", "Auto white balance removes the warm glow; Cloudy keeps it.")
                af(true); metering(); stabilizer()
                s += Setting(Kind.OTHER, "Bracketing (AEB)", "3 shots: −2, 0, +2", "Saves the bright sky and the dark ground in separate frames to combine later.", "Menu → exposure bracketing (AEB / BKT) → ±2.", advanced = true)
                quality()
                tips += "Arrive 30 minutes early. The Plan tab shows the exact golden hour where you are."
                tips += "Keep shooting after the sun has gone: the best colours often come 10–20 minutes later."
                tips += "Turn around! The light on the landscape behind you can be even better."
                mistakes += "Dark ground and bright sky? That's normal. Expose for the sky and lift shadows later, or use bracketing."
                mistakes += "Don't look at the sun through the viewfinder when it's high: it can hurt your eyes."
            }
            "bright" -> {
                mode = Mode.AV; modeWhy = "You fix the aperture; exposure compensation fixes the 'grey snow' problem."
                aperture(clampF(if (crop >= 2) 5.6 else 8.0, lens, focal), "Sharp from front to back.")
                isoFixed(100, "There's plenty of light, so use the cleanest ISO.")
                expComp("+1 to +1⅔", "The camera thinks white is too bright and makes snow, sand and beaches grey. Adding exposure makes them white again.")
                wb("Daylight ☀", "Auto white balance can make snow blue.")
                af(true); metering(); stabilizer()
                s += Setting(Kind.OTHER, "Highlight warning ('blinkies')", "ON", "Blinking areas are pure white with no detail: lower exposure compensation by ⅓ until they mostly stop.", "Menu → playback settings → highlight alert / highlight warning → ON.", advanced = true)
                quality()
                tips += "Sunglasses help you see the screen, and a lens hood stops glare."
                tips += "Protect the camera from sand and salt spray: keep it in the bag when not shooting."
                tips += "Cold drains batteries fast: keep a spare in an inside pocket."
                mistakes += "Grey snow or sand: you forgot the + exposure compensation."
                mistakes += "Changing lenses on a windy beach lets sand onto the sensor. Do it inside a bag."
            }
            "water" -> {
                mode = Mode.M; modeWhy = "You control everything, so each photo in a series looks the same."
                if (!tripod) warn += "Silky water needs a tripod, or put the camera on a rock or your bag and use the self-timer."
                aperture(clampF(11.0, lens, focal), "A small opening lets less light in, so the shutter can stay open longer.", manual = true)
                isoFixed(100, "Lowest ISO, also to allow a slower shutter.")
                shutter("½ to 2 seconds", "Long enough to blur moving water into silk. Try several.")
                s += Setting(Kind.FILTER, "ND filter", "ND8 (3 stops) or ND64 (6 stops) in daylight", "A 'sunglasses' filter. Without one, daylight is too bright for long exposures (see Tools → ND filter).", "Screw the filter onto the front of the lens after focusing.", advanced = true)
                af(true, "then switch to MF", "Focus once, then switch to MF so focus stays put.")
                timer(); stabilizer(); quality()
                tips += "Overcast days and shade are perfect: less light means slower shutter speeds without a filter."
                tips += "Want frozen splashes instead? Use 1/1000 s."
                tips += "Wipe spray off the lens before every shot."
                mistakes += "Completely white photo: too much light. Use a smaller aperture (f/16), ISO 100, or an ND filter."
                mistakes += "Everything blurry, not just the water: the camera moved. Use a tripod and the self-timer."
            }
            "forest" -> {
                mode = Mode.AV; modeWhy = "You set the depth; the camera finds the (often slow) shutter speed."
                aperture(clampF(8.0, lens, focal), "Depth through the trees.")
                if (tripod) { isoFixed(100, "Forests are darker than they look; the tripod lets ISO stay low."); timer() }
                else isoAuto("Under the trees the light drops fast.", hh)
                expComp("−⅓ to −⅔", "Keeps bright gaps and light rays from blowing out, and keeps the moody feel.")
                wb("Cloudy ☁", "Removes the cold blue-green look of shade and rain.")
                af(true, "on a trunk or plant in front"); metering(); stabilizer(); quality()
                tips += "Grey, drizzly days are a gift: soft light, rich greens, no harsh shadows."
                tips += "Look for layers: a fern in front, a trunk in the middle, fog behind."
                tips += "Rain? A plastic bag with a rubber band protects the camera."
                mistakes += "Very dark photos: add +⅓ exposure compensation, the camera is fooled by dark greens."
            }
            "macro" -> {
                mode = Mode.AV; modeWhy = "Aperture decides how much of a tiny subject is sharp."
                aperture(clampF(8.0, lens, focal), "Close up, the sharp zone is paper-thin. f/8 gives a bit more of the subject in focus.")
                isoAuto("Close-ups need fast shutter speeds against shake and wind.", Exposure.shutterAtMost(min(hh, 1 / 250.0)))
                af(true, "single point on the eye or flower centre", "Move the focus point onto the most important part.")
                burst("Tiny movements spoil focus; take several frames.")
                stabilizer(); quality()
                tips += "Zoom in fully and move as close as your lens can still focus (the minimum distance is printed on the lens)."
                tips += "Early morning: insects are slow and there's no wind."
                tips += "A plain background (sky, leaves far away) makes the subject pop."
                mistakes += "Camera won't take the photo? You're closer than the lens can focus. Step back a little."
            }
            "astro" -> {
                mode = Mode.M; modeWhy = "In the dark the camera can't measure the light, so you set everything yourself."
                val npf = npfSeconds(body, lens, focal)
                val t = Exposure.shutterAtMost(min(npf, 30.0))
                val cap = if (body.maxGoodIso >= 3200) 6400 else 3200
                val isoN = Exposure.nearestIso((3200 * (wideOpen / 2.8).pow(2) * (15.0 / t)).coerceIn(1600.0, cap.toDouble()))
                if (!tripod) warn += "Stars need a tripod, or rest the camera on a wall or bag and use the self-timer."
                wideOpenAperture("Gather as much starlight as possible.", manual = true)
                shutter(formatShutter(t), String.format(Locale.US, "The longest time before stars turn into streaks with your camera (about %.0f s).", npf))
                isoFixed(isoN, "High enough to see the Milky Way. Noise can be cleaned up later; a black photo can't.")
                manualFocus("Autofocus can't see stars. Magnify the screen 10× on the brightest star and turn the ring until it's the smallest dot.")
                wb("3900–4200 K (Kelvin)", "Gives a natural blue-black sky instead of orange.")
                timer(); stabilizer(); quality()
                s += Setting(Kind.OTHER, "Long exposure noise reduction", "OFF", "It doubles the wait after every photo. Turn on only for your final shot.", "Menu → long exposure noise reduction → OFF.", advanced = true)
                tips += "Get far away from town lights and pick a night without moon (the Plan tab has a star calendar)."
                tips += "Give your eyes 20 minutes to adjust and use a red light."
                tips += "Bring warm clothes and a spare battery: nights are cold even in summer."
                mistakes += "Stars look like little lines: shorter shutter speed."
                mistakes += "Everything blurry: focus wasn't on the stars. Use manual focus on a bright star."
                if (wideOpen > 3.5) warn += "Your lens is fairly 'slow' (${formatAperture(wideOpen)}). It works, but expect noise."
            }
            "aurora" -> {
                mode = Mode.M; modeWhy = "The camera can't measure faint aurora light; you set it."
                wideOpenAperture("Let in as much light as possible.", manual = true)
                shutter("2–8 seconds (shorter when it's bright and moving fast)", "Short enough to keep the shapes of the lights sharp.")
                isoFixed(if (wideOpen <= 2.8) 1600 else 3200, "Bright enough to record the aurora; adjust after a test shot.")
                manualFocus("Focus on a bright star using the magnified live view, then don't touch the ring.")
                wb("3500–4000 K", "Keeps the greens natural and the sky dark blue.")
                timer(); stabilizer(); quality()
                if (!tripod) warn += "Aurora needs a tripod or a steady surface."
                tips += "Aurora is often visible from about 60° latitude north or south on clear, dark nights."
                tips += "Put a landmark in the foreground: a cabin, mountain or lake reflection."
                tips += "Batteries die fast in the cold. Keep spares warm in your pocket."
                mistakes += "Green blur without shapes: shutter too long. Try 2–4 seconds and raise ISO."
            }
            "city" -> {
                if (tripod) {
                    mode = Mode.AV; modeWhy = "You set the aperture; the camera works out the long exposure."
                    aperture(clampF(8.0, lens, focal), "Sharp everywhere; lights turn into little stars at f/11+.")
                    isoFixed(100, "Clean, noise-free night photos.")
                    sh = "Camera chooses (often 2–15 s)"
                    timer()
                } else {
                    mode = Mode.AV; modeWhy = "Wide aperture and Auto ISO let you shoot handheld at night."
                    wideOpenAperture("Let in all the light you can.")
                    isoAuto("Raises ISO so you can hold the camera.", hh)
                    burst("One of three is usually sharpest.")
                }
                expComp("−⅓ to −⅔", "Night should look like night, and lights don't blow out.")
                wb("Auto, or 4000 K for a bluer sky", "")
                af(true); metering(); stabilizer(); quality()
                tips += "Blue hour, 15–30 minutes after sunset, is the magic moment: the sky still has colour when the lights come on."
                tips += "Bridges and rooftops make great viewpoints."
                tips += "Water doubles the lights with reflections."
                mistakes += "Blurry handheld shots: brace against a wall and breathe out while pressing the button."
            }
            "trails" -> {
                mode = Mode.M; modeWhy = "You choose a long shutter so moving lights become lines."
                if (!tripod) warn += "Light trails need a tripod or a steady surface."
                aperture(clampF(11.0, lens, focal), "Small aperture allows a long shutter without overexposing.", manual = true)
                isoFixed(100, "Clean image during long exposures.")
                shutter("10–30 seconds", "Long enough for cars to cross the whole frame.")
                af(true, "then switch to MF"); timer(); stabilizer(); quality()
                tips += "Find a bridge or a bend in the road at blue hour; the sky stays blue."
                tips += "Start the shot just before a group of cars arrives."
                mistakes += "Photo too bright: smaller aperture (f/16) or shorter time."
            }
            "fireworks" -> {
                mode = Mode.M; modeWhy = "Fixed settings work best: the bursts are too fast for the camera to adjust."
                aperture(clampF(8.0, lens, focal), "Fireworks are bright; f/8 keeps colours rich.", manual = true)
                isoFixed(100, "Clean, colourful bursts.")
                shutter("2–4 seconds (or Bulb)", "Long enough for the trails of a burst, short enough to avoid a white mess.")
                manualFocus("Autofocus struggles in the dark: focus on the first bursts or a distant light, then switch to MF.")
                timer(); stabilizer(); quality()
                if (!tripod) warn += "Fireworks need a tripod or a steady surface."
                tips += "Get there early to find a spot upwind (no smoke) with a nice foreground: a bridge, the skyline, water."
                tips += "Press the button when you hear the launch; release after the burst."
                mistakes += "Big white blobs: shutter too long or aperture too wide. Try f/11 and 2 seconds."
            }
            "indoor" -> {
                mode = Mode.AV; modeWhy = "Wide aperture and Auto ISO for as much light as possible."
                wideOpenAperture("Let in as much light as possible.")
                isoAuto("Indoors is much darker than it looks; the camera will raise ISO.", hh)
                s += Setting(Kind.FLASH, "Flash", "OFF", "Often not allowed in churches and museums, and it makes things look flat.", "Close the pop-up flash, or set flash to OFF in the menu.")
                wb("Auto", "Modern cameras handle indoor lights well.")
                burst("Take three: one is usually sharpest.")
                stabilizer(); quality()
                tips += "Lean against a pillar or wall, breathe out and press the button gently."
                tips += "Check the rules: some places don't allow photos at all."
                mistakes += "Orange photos: set white balance to Auto (or 'Tungsten' / light bulb)."
            }
            "wildlife" -> {
                mode = Mode.TV; modeWhy = "You set a fast shutter speed to freeze movement; the camera handles the rest."
                // The faster of 1/1000 s (moving animals) and 1/(2 × focal length) for long lenses.
                val t = Exposure.nearestShutter(min(1 / 1000.0, 1.0 / (2 * eq)))
                shutter(formatShutter(t), "Freezes moving animals. Resting animals: 1/500 s is enough.")
                ap = "Camera chooses (≈ ${formatAperture(wideOpen)})"
                isoAuto("Shutter speed matters more than a bit of grain.")
                af(false, if (body.animalAf) v.subjectDetect else "zone / area", "Keeps the animal sharp as it moves.")
                burst(); expComp("0 (+⅔ for a dark animal on a bright background)", ""); stabilizer(); quality()
                tips += "Get down to the animal's eye level for more intimate photos."
                tips += "Focus on the eye. If the eye is sharp, the photo works."
                tips += "Keep your distance and never feed or chase wild animals."
                if (eq < 150) warn += "Your longest lens is short for wildlife. Get as close as is safe and crop later."
                mistakes += "Blurry animal, sharp grass: the focus point wasn't on the animal. Use continuous AF and keep the point on it."
            }
            "birds" -> {
                mode = Mode.TV; modeWhy = "A very fast shutter speed is everything for birds in flight."
                shutter("1/2000 s", "Wings move fast. Big gliding birds: 1/1250 s also works.")
                ap = "Camera chooses (≈ ${formatAperture(wideOpen)})"
                isoAuto("Fast shutter speeds need more ISO.")
                af(false, if (body.animalAf) v.subjectDetect else "wide area / tracking")
                burst(); expComp("+⅔ to +1 against bright sky", "Otherwise birds come out as dark silhouettes."); stabilizer(); quality()
                tips += "Start following the bird before you press the shutter, and keep following after."
                tips += "Fly in towards you or across the light: the sun behind you lights the bird."
                mistakes += "Bird is a black shape: add + exposure compensation."
            }
            "action" -> {
                mode = Mode.TV; modeWhy = "Fast shutter speeds freeze the action."
                shutter("1/1000 s", "Freezes running, jumping, surfing and cycling.")
                isoAuto("The camera raises ISO to keep the shutter fast.")
                af(false, "wide area / tracking"); burst("Take bursts and pick the best moment.")
                stabilizer(); quality()
                tips += "Anticipate: focus where the action will be and start the burst just before."
                tips += "For a sense of speed, try panning: 1/60 s while following the subject smoothly."
                mistakes += "Everything blurry: shutter too slow. Check the screen shows 1/1000 or faster."
            }
            "pets" -> {
                mode = Mode.AV; modeWhy = "Wide aperture blurs the background, Auto ISO keeps it bright."
                wideOpenAperture("A blurred background makes your pet stand out.")
                isoAuto("Indoors pets need higher ISO to stay sharp.", Exposure.shutterAtMost(1 / 250.0))
                af(false, if (body.animalAf) "${v.subjectDetect}, eye detection ON" else "zone", "Pets rarely sit still.")
                burst(); stabilizer(); quality()
                tips += "Get down to their eye level, on the floor if needed."
                tips += "A treat or squeaky toy just above the lens gets their attention."
                tips += "Window light is the most flattering light indoors."
                mistakes += "Blurry fur: shutter too slow. Make sure it's at least 1/250 s."
            }
            "portrait" -> {
                mode = Mode.AV; modeWhy = "Wide aperture for a blurred background; the camera handles the rest."
                wideOpenAperture("Blurs the background so the person stands out.")
                isoAuto("Keeps the shutter fast enough.", hh)
                af(true, "eye detection ON (or single point on the nearest eye)", "Sharp eyes make a portrait.")
                expComp("+⅓", "Keeps faces bright.")
                wb("Auto (Cloudy for warmer skin)", "")
                stabilizer(); style(v.styleStandard, "Natural skin tones."); quality()
                tips += "Put people in open shade or with the sun behind them: no squinting, soft light."
                tips += "Step back and zoom in rather than standing close with a wide angle: faces look better."
                tips += "Talk to people while shooting; relaxed people look better. Always ask before photographing strangers."
                mistakes += "Face too dark with bright background: add +⅔ to +1 exposure compensation."
            }
            "street" -> {
                mode = Mode.AV; modeWhy = "Quick and easy: you set the depth, the camera does the rest."
                aperture(clampF(8.0, lens, focal), "Enough depth for quick, unplanned photos.")
                isoAuto("Shade and bright streets change all the time.", Exposure.shutterAtMost(1 / 250.0))
                af(true, "wide area")
                wb("Auto", ""); stabilizer(); quality()
                tips += "Look for colour, light and repeating shapes, then wait for a person to walk into the scene."
                tips += "Midday sun is harsh: shady streets and the hours around sunset look better."
                tips += "Keep the strap around your wrist or body in busy places."
                mistakes += "Tilted buildings: hold the camera level, and step back rather than tilting up."
            }
            "architecture" -> {
                mode = Mode.AV; modeWhy = "Small aperture for sharpness everywhere."
                aperture(clampF(8.0, lens, focal), "Everything sharp from the door to the roof.")
                if (tripod) { isoFixed(100, "Clean detail."); timer() } else isoAuto("Keeps the shutter fast enough to hold steady.", hh)
                af(true); expComp("0 (−⅓ with bright sky)", ""); wb("Daylight ☀ (Auto indoors)", ""); stabilizer(); quality()
                tips += "Hold the camera straight and level so walls don't lean. Step back instead of tilting up."
                tips += "Blue hour, when the lights are on and the sky is still blue, makes buildings glow."
                tips += "Look for symmetry: stand exactly in the middle."
                mistakes += "Leaning buildings: you tilted the camera up. Many editors can straighten this later."
            }
            "food" -> {
                mode = Mode.AV; modeWhy = "Choose how much of the plate is sharp."
                aperture(clampF(4.0, lens, focal), "Slightly blurred background, but the dish sharp.")
                isoAuto("Restaurants are dark; Auto ISO keeps the shutter fast enough.", hh)
                af(true, "single point on the most important part")
                wb("Auto", "")
                s += Setting(Kind.FLASH, "Flash", "OFF", "Flash makes food look flat and greasy.", "Close the flash or turn it off in the menu.")
                stabilizer(); quality()
                tips += "Sit near a window: side light makes food look delicious."
                tips += "Shoot from straight above for flat dishes, from the side for burgers and cakes."
                mistakes += "Orange colours in the evening: set white balance to Auto or Tungsten."
            }
            else -> {}
        }

        val modeLabel = when (mode) {
            Mode.AV -> if (v.av.contains("aperture")) v.av else "${v.av} (aperture priority)"
            Mode.TV -> if (v.tv.contains("shutter")) v.tv else "${v.tv} (shutter priority)"
            Mode.M -> "M (manual)"
        }
        return Recommendation(mode, modeLabel, modeWhy, h.mode(mode), ap, sh, iso, s, tips, mistakes, warn)
    }

    /** Short plain-language explanations for the basics. */
    val basics = listOf(
        "Aperture priority (Av or A)" to "You choose the f-number; the camera picks the shutter speed. The best everyday mode.",
        "Shutter priority (Tv or S)" to "You choose the shutter speed (e.g. 1/1000 to freeze motion); the camera picks the aperture.",
        "Manual (M)" to "You choose both. For stars, fireworks and waterfalls.",
        "Aperture (f/…)" to "Small number (f/2.8) = more light and a blurry background. Big number (f/11) = everything sharp, less light.",
        "Shutter speed" to "How long the camera looks. 1/1000 s freezes action; 1 second blurs moving water (and shaky hands).",
        "ISO" to "How sensitive the camera is. Low (100) = cleanest. High (6400) = brighter but grainier.",
        "Exposure compensation (+/–)" to "Tells the camera 'brighter' or 'darker' in Av, Tv and Auto ISO.",
        "Half-press" to "Press the shutter button halfway to focus, then all the way to take the photo.",
        "Histogram" to "A graph of brightness when you look at your photo. Touching the right edge = parts are pure white.",
    )
}
