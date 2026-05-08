package ua.nure.kryvko.hikeway.service;

import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import ua.nure.kryvko.hikeway.exception.InvalidRouteGeometryException;
import ua.nure.kryvko.hikeway.exception.RouteNotFoundException;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;
import ua.nure.kryvko.hikeway.model.request.CreateRouteRequest;
import ua.nure.kryvko.hikeway.model.request.UpdateRouteRequest;
import ua.nure.kryvko.hikeway.model.response.RouteResponse;
import ua.nure.kryvko.hikeway.repository.RouteGeometryRepository;
import ua.nure.kryvko.hikeway.repository.RouteRepository;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class RouteService {
    private static final String LINE_STRING_TYPE = "LineString";
    private final RouteRepository routeRepository;
    private final RouteGeometryRepository routeGeometryRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Autowired
    public RouteService(RouteRepository routeRepository, RouteGeometryRepository routeGeometryRepository) {
        this.routeRepository = routeRepository;
        this.routeGeometryRepository = routeGeometryRepository;
    }

    public RouteResponse createRoute(CreateRouteRequest request) {
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

        return toRouteResponse(route, geometry);
    }

    public RouteResponse getRoute(Long id) {
        Route route = getRouteEntity(id);
        RouteGeometry geometry = routeGeometryRepository.findByRoute(route).orElse(null);
        return toRouteResponse(route, geometry);
    }

    public RouteResponse updateRoute(Long id, UpdateRouteRequest request) {
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

        return toRouteResponse(route, geometry);
    }

    public void deleteRoute(Long id) {
        Route route = getRouteEntity(id);
        routeGeometryRepository.findByRoute(route).ifPresent(routeGeometryRepository::delete);
        routeRepository.delete(route);
    }

    private Route getRouteEntity(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException("Route not found with id: " + id));
    }

    private RouteResponse toRouteResponse(Route route, RouteGeometry geometry) {
        return new RouteResponse(
                route.getId(),
                route.getName(),
                route.getDescription(),
                route.getDistanceKm(),
                route.getEstimatedTimeMinutes(),
                route.getDifficulty(),
                route.getElevationGain(),
                route.getCreatedAt(),
                route.getCreatedBy(),
                geometry == null ? null : toGeoJson(geometry.getLine())
        );
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
