package com.camerasettings.app.data

import com.camerasettings.app.logic.Exposure
import kotlin.math.sqrt

enum class SensorFormat(val label: String, val crop: Double, val widthMm: Double) {
    MEDIUM("Medium format (0.8×)", 0.79, 43.8),
    FULL_FRAME("Full frame", 1.0, 36.0),
    APS_C("APS-C (1.5×)", 1.5, 23.5),
    APS_C_CANON("APS-C (1.6×)", 1.6, 22.3),
    MFT("Micro Four Thirds (2×)", 2.0, 17.3),
    ONE_INCH("1-inch compact (2.7×)", 2.7, 13.2),
}

data class CameraBody(
    val id: String,
    val brand: Brand,
    val name: String,
    val sensor: SensorFormat,
    val megapixels: Double,
    /** In-body image stabilisation. */
    val ibis: Boolean,
    /** Highest ISO that still gives clean, printable results (our judgement). */
    val maxGoodIso: Int,
    /** Animal / bird subject detection autofocus. */
    val animalAf: Boolean,
    val mirrorless: Boolean,
    /** Lens mount family, see [Gear.mountsFor]. */
    val mount: String,
    /** Built-in lens id for compact cameras. */
    val fixedLens: String? = null,
) {
    val pixelPitchUm: Double get() = sensor.widthMm * 1000 / sqrt(megapixels * 1e6 * 1.5)

    val rawFormat: String
        get() = when (brand) {
            Brand.CANON -> if (id in CANON_CR2) "CR2" else "CR3"
            Brand.NIKON -> "NEF"
            Brand.SONY -> "ARW"
            Brand.FUJIFILM -> "RAF"
            Brand.PANASONIC -> "RW2"
            Brand.OM -> "ORF"
            Brand.OTHER -> "RAW"
        }

    val rawMb: Double get() = megapixels * 1.2
    val craw: Boolean get() = brand == Brand.CANON && rawFormat == "CR3"
    val jpegMb: Double get() = megapixels * 0.33

    private companion object {
        val CANON_CR2 = setOf("5div", "6dii", "6d", "80d", "7dii", "750d", "800d", "2000d", "4000d")
    }
}

data class Lens(
    val id: String,
    val name: String,
    val minFocal: Int,
    val maxFocal: Int,
    val apWide: Double,
    val apTele: Double,
    val hasIs: Boolean,
    val mount: String,
) {
    val isZoom get() = maxFocal > minFocal

    fun apertureAt(focal: Int): Double {
        if (!isZoom) return apWide
        val f = ((focal - minFocal).toDouble() / (maxFocal - minFocal)).coerceIn(0.0, 1.0)
        val raw = apWide + (apTele - apWide) * f
        return Exposure.apertureStops.first { it >= raw - 0.05 }
    }
}

object Gear {
    private fun c(id: String, brand: Brand, name: String, s: SensorFormat, mp: Double, ibis: Boolean, iso: Int, animal: Boolean, mount: String, ml: Boolean = true, fixed: String? = null) =
        CameraBody(id, brand, name, s, mp, ibis, iso, animal, ml, mount, fixed)

    private val FF = SensorFormat.FULL_FRAME
    private val APSC = SensorFormat.APS_C
    private val CAPSC = SensorFormat.APS_C_CANON
    private val MFT = SensorFormat.MFT
    private val ONE = SensorFormat.ONE_INCH

