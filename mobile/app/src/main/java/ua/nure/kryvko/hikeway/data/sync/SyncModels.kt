package ua.nure.kryvko.hikeway.data.sync

import com.google.gson.JsonObject

enum class SyncState {
    PENDING,
    SYNCED,
    CONFLICT,
}

enum class SyncOperation {
    UPSERT,
    DELETE,
}

enum class SyncResourceType {
    ROUTE,
    HIKE,
}

data class GeoJsonLineStringDto(
    val type: String = "LineString",
    val coordinates: List<List<Double>>,
)

data class RouteSyncPayloadDto(
    val name: String,
    val description: String,
    val distanceKm: Double,
    val estimatedTimeMinutes: Int,
    val difficulty: String,
    val elevationGainMeters: Int,
    val terrain: String,
    val geometry: GeoJsonLineStringDto,
)

data class HikeSyncPayloadDto(
    val routeClientId: String?,
    val routeServerId: Long?,
    val routeName: String,
    val startedAt: String,
    val finishedAt: String,
    val activeDurationMillis: Long,
    val wallClockDurationMillis: Long,
    val totalDistanceKm: Double,
    val path: GeoJsonLineStringDto,
)

data class SyncMutationDto<T>(
    val operation: SyncOperation,
    val clientId: String,
    val baseVersion: Long,
    val payload: T? = null,
)

data class SyncRequestDto(
    val cursor: String?,
    val deviceId: String,
    val routeMutations: List<SyncMutationDto<RouteSyncPayloadDto>>,
    val hikeMutations: List<SyncMutationDto<HikeSyncPayloadDto>>,
)

data class AcceptedMutationDto(
    val resourceType: SyncResourceType,
    val clientId: String,
    val serverId: Long,
    val version: Long,
    val operation: SyncOperation,
)

data class SyncConflictDto(
    val resourceType: SyncResourceType,
    val clientId: String,
    val submittedBaseVersion: Long,
    val serverVersion: Long,
    val serverRecord: JsonObject,
)

data class RouteChangeDto(
    val clientId: String,
    val serverId: Long,
    val version: Long,
    val name: String?,
    val description: String?,
    val distanceKm: Double?,
    val estimatedTimeMinutes: Int?,
    val difficulty: String?,
    val elevationGainMeters: Int?,
    val terrain: String?,
    val geometry: GeoJsonLineStringDto?,
    val updatedAt: String,
    val deleted: Boolean,
)

data class HikeChangeDto(
    val clientId: String,
    val serverId: Long,
    val version: Long,
    val routeClientId: String?,
    val routeServerId: Long?,
    val routeName: String?,
    val startedAt: String?,
    val finishedAt: String?,
    val activeDurationMillis: Long?,
    val wallClockDurationMillis: Long?,
    val totalDistanceKm: Double?,
    val path: GeoJsonLineStringDto?,
    val updatedAt: String,
    val deleted: Boolean,
)

data class SyncResponseDto(
    val cursor: String,
    val hasMore: Boolean,
    val accepted: List<AcceptedMutationDto>,
    val conflicts: List<SyncConflictDto>,
    val routeChanges: List<RouteChangeDto>,
    val hikeChanges: List<HikeChangeDto>,
)
