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
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.routes.CustomRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.SaveCustomRouteUseCase

@OptIn(ExperimentalCoroutinesApi::class)
class RouteCreationViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeCustomRouteRepository
    private lateinit var pointOfInterestRepository: FakePointOfInterestRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCustomRouteRepository()
        pointOfInterestRepository = FakePointOfInterestRepository()
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

    @Test
    fun addSelectedPoiAppendsPoiLocationAsRoutePoint() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectPoi(201)
        viewModel.addSelectedPoiToRoute()

        assertEquals(
            listOf(GeoPoint(longitude = 24.2, latitude = 49.9)),
            viewModel.uiState.value.points,
        )
        assertEquals(null, viewModel.uiState.value.selectedPoi)
    }

    @Test
    fun ratePointOfInterestUpdatesUiAndSubmitsRating() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.selectPoi(201)
        viewModel.ratePoi(4)
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.selectedPoi?.userRating)
        assertEquals(201L to 4, pointOfInterestRepository.submittedRatings.single())
    }

    private fun placeTwoPoints(viewModel: RouteCreationViewModel) {
        viewModel.updateCrosshair(GeoPoint(longitude = 24.0, latitude = 49.0))
        viewModel.placePoint()
        viewModel.updateCrosshair(GeoPoint(longitude = 24.01, latitude = 49.0))
        viewModel.placePoint()
    }

    private fun viewModel() = RouteCreationViewModel(
        saveCustomRoute = SaveCustomRouteUseCase(repository),
        getPointsOfInterest = GetPointsOfInterestUseCase(pointOfInterestRepository),
        ratePointOfInterest = RatePointOfInterestUseCase(pointOfInterestRepository),
    )
}

private class FakeCustomRouteRepository : CustomRouteRepository {
    val saved = mutableListOf<Route>()

    override suspend fun save(route: Route): Long {
        saved += route
        return saved.size.toLong()
    }
}

private class FakePointOfInterestRepository : PointOfInterestRepository {
    val submittedRatings = mutableListOf<Pair<Long, Int>>()

    override suspend fun getPointsOfInterest(): List<PointOfInterest> {
        return listOf(
            PointOfInterest(
                id = 201,
                name = "Ridge marker",
                description = "A route waypoint.",
                location = GeoPoint(longitude = 24.2, latitude = 49.9),
                photoResIds = emptyList(),
                averageRating = 4.7,
            )
        )
    }

    override suspend fun submitRating(poiId: Long, rating: Int): PoiRating {
        submittedRatings += poiId to rating
        return PoiRating(rating.toDouble(), 1, rating)
    }
}
