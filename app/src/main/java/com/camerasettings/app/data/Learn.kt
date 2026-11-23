package com.camerasettings.app.data

/**
 * Camera School: a short beginner course, a problem solver and a glossary.
 * Text can contain {AV}, {TV}, {Q}, {AFS}, {AFC}, {IS}, {RAW}, {STYLE}; [Learn.fill] replaces
 * them with the words used on the user's own camera brand.
 */
data class Quiz(val question: String, val options: List<String>, val answer: Int, val explain: String)

data class Lesson(
    val id: String,
    val unit: String,
    val title: String,
    val minutes: Int,
    val summary: String,
    val sections: List<Pair<String, String>>,
    val tryIt: List<String>,
    val quiz: List<Quiz>,
    /** Show the interactive exposure playground inside the lesson. */
    val playground: Boolean = false,
    /** A scene to practise with in the Home tab. */
    val practiceScene: String? = null,
)

data class Problem(val id: String, val emoji: String, val title: String, val fixes: List<Pair<String, String>>)

object Learn {

    fun fill(text: String, brand: Brand?): String {
        val v = (brand ?: Brand.OTHER).vocab
        return text
            .replace("{AV}", v.av)
            .replace("{TV}", v.tv)
            .replace("{Q}", v.quickMenu)
            .replace("{AFS}", v.afSingle)
            .replace("{AFC}", v.afContinuous)
            .replace("{IS}", v.stabilizer)
            .replace("{RAW}", v.raw)
            .replace("{STYLE}", v.pictureStyle)
    }

