package ua.nure.kryvko.hikeway.feature.routesearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
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

    private fun viewModel(locationProvider: LocationProvider): RouteSearchViewModel {
        return RouteSearchViewModel(
            SearchRoutesUseCase(
                repository = StubRouteRepository(),
                locationProvider = locationProvider,
            )
        )
    }
}
