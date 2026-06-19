package ua.nure.kryvko.hikeway.data.routes

import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria

class CompositeRouteRepository(
    private val repositories: List<RouteRepository>,
) : RouteRepository {
    override suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route> {
        return repositories.flatMap { it.search(criteria, origin) }
    }
}
