package com.camerasettings.app.data

import android.content.Context
import java.text.Normalizer
import java.time.ZoneId
import kotlin.math.cos

/** A location for sun, moon and star times. */
data class Place(
    val name: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    val zone: String,
    val population: Int = 0,
    val landmark: Boolean = false,
) {
    val zoneId: ZoneId get() = runCatching { ZoneId.of(zone) }.getOrDefault(ZoneId.systemDefault())
    val subtitle: String get() = if (landmark) "Photo spot · $country" else country

    fun encode() = listOf(name, country, lat, lon, zone, population, landmark).joinToString("|")

    companion object {
        fun decode(s: String?): Place? {
            val p = s?.split("|") ?: return null
            if (p.size < 7) return null
            return runCatching { Place(p[0], p[1], p[2].toDouble(), p[3].toDouble(), p[4], p[5].toInt(), p[6].toBoolean()) }.getOrNull()
        }
    }
}

/**
 * Offline world gazetteer: GeoNames cities with 15,000+ inhabitants (CC BY 4.0, geonames.org)
 * plus famous photo destinations. Loaded once, in the background.
 */
object World {
    @Volatile private var cities: List<Place> = emptyList()
    @Volatile private var keys: List<String> = emptyList()
    @Volatile private var landmarks: List<Place> = emptyList()
    @Volatile private var landmarkKeys: List<String> = emptyList()

    val loaded get() = cities.isNotEmpty()

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        fun read(file: String, landmark: Boolean): Pair<List<Place>, List<String>> {
            val places = ArrayList<Place>(40000)
            val k = ArrayList<String>(40000)
            ctx.assets.open(file).bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val f = line.split('\t')
                    if (f.size >= 7) {
                        places += Place(f[0], f[2], f[3].toDouble(), f[4].toDouble(), f[5], f[6].toIntOrNull() ?: 0, landmark)
                        k += f[1]
                    }
                }
            }
            return places to k
        }
        val (l, lk) = read("landmarks.tsv", true)
        landmarks = l; landmarkKeys = lk
        val (c, ck) = read("cities.tsv", false)
        keys = ck; cities = c
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFKD).replace(Regex("\\p{M}+"), "").lowercase().trim()

    /** Landmarks first, then cities by population; prefix matches before "contains" matches. */
    fun search(query: String, limit: Int = 40): List<Place> {
        val q = normalize(query)
        if (q.length < 2) return emptyList()
        val out = ArrayList<Place>()
        for (i in landmarks.indices) if (landmarkKeys[i].contains(q)) out += landmarks[i]
        val prefix = ArrayList<Place>()
        val contains = ArrayList<Place>()
        for (i in cities.indices) {
            val k = keys[i]
            if (k.startsWith(q)) prefix += cities[i] else if (q.length >= 3 && k.contains(q)) contains += cities[i]
            if (prefix.size >= limit) break
        }
        out += prefix
        out += contains.take(limit)
        return out.take(limit)
    }

    /** Nearest known city: gives a time zone (and a name) for a GPS position. */
    fun nearest(lat: Double, lon: Double): Place? {
        var best: Place? = null
        var bd = Double.MAX_VALUE
        val k = cos(Math.toRadians(lat))
        for (p in cities) {
            val dx = (p.lon - lon) * k
            val dy = p.lat - lat
            val d = dx * dx + dy * dy
            if (d < bd) { bd = d; best = p }
        }
        return best
    }

    /** The biggest city in the phone's own time zone: a sensible default location. */
    fun defaultFor(zone: ZoneId): Place? = cities.firstOrNull { it.zone == zone.id }

    val featured: List<Place> get() = landmarks
}
