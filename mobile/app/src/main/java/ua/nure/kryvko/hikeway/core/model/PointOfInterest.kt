package ua.nure.kryvko.hikeway.core.model

data class PointOfInterest(
    val id: Long,
    val name: String,
    val description: String,
    val location: GeoPoint,
    val photoResIds: List<Int>,
    val averageRating: Double,
    val userRating: Int? = null,
)
