package ua.nure.kryvko.hikeway.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint
import ua.nure.kryvko.hikeway.core.model.PointOfInterest

class MapGeoJsonTest {
    @Test
    fun lineStringFeatureUsesLongitudeLatitudeOrder() {
        val path = listOf(
            GeoPoint(longitude = 24.0316, latitude = 49.8429),
            GeoPoint(longitude = 24.0394, latitude = 49.8461),
        )

        assertEquals(
            """{"type":"Feature","properties":{},"geometry":{"type":"LineString","coordinates":[[24.0316,49.8429],[24.0394,49.8461]]}}""",
            path.toLineStringFeatureGeoJson(),
        )
    }

    @Test
    fun emptyLineStringReturnsEmptyFeatureCollection() {
        assertEquals(emptyFeatureCollectionGeoJson(), emptyList<GeoPoint>().toLineStringFeatureGeoJson())
    }

    @Test
    fun nullPointReturnsEmptyFeatureCollection() {
        assertEquals(emptyFeatureCollectionGeoJson(), (null as GeoPoint?).toPointFeatureGeoJson())
    }

    @Test
    fun poiFeatureCollectionUsesIdAndLongitudeLatitudeOrder() {
        val pois = listOf(
            PointOfInterest(
                id = 7,
                name = "Viewpoint",
                description = "Wide view",
                location = GeoPoint(longitude = 24.0316, latitude = 49.8429),
                photoResIds = emptyList(),
                averageRating = 4.5,
            )
        )

        assertEquals(
            """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"poiId":7},"geometry":{"type":"Point","coordinates":[24.0316,49.8429]}}]}""",
            pois.toPoiFeatureCollectionGeoJson(),
        )
    }
}
