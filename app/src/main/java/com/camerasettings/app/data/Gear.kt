package com.camerasettings.app.data

import com.camerasettings.app.logic.Exposure
import kotlin.math.sqrt

enum class SensorFormat(val label: String, val crop: Double, val widthMm: Double) {
    FULL_FRAME("Full frame", 1.0, 36.0),
    APS_C("APS-C (1.6× crop)", 1.6, 22.3),
    ONE_INCH("1-inch (2.7× crop)", 2.7, 13.2),
}

data class CameraBody(
    val id: String,
    val name: String,
    val sensor: SensorFormat,
    val megapixels: Double,
    /** In-body image stabilisation. */
    val ibis: Boolean,
    /** Highest ISO that still gives clean, printable results on this body (our judgement). */
    val maxGoodIso: Int,
    /** Animal / bird subject detection autofocus. */
    val animalAf: Boolean,
    val mirrorless: Boolean,
) {
    /** Approximate pixel size in micrometres (3:2 sensor). */
    val pixelPitchUm: Double get() = sensor.widthMm * 1000 / sqrt(megapixels * 1e6 * 1.5)
}

data class Lens(
    val id: String,
    val name: String,
    val minFocal: Int,
    val maxFocal: Int,
    /** Widest aperture at the short end. */
    val apWide: Double,
    /** Widest aperture at the long end. */
    val apTele: Double,
    val hasIs: Boolean,
    val mount: String,
) {
    val isZoom get() = maxFocal > minFocal

    /** Widest aperture available at a given focal length (rounded to a real Canon value). */
    fun apertureAt(focal: Int): Double {
        if (!isZoom) return apWide
        val f = ((focal - minFocal).toDouble() / (maxFocal - minFocal)).coerceIn(0.0, 1.0)
        val raw = apWide + (apTele - apWide) * f
        return Exposure.apertureStops.first { it >= raw - 0.05 }
    }
}

object Gear {
    private fun ff(id: String, name: String, mp: Double, ibis: Boolean, iso: Int, animal: Boolean, ml: Boolean = true) =
        CameraBody(id, name, SensorFormat.FULL_FRAME, mp, ibis, iso, animal, ml)

    private fun apsc(id: String, name: String, mp: Double, ibis: Boolean, iso: Int, animal: Boolean, ml: Boolean = true) =
        CameraBody(id, name, SensorFormat.APS_C, mp, ibis, iso, animal, ml)

