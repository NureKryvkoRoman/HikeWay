package ua.nure.kryvko.hikeway.data.routes.local

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain

class RouteMapperTest {
    @Test
    fun serializesGeometryAsGeoJsonLineStringWithLongitudeLatitudeOrder() {
        val entity = route().toEntity(ownerUserId = "user-123")

        assertEquals(
            """{"type":"LineString","coordinates":[[24.0316,49.8429],[24.0394,49.8461]]}""",
            entity.geometryGeoJson,
        )
    }

    @Test
    fun roundTripsRouteGeometry() {
        val route = route()

        assertEquals(route.geometry.points, route.toEntity(ownerUserId = "user-123").toDomain().geometry.points)
    }

    @Test
    fun writesOwnerUserId() {
        assertEquals("user-123", route().toEntity(ownerUserId = "user-123").ownerUserId)
    }

    private fun route() = Route(
        id = 1,
        name = "Local route",
        description = "Created locally",
        distanceKm = 1.2,
        estimatedTimeMinutes = 18,
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
