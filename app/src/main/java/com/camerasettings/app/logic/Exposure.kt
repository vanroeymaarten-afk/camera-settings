package com.camerasettings.app.logic

import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Exposure maths, using the same 1/3-stop values a Canon camera shows. */
object Exposure {

    val shutterStops: List<Double> = listOf(
        1 / 8000.0, 1 / 6400.0, 1 / 5000.0, 1 / 4000.0, 1 / 3200.0, 1 / 2500.0, 1 / 2000.0,
        1 / 1600.0, 1 / 1250.0, 1 / 1000.0, 1 / 800.0, 1 / 640.0, 1 / 500.0, 1 / 400.0,
        1 / 320.0, 1 / 250.0, 1 / 200.0, 1 / 160.0, 1 / 125.0, 1 / 100.0, 1 / 80.0, 1 / 60.0,
        1 / 50.0, 1 / 40.0, 1 / 30.0, 1 / 25.0, 1 / 20.0, 1 / 15.0, 1 / 13.0, 1 / 10.0,
        1 / 8.0, 1 / 6.0, 1 / 5.0, 1 / 4.0, 0.3, 0.4, 0.5, 0.6, 0.8, 1.0, 1.3, 1.6, 2.0,
        2.5, 3.2, 4.0, 5.0, 6.0, 8.0, 10.0, 13.0, 15.0, 20.0, 25.0, 30.0,
    )

    val apertureStops: List<Double> = listOf(
        1.2, 1.4, 1.6, 1.8, 2.0, 2.2, 2.5, 2.8, 3.2, 3.5, 4.0, 4.5, 5.0, 5.6, 6.3, 7.1,
        8.0, 9.0, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 20.0, 22.0,
    )

    val isoStops: List<Int> = listOf(
        100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500,
        3200, 4000, 5000, 6400, 8000, 10000, 12800, 16000, 20000, 25600,
    )

    fun nearestShutter(t: Double): Double = shutterStops.minBy { abs(ln(it / t)) }
    fun nearestAperture(n: Double): Double = apertureStops.minBy { abs(ln(it / n)) }
    fun nearestIso(iso: Double): Int = isoStops.minBy { abs(ln(it / iso)) }

    /** Longest standard shutter speed that is not longer than [t]. */
    fun shutterAtMost(t: Double): Double =
        shutterStops.filter { it <= t * 1.04 }.maxOrNull() ?: shutterStops.first()

    /** Exposure value at ISO 100 from an incident light reading in lux (calibration C = 250). */
    fun ev100FromLux(lux: Double): Double = log2(lux.coerceAtLeast(0.01) / 2.5)

    /** Shutter time (s) for a scene of [ev100] at aperture [n] and [iso]. */
    fun shutterFor(ev100: Double, n: Double, iso: Int): Double =
        n * n / 2.0.pow(ev100 + log2(iso / 100.0))

    /** Aperture for a scene of [ev100] at shutter [t] and [iso]. */
    fun apertureFor(ev100: Double, t: Double, iso: Int): Double =
        sqrt(t * 2.0.pow(ev100 + log2(iso / 100.0)))

    fun formatShutter(t: Double): String = when {
        t >= 1.0 && t == floor(t) -> "${t.toInt()} s"
        t >= 0.3 -> String.format(Locale.US, "%.1f s", t)
        else -> "1/${(1 / t).roundToInt()} s"
    }

    fun formatAperture(n: Double): String =
        if (n >= 10) "f/${n.roundToInt()}" else String.format(Locale.US, "f/%.1f", n)

    fun formatEv(ev: Double): String = String.format(Locale.US, "EV %.1f", ev)
}
