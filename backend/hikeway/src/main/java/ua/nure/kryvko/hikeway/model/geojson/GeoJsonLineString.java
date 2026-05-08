package ua.nure.kryvko.hikeway.model.geojson;

import java.util.List;

public record GeoJsonLineString(
        String type,
        List<List<Double>> coordinates
) {
}
