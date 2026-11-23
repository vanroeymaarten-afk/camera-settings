package com.camerasettings.app.logic

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Offline sun, moon and Milky Way calculations (accurate to a minute or two, which is
 * plenty for planning photos). No internet needed.
 */
object Astro {
    private const val OBLIQUITY = 23.439
    private const val RISE_SET_ALT = -0.833

    /** Centre of the Milky Way (Sagittarius A*). */
    val GALACTIC_CENTER = Equatorial(266.4168, -29.0078)

    data class SkyTarget(val name: String, val pos: Equatorial, val hint: String)

    /** Night-sky targets worth photographing from Chile. */
    val targets = listOf(
        SkyTarget("Milky Way centre (Sagittarius)", GALACTIC_CENTER, "The brightest, most dramatic part. Best March–September."),
        SkyTarget("Carina Nebula & Southern Cross", Equatorial(161.0, -59.9), "Bright southern Milky Way with pink nebula. Best December–May."),
        SkyTarget("Large Magellanic Cloud", Equatorial(80.9, -69.8), "A neighbour galaxy, visible to the naked eye from dark places."),
    )

    data class Equatorial(val raDeg: Double, val decDeg: Double)
    data class Horizontal(val altDeg: Double, val azDeg: Double)

    private fun Double.rad() = Math.toRadians(this)
    private fun Double.deg() = Math.toDegrees(this)
    private fun norm360(x: Double) = ((x % 360) + 360) % 360

    fun julianDay(t: ZonedDateTime): Double = t.toInstant().toEpochMilli() / 86_400_000.0 + 2440587.5

    private fun sunLongitude(jd: Double): Double {
        val n = jd - 2451545.0
        val l = norm360(280.460 + 0.9856474 * n)
        val g = norm360(357.528 + 0.9856003 * n).rad()
        return norm360(l + 1.915 * sin(g) + 0.020 * sin(2 * g))
    }

    /** Moon ecliptic longitude, latitude and horizontal parallax, all in degrees. */
    private fun moonEcliptic(jd: Double): Triple<Double, Double, Double> {
        val t = (jd - 2451545.0) / 36525.0
        fun s(a: Double) = sin(a.rad())
        fun c(a: Double) = cos(a.rad())
        val lon = 218.32 + 481267.881 * t +
            6.29 * s(135.0 + 477198.87 * t) - 1.27 * s(259.3 - 413335.36 * t) +
            0.66 * s(235.7 + 890534.22 * t) + 0.21 * s(269.9 + 954397.74 * t) -
            0.19 * s(357.5 + 35999.05 * t) - 0.11 * s(186.5 + 966404.03 * t)
        val lat = 5.13 * s(93.3 + 483202.02 * t) + 0.28 * s(228.2 + 960400.89 * t) -
            0.28 * s(318.3 + 6003.15 * t) - 0.17 * s(217.6 - 407332.21 * t)
        val par = 0.9508 + 0.0518 * c(135.0 + 477198.87 * t) + 0.0095 * c(259.3 - 413335.36 * t) +
            0.0078 * c(235.7 + 890534.22 * t) + 0.0028 * c(269.9 + 954397.74 * t)
        return Triple(norm360(lon), lat, par)
    }

    private fun eclipticToEquatorial(lonDeg: Double, latDeg: Double): Equatorial {
        val l = lonDeg.rad()
        val b = latDeg.rad()
        val e = OBLIQUITY.rad()
        val ra = atan2(sin(l) * cos(e) - tan(b) * sin(e), cos(l))
        val dec = asin(sin(b) * cos(e) + cos(b) * sin(e) * sin(l))
        return Equatorial(norm360(ra.deg()), dec.deg())
    }

    private fun gmstDeg(jd: Double) = norm360(280.46061837 + 360.98564736629 * (jd - 2451545.0))

    fun horizontal(eq: Equatorial, jd: Double, lat: Double, lon: Double): Horizontal {
        val h = (gmstDeg(jd) + lon - eq.raDeg).rad()
        val phi = lat.rad()
        val d = eq.decDeg.rad()
        val alt = asin(sin(phi) * sin(d) + cos(phi) * cos(d) * cos(h))
        val az = atan2(sin(h), cos(h) * sin(phi) - tan(d) * cos(phi))
        return Horizontal(alt.deg(), norm360(az.deg() + 180))
    }

    fun sunAltitude(jd: Double, lat: Double, lon: Double): Double =
        horizontal(eclipticToEquatorial(sunLongitude(jd), 0.0), jd, lat, lon).altDeg

    /** Topocentric moon altitude (parallax corrected). */
    fun moonAltitude(jd: Double, lat: Double, lon: Double): Double {
        val (l, b, par) = moonEcliptic(jd)
        val geo = horizontal(eclipticToEquatorial(l, b), jd, lat, lon).altDeg
        return geo - par * cos(geo.rad())
    }

