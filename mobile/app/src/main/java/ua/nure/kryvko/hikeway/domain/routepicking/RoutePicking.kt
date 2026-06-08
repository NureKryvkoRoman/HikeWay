package ua.nure.kryvko.hikeway.domain.routepicking

import kotlin.math.atan2
import kotlinx.coroutines.flow.Flow
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route

enum class RoutePickingStatus {
    ACTIVE,
    PAUSED,
}

data class RouteProgress(
    val position: GeoPoint,
    val bearingDegrees: Double,
    val pointIndex: Int,
)

data class RoutePickingSession(
    val route: Route,
    val userPosition: GeoPoint,
    val bearingDegrees: Double,
    val pointIndex: Int,
    val status: RoutePickingStatus,
    val walkedPath: List<GeoPoint>,
    val walkedDistanceKm: Double,
    val activeElapsedMillis: Long,
    val startedAtEpochMillis: Long,
)

interface RouteTrackingProvider {
    fun positions(route: Route, startIndex: Int = 0): Flow<RouteProgress>
}

fun initialProgress(route: Route): RouteProgress? {
    val points = route.geometry.points
    val firstPoint = points.firstOrNull() ?: return null
    return RouteProgress(
        position = firstPoint,
        bearingDegrees = bearingDegrees(firstPoint, points.getOrNull(1) ?: firstPoint),
        pointIndex = 0,
    )
}

fun bearingDegrees(from: GeoPoint, to: GeoPoint): Double {
    if (from == to) return 0.0

    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
    val y = kotlin.math.sin(longitudeDelta) * kotlin.math.cos(toLatitude)
    val x = kotlin.math.cos(fromLatitude) * kotlin.math.sin(toLatitude) -
        kotlin.math.sin(fromLatitude) * kotlin.math.cos(toLatitude) * kotlin.math.cos(longitudeDelta)

    return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
}
