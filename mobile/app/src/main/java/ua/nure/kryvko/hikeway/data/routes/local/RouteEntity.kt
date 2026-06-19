package ua.nure.kryvko.hikeway.data.routes.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String,
    val distanceKm: Double,
    val estimatedTimeMinutes: Int,
    val difficulty: String,
    val elevationGainMeters: Int,
    val terrain: String,
    val geometryGeoJson: String,
)
