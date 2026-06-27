package ua.nure.kryvko.hikeway.feature.routesearch

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.core.model.Difficulty
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PoiComment
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.core.model.Terrain
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider
import ua.nure.kryvko.hikeway.data.services.network.ApiException
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
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routepicking.initialProgress
import ua.nure.kryvko.hikeway.domain.routes.distanceKm
import ua.nure.kryvko.hikeway.domain.routes.GetCurrentLocationUseCase
import ua.nure.kryvko.hikeway.domain.routes.RouteSearchCriteria
import ua.nure.kryvko.hikeway.domain.routes.SearchRoutesUseCase

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

class RouteSearchViewModel(
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
    private var trackingJob: Job? = null
    private var activeTimerJob: Job? = null
    private var poiViewportJob: Job? = null
    private var lastPoiFetchCenter: GeoPoint? = null
    private var lastPoiFetchRadiusMeters: Double? = null

    init {
        centerOnCurrentLocation()
        refresh(RouteSearchCriteria())
        loadPointsOfInterest()
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

    fun refreshCurrentSearch() {
        refresh(_uiState.value.appliedCriteria)
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
                    loadPointsOfInterest(location)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Current location is unavailable.")
                    }
                }
        }
    }

    fun previewRoute(route: Route) {
        _uiState.update { it.copy(previewRoute = route, saveErrorMessage = null, mapContextPoint = null) }
    }

    fun dismissRoutePreview() {
        _uiState.update { it.copy(previewRoute = null) }
    }

    fun selectPoi(poiId: Long) {
        _uiState.update { state ->
            state.copy(
                selectedPoi = state.pointsOfInterest.firstOrNull { it.id == poiId },
                isPoiLoading = true,
                poiErrorMessage = null,
            )
        }
        viewModelScope.launch {
            runCatching { getPointOfInterestDetail(poiId) }
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            selectedPoi = detail,
                            pointsOfInterest = it.pointsOfInterest.upsertPoi(detail),
                            isPoiLoading = false,
                            poiErrorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isPoiLoading = false,
                            poiErrorMessage = error.userMessage("Point of interest is unavailable."),
                        )
                    }
                }
        }
    }

    fun dismissPoi() {
        _uiState.update { it.copy(selectedPoi = null, poiErrorMessage = null, isPoiLoading = false) }
    }

    fun openMapContext(point: GeoPoint) {
        _uiState.update {
            it.copy(
                mapContextPoint = point,
                selectedPoi = null,
                poiCreationPoint = null,
                poiCreationErrorMessage = null,
            )
        }
    }

    fun dismissMapContext() {
        _uiState.update { it.copy(mapContextPoint = null) }
    }

    fun startPoiCreationFromContext() {
        val point = _uiState.value.mapContextPoint ?: return
        _uiState.update {
            it.copy(
                poiCreationPoint = point,
                mapContextPoint = null,
                poiCreationName = "",
                poiCreationDescription = "",
                poiCreationErrorMessage = null,
            )
        }
    }

    fun updatePoiCreationName(name: String) {
        _uiState.update { it.copy(poiCreationName = name, poiCreationErrorMessage = null) }
    }

    fun updatePoiCreationDescription(description: String) {
        _uiState.update { it.copy(poiCreationDescription = description, poiCreationErrorMessage = null) }
    }

    fun cancelPoiCreation() {
        _uiState.update {
            it.copy(
                poiCreationPoint = null,
                poiCreationName = "",
                poiCreationDescription = "",
                poiCreationErrorMessage = null,
                isPoiCreationSaving = false,
            )
        }
    }

    fun createPoi() {
        val state = _uiState.value
        val point = state.poiCreationPoint ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isPoiCreationSaving = true, poiCreationErrorMessage = null) }
            runCatching {
                createPointOfInterest(
                    name = state.poiCreationName,
                    description = state.poiCreationDescription,
                    location = point,
                )
            }.onSuccess { poi ->
                _uiState.update {
                    it.copy(
                        pointsOfInterest = it.pointsOfInterest.upsertPoi(poi),
                        selectedPoi = poi,
                        poiCreationPoint = null,
                        poiCreationName = "",
                        poiCreationDescription = "",
                        isPoiCreationSaving = false,
                        poiCreationErrorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPoiCreationSaving = false,
                        poiCreationErrorMessage = error.userMessage("Could not create point of interest."),
                    )
                }
            }
        }
    }

    fun onMapViewportChanged(center: GeoPoint, zoom: Double) {
        val radiusMeters = radiusForZoom(zoom)
        if (!shouldFetchPois(center, radiusMeters)) {
            return
        }
        poiViewportJob?.cancel()
        poiViewportJob = viewModelScope.launch {
            delay(POI_FETCH_DEBOUNCE_MILLIS)
            fetchPoisNear(center, radiusMeters)
        }
    }

    fun ratePoi(rating: Int) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        if (rating !in 1..5) return
        _uiState.update { it.withPoiRating(poiId, rating) }
        viewModelScope.launch {
            runCatching { ratePointOfInterest(poiId, rating) }
                .onSuccess { updated -> _uiState.update { it.withPoiRating(poiId, updated) } }
                .onFailure { error ->
                    Log.w("RouteSearchViewModel", "Could not submit PoI rating", error)
                    _uiState.update { it.copy(poiErrorMessage = error.userMessage("Could not submit rating.")) }
                }
        }
    }

    fun removePoiRating() {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not remove rating.") {
                val rating = removePointOfInterestRating(poiId)
                _uiState.update { it.withPoiRating(poiId, rating) }
            }
        }
    }

    fun addPoiComment(text: String) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not add comment.") {
                val comment = addPoiCommentUseCase(poiId, text)
                _uiState.update { it.withAddedComment(comment) }
            }
        }
    }

    fun updatePoiComment(commentId: Long, text: String) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not update comment.") {
                val comment = updatePoiCommentUseCase(poiId, commentId, text)
                _uiState.update { it.withUpdatedComment(comment) }
            }
        }
    }

    fun deletePoiComment(commentId: Long) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not delete comment.") {
                deletePoiCommentUseCase(poiId, commentId)
                _uiState.update { it.withDeletedComment(commentId) }
            }
        }
    }

    fun uploadPoiPhoto(upload: PoiPhotoUpload) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not upload photo.") {
                val photo = uploadPoiPhotoUseCase(poiId, upload)
                _uiState.update { it.withAddedPhoto(photo) }
            }
        }
    }

    fun updatePoiPhoto(photoId: Long, caption: String?) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not update photo.") {
                val photo = updatePoiPhotoUseCase(poiId, photoId, caption)
                _uiState.update { it.withUpdatedPhoto(photo) }
            }
        }
    }

    fun deletePoiPhoto(photoId: Long) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not delete photo.") {
                deletePoiPhotoUseCase(poiId, photoId)
                _uiState.update { it.withDeletedPhoto(photoId) }
            }
        }
    }

    fun updateSelectedPoi(name: String, description: String, location: GeoPoint) {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not update point of interest.") {
                val poi = updatePointOfInterest(poiId, name, description, location)
                _uiState.update {
                    it.copy(
                        selectedPoi = it.selectedPoi?.mergeFrom(poi),
                        pointsOfInterest = it.pointsOfInterest.upsertPoi(poi),
                    )
                }
            }
        }
    }

    fun deleteSelectedPoi() {
        val poiId = _uiState.value.selectedPoi?.id ?: return
        viewModelScope.launch {
            runPoiAction("Could not delete point of interest.") {
                deletePointOfInterest(poiId)
                _uiState.update {
                    it.copy(
                        selectedPoi = null,
                        pointsOfInterest = it.pointsOfInterest.filterNot { poi -> poi.id == poiId },
                    )
                }
            }
        }
    }

    fun startPreviewedRoute() {
        val route = _uiState.value.previewRoute ?: return
        startRoute(route)
    }

    private fun startRoute(route: Route) {
        val progress = initialProgress(route) ?: return
        trackingJob?.cancel()
        activeTimerJob?.cancel()
        _uiState.update {
            it.copy(
                saveErrorMessage = null,
                previewRoute = null,
                pickingSession = RoutePickingSession(
                    route = route,
                    userPosition = progress.position,
                    bearingDegrees = progress.bearingDegrees,
                    pointIndex = progress.pointIndex,
                    status = RoutePickingStatus.ACTIVE,
                    walkedPath = listOf(progress.position),
                    walkedDistanceKm = 0.0,
                    activeElapsedMillis = 0L,
                    startedAtEpochMillis = timeProvider.currentTimeMillis(),
                )
            )
        }
        startTracking(route = route, startIndex = progress.pointIndex)
        startActiveTimer(routeId = route.id)
    }

    fun pauseRoute() {
        trackingJob?.cancel()
        trackingJob = null
        activeTimerJob?.cancel()
        activeTimerJob = null
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
        startActiveTimer(routeId = session.route.id)
    }

    fun finishRoute() {
        val session = _uiState.value.pickingSession ?: return
        trackingJob?.cancel()
        trackingJob = null
        activeTimerJob?.cancel()
        activeTimerJob = null
        _uiState.update { state ->
            state.copy(pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED))
        }

        viewModelScope.launch {
            val finishedAt = timeProvider.currentTimeMillis()
            val log = HikeLog(
                routeId = session.route.id,
                routeName = session.route.name,
                startedAtEpochMillis = session.startedAtEpochMillis,
                finishedAtEpochMillis = finishedAt,
                activeDurationMillis = session.activeElapsedMillis,
                wallClockDurationMillis = finishedAt - session.startedAtEpochMillis,
                totalDistanceKm = session.walkedDistanceKm,
                path = session.walkedPath,
            )
            runCatching { saveCompletedHike(log) }
                .onSuccess {
                    _uiState.update { it.copy(pickingSession = null, saveErrorMessage = null) }
                }
                .onFailure {
                    _uiState.update { state ->
                        state.copy(
                            pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED),
                            saveErrorMessage = "Could not save completed hike.",
                        )
                    }
                }
        }
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

    private fun loadPointsOfInterest(center: GeoPoint? = _uiState.value.mapCenter) {
        viewModelScope.launch {
            if (center != null) {
                fetchPoisNear(center, DEFAULT_POI_RADIUS_METERS)
            } else {
                runCatching { getPointsOfInterest() }
                    .onSuccess { pois ->
                        _uiState.update { it.copy(pointsOfInterest = pois) }
                    }
                    .onFailure {
                        _uiState.update { state ->
                            state.copy(errorMessage = state.errorMessage ?: "Points of interest are unavailable.")
                        }
                    }
            }
        }
    }

    private suspend fun fetchPoisNear(center: GeoPoint, radiusMeters: Double) {
        runCatching { getNearbyPointsOfInterest(center, radiusMeters) }
            .onSuccess { pois ->
                lastPoiFetchCenter = center
                lastPoiFetchRadiusMeters = radiusMeters
                _uiState.update { it.copy(pointsOfInterest = pois) }
            }
            .onFailure {
                _uiState.update { state ->
                    state.copy(errorMessage = state.errorMessage ?: "Points of interest are unavailable.")
                }
            }
    }

    private fun shouldFetchPois(center: GeoPoint, radiusMeters: Double): Boolean {
        val lastCenter = lastPoiFetchCenter ?: return true
        val lastRadius = lastPoiFetchRadiusMeters ?: return true
        val movedMeters = distanceKm(lastCenter, center) * 1_000.0
        val radiusChanged = kotlin.math.abs(radiusMeters - lastRadius) / lastRadius
        return movedMeters >= MIN_POI_REFETCH_DISTANCE_METERS ||
            movedMeters >= lastRadius * 0.25 ||
            radiusChanged >= POI_RADIUS_CHANGE_REFETCH_RATIO
    }

    private suspend fun runPoiAction(
        fallbackMessage: String,
        action: suspend () -> Unit,
    ) {
        _uiState.update { it.copy(isPoiActionInProgress = true, poiErrorMessage = null) }
        runCatching { action() }
            .onFailure { error ->
                _uiState.update {
                    it.copy(poiErrorMessage = error.userMessage(fallbackMessage))
                }
            }
        _uiState.update { it.copy(isPoiActionInProgress = false) }
    }

    private fun startTracking(route: Route, startIndex: Int) {
        trackingJob?.cancel()
        trackingJob = viewModelScope.launch {
            routeTrackingProvider.positions(route, startIndex)
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED),
                            saveErrorMessage = it.message ?: "Could not track current location.",
                        )
                    }
                }
                .collect { progress ->
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
                                    walkedPath = session.walkedPath.appendDistinct(progress.position),
                                    walkedDistanceKm = session.walkedDistanceKm +
                                        session.walkedPath.lastOrNull()
                                            ?.takeIf { it != progress.position }
                                            ?.let { distanceKm(it, progress.position) }
                                            .orZero(),
                                ),
                                saveErrorMessage = null,
                            )
                        }
                    }
                }
        }
    }

    private fun startActiveTimer(routeId: Long) {
        activeTimerJob?.cancel()
        activeTimerJob = viewModelScope.launch {
            activeTimer.ticks().collect { deltaMillis ->
                _uiState.update { state ->
                    val session = state.pickingSession
                    if (session?.route?.id != routeId || session.status != RoutePickingStatus.ACTIVE) {
                        state
                    } else {
                        state.copy(
                            pickingSession = session.copy(
                                activeElapsedMillis = session.activeElapsedMillis + deltaMillis
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
            getCurrentLocation: GetCurrentLocationUseCase,
            routeTrackingProvider: RouteTrackingProvider,
            saveCompletedHike: SaveCompletedHikeUseCase,
            timeProvider: TimeProvider,
            activeTimer: ActiveTimer,
            getPointsOfInterest: GetPointsOfInterestUseCase,
            getNearbyPointsOfInterest: GetNearbyPointsOfInterestUseCase,
            getPointOfInterestDetail: GetPointOfInterestDetailUseCase,
            createPointOfInterest: CreatePointOfInterestUseCase,
            updatePointOfInterest: UpdatePointOfInterestUseCase,
            deletePointOfInterest: DeletePointOfInterestUseCase,
            ratePointOfInterest: RatePointOfInterestUseCase,
            removePointOfInterestRating: RemovePointOfInterestRatingUseCase,
            addPoiComment: AddPoiCommentUseCase,
            updatePoiComment: UpdatePoiCommentUseCase,
            deletePoiComment: DeletePoiCommentUseCase,
            uploadPoiPhoto: UploadPoiPhotoUseCase,
            updatePoiPhoto: UpdatePoiPhotoUseCase,
            deletePoiPhoto: DeletePoiPhotoUseCase,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RouteSearchViewModel(
                        searchRoutes = searchRoutes,
                        getCurrentLocation = getCurrentLocation,
                        routeTrackingProvider = routeTrackingProvider,
                        saveCompletedHike = saveCompletedHike,
                        timeProvider = timeProvider,
                        activeTimer = activeTimer,
                        getPointsOfInterest = getPointsOfInterest,
                        getNearbyPointsOfInterest = getNearbyPointsOfInterest,
                        getPointOfInterestDetail = getPointOfInterestDetail,
                        createPointOfInterest = createPointOfInterest,
                        updatePointOfInterest = updatePointOfInterest,
                        deletePointOfInterest = deletePointOfInterest,
                        ratePointOfInterest = ratePointOfInterest,
                        removePointOfInterestRating = removePointOfInterestRating,
                        addPoiCommentUseCase = addPoiComment,
                        updatePoiCommentUseCase = updatePoiComment,
                        deletePoiCommentUseCase = deletePoiComment,
                        uploadPoiPhotoUseCase = uploadPoiPhoto,
                        updatePoiPhotoUseCase = updatePoiPhoto,
                        deletePoiPhotoUseCase = deletePoiPhoto,
                    ) as T
                }
            }
        }
    }
}

private const val DEFAULT_POI_RADIUS_METERS = 20_000.0
private const val MAX_POI_RADIUS_METERS = 100_000.0
private const val MIN_POI_RADIUS_METERS = 2_000.0
private const val POI_FETCH_DEBOUNCE_MILLIS = 500L
private const val MIN_POI_REFETCH_DISTANCE_METERS = 500.0
private const val POI_RADIUS_CHANGE_REFETCH_RATIO = 0.15

private fun RouteSearchUiState.withPoiRating(
    poiId: Long,
    rating: Int,
): RouteSearchUiState {
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

private fun RouteSearchUiState.withPoiRating(
    poiId: Long,
    rating: PoiRating,
): RouteSearchUiState {
    val updatedPois = pointsOfInterest.map { poi ->
        if (poi.id == poiId) {
            poi.copy(
                averageRating = rating.averageRating,
                ratingCount = rating.ratingCount,
                userRating = rating.userRating,
            )
        } else {
            poi
        }
    }
    return copy(
        pointsOfInterest = updatedPois,
        selectedPoi = selectedPoi?.let { selected ->
            if (selected.id == poiId) {
                selected.copy(
                    averageRating = rating.averageRating,
                    ratingCount = rating.ratingCount,
                    userRating = rating.userRating,
                )
            } else {
                selected
            }
        },
    )
}

private fun RouteSearchUiState.withAddedComment(comment: PoiComment): RouteSearchUiState {
    return copy(selectedPoi = selectedPoi?.copy(comments = selectedPoi.comments + comment))
}

private fun RouteSearchUiState.withUpdatedComment(comment: PoiComment): RouteSearchUiState {
    return copy(
        selectedPoi = selectedPoi?.copy(
            comments = selectedPoi.comments.map { if (it.id == comment.id) comment else it }
        )
    )
}

private fun RouteSearchUiState.withDeletedComment(commentId: Long): RouteSearchUiState {
    return copy(
        selectedPoi = selectedPoi?.copy(
            comments = selectedPoi.comments.filterNot { it.id == commentId }
        )
    )
}

private fun RouteSearchUiState.withAddedPhoto(photo: PoiPhoto): RouteSearchUiState {
    val updated = selectedPoi?.copy(photos = selectedPoi.photos + photo)
    return copy(selectedPoi = updated, pointsOfInterest = updated?.let { pointsOfInterest.upsertPoi(it) } ?: pointsOfInterest)
}

private fun RouteSearchUiState.withUpdatedPhoto(photo: PoiPhoto): RouteSearchUiState {
    val updated = selectedPoi?.copy(
        photos = selectedPoi.photos.map { if (it.id == photo.id) photo else it }
    )
    return copy(selectedPoi = updated, pointsOfInterest = updated?.let { pointsOfInterest.upsertPoi(it) } ?: pointsOfInterest)
}

private fun RouteSearchUiState.withDeletedPhoto(photoId: Long): RouteSearchUiState {
    val updated = selectedPoi?.copy(photos = selectedPoi.photos.filterNot { it.id == photoId })
    return copy(selectedPoi = updated, pointsOfInterest = updated?.let { pointsOfInterest.upsertPoi(it) } ?: pointsOfInterest)
}

private fun List<PointOfInterest>.upsertPoi(poi: PointOfInterest): List<PointOfInterest> {
    return if (any { it.id == poi.id }) map { if (it.id == poi.id) it.mergeFrom(poi) else it } else this + poi
}

private fun PointOfInterest.mergeFrom(other: PointOfInterest): PointOfInterest {
    return copy(
        name = other.name,
        description = other.description,
        location = other.location,
        ownerId = other.ownerId ?: ownerId,
        ownerDisplayName = other.ownerDisplayName ?: ownerDisplayName,
        ownedByCurrentUser = other.ownedByCurrentUser,
        photos = other.photos.ifEmpty { photos },
        averageRating = other.averageRating,
        ratingCount = other.ratingCount,
        userRating = other.userRating,
        comments = other.comments.ifEmpty { comments },
        createdAt = other.createdAt ?: createdAt,
        updatedAt = other.updatedAt ?: updatedAt,
    )
}

private fun radiusForZoom(zoom: Double): Double {
    val scale = Math.pow(2.0, 10.0 - zoom)
    return (DEFAULT_POI_RADIUS_METERS * scale).coerceIn(
        MIN_POI_RADIUS_METERS,
        MAX_POI_RADIUS_METERS,
    )
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

private fun <T> List<T>.appendDistinct(value: T): List<T> {
    return if (lastOrNull() == value) this else this + value
}

private fun Double?.orZero(): Double = this ?: 0.0
