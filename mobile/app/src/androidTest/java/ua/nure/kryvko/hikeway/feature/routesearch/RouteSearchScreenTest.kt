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
import ua.nure.kryvko.hikeway.data.routes.stub.StubRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

class RouteSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        val viewModel = RouteSearchViewModel(
            SearchRoutesUseCase(
                repository = StubRouteRepository(),
                locationProvider = StubLocationProvider(),
            )
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
}
