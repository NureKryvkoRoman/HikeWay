package ua.nure.kryvko.hikeway.feature.routecreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.routes.CustomRouteRepository
import ua.nure.kryvko.hikeway.domain.routes.SaveCustomRouteUseCase
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

class RouteCreationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun builderRequiresTwoPointsBeforeDetails() {
        setContent()

        composeRule.onNodeWithText("Finish").performClick()

        composeRule.onNodeWithText("Place at least two points to finish the route.").assertIsDisplayed()
    }

    @Test
    fun canReachDetailsAndEnterRouteFields() {
        setContent()

        composeRule.onNodeWithText("Place a point").performClick()
        composeRule.onNodeWithText("Place a point").performClick()
        composeRule.onNodeWithText("Finish").performClick()

        composeRule.onNodeWithText("Route details").assertIsDisplayed()
        composeRule.onNodeWithText("Route name").performTextInput("My route")
        composeRule.onNodeWithText("Description").performTextInput("Created locally")
        composeRule.onNodeWithText("Easy").performClick()
        composeRule.onNodeWithText("Forest").performClick()
        composeRule.onNodeWithText("Save route").assertIsDisplayed()
    }

    private fun setContent() {
        val viewModel = RouteCreationViewModel(
            saveCustomRoute = SaveCustomRouteUseCase(FakeCustomRouteRepository()),
        )
        composeRule.setContent {
            HikeWayTheme {
                RouteCreationScreen(
                    viewModel = viewModel,
                    onCancel = {},
                    onSaved = {},
                )
            }
        }
    }
}

private class FakeCustomRouteRepository : CustomRouteRepository {
    override suspend fun save(route: Route): Long = 1L
}
