package ua.nure.kryvko.hikeway.data.pois.stub

import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository

class StubPointOfInterestRepository(
    private val pointsOfInterest: List<PointOfInterest> = stubPointsOfInterest,
) : PointOfInterestRepository {
    override suspend fun getPointsOfInterest(): List<PointOfInterest> {
        return pointsOfInterest
    }

    override suspend fun submitRating(poiId: Long, rating: Int) {
        // Backend endpoint is not implemented yet; keep this call as a no-op.
    }
}

val stubPointsOfInterest = listOf(
    PointOfInterest(
        id = 1,
        name = "High Castle Viewpoint",
        description = "A compact hilltop stop with a wide view over central Lviv.",
        location = GeoPoint(longitude = 24.0394, latitude = 49.8461),
        photoResIds = emptyList(),
        averageRating = 4.6,
    ),
    PointOfInterest(
        id = 2,
        name = "Vynnyky Forest Lake",
        description = "A quiet lakeside rest point near shaded forest paths.",
        location = GeoPoint(longitude = 24.1440, latitude = 49.8105),
        photoResIds = emptyList(),
        averageRating = 4.3,
    ),
    PointOfInterest(
        id = 3,
        name = "Rocky Ridge Lookout",
        description = "An exposed ridge marker with open terrain and long-distance views.",
        location = GeoPoint(longitude = 23.9140, latitude = 49.6600),
        photoResIds = emptyList(),
        averageRating = 4.8,
    ),
)
