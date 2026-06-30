package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider
import ua.nure.kryvko.hikeway.domain.pois.AddPoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.CreatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.pois.DeletePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetNearbyPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetPointOfInterestDetailUseCase
import ua.nure.kryvko.hikeway.domain.pois.GetPointsOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.RatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.RemovePointOfInterestRatingUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePoiCommentUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.pois.UpdatePointOfInterestUseCase
import ua.nure.kryvko.hikeway.domain.pois.UploadPoiPhotoUseCase
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingSession
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routes.GetCurrentLocationUseCase
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase
import javax.inject.Inject

data class RouteSearchUiState(
    val routes: List<Route> = emptyList(),
    val mapCenter: GeoPoint? = null,
    val mapCenterRequestId: Long = 0L,
    val draftCriteria: RouteSearchCriteria = RouteSearchCriteria(),
    val appliedCriteria: RouteSearchCriteria = RouteSearchCriteria(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val saveErrorMessage: String? = null,
    val previewRoute: Route? = null,
    val pickingSession: RoutePickingSession? = null,
    val pointsOfInterest: List<PointOfInterest> = emptyList(),
    val selectedPoi: PointOfInterest? = null,
    val isPoiLoading: Boolean = false,
    val isPoiActionInProgress: Boolean = false,
    val poiErrorMessage: String? = null,
    val mapContextPoint: GeoPoint? = null,
    val poiCreationPoint: GeoPoint? = null,
    val poiCreationName: String = "",
    val poiCreationDescription: String = "",
    val isPoiCreationSaving: Boolean = false,
    val poiCreationErrorMessage: String? = null,
)

@HiltViewModel
class RouteSearchViewModel @Inject constructor(
    private val searchRoutes: SearchRoutesUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val routeTrackingProvider: RouteTrackingProvider,
    private val saveCompletedHike: SaveCompletedHikeUseCase,
    private val timeProvider: TimeProvider,
    private val activeTimer: ActiveTimer,
    private val getPointsOfInterest: GetPointsOfInterestUseCase,
    private val getNearbyPointsOfInterest: GetNearbyPointsOfInterestUseCase,
    private val getPointOfInterestDetail: GetPointOfInterestDetailUseCase,
    private val createPointOfInterest: CreatePointOfInterestUseCase,
    private val updatePointOfInterest: UpdatePointOfInterestUseCase,
    private val deletePointOfInterest: DeletePointOfInterestUseCase,
    private val ratePointOfInterest: RatePointOfInterestUseCase,
    private val removePointOfInterestRating: RemovePointOfInterestRatingUseCase,
    private val addPoiCommentUseCase: AddPoiCommentUseCase,
    private val updatePoiCommentUseCase: UpdatePoiCommentUseCase,
    private val deletePoiCommentUseCase: DeletePoiCommentUseCase,
    private val uploadPoiPhotoUseCase: UploadPoiPhotoUseCase,
    private val updatePoiPhotoUseCase: UpdatePoiPhotoUseCase,
    private val deletePoiPhotoUseCase: DeletePoiPhotoUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(RouteSearchUiState())
    val uiState: StateFlow<RouteSearchUiState> = _uiState.asStateFlow()
    private val routeSearchController = RouteSearchController(
        scope = viewModelScope,
        uiState = _uiState,
        searchRoutes = searchRoutes,
    )
    private val hikeTrackingController = HikeTrackingController(
        scope = viewModelScope,
        uiState = _uiState,
        routeTrackingProvider = routeTrackingProvider,
        saveCompletedHike = saveCompletedHike,
        timeProvider = timeProvider,
        activeTimer = activeTimer,
    )
    private val poiController = PoiController(
        scope = viewModelScope,
        uiState = _uiState,
        getPointsOfInterest = getPointsOfInterest,
        getNearbyPointsOfInterest = getNearbyPointsOfInterest,
        getPointOfInterestDetail = getPointOfInterestDetail,
        createPointOfInterest = createPointOfInterest,
        updatePointOfInterest = updatePointOfInterest,
        deletePointOfInterest = deletePointOfInterest,
        ratePointOfInterest = ratePointOfInterest,
        removePointOfInterestRating = removePointOfInterestRating,
        addPoiCommentUseCase = addPoiCommentUseCase,
        updatePoiCommentUseCase = updatePoiCommentUseCase,
        deletePoiCommentUseCase = deletePoiCommentUseCase,
        uploadPoiPhotoUseCase = uploadPoiPhotoUseCase,
        updatePoiPhotoUseCase = updatePoiPhotoUseCase,
        deletePoiPhotoUseCase = deletePoiPhotoUseCase,
    )

    init {
        centerOnCurrentLocation()
        routeSearchController.refresh(RouteSearchCriteria())
        poiController.loadPointsOfInterest()
    }

    fun updateDraft(criteria: RouteSearchCriteria) {
        routeSearchController.updateDraft(criteria)
    }

    fun toggleDifficulty(difficulty: Difficulty) {
        routeSearchController.toggleDifficulty(difficulty)
    }

    fun toggleTerrain(terrain: Terrain) {
        routeSearchController.toggleTerrain(terrain)
    }

    fun applyFilters() {
        routeSearchController.applyFilters()
    }

    fun clearFilters() {
        routeSearchController.clearFilters()
    }

    fun refreshCurrentSearch() {
        routeSearchController.refreshCurrentSearch()
    }

    fun refreshPointsOfInterest() {
        poiController.refreshPointsOfInterest()
    }

    fun centerOnCurrentLocation() {
        viewModelScope.launch {
            runCatching { getCurrentLocation() }
                .onSuccess { location ->
                    _uiState.update {
                        it.copy(
                            mapCenter = location,
                            mapCenterRequestId = it.mapCenterRequestId + 1,
                            errorMessage = null,
                        )
                    }
                    poiController.loadPointsOfInterest(location)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Current location is unavailable.")
                    }
                }
        }
    }

    fun previewRoute(route: Route) {
        routeSearchController.previewRoute(route)
    }

    fun dismissRoutePreview() {
        routeSearchController.dismissRoutePreview()
    }

    fun selectPoi(poiId: Long) {
        poiController.selectPoi(poiId)
    }

    fun dismissPoi() {
        poiController.dismissPoi()
    }

    fun openMapContext(point: GeoPoint) {
        poiController.openMapContext(point)
    }

    fun dismissMapContext() {
        poiController.dismissMapContext()
    }

    fun startPoiCreationFromContext() {
        poiController.startPoiCreationFromContext()
    }

    fun updatePoiCreationName(name: String) {
        poiController.updatePoiCreationName(name)
    }

    fun updatePoiCreationDescription(description: String) {
        poiController.updatePoiCreationDescription(description)
    }

    fun cancelPoiCreation() {
        poiController.cancelPoiCreation()
    }

    fun createPoi() {
        poiController.createPoi()
    }

    fun onMapViewportChanged(center: GeoPoint, zoom: Double) {
        poiController.onMapViewportChanged(center, zoom)
    }

    fun ratePoi(rating: Int) {
        poiController.ratePoi(rating)
    }

    fun removePoiRating() {
        poiController.removePoiRating()
    }

    fun addPoiComment(text: String) {
        poiController.addPoiComment(text)
    }

    fun updatePoiComment(commentId: Long, text: String) {
        poiController.updatePoiComment(commentId, text)
    }

    fun deletePoiComment(commentId: Long) {
        poiController.deletePoiComment(commentId)
    }

    fun uploadPoiPhoto(upload: PoiPhotoUpload) {
        poiController.uploadPoiPhoto(upload)
    }

    fun updatePoiPhoto(photoId: Long, caption: String?) {
        poiController.updatePoiPhoto(photoId, caption)
    }

    fun deletePoiPhoto(photoId: Long) {
        poiController.deletePoiPhoto(photoId)
    }

    fun updateSelectedPoi(name: String, description: String, location: GeoPoint) {
        poiController.updateSelectedPoi(name, description, location)
    }

    fun deleteSelectedPoi() {
        poiController.deleteSelectedPoi()
    }

    fun startPreviewedRoute() {
        val route = _uiState.value.previewRoute ?: return
        startRoute(route)
    }

    private fun startRoute(route: Route) {
        hikeTrackingController.startRoute(route)
    }

    fun pauseRoute() {
        hikeTrackingController.pauseRoute()
    }

    fun unpauseRoute() {
        hikeTrackingController.unpauseRoute()
    }

    fun finishRoute() {
        hikeTrackingController.finishRoute()
    }

}
