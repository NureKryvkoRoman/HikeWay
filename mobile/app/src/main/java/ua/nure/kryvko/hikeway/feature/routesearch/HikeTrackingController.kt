package ua.nure.kryvko.hikeway.feature.routesearch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.domain.hikelogging.ActiveTimer
import ua.nure.kryvko.hikeway.domain.hikelogging.HikeLog
import ua.nure.kryvko.hikeway.domain.hikelogging.SaveCompletedHikeUseCase
import ua.nure.kryvko.hikeway.domain.hikelogging.TimeProvider
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingSession
import ua.nure.kryvko.hikeway.domain.routepicking.RoutePickingStatus
import ua.nure.kryvko.hikeway.domain.routepicking.RouteTrackingProvider
import ua.nure.kryvko.hikeway.domain.routepicking.initialProgress
import ua.nure.kryvko.hikeway.domain.routes.distanceKm

class HikeTrackingController(
    private val scope: CoroutineScope,
    private val uiState: MutableStateFlow<RouteSearchUiState>,
    private val routeTrackingProvider: RouteTrackingProvider,
    private val saveCompletedHike: SaveCompletedHikeUseCase,
    private val timeProvider: TimeProvider,
    private val activeTimer: ActiveTimer,
) {
    private var trackingJob: Job? = null
    private var activeTimerJob: Job? = null

    fun startRoute(route: Route) {
        val progress = initialProgress(route) ?: return
        trackingJob?.cancel()
        activeTimerJob?.cancel()
        uiState.update {
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
        uiState.update { state ->
            state.copy(pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED))
        }
    }

    fun unpauseRoute() {
        val session = uiState.value.pickingSession ?: return
        if (session.status == RoutePickingStatus.ACTIVE) return
        uiState.update {
            it.copy(pickingSession = session.copy(status = RoutePickingStatus.ACTIVE))
        }
        startTracking(route = session.route, startIndex = session.pointIndex)
        startActiveTimer(routeId = session.route.id)
    }

    fun finishRoute() {
        val session = uiState.value.pickingSession ?: return
        trackingJob?.cancel()
        trackingJob = null
        activeTimerJob?.cancel()
        activeTimerJob = null
        uiState.update { state ->
            state.copy(pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED))
        }

        scope.launch {
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
                    uiState.update { it.copy(pickingSession = null, saveErrorMessage = null) }
                }
                .onFailure {
                    uiState.update { state ->
                        state.copy(
                            pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED),
                            saveErrorMessage = "Could not save completed hike.",
                        )
                    }
                }
        }
    }

    private fun startTracking(route: Route, startIndex: Int) {
        trackingJob?.cancel()
        trackingJob = scope.launch {
            routeTrackingProvider.positions(route, startIndex)
                .catch {
                    uiState.update { state ->
                        state.copy(
                            pickingSession = state.pickingSession?.copy(status = RoutePickingStatus.PAUSED),
                            saveErrorMessage = it.message ?: "Could not track current location.",
                        )
                    }
                }
                .collect { progress ->
                    uiState.update { state ->
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
        activeTimerJob = scope.launch {
            activeTimer.ticks().collect { deltaMillis ->
                uiState.update { state ->
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
}

private fun List<GeoPoint>.appendDistinct(value: GeoPoint): List<GeoPoint> {
    return if (lastOrNull() == value) this else this + value
}

private fun Double?.orZero(): Double = this ?: 0.0
