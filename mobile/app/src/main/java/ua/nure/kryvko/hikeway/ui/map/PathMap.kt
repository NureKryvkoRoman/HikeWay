package ua.nure.kryvko.hikeway.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import ua.nure.kryvko.hikeway.core.model.GeoPoint

private const val MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun PathMap(
    path: List<GeoPoint>,
    modifier: Modifier = Modifier,
    pathColor: Color = Color(0xFFD93025),
) {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = path.firstOrNull()?.let {
                Position(longitude = it.longitude, latitude = it.latitude)
            } ?: Position(longitude = 24.0316, latitude = 49.8429),
            zoom = 12.0,
        )
    )
    val geoJson = remember(path) { path.toGeoJsonLineStringFeature() }

    MaplibreMap(
        baseStyle = BaseStyle.Uri(MAP_STYLE),
        cameraState = cameraState,
        modifier = modifier.fillMaxSize(),
    ) {
        val source = rememberGeoJsonSource(GeoJsonData.JsonString(geoJson))
        LineLayer(
            id = "path",
            source = source,
            color = const(pathColor),
            width = const(5.dp),
        )
    }
}

private fun List<GeoPoint>.toGeoJsonLineStringFeature(): String {
    if (isEmpty()) {
        return """{"type":"FeatureCollection","features":[]}"""
    }
    val coordinates = joinToString(separator = ",") {
        "[${it.longitude},${it.latitude}]"
    }
    return """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
}
