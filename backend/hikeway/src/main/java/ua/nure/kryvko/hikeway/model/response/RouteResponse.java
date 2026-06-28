package ua.nure.kryvko.hikeway.model.response;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Terrain;

import java.time.Instant;
import java.util.List;

/**
 * General-purpose Route DTO, includes only route metadata
 */
public record RouteResponse(
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
        List<PoiResponses.Summary> pointsOfInterest
) {
}
