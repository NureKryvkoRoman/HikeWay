package ua.nure.kryvko.hikeway.feature.completedhikes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.ui.map.PathMap

@Composable
fun CompletedHikesScreen(viewModel: CompletedHikesViewModel) {
    val state by viewModel.uiState.collectAsState()
    val selectedHike = state.selectedHike

    BackHandler(enabled = selectedHike != null) {
        viewModel.dismissSelectedHike()
    }

    if (selectedHike != null) {
        CompletedHikeDetail(
            hike = selectedHike,
            onBack = viewModel::dismissSelectedHike,
        )
    } else {
        CompletedHikesList(
            state = state,
            onHikeClick = viewModel::selectHike,
        )
    }
}

@Composable
private fun CompletedHikesList(
    state: CompletedHikesUiState,
    onHikeClick: (HikeLog) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Completed hikes", style = MaterialTheme.typography.headlineSmall)
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.errorMessage != null -> Text(
                state.errorMessage,
                color = MaterialTheme.colorScheme.error,
            )
            state.hikes.isEmpty() -> Text("No completed hikes yet.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.hikes, key = { it.id }) { hike ->
                    CompletedHikeCard(hike = hike, onClick = { onHikeClick(hike) })
                }
            }
        }
    }
}

@Composable
private fun CompletedHikeCard(
    hike: HikeLog,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(hike.routeName, style = MaterialTheme.typography.titleSmall)
            Text("Completed at ${hike.finishedAtEpochMillis.formatCompletedAt()}")
            Text(
                "${hike.totalDistanceKm.formatDistance()} | " +
                    "active ${hike.activeDurationMillis.formatDuration()} | " +
                    "total ${hike.wallClockDurationMillis.formatDuration()}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CompletedHikeDetail(
    hike: HikeLog,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PathMap(
            path = hike.path,
            modifier = Modifier.weight(1f),
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Completed hike", style = MaterialTheme.typography.labelLarge)
                Text(hike.routeName, style = MaterialTheme.typography.titleMedium)
                HikeInfoRow("Distance", hike.totalDistanceKm.formatDistance())
                HikeInfoRow("Active time", hike.activeDurationMillis.formatDuration())
                HikeInfoRow("Total time", hike.wallClockDurationMillis.formatDuration())
                HikeInfoRow("Completed at", hike.finishedAtEpochMillis.formatCompletedAt())
                HikeInfoRow("Path points", hike.path.size.toString())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                    Button(onClick = onBack) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun HikeInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun Long.formatCompletedAt(): String {
    return completedAtFormatter.format(
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
    )
}

private val completedAtFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.US)

private fun Long.formatDuration(): String {
    val totalSeconds = this / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(Locale.US, hours, minutes, seconds)
}

private fun Double.formatDistance(): String {
    return "%.2f km".format(Locale.US, this)
}
