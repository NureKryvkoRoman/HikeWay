package ua.nure.kryvko.hikeway.data.routes.remote

import com.google.gson.Gson
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.data.services.backend.RouteService
import ua.nure.kryvko.hikeway.data.services.network.toApiException
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria

private const val DEFAULT_PAGE_SIZE = 50

class RemoteRouteRepository(
    private val service: RouteService,
    private val gson: Gson,
) : RouteRepository {
    override suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route> {
        return runCatching {
            service.search(
                minDistanceKm = criteria.distanceKm?.start,
                maxDistanceKm = criteria.distanceKm?.endInclusive,
                minEstimatedTimeMinutes = criteria.estimatedTimeMinutes?.first,
                maxEstimatedTimeMinutes = criteria.estimatedTimeMinutes?.last,
                difficulties = criteria.difficulties.map { it.name }.sorted().takeIf { it.isNotEmpty() },
                terrains = criteria.terrains.map { it.name }.sorted().takeIf { it.isNotEmpty() },
                longitude = origin.longitude,
                latitude = origin.latitude,
                maxProximityKm = criteria.maxProximityKm,
                size = DEFAULT_PAGE_SIZE,
            ).items.map { it.toDomain() }
        }.getOrElse {
            throw it.toApiException(gson, "Routes are unavailable.")
        }
    }
}
