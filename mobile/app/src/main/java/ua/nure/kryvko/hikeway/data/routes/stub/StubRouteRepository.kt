package ua.nure.kryvko.hikeway.data.routes.stub

import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.distanceKm

class StubRouteRepository(
    private val routes: List<Route> = stubRoutes,
) : RouteRepository {
    override suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route> {
        return routes.filter { route ->
            criteria.distanceKm?.contains(route.distanceKm) != false &&
                criteria.estimatedTimeMinutes?.contains(route.estimatedTimeMinutes) != false &&
                (criteria.difficulties.isEmpty() || route.difficulty in criteria.difficulties) &&
                (criteria.terrains.isEmpty() || route.terrain in criteria.terrains) &&
                criteria.maxProximityKm?.let { maximum ->
                    route.geometry.points.firstOrNull()?.let { distanceKm(origin, it) <= maximum }
                        ?: false
                } != false
        }
    }
}

private fun route(
    id: Long,
    name: String,
    description: String,
    distanceKm: Double,
    estimatedTimeMinutes: Int,
    difficulty: Difficulty,
    elevationGainMeters: Int,
    terrain: Terrain,
    vararg points: Pair<Double, Double>,
) = Route(
    id = id,
    name = name,
    description = description,
    distanceKm = distanceKm,
    estimatedTimeMinutes = estimatedTimeMinutes,
    difficulty = difficulty,
    elevationGainMeters = elevationGainMeters,
    terrain = terrain,
    geometry = RouteGeometry(points.map { (longitude, latitude) -> GeoPoint(longitude, latitude) }),
)

val stubRoutes = listOf(
    route(
        1, "High Castle Loop", "A short city-edge climb with a panoramic viewpoint.",
        4.8, 95, Difficulty.EASY, 140, Terrain.FOREST,
        24.0316 to 49.8429, 24.0394 to 49.8461, 24.0438 to 49.8488,
    ),
    route(
        2, "Vynnyky Forest Trail", "A rolling forest route with quiet paths and several lakes.",
        11.2, 210, Difficulty.MEDIUM, 310, Terrain.FOREST,
        24.1290 to 49.8170, 24.1440 to 49.8105, 24.1570 to 49.8030,
    ),
    route(
        3, "Rocky Ridge Traverse", "A longer exposed ridge route for experienced hikers.",
        18.6, 390, Difficulty.HARD, 920, Terrain.ROCKY,
        23.8910 to 49.6510, 23.9140 to 49.6600, 23.9400 to 49.6560,
    ),
    route(
        4, "Mountain Meadow Path", "A steady climb through meadows and mixed woodland.",
        8.4, 175, Difficulty.MEDIUM, 460, Terrain.MOUNTAIN,
        23.9870 to 49.7310, 24.0020 to 49.7260, 24.0150 to 49.7190,
    ),
    route(
        5, "Lakeside Mixed Walk", "An accessible mixed-terrain route for a relaxed afternoon.",
        6.1, 120, Difficulty.EASY, 90, Terrain.MIXED,
        24.0710 to 49.8680, 24.0800 to 49.8710, 24.0920 to 49.8660,
    ),
)
