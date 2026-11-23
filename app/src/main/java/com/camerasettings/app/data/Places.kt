package com.camerasettings.app.data

enum class Region(val title: String, val blurb: String) {
    ATACAMA("Atacama Desert", "San Pedro, salt flats, lagoons, geysers and the clearest night skies on Earth"),
    SANTIAGO("Santiago & Valparaíso", "The capital under the Andes, condors in the Maipo canyon, the colourful port"),
    LAKES("Puerto Montt & the Lakes", "Volcanoes over lakes, turquoise waterfalls, Chiloé's stilt houses and rainforest"),
}

data class Place(
    val id: String,
    val name: String,
    val region: Region?,
    val lat: Double,
    val lon: Double,
    val zone: String = "America/Santiago",
    val note: String = "",
)

object Places {
    const val GPS_ID = "gps"

    val all: List<Place> = listOf(
        // Atacama
        Place("sanpedro", "San Pedro de Atacama", Region.ATACAMA, -22.91, -68.20, note = "Your base. 2,400 m altitude."),
        Place("valleluna", "Valle de la Luna", Region.ATACAMA, -22.93, -68.27, note = "Sunset from Mirador de Kari / Piedra del Coyote."),
        Place("eltatio", "El Tatio geysers", Region.ATACAMA, -22.33, -68.01, note = "4,300 m. Best just before sunrise. Very cold."),
        Place("chaxa", "Laguna Chaxa (flamingos)", Region.ATACAMA, -23.29, -68.18, note = "Inside the Salar de Atacama."),
        Place("miscanti", "Lagunas Miscanti & Miñiques", Region.ATACAMA, -23.74, -67.77, note = "4,100 m. Deep blue lagoons."),
        Place("piedrasrojas", "Piedras Rojas", Region.ATACAMA, -23.97, -67.70, note = "Red rocks by a turquoise salt lake."),
        // Santiago & central Chile
        Place("santiago", "Santiago centre", Region.SANTIAGO, -33.44, -70.65),
        Place("sancristobal", "Cerro San Cristóbal (view point)", Region.SANTIAGO, -33.42, -70.63, note = "Skyline with the Andes behind."),
        Place("maipo", "Cajón del Maipo (condors)", Region.SANTIAGO, -33.64, -70.35, note = "Canyon 1.5 h south-east of Santiago."),
        Place("valparaiso", "Valparaíso", Region.SANTIAGO, -33.05, -71.62, note = "Day trip, 1.5 h from Santiago."),
        // Puerto Montt & Lake District
        Place("puertomontt", "Puerto Montt / Angelmó", Region.LAKES, -41.48, -72.94),
        Place("puertovaras", "Puerto Varas", Region.LAKES, -41.32, -72.98, note = "Osorno across Lake Llanquihue."),
        Place("frutillar", "Frutillar", Region.LAKES, -41.12, -73.05, note = "Classic Osorno sunset spot from the pier."),
        Place("petrohue", "Saltos del Petrohué", Region.LAKES, -41.16, -72.41, note = "Turquoise falls below Osorno."),
        Place("alerce", "Alerce Andino rainforest", Region.LAKES, -41.53, -72.62),
        Place("castro", "Castro, Chiloé (palafitos)", Region.LAKES, -42.48, -73.76),
        Place("punihuil", "Puñihuil penguins (Chiloé)", Region.LAKES, -41.92, -74.04, note = "Boat trips Sept–March, morning best."),
    )

    fun byId(id: String?): Place? = all.firstOrNull { it.id == id }
}
