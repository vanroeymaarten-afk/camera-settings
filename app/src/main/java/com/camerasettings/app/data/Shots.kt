package com.camerasettings.app.data

import com.camerasettings.app.logic.Astro
import com.camerasettings.app.logic.Setting
import java.time.ZonedDateTime

/** When an example shot works best, resolved to real times for its location. */
enum class BestTime(val label: String) {
    PRE_DAWN("Before & at sunrise"),
    MORNING("Morning"),
    LATE_AFTERNOON("Late afternoon"),
    SUNSET_GOLDEN("Golden hour & sunset"),
    AFTER_SUNSET("Sunset & afterglow"),
    BLUE_EVENING("Blue hour"),
    DARK_NIGHT("Dark, moonless night"),
    SOFT_LIGHT("Overcast or soft light");

    /** The window on the given day, or null when it isn't time-bound or doesn't happen. */
    fun window(plan: Astro.DayPlan): Pair<ZonedDateTime, ZonedDateTime>? {
        val s = plan.sun
        return when (this) {
            PRE_DAWN -> pair(s.blueMorningStart?.minusMinutes(45), s.sunrise?.plusMinutes(30))
            MORNING -> pair(s.sunrise?.plusHours(1), s.sunrise?.plusHours(4))
            LATE_AFTERNOON -> pair(s.goldenEveningStart?.minusHours(2), s.goldenEveningStart)
            SUNSET_GOLDEN -> pair(s.goldenEveningStart, s.sunset?.plusMinutes(10))
            AFTER_SUNSET -> pair(s.sunset?.minusMinutes(20), s.blueEveningEnd)
            BLUE_EVENING -> pair(s.blueEveningStart?.minusMinutes(15), s.blueEveningEnd?.plusMinutes(10))
            DARK_NIGHT -> plan.night.moonFree.firstOrNull()?.let { it.start to it.end }
            SOFT_LIGHT -> null
        }
    }

    private fun pair(a: ZonedDateTime?, b: ZonedDateTime?) = if (a != null && b != null) a to b else null
}

data class Shot(
    val id: String,
    val region: Region,
    val title: String,
    val placeId: String,
    val sceneId: String,
    val bestTime: BestTime,
    val story: String,
    val howTo: List<String>,
    /** Settings that differ from (or add to) the general scene advice. */
    val overrides: List<Setting>,
    val inCamera: String,
    val presetId: String,
    val practical: String,
) {
    val place: Place get() = Places.byId(placeId)!!
}

