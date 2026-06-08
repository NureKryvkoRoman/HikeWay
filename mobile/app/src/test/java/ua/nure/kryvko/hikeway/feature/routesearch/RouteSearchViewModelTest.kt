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
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class RouteSearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
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
    fun pickingRouteCreatesActiveSession() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        val route = viewModel.uiState.value.routes.first()
        viewModel.pickRoute(route)
        runCurrent()

        val session = viewModel.uiState.value.pickingSession
        assertEquals(route.id, session?.route?.id)
        assertEquals(RoutePickingStatus.ACTIVE, session?.status)
        assertEquals(0, session?.pointIndex)
    }

    @Test
    fun pauseStopsPositionAdvancement() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.pickRoute(viewModel.uiState.value.routes.first())
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

        viewModel.pickRoute(viewModel.uiState.value.routes.first())
        runCurrent()
        viewModel.pauseRoute()
        viewModel.unpauseRoute()
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(RoutePickingStatus.ACTIVE, viewModel.uiState.value.pickingSession?.status)
        assertEquals(1, viewModel.uiState.value.pickingSession?.pointIndex)
    }

    @Test
    fun finishClearsPickingSession() = runTest(dispatcher) {
        val viewModel = viewModel(StubLocationProvider())
        advanceUntilIdle()

        viewModel.pickRoute(viewModel.uiState.value.routes.first())
        runCurrent()
        viewModel.finishRoute()

        assertEquals(null, viewModel.uiState.value.pickingSession)
    }

    private fun viewModel(locationProvider: LocationProvider): RouteSearchViewModel {
        return RouteSearchViewModel(
            searchRoutes = SearchRoutesUseCase(
                repository = StubRouteRepository(),
                locationProvider = locationProvider,
            ),
            routeTrackingProvider = StubRouteTrackingProvider(stepDelayMillis = 1_000L),
        )
    }
}
