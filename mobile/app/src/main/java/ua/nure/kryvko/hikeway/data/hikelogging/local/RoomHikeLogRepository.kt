package ua.nure.kryvko.hikeway.data.hikelogging.local

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ua.nure.kryvko.hikeway.data.geojson.GeoJsonLineStringCodec
import ua.nure.kryvko.hikeway.domain.auth.CurrentUserProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository

class RoomHikeLogRepository(
    private val dao: HikeLogDao,
    private val currentUserProvider: CurrentUserProvider,
) : HikeLogRepository {
    override suspend fun save(log: HikeLog): Long {
        return dao.insert(log.toEntity(currentUserProvider.requireCurrentUserId()))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<HikeLog>> {
        return currentUserProvider.currentUserId.flatMapLatest { ownerUserId ->
            if (ownerUserId == null) {
                flowOf(emptyList())
            } else {
                dao.observeAll(ownerUserId).map { logs -> logs.map { it.toDomain() } }
            }
        }
    }
}

fun HikeLog.toEntity(ownerUserId: String) = HikeLogEntity(
    id = id,
    ownerUserId = ownerUserId,
    routeId = routeId,
    routeName = routeName,
    startedAtEpochMillis = startedAtEpochMillis,
    finishedAtEpochMillis = finishedAtEpochMillis,
    activeDurationMillis = activeDurationMillis,
    wallClockDurationMillis = wallClockDurationMillis,
    totalDistanceKm = totalDistanceKm,
    pathGeoJson = GeoJsonLineStringCodec.encode(path),
)

fun HikeLogEntity.toDomain() = HikeLog(
    id = id,
    routeId = routeId,
    routeName = routeName,
    startedAtEpochMillis = startedAtEpochMillis,
    finishedAtEpochMillis = finishedAtEpochMillis,
    activeDurationMillis = activeDurationMillis,
    wallClockDurationMillis = wallClockDurationMillis,
    totalDistanceKm = totalDistanceKm,
    path = GeoJsonLineStringCodec.decode(pathGeoJson),
)
