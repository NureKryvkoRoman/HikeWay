package ua.nure.kryvko.hikeway.config;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.RouteVisibility;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.repository.RouteGeometryRepository;
import ua.nure.kryvko.hikeway.repository.RouteRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Configuration
@Profile({"dev", "test", "development", "local"})
public class DevRouteDataSeeder {
    static final String CREATED_BY = "seed-route-owner";

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Bean
    ApplicationRunner seedDevRoutes(
            RouteRepository routeRepository,
            RouteGeometryRepository geometryRepository
    ) {
        return ignored -> {
            if (routeRepository.countByCreatedBy(CREATED_BY) > 0) {
                return;
            }
            Instant now = Instant.now();
            seedRoutes().forEach(seed -> {
                Route route = new Route();
                route.setClientId(seed.clientId());
                route.setName(seed.name());
                route.setDescription(seed.description());
                route.setDistanceKm(seed.distanceKm());
                route.setEstimatedTimeMinutes(seed.estimatedTimeMinutes());
                route.setDifficulty(seed.difficulty());
                route.setElevationGain(seed.elevationGainMeters());
                route.setTerrain(seed.terrain());
                route.setCreatedBy(CREATED_BY);
                route.setCreatedAt(now);
                route.setUpdatedAt(now);
                route.setVisibility(RouteVisibility.PUBLIC);
                route.setPublishedAt(now);
                route.setDeleted(false);
                route = routeRepository.save(route);

                RouteGeometry geometry = new RouteGeometry();
                geometry.setRoute(route);
                geometry.setLine(line(seed.coordinates()));
                geometryRepository.save(geometry);
            });
        };
    }

    private List<RouteSeed> seedRoutes() {
        return List.of(
                route(
                        "High Castle Loop",
                        "A short city-edge climb with a panoramic viewpoint.",
                        4.8,
                        95,
                        Difficulty.EASY,
                        140,
                        Terrain.FOREST,
                        coordinate(24.0316, 49.8429),
                        coordinate(24.0394, 49.8461),
                        coordinate(24.0438, 49.8488)
                ),
                route(
                        "Vynnyky Forest Trail",
                        "A rolling forest route with quiet paths and several lakes.",
                        11.2,
                        210,
                        Difficulty.MEDIUM,
                        310,
                        Terrain.FOREST,
                        coordinate(24.1290, 49.8170),
                        coordinate(24.1440, 49.8105),
                        coordinate(24.1570, 49.8030)
                ),
                route(
                        "Rocky Ridge Traverse",
                        "A longer exposed ridge route for experienced hikers.",
                        18.6,
                        390,
                        Difficulty.HARD,
                        920,
                        Terrain.ROCKY,
                        coordinate(23.8910, 49.6510),
                        coordinate(23.9140, 49.6600),
                        coordinate(23.9400, 49.6560)
                ),
                route(
                        "Mountain Meadow Path",
                        "A steady climb through meadows and mixed woodland.",
                        8.4,
                        175,
                        Difficulty.MEDIUM,
                        460,
                        Terrain.MOUNTAIN,
                        coordinate(23.9870, 49.7310),
                        coordinate(24.0020, 49.7260),
                        coordinate(24.0150, 49.7190)
                ),
                route(
                        "Lakeside Mixed Walk",
                        "An accessible mixed-terrain route for a relaxed afternoon.",
                        6.1,
                        120,
                        Difficulty.EASY,
                        90,
                        Terrain.MIXED,
                        coordinate(24.0710, 49.8680),
                        coordinate(24.0800, 49.8710),
                        coordinate(24.0920, 49.8660)
                )
        );
    }

    private RouteSeed route(
            String name,
            String description,
            double distanceKm,
            int estimatedTimeMinutes,
            Difficulty difficulty,
            int elevationGainMeters,
            Terrain terrain,
            Coordinate... coordinates
    ) {
        return new RouteSeed(
                UUID.nameUUIDFromBytes(("dev-route:" + name).getBytes(StandardCharsets.UTF_8)),
                name,
                description,
                distanceKm,
                estimatedTimeMinutes,
                difficulty,
                elevationGainMeters,
                terrain,
                List.of(coordinates)
        );
    }

    private Coordinate coordinate(double longitude, double latitude) {
        return new Coordinate(longitude, latitude);
    }

    private LineString line(List<Coordinate> coordinates) {
        LineString line = geometryFactory.createLineString(coordinates.toArray(Coordinate[]::new));
        line.setSRID(4326);
        return line;
    }

    private record RouteSeed(
            UUID clientId,
            String name,
            String description,
            double distanceKm,
            int estimatedTimeMinutes,
            Difficulty difficulty,
            int elevationGainMeters,
            Terrain terrain,
            List<Coordinate> coordinates
    ) {
    }
}
