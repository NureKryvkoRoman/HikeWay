package ua.nure.kryvko.hikeway.data.geojson

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.nure.kryvko.hikeway.core.model.GeoPoint

class GeoJsonLineStringCodecTest {
    @Test
    fun encodesLineStringWithLongitudeLatitudeOrder() {
        val points = listOf(
            GeoPoint(longitude = 24.0316, latitude = 49.8429),
            GeoPoint(longitude = 24.0394, latitude = 49.8461),
        )

        assertEquals(
            """{"type":"LineString","coordinates":[[24.0316,49.8429],[24.0394,49.8461]]}""",
            GeoJsonLineStringCodec.encode(points),
        )
    }

    @Test
    fun decodesLineStringCoordinates() {
        val lineString = """{"type":"LineString","coordinates":[[24.0316,49.8429],[24.0394,49.8461]]}"""

        assertEquals(
            listOf(
                GeoPoint(longitude = 24.0316, latitude = 49.8429),
                GeoPoint(longitude = 24.0394, latitude = 49.8461),
            ),
            GeoJsonLineStringCodec.decode(lineString),
        )
    }

    @Test
    fun encodesEmptyLineString() {
        assertEquals(
            """{"type":"LineString","coordinates":[]}""",
            GeoJsonLineStringCodec.encode(emptyList()),
        )
    }
}
