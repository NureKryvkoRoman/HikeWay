package ua.nure.kryvko.hikeway.data.routes.local

import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.data.geojson.GeoJsonLineStringCodec
import ua.nure.kryvko.hikeway.domain.auth.CurrentUserProvider
import ua.nure.kryvko.hikeway.domain.routes.CustomRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.matches
import java.util.UUID

class RoomRouteRepository(
    private val dao: RouteDao,
    private val currentUserProvider: CurrentUserProvider,
    private val onLocalMutation: suspend () -> Unit = {},
) : RouteRepository, CustomRouteRepository {
    override suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route> {
        val ownerUserId = currentUserProvider.currentUserId.value ?: return emptyList()
        return dao.getAll(ownerUserId)
            .map { it.toDomain() }
            .filter { it.matches(criteria, origin) }
    }

    override suspend fun save(route: Route): Long {
        return dao.insert(route.toEntity(currentUserProvider.requireCurrentUserId()))
            .also { runCatching { onLocalMutation() } }
    }
}

fun Route.toEntity(
    ownerUserId: String,
    clientId: String = UUID.randomUUID().toString(),
    updatedAtEpochMillis: Long = System.currentTimeMillis(),
) = RouteEntity(
    id = if (id < 0) 0 else id,
    ownerUserId = ownerUserId,
    name = name,
    description = description,
    distanceKm = distanceKm,
    estimatedTimeMinutes = estimatedTimeMinutes,
    difficulty = difficulty.name,
    elevationGainMeters = elevationGainMeters,
    terrain = terrain.name,
    geometryGeoJson = GeoJsonLineStringCodec.encode(geometry.points),
    clientId = clientId,
    updatedAtEpochMillis = updatedAtEpochMillis,
)

fun RouteEntity.toDomain() = Route(
    id = id,
    name = name,
    description = description,
    distanceKm = distanceKm,
    estimatedTimeMinutes = estimatedTimeMinutes,
    difficulty = Difficulty.valueOf(difficulty),
    elevationGainMeters = elevationGainMeters,
    terrain = Terrain.valueOf(terrain),
    geometry = RouteGeometry(GeoJsonLineStringCodec.decode(geometryGeoJson)),
)
