package ua.nure.kryvko.hikeway.domain.hikelogging

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ua.nure.kryvko.hikeway.core.model.GeoPoint

data class HikeLog(
    val id: Long = 0,
    val routeId: Long,
    val routeName: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val activeDurationMillis: Long,
    val wallClockDurationMillis: Long,
    val totalDistanceKm: Double,
    val path: List<GeoPoint>,
)

interface HikeLogRepository {
    suspend fun save(log: HikeLog): Long
    fun observeAll(): Flow<List<HikeLog>>
}

class SaveCompletedHikeUseCase(
    private val repository: HikeLogRepository,
) {
    suspend operator fun invoke(log: HikeLog): Long = repository.save(log)
}

class ObserveCompletedHikesUseCase(
    private val repository: HikeLogRepository,
) {
    operator fun invoke(): Flow<List<HikeLog>> = repository.observeAll()
}

interface TimeProvider {
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

interface ActiveTimer {
    fun ticks(periodMillis: Long = 1_000L): Flow<Long>
}

class SystemActiveTimer : ActiveTimer {
    override fun ticks(periodMillis: Long): Flow<Long> = flow {
        while (true) {
            delay(periodMillis)
            emit(periodMillis)
        }
    }
}
