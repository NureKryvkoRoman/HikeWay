package ua.nure.kryvko.hikeway.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import ua.nure.kryvko.hikeway.core.model.GeoPoint

@Composable
fun PathMap(
    path: List<GeoPoint>,
    modifier: Modifier = Modifier,
    pathColor: Color = Color(0xFFD93025),
) {
    val geoJson = remember(path) { path.toLineStringFeatureGeoJson() }

    HikeWayMap(
        centerMode = MapCenterMode.RouteStart(path),
        initialZoom = 12.0,
        modifier = modifier,
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
