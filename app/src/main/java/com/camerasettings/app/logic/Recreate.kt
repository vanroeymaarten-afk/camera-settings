package com.camerasettings.app.logic

import com.camerasettings.app.data.CameraBody
import com.camerasettings.app.data.Lens
import com.camerasettings.app.data.Photo
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Translate an example photo's settings to the user's own camera and lenses. */
object Recreate {
    data class Plan(
        val lens: Lens,
        val focal: Int,
        val aperture: Double,
        val shutter: Double,
        val iso: Int,
        val notes: List<String>,
    )

    /** 35 mm-equivalent focal length of the original photo. */
    fun equivalentFocal(p: Photo): Double {
        if (p.focal35 > 0) return p.focal35.toDouble()
        val cam = p.camera.lowercase()
        val crop = when {
            listOf("eos r7", "eos r10", "eos r50", "eos 90d", "eos 80d", "eos 7d", "eos 750d", "eos 800d", "eos 60d", "eos 70d", "eos m").any { cam.contains(it) } -> 1.6
            cam.contains("nikon d7") || cam.contains("nikon d5") || cam.contains("nikon d3") || cam.contains("z 50") || cam.contains("z50") -> 1.5
            cam.contains("ilce-6") || cam.contains("nex") -> 1.5
            cam.contains("fujifilm") || cam.contains("x-t") || cam.contains("x-e") || cam.contains("x-s") || cam.contains("x-h") -> 1.5
            cam.contains("olympus") || cam.contains("om digital") || cam.contains("dc-g") || cam.contains("dmc-g") -> 2.0
            else -> 1.0
        }
        return p.focal * crop
    }

    fun plan(p: Photo, body: CameraBody, bag: List<Lens>): Plan? {
        if (bag.isEmpty()) return null
        val notes = mutableListOf<String>()
        val wantedReal = equivalentFocal(p) / body.sensor.crop
        val lens = bag.minBy { l ->
            when {
                wantedReal < l.minFocal -> (l.minFocal - wantedReal) * 2
                wantedReal > l.maxFocal -> wantedReal - l.maxFocal
                else -> 0.0
            }
        }
        val focal = wantedReal.roundToInt().coerceIn(lens.minFocal, lens.maxFocal)
        if (abs(focal - wantedReal) > wantedReal * 0.15) {
            notes += if (focal < wantedReal) "Your lens doesn't zoom in this far: get closer, or crop the photo later."
            else "Your lens isn't this wide: step back, or take several photos for a panorama."
        }
        val maxOpen = lens.apertureAt(focal)
        var f = Exposure.nearestAperture(max(p.aperture, maxOpen))
        // Keep the shutter speed (it decides the motion look) and balance with ISO.
        var t = p.shutter
        var isoD = p.iso * (f / p.aperture) * (f / p.aperture)
        if (f > p.aperture + 0.05) notes += "Your lens can't open to f/${fmt(p.aperture)} here, so the background will be a little less blurry and ISO goes up to compensate."
        if (isoD > body.maxGoodIso * 1.5) {
            // Too much ISO: use a longer shutter if the photo wasn't about freezing motion.
            if (p.shutter >= 1 / 60.0) {
                t *= isoD / body.maxGoodIso
                isoD = body.maxGoodIso.toDouble()
                notes += "Use a tripod: the shutter speed gets longer to keep the noise low."
            } else {
                notes += "Expect more grain than the original."
            }
        }
        val iso = Exposure.nearestIso(isoD.coerceIn(100.0, 25600.0))
        if (t >= 0.5) notes += "This is a long exposure: use a tripod and the 2-second self-timer."
        return Plan(lens, focal, f, Exposure.nearestShutter(t.coerceAtMost(30.0)), iso, notes)
    }

    private fun fmt(f: Double) = if (f >= 10) f.roundToInt().toString() else String.format(java.util.Locale.US, "%.1f", f)
}

/** Depth of field and hyperfocal distance. */
object Dof {
    data class Result(val near: Double, val far: Double, val hyperfocal: Double)

    /** [focalMm] real focal length, [distanceM] focus distance in metres. */
    fun compute(focalMm: Double, aperture: Double, distanceM: Double, crop: Double): Result {
        val coc = 0.03 / crop
        val f = focalMm
        val s = distanceM * 1000
        val h = f * f / (aperture * coc) + f
        val near = s * (h - f) / (h + s - 2 * f)
        val far = if (s >= h) Double.POSITIVE_INFINITY else s * (h - f) / (h - s)
        return Result(near / 1000, far / 1000, h / 1000)
    }
}
