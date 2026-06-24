package ua.nure.kryvko.hikeway.model.request;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

import java.util.List;

public record CreateRouteRequest(
        String name,
        String description,
        double distanceKm,
        int estimatedTimeMinutes,
        Difficulty difficulty,
        int elevationGain,
        GeoJsonLineString geometry,
        List<Long> poiIds
) {
}
