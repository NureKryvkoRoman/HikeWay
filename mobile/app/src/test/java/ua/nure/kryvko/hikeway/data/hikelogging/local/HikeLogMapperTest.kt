package ua.nure.kryvko.hikeway.data.hikelogging.local

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog

class HikeLogMapperTest {
    @Test
    fun serializesPathAsGeoJsonLineStringWithLongitudeLatitudeOrder() {
        val entity = hikeLog(
            path = listOf(
                GeoPoint(longitude = 24.0316, latitude = 49.8429),
                GeoPoint(longitude = 24.0394, latitude = 49.8461),
            )
        ).toEntity(ownerUserId = "user-123")

        assertEquals(
            """{"type":"LineString","coordinates":[[24.0316,49.8429],[24.0394,49.8461]]}""",
            entity.pathGeoJson,
        )
    }

    @Test
    fun roundTripsGeoJsonPath() {
        val log = hikeLog(
            path = listOf(
                GeoPoint(longitude = 24.0316, latitude = 49.8429),
                GeoPoint(longitude = 24.0394, latitude = 49.8461),
            )
        )

        assertEquals(log.path, log.toEntity(ownerUserId = "user-123").toDomain().path)
    }

    @Test
    fun writesOwnerUserId() {
        assertEquals("user-123", hikeLog(emptyList()).toEntity(ownerUserId = "user-123").ownerUserId)
    }

    private fun hikeLog(path: List<GeoPoint>) = HikeLog(
        routeId = 1,
        routeName = "High Castle Loop",
        startedAtEpochMillis = 1_000,
        finishedAtEpochMillis = 5_000,
        activeDurationMillis = 3_000,
        wallClockDurationMillis = 4_000,
        totalDistanceKm = 0.5,
        path = path,
    )
}
