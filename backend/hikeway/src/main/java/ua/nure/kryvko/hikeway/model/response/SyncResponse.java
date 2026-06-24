package ua.nure.kryvko.hikeway.model.response;

import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.RouteVisibility;
import ua.nure.kryvko.hikeway.model.SyncResourceType;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;
import ua.nure.kryvko.hikeway.model.request.SyncRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SyncResponse(
        String cursor,
        boolean hasMore,
        List<AcceptedMutation> accepted,
        List<Conflict> conflicts,
        List<RouteChange> routeChanges,
        List<HikeChange> hikeChanges
) {
    public record AcceptedMutation(
            SyncResourceType resourceType,
            UUID clientId,
            long serverId,
            long version,
            SyncRequest.Operation operation
    ) {
    }

    public record Conflict(
            SyncResourceType resourceType,
            UUID clientId,
            long submittedBaseVersion,
            long serverVersion,
            Object serverRecord
    ) {
    }

    public record RouteChange(
            UUID clientId,
            long serverId,
            long version,
            String name,
            String description,
            Double distanceKm,
            Integer estimatedTimeMinutes,
            Difficulty difficulty,
            Integer elevationGainMeters,
            Terrain terrain,
            GeoJsonLineString geometry,
            RouteVisibility visibility,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            boolean deleted
    ) {
    }

    public record HikeChange(
            UUID clientId,
            long serverId,
            long version,
            UUID routeClientId,
            Long routeServerId,
            String routeName,
            Instant startedAt,
            Instant finishedAt,
            Long activeDurationMillis,
            Long wallClockDurationMillis,
            Double totalDistanceKm,
            GeoJsonLineString path,
            Instant createdAt,
            Instant updatedAt,
            boolean deleted
    ) {
    }
}
