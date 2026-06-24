package ua.nure.kryvko.hikeway.data.sync

import java.time.Instant
import ua.nure.kryvko.hikeway.data.geojson.GeoJsonLineStringCodec
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeLogDao
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeLogEntity
import ua.nure.kryvko.hikeway.data.routes.local.RouteDao
import ua.nure.kryvko.hikeway.data.routes.local.RouteEntity
import ua.nure.kryvko.hikeway.domain.auth.CurrentUserProvider

class RoomSyncLocalDataSource(
    private val routeDao: RouteDao,
    private val hikeLogDao: HikeLogDao,
    private val conflictDao: SyncConflictDao,
    private val currentUserProvider: CurrentUserProvider,
) : SyncLocalDataSource {
    override suspend fun createRequest(cursor: String?, deviceId: String): SyncRequestDto {
        val ownerUserId = currentUserProvider.requireCurrentUserId()
        val routes = routeDao.getPending(ownerUserId).take(MAX_MUTATIONS)
        val hikes = hikeLogDao.getPending(ownerUserId)
            .take(MAX_MUTATIONS - routes.size)
        return SyncRequestDto(
            cursor = cursor,
            deviceId = deviceId,
            routeMutations = routes.map(RouteEntity::toMutation),
            hikeMutations = hikes.map(HikeLogEntity::toMutation),
        )
    }

    override suspend fun apply(response: SyncResponseDto) {
        val ownerUserId = currentUserProvider.requireCurrentUserId()
        val conflictedRoutes = response.conflicts
            .filter { it.resourceType == SyncResourceType.ROUTE }
            .mapTo(mutableSetOf(), SyncConflictDto::clientId)
        val conflictedHikes = response.conflicts
            .filter { it.resourceType == SyncResourceType.HIKE }
            .mapTo(mutableSetOf(), SyncConflictDto::clientId)
        response.accepted.forEach { accepted ->
            conflictDao.delete(ownerUserId, accepted.resourceType.name, accepted.clientId)
            when (accepted.resourceType) {
                SyncResourceType.ROUTE -> routeDao.findByClientId(ownerUserId, accepted.clientId)
                    ?.let {
                        routeDao.update(
                            it.copy(
                                serverId = accepted.serverId,
                                syncVersion = accepted.version,
                                syncState = SyncState.SYNCED.name,
                                deleted = accepted.operation == SyncOperation.DELETE,
                            )
                        )
                    }
                SyncResourceType.HIKE -> hikeLogDao.findByClientId(ownerUserId, accepted.clientId)
                    ?.let {
                        hikeLogDao.update(
                            it.copy(
                                serverId = accepted.serverId,
                                syncVersion = accepted.version,
                                syncState = SyncState.SYNCED.name,
                                deleted = accepted.operation == SyncOperation.DELETE,
                            )
                        )
                    }
            }
        }
        response.conflicts.forEach { conflict ->
            conflictDao.upsert(
                SyncConflictEntity(
                    ownerUserId = ownerUserId,
                    resourceType = conflict.resourceType.name,
                    clientId = conflict.clientId,
                    submittedBaseVersion = conflict.submittedBaseVersion,
                    serverVersion = conflict.serverVersion,
                    serverRecordJson = conflict.serverRecord.toString(),
                    createdAtEpochMillis = System.currentTimeMillis(),
                )
            )
            when (conflict.resourceType) {
                SyncResourceType.ROUTE -> routeDao.findByClientId(ownerUserId, conflict.clientId)
                    ?.let { routeDao.update(it.copy(syncState = SyncState.CONFLICT.name)) }
                SyncResourceType.HIKE -> hikeLogDao.findByClientId(ownerUserId, conflict.clientId)
                    ?.let { hikeLogDao.update(it.copy(syncState = SyncState.CONFLICT.name)) }
            }
        }
        response.routeChanges
            .filterNot { it.clientId in conflictedRoutes }
            .forEach { applyRouteChange(ownerUserId, it) }
        response.hikeChanges
            .filterNot { it.clientId in conflictedHikes }
            .forEach { applyHikeChange(ownerUserId, it) }
    }

    private suspend fun applyRouteChange(ownerUserId: String, change: RouteChangeDto) {
        val existing = routeDao.findByClientId(ownerUserId, change.clientId)
        if (change.deleted) {
            existing?.let {
                routeDao.update(
                    it.copy(
                        serverId = change.serverId,
                        syncVersion = change.version,
                        updatedAtEpochMillis = change.updatedAt.toEpochMillis(),
                        syncState = SyncState.SYNCED.name,
                        deleted = true,
                    )
                )
            }
            return
        }
        val entity = RouteEntity(
            id = existing?.id ?: 0,
            ownerUserId = ownerUserId,
            name = requireNotNull(change.name),
            description = requireNotNull(change.description),
            distanceKm = requireNotNull(change.distanceKm),
            estimatedTimeMinutes = requireNotNull(change.estimatedTimeMinutes),
            difficulty = requireNotNull(change.difficulty),
            elevationGainMeters = requireNotNull(change.elevationGainMeters),
            terrain = requireNotNull(change.terrain),
            geometryGeoJson = requireNotNull(change.geometry).toGeoJson(),
            clientId = change.clientId,
            serverId = change.serverId,
            syncVersion = change.version,
            updatedAtEpochMillis = change.updatedAt.toEpochMillis(),
            syncState = SyncState.SYNCED.name,
            deleted = false,
        )
        routeDao.upsert(entity)
    }

    private suspend fun applyHikeChange(ownerUserId: String, change: HikeChangeDto) {
        val existing = hikeLogDao.findByClientId(ownerUserId, change.clientId)
        if (change.deleted) {
            existing?.let {
                hikeLogDao.update(
                    it.copy(
                        serverId = change.serverId,
                        syncVersion = change.version,
                        updatedAtEpochMillis = change.updatedAt.toEpochMillis(),
                        syncState = SyncState.SYNCED.name,
                        deleted = true,
                    )
                )
            }
            return
        }
        val entity = HikeLogEntity(
            id = existing?.id ?: 0,
            ownerUserId = ownerUserId,
            routeId = existing?.routeId ?: change.routeServerId ?: 0,
            routeName = requireNotNull(change.routeName),
            startedAtEpochMillis = requireNotNull(change.startedAt).toEpochMillis(),
            finishedAtEpochMillis = requireNotNull(change.finishedAt).toEpochMillis(),
            activeDurationMillis = requireNotNull(change.activeDurationMillis),
            wallClockDurationMillis = requireNotNull(change.wallClockDurationMillis),
            totalDistanceKm = requireNotNull(change.totalDistanceKm),
            pathGeoJson = requireNotNull(change.path).toGeoJson(),
            clientId = change.clientId,
            serverId = change.serverId,
            routeClientId = change.routeClientId,
            routeServerId = change.routeServerId,
            syncVersion = change.version,
            updatedAtEpochMillis = change.updatedAt.toEpochMillis(),
            syncState = SyncState.SYNCED.name,
            deleted = false,
        )
        hikeLogDao.upsert(entity)
    }
}

