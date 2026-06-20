package ua.nure.kryvko.hikeway.data.hikelogging.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hike_logs")
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
)