object Shots {
    val all: List<Shot> = listOf(
        // ---------------------------------------------------------------- Atacama
        Shot(
            id = "valle_luna", region = Region.ATACAMA,
            title = "Valle de la Luna at sunset",
            placeId = "valleluna", sceneId = "golden", bestTime = BestTime.SUNSET_GOLDEN,
            story = "Wind-carved salt ridges turn orange and red while Licancabur volcano glows pink on the horizon.",
            howTo = listOf(
                "Go to Mirador de Kari (Piedra del Coyote) and arrive an hour before sunset to pick your spot.",
                "Use the ridges as leading lines towards the volcanoes; keep the horizon in the top third.",
                "Shoot a second series 10–20 minutes after sunset, when the sky turns pink and purple.",
                "Include people on a ridge for scale: they look tiny in a wide frame, which is the point.",
            ),
            overrides = listOf(
                Setting("Exposure compensation", "−⅔", "Keeps the glowing sky and sunlit rock from washing out."),
                Setting("White balance", "Cloudy ☁ (about 6000 K)", "Adds warmth on top of the real glow."),
            ),
            inCamera = "Picture Style: Landscape. White balance Cloudy, plus WB shift A2 (amber) for an extra-warm JPEG.",
            presetId = "atacama_gold",
            practical = "The Kari viewpoint gets busy: arrive early. It cools fast after sunset, so bring a jacket.",
        ),
        Shot(
            id = "milky_way", region = Region.ATACAMA,
            title = "Stars over the Atacama",
            placeId = "sanpedro", sceneId = "astro", bestTime = BestTime.DARK_NIGHT,
            story = "Among the darkest, clearest skies on the planet. In December look for the Carina Nebula, the Southern Cross and the two Magellanic Clouds.",
            howTo = listOf(
                "Drive 15–20 minutes out of San Pedro to escape the town lights, or join a stargazing tour.",
                "Arrive before dark: set up the tripod and compose while you can still see.",
                "Focus manually on a bright star with 10× magnified live view, then don't touch the focus ring again.",
                "Add a foreground: a volcano silhouette, a rock, or a person lit for a second by a headlamp.",
                "Use the star calendar in Sun & Stars to pick a night with no moon.",
            ),
            overrides = emptyList(),
            inCamera = "White balance 4000 K, Picture Style Neutral (an honest preview of the stars on the screen).",
            presetId = "desert_night",
            practical = "Nights at 2,400 m drop to about 5 °C even in summer. Keep spare batteries in an inside pocket.",
        ),
        Shot(
            id = "el_tatio", region = Region.ATACAMA,
            title = "El Tatio geysers at sunrise",
            placeId = "eltatio", sceneId = "geysers", bestTime = BestTime.PRE_DAWN,
            story = "More than 80 geysers steam in freezing air at 4,300 m. When the first sun hits them from behind, the columns glow gold.",
            howTo = listOf(
                "Shoot towards the rising sun so the steam is backlit; hide the sun itself behind a column.",
                "Use people as silhouettes in front of the steam for scale.",
                "Zoom in (70 mm+) to stack several steam columns on top of each other.",
                "The show fades about an hour after sunrise as the air warms up.",
            ),
            overrides = listOf(
                Setting("Exposure compensation", "−⅔ to −1 (into the sun)", "Backlit steam is very bright; this keeps its texture."),
            ),
            inCamera = "White balance Daylight, Picture Style Standard.",
            presetId = "atacama_gold",
            practical = "Tours leave San Pedro at about 4:30–5:00. −5 to −10 °C at dawn: gloves, hat, layers. Stay on the paths: the ground is thin and the water boiling.",
        ),
        Shot(
            id = "flamingos", region = Region.ATACAMA,
            title = "Flamingos at Laguna Chaxa",
            placeId = "chaxa", sceneId = "wildlife", bestTime = BestTime.SUNSET_GOLDEN,
            story = "Andean and Chilean flamingos feed in a mirror-flat lagoon inside the salt flat, with a row of volcanoes behind.",
            howTo = listOf(
                "Late afternoon brings warm light and, often, less wind for reflections.",
                "Crouch low so the birds line up with the volcanoes and their own reflections.",
                "Use your longest zoom and wait for a bird to lift its head or stretch its wings.",
                "Also take a wide shot: tiny pink birds in a huge landscape tell the story.",
            ),
            overrides = listOf(
                Setting("Exposure compensation", "+⅓", "Bright salt and water fool the meter into underexposing."),
            ),
            inCamera = "Picture Style Landscape, White balance Daylight.",
            presetId = "atacama_gold",
            practical = "In the Los Flamencos reserve (entry fee), about 1 hour from San Pedro.",
        ),
        Shot(
            id = "altiplano", region = Region.ATACAMA,
            title = "Altiplano lagoons",
            placeId = "miscanti", sceneId = "landscape", bestTime = BestTime.MORNING,
            story = "Lagunas Miscanti and Miñiques: impossibly blue water, white salt edges and golden paja brava grass at 4,100 m.",
            howTo = listOf(
                "Mornings are calmest: less wind means mirror reflections.",
                "A polarising filter deepens the blue of water and sky; rotate it until it looks right.",
                "Build three layers: golden grass in front, lagoon in the middle, volcano on top.",
                "Shoot some vertical frames too; they look great on phone screens.",
            ),
            overrides = listOf(
                Setting("Exposure compensation", "−⅓", "The high-altitude sky is very bright; protect it."),
            ),
            inCamera = "Picture Style Landscape gives punchy blues and golds straight out of camera.",
            presetId = "andes_clear",
            practical = "Usually combined with Piedras Rojas on a day tour. Drink water and walk slowly: 4,000 m is serious altitude.",
        ),
        // ---------------------------------------------------------------- Santiago & Valparaíso
        Shot(
            id = "santiago", region = Region.SANTIAGO,
            title = "Santiago skyline & the Andes",
            placeId = "sancristobal", sceneId = "blue", bestTime = BestTime.BLUE_EVENING,
            story = "From Cerro San Cristóbal the city lights switch on while the snowy Andes still glow pink behind the towers.",
            howTo = listOf(
                "Take the funicular or cable car up before sunset and claim a spot on the terrace.",
                "Keep shooting until 30 minutes after sunset: blue hour balances the sky with the city lights.",
                "Use a tripod (or rest on the wall) with the 2-second timer.",
                "Zoom to 70–100 mm to make the Andes look huge behind the tall Costanera tower.",
            ),
            overrides = listOf(
                Setting("White balance", "4300 K", "Blue sky, warm city lights."),
                Setting("Exposure compensation", "−⅓", "Night should look like night."),
            ),
            inCamera = "White balance 4000–4500 K for a blue sky with warm lights.",
            presetId = "city_blue",
            practical = "Check the funicular's closing time. Smog can hide the Andes; the day after rain is the clearest.",
        ),
        Shot(
            id = "valparaiso", region = Region.SANTIAGO,
            title = "Valparaíso's colourful hills",
            placeId = "valparaiso", sceneId = "street", bestTime = BestTime.LATE_AFTERNOON,
            story = "Hundreds of painted houses tumble down the hills to the harbour, linked by century-old funiculars (ascensores).",
            howTo = listOf(
                "Viewpoints: Paseo Gervasoni and Paseo Yugoslavo on Cerro Alegre, or Cerro Concepción.",
                "Zoom in (70–150 mm) to compress the houses into a wall of colour.",
                "Look for a focal point: a funicular car, a red roof, or a person on a staircase.",
                "Midday sun is harsh: late afternoon or a cloudy day gives the best colours.",
            ),
            overrides = listOf(
                Setting("Aperture", "f/8", "More depth when you're zoomed in on rows of houses."),
            ),
            inCamera = "Picture Style Standard with Saturation +1.",
            presetId = "puerto_color",
            practical = "Keep your strap on and your bag closed around the port and busy streets.",
        ),
        Shot(
            id = "condor", region = Region.SANTIAGO,
            title = "Condors in the Cajón del Maipo",
            placeId = "maipo", sceneId = "birds", bestTime = BestTime.MORNING,
            story = "The Andean condor, with a 3-metre wingspan, rides the warm updrafts along the canyon walls.",
            howTo = listOf(
                "Mid-morning to noon, when the rocks warm up and thermals form.",
                "Pre-focus on the sky near a ridge, then track the bird and shoot bursts.",
                "Include canyon walls for context, not just a bird in a blue sky.",
            ),
            overrides = emptyList(),
            inCamera = "Picture Style Standard.",
            presetId = "andes_clear",
            practical = "Day trip from Santiago (about 1.5 h). Sightings are never guaranteed; local guides know the spots.",
        ),
        // ---------------------------------------------------------------- Puerto Montt & the Lakes
        Shot(
            id = "osorno", region = Region.LAKES,
            title = "Osorno volcano over Lake Llanquihue",
            placeId = "frutillar", sceneId = "golden", bestTime = BestTime.AFTER_SUNSET,
            story = "A near-perfect snow cone mirrored in the lake. Just after sunset the snow turns pink (alpenglow).",
            howTo = listOf(
                "Shoot from Frutillar's pier or the Puerto Varas waterfront, looking east across the lake.",
                "Calm evenings give a mirror: put the horizon in the middle for a symmetrical shot.",
                "Keep shooting 10–20 minutes after sunset while the snow glows.",
                "Frame the volcano with reeds, the pier or a moored boat.",
            ),
            overrides = listOf(
                Setting("Exposure compensation", "−⅓", ""),
                Setting("White balance", "Cloudy ☁", "Makes the alpenglow pinker."),
            ),
            inCamera = "White balance Cloudy, Picture Style Landscape.",
            presetId = "lakes_volcanoes",
            practical = "Clouds often hide Osorno: look in the morning and go on a clear evening. Frutillar is 30 min from Puerto Varas.",
        ),
        Shot(
            id = "petrohue", region = Region.LAKES,
            title = "Saltos del Petrohué",
            placeId = "petrohue", sceneId = "water", bestTime = BestTime.SOFT_LIGHT,
            story = "Turquoise glacial water rushes over black lava rock, with Osorno volcano rising behind.",
            howTo = listOf(
                "Follow the paths to the platforms; the classic view has Osorno above the falls.",
                "Tripod, small aperture and an ND filter for silky water (0.5–2 s).",
                "A polarising filter removes glare from wet rock and makes the turquoise glow.",
                "Also take a fast version (1/1000 s) to freeze the spray.",
            ),
            overrides = emptyList(),
            inCamera = "Picture Style Landscape, White balance Daylight.",
            presetId = "lakes_volcanoes",
            practical = "In Vicente Pérez Rosales National Park (entry fee), 1 h from Puerto Varas. Combine with Lago Todos los Santos.",
        ),
        Shot(
            id = "palafitos", region = Region.LAKES,
            title = "Palafitos of Castro, Chiloé",
            placeId = "castro", sceneId = "street", bestTime = BestTime.MORNING,
            story = "Colourful wooden houses on stilts over the water, mirrored at high tide.",
            howTo = listOf(
                "Go at high tide (check a tide table) for water under the houses, in calm morning air for reflections.",
                "Shoot from across the water with a short telephoto to line the houses up.",
                "Blue hour with lights on inside the houses is beautiful too.",
            ),
            overrides = listOf(Setting("Aperture", "f/8", "All houses sharp.")),
            inCamera = "Picture Style Standard; White balance Cloudy on grey days.",
            presetId = "puerto_color",
            practical = "Chiloé: about 1.5 h drive plus a 30-minute ferry from Puerto Montt.",
        ),
        Shot(
            id = "forest", region = Region.LAKES,
            title = "Rainforest of Alerce Andino",
            placeId = "alerce", sceneId = "forest", bestTime = BestTime.SOFT_LIGHT,
            story = "Ancient alerce trees, ferns and moss in a temperate rainforest. Mist and light rain make it magical.",
            howTo = listOf(
                "Overcast or drizzly days are best: even light and deep greens.",
                "Build layers: a fern in front, mossy trunks in the middle, mist behind.",
                "Use the polariser to remove glare from wet leaves; colours get much richer.",
                "Shoot vertical frames to show how tall the trees are.",
            ),
            overrides = emptyList(),
            inCamera = "White balance Cloudy, Picture Style Landscape.",
            presetId = "moody_south",
            practical = "Rain gear for you and the camera. Trailheads are about 45 min from Puerto Montt.",
        ),
        Shot(
            id = "penguins", region = Region.LAKES,
            title = "Penguins of Puñihuil",
            placeId = "punihuil", sceneId = "wildlife", bestTime = BestTime.MORNING,
            story = "Humboldt and Magellanic penguins nest on rocky islets off Chiloé's Pacific coast; small boats take you close.",
            howTo = listOf(
                "Boats run in the morning when the sea is calmer (September to March).",
                "The boat rocks: use a fast shutter speed and burst mode.",
                "Focus on the eyes and wait for two penguins to interact.",
                "Around 200 mm or more (full-frame equivalent) is ideal.",
            ),
            overrides = listOf(Setting("Shutter speed", "1/1000 s or faster", "You're on a moving boat.")),
            inCamera = "Picture Style Standard.",
            presetId = "moody_south",
            practical = "Protect the camera from sea spray. Puñihuil is 30 min from Ancud.",
        ),
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id }
}
