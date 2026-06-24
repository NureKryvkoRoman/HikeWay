package ua.nure.kryvko.hikeway.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.nure.kryvko.hikeway.exception.InvalidUserDataException;
import ua.nure.kryvko.hikeway.exception.RouteNotFoundException;
import ua.nure.kryvko.hikeway.exception.SyncBatchTooLargeException;
import ua.nure.kryvko.hikeway.model.*;
import ua.nure.kryvko.hikeway.model.request.SyncRequest;
import ua.nure.kryvko.hikeway.model.response.SyncResponse;
import ua.nure.kryvko.hikeway.repository.CompletedHikeRepository;
import ua.nure.kryvko.hikeway.repository.RouteGeometryRepository;
import ua.nure.kryvko.hikeway.repository.RouteRepository;
import ua.nure.kryvko.hikeway.repository.SyncChangeRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
public class DataSyncService {
    private static final int MAX_MUTATIONS = 100;
    private static final int CHANGE_PAGE_SIZE = 500;

    private final RouteRepository routeRepository;
    private final RouteGeometryRepository routeGeometryRepository;
    private final CompletedHikeRepository hikeRepository;
    private final SyncChangeRepository changeRepository;
    private final GeoJsonGeometryMapper geometryMapper;
    private final RoutePoiService routePoiService;

    public DataSyncService(
            RouteRepository routeRepository,
            RouteGeometryRepository routeGeometryRepository,
            CompletedHikeRepository hikeRepository,
            SyncChangeRepository changeRepository,
            GeoJsonGeometryMapper geometryMapper,
            RoutePoiService routePoiService
    ) {
        this.routeRepository = routeRepository;
        this.routeGeometryRepository = routeGeometryRepository;
        this.hikeRepository = hikeRepository;
        this.changeRepository = changeRepository;
        this.geometryMapper = geometryMapper;
        this.routePoiService = routePoiService;
    }

    @Transactional
    public SyncResponse synchronize(SyncRequest request) {
        String ownerId = currentUserId();
        List<SyncRequest.Mutation<SyncRequest.RoutePayload>> routeMutations =
                Optional.ofNullable(request.routeMutations()).orElse(List.of());
        List<SyncRequest.Mutation<SyncRequest.HikePayload>> hikeMutations =
                Optional.ofNullable(request.hikeMutations()).orElse(List.of());
        if (routeMutations.size() + hikeMutations.size() > MAX_MUTATIONS) {
            throw new SyncBatchTooLargeException("A sync request can contain at most 100 mutations");
        }

        List<SyncResponse.AcceptedMutation> accepted = new ArrayList<>();
        List<SyncResponse.Conflict> conflicts = new ArrayList<>();
        routeMutations.forEach(mutation -> applyRouteMutation(ownerId, mutation, accepted, conflicts));
        hikeMutations.forEach(mutation -> applyHikeMutation(ownerId, mutation, accepted, conflicts));

        return changesSince(ownerId, decodeCursor(request.cursor()), accepted, conflicts);
    }

    @Transactional
    public SyncResponse.RouteChange setPublished(UUID clientId, boolean published) {
        String ownerId = currentUserId();
        Route route = routeRepository.findByCreatedByAndClientId(ownerId, clientId)
                .orElseThrow(() -> new RouteNotFoundException("Route not found"));
        if (route.isDeleted()) {
            throw new RouteNotFoundException("Route not found");
        }
        route.setVisibility(published ? RouteVisibility.PUBLIC : RouteVisibility.PRIVATE);
        route.setPublishedAt(published ? Instant.now() : null);
        route.setUpdatedAt(Instant.now());
        route.setSyncVersion(route.getSyncVersion() + 1);
        routeRepository.save(route);
        recordChange(ownerId, SyncResourceType.ROUTE, clientId, route.getSyncVersion(), false);
        return toRouteChange(route);
    }

