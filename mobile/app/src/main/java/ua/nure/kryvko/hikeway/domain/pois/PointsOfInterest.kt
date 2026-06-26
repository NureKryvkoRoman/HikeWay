package ua.nure.kryvko.hikeway.domain.pois

import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiComment
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

interface PointOfInterestRepository {
    suspend fun getPointsOfInterest(): List<PointOfInterest>
    suspend fun getNearby(center: GeoPoint, radiusMeters: Double): List<PointOfInterest> {
        return getPointsOfInterest()
    }
    suspend fun getDetail(poiId: Long): PointOfInterest = getPointsOfInterest().first { it.id == poiId }
    suspend fun update(poiId: Long, name: String, description: String, location: GeoPoint): PointOfInterest {
        error("PoI update is not supported by this repository.")
    }
    suspend fun delete(poiId: Long) {
        error("PoI deletion is not supported by this repository.")
    }
    suspend fun submitRating(poiId: Long, rating: Int): PoiRating {
        return PoiRating(rating.toDouble(), 1, rating)
    }
    suspend fun removeRating(poiId: Long): PoiRating {
        return PoiRating(0.0, 0, null)
    }
    suspend fun getComments(poiId: Long): List<PoiComment> = emptyList()
    suspend fun addComment(poiId: Long, text: String): PoiComment {
        error("PoI comments are not supported by this repository.")
    }
    suspend fun updateComment(poiId: Long, commentId: Long, text: String): PoiComment {
        error("PoI comments are not supported by this repository.")
    }
    suspend fun deleteComment(poiId: Long, commentId: Long) {
        error("PoI comments are not supported by this repository.")
    }
    suspend fun uploadPhoto(poiId: Long, upload: PoiPhotoUpload): PoiPhoto {
        error("PoI photo uploads are not supported by this repository.")
    }
    suspend fun updatePhoto(poiId: Long, photoId: Long, caption: String?): PoiPhoto {
        error("PoI photos are not supported by this repository.")
    }
    suspend fun deletePhoto(poiId: Long, photoId: Long) {
        error("PoI photos are not supported by this repository.")
    }
}

class GetPointsOfInterestUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(): List<PointOfInterest> {
        return repository.getPointsOfInterest()
    }
}

class GetNearbyPointsOfInterestUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(center: GeoPoint, radiusMeters: Double): List<PointOfInterest> {
        require(radiusMeters > 0.0) { "Radius must be greater than 0." }
        return repository.getNearby(center, radiusMeters)
    }
}

class GetPointOfInterestDetailUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long): PointOfInterest {
        return repository.getDetail(poiId)
    }
}

class UpdatePointOfInterestUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(
        poiId: Long,
        name: String,
        description: String,
        location: GeoPoint,
    ): PointOfInterest {
        require(name.isNotBlank()) { "PoI name is required." }
        require(description.isNotBlank()) { "PoI description is required." }
        return repository.update(poiId, name.trim(), description.trim(), location)
    }
}

class DeletePointOfInterestUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long) {
        repository.delete(poiId)
    }
}

class RatePointOfInterestUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, rating: Int): PoiRating {
        require(rating in 1..5) { "Rating must be from 1 to 5." }
        return repository.submitRating(poiId, rating)
    }
}

class RemovePointOfInterestRatingUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long): PoiRating {
        return repository.removeRating(poiId)
    }
}

class AddPoiCommentUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, text: String): PoiComment {
        return repository.addComment(poiId, requireCommentText(text))
    }
}

class UpdatePoiCommentUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, commentId: Long, text: String): PoiComment {
        return repository.updateComment(poiId, commentId, requireCommentText(text))
    }
}

class DeletePoiCommentUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, commentId: Long) {
        repository.deleteComment(poiId, commentId)
    }
}

class UploadPoiPhotoUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, upload: PoiPhotoUpload): PoiPhoto {
        require(upload.contentType.isNotBlank()) { "Photo content type is required." }
        require(upload.sizeBytes > 0) { "Photo cannot be empty." }
        return repository.uploadPhoto(poiId, upload)
    }
}

class UpdatePoiPhotoUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, photoId: Long, caption: String?): PoiPhoto {
        return repository.updatePhoto(poiId, photoId, caption?.trim()?.takeIf { it.isNotEmpty() })
    }
}

class DeletePoiPhotoUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, photoId: Long) {
        repository.deletePhoto(poiId, photoId)
    }
}

private fun requireCommentText(text: String): String {
    val trimmed = text.trim()
    require(trimmed.isNotEmpty()) { "Comment is required." }
    require(trimmed.length <= 2000) { "Comment must be at most 2000 characters." }
    return trimmed
}
