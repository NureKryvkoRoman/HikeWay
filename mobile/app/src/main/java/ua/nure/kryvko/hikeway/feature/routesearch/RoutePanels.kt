package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingSession
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus

@Composable
fun RoutePreviewPanel(
    route: Route,
    onBack: () -> Unit,
    onStartRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Route overview", style = MaterialTheme.typography.labelLarge)
            Text(route.name, style = MaterialTheme.typography.titleMedium)
            Text(route.description, style = MaterialTheme.typography.bodyMedium)
            RouteInfoRow("Distance", "${route.distanceKm} km")
            RouteInfoRow("Estimated time", "${route.estimatedTimeMinutes} min")
            RouteInfoRow("Elevation gain", "${route.elevationGainMeters} m")
            RouteInfoRow("Difficulty", route.difficulty.label())
            RouteInfoRow("Terrain", route.terrain.label())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
                Button(onClick = onStartRoute) {
                    Text("Start route")
                }
            }
        }
    }
}

@Composable
fun ResultsPanel(
    state: RouteSearchUiState,
    onRouteClick: (Route) -> Unit,
    onFilterClick: () -> Unit,
    onCreateRoute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp),
        tonalElevation = 8.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.routes.size} routes found",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCreateRoute) {
                        Text("Create route")
                    }
                    Button(onClick = onFilterClick) {
                        Text("Filters")
                    }
                }
            }
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                state.errorMessage != null -> Text(
                    state.errorMessage,
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.error,
                )

                state.routes.isEmpty() -> Text(
                    "No routes match the applied filters.",
                    modifier = Modifier.padding(vertical = 16.dp),
                )

                else -> LazyColumn(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.routes, key = { it.id }) { route ->
                        RouteCard(route, onClick = { onRouteClick(route) })
                    }
                }
            }
        }
    }
}

@Composable
fun PickingPanel(
    session: RoutePickingSession,
    saveErrorMessage: String?,
    onPause: () -> Unit,
    onUnpause: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Picked route", style = MaterialTheme.typography.labelLarge)
            Text(session.route.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${session.route.distanceKm} km | ${session.route.estimatedTimeMinutes} min | " +
                    "${session.route.elevationGainMeters} m gain",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Position arrow: ${session.bearingDegrees.toInt()}° | ${session.status.label()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Active time: ${session.activeElapsedMillis.formatDuration()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Walked distance: ${session.walkedDistanceKm.formatDistance()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (saveErrorMessage != null) {
                Text(
                    saveErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (session.status) {
                    RoutePickingStatus.ACTIVE -> Button(onClick = onPause) {
                        Text("Pause")
                    }

                    RoutePickingStatus.PAUSED -> {
                        Button(onClick = onUnpause) {
                            Text("Unpause")
                        }
                        TextButton(onClick = onFinish) {
                            Text("Finish")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RouteCard(route: Route, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(route.name, style = MaterialTheme.typography.titleSmall)
            Text(
                "${route.distanceKm} km | ${route.estimatedTimeMinutes} min | " +
                    "${route.elevationGainMeters} m gain",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                "${route.difficulty.label()} | ${route.terrain.label()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
