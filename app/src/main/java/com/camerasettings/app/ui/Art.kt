package com.camerasettings.app.ui

import androidx.annotation.DrawableRes
import com.camerasettings.app.R

/**
 * The painted example illustrations (see tools/art). Referenced explicitly so the
 * resource shrinker keeps them.
 */
object Art {
    data class Set(@DrawableRes val thumb: Int, @DrawableRes val before: Int, @DrawableRes val after: Int)

    private val sets = mapOf(
        "valle_luna" to Set(R.drawable.thumb_valle_luna, R.drawable.ex_valle_luna_before, R.drawable.ex_valle_luna_after),
        "milky_way" to Set(R.drawable.thumb_milky_way, R.drawable.ex_milky_way_before, R.drawable.ex_milky_way_after),
        "el_tatio" to Set(R.drawable.thumb_el_tatio, R.drawable.ex_el_tatio_before, R.drawable.ex_el_tatio_after),
        "flamingos" to Set(R.drawable.thumb_flamingos, R.drawable.ex_flamingos_before, R.drawable.ex_flamingos_after),
        "altiplano" to Set(R.drawable.thumb_altiplano, R.drawable.ex_altiplano_before, R.drawable.ex_altiplano_after),
        "santiago" to Set(R.drawable.thumb_santiago, R.drawable.ex_santiago_before, R.drawable.ex_santiago_after),
        "valparaiso" to Set(R.drawable.thumb_valparaiso, R.drawable.ex_valparaiso_before, R.drawable.ex_valparaiso_after),
        "condor" to Set(R.drawable.thumb_condor, R.drawable.ex_condor_before, R.drawable.ex_condor_after),
        "osorno" to Set(R.drawable.thumb_osorno, R.drawable.ex_osorno_before, R.drawable.ex_osorno_after),
        "petrohue" to Set(R.drawable.thumb_petrohue, R.drawable.ex_petrohue_before, R.drawable.ex_petrohue_after),
        "palafitos" to Set(R.drawable.thumb_palafitos, R.drawable.ex_palafitos_before, R.drawable.ex_palafitos_after),
        "forest" to Set(R.drawable.thumb_forest, R.drawable.ex_forest_before, R.drawable.ex_forest_after),
        "penguins" to Set(R.drawable.thumb_penguins, R.drawable.ex_penguins_before, R.drawable.ex_penguins_after),
    )

    operator fun get(id: String?): Set? = sets[id]
    val ids: kotlin.collections.Set<String> get() = sets.keys
}
