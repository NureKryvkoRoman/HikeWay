package ua.nure.kryvko.hikeway.feature.routecreation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.ui.map.HikeWayMap
import ua.nure.kryvko.hikeway.ui.map.MapCenterMode
import ua.nure.kryvko.hikeway.ui.map.emptyFeatureCollectionGeoJson
import ua.nure.kryvko.hikeway.ui.map.toGeoPoint
import ua.nure.kryvko.hikeway.ui.map.toLineStringFeatureGeoJson
import ua.nure.kryvko.hikeway.ui.map.toPoiFeatureCollectionGeoJson

@Composable
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
fun RouteBuilderMap(
    points: List<GeoPoint>,
    crosshairPoint: GeoPoint,
    pointsOfInterest: List<PointOfInterest>,
    onCrosshairChange: (GeoPoint) -> Unit,
    onPoiClick: (Long) -> Unit,
) {
    val solidGeoJson = remember(points) { points.toLineStringFeatureGeoJson() }
    val previewGeoJson = remember(points, crosshairPoint) {
        points.lastOrNull()
            ?.let { listOf(it, crosshairPoint).toLineStringFeatureGeoJson() }
            ?: emptyFeatureCollectionGeoJson()
    }
    val poiGeoJson = remember(pointsOfInterest) { pointsOfInterest.toPoiFeatureCollectionGeoJson() }
    val poiPainter = painterResource(R.drawable.poi_marker_24)

    HikeWayMap(
        centerMode = MapCenterMode.Fixed(crosshairPoint),
        initialZoom = 12.0,
        cameraEffect = { cameraState ->
            LaunchedEffect(cameraState) {
                snapshotFlow { cameraState.position.target }
                    .distinctUntilChanged()
                    .collect { target ->
                        onCrosshairChange(target.toGeoPoint())
                    }
            }
        },
    ) {
        if (points.size >= 2) {
            val solidSource = rememberGeoJsonSource(GeoJsonData.JsonString(solidGeoJson))
            LineLayer(
                id = "created-route",
                source = solidSource,
                color = const(Color(0xFF0B57D0)),
                width = const(5.dp),
            )
        }
        if (points.isNotEmpty()) {
            val previewSource = rememberGeoJsonSource(GeoJsonData.JsonString(previewGeoJson))
            LineLayer(
                id = "route-preview-segment",
                source = previewSource,
                color = const(Color(0xFFD93025)),
                dasharray = const(listOf(1.5, 1.5)),
                width = const(4.dp),
            )
        }
        if (pointsOfInterest.isNotEmpty()) {
            val poiSource = rememberGeoJsonSource(GeoJsonData.JsonString(poiGeoJson))
            SymbolLayer(
                id = "route-creation-points-of-interest",
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
    }
}
