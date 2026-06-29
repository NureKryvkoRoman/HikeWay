package ua.nure.kryvko.hikeway.data.routes.remote

import com.google.gson.Gson
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.data.services.backend.RouteService
import ua.nure.kryvko.hikeway.data.services.network.ApiException
import ua.nure.kryvko.hikeway.data.services.network.RetrofitFactory
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria

class RemoteRouteRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteRouteRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val gson = Gson()
        val service = RetrofitFactory.create(server.url("/").toString(), gson)
            .create(RouteService::class.java)
        repository = RemoteRouteRepository(service, gson)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun searchesRoutesAndMapsGeoJsonCoordinates() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "items": [
                    {
                      "id": 7,
                      "name": "Forest Trail",
                      "description": "Public route",
                      "distanceKm": 8.5,
                      "estimatedTimeMinutes": 120,
                      "difficulty": "EASY",
                      "elevationGain": 180,
                      "terrain": "FOREST",
                      "createdAt": "2026-06-25T10:00:00Z",
                      "createdBy": "seed-route-owner",
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [[24.1, 49.8], [24.2, 49.9]]
                      }
                    }
                  ],
                  "page": 0,
                  "size": 50,
                  "totalElements": 1,
                  "totalPages": 1
                }
                """.trimIndent()
            )
        )

        val routes = repository.search(
            RouteSearchCriteria(
                distanceKm = 5.0..12.0,
                estimatedTimeMinutes = 60..180,
                difficulties = setOf(Difficulty.EASY, Difficulty.HARD),
                terrains = setOf(Terrain.FOREST, Terrain.MIXED),
                maxProximityKm = 15.0,
            ),
            GeoPoint(longitude = 24.1, latitude = 49.8),
        )

        assertEquals(1, routes.size)
        assertEquals("Forest Trail", routes.single().name)
        assertEquals(Terrain.FOREST, routes.single().terrain)
        assertEquals(180, routes.single().elevationGainMeters)
        assertEquals(GeoPoint(24.1, 49.8), routes.single().geometry.points.first())
        assertEquals(
            "/routes?minDistanceKm=5.0&maxDistanceKm=12.0&minEstimatedTimeMinutes=60" +
                "&maxEstimatedTimeMinutes=180&difficulties=EASY&difficulties=HARD" +
                "&terrains=FOREST&terrains=MIXED&longitude=24.1&latitude=49.8" +
                "&maxProximityKm=15.0&page=0&size=50",
            server.takeRequest().path,
        )
    }

    @Test
    fun sendsLocationWithoutProximityFilter() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[],"page":0,"size":50,"totalElements":0,"totalPages":0}"""
            )
        )

        repository.search(RouteSearchCriteria(), GeoPoint(longitude = 24.1, latitude = 49.8))

        assertEquals("/routes?longitude=24.1&latitude=49.8&page=0&size=50", server.takeRequest().path)
    }

    @Test
    fun convertsApiFailures() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"code":"SERVER_ERROR"}"""))

        val error = runCatching {
            repository.search(RouteSearchCriteria(), GeoPoint(longitude = 24.1, latitude = 49.8))
        }.exceptionOrNull()

        assertTrue(error is ApiException)
    }

    @Test
    fun convertsUnauthenticatedResponsesToApiFailures() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody(""))

        val error = runCatching {
            repository.search(RouteSearchCriteria(), GeoPoint(longitude = 24.1, latitude = 49.8))
        }.exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals(401, (error as ApiException).statusCode)
    }
}
