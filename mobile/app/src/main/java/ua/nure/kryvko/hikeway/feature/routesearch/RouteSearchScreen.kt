package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.expressions.value.IconRotationAlignment
import org.maplibre.compose.util.ClickResult
import ua.nure.kryvko.hikeway.R
import java.util.Locale
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingSession
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.feature.pois.PoiOverviewPopup
import ua.nure.kryvko.hikeway.ui.map.HikeWayMap
import ua.nure.kryvko.hikeway.ui.map.MapCenterMode
import ua.nure.kryvko.hikeway.ui.map.MapCenterRequest
import ua.nure.kryvko.hikeway.ui.map.toLineStringFeatureGeoJson
import ua.nure.kryvko.hikeway.ui.map.toPoiFeatureCollectionGeoJson
import ua.nure.kryvko.hikeway.ui.map.toPointFeatureGeoJson
import ua.nure.kryvko.hikeway.ui.map.toRouteFeatureCollectionGeoJson

@Composable
fun RouteSearchScreen(
    viewModel: RouteSearchViewModel,
    onCreateRoute: () -> Unit = {},
    isAdmin: Boolean = false,
) {
    val state by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    val pickingSession = state.pickingSession
    val previewRoute = state.previewRoute

    BackHandler(enabled = previewRoute != null) {
        viewModel.dismissRoutePreview()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RoutesMap(
            routes = when {
                pickingSession != null -> listOf(pickingSession.route)
                previewRoute != null -> listOf(previewRoute)
                else -> state.routes
            },
            walkedPath = pickingSession?.walkedPath ?: emptyList(),
            userPosition = pickingSession?.userPosition,
            userBearingDegrees = pickingSession?.bearingDegrees ?: 0.0,
            pointsOfInterest = state.pointsOfInterest,
            mapCenter = state.mapCenter,
            mapCenterRequestId = state.mapCenterRequestId,
            onCenterLocation = viewModel::centerOnCurrentLocation,
            onPoiClick = viewModel::selectPoi,
            centerMode = previewRoute
                ?.takeIf { pickingSession == null }
                ?.let { MapCenterMode.RouteStart(it.geometry.points) }
                ?: MapCenterMode.CurrentLocation,
            highlightedMode = pickingSession != null || previewRoute != null,
        )
        if (pickingSession != null) {
            PickingPanel(
                session = pickingSession,
                saveErrorMessage = state.saveErrorMessage,
                onPause = viewModel::pauseRoute,
                onUnpause = viewModel::unpauseRoute,
                onFinish = viewModel::finishRoute,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else if (previewRoute != null) {
            RoutePreviewPanel(
                route = previewRoute,
                onBack = viewModel::dismissRoutePreview,
                onStartRoute = viewModel::startPreviewedRoute,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        } else {
            ResultsPanel(
                state = state,
                onRouteClick = viewModel::previewRoute,
                onFilterClick = { showFilters = true },
                onCreateRoute = onCreateRoute,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        state.selectedPoi?.let { poi ->
            PoiOverviewPopup(
                poi = poi,
                onDismiss = viewModel::dismissPoi,
                onRate = viewModel::ratePoi,
                isLoading = state.isPoiLoading,
                isActionInProgress = state.isPoiActionInProgress,
                errorMessage = state.poiErrorMessage,
                onRemoveRating = viewModel::removePoiRating,
                onAddComment = viewModel::addPoiComment,
                onUpdateComment = viewModel::updatePoiComment,
                onDeleteComment = viewModel::deletePoiComment,
                onUploadPhoto = viewModel::uploadPoiPhoto,
                onUpdatePhoto = viewModel::updatePoiPhoto,
                onDeletePhoto = viewModel::deletePoiPhoto,
                onUpdatePoi = viewModel::updateSelectedPoi,
                onDeletePoi = viewModel::deleteSelectedPoi,
                isAdmin = isAdmin,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
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
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
private fun RoutesMap(
    routes: List<Route>,
    walkedPath: List<GeoPoint>,
    userPosition: GeoPoint?,
    userBearingDegrees: Double,
    pointsOfInterest: List<PointOfInterest>,
    mapCenter: GeoPoint?,
    mapCenterRequestId: Long,
    onCenterLocation: () -> Unit,
    onPoiClick: (Long) -> Unit,
    centerMode: MapCenterMode,
    highlightedMode: Boolean,
) {
    val geoJson = remember(routes) { routes.toRouteFeatureCollectionGeoJson() }
    val walkedPathGeoJson = remember(walkedPath) { walkedPath.toLineStringFeatureGeoJson() }
    val userGeoJson = remember(userPosition) { userPosition.toPointFeatureGeoJson() }
    val poiGeoJson = remember(pointsOfInterest) { pointsOfInterest.toPoiFeatureCollectionGeoJson() }
    val arrowPainter = rememberVectorPainter(Icons.Default.KeyboardArrowUp)
    val poiPainter = painterResource(R.drawable.poi_marker_24)
    val currentLocationCenterRequest = mapCenter?.let {
        MapCenterRequest(
            target = it,
            zoom = 14.0,
            requestId = mapCenterRequestId,
        )
    }
    val routeStartCenterRequest = (centerMode as? MapCenterMode.RouteStart)
        ?.points
        ?.firstOrNull()
        ?.let {
            MapCenterRequest(
                target = it,
                zoom = 12.0,
                requestId = it.hashCode().toLong(),
            )
        }

    HikeWayMap(
        centerMode = centerMode,
        currentLocation = mapCenter,
        centerRequest = routeStartCenterRequest ?: currentLocationCenterRequest,
        overlays = {
            Button(
                onClick = onCenterLocation,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(70.dp)
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.center_24),
                    contentDescription = "Center location button",
                )
            }
        },
    ) {
            val routesSource = rememberGeoJsonSource(GeoJsonData.JsonString(geoJson))
            LineLayer(
                id = "matching-routes",
                source = routesSource,
                color = const(if (highlightedMode) Color(0xFF0B57D0) else Color(0xFF176B3A)),
                width = const(if (highlightedMode) 6.dp else 4.dp),
            )
            if (walkedPath.isNotEmpty()) {
                val walkedPathSource =
                    rememberGeoJsonSource(GeoJsonData.JsonString(walkedPathGeoJson))
                LineLayer(
                    id = "walked-path",
                    source = walkedPathSource,
                    color = const(Color(0xFFD93025)),
                    width = const(4.dp),
                )
            }
            if (userPosition != null) {
                val userSource = rememberGeoJsonSource(GeoJsonData.JsonString(userGeoJson))
                SymbolLayer(
                    id = "user-position",
                    source = userSource,
                    iconImage = image(
                        value = arrowPainter,
                        size = DpSize(32.dp, 32.dp),
                        drawAsSdf = true,
                    ),
                    iconColor = const(Color(0xFFD93025)),
                    iconHaloColor = const(Color.White),
                    iconHaloWidth = const(2.dp),
                    iconAllowOverlap = const(true),
                    iconIgnorePlacement = const(true),
                    iconRotationAlignment = const(IconRotationAlignment.Map),
                    iconRotate = const(userBearingDegrees.toFloat()),
                )
            }
            if (pointsOfInterest.isNotEmpty()) {
                val poiSource = rememberGeoJsonSource(GeoJsonData.JsonString(poiGeoJson))
                SymbolLayer(
                    id = "points-of-interest",
                    source = poiSource,
                    iconImage = image(
                        value = poiPainter,
                        size = DpSize(34.dp, 34.dp),
                        drawAsSdf = true,
                    ),
                    iconColor = const(Color(0xFFB3261E)),
                    iconHaloColor = const(Color.White),
                    iconHaloWidth = const(2.dp),
                    iconAllowOverlap = const(true),
                    iconIgnorePlacement = const(true),
                    onClick = { features ->
                        val poiId = features.firstOrNull()
                            ?.properties
                            ?.get("poiId")
                            ?.jsonPrimitive
                            ?.longOrNull
                        if (poiId != null) {
                            onPoiClick(poiId)
                            ClickResult.Consume
                        } else {
                            ClickResult.Pass
                        }
                    },
                )
            }
        }
}

@Composable
private fun RoutePreviewPanel(
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
private fun ResultsPanel(
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

@Composable
private fun PickingPanel(
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
    var minimumDistance by remember {
        mutableStateOf(
            criteria.distanceKm?.start?.toString().orEmpty()
        )
    }
    var maximumDistance by remember {
        mutableStateOf(
            criteria.distanceKm?.endInclusive?.toString().orEmpty()
        )
    }
    var minimumTime by remember {
        mutableStateOf(
            criteria.estimatedTimeMinutes?.first?.toString().orEmpty()
        )
    }
    var maximumTime by remember {
        mutableStateOf(
            criteria.estimatedTimeMinutes?.last?.toString().orEmpty()
        )
    }
    var maximumProximity by remember {
        mutableStateOf(
            criteria.maxProximityKm?.toString().orEmpty()
        )
    }

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
                        onCriteriaChange(
                            criteria.copy(
                                estimatedTimeMinutes = integerRange(
                                    minimum,
                                    maximum
                                )
                            )
                        )
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

private fun Difficulty.label() = name.lowercase().replaceFirstChar(Char::uppercase)

private fun Terrain.label() = name.lowercase().replaceFirstChar(Char::uppercase)

private fun RoutePickingStatus.label() = name.lowercase().replaceFirstChar(Char::uppercase)

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
