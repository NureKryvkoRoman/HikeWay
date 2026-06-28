package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.core.location.StubLocationProvider
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemTimeProvider
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.AddPoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.CreatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetNearbyPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetPointOfInterestDetailUseCase
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.RemovePointOfInterestRatingUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.UploadPoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.routes.GetCurrentLocationUseCase
import ua.nure.kryvko.hikeway.domain.routes.RouteRepository
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase
import ua.nure.kryvko.hikeway.domain.routes.matches
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class RouteSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        val locationProvider = StubLocationProvider()
        val poiRepository = FakePointOfInterestRepository()
        val viewModel = RouteSearchViewModel(
            searchRoutes = SearchRoutesUseCase(
                repository = FakeRouteRepository(),
                locationProvider = locationProvider,
            ),
            getCurrentLocation = GetCurrentLocationUseCase(locationProvider),
            routeTrackingProvider = StubRouteTrackingProvider(),
            saveCompletedHike = SaveCompletedHikeUseCase(FakeHikeLogRepository()),
            timeProvider = SystemTimeProvider(),
            activeTimer = NoOpActiveTimer(),
            getPointsOfInterest = GetPointsOfInterestUseCase(poiRepository),
            getNearbyPointsOfInterest = GetNearbyPointsOfInterestUseCase(poiRepository),
            getPointOfInterestDetail = GetPointOfInterestDetailUseCase(poiRepository),
            createPointOfInterest = CreatePointOfInterestUseCase(poiRepository),
            updatePointOfInterest = UpdatePointOfInterestUseCase(poiRepository),
            deletePointOfInterest = DeletePointOfInterestUseCase(poiRepository),
            ratePointOfInterest = RatePointOfInterestUseCase(poiRepository),
            removePointOfInterestRating = RemovePointOfInterestRatingUseCase(poiRepository),
            addPoiCommentUseCase = AddPoiCommentUseCase(poiRepository),
            updatePoiCommentUseCase = UpdatePoiCommentUseCase(poiRepository),
            deletePoiCommentUseCase = DeletePoiCommentUseCase(poiRepository),
            uploadPoiPhotoUseCase = UploadPoiPhotoUseCase(poiRepository),
            updatePoiPhotoUseCase = UpdatePoiPhotoUseCase(poiRepository),
            deletePoiPhotoUseCase = DeletePoiPhotoUseCase(poiRepository),
        )
        composeRule.setContent {
            HikeWayTheme {
                RouteSearchScreen(viewModel)
            }
        }
        composeRule.waitUntil { composeRule.onAllNodes(hasText("5 routes found")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun appliesDifficultyFilterAndUpdatesCards() {
        composeRule.onNodeWithText("Filters").performClick()
        composeRule.onNodeWithText("Route filters").assertIsDisplayed()
        composeRule.onNodeWithText("Hard").performClick()
        composeRule.onNodeWithText("Apply").performClick()

        composeRule.onNodeWithText("1 routes found").assertIsDisplayed()
        composeRule.onNodeWithText("Rocky Ridge Traverse").assertIsDisplayed()
    }

    @Test
    fun showsEmptyStateForUnmatchedFilters() {
        composeRule.onNodeWithText("Filters").performClick()
        composeRule.onNodeWithText("Hard").performClick()
        composeRule.onNodeWithText("Maximum distance from you, km").performTextInput("0.1")
        composeRule.onNodeWithText("Apply").performClick()

        composeRule.onNodeWithText("No routes match the applied filters.").assertIsDisplayed()
    }

    @Test
    fun routeCardShowsPreviewAndBackReturnsToResults() {
        composeRule.onNodeWithText("High Castle Loop").performClick()

        composeRule.onNodeWithText("Route overview").assertIsDisplayed()
        composeRule.onNodeWithText("A short city-edge climb with a panoramic viewpoint.").assertIsDisplayed()
        composeRule.onNodeWithText("Start route").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.onNodeWithText("5 routes found").assertIsDisplayed()
    }

    @Test
    fun startRouteFromPreviewShowsPauseThenUnpauseAndFinishControls() {
        composeRule.onNodeWithText("High Castle Loop").performClick()
        composeRule.onNodeWithText("Start route").performClick()

        composeRule.onNodeWithText("Picked route").assertIsDisplayed()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()

        composeRule.onNodeWithText("Pause").performClick()
        composeRule.onNodeWithText("Unpause").assertIsDisplayed()
        composeRule.onNodeWithText("Finish").assertIsDisplayed()

        composeRule.onNodeWithText("Finish").performClick()
        composeRule.onNodeWithText("5 routes found").assertIsDisplayed()
    }

    @Test
    fun pickingRouteShowsHikeStats() {
        composeRule.onNodeWithText("High Castle Loop").performClick()
        composeRule.onNodeWithText("Start route").performClick()

        composeRule.onNodeWithText("Active time: 00:00:00").assertIsDisplayed()
        composeRule.onNodeWithText("Walked distance: 0.00 km").assertIsDisplayed()
    }

    @Test
    fun routePreviewShowsMetadata() {
        composeRule.onNodeWithText("High Castle Loop").performClick()

        composeRule.onNodeWithText("Distance").assertIsDisplayed()
        composeRule.onNodeWithText("4.8 km").assertIsDisplayed()
        composeRule.onNodeWithText("Estimated time").assertIsDisplayed()
        composeRule.onNodeWithText("95 min").assertIsDisplayed()
        composeRule.onNodeWithText("Elevation gain").assertIsDisplayed()
        composeRule.onNodeWithText("140 m").assertIsDisplayed()
        composeRule.onNodeWithText("Difficulty").assertIsDisplayed()
        composeRule.onNodeWithText("Easy").assertIsDisplayed()
        composeRule.onNodeWithText("Terrain").assertIsDisplayed()
        composeRule.onNodeWithText("Forest").assertIsDisplayed()
    }
}

private class NoOpActiveTimer : ActiveTimer {
    override fun ticks(periodMillis: Long): Flow<Long> = flowOf()
}

private class FakeHikeLogRepository : HikeLogRepository {
    override suspend fun save(log: HikeLog): Long = 1L

    override fun observeAll(): Flow<List<HikeLog>> = emptyFlow()
}

private class FakeRouteRepository : RouteRepository {
    private val routes = listOf(
        route(
            1,
            "High Castle Loop",
            "A short city-edge climb with a panoramic viewpoint.",
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
            "A rolling forest route with quiet paths and several lakes.",
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
            "A longer exposed ridge route for experienced hikers.",
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
            "A steady climb through meadows and mixed woodland.",
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
            "An accessible mixed-terrain route for a relaxed afternoon.",
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

private fun route(
    id: Long,
    name: String,
    description: String,
    distanceKm: Double,
    estimatedTimeMinutes: Int,
    difficulty: Difficulty,
    elevationGainMeters: Int,
    terrain: Terrain,
    vararg points: Pair<Double, Double>,
) = Route(
    id = id,
    name = name,
    description = description,
    distanceKm = distanceKm,
    estimatedTimeMinutes = estimatedTimeMinutes,
    difficulty = difficulty,
    elevationGainMeters = elevationGainMeters,
    terrain = terrain,
    geometry = RouteGeometry(points.map { (longitude, latitude) -> GeoPoint(longitude, latitude) }),
)

private class FakePointOfInterestRepository : PointOfInterestRepository {
    override suspend fun getPointsOfInterest(): List<PointOfInterest> {
        return listOf(
            PointOfInterest(
                id = 1,
                name = "Test PoI",
                description = "A test point.",
                location = GeoPoint(longitude = 24.0, latitude = 49.0),
                photoResIds = emptyList(),
                averageRating = 4.0,
            )
        )
    }

    override suspend fun submitRating(poiId: Long, rating: Int): PoiRating {
        return PoiRating(rating.toDouble(), 1, rating)
    }
}
