package ua.nure.kryvko.hikeway.model.request;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Terrain;

import java.util.Set;

public record RouteSearchRequest(
        Double minDistanceKm,
        Double maxDistanceKm,
        Integer minEstimatedTimeMinutes,
        Integer maxEstimatedTimeMinutes,
        Set<Difficulty> difficulties,
        Set<Terrain> terrains,
        Double longitude,
        Double latitude,
        Double maxProximityKm,
        int page,
        int size
) {
}
