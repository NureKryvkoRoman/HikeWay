package ua.nure.kryvko.hikeway.domain.routes

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import ua.nure.kryvko.hikeway.core.location.LocationProvider
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain

data class RouteSearchCriteria(
    val distanceKm: ClosedFloatingPointRange<Double>? = null,
    val estimatedTimeMinutes: IntRange? = null,
    val difficulties: Set<Difficulty> = emptySet(),
    val terrains: Set<Terrain> = emptySet(),
    val maxProximityKm: Double? = null,
)

interface RouteRepository {
    suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route>
}

interface CustomRouteRepository {
    suspend fun save(route: Route): Long
}

class SearchRoutesUseCase(
    private val repository: RouteRepository,
    private val locationProvider: LocationProvider,
) {
    suspend operator fun invoke(criteria: RouteSearchCriteria): List<Route> {
        return repository.search(criteria, locationProvider.getCurrentLocation())
    }
}

class GetCurrentLocationUseCase(
    private val locationProvider: LocationProvider,
) {
    suspend operator fun invoke(): GeoPoint {
        return locationProvider.getCurrentLocation()
    }
}

class SaveCustomRouteUseCase(
    private val repository: CustomRouteRepository,
) {
    suspend operator fun invoke(route: Route): Long = repository.save(route)
}

fun Route.matches(criteria: RouteSearchCriteria, origin: GeoPoint): Boolean {
    return criteria.distanceKm?.contains(distanceKm) != false &&
        criteria.estimatedTimeMinutes?.contains(estimatedTimeMinutes) != false &&
        (criteria.difficulties.isEmpty() || difficulty in criteria.difficulties) &&
        (criteria.terrains.isEmpty() || terrain in criteria.terrains) &&
        criteria.maxProximityKm?.let { maximum ->
            geometry.points.firstOrNull()?.let { distanceKm(origin, it) <= maximum } ?: false
        } != false
}

fun distanceKm(from: GeoPoint, to: GeoPoint): Double {
    val earthRadiusKm = 6371.0
    val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val haversine = sin(latitudeDelta / 2).pow(2) +
        cos(fromLatitude) * cos(toLatitude) * sin(longitudeDelta / 2).pow(2)
    return earthRadiusKm * 2 * asin(sqrt(haversine))
}
