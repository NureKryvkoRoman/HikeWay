package ua.nure.kryvko.hikeway.model.request;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

public record UpdateRouteRequest(
        String name,
        String description,
        Double distanceKm,
        Integer estimatedTimeMinutes,
        Difficulty difficulty,
        Integer elevationGain,
        GeoJsonLineString geometry
) {
}
