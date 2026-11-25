package com.camerasettings.app.data

/**
 * A short, plain-language "how do I…" guide for a specific camera, written for this app
 * (not copied from the manufacturer's manual). The official manual can be added as a PDF.
 */
data class ManualTopic(val id: String, val group: String, val title: String, val steps: List<String>, val tip: String = "")

data class CameraManual(
    val cameraId: String,
    val buttons: List<Pair<String, String>>,
    val topics: List<ManualTopic>,
    val officialUrl: String,
)

object Manuals {
    /** Where each brand publishes its manuals (opened in the browser). */
    fun officialUrl(brand: Brand?): String = when (brand) {
        Brand.CANON -> "https://www.canon.com/icpd/"
        Brand.NIKON -> "https://downloadcenter.nikonimglib.com/"
        Brand.SONY -> "https://helpguide.sony.net/"
        Brand.FUJIFILM -> "https://fujifilm-dsc.com/en/manual/"
        Brand.PANASONIC -> "https://help.na.panasonic.com/"
        Brand.OM -> "https://explore.omsystem.com/"
        else -> "https://www.google.com/search?q=camera+user+manual+pdf"
    }

    fun forCamera(id: String?): CameraManual? = all.firstOrNull { it.cameraId == id }

    private val eos750d = CameraManual(
        cameraId = "750d",
        officialUrl = "https://www.canon.com/icpd/",
        buttons = listOf(
            "Mode dial (top, left)" to "Choose the shooting mode: A+ (full auto), P, Tv, Av, M and the scene modes.",
            "Power switch (top, left, around the mode dial)" to "OFF · ON · the movie camera symbol for video.",
            "Shutter button (top, front of the grip)" to "Half-press to focus, press fully to take the photo.",
            "Main dial (top, just behind the shutter button)" to "Changes the main setting: aperture in Av, shutter speed in Tv and M.",
            "ISO button (top, next to the main dial)" to "Press it and turn the main dial to change ISO.",
            "Av± button (back, right, near the top)" to "Hold it and turn the main dial: exposure compensation (+/–) in P/Av/Tv, aperture in M.",
            "Q button (back, right of the screen)" to "Quick menu on the screen: change almost any setting fast.",
            "Cross keys (back)" to "▲ white balance · ▶ AF mode · ◀ drive mode / self-timer · ▼ Picture Style. SET in the middle confirms.",
            "AF point button and magnify button (back, top right)" to "Choose the focus point. In playback / Live View, these zoom in and out.",
            "Live View button (back, right of the viewfinder)" to "Shows the picture on the screen instead of the viewfinder. Starts and stops video in movie mode.",
            "MENU / INFO (back, top left)" to "MENU opens all settings. INFO changes what the screen shows (e.g. the level).",
            "▶ Playback and 🗑 Delete (back, bottom right)" to "Look at your photos and delete them.",
            "Lens switches" to "AF/MF (autofocus or manual focus) and STABILIZER (Canon) / VC (Tamron) ON/OFF.",
        ),
        topics = listOf(
            ManualTopic("auto", "Basics", "Take a photo in full auto", listOf(
                "Turn the power switch to ON.",
                "Turn the mode dial to the green A+ symbol.",
                "Look through the viewfinder, half-press the shutter button until you hear a beep.",
                "Press the button fully.",
            ), "Auto is great for learning to frame photos. Use Av for more control."),
            ManualTopic("av", "Basics", "Use aperture priority (Av)", listOf(
                "Turn the mode dial to Av.",
                "Turn the main dial: the f-number on the screen changes (F4.0, F8.0…).",
                "Small number = blurry background, big number = everything sharp.",
                "The camera picks the shutter speed itself.",
            )),
            ManualTopic("tv", "Basics", "Use shutter priority (Tv)", listOf(
                "Turn the mode dial to Tv.",
                "Turn the main dial to choose the shutter speed, e.g. 1000 (= 1/1000 s) to freeze movement.",
                "The camera picks the aperture.",
            ), "If the aperture value blinks, there isn't enough light: raise ISO."),
            ManualTopic("m", "Basics", "Use manual mode (M)", listOf(
                "Turn the mode dial to M.",
                "Turn the main dial for the shutter speed.",
                "Hold the Av± button and turn the main dial for the aperture.",
                "Press ISO and turn the main dial for ISO.",
                "The scale at the bottom of the viewfinder shows if the photo will be too dark (−) or too bright (+).",
            )),
            ManualTopic("iso", "Exposure", "Change ISO (or set it to AUTO)", listOf(
                "Press the ISO button on top.",
                "Turn the main dial, or pick AUTO.",
                "Half-press the shutter button to go back.",
            ), "To limit Auto ISO: MENU → red shooting menu → ISO speed settings → Max for Auto (e.g. 3200)."),
            ManualTopic("expcomp", "Exposure", "Make photos brighter or darker (+/–)", listOf(
                "In P, Av or Tv: hold the Av± button.",
                "Turn the main dial left (darker) or right (brighter). Watch the scale in the viewfinder.",
                "Set it back to 0 afterwards.",
            ), "+1 for snow and beaches, −⅔ for sunsets."),
            ManualTopic("aeb", "Exposure", "Bracketing (3 photos at different brightness)", listOf(
                "MENU → red shooting menu → Expo.comp./AEB.",
                "Turn the main dial to spread the marks (e.g. −2, 0, +2). Press SET.",
                "Set drive mode to continuous (◀ key) and hold the shutter button: you get 3 photos.",
            ), "Turn it off again in the same menu when you're done."),
            ManualTopic("wb", "Colour", "White balance", listOf(
                "Press ▲ (WB) on the cross keys.",
                "Choose AWB (auto), ☀ daylight, shade, ☁ cloudy, 💡 tungsten…",
                "Press SET.",
            )),
            ManualTopic("style", "Colour", "Picture Style (look of the JPEG)", listOf(
                "Press ▼ on the cross keys.",
                "Choose Standard, Portrait, Landscape, Neutral, Faithful, Monochrome…",
                "Press SET.",
            ), "Only changes the JPEG; RAW files can be adjusted later."),
            ManualTopic("afmode", "Focus", "Focus for still or moving subjects", listOf(
                "Press ▶ (AF) on the cross keys.",
                "ONE SHOT for still subjects, AI SERVO for moving subjects (AI FOCUS lets the camera decide).",
                "Press SET.",
            )),
            ManualTopic("afpoint", "Focus", "Choose the focus point", listOf(
                "Press the AF point selection button (back, top right).",
                "Use the cross keys or main dial to move the point onto your subject.",
                "Press SET to jump back to the centre point. Half-press to return to shooting.",
            ), "Pressing the button repeatedly also changes the AF area (single point, zone, automatic)."),
            ManualTopic("mf", "Focus", "Manual focus (for stars)", listOf(
                "Slide the switch on the lens from AF to MF.",
                "Press the Live View button so the picture shows on the screen.",
                "Press the magnify button twice (10×) and move the frame onto a bright star or light.",
                "Turn the focus ring until it's the smallest, sharpest dot.",
            ), "Don't touch the ring afterwards. Slide back to AF later!"),
            ManualTopic("drive", "Drive", "Self-timer and burst", listOf(
                "Press ◀ on the cross keys.",
                "Single, continuous (burst), 10-second or 2-second self-timer, or remote.",
                "Press SET.",
            ), "The 2-second timer avoids shake on a tripod."),
            ManualTopic("quality", "Files", "Shoot RAW + JPEG", listOf(
                "MENU → red shooting menu 1 → Image quality.",
                "Turn the main dial to RAW, and use the cross keys to pick L (large JPEG).",
                "Press SET.",
            ), "Your 750D saves RAW as .CR2 files."),
            ManualTopic("format", "Files", "Format (wipe) the memory card", listOf(
                "First copy your photos to a computer!",
                "MENU → yellow settings menu (wrench) → Format card → OK.",
            )),
            ManualTopic("datetime", "Files", "Set date, time and time zone", listOf(
                "MENU → yellow settings menu → Date/Time/Zone.",
                "Set the time zone of where you are, then date and time. Press OK.",
            )),
            ManualTopic("review", "Playback", "Check photos and zoom in", listOf(
                "Press ▶ (playback).",
                "Turn the main dial for the previous or next photo.",
                "Press the magnify button to zoom in, the button next to it to zoom out.",
                "Press INFO to see the histogram and settings.",
            ), "Turn on Highlight alert (blue playback menu) to see over-bright parts blink."),
            ManualTopic("level", "Display", "Grid and level", listOf(
                "Level: press INFO while in shooting mode until the level appears.",
                "Grid in Live View: MENU → red shooting menu → Grid display.",
                "Grid in the viewfinder: MENU → yellow settings menu → Viewfinder display → Grid.",
            )),
            ManualTopic("liveview", "Display", "Use the screen instead of the viewfinder", listOf(
                "Press the Live View button (right of the viewfinder).",
                "Tap the screen to focus and shoot (Touch Shutter can be turned on and off on screen).",
                "Press the button again to go back to the viewfinder.",
            )),
            ManualTopic("flash", "Other", "Flash on or off", listOf(
                "Close the pop-up flash with your finger to keep it off.",
                "To pop it up: press the flash button (front left of the lens mount).",
                "In A+ mode the flash may pop up by itself; use the 'flash off' mode on the dial instead.",
            )),
            ManualTopic("video", "Other", "Record a video", listOf(
                "Turn the power switch to the movie camera symbol.",
                "Press the Live View button to start, again to stop.",
            )),
            ManualTopic("wifi", "Other", "Send photos to your phone (Wi-Fi)", listOf(
                "Install Canon Camera Connect on your phone.",
                "MENU → yellow settings menu → Wireless communication settings → Wi-Fi/NFC → Enable.",
                "Choose 'Connect to smartphone' and follow the screen. On phones with NFC, touch the phone to the N mark on the camera.",
            )),
            ManualTopic("reset", "Other", "Reset all settings", listOf(
                "MENU → yellow settings menu → Clear settings → Clear all camera settings → OK.",
            ), "Your photos are not deleted."),
            ManualTopic("battery", "Other", "Battery and card", listOf(
                "Battery: LP-E17, under the bottom of the camera. Charge it in the LC-E17 charger (orange = charging, green = full).",
                "Card: SD / SDHC / SDXC, in the slot on the right side of the grip.",
            )),
        ),
    )

    val all = listOf(eos750d)
}