    val lessons: List<Lesson> = listOf(
        // ======================================================== 1. Getting started
        Lesson(
            "meet", "Getting started", "Meet your camera", 3,
            "The handful of buttons you'll actually use.",
            listOf(
                "The shutter button" to "Press it halfway and the camera focuses (you hear a beep or see a green square). Press it all the way to take the photo. This 'half-press' is the most important habit in photography.",
                "The mode dial" to "The round dial on top with letters. AUTO (often a green symbol) lets the camera decide everything. {AV} is aperture priority: your new favourite. {TV} is shutter priority. M is manual. (Some cameras, like most Fujifilms, use separate dials instead.)",
                "The main dial" to "A wheel near the shutter button. In {AV} it changes the f-number, in {TV} the shutter speed.",
                "The quick menu" to "The {Q} opens a menu on the screen with the most useful settings: ISO, focus, white balance, drive mode. You rarely need the big MENU.",
                "Playback" to "The ▶ button shows your photos. Use the magnify button (or pinch on a touchscreen) to zoom in and check sharpness.",
                "On the lens" to "The zoom ring (if it's a zoom), sometimes an AF/MF switch (keep it on AF) and a stabilizer switch ({IS}, keep it ON when handheld).",
            ),
            listOf(
                "Find the shutter button, the mode dial, the main dial and the {Q} on your camera.",
                "Half-press while pointing at something and watch the focus confirmation, then press fully.",
                "Press ▶ and zoom into the photo you just took.",
            ),
            listOf(
                Quiz("What happens when you press the shutter button halfway?", listOf("It takes a photo", "The camera focuses and measures the light", "The flash pops up"), 1, "Half-press = focus and measure. Full press = photo."),
                Quiz("Where are ISO, focus and white balance quickest to change?", listOf("In the {Q}", "Only in the big MENU", "In the playback screen"), 0, "The quick menu has all the everyday settings in one place."),
            ),
        ),
        Lesson(
            "steady", "Getting started", "Hold it steady", 2,
            "Most blurry photos are caused by shaky hands, not by the camera.",
            listOf(
                "Good grip" to "Right hand on the grip, finger on the shutter button. Left hand under the lens, palm up, like holding a tray. Tuck your elbows against your body.",
                "Use your body" to "Put the viewfinder against your eye (your head is a third support), or hold the screen close. Lean against a wall, tree or railing.",
                "Breathe" to "Breathe out slowly and squeeze the button gently, don't stab it.",
                "Stabilizer" to "Leave {IS} ON when you hold the camera. Switch it OFF only on a tripod.",
            ),
            listOf(
                "Indoors, take one photo holding the camera with one hand at arm's length, and one with the good grip.",
                "Zoom into both photos with ▶ and compare sharpness.",
            ),
            listOf(
                Quiz("What's the best way to hold the camera?", listOf("At arm's length so you see the screen", "Elbows tucked in, left hand under the lens", "With one hand"), 1, "Elbows in and a hand under the lens make you a human tripod."),
                Quiz("When should the stabilizer be OFF?", listOf("Always", "When handheld", "On a tripod"), 2, "On a tripod the stabilizer can actually add blur."),
            ),
        ),
        Lesson(
            "auto", "Getting started", "Your first photos (Auto is fine!)", 2,
            "Start in AUTO and concentrate on what's in the picture.",
            listOf(
                "Why start in Auto" to "Modern cameras are very good in AUTO. Use it for your first day so you can learn the buttons and think about what you're photographing.",
                "When Auto fails" to "Auto struggles with snow and beaches (grey), sunsets (washed out), moving things (blur) and blurry backgrounds. That's what the next lessons fix.",
                "Check every photo" to "After each shot, press ▶, zoom in on the most important part and check it's sharp. It takes 3 seconds and saves your trip photos.",
            ),
            listOf(
                "Set the mode dial to AUTO and take 10 photos around you: a person, a plant, a window, something moving.",
                "Check each photo with ▶ and the magnifier.",
            ),
            listOf(
                Quiz("Is it OK to start in AUTO?", listOf("Yes, to learn the camera and composition", "No, professionals never use it"), 0, "Auto is a fine start; the creative modes come next."),
            ),
            practiceScene = "street",
        ),
        Lesson(
            "review", "Getting started", "Look at your photo like a pro", 3,
            "Zoom in, and read the histogram.",
            listOf(
                "Zoom in" to "On the camera's small screen almost everything looks sharp. Zoom in on eyes, text or leaves to really check.",
                "The histogram" to "Press INFO / DISP during playback to show a graph of brightness: dark tones on the left, bright on the right. A big pile against the right edge means parts are pure white; against the left edge, pure black.",
                "Blinkies" to "Turn on the highlight warning in the playback menu: areas that are pure white will blink. A few blinking lamps or the sun are fine; blinking clouds or faces are not.",
            ),
            listOf(
                "Take a photo of a bright window and turn on the histogram in playback.",
                "Turn on the highlight warning and see which parts blink.",
            ),
            listOf(
                Quiz("The histogram is piled up against the right edge. What does that mean?", listOf("The photo is too dark", "Parts are pure white (overexposed)", "The photo is blurry"), 1, "Right edge = brightest tones. Add − exposure compensation."),
            ),
        ),
        // ======================================================== 2. Light & exposure
        Lesson(
            "exposure", "Light & exposure", "What is exposure?", 3,
            "Three controls decide how bright your photo is. Play with them below.",
            listOf(
                "Filling a bucket" to "Think of the photo as a bucket filled with light. The aperture is how wide the tap is, the shutter speed is how long it runs, and ISO is how sensitive the bucket is (like a smaller bucket that fills faster, but looks grainier).",
                "Every control has a side effect" to "Aperture changes how blurry the background is. Shutter speed freezes or blurs movement. ISO adds grain at high values. Choosing settings is choosing the side effect you want.",
                "Stops" to "Photographers measure light in 'stops'. One stop = double or half the light. f/4 → f/5.6, 1/250 → 1/125 and ISO 100 → 200 are all one stop.",
            ),
            listOf(
                "Use the playground below: make the photo correctly exposed with a blurred background.",
                "Then make it correctly exposed with a frozen cyclist.",
            ),
            listOf(
                Quiz("Which three controls set the brightness of a photo?", listOf("Zoom, focus and flash", "Aperture, shutter speed and ISO", "White balance, ISO and zoom"), 1, "The 'exposure triangle'."),
                Quiz("You double the shutter time (1/250 → 1/125). How much more light?", listOf("Twice as much (one stop)", "Four times", "The same"), 0, "Double the time = double the light = one stop."),
            ),
            playground = true,
        ),
        Lesson(
            "aperture", "Light & exposure", "Aperture: blurry or sharp background", 3,
            "Small f-number = blurry background. Big f-number = everything sharp.",
            listOf(
                "The confusing bit" to "The f-number is a fraction: f/2 is a BIG opening (lots of light, blurry background), f/16 is a SMALL opening (less light, everything sharp). Small number, big blur.",
                "When to use what" to "Portraits, pets, flowers: the smallest number your lens allows. Landscapes, groups, buildings: f/8. Sunstars and maximum depth: f/11–f/16.",
                "More blur" to "Zoom in, get closer to your subject, and keep the background far away. A lens like a 50mm f/1.8 makes this easy.",
                "In {AV} mode" to "Turn the dial to choose the f-number. The camera finds the right shutter speed automatically.",
            ),
            listOf(
                "Set the mode dial to {AV}. Photograph a flower or a cup with the smallest f-number your lens allows.",
                "Take the same photo at f/11. Compare the backgrounds.",
            ),
            listOf(
                Quiz("You want a blurry background behind a person. Which f-number?", listOf("f/2.8 (or your lowest)", "f/16"), 0, "Small number = big opening = blurry background."),
                Quiz("For a landscape that is sharp from front to back?", listOf("f/1.8", "f/8"), 1, "f/8 gives depth and is where most lenses are sharpest."),
            ),
            practiceScene = "portrait",
        ),
        Lesson(
            "shutter", "Light & exposure", "Shutter speed: freeze or blur", 3,
            "Fast shutter freezes movement; slow shutter shows it.",
            listOf(
                "Fractions of a second" to "1/1000 s is very fast (freezes a running dog), 1/60 s is slow-ish, 1 second is very slow (moving water turns silky).",
                "The handheld rule" to "To avoid blur from your own hands, use at least 1 divided by the zoom number: at 50 mm use 1/50 s or faster. The stabilizer buys you a few stops extra.",
                "In {TV} mode" to "Choose the shutter speed; the camera picks the aperture. Great for sports, kids, pets and birds.",
            ),
            listOf(
                "Set {TV}. Photograph running water or a passing car at 1/1000 s, then at 1/15 s.",
                "Compare: frozen drops vs. smooth movement.",
            ),
            listOf(
                Quiz("Your child is running. Which shutter speed?", listOf("1/30 s", "1/1000 s"), 1, "Fast movement needs a fast shutter."),
                Quiz("You're zoomed to 200 mm, handheld, no stabilizer. The slowest safe speed is about…", listOf("1/200 s", "1/20 s"), 0, "1 ÷ focal length: 1/200 s."),
            ),
            practiceScene = "action",
        ),
        Lesson(
            "iso", "Light & exposure", "ISO: brightness at a cost", 2,
            "Higher ISO brightens the photo but adds grain.",
            listOf(
                "What it does" to "ISO 100 is the cleanest. Every doubling (200, 400, 800…) makes the photo one stop brighter, with a little more grain ('noise').",
                "Auto ISO is your friend" to "Set ISO to AUTO and let the camera raise it only when needed. Many cameras let you set a maximum (for example 6400) in the menu.",
                "Grain vs blur" to "A slightly grainy sharp photo is always better than a clean blurry one. Don't be afraid of ISO 3200 indoors.",
            ),
            listOf(
                "In a dim room, take the same photo at ISO 100 and at ISO 6400 on a tripod or table. Zoom in and compare the grain.",
            ),
            listOf(
                Quiz("Indoors, your photos are blurry at ISO 100. Best fix?", listOf("Raise ISO (or use AUTO)", "Use f/16"), 0, "Higher ISO allows a faster, sharper shutter speed."),
            ),
            practiceScene = "indoor",
        ),
        Lesson(
            "expcomp", "Light & exposure", "Brighter or darker: the +/– button", 2,
            "Exposure compensation tells the camera you want it brighter or darker.",
            listOf(
                "Why the camera gets it wrong" to "The camera assumes the world is mid-grey. Snow, beaches and white walls look 'too bright' to it, so it makes them grey. Dark scenes get lifted to grey too.",
                "The fix" to "+1 for snow, sand and bright beaches. −⅔ for sunsets and dark scenes. 0 for everything else. It works in AUTO-ISO, {AV} and {TV}.",
                "Don't forget to reset" to "The setting stays until you change it. Set it back to 0 after the special scene.",
            ),
            listOf(
                "Photograph a white sheet of paper filling the frame: it comes out grey. Add +1 and try again.",
            ),
            listOf(
                Quiz("Your snow photos look grey. What do you do?", listOf("Add + exposure compensation", "Add − exposure compensation", "Lower the ISO"), 0, "The camera darkens bright scenes; + makes snow white again."),
            ),
            practiceScene = "bright",
        ),
        // ======================================================== 3. Modes & focus
        Lesson(
            "avmode", "Modes & focus", "Your everyday mode: {AV}", 2,
            "One setup that works for 80% of travel photos.",
            listOf(
                "The recipe" to "Mode dial on {AV}. ISO on AUTO. Exposure compensation 0. Focus on {AFS} with a single focus point in the middle.",
                "Then just choose the f-number" to "Lowest number for people, pets and details with a blurry background. f/8 for landscapes, groups and buildings.",
                "When to switch" to "Moving subjects → {TV} at 1/1000. Night sky, fireworks, waterfalls → M (use the Home tab for the exact settings).",
            ),
            listOf("Set up the recipe on your camera and use it for a whole walk."),
            listOf(
                Quiz("In {AV} mode, what do YOU choose?", listOf("The f-number (aperture)", "The shutter speed", "Nothing"), 0, "You pick the aperture; the camera picks the shutter speed."),
            ),
            practiceScene = "landscape",
        ),
        Lesson(
            "focus", "Modes & focus", "Focus: single or continuous", 3,
            "Tell the camera what to focus on, and whether it moves.",
            listOf(
                "{AFS}" to "Single focus: half-press, the camera focuses once and locks. For things that stand still.",
                "{AFC}" to "Continuous focus: keeps refocusing while you hold the half-press. For children, pets, sports and birds.",
                "Focus points" to "Choose a single point and move it onto the eye or the most important part. Or use eye/animal detection if your camera has it.",
                "Focus and recompose" to "Put the centre point on the subject, half-press, keep holding, move the camera to frame the shot, then press fully.",
            ),
            listOf(
                "Set {AFS}, focus on a near object, recompose and shoot.",
                "Set {AFC}, follow a moving person or pet while holding the half-press, and take a burst.",
            ),
            listOf(
                Quiz("Your dog is running towards you. Which focus mode?", listOf("{AFS}", "{AFC}"), 1, "Continuous AF tracks the movement."),
            ),
            practiceScene = "pets",
        ),
        Lesson(
            "manual", "Modes & focus", "Manual mode without fear", 3,
            "M is only needed for a few situations, and it's easier than it looks.",
            listOf(
                "When to use M" to "When the camera can't measure the light: stars, fireworks, northern lights, long exposures. Also when you want every photo identical (panoramas).",
                "The light meter" to "In M the screen shows a scale from −3 to +3. Turn the dials until the marker sits at 0 for a 'normal' exposure, or follow the Home tab's recipe.",
                "Start from a recipe" to "Use a recipe (for example f/2.8 · 15 s · ISO 3200 for stars), take a test shot, look at it, and adjust one thing at a time.",
            ),
            listOf("Indoors, set M, f/5.6, ISO 800 and change only the shutter speed until the meter shows 0."),
            listOf(
                Quiz("When do you really need M mode?", listOf("For everyday photos", "For stars, fireworks and long exposures"), 1, "The camera can't measure light properly in those situations."),
            ),
            practiceScene = "astro",
        ),
        // ======================================================== 4. Colour & light
        Lesson(
            "wb", "Colour & light", "White balance: why photos look orange or blue", 2,
            "Tell the camera what colour the light is.",
            listOf(
                "Light has colour" to "Lamps are orange, shade is blue, sunsets are golden. Your eyes adjust; the camera needs white balance (WB).",
                "Settings" to "AUTO works most of the time. Daylight ☀ keeps sunsets warm. Cloudy ☁ makes things a little warmer. Tungsten 💡 removes orange from lamps. K (Kelvin) lets you pick a number: 4000 K for night skies.",
                "RAW fixes it later" to "If you shoot RAW, you can change white balance afterwards on the computer without any loss.",
            ),
            listOf("Photograph the same lamp-lit room with AUTO, Daylight and Tungsten white balance and compare."),
            listOf(
                Quiz("Your sunset photo looks dull and grey-ish. Try…", listOf("White balance Cloudy or Daylight", "White balance Tungsten"), 0, "Auto WB 'corrects' the warm glow away."),
            ),
        ),
        Lesson(
            "light", "Colour & light", "The best light of the day", 2,
            "When you shoot matters more than which camera you have.",
            listOf(
                "Golden hour" to "The hour after sunrise and before sunset: warm, soft light and long shadows. Landscapes and portraits glow.",
                "Blue hour" to "About 20 minutes after sunset: deep blue sky, city lights on. Perfect for cities.",
                "Midday" to "Harsh light and hard shadows. Look for shade, shoot details, or go inside.",
                "Cloudy days" to "Soft, even light: ideal for portraits, forests and waterfalls.",
            ),
            listOf("Open the Plan tab and find today's golden and blue hour where you are. Photograph the same view at midday and at golden hour."),
            listOf(
                Quiz("Best time for a warm, glowing landscape?", listOf("12:00 noon", "Golden hour"), 1, "Low sun = soft, warm light."),
            ),
            practiceScene = "golden",
        ),
        // ======================================================== 5. Better pictures
        Lesson(
            "composition", "Better pictures", "Composition: thirds, lines and foreground", 3,
            "A few simple rules make photos instantly better.",
            listOf(
                "Turn on the grid" to "Most cameras can show a 3×3 grid on the screen (look for 'grid display'). Place horizons on a line, not in the middle; put people or trees where lines cross.",
                "Leading lines" to "Roads, rivers, fences and paths lead the eye into the photo. Let them start in a corner.",
                "Foreground" to "In landscapes put something close: rocks, flowers, a person. It gives depth.",
                "Simplify" to "Ask 'what is my subject?' and remove everything else: step closer, zoom in, or change angle.",
            ),
            listOf("Take one photo with the horizon in the middle and one with the horizon on the lower third line. Which feels better?"),
            listOf(
                Quiz("Where should the horizon usually go?", listOf("Exactly in the middle", "On the top or bottom third line"), 1, "Thirds feel more balanced and dynamic."),
            ),
        ),
        Lesson(
            "people", "Better pictures", "Photographing people", 2,
            "Eyes, light and respect.",
            listOf(
                "Focus on the eyes" to "Use eye detection or put the focus point on the nearest eye.",
                "Light" to "Avoid harsh midday sun on faces: move to the shade, or put the sun behind them and add +⅔ exposure compensation.",
                "Respect" to "Ask before photographing strangers, especially children. A smile and showing the photo on the screen goes a long way.",
            ),
            listOf("Photograph a family member in the shade near a window, at eye level, with your lowest f-number."),
            listOf(
                Quiz("Harsh sun on a face. Best fix?", listOf("Move into the shade", "Use f/16"), 0, "Open shade gives soft, flattering light."),
            ),
            practiceScene = "portrait",
        ),
        // ======================================================== 6. Files & care
        Lesson(
            "files", "Files & care", "RAW, JPEG and backups", 2,
            "Never lose a photo.",
            listOf(
                "JPEG vs RAW" to "JPEG is a finished photo, small and ready to share. RAW ({RAW}) keeps everything the sensor saw for editing later. Shoot RAW + JPEG to have both.",
                "Cards" to "Use a few medium cards rather than one huge one. Format a card in the camera (menu → format) only after its photos are backed up.",
                "Back up" to "Keep two copies of every photo on two devices: a computer, a portable drive, or the cloud.",
            ),
            listOf("Set image quality to RAW + JPEG and copy today's photos to your computer or phone."),
            listOf(
                Quiz("When may you format (wipe) a memory card?", listOf("Any time", "Only after the photos are safely copied twice"), 1, "Formatting erases everything on the card."),
            ),
        ),
        Lesson(
            "care", "Files & care", "Taking care of your camera", 2,
            "Batteries, dust, rain and cold.",
            listOf(
                "Batteries" to "Always carry a charged spare. Cold drains them: keep the spare in an inside pocket.",
                "Dust and sand" to "Change lenses quickly, camera pointing down, out of the wind. Use a blower, not your breath.",
                "Rain" to "A plastic bag and a rubber band is a fine rain cover. Dry the camera at room temperature.",
                "Condensation" to "Coming from cold into a warm room, put the camera in a closed bag for an hour so it doesn't fog up inside.",
                "Cleaning the lens" to "Blow off dust first, then wipe gently with a microfibre cloth in circles.",
            ),
            listOf("Check you have: spare battery, spare card, microfibre cloth, and a plastic bag."),
            listOf(
                Quiz("You come inside from the freezing cold. What do you do with the camera?", listOf("Use it straight away", "Leave it in a closed bag to warm up slowly"), 1, "Prevents condensation inside the camera."),
            ),
        ),
    )

