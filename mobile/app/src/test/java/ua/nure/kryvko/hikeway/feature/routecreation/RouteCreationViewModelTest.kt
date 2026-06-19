package ua.nure.kryvko.hikeway.feature.routecreation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routes.CustomRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.SaveCustomRouteUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class RouteCreationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCustomRouteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCustomRouteRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun placesCurrentCrosshairPoint() = runTest(dispatcher) {
        val viewModel = viewModel()
        val point = GeoPoint(longitude = 24.1, latitude = 49.8)

        viewModel.updateCrosshair(point)
        viewModel.placePoint()

        assertEquals(listOf(point), viewModel.uiState.value.points)
    }

    @Test
    fun finishRequiresAtLeastTwoPoints() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.placePoint()

        viewModel.finishMapStep()

        assertEquals(RouteCreationStep.MAP, viewModel.uiState.value.step)
        assertNotNull(viewModel.uiState.value.validationMessage)
    }

    @Test
    fun calculatesEstimatedTimeAtFourKilometersPerHour() {
        assertEquals(30, estimateTimeMinutes(2.0))
        assertEquals(31, estimateTimeMinutes(2.01))
    }

    @Test
    fun saveRequiresRouteDetails() = runTest(dispatcher) {
        val viewModel = viewModel()
        placeTwoPoints(viewModel)
        viewModel.finishMapStep()

        viewModel.saveRoute()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.validationMessage)
        assertEquals(emptyList<Route>(), repository.saved)
    }

    @Test
    fun savePersistsRouteWithCalculatedFieldsAndPlaceholderElevation() = runTest(dispatcher) {
        val viewModel = viewModel()
        placeTwoPoints(viewModel)
        viewModel.finishMapStep()
        viewModel.updateName("My route")
        viewModel.updateDescription("Local route")
        viewModel.selectDifficulty(Difficulty.EASY)
        viewModel.selectTerrain(Terrain.FOREST)

        viewModel.saveRoute()
        advanceUntilIdle()

        val saved = repository.saved.single()
        assertEquals("My route", saved.name)
        assertEquals("Local route", saved.description)
        assertEquals(Difficulty.EASY, saved.difficulty)
        assertEquals(Terrain.FOREST, saved.terrain)
        assertEquals(0, saved.elevationGainMeters)
        assertEquals(2, saved.geometry.points.size)
        assertEquals(11, saved.estimatedTimeMinutes)
        assertEquals(true, viewModel.uiState.value.didSave)
    }

    private fun placeTwoPoints(viewModel: RouteCreationViewModel) {
        viewModel.updateCrosshair(GeoPoint(longitude = 24.0, latitude = 49.0))
        viewModel.placePoint()
        viewModel.updateCrosshair(GeoPoint(longitude = 24.01, latitude = 49.0))
        viewModel.placePoint()
    }

    private fun viewModel() = RouteCreationViewModel(
        saveCustomRoute = SaveCustomRouteUseCase(repository),
    )
}

private class FakeCustomRouteRepository : CustomRouteRepository {
    val saved = mutableListOf<Route>()

    override suspend fun save(route: Route): Long {
        saved += route
        return saved.size.toLong()
    }
}
