package com.camerasettings.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.camerasettings.app.logic.Support
import java.time.ZoneId

/** Everything the user picked, saved on the phone so it survives restarts. */
class AppState(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var cameraId by mutableStateOf(prefs.getString("camera", null))
        private set
    var lensIds by mutableStateOf(prefs.getStringSet("lenses", emptySet())!!.toSet())
        private set
    var customLens by mutableStateOf(loadCustomLens())
        private set
    var support by mutableStateOf(
        runCatching { Support.valueOf(prefs.getString("support", "HANDHELD")!!) }.getOrDefault(Support.HANDHELD),
    )
        private set
    var placeId by mutableStateOf(prefs.getString("place", "sanpedro")!!)
        private set
    var gpsLat by mutableStateOf(prefs.getFloat("gps_lat", Float.NaN).toDouble())
        private set
    var gpsLon by mutableStateOf(prefs.getFloat("gps_lon", Float.NaN).toDouble())
        private set

    var checked by mutableStateOf(prefs.getStringSet("checked", emptySet())!!.toSet())
        private set
    var shotsPerDay by mutableStateOf(prefs.getInt("shots_per_day", 150))
        private set
    var tripDays by mutableStateOf(prefs.getInt("trip_days", 21))
        private set

    val camera: CameraBody? get() = Gear.camera(cameraId)

    fun toggleChecked(id: String) {
        checked = if (id in checked) checked - id else checked + id
        prefs.edit().putStringSet("checked", checked).apply()
    }

    fun setStoragePlan(perDay: Int, days: Int) {
        shotsPerDay = perDay
        tripDays = days
        prefs.edit().putInt("shots_per_day", perDay).putInt("trip_days", days).apply()
    }

    val lenses: List<Lens>
        get() = Gear.lenses.filter { it.id in lensIds } +
            listOfNotNull(customLens?.takeIf { it.id in lensIds })

    val hasGear get() = camera != null && lenses.isNotEmpty()

    /** The place used for sun/moon times. */
    val place: Place
        get() = if (placeId == Places.GPS_ID && !gpsLat.isNaN()) {
            Place(
                Places.GPS_ID, "My location", null, gpsLat, gpsLon, ZoneId.systemDefault().id,
                String.format(java.util.Locale.US, "%.3f, %.3f", gpsLat, gpsLon),
            )
        } else {
            Places.byId(placeId) ?: Places.all.first()
        }

    fun selectCamera(id: String) {
        cameraId = id
        prefs.edit().putString("camera", id).apply()
    }

    fun toggleLens(id: String) {
        lensIds = if (id in lensIds) lensIds - id else lensIds + id
        prefs.edit().putStringSet("lenses", lensIds).apply()
    }

    fun saveCustomLens(lens: Lens) {
        customLens = lens
        prefs.edit()
            .putString("c_name", lens.name)
            .putInt("c_min", lens.minFocal)
            .putInt("c_max", lens.maxFocal)
            .putFloat("c_apw", lens.apWide.toFloat())
            .putFloat("c_apt", lens.apTele.toFloat())
            .putBoolean("c_is", lens.hasIs)
            .apply()
        if (lens.id !in lensIds) toggleLens(lens.id)
    }

    fun selectSupport(s: Support) {
        support = s
        prefs.edit().putString("support", s.name).apply()
    }

    fun selectPlace(id: String) {
        placeId = id
        prefs.edit().putString("place", id).apply()
    }

    fun setGps(lat: Double, lon: Double) {
        gpsLat = lat
        gpsLon = lon
        placeId = Places.GPS_ID
        prefs.edit().putFloat("gps_lat", lat.toFloat()).putFloat("gps_lon", lon.toFloat())
            .putString("place", Places.GPS_ID).apply()
    }

    private fun loadCustomLens(): Lens? {
        val name = prefs.getString("c_name", null) ?: return null
        return Lens(
            CUSTOM_LENS_ID, name,
            prefs.getInt("c_min", 18), prefs.getInt("c_max", 55),
            prefs.getFloat("c_apw", 3.5f).toDouble(), prefs.getFloat("c_apt", 5.6f).toDouble(),
            prefs.getBoolean("c_is", true), "Custom",
        )
    }

    companion object {
        const val CUSTOM_LENS_ID = "custom"
    }
}