    private void applyRouteMutation(
            String ownerId,
            SyncRequest.Mutation<SyncRequest.RoutePayload> mutation,
            List<SyncResponse.AcceptedMutation> accepted,
            List<SyncResponse.Conflict> conflicts
    ) {
        validateMutation(mutation);
        Optional<Route> existing = routeRepository.findByCreatedByAndClientId(ownerId, mutation.clientId());
        if (existing.isPresent() &&
                existing.get().getSyncVersion() == mutation.baseVersion() + 1 &&
                routeMatchesMutation(existing.get(), mutation)) {
            Route route = existing.get();
            accepted.add(new SyncResponse.AcceptedMutation(
                    SyncResourceType.ROUTE,
                    route.getClientId(),
                    route.getId(),
                    route.getSyncVersion(),
                    mutation.operation()
            ));
            return;
        }
        if (existing.isPresent() && existing.get().getSyncVersion() != mutation.baseVersion()) {
            Route route = existing.get();
            conflicts.add(new SyncResponse.Conflict(
                    SyncResourceType.ROUTE,
                    mutation.clientId(),
                    mutation.baseVersion(),
                    route.getSyncVersion(),
                    toRouteChange(route)
            ));
            return;
        }
        if (existing.isEmpty() && mutation.baseVersion() != 0) {
            throw new InvalidUserDataException("A new route must use baseVersion 0");
        }

        Route route = existing.orElseGet(Route::new);
        Instant now = Instant.now();
        if (existing.isEmpty()) {
            route.setClientId(mutation.clientId());
            route.setCreatedBy(ownerId);
            route.setCreatedAt(now);
            route.setVisibility(RouteVisibility.PRIVATE);
        }
        route.setSyncVersion(existing.map(value -> value.getSyncVersion() + 1).orElse(1L));
        route.setUpdatedAt(now);
        route.setDeleted(mutation.operation() == SyncRequest.Operation.DELETE);
        if (!route.isDeleted()) {
            applyRoutePayload(route, Objects.requireNonNull(mutation.payload()));
        }
        route = routeRepository.save(route);
        if (!route.isDeleted()) {
            saveRouteGeometry(route, mutation.payload());
            if (existing.isEmpty() || mutation.payload().poiIds() != null) {
                routePoiService.replace(route, mutation.payload().poiIds());
            }
        } else {
            routePoiService.deleteForRoute(route.getId());
        }
        recordChange(ownerId, SyncResourceType.ROUTE, route.getClientId(), route.getSyncVersion(), route.isDeleted());
        accepted.add(new SyncResponse.AcceptedMutation(
                SyncResourceType.ROUTE,
                route.getClientId(),
                route.getId(),
                route.getSyncVersion(),
                mutation.operation()
        ));
    }

    private void applyHikeMutation(
            String ownerId,
            SyncRequest.Mutation<SyncRequest.HikePayload> mutation,
            List<SyncResponse.AcceptedMutation> accepted,
            List<SyncResponse.Conflict> conflicts
    ) {
        validateMutation(mutation);
        Optional<CompletedHike> existing = hikeRepository.findByOwnerIdAndClientId(ownerId, mutation.clientId());
        if (existing.isPresent() &&
                existing.get().getSyncVersion() == mutation.baseVersion() + 1 &&
                hikeMatchesMutation(existing.get(), mutation)) {
            CompletedHike hike = existing.get();
            accepted.add(new SyncResponse.AcceptedMutation(
                    SyncResourceType.HIKE,
                    hike.getClientId(),
                    hike.getId(),
                    hike.getSyncVersion(),
                    mutation.operation()
            ));
            return;
        }
        if (existing.isPresent() && existing.get().getSyncVersion() != mutation.baseVersion()) {
            CompletedHike hike = existing.get();
            conflicts.add(new SyncResponse.Conflict(
                    SyncResourceType.HIKE,
                    mutation.clientId(),
                    mutation.baseVersion(),
                    hike.getSyncVersion(),
                    toHikeChange(hike)
            ));
            return;
        }
        if (existing.isEmpty() && mutation.baseVersion() != 0) {
            throw new InvalidUserDataException("A new hike must use baseVersion 0");
        }

        CompletedHike hike = existing.orElseGet(CompletedHike::new);
        Instant now = Instant.now();
        if (existing.isEmpty()) {
            hike.setClientId(mutation.clientId());
            hike.setOwnerId(ownerId);
            hike.setCreatedAt(now);
        }
        hike.setSyncVersion(existing.map(value -> value.getSyncVersion() + 1).orElse(1L));
        hike.setUpdatedAt(now);
        hike.setDeleted(mutation.operation() == SyncRequest.Operation.DELETE);
        if (!hike.isDeleted()) {
            applyHikePayload(hike, Objects.requireNonNull(mutation.payload()));
        }
        hike = hikeRepository.save(hike);
        recordChange(ownerId, SyncResourceType.HIKE, hike.getClientId(), hike.getSyncVersion(), hike.isDeleted());
        accepted.add(new SyncResponse.AcceptedMutation(
                SyncResourceType.HIKE,
                hike.getClientId(),
                hike.getId(),
                hike.getSyncVersion(),
                mutation.operation()
        ));
    }

