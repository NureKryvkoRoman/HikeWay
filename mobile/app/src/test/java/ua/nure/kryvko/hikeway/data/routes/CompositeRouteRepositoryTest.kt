package ua.nure.kryvko.hikeway.data.routes

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.matches

class CompositeRouteRepositoryTest {
    private val origin = GeoPoint(longitude = 24.0, latitude = 49.0)

    @Test
    fun searchesAllRepositories() = runTest {
        val repository = CompositeRouteRepository(
            listOf(
                StaticRouteRepository(listOf(route(1, "Built in"))),
                StaticRouteRepository(listOf(route(2, "Local"))),
            )
        )

        val routes = repository.search(RouteSearchCriteria(), origin)

        assertEquals(listOf("Built in", "Local"), routes.map { it.name })
    }

    @Test
    fun childRepositoriesCanApplyFilters() = runTest {
        val repository = CompositeRouteRepository(
            listOf(
                StaticRouteRepository(
                    listOf(
                        route(1, "Easy", Difficulty.EASY),
                        route(2, "Hard", Difficulty.HARD),
                    )
                )
            )
        )

        val routes = repository.search(
            RouteSearchCriteria(difficulties = setOf(Difficulty.HARD)),
            origin,
        )

        assertEquals(listOf("Hard"), routes.map { it.name })
    }
}

private class StaticRouteRepository(
    private val routes: List<Route>,
) : RouteRepository {
    override suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route> {
        return routes.filter { it.matches(criteria, origin) }
    }
}

private fun route(
    id: Long,
    name: String,
    difficulty: Difficulty = Difficulty.EASY,
) = Route(
    id = id,
    name = name,
    description = name,
    distanceKm = 1.0,
    estimatedTimeMinutes = 15,
    difficulty = difficulty,
    elevationGainMeters = 0,
    terrain = Terrain.FOREST,
    geometry = RouteGeometry(listOf(GeoPoint(longitude = 24.0, latitude = 49.0))),
)