    val cameras: List<CameraBody> = listOf(
        // ------------------------------------------------------------ Canon
        c("r1", Brand.CANON, "EOS R1", FF, 24.0, true, 12800, true, "RF"),
        c("r3", Brand.CANON, "EOS R3", FF, 24.0, true, 12800, true, "RF"),
        c("r5ii", Brand.CANON, "EOS R5 Mark II", FF, 45.0, true, 6400, true, "RF"),
        c("r5", Brand.CANON, "EOS R5", FF, 45.0, true, 6400, true, "RF"),
        c("r6iii", Brand.CANON, "EOS R6 Mark III", FF, 32.5, true, 12800, true, "RF"),
        c("r6ii", Brand.CANON, "EOS R6 Mark II", FF, 24.0, true, 12800, true, "RF"),
        c("r6", Brand.CANON, "EOS R6", FF, 20.0, true, 12800, true, "RF"),
        c("r8", Brand.CANON, "EOS R8", FF, 24.0, false, 12800, true, "RF"),
        c("rp", Brand.CANON, "EOS RP", FF, 26.0, false, 6400, false, "RF"),
        c("r", Brand.CANON, "EOS R", FF, 30.0, false, 6400, false, "RF"),
        c("r7", Brand.CANON, "EOS R7", CAPSC, 32.5, true, 3200, true, "RF"),
        c("r10", Brand.CANON, "EOS R10", CAPSC, 24.0, false, 3200, true, "RF"),
        c("r50", Brand.CANON, "EOS R50", CAPSC, 24.0, false, 3200, true, "RF"),
        c("r100", Brand.CANON, "EOS R100", CAPSC, 24.0, false, 3200, false, "RF"),
        c("m50ii", Brand.CANON, "EOS M50 Mark II", CAPSC, 24.0, false, 3200, false, "EF-M"),
        c("m50", Brand.CANON, "EOS M50", CAPSC, 24.0, false, 3200, false, "EF-M"),
        c("m6ii", Brand.CANON, "EOS M6 Mark II", CAPSC, 32.5, false, 3200, false, "EF-M"),
        c("m200", Brand.CANON, "EOS M200 / M100", CAPSC, 24.0, false, 3200, false, "EF-M"),
        c("5div", Brand.CANON, "EOS 5D Mark IV", FF, 30.0, false, 6400, false, "EF", ml = false),
        c("6dii", Brand.CANON, "EOS 6D Mark II", FF, 26.0, false, 6400, false, "EF", ml = false),
        c("6d", Brand.CANON, "EOS 6D", FF, 20.0, false, 6400, false, "EF", ml = false),
        c("90d", Brand.CANON, "EOS 90D", CAPSC, 32.5, false, 3200, false, "EF-S", ml = false),
        c("80d", Brand.CANON, "EOS 80D", CAPSC, 24.0, false, 3200, false, "EF-S", ml = false),
        c("7dii", Brand.CANON, "EOS 7D Mark II", CAPSC, 20.0, false, 3200, false, "EF-S", ml = false),
        c("850d", Brand.CANON, "EOS 850D / Rebel T8i", CAPSC, 24.0, false, 3200, false, "EF-S", ml = false),
        c("750d", Brand.CANON, "EOS 750D / Rebel T6i", CAPSC, 24.0, false, 3200, false, "EF-S", ml = false),
        c("800d", Brand.CANON, "EOS 800D / Rebel T7i", CAPSC, 24.0, false, 3200, false, "EF-S", ml = false),
        c("250d", Brand.CANON, "EOS 250D / Rebel SL3", CAPSC, 24.0, false, 3200, false, "EF-S", ml = false),
        c("2000d", Brand.CANON, "EOS 2000D / Rebel T7", CAPSC, 24.0, false, 1600, false, "EF-S", ml = false),
        c("4000d", Brand.CANON, "EOS 4000D / Rebel T100", CAPSC, 18.0, false, 1600, false, "EF-S", ml = false),
        c("g7xiii", Brand.CANON, "PowerShot G7 X Mark III", ONE, 20.0, false, 1600, false, "fixed", fixed = "g7x"),
        c("g5xii", Brand.CANON, "PowerShot G5 X Mark II", ONE, 20.0, false, 1600, false, "fixed", fixed = "g5x"),
        // ------------------------------------------------------------ Nikon
        c("z9", Brand.NIKON, "Z9", FF, 45.7, true, 6400, true, "Z"),
        c("z8", Brand.NIKON, "Z8", FF, 45.7, true, 6400, true, "Z"),
        c("z7ii", Brand.NIKON, "Z7 II", FF, 45.7, true, 6400, true, "Z"),
        c("z6iii", Brand.NIKON, "Z6 III", FF, 24.5, true, 12800, true, "Z"),
        c("z6ii", Brand.NIKON, "Z6 II", FF, 24.5, true, 12800, true, "Z"),
        c("zf", Brand.NIKON, "Zf", FF, 24.5, true, 12800, true, "Z"),
        c("z5ii", Brand.NIKON, "Z5 II", FF, 24.5, true, 12800, true, "Z"),
        c("z5", Brand.NIKON, "Z5", FF, 24.3, true, 12800, true, "Z"),
        c("z50ii", Brand.NIKON, "Z50 II", APSC, 20.9, false, 6400, true, "Z"),
        c("z50", Brand.NIKON, "Z50", APSC, 20.9, false, 6400, true, "Z"),
        c("zfc", Brand.NIKON, "Z fc", APSC, 20.9, false, 6400, true, "Z"),
        c("z30", Brand.NIKON, "Z30", APSC, 20.9, false, 6400, true, "Z"),
        c("d850", Brand.NIKON, "D850", FF, 45.7, false, 6400, false, "F", ml = false),
        c("d780", Brand.NIKON, "D780", FF, 24.5, false, 12800, false, "F", ml = false),
        c("d750", Brand.NIKON, "D750", FF, 24.3, false, 6400, false, "F", ml = false),
        c("d7500", Brand.NIKON, "D7500", APSC, 20.9, false, 6400, false, "F-DX", ml = false),
        c("d5600", Brand.NIKON, "D5600", APSC, 24.2, false, 3200, false, "F-DX", ml = false),
        c("d3500", Brand.NIKON, "D3500", APSC, 24.2, false, 3200, false, "F-DX", ml = false),
        // ------------------------------------------------------------ Sony
        c("a1ii", Brand.SONY, "Alpha 1 II", FF, 50.1, true, 6400, true, "FE"),
        c("a7rv", Brand.SONY, "A7R V", FF, 61.0, true, 6400, true, "FE"),
        c("a7iv", Brand.SONY, "A7 IV", FF, 33.0, true, 12800, true, "FE"),
        c("a7iii", Brand.SONY, "A7 III", FF, 24.2, true, 12800, true, "FE"),
        c("a7cii", Brand.SONY, "A7C II", FF, 33.0, true, 12800, true, "FE"),
        c("a7c", Brand.SONY, "A7C", FF, 24.2, true, 12800, true, "FE"),
        c("a6700", Brand.SONY, "A6700", APSC, 26.0, true, 6400, true, "E"),
        c("a6600", Brand.SONY, "A6600", APSC, 24.2, true, 3200, true, "E"),
        c("a6400", Brand.SONY, "A6400", APSC, 24.2, false, 3200, true, "E"),
        c("a6100", Brand.SONY, "A6100", APSC, 24.2, false, 3200, true, "E"),
        c("zve10", Brand.SONY, "ZV-E10", APSC, 24.2, false, 3200, false, "E"),
        c("rx100vii", Brand.SONY, "RX100 VII", ONE, 20.1, false, 1600, true, "fixed", fixed = "rx100"),
        // ------------------------------------------------------------ Fujifilm
        c("xt5", Brand.FUJIFILM, "X-T5", APSC, 40.2, true, 6400, true, "X"),
        c("xt4", Brand.FUJIFILM, "X-T4", APSC, 26.1, true, 6400, false, "X"),
        c("xt50", Brand.FUJIFILM, "X-T50", APSC, 40.2, true, 6400, true, "X"),
        c("xt30ii", Brand.FUJIFILM, "X-T30 II", APSC, 26.1, false, 6400, false, "X"),
        c("xs20", Brand.FUJIFILM, "X-S20", APSC, 26.1, true, 6400, true, "X"),
        c("xs10", Brand.FUJIFILM, "X-S10", APSC, 26.1, true, 6400, false, "X"),
        c("xh2", Brand.FUJIFILM, "X-H2", APSC, 40.2, true, 6400, true, "X"),
        c("xe4", Brand.FUJIFILM, "X-E4", APSC, 26.1, false, 6400, false, "X"),
        c("x100vi", Brand.FUJIFILM, "X100VI", APSC, 40.2, true, 6400, true, "fixed", fixed = "x100"),
        c("x100v", Brand.FUJIFILM, "X100V", APSC, 26.1, false, 6400, false, "fixed", fixed = "x100"),
        // ------------------------------------------------------------ Panasonic
        c("s5ii", Brand.PANASONIC, "Lumix S5 II / S5 IIX", FF, 24.2, true, 12800, true, "L"),
        c("s5", Brand.PANASONIC, "Lumix S5", FF, 24.2, true, 12800, false, "L"),
        c("g9ii", Brand.PANASONIC, "Lumix G9 II", MFT, 25.2, true, 3200, true, "MFT"),
        c("g9", Brand.PANASONIC, "Lumix G9", MFT, 20.3, true, 3200, false, "MFT"),
        c("gh6", Brand.PANASONIC, "Lumix GH6", MFT, 25.2, true, 3200, false, "MFT"),
        c("g85", Brand.PANASONIC, "Lumix G85 / G80", MFT, 16.0, true, 3200, false, "MFT"),
        c("g100", Brand.PANASONIC, "Lumix G100", MFT, 20.3, false, 3200, false, "MFT"),
        c("tz200", Brand.PANASONIC, "Lumix TZ200 / ZS200", ONE, 20.1, false, 1600, false, "fixed", fixed = "tz200"),
        // ------------------------------------------------------------ OM System / Olympus
        c("om1ii", Brand.OM, "OM-1 Mark II", MFT, 20.4, true, 3200, true, "MFT"),
        c("om1", Brand.OM, "OM-1", MFT, 20.4, true, 3200, true, "MFT"),
        c("om5", Brand.OM, "OM-5", MFT, 20.4, true, 3200, false, "MFT"),
        c("em1iii", Brand.OM, "OM-D E-M1 Mark III", MFT, 20.4, true, 3200, false, "MFT"),
        c("em5iii", Brand.OM, "OM-D E-M5 Mark III", MFT, 20.4, true, 3200, false, "MFT"),
        c("em10iv", Brand.OM, "OM-D E-M10 Mark IV", MFT, 20.3, true, 3200, false, "MFT"),
        // ------------------------------------------------------------ don't know the model
        c("other_ff", Brand.OTHER, "Full-frame camera (model not listed)", FF, 24.0, false, 6400, false, "any"),
        c("other_apsc", Brand.OTHER, "APS-C camera (model not listed)", APSC, 24.0, false, 3200, false, "any"),
        c("other_mft", Brand.OTHER, "Micro Four Thirds camera (model not listed)", MFT, 20.0, false, 3200, false, "any"),
        c("other_1inch", Brand.OTHER, "Compact camera with 1-inch sensor", ONE, 20.0, false, 1600, false, "any"),
    )