    private SyncResponse changesSince(
            String ownerId,
            long cursor,
            List<SyncResponse.AcceptedMutation> accepted,
            List<SyncResponse.Conflict> conflicts
    ) {
        List<SyncChange> page = changeRepository.findByOwnerIdAndIdGreaterThanOrderByIdAsc(
                ownerId,
                cursor,
                PageRequest.of(0, CHANGE_PAGE_SIZE + 1)
        );
        boolean hasMore = page.size() > CHANGE_PAGE_SIZE;
        List<SyncChange> included = hasMore ? page.subList(0, CHANGE_PAGE_SIZE) : page;
        List<SyncResponse.RouteChange> routes = new ArrayList<>();
        List<SyncResponse.HikeChange> hikes = new ArrayList<>();
        included.forEach(change -> {
            if (change.getResourceType() == SyncResourceType.ROUTE) {
                routeRepository.findByCreatedByAndClientId(ownerId, change.getClientId())
                        .map(this::toRouteChange)
                        .ifPresent(routes::add);
            } else {
                hikeRepository.findByOwnerIdAndClientId(ownerId, change.getClientId())
                        .map(this::toHikeChange)
                        .ifPresent(hikes::add);
            }
        });
        long nextCursor = included.isEmpty() ? cursor : included.get(included.size() - 1).getId();
        return new SyncResponse(
                encodeCursor(nextCursor),
                hasMore,
                accepted,
                conflicts,
                routes,
                hikes
        );
    }

    private void applyRoutePayload(Route route, SyncRequest.RoutePayload payload) {
        if (payload.name() == null || payload.name().isBlank() ||
                payload.description() == null || payload.description().isBlank() ||
                payload.distanceKm() < 0 || payload.estimatedTimeMinutes() < 0 ||
                payload.elevationGainMeters() < 0) {
            throw new InvalidUserDataException("Invalid route data");
        }
        route.setName(payload.name());
        route.setDescription(payload.description());
        route.setDistanceKm(payload.distanceKm());
        route.setEstimatedTimeMinutes(payload.estimatedTimeMinutes());
        route.setDifficulty(Objects.requireNonNull(payload.difficulty()));
        route.setElevationGain(payload.elevationGainMeters());
        route.setTerrain(Objects.requireNonNull(payload.terrain()));
    }

    private boolean routeMatchesMutation(
            Route route,
            SyncRequest.Mutation<SyncRequest.RoutePayload> mutation
    ) {
        if (mutation.operation() == SyncRequest.Operation.DELETE) {
            return route.isDeleted();
        }
        SyncRequest.RoutePayload payload = mutation.payload();
        RouteGeometry geometry = routeGeometryRepository.findByRoute(route).orElse(null);
        return !route.isDeleted() &&
                payload != null &&
                Objects.equals(route.getName(), payload.name()) &&
                Objects.equals(route.getDescription(), payload.description()) &&
                Double.compare(route.getDistanceKm(), payload.distanceKm()) == 0 &&
                route.getEstimatedTimeMinutes() == payload.estimatedTimeMinutes() &&
                route.getDifficulty() == payload.difficulty() &&
                route.getElevationGain() == payload.elevationGainMeters() &&
                route.getTerrain() == payload.terrain() &&
                geometry != null &&
                Objects.equals(geometryMapper.toGeoJson(geometry.getLine()), payload.geometry()) &&
                (payload.poiIds() == null ||
                        Objects.equals(routePoiService.ids(route.getId()), normalizedPoiIds(payload.poiIds())));
    }

    private void saveRouteGeometry(Route route, SyncRequest.RoutePayload payload) {
        RouteGeometry geometry = routeGeometryRepository.findByRoute(route).orElseGet(RouteGeometry::new);
        geometry.setRoute(route);
        geometry.setLine(geometryMapper.toLineString(payload.geometry(), false));
        routeGeometryRepository.save(geometry);
    }

    private void applyHikePayload(CompletedHike hike, SyncRequest.HikePayload payload) {
        if (payload.routeName() == null || payload.routeName().isBlank() ||
                payload.startedAt() == null || payload.finishedAt() == null ||
                payload.finishedAt().isBefore(payload.startedAt()) ||
                payload.activeDurationMillis() < 0 || payload.wallClockDurationMillis() < 0 ||
                payload.totalDistanceKm() < 0) {
            throw new InvalidUserDataException("Invalid completed hike data");
        }
        hike.setRouteClientId(payload.routeClientId());
        hike.setRouteServerId(payload.routeServerId());
        hike.setRouteName(payload.routeName());
        hike.setStartedAt(payload.startedAt());
        hike.setFinishedAt(payload.finishedAt());
        hike.setActiveDurationMillis(payload.activeDurationMillis());
        hike.setWallClockDurationMillis(payload.wallClockDurationMillis());
        hike.setTotalDistanceKm(payload.totalDistanceKm());
        hike.setPath(geometryMapper.toLineString(payload.path(), true));
    }

