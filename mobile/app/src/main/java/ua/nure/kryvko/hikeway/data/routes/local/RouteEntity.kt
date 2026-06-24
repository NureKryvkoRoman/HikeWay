package ua.nure.kryvko.hikeway.data.routes.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routes",
    indices = [Index(value = ["ownerUserId", "clientId"], unique = true)],
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ownerUserId: String,
    val name: String,
    val description: String,
    val distanceKm: Double,
    val estimatedTimeMinutes: Int,
    val difficulty: String,
    val elevationGainMeters: Int,
    val terrain: String,
    val geometryGeoJson: String,
    val clientId: String,
    val serverId: Long? = null,
    val syncVersion: Long = 0,
    val updatedAtEpochMillis: Long,
    val syncState: String = "PENDING",
    val deleted: Boolean = false,
)
