package ua.nure.kryvko.hikeway.domain.routepicking

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider

class RoutePickingTest {
    @Test
    fun bearingCalculationHandlesCardinalDirections() {
        val origin = GeoPoint(longitude = 0.0, latitude = 0.0)

        assertEquals(0.0, bearingDegrees(origin, GeoPoint(0.0, 1.0)), 0.1)
        assertEquals(90.0, bearingDegrees(origin, GeoPoint(1.0, 0.0)), 0.1)
        assertEquals(180.0, bearingDegrees(origin, GeoPoint(0.0, -1.0)), 0.1)
        assertEquals(270.0, bearingDegrees(origin, GeoPoint(-1.0, 0.0)), 0.1)
    }

    @Test
    fun simulatedTrackerClampsAtFinalPoint() = runTest {
        val route = testRoute()

        val progress = StubRouteTrackingProvider(stepDelayMillis = 0L)
            .positions(route, startIndex = 0)
            .toList()

        assertEquals(listOf(0, 1, 2), progress.map { it.pointIndex })
        assertEquals(route.geometry.points.last(), progress.last().position)
    }

    @Test
    fun simulatedTrackerResumesFromRequestedIndex() = runTest {
        val progress = StubRouteTrackingProvider(stepDelayMillis = 0L)
            .positions(testRoute(), startIndex = 1)
            .toList()

        assertEquals(listOf(1, 2), progress.map { it.pointIndex })
    }

    private fun testRoute() = Route(
        id = 100,
        name = "Test route",
        description = "Test",
        distanceKm = 1.0,
        estimatedTimeMinutes = 10,
        difficulty = Difficulty.EASY,
        elevationGainMeters = 0,
        terrain = Terrain.MIXED,
        geometry = RouteGeometry(
            listOf(
                GeoPoint(0.0, 0.0),
                GeoPoint(1.0, 0.0),
                GeoPoint(2.0, 0.0),
            )
        ),
    )
}
