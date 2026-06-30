package ua.nure.kryvko.hikeway.feature.routesearch

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PoiComment
import ua.nure.kryvko.hikeway.core.model.PoiPhoto
import ua.nure.kryvko.hikeway.core.model.PoiPhotoUpload
import ua.nure.kryvko.hikeway.core.model.PoiRating
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
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
import ua.nure.kryvko.hikeway.domain.routes.distanceKm

class PoiController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<RouteSearchUiState>,
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
) {
    private var viewportJob: Job? = null
    private var lastFetchCenter: GeoPoint? = null
    private var lastFetchRadiusMeters: Double? = null
    private var loadRequestId = 0L

    fun refreshPointsOfInterest() {
        loadPointsOfInterest()
    }

    fun loadPointsOfInterest(center: GeoPoint? = uiState.value.mapCenter) {
        val requestId = nextLoadRequestId()
        scope.launch {
            if (center != null) {
                fetchPoisNear(center, DEFAULT_POI_RADIUS_METERS, requestId)
            } else {
                runCatching { getPointsOfInterest() }
                    .onSuccess { pois ->
                        if (isLatestLoadRequest(requestId)) {
                            uiState.update { it.copy(pointsOfInterest = pois) }
                        }
                    }
                    .onFailure {
                        if (isLatestLoadRequest(requestId)) {
                            uiState.update { state ->
                                state.copy(errorMessage = state.errorMessage ?: "Points of interest are unavailable.")
                            }
                        }
                    }
            }
        }
    }

    fun selectPoi(poiId: Long) {
        uiState.update { state ->
            state.copy(
                selectedPoi = state.pointsOfInterest.firstOrNull { it.id == poiId },
                isPoiLoading = true,
                poiErrorMessage = null,
            )
        }
        scope.launch {
            runCatching { getPointOfInterestDetail(poiId) }
                .onSuccess { detail ->
                    uiState.update {
                        it.copy(
                            selectedPoi = detail,
                            pointsOfInterest = it.pointsOfInterest.upsertPoi(detail),
                            isPoiLoading = false,
                            poiErrorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    uiState.update {
                        it.copy(
                            isPoiLoading = false,
                            poiErrorMessage = error.userMessage("Point of interest is unavailable."),
                        )
                    }
                }
        }
    }

    fun dismissPoi() {
        uiState.update { it.copy(selectedPoi = null, poiErrorMessage = null, isPoiLoading = false) }
    }

    fun openMapContext(point: GeoPoint) {
        uiState.update {
            it.copy(
                mapContextPoint = point,
                selectedPoi = null,
                poiCreationPoint = null,
                poiCreationErrorMessage = null,
            )
        }
    }

    fun dismissMapContext() {
        uiState.update { it.copy(mapContextPoint = null) }
    }

    fun startPoiCreationFromContext() {
        val point = uiState.value.mapContextPoint ?: return
        uiState.update {
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
        uiState.update { it.copy(poiCreationName = name, poiCreationErrorMessage = null) }
    }

    fun updatePoiCreationDescription(description: String) {
        uiState.update { it.copy(poiCreationDescription = description, poiCreationErrorMessage = null) }
    }

    fun cancelPoiCreation() {
        uiState.update {
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
        val state = uiState.value
        val point = state.poiCreationPoint ?: return
        scope.launch {
            uiState.update { it.copy(isPoiCreationSaving = true, poiCreationErrorMessage = null) }
            runCatching {
                createPointOfInterest(
                    name = state.poiCreationName,
                    description = state.poiCreationDescription,
                    location = point,
                )
            }.onSuccess { poi ->
                uiState.update {
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
                uiState.update {
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
        viewportJob?.cancel()
        val requestId = nextLoadRequestId()
        viewportJob = scope.launch {
            delay(POI_FETCH_DEBOUNCE_MILLIS)
            fetchPoisNear(center, radiusMeters, requestId)
        }
    }

    fun ratePoi(rating: Int) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        if (rating !in 1..5) return
        uiState.update { it.withPoiRating(poiId, rating) }
        scope.launch {
            runCatching { ratePointOfInterest(poiId, rating) }
                .onSuccess { updated -> uiState.update { it.withPoiRating(poiId, updated) } }
                .onFailure { error ->
                    Log.w("PoiController", "Could not submit PoI rating", error)
                    uiState.update { it.copy(poiErrorMessage = error.userMessage("Could not submit rating.")) }
                }
        }
    }

    fun removePoiRating() {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not remove rating.") {
                val rating = removePointOfInterestRating(poiId)
                uiState.update { it.withPoiRating(poiId, rating) }
            }
        }
    }

    fun addPoiComment(text: String) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not add comment.") {
                val comment = addPoiCommentUseCase(poiId, text)
                uiState.update { it.withAddedComment(comment) }
            }
        }
    }

    fun updatePoiComment(commentId: Long, text: String) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not update comment.") {
                val comment = updatePoiCommentUseCase(poiId, commentId, text)
                uiState.update { it.withUpdatedComment(comment) }
            }
        }
    }

    fun deletePoiComment(commentId: Long) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not delete comment.") {
                deletePoiCommentUseCase(poiId, commentId)
                uiState.update { it.withDeletedComment(commentId) }
            }
        }
    }

    fun uploadPoiPhoto(upload: PoiPhotoUpload) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not upload photo.") {
                val photo = uploadPoiPhotoUseCase(poiId, upload)
                uiState.update { it.withAddedPhoto(photo) }
            }
        }
    }

    fun updatePoiPhoto(photoId: Long, caption: String?) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not update photo.") {
                val photo = updatePoiPhotoUseCase(poiId, photoId, caption)
                uiState.update { it.withUpdatedPhoto(photo) }
            }
        }
    }

    fun deletePoiPhoto(photoId: Long) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not delete photo.") {
                deletePoiPhotoUseCase(poiId, photoId)
                uiState.update { it.withDeletedPhoto(photoId) }
            }
        }
    }

    fun updateSelectedPoi(name: String, description: String, location: GeoPoint) {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not update point of interest.") {
                val poi = updatePointOfInterest(poiId, name, description, location)
                uiState.update {
                    it.copy(
                        selectedPoi = it.selectedPoi?.mergeFrom(poi),
                        pointsOfInterest = it.pointsOfInterest.upsertPoi(poi),
                    )
                }
            }
        }
    }

    fun deleteSelectedPoi() {
        val poiId = uiState.value.selectedPoi?.id ?: return
        scope.launch {
            runPoiAction("Could not delete point of interest.") {
                deletePointOfInterest(poiId)
                uiState.update {
                    it.copy(
                        selectedPoi = null,
                        pointsOfInterest = it.pointsOfInterest.filterNot { poi -> poi.id == poiId },
                    )
                }
            }
        }
    }

    private suspend fun fetchPoisNear(center: GeoPoint, radiusMeters: Double, requestId: Long) {
        runCatching { getNearbyPointsOfInterest(center, radiusMeters) }
            .onSuccess { pois ->
                if (isLatestLoadRequest(requestId)) {
                    lastFetchCenter = center
                    lastFetchRadiusMeters = radiusMeters
                    uiState.update { it.copy(pointsOfInterest = pois) }
                }
            }
            .onFailure {
                if (isLatestLoadRequest(requestId)) {
                    uiState.update { state ->
                        state.copy(errorMessage = state.errorMessage ?: "Points of interest are unavailable.")
                    }
                }
            }
    }

    private fun nextLoadRequestId(): Long {
        loadRequestId += 1
        return loadRequestId
    }

    private fun isLatestLoadRequest(requestId: Long): Boolean = requestId == loadRequestId

    private fun shouldFetchPois(center: GeoPoint, radiusMeters: Double): Boolean {
        val lastCenter = lastFetchCenter ?: return true
        val lastRadius = lastFetchRadiusMeters ?: return true
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
        uiState.update { it.copy(isPoiActionInProgress = true, poiErrorMessage = null) }
        runCatching { action() }
            .onFailure { error ->
                uiState.update {
                    it.copy(poiErrorMessage = error.userMessage(fallbackMessage))
                }
            }
        uiState.update { it.copy(isPoiActionInProgress = false) }
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
