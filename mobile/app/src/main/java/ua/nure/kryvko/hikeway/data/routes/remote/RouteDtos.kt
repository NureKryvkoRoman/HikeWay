package ua.nure.kryvko.hikeway.data.routes.remote

import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain

data class PageResponseDto<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class GeoJsonLineStringDto(
    val type: String,
    val coordinates: List<List<Double>>,
)

data class RouteSummaryDto(
    val id: Long,
    val name: String,
    val description: String,
    val distanceKm: Double,
    val estimatedTimeMinutes: Int,
    val difficulty: String,
    val elevationGain: Int,
    val terrain: String,
    val geometry: GeoJsonLineStringDto,
)

fun RouteSummaryDto.toDomain(): Route = Route(
    id = id,
    name = name,
    description = description,
    distanceKm = distanceKm,
    estimatedTimeMinutes = estimatedTimeMinutes,
    difficulty = Difficulty.valueOf(difficulty),
    elevationGainMeters = elevationGain,
    terrain = Terrain.valueOf(terrain),
    geometry = RouteGeometry(
        geometry.coordinates.mapNotNull { coordinate ->
            if (coordinate.size < 2) {
                null
            } else {
                GeoPoint(
                    longitude = coordinate[0],
                    latitude = coordinate[1],
                )
            }
        }
    ),
)
