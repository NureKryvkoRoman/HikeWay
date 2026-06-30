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
import org.maplibre.compose.util.ClickResult
import ua.nure.kryvko.hikeway.R
import java.util.Locale
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.feature.pois.PoiOverviewPopup
import ua.nure.kryvko.hikeway.ui.map.HikeWayMap
import ua.nure.kryvko.hikeway.ui.map.MapCenterMode
import ua.nure.kryvko.hikeway.ui.map.emptyFeatureCollectionGeoJson
import ua.nure.kryvko.hikeway.ui.map.toGeoPoint
import ua.nure.kryvko.hikeway.ui.map.toLineStringFeatureGeoJson
import ua.nure.kryvko.hikeway.ui.map.toPoiFeatureCollectionGeoJson

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
            onPoiClick = viewModel::selectPoi,
            onDismissPoi = viewModel::dismissPoi,
            onRatePoi = viewModel::ratePoi,
            onAddPoiToRoute = viewModel::addSelectedPoiToRoute,
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
