package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hike_logs",
    indices = [Index(value = ["ownerUserId", "clientId"], unique = true)],
)
data class HikeLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUserId: String,
    val routeId: Long,
    val routeName: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val activeDurationMillis: Long,
    val wallClockDurationMillis: Long,
    val totalDistanceKm: Double,
    val pathGeoJson: String,
    val clientId: String,
    val serverId: Long? = null,
    val routeClientId: String? = null,
    val routeServerId: Long? = null,
    val syncVersion: Long = 0,
    val updatedAtEpochMillis: Long,
    val syncState: String = "PENDING",
    val deleted: Boolean = false,
)
