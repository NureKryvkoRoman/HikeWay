package ua.nure.kryvko.hikeway.feature.routesearch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.data.services.network.ApiException
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase

class RouteSearchController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<RouteSearchUiState>,
    private val searchRoutes: SearchRoutesUseCase,
) {
    fun updateDraft(criteria: RouteSearchCriteria) {
        uiState.update { it.copy(draftCriteria = criteria) }
    }

    fun toggleDifficulty(difficulty: Difficulty) {
        val selected = uiState.value.draftCriteria.difficulties.toggle(difficulty)
        updateDraft(uiState.value.draftCriteria.copy(difficulties = selected))
    }

    fun toggleTerrain(terrain: Terrain) {
        val selected = uiState.value.draftCriteria.terrains.toggle(terrain)
        updateDraft(uiState.value.draftCriteria.copy(terrains = selected))
    }

    fun applyFilters() {
        refresh(uiState.value.draftCriteria)
    }

    fun clearFilters() {
        val emptyCriteria = RouteSearchCriteria()
        uiState.update { it.copy(draftCriteria = emptyCriteria) }
        refresh(emptyCriteria)
    }

    fun refreshCurrentSearch() {
        refresh(uiState.value.appliedCriteria)
    }

    fun previewRoute(route: Route) {
        uiState.update { it.copy(previewRoute = route, saveErrorMessage = null, mapContextPoint = null) }
    }

    fun dismissRoutePreview() {
        uiState.update { it.copy(previewRoute = null) }
    }

    fun refresh(criteria: RouteSearchCriteria) {
        scope.launch {
            uiState.update {
                it.copy(isLoading = true, errorMessage = null, appliedCriteria = criteria)
            }
            runCatching { searchRoutes(criteria) }
                .onSuccess { routes ->
                    uiState.update { it.copy(routes = routes, isLoading = false) }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            routes = emptyList(),
                            isLoading = false,
                            errorMessage = error.userMessage("Routes are unavailable."),
                        )
                    }
                }
        }
    }
}

private fun Throwable.userMessage(fallback: String): String {
    return when (this) {
        is ApiException -> when {
            statusCode == 403 || code == "FORBIDDEN" -> "You do not have permission to do that."
            statusCode == 404 -> "Point of interest is no longer available."
            message?.isNotBlank() == true -> message ?: fallback
            else -> fallback
        }
        else -> message ?: fallback
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) this - value else this + value
}
