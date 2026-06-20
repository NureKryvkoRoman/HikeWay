package ua.nure.kryvko.hikeway.data.routes.local

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.data.hikelogging.local.HikeWayDatabase
import ua.nure.kryvko.hikeway.domain.auth.MutableCurrentUserProvider
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria

class RouteDaoTest {
    private lateinit var database: HikeWayDatabase
    private lateinit var repository: RoomRouteRepository
    private lateinit var currentUserProvider: MutableCurrentUserProvider

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            HikeWayDatabase::class.java,
        ).allowMainThreadQueries().build()
        currentUserProvider = MutableCurrentUserProvider()
        currentUserProvider.setCurrentUserId("user-1")
        repository = RoomRouteRepository(database.routeDao(), currentUserProvider)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertsAndReadsCustomRoute() = runBlocking {
        val id = repository.save(route())

        val routes = repository.search(
            criteria = RouteSearchCriteria(),
            origin = GeoPoint(longitude = 24.0316, latitude = 49.8429),
        )

        assertEquals(id, routes.single().id)
        assertEquals("Local route", routes.single().name)
        assertEquals(2, routes.single().geometry.points.size)
    }

    @Test
    fun searchesOnlyCurrentUsersCustomRoutes() = runBlocking {
        repository.save(route(name = "User one route"))
        currentUserProvider.setCurrentUserId("user-2")
        repository.save(route(name = "User two route"))

        val userTwoRoutes = repository.search(
            criteria = RouteSearchCriteria(),
            origin = GeoPoint(longitude = 24.0316, latitude = 49.8429),
        )
        currentUserProvider.setCurrentUserId("user-1")
        val userOneRoutes = repository.search(
            criteria = RouteSearchCriteria(),
            origin = GeoPoint(longitude = 24.0316, latitude = 49.8429),
        )

        assertEquals("User two route", userTwoRoutes.single().name)
        assertEquals("User one route", userOneRoutes.single().name)
    }

    private fun route(name: String = "Local route") = Route(
        id = 0,
        name = name,
        description = "Created locally",
        distanceKm = 0.5,
        estimatedTimeMinutes = 8,
        difficulty = Difficulty.EASY,
        elevationGainMeters = 0,
        terrain = Terrain.FOREST,
        geometry = RouteGeometry(
            listOf(
                GeoPoint(longitude = 24.0316, latitude = 49.8429),
                GeoPoint(longitude = 24.0394, latitude = 49.8461),
            )
        ),
    )
}
