package ua.nure.kryvko.hikeway.config;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ua.nure.kryvko.hikeway.model.PointOfInterest;
import ua.nure.kryvko.hikeway.repository.PointOfInterestRepository;

import java.time.Instant;
import java.util.List;

@Configuration
@Profile({"dev", "test", "development", "local"})
public class DevPoiDataSeeder {
    private static final String OWNER_ID = "seed-poi-owner";
    private static final String OWNER_DISPLAY_NAME = "HikeWay seed";

    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Bean
    ApplicationRunner seedDevPois(PointOfInterestRepository repository) {
        return ignored -> {
            if (repository.count() > 0) {
                return;
            }
            Instant now = Instant.now();
            repository.saveAll(List.of(
                    poi(
                            "High Castle Viewpoint",
                            "A compact hilltop stop with a wide view over central Lviv.",
                            24.0394,
                            49.8461,
                            now
                    ),
                    poi(
                            "Vynnyky Forest Lake",
                            "A quiet lakeside rest point near shaded forest paths.",
                            24.1440,
                            49.8105,
                            now
                    ),
                    poi(
                            "Rocky Ridge Lookout",
                            "An exposed ridge marker with open terrain and long-distance views.",
                            23.9140,
                            49.6600,
                            now
                    ),
                    poi(
                            "Lviv Forest Spring",
                            "A small spring close to forest walking routes.",
                            24.0741,
                            49.8317,
                            now
                    )
            ));
        };
    }

    private PointOfInterest poi(
            String name,
            String description,
            double longitude,
            double latitude,
            Instant now
    ) {
        PointOfInterest poi = new PointOfInterest();
        poi.setOwnerId(OWNER_ID);
        poi.setOwnerDisplayName(OWNER_DISPLAY_NAME);
        poi.setName(name);
        poi.setDescription(description);
        Point point = geometryFactory.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        poi.setLocation(point);
        poi.setCreatedAt(now);
        poi.setUpdatedAt(now);
        poi.setDeleted(false);
        return poi;
    }
}
