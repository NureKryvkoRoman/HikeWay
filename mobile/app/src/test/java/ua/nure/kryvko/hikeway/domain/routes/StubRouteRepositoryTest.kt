package ua.nure.kryvko.hikeway.domain.routes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository

class StubRouteRepositoryTest {
    private val origin = GeoPoint(longitude = 24.0316, latitude = 49.8429)
    private val repository = StubRouteRepository()

    @Test
    fun filtersDistanceAndDurationInclusively() = runTest {
        val routes = repository.search(
            criteria = RouteSearchCriteria(
                distanceKm = 4.8..6.1,
                estimatedTimeMinutes = 95..120,
            ),
            origin = origin,
        )

        assertEquals(listOf("High Castle Loop", "Lakeside Mixed Walk"), routes.map { it.name })
    }

    @Test
    fun filtersDifficultyAndTerrainWithMultiSelect() = runTest {
        val routes = repository.search(
            criteria = RouteSearchCriteria(
                difficulties = setOf(Difficulty.EASY, Difficulty.MEDIUM),
                terrains = setOf(Terrain.MOUNTAIN, Terrain.MIXED),
            ),
            origin = origin,
        )

        assertEquals(listOf("Mountain Meadow Path", "Lakeside Mixed Walk"), routes.map { it.name })
    }

    @Test
    fun filtersProximityFromFirstGeometryPoint() = runTest {
        val routes = repository.search(
            criteria = RouteSearchCriteria(maxProximityKm = 0.1),
            origin = origin,
        )

        assertEquals(listOf("High Castle Loop"), routes.map { it.name })
    }

    @Test
    fun returnsEmptyListWhenCombinedFiltersDoNotMatch() = runTest {
        val routes = repository.search(
            criteria = RouteSearchCriteria(
                distanceKm = 0.0..5.0,
                difficulties = setOf(Difficulty.HARD),
            ),
            origin = origin,
        )

        assertTrue(routes.isEmpty())
    }

    @Test
    fun haversineDistanceIsApproximatelyCorrect() {
        val oneDegreeNorth = GeoPoint(longitude = origin.longitude, latitude = origin.latitude + 1)

        assertEquals(111.2, distanceKm(origin, oneDegreeNorth), 0.2)
    }
}