private fun RouteEntity.toMutation() = SyncMutationDto(
    operation = if (deleted) SyncOperation.DELETE else SyncOperation.UPSERT,
    clientId = clientId,
    baseVersion = syncVersion,
    payload = if (deleted) null else RouteSyncPayloadDto(
        name = name,
        description = description,
        distanceKm = distanceKm,
        estimatedTimeMinutes = estimatedTimeMinutes,
        difficulty = difficulty,
        elevationGainMeters = elevationGainMeters,
        terrain = terrain,
        geometry = geometryGeoJson.toGeoJsonDto(),
    ),
)

private fun HikeLogEntity.toMutation() = SyncMutationDto(
    operation = if (deleted) SyncOperation.DELETE else SyncOperation.UPSERT,
    clientId = clientId,
    baseVersion = syncVersion,
    payload = if (deleted) null else HikeSyncPayloadDto(
        routeClientId = routeClientId,
        routeServerId = routeServerId,
        routeName = routeName,
        startedAt = Instant.ofEpochMilli(startedAtEpochMillis).toString(),
        finishedAt = Instant.ofEpochMilli(finishedAtEpochMillis).toString(),
        activeDurationMillis = activeDurationMillis,
        wallClockDurationMillis = wallClockDurationMillis,
        totalDistanceKm = totalDistanceKm,
        path = pathGeoJson.toGeoJsonDto(),
    ),
)

private fun String.toGeoJsonDto(): GeoJsonLineStringDto {
    return GeoJsonLineStringDto(
        coordinates = GeoJsonLineStringCodec.decode(this)
            .map { listOf(it.longitude, it.latitude) },
    )
}

private fun GeoJsonLineStringDto.toGeoJson(): String {
    return GeoJsonLineStringCodec.encode(
        coordinates.map { coordinate ->
            ua.nure.kryvko.hikeway.core.model.GeoPoint(
                longitude = coordinate[0],
                latitude = coordinate[1],
            )
        }
    )
}

private fun String.toEpochMillis(): Long = Instant.parse(this).toEpochMilli()

private const val MAX_MUTATIONS = 100
