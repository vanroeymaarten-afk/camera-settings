package com.camerasettings.app.logic

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

data class Scene(
    val id: String,
    val emoji: String,
    val title: String,
    val where: String,
    val lensPref: LensPref,
    /** Illustration shown on the card (an example from the Shot guide), if any. */
    val art: String? = null,
)

data class Setting(val name: String, val value: String, val why: String)

data class Recommendation(
    val mode: String,
    val modeWhy: String,
    val settings: List<Setting>,
    val tips: List<String>,
    val warnings: List<String>,
)

object Scenes {
    val all = listOf(
        Scene("landscape", "🏔️", "Landscapes in daylight", "Altiplano lagoons, Atacama desert, volcanoes", LensPref.WIDE, "altiplano"),
        Scene("golden", "🌅", "Sunrise, sunset & golden hour", "Valle de la Luna, Osorno from Frutillar", LensPref.WIDE, "valle_luna"),
        Scene("snow", "🧂", "Salt flats, snow & bright scenes", "Salar de Atacama, Laguna Cejar, Osorno's snow cap", LensPref.WIDE, "flamingos"),
        Scene("wildlife", "🐧", "Wildlife", "Flamingos, vicuñas, Puñihuil penguins, sea lions", LensPref.LONG, "penguins"),
        Scene("birds", "🦅", "Birds in flight", "Andean condors, flamingos taking off", LensPref.LONG, "condor"),
        Scene("water", "💧", "Waterfalls & silky water", "Saltos del Petrohué, Lake District rivers", LensPref.WIDE, "petrohue"),
        Scene("astro", "🌌", "Milky Way & stars", "Atacama: one of the darkest skies on Earth", LensPref.ASTRO, "milky_way"),
        Scene("blue", "🌃", "Blue hour & night city", "Santiago skyline, Valparaíso, Puerto Varas", LensPref.WIDE, "santiago"),
        Scene("street", "🎨", "Streets & colourful towns", "Valparaíso, Chiloé palafitos, markets", LensPref.NORMAL, "valparaiso"),
        Scene("forest", "🌿", "Rainforest & moody weather", "Alerce Andino, Chiloé, rainy Lake District days", LensPref.WIDE, "forest"),
        Scene("geysers", "♨️", "El Tatio geysers at dawn", "Steam, low light, −10 °C", LensPref.WIDE, "el_tatio"),
        Scene("portrait", "🙂", "People & travel portraits", "Your travel companions, locals (ask first!)", LensPref.PORTRAIT, "palafitos"),
        Scene("indoor", "⛪", "Indoors & low light", "Chiloé's wooden churches, museums, restaurants", LensPref.NORMAL),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }
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
                bag.minWith(compareBy<Lens> { it.apertureAt(focalFor(it, pref, crop)) + distancePenalty(it, target) })
            }
            LensPref.NORMAL -> {
                val target = (35 / crop).roundToInt()
                bag.minWith(compareBy<Lens> { distancePenalty(it, target) }.thenBy { if (it.isZoom) 0 else 1 })
            }
        }
    }

    private fun distancePenalty(lens: Lens, target: Int): Double = when {
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

    private fun stabilisationStops(body: CameraBody, lens: Lens): Double = when {
        body.ibis && lens.hasIs -> 4.0
        body.ibis -> 3.0
        lens.hasIs -> 2.5
        else -> 0.0
    }

    /** Slowest shutter speed that is reasonably safe handheld (people, not just walls). */
    fun handheldShutter(body: CameraBody, lens: Lens, focal: Int): Double {
        val eq = focal * body.sensor.crop
        val t = (1.0 / eq) * 2.0.pow(stabilisationStops(body, lens))
        return Exposure.shutterAtMost(min(t, 1 / 15.0))
    }

    /** NPF rule: longest exposure (s) before stars visibly turn into lines. */
    fun npfSeconds(body: CameraBody, lens: Lens, focal: Int): Double {
        val n = lens.apertureAt(focal)
        return (35 * n + 30 * body.pixelPitchUm) / focal
    }

    /** The classic (looser) 500 rule, for comparison. */
    fun rule500(body: CameraBody, focal: Int): Double = 500.0 / (focal * body.sensor.crop)

    private fun clampAperture(target: Double, lens: Lens, focal: Int): Double =
        Exposure.nearestAperture(max(target, lens.apertureAt(focal)))

    private fun iso(n: Int) = "ISO $n"

    // ------------------------------------------------------------------ the advice

    fun recommend(scene: Scene, body: CameraBody, lens: Lens, focal: Int, support: Support): Recommendation {
        val crop = body.sensor.crop
        val tripod = support == Support.TRIPOD
        val wideOpen = lens.apertureAt(focal)
        val hh = handheldShutter(body, lens, focal)
        val autoIso = "Auto (max ${body.maxGoodIso})"
        val hasIs = lens.hasIs || body.ibis
        val dslr = !body.mirrorless
        val eq = (focal * crop).roundToInt()

        val s = mutableListOf<Setting>()
        val tips = mutableListOf<String>()
        val warn = mutableListOf<String>()

        s += Setting(
            "Focal length",
            "$focal mm" + if (crop != 1.0) " (looks like $eq mm on full frame)" else "",
            when (scene.lensPref) {
                LensPref.WIDE -> "Zoom out fully to fit in the big scenery."
                LensPref.ASTRO -> "Widest setting: more sky, and you can expose longer before stars streak."
                LensPref.LONG -> "Zoom in fully: animals are shy, and you should keep your distance anyway."
                LensPref.PORTRAIT -> "A short telephoto flatters faces and blurs the background."
                LensPref.NORMAL -> "A natural, 'what you see' angle for streets and interiors."
            },
        )

        fun stabiliser(onWhy: String = "Lets you shoot handheld at slower speeds without blur.") {
            if (!hasIs) return
            s += if (tripod) {
                Setting("Image stabilizer (IS)", "OFF", "On a tripod the stabilizer can 'hunt' and add blur.")
            } else {
                Setting("Image stabilizer (IS)", "ON", onWhy)
            }
        }

        val quality = Setting(
            "Image quality",
            "RAW + JPEG",
            "JPEG to share right away; RAW keeps extra detail in bright skies and shadows to fix later.",
        )

        val rec: Recommendation = when (scene.id) {

            "landscape" -> {
                val f = clampAperture(if (crop >= 2.0) 5.6 else if (crop > 1) 8.0 else 9.0, lens, focal)
                s += Setting("Aperture", formatAperture(f), "Sharpest setting of most lenses, and everything from near to far is in focus.")
                if (tripod) {
                    s += Setting("ISO", iso(100), "Lowest ISO = cleanest image. The tripod takes care of slow shutter speeds.")
                    s += Setting("Drive", "2-second self-timer", "Pressing the shutter shakes the camera; the timer lets it settle.")
                } else {
                    s += Setting("ISO", autoIso, "Lets the camera raise ISO only when needed.")
                    s += Setting("Auto ISO min. shutter", formatShutter(min(hh, 1 / 125.0).let { Exposure.shutterAtMost(it) }),
                        "Wind on the Altiplano shakes you and the camera. This keeps shots sharp.")
                    s += Setting("Drive", "Single shot", "")
                }
                s += Setting("Focus", "One Shot AF, single point ⅓ into the scene", "Focusing a third into the scene keeps the foreground and mountains sharp.")
                s += Setting("Metering", "Evaluative", "Canon's all-round metering is very good for landscapes.")
                s += Setting("Exposure compensation", "0, or −⅓ with lots of bright sky", "Protects bright clouds from turning pure white.")
                s += Setting("White balance", "Daylight ☀", "Keeps colours consistent from shot to shot (Auto sometimes greys out warm light).")
                stabiliser()
                s += quality
                tips += "A circular polarising filter makes the Altiplano lagoons and Lake Llanquihue deep blue and cuts glare. Turn it while looking through the viewfinder."
                tips += "Put something in the foreground (rocks, flowers, a person) to show scale."
                tips += "Check the histogram: if the graph hits the right edge, dial in −⅓ or −⅔."
                Recommendation("Av (Aperture priority)", "You set the aperture for depth of field; the camera picks the shutter speed.", s, tips, warn)
            }

            "golden" -> {
                val f = clampAperture(8.0, lens, focal)
                s += Setting("Aperture", formatAperture(f), "Front-to-back sharpness. Go to f/11–f/16 if you want a 'sunstar' on the sun.")
                s += Setting("ISO", if (tripod) iso(100) else autoIso, if (tripod) "Clean images; the tripod handles slow speeds." else "Light falls fast; Auto ISO keeps up.")
                s += Setting("Exposure compensation", "−⅔ to −1", "Keeps the sky's colours rich instead of washed out. Shadows can be lifted later from RAW.")
                s += Setting("White balance", "Daylight ☀ or Cloudy ☁", "Auto white balance 'corrects' the warm glow away. These keep it.")
                s += Setting("Metering", "Evaluative", "")
                s += Setting("Bracketing (AEB)", "±2 stops, 3 shots", "Captures the bright sky and dark ground separately so nothing is lost; merge later or pick the best.")
                s += Setting("Drive", if (tripod) "2-second self-timer" else "High-speed continuous (for brackets)", "")
                s += Setting("Focus", "One Shot AF on the main subject", "")
                stabiliser()
                s += quality
                tips += "Open the 'Sun & Stars' tab for exact golden & blue hour times where you are."
                tips += "Osorno volcano glows pink for 10–15 minutes after sunset, seen from Frutillar or Puerto Varas. Valle de la Luna is best from Mirador de Kari."
                tips += "After sunset, keep shooting: the best colour in the clouds often comes 10–20 minutes later."
                Recommendation("Av (Aperture priority)", "You fix the aperture; the camera follows the fast-changing light.", s, tips, warn)
            }

            "snow" -> {
                val f = clampAperture(if (crop > 1) 8.0 else 9.0, lens, focal)
                s += Setting("Aperture", formatAperture(f), "Sharp from front to back.")
                s += Setting("ISO", if (tripod) iso(100) else autoIso, "There's lots of light here, ISO will stay low.")
                s += Setting("Exposure compensation", "+1 to +1⅔", "The camera thinks white = too bright and turns snow and salt grey. Adding exposure makes it white again.")
                s += Setting("Metering", "Evaluative", "")
                s += Setting("White balance", "Daylight ☀", "Auto WB can tint snow blue.")
                s += Setting("Highlight alert ('blinkies')", "ON in playback menu", "Blinking areas are blown out; reduce compensation by ⅓ until they mostly stop.")
                stabiliser()
                s += quality
                tips += "A polarising filter deepens the sky over salt flats, but skip it on ultra-wide angles: the sky turns patchy."
                tips += "Fine salt and dust in Atacama: change lenses with the camera pointing down, out of the wind."
                tips += "Flamingos at Laguna Chaxa? Switch to the Wildlife scene."
                Recommendation("Av (Aperture priority)", "You set the aperture; exposure compensation fixes the 'grey snow' problem.", s, tips, warn)
            }

            "wildlife" -> {
                val t = Exposure.nearestShutter(max(1 / 1000.0, min(hh, 1.0 / (2 * eq))))
                s += Setting("Shutter speed", "${formatShutter(t)} (moving) · 1/500 s (resting)", "Freezes walking guanacos and waddling penguins.")
                s += Setting("Aperture", "Camera chooses (≈ ${formatAperture(wideOpen)})", "Wide open lets the most light in and blurs the background.")
                s += Setting("ISO", autoIso, "Shutter speed matters more than a bit of noise.")
                s += Setting(
                    "Autofocus",
                    if (body.animalAf) "Servo AF + subject detection: Animals, eye detection ON" else if (dslr) "AI Servo AF, Zone AF" else "Servo AF, Zone AF",
                    "Servo keeps refocusing while the animal moves.",
                )
                s += Setting("Drive", "High-speed continuous", "Take bursts of 3–6 and keep the sharpest one.")
                s += Setting("Metering", "Evaluative", "")
                s += Setting("Exposure compensation", "0 (+⅔ if the animal is dark against bright sky)", "")
                stabiliser("Lets you hold a long lens steady.")
                s += quality
                tips += "Get low to the animal's eye level; it makes photos far more intimate."
                tips += "Puñihuil (Chiloé): Humboldt and Magellanic penguins from small boats, best in the morning. Flamingos at Laguna Chaxa, vicuñas on the road to the Altiplano lagoons, sea lions at Angelmó market in Puerto Montt."
                tips += "Keep your distance and never chase animals. Let them come to you."
                if (focal * crop < 150) warn += "Your longest lens is fairly short for wildlife. Get as close as is safe and crop later."
                Recommendation("Tv (Shutter priority)", "You set a fast shutter speed to freeze motion; the camera handles the rest.", s, tips, warn)
            }

            "birds" -> {
                s += Setting("Shutter speed", "1/2000 s", "Wings move fast. Condors glide, so 1/1250 s also works for them.")
                s += Setting("Aperture", "Camera chooses (≈ ${formatAperture(wideOpen)})", "")
                s += Setting("ISO", autoIso, "")
                s += Setting(
                    "Autofocus",
                    if (body.animalAf) "Servo AF + subject: Animals (birds), whole-area tracking" else if (dslr) "AI Servo AF, Zone AF or Large Zone" else "Servo AF, Zone AF",
                    "Finds the bird against the sky and keeps tracking it.",
                )
                s += Setting("Drive", "High-speed continuous", "")
                s += Setting("Exposure compensation", "+⅔ to +1 against bright sky", "Otherwise birds become dark silhouettes.")
                if (hasIs) s += Setting("Image stabilizer", "ON (Mode 2 for panning if your lens has it)", "")
                s += quality
                tips += "Andean condors ride the thermals mid-morning. Best near Santiago: Cajón del Maipo and the road to Farellones."
                tips += "Start tracking the bird before you press the shutter, and keep following through."
                Recommendation("Tv (Shutter priority)", "Fast shutter speed is everything for birds in flight.", s, tips, warn)
            }

            "water" -> {
                if (!tripod) warn += "Silky water needs a tripod (or rest the camera on a rock / your bag and use the self-timer)."
                val f = clampAperture(11.0, lens, focal)
                s += Setting("Aperture", formatAperture(f), "Small opening lets in less light, so the shutter can stay open longer.")
                s += Setting("ISO", iso(100), "Lowest ISO, also to allow a slower shutter.")
                s += Setting("Shutter speed", "0.5 – 2 s", "Long enough to blur water into silk. Try several.")
                s += Setting("ND filter", "ND8 (3 stops) or ND64 (6 stops) in daylight", "Without one, daylight is too bright for long exposures. See the ND calculator in the Meter tab.")
                s += Setting("Focus", "One Shot AF, then switch the lens to MF", "So focus doesn't change between shots.")
                s += Setting("Drive", "2-second self-timer", "")
                stabiliser()
                s += quality
                tips += "Overcast days or shade are perfect: less light, so you might not need an ND filter."
                tips += "Want frozen splashes instead? Use 1/1000 s in Tv mode."
                tips += "Saltos del Petrohué (1 h from Puerto Varas) is the classic: turquoise water over black lava with Osorno behind. Mind the spray on your lens."
                Recommendation("M (Manual)", "You control everything so each shot in a series is the same.", s, tips, warn)
            }

            "astro" -> {
                val npf = npfSeconds(body, lens, focal)
                val t = Exposure.shutterAtMost(min(npf, 30.0))
                val loose = Exposure.shutterAtMost(min(npf * 1.5, 30.0))
                val cap = if (body.maxGoodIso >= 3200) 6400 else 3200
                val needed = 3200 * (wideOpen / 2.8).pow(2) * (15.0 / t)
                val isoVal = Exposure.nearestIso(needed.coerceIn(1600.0, cap.toDouble()))
                if (!tripod) warn += "Stars need a tripod. Without one, rest the camera on a wall or bag and use the 2-s timer."
                s += Setting("Aperture", formatAperture(wideOpen) + " (wide open)", "Gather as much starlight as possible.")
                s += Setting("Shutter speed", formatShutter(t),
                    "NPF rule for your camera: up to ${String.format(Locale.US, "%.0f", npf)} s before stars blur. Up to ${formatShutter(loose)} is fine for phone-size viewing.")
                s += Setting("ISO", iso(isoVal), "High enough to see the Milky Way. Noise can be reduced later; an underexposed photo can't be saved.")
                s += Setting("Focus", "MF: magnify live view 10× on the brightest star", "Turn the ring until the star is the smallest dot. Then tape the ring!")
                s += Setting("White balance", "3900–4200 K", "Gives a natural blue-black sky instead of orange.")
                s += Setting("Drive", "2- or 10-second self-timer", "")
                s += Setting("Long exposure noise reduction", "OFF", "It doubles the wait for every photo. Turn on only for your best composition.")
                stabiliser()
                s += quality
                if (dslr) s += Setting("Viewfinder", "Use Live View (rear screen)", "You can't focus on stars through an optical viewfinder.")
                tips += "Check the 'Sun & Stars' tab: it shows when it's really dark, when the moon is down and when the Milky Way centre is up."
                tips += "In December the Milky Way centre is hidden behind the sun. Instead shoot the bright southern Milky Way (Carina Nebula, Southern Cross) and the Magellanic Clouds, which rise later in the night. The Sun & Stars tab shows when."
                tips += "Atacama is the place for stars on your trip. The Puerto Montt region is often cloudy and has more light pollution."
                tips += "Turn the screen brightness down and use a red headlamp to keep your night vision."
                tips += "Atacama nights are close to 0 °C: bring warm clothes and a spare battery in an inside pocket."
                if (wideOpen > 3.5) warn += "Your lens is fairly 'slow' (${formatAperture(wideOpen)}). It works, but expect noise. Take several shots of the same view to stack later."
                Recommendation("M (Manual)", "In the dark the camera can't meter, so you set everything yourself.", s, tips, warn)
            }

            "blue" -> {
                if (tripod) {
                    val f = clampAperture(8.0, lens, focal)
                    s += Setting("Aperture", formatAperture(f), "Sharp throughout; city lights become little stars at f/11+.")
                    s += Setting("ISO", iso(100), "Clean, noise-free night shots.")
                    s += Setting("Shutter speed", "Camera chooses (often 2–15 s)", "Car lights become streaks, which looks great.")
                    s += Setting("Drive", "2-second self-timer", "")
                } else {
                    s += Setting("Aperture", formatAperture(wideOpen) + " (wide open)", "Let in all the light you can.")
                    s += Setting("ISO", autoIso, "")
                    s += Setting("Auto ISO min. shutter", formatShutter(hh), "Slowest speed you can hold steady with this lens.")
                    s += Setting("Drive", "High-speed continuous, bursts of 3", "One of the three is usually sharpest.")
                }
                s += Setting("Exposure compensation", "−⅓ to −⅔", "Night scenes should look like night, and bright lights stay under control.")
                s += Setting("White balance", "Auto, or 4000 K for a bluer sky", "")
                s += Setting("Metering", "Evaluative", "")
                stabiliser()
                s += quality
                tips += "Blue hour lasts only ~20 minutes after sunset. The sky then matches the city lights perfectly."
                tips += "Valparaíso: shoot from Paseo Gervasoni or Cerro Alegre looking over the bay."
                tips += "Santiago: Cerro San Cristóbal or Cerro Santa Lucía with the Andes behind the skyline."
                Recommendation("Av (Aperture priority)", "Aperture fixed; the camera works out the long shutter speed.", s, tips, warn)
            }

            "street" -> {
                val f = clampAperture(5.6, lens, focal)
                s += Setting("Aperture", formatAperture(f), "Enough depth of field for quick, unplanned shots.")
                s += Setting("ISO", autoIso, "")
                s += Setting("Auto ISO min. shutter", "1/250 s", "Freezes people walking by.")
                s += Setting("Autofocus", if (body.mirrorless) "Servo AF, face/eye detection ON, whole area" else "One Shot AF, centre point", "")
                s += Setting("Metering", "Evaluative", "")
                s += Setting("Exposure compensation", "0 (−⅓ in harsh sun)", "")
                s += Setting("White balance", "Auto", "")
                stabiliser()
                s += quality
                tips += "Valparaíso murals: midday light is harsh; shady streets and overcast days make colours pop."
                tips += "Always ask before photographing people up close. A smile and '¿Puedo tomar una foto?' goes a long way."
                tips += "Keep the camera strap around your wrist or body in busy areas of Santiago and Valparaíso."
                Recommendation("Av (Aperture priority)", "Quick and easy: you set depth of field, the camera does the rest.", s, tips, warn)
            }

            "portrait" -> {
                s += Setting("Aperture", formatAperture(wideOpen) + " (wide open)", "Blurs the background so the person stands out.")
                s += Setting("ISO", autoIso, "")
                s += Setting("Autofocus", if (body.mirrorless) "One Shot or Servo, eye detection ON" else "One Shot AF, single point on the nearest eye", "Sharp eyes are what make a portrait.")
                s += Setting("Metering", "Evaluative", "")
                s += Setting("Exposure compensation", "+⅓ (+⅔ to +1 with the sun behind them)", "Keeps faces bright.")
                s += Setting("White balance", "Auto (or Cloudy for warmer skin)", "")
                stabiliser()
                s += quality
                tips += "Avoid midday sun on faces: put people in the shade, or with the sun behind them and add exposure. Desert sun at 2,400 m is extra harsh."
                tips += "Step back and zoom in instead of standing close with a wide angle; faces look better."
                Recommendation("Av (Aperture priority)", "Wide aperture for a blurred background; the camera handles the rest.", s, tips, warn)
            }

            "geysers" -> {
                val f = clampAperture(5.6, lens, focal)
                s += Setting("Aperture", formatAperture(f), "")
                s += Setting("ISO", if (tripod) iso(400) else autoIso, "It's still dim at dawn.")
                if (!tripod) s += Setting("Auto ISO min. shutter", formatShutter(min(hh, 1 / 60.0).let { Exposure.shutterAtMost(it) }), "Hands shake in the cold.")
                s += Setting("Exposure compensation", "−⅔ when shooting into the sun", "Backlit steam is very bright; this keeps texture in it.")
                s += Setting("White balance", "Daylight ☀", "Keeps the golden-blue dawn colours.")
                s += Setting("Metering", "Evaluative", "")
                s += Setting("Drive", "High-speed continuous", "Eruptions are short; take bursts.")
                stabiliser()
                s += quality
                tips += "Tours leave San Pedro around 4:30–5:00. The steam is best just before and right after sunrise (~6:30)."
                tips += "It can be −10 °C at 4,300 m. Keep a spare battery in an inside pocket; cold batteries die fast."
                tips += "Put the camera in a bag before going back into a warm car to avoid condensation."
                Recommendation("Av (Aperture priority)", "The light changes minute by minute; let the camera adjust the shutter.", s, tips, warn)
            }

            "forest" -> {
                s += Setting("Aperture", formatAperture(clampAperture(8.0, lens, focal)), "Depth through the trees.")
                if (tripod) {
                    s += Setting("ISO", iso(100), "Forests are darker than they look; the tripod lets you keep ISO low.")
                    s += Setting("Drive", "2-second self-timer", "")
                } else {
                    s += Setting("ISO", autoIso, "Under the canopy the light drops fast.")
                    s += Setting("Auto ISO min. shutter", formatShutter(hh), "Slowest speed you can hold steady with this lens.")
                }
                s += Setting("Exposure compensation", "−⅓ to −⅔", "Keeps the bright sky gaps and light rays from blowing out and keeps the moody feel.")
                s += Setting("White balance", "Cloudy ☁", "Removes the cold green-blue cast of shade and rain.")
                s += Setting("Metering", "Evaluative", "")
                s += Setting("Focus", "One Shot AF on a trunk or fern in the foreground", "")
                stabiliser()
                s += quality
                tips += "Rainy and overcast days are a gift here: soft light, saturated greens, no harsh shadows."
                tips += "Look for layers: a fern in front, mossy trunks in the middle, mist behind."
                tips += "Bring a rain cover or a plastic bag and a microfibre cloth; wipe the front of the lens before every shot."
                tips += "Nalca (giant rhubarb) leaves make great foreground frames in Chiloé and Alerce Andino."
                Recommendation("Av (Aperture priority)", "You set depth of field; the camera finds the (often slow) shutter speed.", s, tips, warn)
            }

            else -> { // indoor
                s += Setting("Aperture", formatAperture(wideOpen) + " (wide open)", "Let in as much light as possible.")
                s += Setting("ISO", autoIso, "")
                s += Setting("Auto ISO min. shutter", formatShutter(hh), "The slowest speed you can hold steady with this lens.")
                s += Setting("Flash", "OFF", "Often not allowed in churches/museums, and it flattens the light anyway.")
                s += Setting("White balance", "Auto (White priority) in warm-lit rooms", "Removes the orange cast of tungsten lights.")
                s += Setting("Drive", "Continuous, bursts of 3", "")
                stabiliser()
                s += quality
                tips += "Brace yourself against a pillar or wall, breathe out and squeeze the shutter gently."
                tips += "Check the rules: some churches and museums don't allow photos at all. Chiloé's UNESCO wooden churches are usually fine without flash."
                Recommendation("Av (Aperture priority)", "Wide aperture and Auto ISO for maximum light.", s, tips, warn)
            }
        }
        return rec
    }

    /** Small glossary for beginners. */
    val basics = listOf(
        "Av" to "Aperture priority. You turn the dial to choose the f-number; the camera picks the shutter speed. Best for most situations.",
        "Tv" to "Shutter priority. You choose the shutter speed (e.g. 1/1000 to freeze motion); the camera picks the aperture.",
        "M" to "Manual. You choose both. Use it for stars, waterfalls and when you want every shot identical.",
        "Aperture (f/…)" to "Small number (f/2.8) = more light and blurry background. Big number (f/11) = everything sharp, less light.",
        "Shutter speed" to "1/1000 s freezes action. 1 s blurs moving water or shaky hands.",
        "ISO" to "Sensor sensitivity. Low (100) = cleanest. High (6400) = brighter but grainier.",
        "Exposure compensation (+/−)" to "Tell the camera 'brighter' or 'darker'. In Av/Tv: turn the rear dial (or press Av± on Rebels).",
        "Q button" to "Opens the quick menu on the screen: fastest way to change ISO, AF, drive mode and white balance.",
        "Histogram" to "Graph of brightness in playback (press INFO). Touching the right edge = blown-out highlights.",
    )
}
