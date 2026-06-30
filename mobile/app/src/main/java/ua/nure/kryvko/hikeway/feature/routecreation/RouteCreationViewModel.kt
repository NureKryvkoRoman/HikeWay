package ua.nure.kryvko.hikeway.feature.routecreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.math.ceil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.RouteGeometry
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.routes.SaveCustomRouteUseCase
import ua.nure.kryvko.hikeway.domain.routes.distanceKm
import javax.inject.Inject

private const val WALKING_SPEED_KMH = 4.0
private const val PLACEHOLDER_ELEVATION_GAIN_METERS = 0

enum class RouteCreationStep {
    MAP,
    DETAILS,
}

data class RouteCreationUiState(
    val step: RouteCreationStep = RouteCreationStep.MAP,
    val points: List<GeoPoint> = emptyList(),
    val crosshairPoint: GeoPoint = GeoPoint(longitude = 24.0316, latitude = 49.8429),
    val name: String = "",
    val description: String = "",
    val difficulty: Difficulty? = null,
    val terrain: Terrain? = null,
    val validationMessage: String? = null,
    val saveErrorMessage: String? = null,
    val isSaving: Boolean = false,
    val didSave: Boolean = false,
    val pointsOfInterest: List<PointOfInterest> = emptyList(),
    val selectedPoi: PointOfInterest? = null,
) {
    val distanceKm: Double = points.totalDistanceKm()
    val estimatedTimeMinutes: Int = estimateTimeMinutes(distanceKm)
}

@HiltViewModel
class RouteCreationViewModel @Inject constructor(
    private val saveCustomRoute: SaveCustomRouteUseCase,
    private val getPointsOfInterest: GetPointsOfInterestUseCase,
    private val ratePointOfInterest: RatePointOfInterestUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteCreationUiState())
    val uiState: StateFlow<RouteCreationUiState> = _uiState.asStateFlow()

    init {
        loadPointsOfInterest()
    }

    fun updateCrosshair(point: GeoPoint) {
        _uiState.update { it.copy(crosshairPoint = point) }
    }

    fun placePoint() {
        _uiState.update {
            it.copy(
                points = it.points + it.crosshairPoint,
                validationMessage = null,
            )
        }
    }

    fun selectPoi(poiId: Long) {
        _uiState.update { state ->
            state.copy(selectedPoi = state.pointsOfInterest.firstOrNull { it.id == poiId })
        }
    }

    fun dismissPoi() {
        _uiState.update { it.copy(selectedPoi = null) }
    }

    fun addSelectedPoiToRoute() {
        _uiState.update { state ->
            val poi = state.selectedPoi ?: return@update state
            state.copy(
                points = state.points + poi.location,
                selectedPoi = null,
                validationMessage = null,
            )
        }
    }

    fun ratePoi(rating: Int) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        if (rating !in 1..5) return
        _uiState.update { it.withPoiRating(poiId, rating) }
        viewModelScope.launch {
            runCatching { ratePointOfInterest(poiId, rating) }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(saveErrorMessage = state.saveErrorMessage ?: "Could not submit rating.")
                    }
                }
        }
    }

    fun finishMapStep() {
        _uiState.update {
            if (it.points.size < 2) {
                it.copy(validationMessage = "Place at least two points to finish the route.")
            } else {
                it.copy(step = RouteCreationStep.DETAILS, validationMessage = null)
            }
        }
    }

    fun backToMap() {
        _uiState.update { it.copy(step = RouteCreationStep.MAP, validationMessage = null) }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name, validationMessage = null) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description, validationMessage = null) }
    }

    fun selectDifficulty(difficulty: Difficulty) {
        _uiState.update { it.copy(difficulty = difficulty, validationMessage = null) }
    }

    fun selectTerrain(terrain: Terrain) {
        _uiState.update { it.copy(terrain = terrain, validationMessage = null) }
    }

    fun saveRoute() {
        val state = _uiState.value
        val name = state.name.trim()
        val description = state.description.trim()
        val difficulty = state.difficulty
        val terrain = state.terrain
        val validationMessage = when {
            state.points.size < 2 -> "Place at least two points to save the route."
            name.isEmpty() -> "Route name is required."
            description.isEmpty() -> "Route description is required."
            difficulty == null -> "Difficulty is required."
            terrain == null -> "Terrain is required."
            else -> null
        }
        if (validationMessage != null) {
            _uiState.update { it.copy(validationMessage = validationMessage) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, saveErrorMessage = null) }
            val route = Route(
                id = 0,
                name = name,
                description = description,
                distanceKm = state.distanceKm,
                estimatedTimeMinutes = state.estimatedTimeMinutes,
                difficulty = requireNotNull(difficulty),
                elevationGainMeters = PLACEHOLDER_ELEVATION_GAIN_METERS,
                terrain = requireNotNull(terrain),
                geometry = RouteGeometry(state.points),
            )
            runCatching { saveCustomRoute(route) }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, didSave = true) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveErrorMessage = "Could not save route.",
                        )
                    }
                }
        }
    }

    fun reset() {
        _uiState.value = RouteCreationUiState()
        loadPointsOfInterest()
    }

    private fun loadPointsOfInterest() {
        viewModelScope.launch {
            runCatching { getPointsOfInterest() }
                .onSuccess { pois ->
                    _uiState.update { it.copy(pointsOfInterest = pois) }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(saveErrorMessage = state.saveErrorMessage ?: "Points of interest are unavailable.")
                    }
                }
        }
    }
}

private fun RouteCreationUiState.withPoiRating(
    poiId: Long,
    rating: Int,
): RouteCreationUiState {
    val updatedPois = pointsOfInterest.map { poi ->
        if (poi.id == poiId) poi.copy(userRating = rating) else poi
    }
    return copy(
        pointsOfInterest = updatedPois,
        selectedPoi = selectedPoi?.let { selected ->
            if (selected.id == poiId) selected.copy(userRating = rating) else selected
        },
    )
}

fun List<GeoPoint>.totalDistanceKm(): Double {
    return zipWithNext().sumOf { (from, to) -> distanceKm(from, to) }
}

fun estimateTimeMinutes(distanceKm: Double): Int {
    return ceil((distanceKm / WALKING_SPEED_KMH) * 60).toInt()
}
