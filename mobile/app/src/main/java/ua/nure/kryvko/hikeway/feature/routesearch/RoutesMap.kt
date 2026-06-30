package ua.nure.kryvko.hikeway.feature.routesearch

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import org.maplibre.compose.expressions.value.IconRotationAlignment
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import ua.nure.kryvko.hikeway.R
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route
import ua.nure.kryvko.hikeway.ui.map.HikeWayMap
import ua.nure.kryvko.hikeway.ui.map.MapCenterMode
import ua.nure.kryvko.hikeway.ui.map.MapCenterRequest
import ua.nure.kryvko.hikeway.ui.map.emptyFeatureCollectionGeoJson
import ua.nure.kryvko.hikeway.ui.map.toGeoPoint
import ua.nure.kryvko.hikeway.ui.map.toLineStringFeatureGeoJson
import ua.nure.kryvko.hikeway.ui.map.toPoiFeatureCollectionGeoJson
import ua.nure.kryvko.hikeway.ui.map.toPointFeatureGeoJson
import ua.nure.kryvko.hikeway.ui.map.toRouteFeatureCollectionGeoJson

@Composable
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
fun RoutesMap(
    routes: List<Route>,
    walkedPath: List<GeoPoint>,
    userPosition: GeoPoint?,
    userBearingDegrees: Double,
    pointsOfInterest: List<PointOfInterest>,
    contextPoint: GeoPoint?,
    mapCenter: GeoPoint?,
    mapCenterRequestId: Long,
    onCenterLocation: () -> Unit,
    onPoiClick: (Long) -> Unit,
    onMapLongClick: ((GeoPoint) -> Unit)?,
    onMapViewportChanged: (GeoPoint, Double) -> Unit,
    centerMode: MapCenterMode,
    highlightedMode: Boolean,
) {
    val geoJson = remember(routes) { routes.toRouteFeatureCollectionGeoJson() }
    val walkedPathGeoJson = remember(walkedPath) { walkedPath.toLineStringFeatureGeoJson() }
    val userGeoJson = remember(userPosition) { userPosition.toPointFeatureGeoJson() }
    val poiGeoJson = remember(pointsOfInterest) { pointsOfInterest.toPoiFeatureCollectionGeoJson() }
    val contextGeoJson = remember(contextPoint) {
        contextPoint?.toPointFeatureGeoJson() ?: emptyFeatureCollectionGeoJson()
    }
    val arrowPainter = rememberVectorPainter(Icons.Default.KeyboardArrowUp)
    val poiPainter = painterResource(R.drawable.poi_marker_24)
    val currentLocationCenterRequest = mapCenter?.let {
        MapCenterRequest(
            target = it,
            zoom = 14.0,
            requestId = mapCenterRequestId,
        )
    }
    val routeStartCenterRequest = (centerMode as? MapCenterMode.RouteStart)
        ?.points
        ?.firstOrNull()
        ?.let {
            MapCenterRequest(
                target = it,
                zoom = 12.0,
                requestId = it.hashCode().toLong(),
            )
        }

    HikeWayMap(
        centerMode = centerMode,
        currentLocation = mapCenter,
        centerRequest = routeStartCenterRequest ?: currentLocationCenterRequest,
        onMapLongClick = onMapLongClick,
        cameraEffect = { cameraState ->
            LaunchedEffect(cameraState) {
                snapshotFlow {
                    cameraState.position.target.toGeoPoint() to cameraState.position.zoom
                }
                    .distinctUntilChanged()
                    .collect { (center, zoom) ->
                        onMapViewportChanged(center, zoom)
                    }
            }
        },
        overlays = {
            Button(
                onClick = onCenterLocation,
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(70.dp)
                    .padding(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.center_24),
                    contentDescription = "Center location button",
                )
            }
        },
    ) {
        val routesSource = rememberGeoJsonSource(GeoJsonData.JsonString(geoJson))
        LineLayer(
            id = "matching-routes",
            source = routesSource,
            color = const(if (highlightedMode) Color(0xFF0B57D0) else Color(0xFF176B3A)),
            width = const(if (highlightedMode) 6.dp else 4.dp),
        )
        if (walkedPath.isNotEmpty()) {
            val walkedPathSource =
                rememberGeoJsonSource(GeoJsonData.JsonString(walkedPathGeoJson))
            LineLayer(
                id = "walked-path",
                source = walkedPathSource,
                color = const(Color(0xFFD93025)),
                width = const(4.dp),
            )
        }
        if (userPosition != null) {
            val userSource = rememberGeoJsonSource(GeoJsonData.JsonString(userGeoJson))
            SymbolLayer(
                id = "user-position",
                source = userSource,
                iconImage = image(
                    value = arrowPainter,
                    size = DpSize(32.dp, 32.dp),
                    drawAsSdf = true,
                ),
                iconColor = const(Color(0xFFD93025)),
                iconHaloColor = const(Color.White),
                iconHaloWidth = const(2.dp),
                iconAllowOverlap = const(true),
                iconIgnorePlacement = const(true),
                iconRotationAlignment = const(IconRotationAlignment.Map),
                iconRotate = const(userBearingDegrees.toFloat()),
            )
        }
        if (pointsOfInterest.isNotEmpty()) {
            val poiSource = rememberGeoJsonSource(GeoJsonData.JsonString(poiGeoJson))
            SymbolLayer(
                id = "points-of-interest",
                source = poiSource,
                iconImage = image(
                    value = poiPainter,
                    size = DpSize(34.dp, 34.dp),
                    drawAsSdf = true,
                ),
                iconColor = const(Color(0xFFB3261E)),
                iconHaloColor = const(Color.White),
                iconHaloWidth = const(2.dp),
                iconAllowOverlap = const(true),
                iconIgnorePlacement = const(true),
                onClick = { features ->
                    val poiId = features.firstOrNull()
                        ?.properties
                        ?.get("poiId")
                        ?.jsonPrimitive
                        ?.longOrNull
                    if (poiId != null) {
                        onPoiClick(poiId)
                        ClickResult.Consume
                    } else {
                        ClickResult.Pass
                    }
                },
            )
        }
        if (contextPoint != null) {
            val contextSource = rememberGeoJsonSource(GeoJsonData.JsonString(contextGeoJson))
            SymbolLayer(
                id = "map-context-point",
                source = contextSource,
                iconImage = image(
                    value = painterResource(R.drawable.center_24),
                    size = DpSize(34.dp, 34.dp),
                    drawAsSdf = true,
                ),
                iconColor = const(Color(0xFF0B57D0)),
                iconHaloColor = const(Color.White),
                iconHaloWidth = const(2.dp),
                iconAllowOverlap = const(true),
                iconIgnorePlacement = const(true),
            )
        }
    }
}
