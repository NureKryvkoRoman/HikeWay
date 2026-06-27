package ua.nure.kryvko.hikeway.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlin.time.Duration.Companion.milliseconds
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Position
import ua.nure.kryvko.hikeway.core.model.GeoPoint

const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
val DEFAULT_MAP_CENTER = GeoPoint(longitude = 24.0316, latitude = 49.8429)
const val DEFAULT_MAP_ZOOM = 10.0

data class MapCenterRequest(
    val target: GeoPoint,
    val zoom: Double? = null,
    val requestId: Long = 0L,
)

sealed interface MapCenterMode {
    data object CurrentLocation : MapCenterMode
    data class RouteStart(val points: List<GeoPoint>) : MapCenterMode
    data class Fixed(val point: GeoPoint) : MapCenterMode
}

fun MapCenterMode.resolveCenter(
    currentLocation: GeoPoint? = null,
    defaultCenter: GeoPoint = DEFAULT_MAP_CENTER,
): GeoPoint {
    return when (this) {
        MapCenterMode.CurrentLocation -> currentLocation ?: defaultCenter
        is MapCenterMode.RouteStart -> points.firstOrNull() ?: defaultCenter
        is MapCenterMode.Fixed -> point
    }
}

@Composable
fun HikeWayMap(
    centerMode: MapCenterMode,
    modifier: Modifier = Modifier,
    currentLocation: GeoPoint? = null,
    initialZoom: Double = DEFAULT_MAP_ZOOM,
    centerRequest: MapCenterRequest? = null,
    cameraEffect: @Composable (CameraState) -> Unit = {},
    onMapLongClick: ((GeoPoint) -> Unit)? = null,
    overlays: @Composable BoxScope.() -> Unit = {},
    content: @Composable @MaplibreComposable () -> Unit,
) {
    val initialCenter = remember(centerMode, currentLocation) {
        centerMode.resolveCenter(currentLocation)
    }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = initialCenter.toPosition(),
            zoom = initialZoom,
        )
    )

    LaunchedEffect(centerRequest) {
        centerRequest?.let { request ->
            cameraState.animateTo(
                finalPosition = cameraState.position.copy(
                    target = request.target.toPosition(),
                    zoom = request.zoom ?: cameraState.position.zoom,
                ),
                duration = 300.milliseconds,
            )
        }
    }
    cameraEffect(cameraState)

    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            baseStyle = BaseStyle.Uri(MAP_STYLE),
            cameraState = cameraState,
            onMapLongClick = { position, _ ->
                if (onMapLongClick == null) {
                    ClickResult.Pass
                } else {
                    onMapLongClick(position.toGeoPoint())
                    ClickResult.Consume
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
        }
        overlays()
    }
}

fun GeoPoint.toPosition(): Position {
    return Position(longitude = longitude, latitude = latitude)
}

fun Position.toGeoPoint(): GeoPoint {
    return GeoPoint(longitude = longitude, latitude = latitude)
}
