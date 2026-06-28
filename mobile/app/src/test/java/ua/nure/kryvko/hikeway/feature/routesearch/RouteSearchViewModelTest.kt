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
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider
import ua.nure.kryvko.hikeway.domain.pois.AddPoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.CreatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetNearbyPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetPointOfInterestDetailUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.RemovePointOfInterestRatingUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.UploadPoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus
import ua.nure.kryvko.hikeway.domain.routepicking.RouteProgress
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routes.GetCurrentLocationUseCase
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase
import ua.nure.kryvko.hikeway.domain.routes.matches
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
    fun exposesRouteFetchFailureAsRouteError() = runTest(dispatcher) {
        val viewModel = viewModel(
            locationProvider = StubLocationProvider(),
            routeRepository = FailingRouteRepository(),
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Routes are unavailable.", viewModel.uiState.value.errorMessage)
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
    fun longPressOpensContextAndStartsPoiCreation() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        val point = GeoPoint(longitude = 24.03, latitude = 49.84)
        viewModel.openMapContext(point)
        assertEquals(point, viewModel.uiState.value.mapContextPoint)

        viewModel.startPoiCreationFromContext()

        assertEquals(null, viewModel.uiState.value.mapContextPoint)
        assertEquals(point, viewModel.uiState.value.poiCreationPoint)
    }

    @Test
    fun createsPoiAndSelectsReturnedDetail() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        val point = GeoPoint(longitude = 24.03, latitude = 49.84)
        viewModel.openMapContext(point)
        viewModel.startPoiCreationFromContext()
        viewModel.updatePoiCreationName("New spring")
        viewModel.updatePoiCreationDescription("Fresh water near the trail.")
        viewModel.createPoi()
        advanceUntilIdle()

        assertEquals("New spring", viewModel.uiState.value.selectedPoi?.name)
        assertEquals(null, viewModel.uiState.value.poiCreationPoint)
        assertEquals(point, pointOfInterestRepository.created.single().location)
        assertEquals(true, viewModel.uiState.value.pointsOfInterest.any { it.name == "New spring" })
    }

    @Test
    fun createPoiFailurePreservesInputAndShowsError() = runTest(dispatcher) {
        pointOfInterestRepository.failCreates = true
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        val point = GeoPoint(longitude = 24.03, latitude = 49.84)
        viewModel.openMapContext(point)
        viewModel.startPoiCreationFromContext()
        viewModel.updatePoiCreationName("New spring")
        viewModel.updatePoiCreationDescription("Fresh water near the trail.")
        viewModel.createPoi()
        advanceUntilIdle()

        assertEquals(point, viewModel.uiState.value.poiCreationPoint)
        assertEquals("New spring", viewModel.uiState.value.poiCreationName)
        assertNotNull(viewModel.uiState.value.poiCreationErrorMessage)
    }

    @Test
    fun mapViewportChangesFetchNearbyPoisAfterDebounce() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()
        pointOfInterestRepository.nearbyRequests.clear()

        viewModel.onMapViewportChanged(GeoPoint(longitude = 24.2, latitude = 49.9), zoom = 12.0)
        advanceTimeBy(499)
        runCurrent()
        assertEquals(0, pointOfInterestRepository.nearbyRequests.size)

        advanceTimeBy(1)
        runCurrent()

        assertEquals(1, pointOfInterestRepository.nearbyRequests.size)
    }

    @Test
    fun smallMapViewportChangesDoNotRefetchNearbyPois() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()
        pointOfInterestRepository.nearbyRequests.clear()

        viewModel.onMapViewportChanged(GeoPoint(longitude = 24.2, latitude = 49.9), zoom = 12.0)
        advanceTimeBy(500)
        runCurrent()
        viewModel.onMapViewportChanged(GeoPoint(longitude = 24.2001, latitude = 49.9001), zoom = 12.0)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, pointOfInterestRepository.nearbyRequests.size)
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
        routeRepository: RouteRepository = FakeRouteRepository(),
    ): RouteSearchViewModel {
        return RouteSearchViewModel(
            searchRoutes = SearchRoutesUseCase(
                repository = routeRepository,
                locationProvider = locationProvider,
            ),
            getCurrentLocation = GetCurrentLocationUseCase(locationProvider),
            routeTrackingProvider = routeTrackingProvider,
            saveCompletedHike = SaveCompletedHikeUseCase(hikeLogRepository),
            timeProvider = timeProvider,
            activeTimer = TestActiveTimer(),
            getPointsOfInterest = GetPointsOfInterestUseCase(pointOfInterestRepository),
            getNearbyPointsOfInterest = GetNearbyPointsOfInterestUseCase(pointOfInterestRepository),
            getPointOfInterestDetail = GetPointOfInterestDetailUseCase(pointOfInterestRepository),
            createPointOfInterest = CreatePointOfInterestUseCase(pointOfInterestRepository),
            updatePointOfInterest = UpdatePointOfInterestUseCase(pointOfInterestRepository),
            deletePointOfInterest = DeletePointOfInterestUseCase(pointOfInterestRepository),
            ratePointOfInterest = RatePointOfInterestUseCase(pointOfInterestRepository),
            removePointOfInterestRating = RemovePointOfInterestRatingUseCase(pointOfInterestRepository),
            addPoiCommentUseCase = AddPoiCommentUseCase(pointOfInterestRepository),
            updatePoiCommentUseCase = UpdatePoiCommentUseCase(pointOfInterestRepository),
            deletePoiCommentUseCase = DeletePoiCommentUseCase(pointOfInterestRepository),
            uploadPoiPhotoUseCase = UploadPoiPhotoUseCase(pointOfInterestRepository),
            updatePoiPhotoUseCase = UpdatePoiPhotoUseCase(pointOfInterestRepository),
            deletePoiPhotoUseCase = DeletePoiPhotoUseCase(pointOfInterestRepository),
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

private class FakeRouteRepository : RouteRepository {
    private val routes = listOf(
        route(
            1,
            "High Castle Loop",
            4.8,
            95,
            Difficulty.EASY,
            140,
            Terrain.FOREST,
            24.0316 to 49.8429,
            24.0394 to 49.8461,
            24.0438 to 49.8488,
        ),
        route(
            2,
            "Vynnyky Forest Trail",
            11.2,
            210,
            Difficulty.MEDIUM,
            310,
            Terrain.FOREST,
            24.1290 to 49.8170,
            24.1440 to 49.8105,
            24.1570 to 49.8030,
        ),
        route(
            3,
            "Rocky Ridge Traverse",
            18.6,
            390,
            Difficulty.HARD,
            920,
            Terrain.ROCKY,
            23.8910 to 49.6510,
            23.9140 to 49.6600,
            23.9400 to 49.6560,
        ),
        route(
            4,
            "Mountain Meadow Path",
            8.4,
            175,
            Difficulty.MEDIUM,
            460,
            Terrain.MOUNTAIN,
            23.9870 to 49.7310,
            24.0020 to 49.7260,
            24.0150 to 49.7190,
        ),
        route(
            5,
            "Lakeside Mixed Walk",
            6.1,
            120,
            Difficulty.EASY,
            90,
            Terrain.MIXED,
            24.0710 to 49.8680,
            24.0800 to 49.8710,
            24.0920 to 49.8660,
        ),
    )

    override suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route> {
        return routes.filter { it.matches(criteria, origin) }
    }
}

private class FailingRouteRepository : RouteRepository {
    override suspend fun search(criteria: RouteSearchCriteria, origin: GeoPoint): List<Route> {
        error("Routes are unavailable.")
    }
}

private fun route(
    id: Long,
    name: String,
    distanceKm: Double,
    estimatedTimeMinutes: Int,
    difficulty: Difficulty,
    elevationGainMeters: Int,
    terrain: Terrain,
    vararg points: Pair<Double, Double>,
) = Route(
    id = id,
    name = name,
    description = name,
    distanceKm = distanceKm,
    estimatedTimeMinutes = estimatedTimeMinutes,
    difficulty = difficulty,
    elevationGainMeters = elevationGainMeters,
    terrain = terrain,
    geometry = RouteGeometry(points.map { (longitude, latitude) -> GeoPoint(longitude, latitude) }),
)

private class FakePointOfInterestRepository : PointOfInterestRepository {
    val submittedRatings = mutableListOf<Pair<Long, Int>>()
    val created = mutableListOf<PointOfInterest>()
    val nearbyRequests = mutableListOf<Pair<GeoPoint, Double>>()
    var failCreates = false

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

    override suspend fun getNearby(center: GeoPoint, radiusMeters: Double): List<PointOfInterest> {
        nearbyRequests += center to radiusMeters
        return getPointsOfInterest()
    }

    override suspend fun create(
        name: String,
        description: String,
        location: GeoPoint,
    ): PointOfInterest {
        if (failCreates) error("create failed")
        return PointOfInterest(
            id = 202,
            name = name,
            description = description,
            location = location,
            ownerId = "me",
            ownerDisplayName = "Me",
            ownedByCurrentUser = true,
        ).also { created += it }
    }

    override suspend fun submitRating(poiId: Long, rating: Int): PoiRating {
        submittedRatings += poiId to rating
        return PoiRating(rating.toDouble(), 1, rating)
    }
}
