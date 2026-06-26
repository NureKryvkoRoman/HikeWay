package ua.nure.kryvko.hikeway.data.pois.remote

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiComment
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.data.services.backend.PoiService
import ua.nure.kryvko.hikeway.data.services.network.toApiException
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository

private const val DEFAULT_PAGE_SIZE = 50
private const val DEFAULT_NEARBY_RADIUS_METERS = 20_000.0

class RemotePointOfInterestRepository(
    private val service: PoiService,
    private val gson: Gson,
) : PointOfInterestRepository {
    override suspend fun getPointsOfInterest(): List<PointOfInterest> {
        return apiCall("Points of interest are unavailable.") {
            service.list(size = DEFAULT_PAGE_SIZE).items.map { it.toDomain() }
        }
    }

    override suspend fun getNearby(center: GeoPoint, radiusMeters: Double): List<PointOfInterest> {
        return apiCall("Nearby points of interest are unavailable.") {
            service.nearby(
                longitude = center.longitude,
                latitude = center.latitude,
                radiusMeters = radiusMeters.coerceAtMost(100_000.0).takeIf { it > 0.0 }
                    ?: DEFAULT_NEARBY_RADIUS_METERS,
                size = DEFAULT_PAGE_SIZE,
            ).items.map { it.toDomain() }
        }
    }

    override suspend fun getDetail(poiId: Long): PointOfInterest {
        return apiCall("Point of interest is unavailable.") {
            val detail = service.get(poiId).toDomain()
            detail.copy(comments = service.comments(poiId, size = DEFAULT_PAGE_SIZE).items.map { it.toDomain() })
        }
    }

    override suspend fun update(
        poiId: Long,
        name: String,
        description: String,
        location: GeoPoint,
    ): PointOfInterest {
        return apiCall("Could not update point of interest.") {
            service.update(
                poiId,
                PoiUpdateRequestDto(
                    name = name,
                    description = description,
                    longitude = location.longitude,
                    latitude = location.latitude,
                )
            ).toDomain()
        }
    }

    override suspend fun delete(poiId: Long) {
        return apiCall("Could not delete point of interest.") {
            service.delete(poiId)
        }
    }

    override suspend fun submitRating(poiId: Long, rating: Int): PoiRating {
        return apiCall("Could not submit rating.") {
            service.rate(poiId, PoiRatingRequestDto(rating)).toDomain()
        }
    }

    override suspend fun removeRating(poiId: Long): PoiRating {
        return apiCall("Could not remove rating.") {
            service.removeRating(poiId).toDomain()
        }
    }

    override suspend fun getComments(poiId: Long): List<PoiComment> {
        return apiCall("Comments are unavailable.") {
            service.comments(poiId, size = DEFAULT_PAGE_SIZE).items.map { it.toDomain() }
        }
    }

    override suspend fun addComment(poiId: Long, text: String): PoiComment {
        return apiCall("Could not add comment.") {
            service.addComment(poiId, PoiCommentRequestDto(text)).toDomain()
        }
    }

    override suspend fun updateComment(poiId: Long, commentId: Long, text: String): PoiComment {
        return apiCall("Could not update comment.") {
            service.updateComment(poiId, commentId, PoiCommentRequestDto(text)).toDomain()
        }
    }

    override suspend fun deleteComment(poiId: Long, commentId: Long) {
        return apiCall("Could not delete comment.") {
            service.deleteComment(poiId, commentId)
        }
    }

    override suspend fun uploadPhoto(poiId: Long, upload: PoiPhotoUpload): PoiPhoto {
        return apiCall("Could not upload photo.") {
            val uploadTicket = service.createPhotoUpload(
                poiId,
                PoiPhotoUploadRequestDto(
                    contentType = upload.contentType,
                    sizeBytes = upload.sizeBytes,
                )
            )
            val body = upload.bytes.toRequestBody(upload.contentType.toMediaType())
            service.uploadPhotoBytes(uploadTicket.uploadUrl, body).close()
            service.finalizePhoto(
                poiId,
                PoiPhotoFinalizeRequestDto(
                    photoId = uploadTicket.photoId,
                    caption = upload.caption?.trim()?.takeIf { it.isNotEmpty() },
                )
            ).toDomain()
        }
    }

    override suspend fun updatePhoto(poiId: Long, photoId: Long, caption: String?): PoiPhoto {
        return apiCall("Could not update photo.") {
            service.updatePhoto(poiId, photoId, PoiPhotoUpdateRequestDto(caption)).toDomain()
        }
    }

    override suspend fun deletePhoto(poiId: Long, photoId: Long) {
        return apiCall("Could not delete photo.") {
            service.deletePhoto(poiId, photoId)
        }
    }

    private suspend fun <T> apiCall(fallbackMessage: String, block: suspend () -> T): T {
        return runCatching { block() }
            .getOrElse { throw it.toApiException(gson, fallbackMessage) }
    }
}