    private boolean hikeMatchesMutation(
            CompletedHike hike,
            SyncRequest.Mutation<SyncRequest.HikePayload> mutation
    ) {
        if (mutation.operation() == SyncRequest.Operation.DELETE) {
            return hike.isDeleted();
        }
        SyncRequest.HikePayload payload = mutation.payload();
        return !hike.isDeleted() &&
                payload != null &&
                Objects.equals(hike.getRouteClientId(), payload.routeClientId()) &&
                Objects.equals(hike.getRouteServerId(), payload.routeServerId()) &&
                Objects.equals(hike.getRouteName(), payload.routeName()) &&
                Objects.equals(hike.getStartedAt(), payload.startedAt()) &&
                Objects.equals(hike.getFinishedAt(), payload.finishedAt()) &&
                hike.getActiveDurationMillis() == payload.activeDurationMillis() &&
                hike.getWallClockDurationMillis() == payload.wallClockDurationMillis() &&
                Double.compare(hike.getTotalDistanceKm(), payload.totalDistanceKm()) == 0 &&
                Objects.equals(geometryMapper.toGeoJson(hike.getPath()), payload.path());
    }

    private SyncResponse.RouteChange toRouteChange(Route route) {
        RouteGeometry geometry = routeGeometryRepository.findByRoute(route).orElse(null);
        return new SyncResponse.RouteChange(
                route.getClientId(),
                route.getId(),
                route.getSyncVersion(),
                route.isDeleted() ? null : route.getName(),
                route.isDeleted() ? null : route.getDescription(),
                route.isDeleted() ? null : route.getDistanceKm(),
                route.isDeleted() ? null : route.getEstimatedTimeMinutes(),
                route.isDeleted() ? null : route.getDifficulty(),
                route.isDeleted() ? null : route.getElevationGain(),
                route.isDeleted() ? null : route.getTerrain(),
                route.isDeleted() || geometry == null ? null : geometryMapper.toGeoJson(geometry.getLine()),
                route.isDeleted() ? List.of() : routePoiService.summaries(route.getId()),
                route.getVisibility(),
                route.getCreatedAt(),
                route.getUpdatedAt(),
                route.getPublishedAt(),
                route.isDeleted()
        );
    }

    private SyncResponse.HikeChange toHikeChange(CompletedHike hike) {
        return new SyncResponse.HikeChange(
                hike.getClientId(),
                hike.getId(),
                hike.getSyncVersion(),
                hike.getRouteClientId(),
                hike.getRouteServerId(),
                hike.isDeleted() ? null : hike.getRouteName(),
                hike.isDeleted() ? null : hike.getStartedAt(),
                hike.isDeleted() ? null : hike.getFinishedAt(),
                hike.isDeleted() ? null : hike.getActiveDurationMillis(),
                hike.isDeleted() ? null : hike.getWallClockDurationMillis(),
                hike.isDeleted() ? null : hike.getTotalDistanceKm(),
                hike.isDeleted() ? null : geometryMapper.toGeoJson(hike.getPath()),
                hike.getCreatedAt(),
                hike.getUpdatedAt(),
                hike.isDeleted()
        );
    }

    private <T> void validateMutation(SyncRequest.Mutation<T> mutation) {
        if (mutation == null || mutation.operation() == null || mutation.clientId() == null ||
                mutation.baseVersion() < 0 ||
                (mutation.operation() == SyncRequest.Operation.UPSERT && mutation.payload() == null)) {
            throw new InvalidUserDataException("Invalid sync mutation");
        }
    }

    private List<Long> normalizedPoiIds(List<Long> poiIds) {
        return poiIds == null ? List.of() : poiIds;
    }

    private void recordChange(
            String ownerId,
            SyncResourceType resourceType,
            UUID clientId,
            long version,
            boolean deleted
    ) {
        SyncChange change = new SyncChange();
        change.setOwnerId(ownerId);
        change.setResourceType(resourceType);
        change.setClientId(clientId);
        change.setResourceVersion(version);
        change.setDeleted(deleted);
        change.setCreatedAt(Instant.now());
        changeRepository.save(change);
    }

    private String currentUserId() {
        return ((Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getSubject();
    }

    private String encodeCursor(long cursor) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(cursor).getBytes(StandardCharsets.UTF_8));
    }

    private long decodeCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            ));
        } catch (RuntimeException exception) {
            throw new InvalidUserDataException("Invalid sync cursor");
        }
    }
}
