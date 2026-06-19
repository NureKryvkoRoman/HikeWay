package ua.nure.kryvko.hikeway.feature.routecreation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import java.util.Locale
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Terrain

private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun RouteCreationScreen(
    viewModel: RouteCreationViewModel,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.didSave) {
        if (state.didSave) {
            viewModel.reset()
            onSaved()
        }
    }

    BackHandler {
        when (state.step) {
            RouteCreationStep.MAP -> onCancel()
            RouteCreationStep.DETAILS -> viewModel.backToMap()
        }
    }

    when (state.step) {
        RouteCreationStep.MAP -> RouteBuilderScreen(
            state = state,
            onCrosshairChange = viewModel::updateCrosshair,
            onCancel = onCancel,
            onPlacePoint = viewModel::placePoint,
            onFinish = viewModel::finishMapStep,
        )
        RouteCreationStep.DETAILS -> RouteDetailsScreen(
            state = state,
            onBack = viewModel::backToMap,
            onCancel = onCancel,
            onNameChange = viewModel::updateName,
            onDescriptionChange = viewModel::updateDescription,
            onDifficultySelect = viewModel::selectDifficulty,
            onTerrainSelect = viewModel::selectTerrain,
            onSave = viewModel::saveRoute,
        )
    }
}

@Composable
private fun RouteBuilderScreen(
    state: RouteCreationUiState,
    onCrosshairChange: (GeoPoint) -> Unit,
    onCancel: () -> Unit,
    onPlacePoint: () -> Unit,
    onFinish: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        RouteBuilderMap(
            points = state.points,
            crosshairPoint = state.crosshairPoint,
            onCrosshairChange = onCrosshairChange,
        )
        Crosshair(modifier = Modifier.align(Alignment.Center))
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Create route", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Placed points: ${state.points.size} | Distance: ${state.distanceKm.formatDistance()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.validationMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Button(onClick = onPlacePoint) {
                        Text("Place a point")
                    }
                    Button(onClick = onFinish) {
                        Text("Finish")
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteBuilderMap(
    points: List<GeoPoint>,
    crosshairPoint: GeoPoint,
    onCrosshairChange: (GeoPoint) -> Unit,
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = crosshairPoint.longitude, latitude = crosshairPoint.latitude),
            zoom = 12.0,
        )
    )
    val solidGeoJson = remember(points) { points.toLineFeatureGeoJson() }
    val previewGeoJson = remember(points, crosshairPoint) {
        points.lastOrNull()
            ?.let { listOf(it, crosshairPoint).toLineFeatureGeoJson() }
            ?: emptyFeatureCollection()
    }

    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.position.target }
            .distinctUntilChanged()
            .collect { target ->
                onCrosshairChange(GeoPoint(longitude = target.longitude, latitude = target.latitude))
            }
    }

    MaplibreMap(
        baseStyle = BaseStyle.Uri(MAP_STYLE),
        cameraState = cameraState,
        modifier = Modifier.fillMaxSize(),
    ) {
        if (points.size >= 2) {
            val solidSource = rememberGeoJsonSource(GeoJsonData.JsonString(solidGeoJson))
            LineLayer(
                id = "created-route",
                source = solidSource,
                color = const(Color(0xFF0B57D0)),
                width = const(5.dp),
            )
        }
        if (points.isNotEmpty()) {
            val previewSource = rememberGeoJsonSource(GeoJsonData.JsonString(previewGeoJson))
            LineLayer(
                id = "route-preview-segment",
                source = previewSource,
                color = const(Color(0xFFD93025)),
                dasharray = const(listOf(1.5, 1.5)),
                width = const(4.dp),
            )
        }
    }
}

@Composable
private fun Crosshair(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
        Box(
            modifier = Modifier
                .size(width = 2.dp, height = 40.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RouteDetailsScreen(
    state: RouteCreationUiState,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDifficultySelect: (Difficulty) -> Unit,
    onTerrainSelect: (Terrain) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Route details", style = MaterialTheme.typography.headlineSmall)
        Text("Distance: ${state.distanceKm.formatDistance()}")
        Text("Estimated time: ${state.estimatedTimeMinutes} min")
        Text("Elevation gain: 0 m")
        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Route name") },
            singleLine = true,
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description") },
            minLines = 3,
        )
        Text("Difficulty", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { difficulty ->
                ChoiceChip(
                    label = difficulty.label(),
                    selected = state.difficulty == difficulty,
                    onClick = { onDifficultySelect(difficulty) },
                )
            }
        }
        Text("Terrain", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Terrain.entries.forEach { terrain ->
                ChoiceChip(
                    label = terrain.label(),
                    selected = state.terrain == terrain,
                    onClick = { onTerrainSelect(terrain) },
                )
            }
        }
        state.validationMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.saveErrorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = onSave, enabled = !state.isSaving) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                } else {
                    Text("Save route")
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        ElevatedAssistChip(onClick = onClick, label = { Text(label) })
    } else {
        AssistChip(onClick = onClick, label = { Text(label) })
    }
}

private fun List<GeoPoint>.toLineFeatureGeoJson(): String {
    if (size < 2) return emptyFeatureCollection()
    val coordinates = joinToString(separator = ",") {
        "[${it.longitude},${it.latitude}]"
    }
    return """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
}

private fun emptyFeatureCollection() = """{"type":"FeatureCollection","features":[]}"""

private fun Difficulty.label() = name.lowercase().replaceFirstChar(Char::uppercase)

private fun Terrain.label() = name.lowercase().replaceFirstChar(Char::uppercase)

private fun Double.formatDistance(): String {
    return "%.2f km".format(Locale.US, this)
}
