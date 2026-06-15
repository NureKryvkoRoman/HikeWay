package ua.nure.kryvko.hikeway.feature.completedhikes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.ObserveCompletedHikesUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class CompletedHikesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeHikeLogRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeHikeLogRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun exposesCompletedHikesFromRepository() = runTest(dispatcher) {
        val newest = hikeLog(id = 2, routeName = "Newest", finishedAtEpochMillis = 2_000)
        val oldest = hikeLog(id = 1, routeName = "Oldest", finishedAtEpochMillis = 1_000)
        repository.logs.value = listOf(newest, oldest)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf("Newest", "Oldest"), viewModel.uiState.value.hikes.map { it.routeName })
    }

    @Test
    fun selectingHikeOpensDetail() = runTest(dispatcher) {
        val hike = hikeLog(id = 1, routeName = "High Castle Loop")
        repository.logs.value = listOf(hike)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectHike(hike)

        assertEquals(hike.id, viewModel.uiState.value.selectedHike?.id)
    }

    @Test
    fun dismissingSelectedHikeReturnsToList() = runTest(dispatcher) {
        val hike = hikeLog(id = 1, routeName = "High Castle Loop")
        repository.logs.value = listOf(hike)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectHike(hike)
        viewModel.dismissSelectedHike()

        assertEquals(null, viewModel.uiState.value.selectedHike)
    }

    private fun viewModel(): CompletedHikesViewModel {
        return CompletedHikesViewModel(
            observeCompletedHikes = ObserveCompletedHikesUseCase(repository),
        )
    }
}

private class FakeHikeLogRepository : HikeLogRepository {
    val logs = MutableStateFlow<List<HikeLog>>(emptyList())

    override suspend fun save(log: HikeLog): Long = error("Not used")

    override fun observeAll(): Flow<List<HikeLog>> = logs
}

private fun hikeLog(
    id: Long,
    routeName: String,
    finishedAtEpochMillis: Long = 5_000,
) = HikeLog(
    id = id,
    routeId = id,
    routeName = routeName,
    startedAtEpochMillis = 1_000,
    finishedAtEpochMillis = finishedAtEpochMillis,
    activeDurationMillis = 3_000,
    wallClockDurationMillis = 4_000,
    totalDistanceKm = 0.42,
    path = listOf(
        GeoPoint(longitude = 24.0316, latitude = 49.8429),
        GeoPoint(longitude = 24.0394, latitude = 49.8461),
    ),
)
