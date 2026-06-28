package ua.nure.kryvko.hikeway.service;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.nure.kryvko.hikeway.exception.InvalidPoiDataException;
import ua.nure.kryvko.hikeway.exception.InvalidRouteGeometryException;
import ua.nure.kryvko.hikeway.exception.RouteNotFoundException;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;
import ua.nure.kryvko.hikeway.model.request.CreateRouteRequest;
import ua.nure.kryvko.hikeway.model.request.RouteSearchRequest;
import ua.nure.kryvko.hikeway.model.request.UpdateRouteRequest;
import ua.nure.kryvko.hikeway.model.response.FullRouteResponse;
import ua.nure.kryvko.hikeway.model.response.PageResponse;
import ua.nure.kryvko.hikeway.model.response.RouteResponse;
import ua.nure.kryvko.hikeway.model.response.RouteSearchResponse;
import ua.nure.kryvko.hikeway.repository.RouteGeometryRepository;
import ua.nure.kryvko.hikeway.repository.RouteRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class RouteService {
    private static final String LINE_STRING_TYPE = "LineString";
    private static final int MAX_PAGE_SIZE = 100;
    private final RouteRepository routeRepository;
    private final RouteGeometryRepository routeGeometryRepository;
    private final RoutePoiService routePoiService;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Autowired
    public RouteService(
            RouteRepository routeRepository,
            RouteGeometryRepository routeGeometryRepository,
            RoutePoiService routePoiService
    ) {
        this.routeRepository = routeRepository;
        this.routeGeometryRepository = routeGeometryRepository;
        this.routePoiService = routePoiService;
    }

    @Transactional(readOnly = true)
    public PageResponse<RouteSearchResponse> searchRoutes(RouteSearchRequest request) {
        validateSearch(request);
        var routes = routeRepository.searchPublishedRoutes(
                request,
                PageRequest.of(request.page(), request.size())
        );
        return PageResponse.from(
                routes,
                routes.stream().map(this::toSearchResponse).toList()
        );
    }

    public FullRouteResponse createRoute(CreateRouteRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = ((Jwt) authentication.getPrincipal()).getSubject();

        Route route = new Route();
        route.setName(request.name());
        route.setDescription(request.description());
        route.setDistanceKm(request.distanceKm());
        route.setEstimatedTimeMinutes(request.estimatedTimeMinutes());
        route.setDifficulty(request.difficulty());
        route.setElevationGain(request.elevationGain());
        route.setCreatedAt(Instant.now());
        route.setCreatedBy(createdBy);

        route = routeRepository.save(route);

        RouteGeometry geometry = null;
        if (request.geometry() != null) {
            geometry = new RouteGeometry();
            geometry.setRoute(route);
            geometry.setLine(createLineString(request.geometry()));
            geometry = routeGeometryRepository.save(geometry);
        }
        routePoiService.replace(route, request.poiIds());

        return toFullRouteResponse(route, geometry);
    }

    public FullRouteResponse getRouteWithGeometry(Long id) {
        Route route = getRouteEntity(id);
        RouteGeometry geometry = routeGeometryRepository.findByRoute(route).orElse(null);
        return toFullRouteResponse(route, geometry);
    }

    public RouteResponse getRoute(Long id) {
        Route route = getRouteEntity(id);
        return toRouteResponse(route);
    }

    public GeoJsonLineString getRouteGeometry(Long routeId) {
        RouteGeometry geometry = routeGeometryRepository.findByRouteId(routeId)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with id: " + routeId));
        return toGeoJson(geometry.getLine());
    }

    public FullRouteResponse updateRoute(Long id, UpdateRouteRequest request) {
        Route route = getRouteEntity(id);

        if (request.name() != null) route.setName(request.name());
        if (request.description() != null) route.setDescription(request.description());
        if (request.distanceKm() != null) route.setDistanceKm(request.distanceKm());
        if (request.estimatedTimeMinutes() != null) route.setEstimatedTimeMinutes(request.estimatedTimeMinutes());
        if (request.difficulty() != null) route.setDifficulty(request.difficulty());
        if (request.elevationGain() != null) route.setElevationGain(request.elevationGain());

        route = routeRepository.save(route);

        RouteGeometry geometry = routeGeometryRepository.findByRoute(route).orElse(null);
        if (request.geometry() != null) {
            if (geometry == null) {
                geometry = new RouteGeometry();
                geometry.setRoute(route);
            }
            geometry.setLine(createLineString(request.geometry()));
            geometry = routeGeometryRepository.save(geometry);
        }
        if (request.poiIds() != null) {
            routePoiService.replace(route, request.poiIds());
        }

        return toFullRouteResponse(route, geometry);
    }

    public void deleteRoute(Long id) {
        Route route = getRouteEntity(id);
        routePoiService.deleteForRoute(id);
        routeGeometryRepository.findByRoute(route).ifPresent(routeGeometryRepository::delete);
        routeRepository.delete(route);
    }

    private Route getRouteEntity(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with id: " + id));
    }

    private FullRouteResponse toFullRouteResponse(Route route, RouteGeometry geometry) {
        return new FullRouteResponse(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getDistanceKm(),
                route.getEstimatedTimeMinutes(),
                route.getDifficulty(),
                route.getElevationGain(),
                route.getTerrain(),
                route.getCreatedAt(),
                route.getCreatedBy(),
                geometry == null ? null : toGeoJson(geometry.getLine()),
                routePoiService.summaries(route.getId())
        );
    }

    private RouteResponse toRouteResponse(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getDistanceKm(),
                route.getEstimatedTimeMinutes(),
                route.getDifficulty(),
                route.getElevationGain(),
                route.getTerrain(),
                route.getCreatedAt(),
                route.getCreatedBy(),
                routePoiService.summaries(route.getId())
        );
    }

    private RouteSearchResponse toSearchResponse(RouteGeometry geometry) {
        Route route = geometry.getRoute();
        return new RouteSearchResponse(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getDistanceKm(),
                route.getEstimatedTimeMinutes(),
                route.getDifficulty(),
                route.getElevationGain(),
                route.getTerrain(),
                route.getCreatedAt(),
                route.getCreatedBy(),
                toGeoJson(geometry.getLine())
        );
    }

    private void validateSearch(RouteSearchRequest request) {
        requirePage(request.page(), request.size());
        requireNonNegative(request.minDistanceKm(), "Minimum distance must be non-negative");
        requireNonNegative(request.maxDistanceKm(), "Maximum distance must be non-negative");
        if (request.minDistanceKm() != null && request.maxDistanceKm() != null &&
                request.minDistanceKm() > request.maxDistanceKm()) {
            throw invalidRequest("Minimum distance must not exceed maximum distance");
        }
        requireNonNegative(request.minEstimatedTimeMinutes(), "Minimum estimated time must be non-negative");
        requireNonNegative(request.maxEstimatedTimeMinutes(), "Maximum estimated time must be non-negative");
        if (request.minEstimatedTimeMinutes() != null && request.maxEstimatedTimeMinutes() != null &&
                request.minEstimatedTimeMinutes() > request.maxEstimatedTimeMinutes()) {
            throw invalidRequest("Minimum estimated time must not exceed maximum estimated time");
        }
        validateProximity(request);
    }

    private void validateProximity(RouteSearchRequest request) {
        boolean hasAnyProximity = request.longitude() != null ||
                request.latitude() != null ||
                request.maxProximityKm() != null;
        if (!hasAnyProximity) {
            return;
        }
        if (request.longitude() == null || request.latitude() == null || request.maxProximityKm() == null) {
            throw invalidRequest("Longitude, latitude, and maxProximityKm must be provided together");
        }
        requireLongitude(request.longitude());
        requireLatitude(request.latitude());
        requirePositive(request.maxProximityKm(), "Maximum proximity must be greater than 0");
    }

    private void requirePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw invalidRequest("Page must be non-negative and size must be 1 to 100");
        }
    }

    private void requireNonNegative(Double value, String message) {
        if (value != null && (!Double.isFinite(value) || value < 0)) {
            throw invalidRequest(message);
        }
    }

    private void requireNonNegative(Integer value, String message) {
        if (value != null && value < 0) {
            throw invalidRequest(message);
        }
    }

    private void requirePositive(Double value, String message) {
        if (!Double.isFinite(value) || value <= 0) {
            throw invalidRequest(message);
        }
    }

    private void requireLongitude(Double value) {
        if (!Double.isFinite(value) || value < -180 || value > 180) {
            throw invalidRequest("Longitude must be between -180 and 180");
        }
    }

    private void requireLatitude(Double value) {
        if (!Double.isFinite(value) || value < -90 || value > 90) {
            throw invalidRequest("Latitude must be between -90 and 90");
        }
    }

    private InvalidPoiDataException invalidRequest(String message) {
        return new InvalidPoiDataException("INVALID_REQUEST", message);
    }

    private LineString createLineString(GeoJsonLineString geometry) {
        validateGeoJsonLineString(geometry);
        Coordinate[] coords = geometry.coordinates().stream()
                .map(this::toCoordinate)
                .toArray(Coordinate[]::new);
        return geometryFactory.createLineString(coords);
    }

    private Coordinate toCoordinate(List<Double> coordinate) {
        if (coordinate == null || coordinate.size() < 2) {
            throw new InvalidRouteGeometryException("GeoJSON coordinates must contain longitude and latitude");
        }
        Double longitude = coordinate.get(0);
        Double latitude = coordinate.get(1);
        if (longitude == null || latitude == null) {
            throw new InvalidRouteGeometryException("GeoJSON coordinates must contain longitude and latitude");
        }
        return new Coordinate(longitude, latitude);
    }

    private void validateGeoJsonLineString(GeoJsonLineString geometry) {
        if (!LINE_STRING_TYPE.equals(geometry.type())) {
            throw new InvalidRouteGeometryException("Route geometry must be a GeoJSON LineString");
        }
        if (geometry.coordinates() == null || geometry.coordinates().size() < 2) {
            throw new InvalidRouteGeometryException("Route geometry must contain at least two coordinates");
        }
    }

    private GeoJsonLineString toGeoJson(LineString lineString) {
        List<List<Double>> coordinates = Arrays.stream(lineString.getCoordinates())
                .map(coordinate -> List.of(coordinate.getX(), coordinate.getY()))
                .toList();

        return new GeoJsonLineString(LINE_STRING_TYPE, coordinates);
    }
}
