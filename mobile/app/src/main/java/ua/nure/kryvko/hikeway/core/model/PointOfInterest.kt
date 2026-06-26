package ua.nure.kryvko.hikeway.core.model

import java.time.Instant

data class PointOfInterest(
    val id: Long,
    val name: String,
    val description: String,
    val location: GeoPoint,
    val ownerId: String? = null,
    val ownerDisplayName: String? = null,
    val ownedByCurrentUser: Boolean = false,
    val photos: List<PoiPhoto> = emptyList(),
    val photoResIds: List<Int> = emptyList(),
    val averageRating: Double = 0.0,
    val ratingCount: Long = 0L,
    val userRating: Int? = null,
    val distanceMeters: Double? = null,
    val comments: List<PoiComment> = emptyList(),
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

data class PoiPhoto(
    val id: Long,
    val contributorId: String,
    val contributorDisplayName: String,
    val ownedByCurrentUser: Boolean,
    val url: String,
    val contentType: String,
    val sizeBytes: Long,
    val caption: String?,
    val createdAt: Instant? = null,
)

data class PoiComment(
    val id: Long,
    val authorId: String,
    val authorDisplayName: String,
    val ownedByCurrentUser: Boolean,
    val text: String,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
)

data class PoiRating(
    val averageRating: Double,
    val ratingCount: Long,
    val userRating: Int?,
)

data class PoiPhotoUpload(
    val bytes: ByteArray,
    val contentType: String,
    val caption: String?,
) {
    val sizeBytes: Long = bytes.size.toLong()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PoiPhotoUpload
        return bytes.contentEquals(other.bytes) &&
            contentType == other.contentType &&
            caption == other.caption
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + (caption?.hashCode() ?: 0)
        return result
    }
}
