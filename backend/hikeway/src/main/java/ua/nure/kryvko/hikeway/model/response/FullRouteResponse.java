package ua.nure.kryvko.hikeway.model.response;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

import java.time.Instant;
import java.util.List;

/**
 * Full route data, including GeoJson geometry and route metadata.
 */
public record FullRouteResponse(
        Long id,
        String name,
        String description,
        double distanceKm,
        int estimatedTimeMinutes,
        Difficulty difficulty,
        int elevationGain,
        Instant createdAt,
        String createdBy,
        GeoJsonLineString geometry,
        List<PoiResponses.Summary> pointsOfInterest
) {
}
