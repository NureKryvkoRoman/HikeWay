package ua.nure.kryvko.hikeway.data.pois.remote

import java.time.Instant
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiComment
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

data class PageResponseDto<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)

data class PoiSummaryDto(
    val id: Long,
    val name: String,
    val description: String,
    val longitude: Double,
    val latitude: Double,
    val ownerId: String?,
    val ownerDisplayName: String?,
    val ownedByCurrentUser: Boolean,
    val averageRating: Double,
    val ratingCount: Long,
    val userRating: Int?,
    val photos: List<PoiPhotoDto>,
)

data class PoiNearbySummaryDto(
    val id: Long,
    val name: String,
    val description: String,
    val longitude: Double,
    val latitude: Double,
    val ownerDisplayName: String?,
    val ownedByCurrentUser: Boolean,
    val distanceMeters: Double,
)

data class PoiDetailDto(
    val id: Long,
    val name: String,
    val description: String,
    val longitude: Double,
    val latitude: Double,
    val ownerId: String?,
    val ownerDisplayName: String?,
    val ownedByCurrentUser: Boolean,
    val averageRating: Double,
    val ratingCount: Long,
    val userRating: Int?,
    val photos: List<PoiPhotoDto>,
    val createdAt: String?,
    val updatedAt: String?,
)

data class PoiRatingDto(
    val averageRating: Double,
    val ratingCount: Long,
    val userRating: Int?,
)

data class PoiCommentDto(
    val id: Long,
    val authorId: String,
    val authorDisplayName: String,
    val ownedByCurrentUser: Boolean,
    val text: String,
    val createdAt: String?,
    val updatedAt: String?,
)

data class PoiPhotoDto(
    val id: Long,
    val contributorId: String,
    val contributorDisplayName: String,
    val ownedByCurrentUser: Boolean,
    val url: String,
    val contentType: String,
    val sizeBytes: Long,
    val caption: String?,
    val createdAt: String?,
)

data class PoiUploadDto(
    val photoId: Long,
    val objectKey: String,
    val uploadUrl: String,
    val expiresAt: String?,
    val contentType: String,
    val sizeBytes: Long,
)

data class PoiUpdateRequestDto(
    val name: String,
    val description: String,
    val longitude: Double,
    val latitude: Double,
)

data class PoiCreateRequestDto(
    val name: String,
    val description: String,
    val longitude: Double,
    val latitude: Double,
)

data class PoiRatingRequestDto(val score: Int)
data class PoiCommentRequestDto(val text: String)
data class PoiPhotoUploadRequestDto(val contentType: String, val sizeBytes: Long)
data class PoiPhotoFinalizeRequestDto(val photoId: Long, val caption: String?)
data class PoiPhotoUpdateRequestDto(val caption: String?)

fun PoiSummaryDto.toDomain(): PointOfInterest = PointOfInterest(
    id = id,
    name = name,
    description = description,
    location = GeoPoint(longitude, latitude),
    ownerId = ownerId,
    ownerDisplayName = ownerDisplayName,
    ownedByCurrentUser = ownedByCurrentUser,
    photos = photos.map { it.toDomain() },
    averageRating = averageRating,
    ratingCount = ratingCount,
    userRating = userRating,
)

fun PoiNearbySummaryDto.toDomain(): PointOfInterest = PointOfInterest(
    id = id,
    name = name,
    description = description,
    location = GeoPoint(longitude, latitude),
    ownerDisplayName = ownerDisplayName,
    ownedByCurrentUser = ownedByCurrentUser,
    distanceMeters = distanceMeters,
)

fun PoiDetailDto.toDomain(): PointOfInterest = PointOfInterest(
    id = id,
    name = name,
    description = description,
    location = GeoPoint(longitude, latitude),
    ownerId = ownerId,
    ownerDisplayName = ownerDisplayName,
    ownedByCurrentUser = ownedByCurrentUser,
    photos = photos.map { it.toDomain() },
    averageRating = averageRating,
    ratingCount = ratingCount,
    userRating = userRating,
    createdAt = createdAt.toInstantOrNull(),
    updatedAt = updatedAt.toInstantOrNull(),
)

fun PoiRatingDto.toDomain(): PoiRating = PoiRating(
    averageRating = averageRating,
    ratingCount = ratingCount,
    userRating = userRating,
)

fun PoiCommentDto.toDomain(): PoiComment = PoiComment(
    id = id,
    authorId = authorId,
    authorDisplayName = authorDisplayName,
    ownedByCurrentUser = ownedByCurrentUser,
    text = text,
    createdAt = createdAt.toInstantOrNull(),
    updatedAt = updatedAt.toInstantOrNull(),
)

fun PoiPhotoDto.toDomain(): PoiPhoto = PoiPhoto(
    id = id,
    contributorId = contributorId,
    contributorDisplayName = contributorDisplayName,
    ownedByCurrentUser = ownedByCurrentUser,
    url = url,
    contentType = contentType,
    sizeBytes = sizeBytes,
    caption = caption,
    createdAt = createdAt.toInstantOrNull(),
)

private fun String?.toInstantOrNull(): Instant? {
    return this?.let { runCatching { Instant.parse(it) }.getOrNull() }
}
