package ua.nure.kryvko.hikeway.feature.routesearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.location.LocationProvider
import ua.nure.kryvko.hikeway.core.location.StubLocationProvider
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus
import ua.nure.kryvko.hikeway.domain.routepicking.RouteProgress
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routes.GetCurrentLocationUseCase
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

@OptIn(ExperimentalCoroutinesApi::class)
class RouteSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var hikeLogRepository: FakeHikeLogRepository
    private lateinit var pointOfInterestRepository: FakePointOfInterestRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        timeProvider = FakeTimeProvider()
        hikeLogRepository = FakeHikeLogRepository()
        pointOfInterestRepository = FakePointOfInterestRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun draftFiltersDoNotAffectRoutesUntilApply() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()
        assertEquals(5, viewModel.uiState.value.routes.size)

        viewModel.updateDraft(
            RouteSearchCriteria(difficulties = setOf(Difficulty.HARD))
        )
        assertEquals(5, viewModel.uiState.value.routes.size)

        viewModel.applyFilters()
        advanceUntilIdle()

        assertEquals(listOf("Rocky Ridge Traverse"), viewModel.uiState.value.routes.map { it.name })
    }

    @Test
    fun exposesLocationFailure() = runTest(dispatcher) {
        val viewModel = viewModel(
            locationProvider = object : LocationProvider {
                override suspend fun getCurrentLocation(): GeoPoint {
                    error("Location unavailable")
                }
            }
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun centersMapOnCurrentLocationAtStartup() = runTest(dispatcher) {
        val currentLocation = GeoPoint(longitude = 30.5234, latitude = 50.4501)
        val viewModel = viewModel(StubLocationProvider(currentLocation))
        advanceUntilIdle()

        assertEquals(currentLocation, viewModel.uiState.value.mapCenter)
    }

    @Test
    fun centerOnCurrentLocationUpdatesMapCenter() = runTest(dispatcher) {
        val locationProvider = MutableLocationProvider(
            location = GeoPoint(longitude = 24.0316, latitude = 49.8429),
        )
        val viewModel = viewModel(locationProvider)
        advanceUntilIdle()

        val newLocation = GeoPoint(longitude = 30.5234, latitude = 50.4501)
        locationProvider.location = newLocation
        viewModel.centerOnCurrentLocation()
        advanceUntilIdle()

        assertEquals(newLocation, viewModel.uiState.value.mapCenter)
    }

    @Test
    fun selectingRouteShowsPreviewWithoutStartingSession() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        val route = viewModel.uiState.value.routes.first()
        viewModel.previewRoute(route)
        runCurrent()

        assertEquals(route.id, viewModel.uiState.value.previewRoute?.id)
        assertEquals(null, viewModel.uiState.value.pickingSession)
    }

    @Test
    fun loadsAndSelectsPointOfInterest() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.selectPoi(101)

        assertEquals("Forest spring", viewModel.uiState.value.selectedPoi?.name)
    }

    @Test
    fun ratePointOfInterestUpdatesUiAndSubmitsRating() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.selectPoi(101)
        viewModel.ratePoi(5)
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.selectedPoi?.userRating)
        assertEquals(101L to 5, pointOfInterestRepository.submittedRatings.single())
    }

    @Test
    fun startingPreviewedRouteCreatesActiveSessionAndClearsPreview() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        val route = viewModel.uiState.value.routes.first()
        viewModel.previewRoute(route)
        viewModel.startPreviewedRoute()
        runCurrent()

        val session = viewModel.uiState.value.pickingSession
        assertEquals(null, viewModel.uiState.value.previewRoute)
        assertEquals(route.id, session?.route?.id)
        assertEquals(RoutePickingStatus.ACTIVE, session?.status)
        assertEquals(0, session?.pointIndex)
        viewModel.pauseRoute()
    }

    @Test
    fun dismissingPreviewKeepsSearchResults() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.dismissRoutePreview()

        assertEquals(null, viewModel.uiState.value.previewRoute)
        assertEquals(5, viewModel.uiState.value.routes.size)
    }

    @Test
    fun pauseStopsPositionAdvancement() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        runCurrent()
        viewModel.pauseRoute()
        val pausedIndex = viewModel.uiState.value.pickingSession?.pointIndex

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(pausedIndex, viewModel.uiState.value.pickingSession?.pointIndex)
        assertEquals(RoutePickingStatus.PAUSED, viewModel.uiState.value.pickingSession?.status)
    }

    @Test
    fun unpauseResumesPositionAdvancement() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        runCurrent()
        viewModel.pauseRoute()
        viewModel.unpauseRoute()
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(RoutePickingStatus.ACTIVE, viewModel.uiState.value.pickingSession?.status)
        assertEquals(1, viewModel.uiState.value.pickingSession?.pointIndex)
        viewModel.pauseRoute()
    }

    @Test
    fun finishClearsPickingSession() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        runCurrent()
        viewModel.finishRoute()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.pickingSession)
    }

    @Test
    fun activeElapsedTimeAdvancesOnlyWhileUnpaused() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        advanceTimeBy(2_000)
        runCurrent()
        viewModel.pauseRoute()
        val elapsedAtPause = viewModel.uiState.value.pickingSession?.activeElapsedMillis

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(2_000L, elapsedAtPause)
        assertEquals(elapsedAtPause, viewModel.uiState.value.pickingSession?.activeElapsedMillis)
    }

    @Test
    fun walkedPathAndDistanceAccumulateWhileActive() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        advanceTimeBy(2_000)
        runCurrent()

        val session = viewModel.uiState.value.pickingSession
        assertEquals(3, session?.walkedPath?.size)
        assertEquals(true, (session?.walkedDistanceKm ?: 0.0) > 0.0)
        viewModel.pauseRoute()
    }

    @Test
    fun finishSavesCompletedHikeWithActiveAndWallClockDurations() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        advanceTimeBy(2_000)
        runCurrent()
        viewModel.pauseRoute()
        timeProvider.now += 10_000
        viewModel.finishRoute()
        advanceUntilIdle()

        val saved = hikeLogRepository.saved.single()
        assertEquals(2_000L, saved.activeDurationMillis)
        assertEquals(10_000L, saved.wallClockDurationMillis)
        assertEquals(true, saved.path.size >= 2)
        assertEquals(null, viewModel.uiState.value.pickingSession)
    }

    @Test
    fun saveFailureKeepsPausedSessionAndShowsError() = runTest(dispatcher) {
        hikeLogRepository.failSaves = true
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        runCurrent()
        viewModel.pauseRoute()
        viewModel.finishRoute()
        advanceUntilIdle()

        assertEquals(RoutePickingStatus.PAUSED, viewModel.uiState.value.pickingSession?.status)
        assertNotNull(viewModel.uiState.value.saveErrorMessage)
    }

    @Test
    fun trackingFailurePausesSessionAndShowsError() = runTest(dispatcher) {
        val viewModel = viewModel(
            locationProvider = StubLocationProvider(),
            routeTrackingProvider = object : RouteTrackingProvider {
                override fun positions(route: ua.nure.kryvko.hikeway.core.model.Route, startIndex: Int): Flow<RouteProgress> {
                    return flow { error("GPS unavailable") }
                }
            },
        )
        advanceUntilIdle()

        viewModel.previewRoute(viewModel.uiState.value.routes.first())
        viewModel.startPreviewedRoute()
        runCurrent()

        assertEquals(RoutePickingStatus.PAUSED, viewModel.uiState.value.pickingSession?.status)
        assertEquals("GPS unavailable", viewModel.uiState.value.saveErrorMessage)
        viewModel.finishRoute()
        advanceUntilIdle()
    }

    private fun viewModel(
        locationProvider: LocationProvider,
        routeTrackingProvider: RouteTrackingProvider = StubRouteTrackingProvider(stepDelayMillis = 1_000L),
    ): RouteSearchViewModel {
        return RouteSearchViewModel(
            searchRoutes = SearchRoutesUseCase(
                repository = StubRouteRepository(),
                locationProvider = locationProvider,
            ),
            getCurrentLocation = GetCurrentLocationUseCase(locationProvider),
            routeTrackingProvider = routeTrackingProvider,
            saveCompletedHike = SaveCompletedHikeUseCase(hikeLogRepository),
            timeProvider = timeProvider,
            activeTimer = TestActiveTimer(),
            getPointsOfInterest = GetPointsOfInterestUseCase(pointOfInterestRepository),
            ratePointOfInterest = RatePointOfInterestUseCase(pointOfInterestRepository),
        )
    }
}

