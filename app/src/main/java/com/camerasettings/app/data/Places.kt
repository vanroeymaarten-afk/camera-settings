package com.camerasettings.app.data

data class Place(
    val id: String,
    val name: String,
    val region: String,
    val lat: Double,
    val lon: Double,
    val zone: String = "America/Santiago",
)

object Places {
    const val GPS_ID = "gps"

    val chile: List<Place> = listOf(
        Place("santiago", "Santiago", "Central Chile", -33.45, -70.67),
        Place("valparaiso", "Valparaíso", "Central Chile", -33.05, -71.62),
        Place("elqui", "Valle del Elqui (dark-sky area)", "Norte Chico", -30.12, -70.49),
        Place("sanpedro", "San Pedro de Atacama", "Atacama", -22.91, -68.20),
        Place("valleluna", "Valle de la Luna", "Atacama", -22.93, -68.25),
        Place("eltatio", "El Tatio geysers", "Atacama", -22.33, -68.01),
        Place("chaxa", "Laguna Chaxa / Salar de Atacama", "Atacama", -23.29, -68.18),
        Place("pucon", "Pucón / Villarrica", "Lake District", -39.27, -71.98),
        Place("puertovaras", "Puerto Varas / Petrohué", "Lake District", -41.32, -72.98),
        Place("chiloe", "Castro, Chiloé", "Lake District", -42.48, -73.76),
        Place("coyhaique", "Coyhaique / Carretera Austral", "Aysén", -45.57, -72.07),
        Place("torres", "Torres del Paine", "Patagonia", -50.94, -73.41, "America/Punta_Arenas"),
        Place("natales", "Puerto Natales", "Patagonia", -51.73, -72.49, "America/Punta_Arenas"),
        Place("puntaarenas", "Punta Arenas / penguins", "Patagonia", -53.16, -70.91, "America/Punta_Arenas"),
        Place("rapanui", "Easter Island (Rapa Nui)", "Pacific", -27.15, -109.43, "Pacific/Easter"),
    )

    fun byId(id: String?): Place? = chile.firstOrNull { it.id == id }
}
