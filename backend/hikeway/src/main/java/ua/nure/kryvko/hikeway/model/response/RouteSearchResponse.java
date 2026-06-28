package ua.nure.kryvko.hikeway.model.response;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

import java.time.Instant;

public record RouteSearchResponse(
        Long id,
        String name,
        String description,
        double distanceKm,
        int estimatedTimeMinutes,
        Difficulty difficulty,
        int elevationGain,
        Terrain terrain,
        Instant createdAt,
        String createdBy,
        GeoJsonLineString geometry
) {
}