    data class MoonPhase(val illumination: Double, val waxing: Boolean) {
        val name: String
            get() = when {
                illumination < 0.03 -> "New moon"
                illumination > 0.97 -> "Full moon"
                illumination in 0.45..0.55 -> if (waxing) "First quarter" else "Last quarter"
                illumination < 0.5 -> if (waxing) "Waxing crescent" else "Waning crescent"
                else -> if (waxing) "Waxing gibbous" else "Waning gibbous"
            }
    }

    fun moonPhase(jd: Double): MoonPhase {
        val ls = sunLongitude(jd)
        val (lm, bm, _) = moonEcliptic(jd)
        val elong = acos(cos(bm.rad()) * cos((lm - ls).rad()))
        return MoonPhase((1 - cos(elong)) / 2, norm360(lm - ls) < 180)
    }

    // ---------------------------------------------------------------- day planning

    data class Window(val start: ZonedDateTime, val end: ZonedDateTime)

    data class SunTimes(
        val astroDawn: ZonedDateTime?,
        val blueMorningStart: ZonedDateTime?,
        val goldenMorningStart: ZonedDateTime?,
        val sunrise: ZonedDateTime?,
        val goldenMorningEnd: ZonedDateTime?,
        val goldenEveningStart: ZonedDateTime?,
        val sunset: ZonedDateTime?,
        val blueEveningStart: ZonedDateTime?,
        val blueEveningEnd: ZonedDateTime?,
        val astroDusk: ZonedDateTime?,
    )

    data class Night(
        /** True when the sun gets at least 18° below the horizon (real darkness). */
        val fullyDark: Boolean,
        /** Lowest sun altitude during the night, in degrees. */
        val lowestSun: Double,
        /** Windows where the sky is dark enough for stars (astronomical, or nautical if never fully dark). */
        val dark: List<Window>,
        /** Dark AND moon below the horizon. */
        val moonFree: List<Window>,
        /** For each of [targets]: when it is 15°+ up in a dark, moon-free sky. */
        val targets: List<TargetVisibility>,
    ) {
        val milkyWayCore: List<Window> get() = targets.first().windows
    }

    data class TargetVisibility(
        val target: SkyTarget,
        val windows: List<Window>,
        val bestTime: ZonedDateTime?,
        val bestAlt: Double,
        val bestAz: Double,
    )

    data class DayPlan(
        val date: LocalDate,
        val zone: ZoneId,
        val sun: SunTimes,
        val moonrise: ZonedDateTime?,
        val moonset: ZonedDateTime?,
        val moon: MoonPhase,
        val night: Night,
    )

    private class Crossing(val time: ZonedDateTime, val rising: Boolean)

    private fun windows(times: List<ZonedDateTime>, flags: List<Boolean>): List<Window> {
        val out = mutableListOf<Window>()
        var startIdx = -1
        for (i in flags.indices) {
            if (flags[i] && startIdx < 0) startIdx = i
            if ((!flags[i] || i == flags.lastIndex) && startIdx >= 0) {
                val endIdx = if (flags[i]) i else i - 1
                if (endIdx > startIdx) out += Window(times[startIdx], times[endIdx])
                startIdx = -1
            }
        }
        return out
    }

    /** Sample a function every [step] minutes and return all threshold crossings. */
    private class Samples(val start: ZonedDateTime, val step: Long, val values: DoubleArray) {
        fun crossings(threshold: Double): List<Crossing> {
            val out = mutableListOf<Crossing>()
            for (i in 1 until values.size) {
                val v0 = values[i - 1] - threshold
                val v1 = values[i] - threshold
                if ((v0 < 0) != (v1 < 0)) {
                    val frac = v0 / (v0 - v1)
                    out += Crossing(start.plusSeconds((((i - 1) + frac) * step * 60).toLong()), v1 > v0)
                }
            }
            return out
        }
    }

    private fun sample(start: ZonedDateTime, minutes: Long, step: Long, f: (Double) -> Double): Samples {
        val n = (minutes / step).toInt() + 1
        val jd0 = julianDay(start)
        return Samples(start, step, DoubleArray(n) { f(jd0 + it * step / 1440.0) })
    }