    val units: List<String> get() = lessons.map { it.unit }.distinct()

    fun lesson(id: String?) = lessons.firstOrNull { it.id == id }

    // ---------------------------------------------------------------- problem solver

    val problems: List<Problem> = listOf(
        Problem(
            "blurry", "🌫️", "My photos are blurry",
            listOf(
                "Everything is blurry, even still things" to "Your hands moved. Use a faster shutter speed (raise ISO or set it to AUTO), switch {IS} ON, hold the camera with elbows tucked in, or use the self-timer on a steady surface.",
                "The person is blurry but the background is sharp" to "The camera focused on the background. Use a single focus point on the person's eye and half-press before shooting.",
                "Moving things are blurry" to "Shutter too slow. Use {TV} with 1/1000 s and {AFC}.",
                "Nothing will focus at all" to "Check the lens switch is on AF (not MF), and that you're not closer than the lens can focus. Clean the lens.",
            ),
        ),
        Problem(
            "dark", "🌑", "My photos are too dark",
            listOf(
                "Everything is too dark" to "Check exposure compensation is not set to − (reset it to 0). In M mode, the meter must be near 0.",
                "The subject is dark against a bright sky or window" to "The camera exposed for the bright background. Add +1 exposure compensation, or move so the light falls on your subject.",
                "At night" to "Raise ISO (try 3200–6400), use your lowest f-number, or put the camera on something steady for a longer shutter speed.",
            ),
        ),
        Problem(
            "bright", "☀️", "My photos are too bright or white",
            listOf(
                "Everything is too bright" to "Exposure compensation might be on +. Reset it to 0. In M mode, use a faster shutter speed or a bigger f-number.",
                "The sky is pure white" to "Use −⅔ exposure compensation, or shoot when the sky is less bright (golden hour).",
                "Long exposures in daylight are white" to "Too much light: use ISO 100, f/16, or an ND filter.",
            ),
        ),
        Problem(
            "colour", "🎨", "Colours look orange, blue or green",
            listOf(
                "Orange indoors" to "White balance is on Daylight or a fixed Kelvin number. Set it back to AUTO (or Tungsten 💡).",
                "Blue in the shade or snow" to "Use Cloudy ☁ or Shade white balance.",
                "Strange colours everywhere" to "Check {STYLE} (picture style) isn't set to Monochrome or a creative effect.",
            ),
        ),
        Problem(
            "noise", "🔉", "My photos are grainy",
            listOf(
                "Lots of grain in dark photos" to "ISO was very high. Use more light (move near a window), a lower f-number, or a tripod with a lower ISO.",
                "Grain after brightening on the computer" to "The photo was too dark when taken. Get the exposure right in the camera (+ compensation).",
            ),
        ),
        Problem(
            "noshot", "🚫", "The camera won't take a photo",
            listOf(
                "It beeps or blinks but won't shoot" to "It can't focus: you're too close, or the subject has no contrast (a white wall). Step back or focus on an edge.",
                "Card message" to "The card is full, missing, or its little lock switch is on. Replace it or slide the lock up.",
                "Nothing happens" to "Check the battery, and that the self-timer isn't running.",
            ),
        ),
        Problem(
            "background", "🌳", "My background isn't blurry",
            listOf(
                "Everything is sharp" to "Use {AV} with the smallest f-number your lens allows.",
                "Still not blurry enough" to "Zoom in, get closer to your subject, and move the subject away from the background. A 50mm f/1.8 lens helps a lot.",
            ),
        ),
        Problem(
            "night", "✨", "Night and star photos don't work",
            listOf(
                "The photo is black" to "Use M mode: lowest f-number, 15–20 seconds, ISO 3200. Use the 'Stars & Milky Way' guide on the Home tab.",
                "Stars are blurry dots" to "Autofocus can't see stars. Switch to MF and focus on a bright star using the magnified screen.",
                "Stars are little lines" to "Shutter too long for your zoom. Zoom out or use a shorter time.",
            ),
        ),
        Problem(
            "tilted", "📐", "Horizons are crooked, buildings lean",
            listOf(
                "Crooked horizon" to "Turn on the grid or electronic level in the display settings.",
                "Buildings lean backwards" to "You tilted the camera up. Keep the camera level and step back, or straighten later in an editor.",
            ),
        ),
        Problem(
            "battery", "🔋", "The battery dies quickly",
            listOf(
                "In the cold" to "Keep the spare battery in an inside pocket and swap them.",
                "Always" to "Turn off Wi-Fi/Bluetooth when not needed, lower the screen brightness, and switch the camera off between shots.",
            ),
        ),
        Problem(
            "reset", "↩️", "I changed something and everything is weird",
            listOf(
                "Start over" to "Turn the mode dial to AUTO: you can always take good photos there. Then use a guide from the Home tab.",
                "Full reset" to "Menu → look for 'Reset' or 'Clear all camera settings'. This restores factory settings (your photos are not deleted).",
            ),
        ),
    )

