package ua.nure.kryvko.hikeway.data.geojson

import ua.nure.kryvko.hikeway.core.model.GeoPoint

object GeoJsonLineStringCodec {
    fun encode(points: List<GeoPoint>): String {
        val coordinates = points.joinToString(separator = ",") {
            "[${it.longitude},${it.latitude}]"
        }
        return """{"type":"LineString","coordinates":[$coordinates]}"""
    }

    fun decode(lineString: String): List<GeoPoint> {
        return coordinateRegex.findAll(lineString).map { match ->
            GeoPoint(
                longitude = match.groupValues[1].toDouble(),
                latitude = match.groupValues[2].toDouble(),
            )
        }.toList()
    }

    private val coordinateRegex = Regex("""\[\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*]""")
}
