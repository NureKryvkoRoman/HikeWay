package ua.nure.kryvko.hikeway.feature.completedhikes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLogRepository
import ua.nure.kryvko.hikeway.domain.hikelogging.ObserveCompletedHikesUseCase
import ua.nure.kryvko.hikeway.ui.theme.HikeWayTheme

class CompletedHikesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsEmptyState() {
        setContent(emptyList())

        composeRule.onNodeWithText("No completed hikes yet.").assertIsDisplayed()
    }

    @Test
    fun showsCompletedHikeSummary() {
        setContent(listOf(hikeLog(routeName = "High Castle Loop")))

        composeRule.onNodeWithText("High Castle Loop").assertIsDisplayed()
        composeRule.onNodeWithText("0.42 km | active 00:00:03 | total 00:00:04").assertIsDisplayed()
    }

    @Test
    fun tappingHikeOpensDetailAndBackReturnsToList() {
        setContent(listOf(hikeLog(routeName = "High Castle Loop")))

        composeRule.onNodeWithText("High Castle Loop").performClick()
        composeRule.onNodeWithText("Completed hike").assertIsDisplayed()
        composeRule.onNodeWithText("Distance").assertIsDisplayed()
        composeRule.onNodeWithText("Active time").assertIsDisplayed()
        composeRule.onNodeWithText("Total time").assertIsDisplayed()
        composeRule.onNodeWithText("Completed at").assertIsDisplayed()
        composeRule.onNodeWithText("Path points").assertIsDisplayed()

        composeRule.onNodeWithText("Back").performClick()
        composeRule.waitUntil {
            composeRule.onAllNodes(hasText("Completed hikes")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("High Castle Loop").assertIsDisplayed()
    }

    private fun setContent(hikes: List<HikeLog>) {
        val viewModel = CompletedHikesViewModel(
            observeCompletedHikes = ObserveCompletedHikesUseCase(FakeHikeLogRepository(hikes)),
        )
        composeRule.setContent {
            HikeWayTheme {
                CompletedHikesScreen(viewModel)
            }
        }
        composeRule.waitUntil {
            composeRule.onAllNodes(hasText("Completed hikes")).fetchSemanticsNodes().isNotEmpty()
        }
    }
}

private class FakeHikeLogRepository(hikes: List<HikeLog>) : HikeLogRepository {
    private val logs = MutableStateFlow(hikes)

    override suspend fun save(log: HikeLog): Long = error("Not used")

    override fun observeAll(): Flow<List<HikeLog>> = logs
}

private fun hikeLog(routeName: String) = HikeLog(
    id = 1,
    routeId = 1,
    routeName = routeName,
    startedAtEpochMillis = 1_000,
    finishedAtEpochMillis = 5_000,
    activeDurationMillis = 3_000,
    wallClockDurationMillis = 4_000,
    totalDistanceKm = 0.42,
    path = listOf(
        GeoPoint(longitude = 24.0316, latitude = 49.8429),
        GeoPoint(longitude = 24.0394, latitude = 49.8461),
    ),
)
