package ua.nure.kryvko.hikeway.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.RouteVisibility;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.repository.RouteGeometryRepository;
import ua.nure.kryvko.hikeway.repository.RouteRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevRouteDataSeederTest {
    @Test
    void seedsPublicRoutesWhenSeedOwnerHasNoRoutes() throws Exception {
        RouteRepository routeRepository = mock(RouteRepository.class);
        RouteGeometryRepository geometryRepository = mock(RouteGeometryRepository.class);
        when(routeRepository.countByCreatedBy(DevRouteDataSeeder.CREATED_BY)).thenReturn(0L);
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(geometryRepository.save(any(RouteGeometry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new DevRouteDataSeeder().seedDevRoutes(routeRepository, geometryRepository).run(null);

        ArgumentCaptor<Route> routes = ArgumentCaptor.forClass(Route.class);
        ArgumentCaptor<RouteGeometry> geometries = ArgumentCaptor.forClass(RouteGeometry.class);
        verify(routeRepository, org.mockito.Mockito.times(5)).save(routes.capture());
        verify(geometryRepository, org.mockito.Mockito.times(5)).save(geometries.capture());

        Route first = routes.getAllValues().getFirst();
        assertEquals("High Castle Loop", first.getName());
        assertEquals(DevRouteDataSeeder.CREATED_BY, first.getCreatedBy());
        assertEquals(RouteVisibility.PUBLIC, first.getVisibility());
        assertEquals(Terrain.FOREST, first.getTerrain());
        assertFalse(first.isDeleted());
        assertNotNull(first.getPublishedAt());
        assertNotNull(first.getClientId());

        RouteGeometry firstGeometry = geometries.getAllValues().getFirst();
        assertEquals(first, firstGeometry.getRoute());
        assertEquals(4326, firstGeometry.getLine().getSRID());
        assertEquals(3, firstGeometry.getLine().getNumPoints());
        assertEquals(24.0316, firstGeometry.getLine().getCoordinateN(0).getX(), 0.0001);
        assertEquals(49.8429, firstGeometry.getLine().getCoordinateN(0).getY(), 0.0001);
    }

    @Test
    void skipsSeedingWhenSeedOwnerAlreadyHasRoutes() throws Exception {
        RouteRepository routeRepository = mock(RouteRepository.class);
        RouteGeometryRepository geometryRepository = mock(RouteGeometryRepository.class);
        when(routeRepository.countByCreatedBy(DevRouteDataSeeder.CREATED_BY)).thenReturn(1L);

        new DevRouteDataSeeder().seedDevRoutes(routeRepository, geometryRepository).run(null);

        verify(routeRepository, never()).save(any(Route.class));
        verify(geometryRepository, never()).save(any(RouteGeometry.class));
    }
}