    // ---------------------------------------------------------------- glossary

    val glossary: List<Pair<String, String>> = listOf(
        "Aperture" to "The opening in the lens that lets light in. Written as an f-number (f/2.8, f/8…). Small number = big opening = blurry background.",
        "APS-C" to "A sensor size used in many consumer cameras, smaller than full frame. Makes lenses act more 'zoomed in' (crop factor 1.5–1.6).",
        "Auto ISO" to "The camera chooses the ISO for you. Very useful for beginners.",
        "Blue hour" to "The 20–30 minutes after sunset (or before sunrise) when the sky is deep blue.",
        "Bokeh" to "The quality of the blur in the background, especially of small lights.",
        "Bracketing (AEB)" to "Taking several photos at different brightness to combine or pick the best.",
        "Bulb" to "A shutter mode that stays open as long as you hold the button. For fireworks and long night exposures.",
        "Burst / continuous" to "Taking many photos per second while you hold the button.",
        "Crop factor" to "How much smaller a sensor is than full frame. 50 mm on APS-C (1.5×) looks like 75 mm.",
        "Depth of field" to "How much of the photo, from near to far, is sharp. Small f-number = shallow; big f-number = deep.",
        "Exposure" to "How much light reaches the sensor, set by aperture, shutter speed and ISO.",
        "Exposure compensation" to "The +/– setting that makes photos brighter or darker in automatic modes.",
        "Focal length" to "The zoom number in mm. Small (16 mm) = wide view, large (200 mm) = zoomed in.",
        "Full frame" to "A sensor the size of old 35 mm film. The reference for 'equivalent' focal lengths.",
        "Golden hour" to "The hour after sunrise and before sunset with warm, soft light.",
        "Half-press" to "Pressing the shutter button halfway to focus before taking the photo.",
        "Histogram" to "A graph of the brightness in a photo, from black (left) to white (right).",
        "Image stabilization" to "A system in the lens or camera that reduces blur from shaky hands.",
        "ISO" to "The sensitivity of the sensor. Higher = brighter but grainier.",
        "JPEG" to "A finished, compressed photo file, ready to share.",
        "Kelvin (K)" to "A number for the colour of light: 3000 K is warm lamp light, 5500 K daylight, 7000 K shade.",
        "Long exposure" to "A shutter speed of a second or more, to blur water or create light trails.",
        "Manual focus (MF)" to "You turn the focus ring yourself. Needed for stars.",
        "Metering" to "How the camera measures the light in a scene.",
        "ND filter" to "Dark 'sunglasses' for the lens that allow long exposures in daylight.",
        "Noise" to "Grain and coloured speckles, mostly at high ISO.",
        "Polarizing filter" to "A filter that deepens blue skies and removes reflections from water and leaves.",
        "Prime lens" to "A lens that doesn't zoom. Usually sharper and brighter (low f-number).",
        "RAW" to "A file with all the sensor's data, for editing later. Must be converted before sharing.",
        "Rule of thirds" to "Placing important things on the lines of a 3×3 grid rather than in the middle.",
        "Self-timer" to "A delay between pressing the button and the photo; 2 seconds avoids shake on a tripod.",
        "Shutter speed" to "How long the sensor is exposed to light: 1/1000 s freezes, 1 s blurs.",
        "Stop" to "A doubling or halving of light.",
        "Telephoto" to "A lens or zoom setting that brings far things closer (about 70 mm and more).",
        "White balance" to "The setting that makes white look white under different colours of light.",
        "Wide angle" to "A lens or zoom setting that shows a wide view (about 24 mm and less).",
    )
}
