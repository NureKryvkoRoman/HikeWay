package ua.nure.kryvko.hikeway.core.model

data class GeoPoint(
    val longitude: Double,
    val latitude: Double,
)

data class RouteGeometry(
    val points: List<GeoPoint>,
)

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD,
}

enum class Terrain {
    FOREST,
    MOUNTAIN,
    ROCKY,
    MIXED,
}

data class Route(
    val id: Long,
    val name: String,
    val description: String,
    val distanceKm: Double,
    val estimatedTimeMinutes: Int,
    val difficulty: Difficulty,
    val elevationGainMeters: Int,
    val terrain: Terrain,
    val geometry: RouteGeometry,
)