    fun planDay(date: LocalDate, zone: ZoneId, lat: Double, lon: Double): DayPlan {
        val midnight = date.atStartOfDay(zone)
        val sunF = { jd: Double -> sunAltitude(jd, lat, lon) }
        val sunDay = sample(midnight, 1440, 2, sunF)
        fun rise(th: Double) = sunDay.crossings(th).firstOrNull { it.rising }?.time
        fun set(th: Double) = sunDay.crossings(th).firstOrNull { !it.rising }?.time

        val sun = SunTimes(
            astroDawn = rise(-18.0),
            blueMorningStart = rise(-6.0),
            goldenMorningStart = rise(-4.0),
            sunrise = rise(RISE_SET_ALT),
            goldenMorningEnd = rise(6.0),
            goldenEveningStart = set(6.0),
            sunset = set(RISE_SET_ALT),
            blueEveningStart = set(-4.0),
            blueEveningEnd = set(-6.0),
            astroDusk = set(-18.0),
        )

        val moonF = { jd: Double -> moonAltitude(jd, lat, lon) }
        val moonCross = sample(midnight, 1440, 4, moonF).crossings(RISE_SET_ALT)
        val moonrise = moonCross.firstOrNull { it.rising }?.time
        val moonset = moonCross.firstOrNull { !it.rising }?.time

        // Night: from today's sunset (or 18:00) until tomorrow's sunrise (or 08:00)
        val nightStart = sun.sunset ?: midnight.plusHours(18)
        val nextSunrise = sample(midnight.plusDays(1), 720, 2, sunF).crossings(RISE_SET_ALT)
            .firstOrNull { it.rising }?.time ?: midnight.plusDays(1).plusHours(8)
        val times = mutableListOf<ZonedDateTime>()
        var t = nightStart
        while (!t.isAfter(nextSunrise)) { times += t; t = t.plusMinutes(5) }
        val jds = times.map { julianDay(it) }
        val sunAlt = jds.map { sunF(it) }
        val moonAlt = jds.map { moonF(it) }
        val lowest = sunAlt.minOrNull() ?: 0.0
        val fullyDark = lowest <= -18.0
        val darkLimit = if (fullyDark) -18.0 else -12.0
        val phase = moonPhase(julianDay(midnight.plusHours(24)))

        val darkFlags = sunAlt.map { it <= darkLimit }
        // A thin crescent barely brightens the sky, so treat it as moon-free.
        val moonFreeFlags = darkFlags.indices.map { darkFlags[it] && (moonAlt[it] < 0 || phase.illumination < 0.1) }

        val visibility = targets.map { target ->
            val pos = jds.map { horizontal(target.pos, it, lat, lon) }
            val flags = moonFreeFlags.indices.map { moonFreeFlags[it] && pos[it].altDeg >= 15 }
            var best = -1
            for (i in flags.indices) if (flags[i] && (best < 0 || pos[i].altDeg > pos[best].altDeg)) best = i
            TargetVisibility(
                target,
                windows(times, flags),
                if (best >= 0) times[best] else null,
                if (best >= 0) pos[best].altDeg else 0.0,
                if (best >= 0) pos[best].azDeg else 0.0,
            )
        }

        val night = Night(
            fullyDark = fullyDark,
            lowestSun = lowest,
            dark = windows(times, darkFlags),
            moonFree = windows(times, moonFreeFlags),
            targets = visibility,
        )

        return DayPlan(date, zone, sun, moonrise, moonset, phase, night)
    }

    // ---------------------------------------------------------------- helpers for the UI

    data class LightEvent(val label: String, val time: ZonedDateTime, val kind: Kind) {
        enum class Kind { BLUE, GOLDEN, SUN, DARK }
    }

    /** All photo-relevant light moments of a day, in time order. */
    fun events(plan: DayPlan): List<LightEvent> {
        val s = plan.sun
        return listOfNotNull(
            s.astroDawn?.let { LightEvent("Night ends", it, LightEvent.Kind.DARK) },
            s.blueMorningStart?.let { LightEvent("Blue hour", it, LightEvent.Kind.BLUE) },
            s.goldenMorningStart?.let { LightEvent("Golden hour", it, LightEvent.Kind.GOLDEN) },
            s.sunrise?.let { LightEvent("Sunrise", it, LightEvent.Kind.SUN) },
            s.goldenEveningStart?.let { LightEvent("Golden hour", it, LightEvent.Kind.GOLDEN) },
            s.sunset?.let { LightEvent("Sunset", it, LightEvent.Kind.SUN) },
            s.blueEveningStart?.let { LightEvent("Blue hour", it, LightEvent.Kind.BLUE) },
            s.astroDusk?.let { LightEvent("Dark sky", it, LightEvent.Kind.DARK) },
        ).sortedBy { it.time }
    }

    /** Next light moment after [now] (looks into tomorrow if needed). */
    fun nextEvent(now: ZonedDateTime, lat: Double, lon: Double): LightEvent? {
        for (d in 0L..1L) {
            val date = now.toLocalDate().plusDays(d)
            val e = events(planDay(date, now.zone, lat, lon)).firstOrNull { it.time.isAfter(now) }
            if (e != null) return e
        }
        return null
    }

    data class StarNight(val date: LocalDate, val moonFreeMinutes: Long, val illumination: Double, val waxing: Boolean, val fullyDark: Boolean)

    /** Moon-free dark minutes for each night starting at [from], for [days] nights. */
    fun starCalendar(from: LocalDate, days: Int, zone: ZoneId, lat: Double, lon: Double): List<StarNight> =
        (0 until days).map { i ->
            val d = from.plusDays(i.toLong())
            val p = planDay(d, zone, lat, lon)
            val minutes = p.night.moonFree.sumOf { java.time.Duration.between(it.start, it.end).toMinutes() }
            StarNight(d, minutes, p.moon.illumination, p.moon.waxing, p.night.fullyDark)
        }

    fun compass(azDeg: Double): String {
        val names = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return names[(((azDeg + 22.5) % 360) / 45).toInt()]
    }
}
