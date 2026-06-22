package ua.nure.kryvko.hikeway.ui.map

import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest
import ua.nure.kryvko.hikeway.core.model.Route

fun emptyFeatureCollectionGeoJson(): String {
    return """{"type":"FeatureCollection","features":[]}"""
}

fun GeoPoint?.toPointFeatureGeoJson(): String {
    return if (this == null) {
        emptyFeatureCollectionGeoJson()
    } else {
        """{"type":"Feature","properties":{},"geometry":{"type":"Point","coordinates":[$longitude,$latitude]}}"""
    }
}

fun List<GeoPoint>.toLineStringFeatureGeoJson(): String {
    if (isEmpty()) return emptyFeatureCollectionGeoJson()
    val coordinates = joinToString(separator = ",") {
        "[${it.longitude},${it.latitude}]"
    }
    return """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
}

fun List<Route>.toRouteFeatureCollectionGeoJson(): String {
    val features = joinToString(separator = ",") { route ->
        val coordinates = route.geometry.points.joinToString(separator = ",") {
            "[${it.longitude},${it.latitude}]"
        }
        """{"type":"Feature","properties":{"routeId":${route.id}},"geometry":{"type":"LineString","coordinates":[$coordinates]}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}

fun List<PointOfInterest>.toPoiFeatureCollectionGeoJson(): String {
    val features = joinToString(separator = ",") { poi ->
        """{"type":"Feature","properties":{"poiId":${poi.id}},"geometry":{"type":"Point","coordinates":[${poi.location.longitude},${poi.location.latitude}]}}"""
    }
    return """{"type":"FeatureCollection","features":[$features]}"""
}
