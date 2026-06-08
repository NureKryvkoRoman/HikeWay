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
import ua.nure.kryvko.hikeway.core.location.StubLocationProvider
import ua.nure.kryvko.hikeway.data.routepicking.stub.StubRouteTrackingProvider
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.SystemTimeProvider
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

class RouteSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        val viewModel = RouteSearchViewModel(
            searchRoutes = SearchRoutesUseCase(
                repository = StubRouteRepository(),
                locationProvider = StubLocationProvider(),
            ),
            routeTrackingProvider = StubRouteTrackingProvider(),
            saveCompletedHike = SaveCompletedHikeUseCase(FakeHikeLogRepository()),
            timeProvider = SystemTimeProvider(),
            activeTimer = NoOpActiveTimer(),
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
    fun pickingRouteShowsPauseThenUnpauseAndFinishControls() {
        composeRule.onNodeWithText("High Castle Loop").performClick()

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

        composeRule.onNodeWithText("Active time: 00:00:00").assertIsDisplayed()
        composeRule.onNodeWithText("Walked distance: 0.00 km").assertIsDisplayed()
    }
}

private class NoOpActiveTimer : ActiveTimer {
    override fun ticks(periodMillis: Long): Flow<Long> = flowOf()
}

private class FakeHikeLogRepository : HikeLogRepository {
    override suspend fun save(log: HikeLog): Long = 1L

    override fun observeAll(): Flow<List<HikeLog>> = emptyFlow()
}