    val lenses: List<Lens> = listOf(
        // Canon RF
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
        Lens("rfs10-18", "RF-S 10-18mm f/4.5-6.3 IS STM", 10, 18, 4.5, 6.3, true, "RF-S"),
        Lens("rfs18-45", "RF-S 18-45mm f/4.5-6.3 IS STM", 18, 45, 4.5, 6.3, true, "RF-S"),
        Lens("rfs18-150", "RF-S 18-150mm f/3.5-6.3 IS STM", 18, 150, 3.5, 6.3, true, "RF-S"),
        Lens("rfs55-210", "RF-S 55-210mm f/5-7.1 IS STM", 55, 210, 5.0, 7.1, true, "RF-S"),
        // Canon EF / EF-S / EF-M
        Lens("efs10-18", "EF-S 10-18mm f/4.5-5.6 IS STM", 10, 18, 4.5, 5.6, true, "EF-S"),
        Lens("efs18-55", "EF-S 18-55mm f/3.5-5.6 IS (kit)", 18, 55, 3.5, 5.6, true, "EF-S"),
        Lens("efs18-55nois", "EF-S 18-55mm f/3.5-5.6 III (no IS)", 18, 55, 3.5, 5.6, false, "EF-S"),
        Lens("efs18-135", "EF-S 18-135mm f/3.5-5.6 IS", 18, 135, 3.5, 5.6, true, "EF-S"),
        Lens("efs55-250", "EF-S 55-250mm f/4-5.6 IS STM", 55, 250, 4.0, 5.6, true, "EF-S"),
        Lens("efs24", "EF-S 24mm f/2.8 STM", 24, 24, 2.8, 2.8, false, "EF-S"),
        Lens("ef16-35l", "EF 16-35mm f/4L IS", 16, 35, 4.0, 4.0, true, "EF"),
        Lens("ef24-105l", "EF 24-105mm f/4L IS", 24, 105, 4.0, 4.0, true, "EF"),
        Lens("ef50", "EF 50mm f/1.8 STM", 50, 50, 1.8, 1.8, false, "EF"),
        Lens("ef70-300", "EF 70-300mm f/4-5.6 IS II", 70, 300, 4.0, 5.6, true, "EF"),
        Lens("ef100-400l", "EF 100-400mm f/4.5-5.6L IS II", 100, 400, 4.5, 5.6, true, "EF"),
        Lens("efm11-22", "EF-M 11-22mm f/4-5.6 IS STM", 11, 22, 4.0, 5.6, true, "EF-M"),
        Lens("efm15-45", "EF-M 15-45mm f/3.5-6.3 IS STM", 15, 45, 3.5, 6.3, true, "EF-M"),
        Lens("efm18-150", "EF-M 18-150mm f/3.5-6.3 IS STM", 18, 150, 3.5, 6.3, true, "EF-M"),
        Lens("efm55-200", "EF-M 55-200mm f/4.5-6.3 IS STM", 55, 200, 4.5, 6.3, true, "EF-M"),
        Lens("efm22", "EF-M 22mm f/2 STM", 22, 22, 2.0, 2.0, false, "EF-M"),
        // Nikon Z
        Lens("nz14-30", "Nikkor Z 14-30mm f/4 S", 14, 30, 4.0, 4.0, false, "Z"),
        Lens("nz24-70f4", "Nikkor Z 24-70mm f/4 S", 24, 70, 4.0, 4.0, false, "Z"),
        Lens("nz24-120", "Nikkor Z 24-120mm f/4 S", 24, 120, 4.0, 4.0, false, "Z"),
        Lens("nz24-200", "Nikkor Z 24-200mm f/4-6.3 VR", 24, 200, 4.0, 6.3, true, "Z"),
        Lens("nz24-50", "Nikkor Z 24-50mm f/4-6.3", 24, 50, 4.0, 6.3, false, "Z"),
        Lens("nz40", "Nikkor Z 40mm f/2", 40, 40, 2.0, 2.0, false, "Z"),
        Lens("nz50", "Nikkor Z 50mm f/1.8 S", 50, 50, 1.8, 1.8, false, "Z"),
        Lens("nz70-180", "Nikkor Z 70-180mm f/2.8", 70, 180, 2.8, 2.8, false, "Z"),
        Lens("nz100-400", "Nikkor Z 100-400mm f/4.5-5.6 VR S", 100, 400, 4.5, 5.6, true, "Z"),
        Lens("nz180-600", "Nikkor Z 180-600mm f/5.6-6.3 VR", 180, 600, 5.6, 6.3, true, "Z"),
        Lens("nzdx16-50", "Nikkor Z DX 16-50mm f/3.5-6.3 VR", 16, 50, 3.5, 6.3, true, "Z"),
        Lens("nzdx50-250", "Nikkor Z DX 50-250mm f/4.5-6.3 VR", 50, 250, 4.5, 6.3, true, "Z"),
        Lens("nzdx18-140", "Nikkor Z DX 18-140mm f/3.5-6.3 VR", 18, 140, 3.5, 6.3, true, "Z"),
        // Nikon F
        Lens("nf18-55", "AF-P DX 18-55mm f/3.5-5.6G VR", 18, 55, 3.5, 5.6, true, "F-DX"),
        Lens("nf70-300dx", "AF-P DX 70-300mm f/4.5-6.3G VR", 70, 300, 4.5, 6.3, true, "F-DX"),
        Lens("nf18-140", "AF-S DX 18-140mm f/3.5-5.6G VR", 18, 140, 3.5, 5.6, true, "F-DX"),
        Lens("nf24-120", "AF-S 24-120mm f/4G VR", 24, 120, 4.0, 4.0, true, "F"),
        Lens("nf50", "AF-S 50mm f/1.8G", 50, 50, 1.8, 1.8, false, "F"),
        // Sony
        Lens("fe28-70", "FE 28-70mm f/3.5-5.6 OSS (kit)", 28, 70, 3.5, 5.6, true, "FE"),
        Lens("fe20-70", "FE 20-70mm f/4 G", 20, 70, 4.0, 4.0, false, "FE"),
        Lens("fe24-105", "FE 24-105mm f/4 G OSS", 24, 105, 4.0, 4.0, true, "FE"),
        Lens("fe24-70gm", "FE 24-70mm f/2.8 GM II", 24, 70, 2.8, 2.8, false, "FE"),
        Lens("fe16-35", "FE PZ 16-35mm f/4 G", 16, 35, 4.0, 4.0, false, "FE"),
        Lens("fe70-200", "FE 70-200mm f/4 Macro G OSS II", 70, 200, 4.0, 4.0, true, "FE"),
        Lens("fe200-600", "FE 200-600mm f/5.6-6.3 G OSS", 200, 600, 5.6, 6.3, true, "FE"),
        Lens("fe50", "FE 50mm f/1.8", 50, 50, 1.8, 1.8, false, "FE"),
        Lens("e16-50", "E PZ 16-50mm f/3.5-5.6 OSS (kit)", 16, 50, 3.5, 5.6, true, "E"),
        Lens("e18-135", "E 18-135mm f/3.5-5.6 OSS", 18, 135, 3.5, 5.6, true, "E"),
        Lens("e70-350", "E 70-350mm f/4.5-6.3 G OSS", 70, 350, 4.5, 6.3, true, "E"),
        Lens("e55-210", "E 55-210mm f/4.5-6.3 OSS", 55, 210, 4.5, 6.3, true, "E"),
        Lens("sigma18-50", "Sigma 18-50mm f/2.8 DC DN", 18, 50, 2.8, 2.8, false, "E"),
        // Fujifilm
        Lens("xf18-55", "XF 18-55mm f/2.8-4 R LM OIS", 18, 55, 2.8, 4.0, true, "X"),
        Lens("xf16-80", "XF 16-80mm f/4 R OIS WR", 16, 80, 4.0, 4.0, true, "X"),
        Lens("xc15-45", "XC 15-45mm f/3.5-5.6 OIS PZ", 15, 45, 3.5, 5.6, true, "X"),
        Lens("xf10-24", "XF 10-24mm f/4 R OIS WR", 10, 24, 4.0, 4.0, true, "X"),
        Lens("xf70-300", "XF 70-300mm f/4-5.6 R LM OIS WR", 70, 300, 4.0, 5.6, true, "X"),
        Lens("xf100-400", "XF 100-400mm f/4.5-5.6 R LM OIS WR", 100, 400, 4.5, 5.6, true, "X"),
        Lens("xf23", "XF 23mm f/2 R WR", 23, 23, 2.0, 2.0, false, "X"),
        Lens("xf35", "XF 35mm f/1.4 R", 35, 35, 1.4, 1.4, false, "X"),
        Lens("xf56", "XF 56mm f/1.2 R WR", 56, 56, 1.2, 1.2, false, "X"),
        // Panasonic L / Micro Four Thirds / OM
        Lens("ls20-60", "Lumix S 20-60mm f/3.5-5.6", 20, 60, 3.5, 5.6, false, "L"),
        Lens("ls24-105", "Lumix S 24-105mm f/4 Macro O.I.S.", 24, 105, 4.0, 4.0, true, "L"),
        Lens("ls70-300", "Lumix S 70-300mm f/4.5-5.6 Macro O.I.S.", 70, 300, 4.5, 5.6, true, "L"),
        Lens("ls50", "Lumix S 50mm f/1.8", 50, 50, 1.8, 1.8, false, "L"),
        Lens("m12-60", "Lumix G 12-60mm f/3.5-5.6 O.I.S.", 12, 60, 3.5, 5.6, true, "MFT"),
        Lens("m14-42", "Lumix G 14-42mm f/3.5-5.6 (kit)", 14, 42, 3.5, 5.6, true, "MFT"),
        Lens("m45-150", "Lumix G 45-150mm f/4-5.6 O.I.S.", 45, 150, 4.0, 5.6, true, "MFT"),
        Lens("m100-400", "Leica DG 100-400mm f/4-6.3", 100, 400, 4.0, 6.3, true, "MFT"),
        Lens("m25", "Lumix G 25mm f/1.7", 25, 25, 1.7, 1.7, false, "MFT"),
        Lens("om12-45", "M.Zuiko 12-45mm f/4 PRO", 12, 45, 4.0, 4.0, false, "MFT"),
        Lens("om12-40", "M.Zuiko 12-40mm f/2.8 PRO", 12, 40, 2.8, 2.8, false, "MFT"),
        Lens("om14-150", "M.Zuiko 14-150mm f/4-5.6 II", 14, 150, 4.0, 5.6, false, "MFT"),
        Lens("om40-150", "M.Zuiko 40-150mm f/4-5.6 R", 40, 150, 4.0, 5.6, false, "MFT"),
        Lens("om100-400", "M.Zuiko 100-400mm f/5-6.3 IS", 100, 400, 5.0, 6.3, true, "MFT"),
        Lens("om45", "M.Zuiko 45mm f/1.8", 45, 45, 1.8, 1.8, false, "MFT"),
        // Any mount (third party / generic)
        Lens("samyang14", "Samyang/Rokinon 14mm f/2.8 (manual)", 14, 14, 2.8, 2.8, false, "any"),
        Lens("gen_kit", "Standard kit zoom 18-55mm f/3.5-5.6", 18, 55, 3.5, 5.6, true, "any"),
        Lens("gen_tele", "Telephoto zoom 70-300mm f/4.5-5.6", 70, 300, 4.5, 5.6, true, "any"),
        Lens("gen_super", "All-in-one zoom 18-200mm f/3.5-6.3", 18, 200, 3.5, 6.3, true, "any"),
        Lens("gen_std", "Standard zoom 24-105mm f/4", 24, 105, 4.0, 4.0, true, "any"),
        Lens("gen_50", "50mm f/1.8 prime", 50, 50, 1.8, 1.8, false, "any"),
        Lens("gen_mft", "Micro Four Thirds kit zoom 14-42mm f/3.5-5.6", 14, 42, 3.5, 5.6, true, "any"),
        // Built-in lenses of compact cameras
        Lens("g7x", "Built-in 8.8-36.8mm f/1.8-2.8 (24-100mm look)", 9, 37, 1.8, 2.8, true, "builtin:g7x"),
        Lens("g5x", "Built-in 8.8-44mm f/1.8-2.8 (24-120mm look)", 9, 44, 1.8, 2.8, true, "builtin:g5x"),
        Lens("rx100", "Built-in 9-72mm f/2.8-4.5 (24-200mm look)", 9, 72, 2.8, 4.5, true, "builtin:rx100"),
        Lens("x100", "Built-in 23mm f/2 (35mm look)", 23, 23, 2.0, 2.0, false, "builtin:x100"),
        Lens("tz200", "Built-in 8.8-132mm f/3.3-6.4 (24-360mm look)", 9, 132, 3.3, 6.4, true, "builtin:tz200"),
    )

    fun camera(id: String?): CameraBody? = cameras.firstOrNull { it.id == id }

    /** Which lens mounts fit a camera (including common adapters). */
    fun mountsFor(body: CameraBody?): Set<String> {
        if (body == null) return lenses.map { it.mount }.toSet()
        if (body.fixedLens != null) return setOf("builtin:${body.fixedLens}")
        return when (body.mount) {
            "RF" -> setOf("RF", "RF-S", "EF", "EF-S", "any")
            "EF-M" -> setOf("EF-M", "EF", "EF-S", "any")
            "EF" -> setOf("EF", "any")
            "EF-S" -> setOf("EF", "EF-S", "any")
            "Z" -> setOf("Z", "F", "F-DX", "any")
            "F" -> setOf("F", "any")
            "F-DX" -> setOf("F", "F-DX", "any")
            "FE" -> setOf("FE", "E", "any")
            "E" -> setOf("FE", "E", "any")
            "X" -> setOf("X", "any")
            "L" -> setOf("L", "any")
            "MFT" -> setOf("MFT", "any")
            else -> setOf("any")
        }
    }

    fun lensesFor(body: CameraBody?): List<Lens> {
        val m = mountsFor(body)
        return lenses.filter { it.mount in m }
    }
}
