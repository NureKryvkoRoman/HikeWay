package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingSession
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routepicking.initialProgress
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase

data class RouteSearchUiState(
    val routes: List<Route> = emptyList(),
    val draftCriteria: RouteSearchCriteria = RouteSearchCriteria(),
    val appliedCriteria: RouteSearchCriteria = RouteSearchCriteria(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pickingSession: RoutePickingSession? = null,
)

class RouteSearchViewModel(
    private val searchRoutes: SearchRoutesUseCase,
    private val routeTrackingProvider: RouteTrackingProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteSearchUiState())
    val uiState: StateFlow<RouteSearchUiState> = _uiState.asStateFlow()
    private var trackingJob: Job? = null

    init {
        refresh(RouteSearchCriteria())
    }

    fun updateDraft(criteria: RouteSearchCriteria) {
        _uiState.update { it.copy(draftCriteria = criteria) }
    }

    fun toggleDifficulty(difficulty: Difficulty) {
        val selected = _uiState.value.draftCriteria.difficulties.toggle(difficulty)
        updateDraft(_uiState.value.draftCriteria.copy(difficulties = selected))
    }

    fun toggleTerrain(terrain: Terrain) {
        val selected = _uiState.value.draftCriteria.terrains.toggle(terrain)
        updateDraft(_uiState.value.draftCriteria.copy(terrains = selected))
    }

    fun applyFilters() {
        refresh(_uiState.value.draftCriteria)
    }

    fun clearFilters() {
        val emptyCriteria = RouteSearchCriteria()
        _uiState.update { it.copy(draftCriteria = emptyCriteria) }
        refresh(emptyCriteria)
    }

    fun pickRoute(route: Route) {
        val progress = initialProgress(route) ?: return
        trackingJob?.cancel()
        _uiState.update {
            it.copy(
                pickingSession = RoutePickingSession(
                    route = route,
                    userPosition = progress.position,
                    bearingDegrees = progress.bearingDegrees,
                    pointIndex = progress.pointIndex,
                    status = RoutePickingStatus.ACTIVE,
                )
            )
        }
        startTracking(route = route, startIndex = progress.pointIndex)
    }

    fun pauseRoute() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.update { state ->
            state.copy(pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED))
        }
    }

    fun unpauseRoute() {
        val session = _uiState.value.pickingSession ?: return
        if (session.status == RoutePickingStatus.ACTIVE) return
        _uiState.update {
            it.copy(pickingSession = session.copy(status = RoutePickingStatus.ACTIVE))
        }
        startTracking(route = session.route, startIndex = session.pointIndex)
    }

    fun finishRoute() {
        trackingJob?.cancel()
        trackingJob = null
        _uiState.update { it.copy(pickingSession = null) }
    }

    private fun refresh(criteria: RouteSearchCriteria) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, appliedCriteria = criteria)
            }
            runCatching { searchRoutes(criteria) }
                .onSuccess { routes ->
                    _uiState.update { it.copy(routes = routes, isLoading = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            routes = emptyList(),
                            isLoading = false,
                            errorMessage = "Current location is unavailable.",
                        )
                    }
                }
        }
    }

    private fun startTracking(route: Route, startIndex: Int) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            routeTrackingProvider.positions(route, startIndex).collect { progress ->
                _uiState.update { state ->
                    val session = state.pickingSession
                    if (session?.route?.id != route.id || session.status != RoutePickingStatus.ACTIVE) {
                        state
                    } else {
                        state.copy(
                            pickingSession = session.copy(
                                userPosition = progress.position,
                                bearingDegrees = progress.bearingDegrees,
                                pointIndex = progress.pointIndex,
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(
            searchRoutes: SearchRoutesUseCase,
            routeTrackingProvider: RouteTrackingProvider,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RouteSearchViewModel(searchRoutes, routeTrackingProvider) as T
                }
            }
        }
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) this - value else this + value
}