private class MutableLocationProvider(
    var location: GeoPoint,
) : LocationProvider {
    override suspend fun getCurrentLocation(): GeoPoint = location
}

private class FakeTimeProvider : TimeProvider {
    var now = 1_000L

    override fun currentTimeMillis(): Long = now
}

private class TestActiveTimer : ActiveTimer {
    override fun ticks(periodMillis: Long): Flow<Long> = flow {
        while (true) {
            delay(periodMillis)
            emit(periodMillis)
        }
    }
}

private class FakeHikeLogRepository : HikeLogRepository {
    val saved = mutableListOf<HikeLog>()
    var failSaves = false

    override suspend fun save(log: HikeLog): Long {
        if (failSaves) error("save failed")
        saved += log
        return saved.size.toLong()
    }

    override fun observeAll(): Flow<List<HikeLog>> = emptyFlow()
}

private class FakePointOfInterestRepository : PointOfInterestRepository {
    val submittedRatings = mutableListOf<Pair<Long, Int>>()

    override suspend fun getPointsOfInterest(): List<PointOfInterest> {
        return listOf(
            PointOfInterest(
                id = 101,
                name = "Forest spring",
                description = "A shaded water stop.",
                location = GeoPoint(longitude = 24.1, latitude = 49.8),
                photoResIds = emptyList(),
                averageRating = 4.2,
            )
        )
    }

    override suspend fun submitRating(poiId: Long, rating: Int) {
        submittedRatings += poiId to rating
    }
}
