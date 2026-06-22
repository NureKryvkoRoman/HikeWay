package ua.nure.kryvko.hikeway.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint

class MapCenterModeTest {
    @Test
    fun currentLocationUsesProvidedLocation() {
        val currentLocation = GeoPoint(longitude = 30.5234, latitude = 50.4501)

        assertEquals(
            currentLocation,
            MapCenterMode.CurrentLocation.resolveCenter(currentLocation = currentLocation),
        )
    }

    @Test
    fun routeStartUsesFirstPoint() {
        val first = GeoPoint(longitude = 24.0316, latitude = 49.8429)

        assertEquals(
            first,
            MapCenterMode.RouteStart(
                listOf(
                    first,
                    GeoPoint(longitude = 24.0394, latitude = 49.8461),
                )
            ).resolveCenter(),
        )
    }

    @Test
    fun emptyRouteStartFallsBackToDefaultCenter() {
        assertEquals(DEFAULT_MAP_CENTER, MapCenterMode.RouteStart(emptyList()).resolveCenter())
    }

    @Test
    fun fixedUsesProvidedPoint() {
        val point = GeoPoint(longitude = 24.0, latitude = 49.0)

        assertEquals(point, MapCenterMode.Fixed(point).resolveCenter())
    }
}
