package ua.nure.kryvko.hikeway.model.request;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncRequest(
        String cursor,
        UUID deviceId,
        List<Mutation<RoutePayload>> routeMutations,
        List<Mutation<HikePayload>> hikeMutations
) {
    public enum Operation {
        UPSERT,
        DELETE
    }

    public record Mutation<T>(
            Operation operation,
            UUID clientId,
            long baseVersion,
            T payload
    ) {
    }

    public record RoutePayload(
            String name,
            String description,
            double distanceKm,
            int estimatedTimeMinutes,
            Difficulty difficulty,
            int elevationGainMeters,
            Terrain terrain,
            GeoJsonLineString geometry,
            List<Long> poiIds
    ) {
    }

    public record HikePayload(
            UUID routeClientId,
            Long routeServerId,
            String routeName,
            Instant startedAt,
            Instant finishedAt,
            long activeDurationMillis,
            long wallClockDurationMillis,
            double totalDistanceKm,
            GeoJsonLineString path
    ) {
    }
}