    val cameras: List<CameraBody> = listOf(
        // Full-frame mirrorless (RF mount)
        ff("r1", "EOS R1", 24.0, true, 12800, true),
        ff("r3", "EOS R3", 24.0, true, 12800, true),
        ff("r5ii", "EOS R5 Mark II", 45.0, true, 6400, true),
        ff("r5", "EOS R5", 45.0, true, 6400, true),
        ff("r6iii", "EOS R6 Mark III", 32.5, true, 12800, true),
        ff("r6ii", "EOS R6 Mark II", 24.0, true, 12800, true),
        ff("r6", "EOS R6", 20.0, true, 12800, true),
        ff("r8", "EOS R8", 24.0, false, 12800, true),
        ff("rp", "EOS RP", 26.0, false, 6400, false),
        ff("r", "EOS R", 30.0, false, 6400, false),
        // APS-C mirrorless (RF mount)
        apsc("r7", "EOS R7", 32.5, true, 3200, true),
        apsc("r10", "EOS R10", 24.0, false, 3200, true),
        apsc("r50", "EOS R50", 24.0, false, 3200, true),
        apsc("r100", "EOS R100", 24.0, false, 3200, false),
        // EOS M (EF-M mount)
        apsc("m50ii", "EOS M50 Mark II", 24.0, false, 3200, false),
        apsc("m50", "EOS M50", 24.0, false, 3200, false),
        apsc("m6ii", "EOS M6 Mark II", 32.5, false, 3200, false),
        apsc("m200", "EOS M200 / M100", 24.0, false, 3200, false),
        // Full-frame DSLR
        ff("5div", "EOS 5D Mark IV", 30.0, false, 6400, false, ml = false),
        ff("6dii", "EOS 6D Mark II", 26.0, false, 6400, false, ml = false),
        ff("6d", "EOS 6D", 20.0, false, 6400, false, ml = false),
        // APS-C DSLR
        apsc("90d", "EOS 90D", 32.5, false, 3200, false, ml = false),
        apsc("80d", "EOS 80D", 24.0, false, 3200, false, ml = false),
        apsc("7dii", "EOS 7D Mark II", 20.0, false, 3200, false, ml = false),
        apsc("850d", "EOS 850D / Rebel T8i", 24.0, false, 3200, false, ml = false),
        apsc("800d", "EOS 800D / Rebel T7i", 24.0, false, 3200, false, ml = false),
        apsc("250d", "EOS 250D / Rebel SL3", 24.0, false, 3200, false, ml = false),
        apsc("2000d", "EOS 2000D / Rebel T7", 24.0, false, 1600, false, ml = false),
        apsc("4000d", "EOS 4000D / Rebel T100", 18.0, false, 1600, false, ml = false),
        // Compacts
        CameraBody("g7xiii", "PowerShot G7 X Mark III", SensorFormat.ONE_INCH, 20.0, false, 1600, false, true),
        CameraBody("g5xii", "PowerShot G5 X Mark II", SensorFormat.ONE_INCH, 20.0, false, 1600, false, true),
        // Fallbacks
        ff("other_ff", "Other Canon (full frame)", 24.0, false, 6400, false),
        apsc("other_apsc", "Other Canon (APS-C)", 24.0, false, 3200, false),
    )

