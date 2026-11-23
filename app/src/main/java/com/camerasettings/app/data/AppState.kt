package com.camerasettings.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.camerasettings.app.logic.Support

enum class Level(val label: String, val blurb: String) {
    NEW("I'm brand new", "Explain everything step by step"),
    SOME("I know the basics", "Short explanations"),
    PRO("I'm comfortable", "Just give me the settings"),
}

/** Everything the user picked, saved on the phone so it survives restarts. */
class AppState(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var cameraId by mutableStateOf(prefs.getString("camera", DEFAULT_CAMERA)); private set
    var brand by mutableStateOf(prefs.getString("brand", null)?.let { runCatching { Brand.valueOf(it) }.getOrNull() } ?: Gear.camera(prefs.getString("camera", DEFAULT_CAMERA))?.brand)
        private set
    var lensIds by mutableStateOf(prefs.getStringSet("lenses", DEFAULT_LENSES)!!.toSet()); private set
    var customLens by mutableStateOf(loadCustomLens()); private set
    var support by mutableStateOf(runCatching { Support.valueOf(prefs.getString("support", "HANDHELD")!!) }.getOrDefault(Support.HANDHELD)); private set
    var level by mutableStateOf(runCatching { Level.valueOf(prefs.getString("level", "NEW")!!) }.getOrDefault(Level.NEW)); private set
    var simple by mutableStateOf(prefs.getBoolean("simple", true)); private set
    var textScale by mutableStateOf(prefs.getFloat("text_scale", 1f)); private set
    var onboarded by mutableStateOf(prefs.getBoolean("onboarded", false) || prefs.getString("camera", null) != null); private set
    var lessonsDone by mutableStateOf(prefs.getStringSet("lessons", emptySet())!!.toSet()); private set
    var place by mutableStateOf(Place.decode(prefs.getString("place_v2", null))); private set
    var recentPlaces by mutableStateOf(prefs.getString("recent_places", "")!!.split("\n").mapNotNull { Place.decode(it) }); private set
    var checked by mutableStateOf(prefs.getStringSet("checked", emptySet())!!.toSet()); private set
    var shotsPerDay by mutableStateOf(prefs.getInt("shots_per_day", 150)); private set
    var tripDays by mutableStateOf(prefs.getInt("trip_days", 14)); private set

    val camera: CameraBody? get() = Gear.camera(cameraId)

    val lenses: List<Lens>
        get() {
            val body = camera
            if (body?.fixedLens != null) return Gear.lenses.filter { it.id == body.fixedLens }
            return Gear.lenses.filter { it.id in lensIds } + listOfNotNull(customLens?.takeIf { it.id in lensIds })
        }

    val hasGear get() = camera != null && lenses.isNotEmpty()

    fun selectBrand(b: Brand) {
        brand = b
        prefs.edit().putString("brand", b.name).apply()
    }

    fun selectCamera(id: String) {
        cameraId = id
        Gear.camera(id)?.let { brand = it.brand; prefs.edit().putString("brand", it.brand.name).apply() }
        // Drop lenses that don't fit the new camera.
        val fits = Gear.lensesFor(Gear.camera(id)).map { it.id }.toSet() + CUSTOM_LENS_ID
        lensIds = lensIds.filter { it in fits }.toSet()
        prefs.edit().putString("camera", id).putStringSet("lenses", lensIds).apply()
    }

    fun toggleLens(id: String) {
        lensIds = if (id in lensIds) lensIds - id else lensIds + id
        prefs.edit().putStringSet("lenses", lensIds).apply()
    }

    fun saveCustomLens(lens: Lens) {
        customLens = lens
        prefs.edit()
            .putString("c_name", lens.name).putInt("c_min", lens.minFocal).putInt("c_max", lens.maxFocal)
            .putFloat("c_apw", lens.apWide.toFloat()).putFloat("c_apt", lens.apTele.toFloat()).putBoolean("c_is", lens.hasIs)
            .apply()
        if (lens.id !in lensIds) toggleLens(lens.id)
    }

    fun selectSupport(s: Support) { support = s; prefs.edit().putString("support", s.name).apply() }

    fun selectLevel(l: Level) {
        level = l
        changeSimple(l != Level.PRO)
        prefs.edit().putString("level", l.name).apply()
    }

    fun changeSimple(on: Boolean) { simple = on; prefs.edit().putBoolean("simple", on).apply() }

    fun changeTextScale(s: Float) { textScale = s; prefs.edit().putFloat("text_scale", s).apply() }

    fun finishOnboarding() { onboarded = true; prefs.edit().putBoolean("onboarded", true).apply() }

    fun markLesson(id: String, done: Boolean = true) {
        lessonsDone = if (done) lessonsDone + id else lessonsDone - id
        prefs.edit().putStringSet("lessons", lessonsDone).apply()
    }

    fun selectPlace(p: Place) {
        place = p
        recentPlaces = (listOf(p) + recentPlaces.filter { it.name != p.name || it.country != p.country }).take(6)
        prefs.edit().putString("place_v2", p.encode()).putString("recent_places", recentPlaces.joinToString("\n") { it.encode() }).apply()
    }

    fun toggleChecked(id: String) {
        checked = if (id in checked) checked - id else checked + id
        prefs.edit().putStringSet("checked", checked).apply()
    }

    fun setStoragePlan(perDay: Int, days: Int) {
        shotsPerDay = perDay; tripDays = days
        prefs.edit().putInt("shots_per_day", perDay).putInt("trip_days", days).apply()
    }

    private fun loadCustomLens(): Lens? {
        val name = prefs.getString("c_name", null) ?: return null
        return Lens(
            CUSTOM_LENS_ID, name, prefs.getInt("c_min", 18), prefs.getInt("c_max", 55),
            prefs.getFloat("c_apw", 3.5f).toDouble(), prefs.getFloat("c_apt", 5.6f).toDouble(),
            prefs.getBoolean("c_is", true), "any",
        )
    }

    companion object {
        const val CUSTOM_LENS_ID = "custom"

        /** The owner's camera (Canon EOS 750D, from a photo's EXIF data). Lens to be confirmed. */
        const val DEFAULT_CAMERA = "750d"
        val DEFAULT_LENSES = setOf("efs18-135")
    }
}
