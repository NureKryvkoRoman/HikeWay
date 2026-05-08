package ua.nure.kryvko.hikeway.model.response;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

import java.time.Instant;

public record RouteResponse(
        Long id,
        String name,
        String description,
        double distanceKm,
        int estimatedTimeMinutes,
        Difficulty difficulty,
        int elevationGain,
        Instant createdAt,
        String createdBy,
        GeoJsonLineString geometry
) {
}