    val lenses: List<Lens> = listOf(
        // RF full-frame
        Lens("rf14-35l", "RF 14-35mm f/4L IS", 14, 35, 4.0, 4.0, true, "RF"),
        Lens("rf15-35l", "RF 15-35mm f/2.8L IS", 15, 35, 2.8, 2.8, true, "RF"),
        Lens("rf16", "RF 16mm f/2.8 STM", 16, 16, 2.8, 2.8, false, "RF"),
        Lens("rf24-50", "RF 24-50mm f/4.5-6.3 IS STM", 24, 50, 4.5, 6.3, true, "RF"),
        Lens("rf24-105l", "RF 24-105mm f/4L IS", 24, 105, 4.0, 4.0, true, "RF"),
        Lens("rf24-105stm", "RF 24-105mm f/4-7.1 IS STM", 24, 105, 4.0, 7.1, true, "RF"),
        Lens("rf24-70l", "RF 24-70mm f/2.8L IS", 24, 70, 2.8, 2.8, true, "RF"),
        Lens("rf24-240", "RF 24-240mm f/4-6.3 IS", 24, 240, 4.0, 6.3, true, "RF"),
        Lens("rf28-70", "RF 28-70mm f/2.8 IS STM", 28, 70, 2.8, 2.8, true, "RF"),
        Lens("rf35", "RF 35mm f/1.8 Macro IS STM", 35, 35, 1.8, 1.8, true, "RF"),
        Lens("rf50", "RF 50mm f/1.8 STM", 50, 50, 1.8, 1.8, false, "RF"),
        Lens("rf85", "RF 85mm f/2 Macro IS STM", 85, 85, 2.0, 2.0, true, "RF"),
        Lens("rf70-200l", "RF 70-200mm f/4L IS", 70, 200, 4.0, 4.0, true, "RF"),
        Lens("rf100-400", "RF 100-400mm f/5.6-8 IS", 100, 400, 5.6, 8.0, true, "RF"),
        Lens("rf100-500l", "RF 100-500mm f/4.5-7.1L IS", 100, 500, 4.5, 7.1, true, "RF"),
        Lens("rf200-800", "RF 200-800mm f/6.3-9 IS", 200, 800, 6.3, 9.0, true, "RF"),
        // RF-S (APS-C)
        Lens("rfs10-18", "RF-S 10-18mm f/4.5-6.3 IS STM", 10, 18, 4.5, 6.3, true, "RF-S"),
        Lens("rfs18-45", "RF-S 18-45mm f/4.5-6.3 IS STM", 18, 45, 4.5, 6.3, true, "RF-S"),
        Lens("rfs18-150", "RF-S 18-150mm f/3.5-6.3 IS STM", 18, 150, 3.5, 6.3, true, "RF-S"),
        Lens("rfs55-210", "RF-S 55-210mm f/5-7.1 IS STM", 55, 210, 5.0, 7.1, true, "RF-S"),
        // EF-S (APS-C DSLR)
        Lens("efs10-18", "EF-S 10-18mm f/4.5-5.6 IS STM", 10, 18, 4.5, 5.6, true, "EF-S"),
        Lens("efs18-55", "EF-S 18-55mm f/3.5-5.6 IS (kit)", 18, 55, 3.5, 5.6, true, "EF-S"),
        Lens("efs18-55nois", "EF-S 18-55mm f/3.5-5.6 III (no IS)", 18, 55, 3.5, 5.6, false, "EF-S"),
        Lens("efs18-135", "EF-S 18-135mm f/3.5-5.6 IS", 18, 135, 3.5, 5.6, true, "EF-S"),
        Lens("efs55-250", "EF-S 55-250mm f/4-5.6 IS STM", 55, 250, 4.0, 5.6, true, "EF-S"),
        Lens("efs24", "EF-S 24mm f/2.8 STM", 24, 24, 2.8, 2.8, false, "EF-S"),
        // EF (full frame, also fits APS-C and RF with adapter)
        Lens("ef16-35l", "EF 16-35mm f/4L IS", 16, 35, 4.0, 4.0, true, "EF"),
        Lens("ef24-105l", "EF 24-105mm f/4L IS", 24, 105, 4.0, 4.0, true, "EF"),
        Lens("ef24-70l", "EF 24-70mm f/2.8L", 24, 70, 2.8, 2.8, false, "EF"),
        Lens("ef50", "EF 50mm f/1.8 STM", 50, 50, 1.8, 1.8, false, "EF"),
        Lens("ef70-300", "EF 70-300mm f/4-5.6 IS II", 70, 300, 4.0, 5.6, true, "EF"),
        Lens("ef100-400l", "EF 100-400mm f/4.5-5.6L IS II", 100, 400, 4.5, 5.6, true, "EF"),
        // EF-M
        Lens("efm11-22", "EF-M 11-22mm f/4-5.6 IS STM", 11, 22, 4.0, 5.6, true, "EF-M"),
        Lens("efm15-45", "EF-M 15-45mm f/3.5-6.3 IS STM", 15, 45, 3.5, 6.3, true, "EF-M"),
        Lens("efm18-150", "EF-M 18-150mm f/3.5-6.3 IS STM", 18, 150, 3.5, 6.3, true, "EF-M"),
        Lens("efm55-200", "EF-M 55-200mm f/4.5-6.3 IS STM", 55, 200, 4.5, 6.3, true, "EF-M"),
        Lens("efm22", "EF-M 22mm f/2 STM", 22, 22, 2.0, 2.0, false, "EF-M"),
        // Third party favourites
        Lens("samyang14", "Samyang/Rokinon 14mm f/2.8 (manual)", 14, 14, 2.8, 2.8, false, "Other"),
        Lens("sigma18-35", "Sigma 18-35mm f/1.8 Art", 18, 35, 1.8, 1.8, false, "Other"),
        // Compacts' built-in lenses
        Lens("g7x", "PowerShot G7 X built-in 8.8-36.8mm", 9, 37, 1.8, 2.8, true, "Built-in"),
        Lens("g5x", "PowerShot G5 X II built-in 8.8-44mm", 9, 44, 1.8, 2.8, true, "Built-in"),
    )

    fun camera(id: String?): CameraBody? = cameras.firstOrNull { it.id == id }
}
