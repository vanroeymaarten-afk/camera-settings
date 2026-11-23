package com.camerasettings.app.data

/** The words and buttons each camera brand uses, so instructions match what's on the camera. */
data class Vocab(
    val av: String,
    val tv: String,
    val afSingle: String,
    val afContinuous: String,
    val metering: String,
    val quickMenu: String,
    val pictureStyle: String,
    val styleLandscape: String,
    val styleNeutral: String,
    val styleStandard: String,
    val stabilizer: String,
    val raw: String,
    val expComp: String,
    val apertureDial: String,
    val shutterDial: String,
    val subjectDetect: String,
    val minShutter: String,
    val dialMode: String,
)

enum class Brand(val label: String, val vocab: Vocab) {
    CANON(
        "Canon",
        Vocab(
            av = "Av", tv = "Tv", afSingle = "One Shot", afContinuous = "Servo AF (AI Servo on DSLRs)",
            metering = "Evaluative", quickMenu = "Q button", pictureStyle = "Picture Style",
            styleLandscape = "Landscape", styleNeutral = "Neutral", styleStandard = "Standard",
            stabilizer = "IS", raw = "CR3 (CR2 on older models)",
            expComp = "turn the rear wheel (on cameras with one dial: hold the Av± button and turn the main dial)",
            apertureDial = "main dial (just behind the shutter button)",
            shutterDial = "main dial (just behind the shutter button)",
            subjectDetect = "Subject to detect: Animals", minShutter = "Auto ISO → Min. shutter speed",
            dialMode = "Turn the mode dial on top of the camera to",
        ),
    ),
    NIKON(
        "Nikon",
        Vocab(
            av = "A", tv = "S", afSingle = "AF-S", afContinuous = "AF-C", metering = "Matrix",
            quickMenu = "i button", pictureStyle = "Picture Control",
            styleLandscape = "Landscape", styleNeutral = "Neutral", styleStandard = "Standard",
            stabilizer = "VR", raw = "NEF", expComp = "hold the +/– button and turn the rear dial",
            apertureDial = "front dial", shutterDial = "rear dial",
            subjectDetect = "Subject detection: Animal", minShutter = "Auto ISO → Minimum shutter speed",
            dialMode = "Turn the mode dial to",
        ),
    ),
    SONY(
        "Sony",
        Vocab(
            av = "A", tv = "S", afSingle = "AF-S", afContinuous = "AF-C", metering = "Multi",
            quickMenu = "Fn button", pictureStyle = "Creative Look / Creative Style",
            styleLandscape = "VV (Vivid) or Landscape", styleNeutral = "NT (Neutral)", styleStandard = "ST (Standard)",
            stabilizer = "SteadyShot", raw = "ARW", expComp = "turn the exposure compensation dial (or press the +/– on the control wheel)",
            apertureDial = "front or rear dial", shutterDial = "front or rear dial",
            subjectDetect = "Subject Recognition: Animal or Bird", minShutter = "ISO AUTO Min. SS",
            dialMode = "Turn the mode dial to",
        ),
    ),
    FUJIFILM(
        "Fujifilm",
        Vocab(
            av = "A (aperture priority)", tv = "S (shutter priority)", afSingle = "S (single AF)", afContinuous = "C (continuous AF)",
            metering = "Multi", quickMenu = "Q button", pictureStyle = "Film Simulation",
            styleLandscape = "Velvia", styleNeutral = "Provia / Standard", styleStandard = "Provia / Standard",
            stabilizer = "OIS / IBIS", raw = "RAF", expComp = "turn the exposure compensation dial on top",
            apertureDial = "aperture ring on the lens (or the front dial)", shutterDial = "shutter speed dial on top",
            subjectDetect = "Subject detection: Animal or Bird", minShutter = "Auto ISO → Min. shutter speed",
            dialMode = "Set the dials for",
        ),
    ),
    PANASONIC(
        "Panasonic Lumix",
        Vocab(
            av = "A", tv = "S", afSingle = "AFS", afContinuous = "AFC", metering = "Multiple",
            quickMenu = "Q button", pictureStyle = "Photo Style",
            styleLandscape = "Landscape", styleNeutral = "Natural", styleStandard = "Standard",
            stabilizer = "I.S. (O.I.S. / IBIS)", raw = "RW2", expComp = "press +/– (or use the rear dial) and adjust",
            apertureDial = "front dial", shutterDial = "front dial",
            subjectDetect = "AF Detection: Animal", minShutter = "Min. Shtr Speed (ISO Auto)",
            dialMode = "Turn the mode dial to",
        ),
    ),
    OM(
        "OM System / Olympus",
        Vocab(
            av = "A", tv = "S", afSingle = "S-AF", afContinuous = "C-AF", metering = "ESP (Digital ESP)",
            quickMenu = "OK button (Super Control Panel)", pictureStyle = "Picture Mode",
            styleLandscape = "Vivid", styleNeutral = "Natural", styleStandard = "Natural",
            stabilizer = "Image Stabilizer (IS)", raw = "ORF", expComp = "turn the front or rear dial (or press +/–)",
            apertureDial = "front dial", shutterDial = "front dial",
            subjectDetect = "Subject detection: Birds or Animals", minShutter = "ISO-Auto → Low Limit",
            dialMode = "Turn the mode dial to",
        ),
    ),
    OTHER(
        "Other brand",
        Vocab(
            av = "A / Av", tv = "S / Tv", afSingle = "single AF (AF-S / One Shot)", afContinuous = "continuous AF (AF-C / Servo)",
            metering = "Multi-zone (Matrix / Evaluative)", quickMenu = "quick menu button", pictureStyle = "picture style / colour mode",
            styleLandscape = "Landscape or Vivid", styleNeutral = "Neutral", styleStandard = "Standard",
            stabilizer = "image stabiliser", raw = "RAW", expComp = "use the +/– exposure compensation control",
            apertureDial = "main dial", shutterDial = "main dial",
            subjectDetect = "animal / bird detection", minShutter = "Auto ISO minimum shutter speed",
            dialMode = "Turn the mode dial to",
        ),
    );
}
