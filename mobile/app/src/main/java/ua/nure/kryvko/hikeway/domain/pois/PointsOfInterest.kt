package ua.nure.kryvko.hikeway.domain.pois

import ua.nure.kryvko.hikeway.core.model.PointOfInterest

interface PointOfInterestRepository {
    suspend fun getPointsOfInterest(): List<PointOfInterest>
    suspend fun submitRating(poiId: Long, rating: Int)
}

class GetPointsOfInterestUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(): List<PointOfInterest> {
        return repository.getPointsOfInterest()
    }
}

class RatePointOfInterestUseCase(
    private val repository: PointOfInterestRepository,
) {
    suspend operator fun invoke(poiId: Long, rating: Int) {
        require(rating in 1..5) { "Rating must be from 1 to 5." }
        repository.submitRating(poiId, rating)
    }
}
