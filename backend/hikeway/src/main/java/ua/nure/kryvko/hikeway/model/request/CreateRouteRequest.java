package ua.nure.kryvko.hikeway.model.request;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

public record CreateRouteRequest(
        String name,
        String description,
        double distanceKm,
        int estimatedTimeMinutes,
        Difficulty difficulty,
        int elevationGain,
        GeoJsonLineString geometry
) {
}
