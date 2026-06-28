package ua.nure.kryvko.hikeway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ua.nure.kryvko.hikeway.exception.InvalidPoiDataException;
import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.model.request.RouteSearchRequest;
import ua.nure.kryvko.hikeway.repository.RouteGeometryRepository;
import ua.nure.kryvko.hikeway.repository.RouteRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteServiceTest {
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private RouteRepository routeRepository;
    private RouteService service;

    @BeforeEach
    void setUp() {
        routeRepository = mock(RouteRepository.class);
        service = new RouteService(
                routeRepository,
                mock(RouteGeometryRepository.class),
                mock(RoutePoiService.class)
        );
    }

    @Test
    void searchesPublishedRoutesAndMapsGeometry() {
        var request = new RouteSearchRequest(
                5.0,
                12.0,
                60,
                180,
                Set.of(Difficulty.EASY),
                Set.of(Terrain.FOREST),
                24.1,
                49.8,
                15.0,
                0,
                50
        );
        when(routeRepository.searchPublishedRoutes(request, PageRequest.of(0, 50)))
                .thenReturn(new PageImpl<>(List.of(routeGeometry()), PageRequest.of(0, 50), 1));

        var response = service.searchRoutes(request);

        assertEquals(1, response.items().size());
        assertEquals(7L, response.items().getFirst().id());
        assertEquals(Terrain.FOREST, response.items().getFirst().terrain());
        assertEquals("LineString", response.items().getFirst().geometry().type());
        assertEquals(List.of(24.1, 49.8), response.items().getFirst().geometry().coordinates().getFirst());
        assertEquals(1, response.totalElements());
        verify(routeRepository).searchPublishedRoutes(request, PageRequest.of(0, 50));
    }

    @Test
    void rejectsIncompleteProximityFilter() {
        var request = new RouteSearchRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                24.1,
                49.8,
                null,
                0,
                50
        );

        assertThrows(InvalidPoiDataException.class, () -> service.searchRoutes(request));
    }

    @Test
    void rejectsInvalidRangesAndPagination() {
        assertThrows(InvalidPoiDataException.class, () -> service.searchRoutes(new RouteSearchRequest(
                12.0,
                5.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                50
        )));
        assertThrows(InvalidPoiDataException.class, () -> service.searchRoutes(new RouteSearchRequest(
                null,
                null,
                180,
                60,
                null,
                null,
                null,
                null,
                null,
                0,
                50
        )));
        assertThrows(InvalidPoiDataException.class, () -> service.searchRoutes(new RouteSearchRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -1,
                50
        )));
    }

    private RouteGeometry routeGeometry() {
        Route route = new Route();
        route.setId(7L);
        route.setName("Forest Trail");
        route.setDescription("Public route");
        route.setDistanceKm(8.5);
        route.setEstimatedTimeMinutes(120);
        route.setDifficulty(Difficulty.EASY);
        route.setElevationGain(180);
        route.setTerrain(Terrain.FOREST);
        route.setCreatedAt(Instant.parse("2026-06-25T10:00:00Z"));
        route.setCreatedBy("user-1");

        var line = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(24.1, 49.8),
                new Coordinate(24.2, 49.9),
        });
        line.setSRID(4326);

        RouteGeometry geometry = new RouteGeometry();
        geometry.setId(3L);
        geometry.setRoute(route);
        geometry.setLine(line);
        return geometry;
    }
}
