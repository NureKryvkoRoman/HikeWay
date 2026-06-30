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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
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
import ua.nure.kryvko.hikeway.ui.map.MapContextAction
import ua.nure.kryvko.hikeway.ui.map.MapContextMenu
import ua.nure.kryvko.hikeway.ui.map.MapCenterMode
import ua.nure.kryvko.hikeway.ui.map.MapCenterRequest
import ua.nure.kryvko.hikeway.ui.map.emptyFeatureCollectionGeoJson
import ua.nure.kryvko.hikeway.ui.map.toLineStringFeatureGeoJson
import ua.nure.kryvko.hikeway.ui.map.toGeoPoint
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
            contextPoint = state.mapContextPoint ?: state.poiCreationPoint,
            mapCenter = state.mapCenter,
            mapCenterRequestId = state.mapCenterRequestId,
            onCenterLocation = viewModel::centerOnCurrentLocation,
            onPoiClick = viewModel::selectPoi,
            onMapLongClick = if (pickingSession == null && previewRoute == null) {
                viewModel::openMapContext
            } else {
                null
            },
            onMapViewportChanged = viewModel::onMapViewportChanged,
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
        state.mapContextPoint?.let { point ->
            MapContextMenu(
                point = point,
                actions = listOf(
                    MapContextAction(
                        label = "Add PoI",
                        onClick = viewModel::startPoiCreationFromContext,
                    )
                ),
                onDismiss = viewModel::dismissMapContext,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }

    state.poiCreationPoint?.let { point ->
        CreatePoiDialog(
            point = point,
            name = state.poiCreationName,
            description = state.poiCreationDescription,
            isSaving = state.isPoiCreationSaving,
            errorMessage = state.poiCreationErrorMessage,
            onNameChange = viewModel::updatePoiCreationName,
            onDescriptionChange = viewModel::updatePoiCreationDescription,
            onSave = viewModel::createPoi,
            onDismiss = viewModel::cancelPoiCreation,
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
