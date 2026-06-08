package ua.nure.kryvko.hikeway.data.routepicking.stub

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.routepicking.RouteProgress
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routepicking.bearingDegrees

class StubRouteTrackingProvider(
    private val stepDelayMillis: Long = 1_000L,
) : RouteTrackingProvider {
    override fun positions(route: Route, startIndex: Int): Flow<RouteProgress> = flow {
        val points = route.geometry.points
        if (points.isEmpty()) return@flow

        val firstIndex = startIndex.coerceIn(points.indices)
        for (index in firstIndex..points.lastIndex) {
            val current = points[index]
            val next = points.getOrNull(index + 1) ?: current
            emit(
                RouteProgress(
                    position = current,
                    bearingDegrees = bearingDegrees(current, next),
                    pointIndex = index,
                )
            )
            if (index < points.lastIndex) {
                delay(stepDelayMillis)
            }
        }
    }
}
