package ua.nure.kryvko.hikeway.feature.routecreation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.PointOfInterestRepository
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
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
            getPointsOfInterest = GetPointsOfInterestUseCase(FakePointOfInterestRepository()),
            ratePointOfInterest = RatePointOfInterestUseCase(FakePointOfInterestRepository()),
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
