package ua.nure.kryvko.hikeway.data.pois.stub

import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiComment
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository

class StubPointOfInterestRepository(
    private val pointsOfInterest: List<PointOfInterest> = stubPointsOfInterest,
) : PointOfInterestRepository {
    override suspend fun getPointsOfInterest(): List<PointOfInterest> {
        return pointsOfInterest
    }

    override suspend fun getDetail(poiId: Long): PointOfInterest {
        return pointsOfInterest.first { it.id == poiId }
    }

    override suspend fun create(
        name: String,
        description: String,
        location: GeoPoint,
    ): PointOfInterest {
        return PointOfInterest(
            id = (pointsOfInterest.maxOfOrNull { it.id } ?: 0L) + 1L,
            name = name,
            description = description,
            location = location,
            ownerId = "stub",
            ownerDisplayName = "Stub user",
            ownedByCurrentUser = true,
        )
    }

    override suspend fun update(
        poiId: Long,
        name: String,
        description: String,
        location: GeoPoint,
    ): PointOfInterest {
        return getDetail(poiId).copy(name = name, description = description, location = location)
    }

    override suspend fun delete(poiId: Long) = Unit

    override suspend fun submitRating(poiId: Long, rating: Int): PoiRating {
        return PoiRating(averageRating = rating.toDouble(), ratingCount = 1, userRating = rating)
    }

    override suspend fun removeRating(poiId: Long): PoiRating {
        return PoiRating(averageRating = 0.0, ratingCount = 0, userRating = null)
    }

    override suspend fun getComments(poiId: Long): List<PoiComment> = emptyList()

    override suspend fun addComment(poiId: Long, text: String): PoiComment {
        return PoiComment(1, "stub", "Stub user", true, text)
    }

    override suspend fun updateComment(poiId: Long, commentId: Long, text: String): PoiComment {
        return PoiComment(commentId, "stub", "Stub user", true, text)
    }

    override suspend fun deleteComment(poiId: Long, commentId: Long) = Unit

    override suspend fun uploadPhoto(poiId: Long, upload: PoiPhotoUpload): PoiPhoto {
        return PoiPhoto(
            id = 1,
            contributorId = "stub",
            contributorDisplayName = "Stub user",
            ownedByCurrentUser = true,
            url = "",
            contentType = upload.contentType,
            sizeBytes = upload.sizeBytes,
            caption = upload.caption,
        )
    }

    override suspend fun updatePhoto(poiId: Long, photoId: Long, caption: String?): PoiPhoto {
        return PoiPhoto(photoId, "stub", "Stub user", true, "", "image/jpeg", 0, caption)
    }

    override suspend fun deletePhoto(poiId: Long, photoId: Long) = Unit
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
