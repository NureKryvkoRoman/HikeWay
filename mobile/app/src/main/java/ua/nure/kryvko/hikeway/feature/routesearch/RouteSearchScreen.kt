package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria

private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun RouteSearchScreen(viewModel: RouteSearchViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        RoutesMap(routes = state.routes)
        ResultsPanel(
            state = state,
            onFilterClick = { showFilters = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showFilters) {
        FilterDialog(
            criteria = state.draftCriteria,
            onCriteriaChange = viewModel::updateDraft,
            onDifficultyToggle = viewModel::toggleDifficulty,
            onTerrainToggle = viewModel::toggleTerrain,
            onClear = {
                viewModel.clearFilters()
                showFilters = false
            },
            onApply = {
                viewModel.applyFilters()
                showFilters = false
            },
            onDismiss = { showFilters = false },
        )
    }
}

@Composable
private fun RoutesMap(routes: List<Route>) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = 24.0316, latitude = 49.8429),
            zoom = 10.0,
        )
    )
    val geoJson = remember(routes) { routes.toGeoJson() }

    MaplibreMap(
        baseStyle = BaseStyle.Uri(MAP_STYLE),
        cameraState = cameraState,
        modifier = Modifier.fillMaxSize(),
    ) {
        val routesSource = rememberGeoJsonSource(GeoJsonData.JsonString(geoJson))
        LineLayer(
            id = "matching-routes",
            source = routesSource,
            color = const(Color(0xFF176B3A)),
            width = const(4.dp),
        )
    }
}

@Composable
private fun ResultsPanel(
    state: RouteSearchUiState,
    onFilterClick: () -> Unit,
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
                Text("${state.routes.size} routes found", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onFilterClick) {
                    Text("Filters")
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
                        RouteCard(route)
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteCard(route: Route) {
    Card(modifier = Modifier.fillMaxWidth()) {
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

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun FilterDialog(
    criteria: RouteSearchCriteria,
    onCriteriaChange: (RouteSearchCriteria) -> Unit,
    onDifficultyToggle: (Difficulty) -> Unit,
    onTerrainToggle: (Terrain) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    var minimumDistance by remember { mutableStateOf(criteria.distanceKm?.start?.toString().orEmpty()) }
    var maximumDistance by remember { mutableStateOf(criteria.distanceKm?.endInclusive?.toString().orEmpty()) }
    var minimumTime by remember { mutableStateOf(criteria.estimatedTimeMinutes?.first?.toString().orEmpty()) }
    var maximumTime by remember { mutableStateOf(criteria.estimatedTimeMinutes?.last?.toString().orEmpty()) }
    var maximumProximity by remember { mutableStateOf(criteria.maxProximityKm?.toString().orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Route filters") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Distance, km", style = MaterialTheme.typography.labelLarge)
                RangeFields(
                    minimum = minimumDistance,
                    maximum = maximumDistance,
                    onChange = { minimum, maximum ->
                        minimumDistance = minimum
                        maximumDistance = maximum
                        onCriteriaChange(criteria.copy(distanceKm = decimalRange(minimum, maximum)))
                    },
                )
                Text("Estimated time, minutes", style = MaterialTheme.typography.labelLarge)
                RangeFields(
                    minimum = minimumTime,
                    maximum = maximumTime,
                    onChange = { minimum, maximum ->
                        minimumTime = minimum
                        maximumTime = maximum
                        onCriteriaChange(criteria.copy(estimatedTimeMinutes = integerRange(minimum, maximum)))
                    },
                )
                OutlinedTextField(
                    value = maximumProximity,
                    onValueChange = {
                        maximumProximity = it
                        onCriteriaChange(criteria.copy(maxProximityKm = it.toDoubleOrNull()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Maximum distance from you, km") },
                    singleLine = true,
                )
                Text("Difficulty", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { difficulty ->
                        ChoiceChip(
                            label = difficulty.label(),
                            selected = difficulty in criteria.difficulties,
                            onClick = { onDifficultyToggle(difficulty) },
                        )
                    }
                }
                Text("Terrain", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Terrain.entries.forEach { terrain ->
                        ChoiceChip(
                            label = terrain.label(),
                            selected = terrain in criteria.terrains,
                            onClick = { onTerrainToggle(terrain) },
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onApply) { Text("Apply") } },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        ElevatedAssistChip(onClick = onClick, label = { Text(label) })
    } else {
        AssistChip(onClick = onClick, label = { Text(label) })
    }
}

@Composable
private fun RangeFields(
    minimum: String,
    maximum: String,
    onChange: (String, String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = minimum,
            onValueChange = { onChange(it, maximum) },
            modifier = Modifier.weight(1f),
            label = { Text("Min") },
            singleLine = true,
        )
        OutlinedTextField(
            value = maximum,
            onValueChange = { onChange(minimum, it) },
            modifier = Modifier.weight(1f),
            label = { Text("Max") },
            singleLine = true,
        )
    }
}

private fun decimalRange(minimum: String, maximum: String): ClosedFloatingPointRange<Double>? {
    val min = minimum.toDoubleOrNull() ?: return null
    val max = maximum.toDoubleOrNull() ?: return null
    return min..max
}

private fun integerRange(minimum: String, maximum: String): IntRange? {
    val min = minimum.toIntOrNull() ?: return null
    val max = maximum.toIntOrNull() ?: return null
    return min..max
}

private fun List<Route>.toGeoJson(): String {
    val features = joinToString(separator = ",") { route ->
        val coordinates = route.geometry.points.joinToString(separator = ",") {
            "[${it.longitude},${it.latitude}]"
        }
        """{"type":"Feature","properties":{"routeId":${route.id}},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

private fun Difficulty.label() = name.lowercase().replaceFirstChar(Char::uppercase)

private fun Terrain.label() = name.lowercase().replaceFirstChar(Char::uppercase)
