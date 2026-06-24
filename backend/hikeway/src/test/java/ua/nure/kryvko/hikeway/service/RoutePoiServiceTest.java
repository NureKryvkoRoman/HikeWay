package ua.nure.kryvko.hikeway.service;

import org.junit.jupiter.api.Test;
import ua.nure.kryvko.hikeway.exception.InvalidPoiDataException;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.repository.PointOfInterestRepository;
import ua.nure.kryvko.hikeway.repository.RoutePoiRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RoutePoiServiceTest {
    @Test
    void rejectsDuplicatePoiReferencesWithoutReplacingAssociations() {
        RoutePoiRepository routePoiRepository = mock(RoutePoiRepository.class);
        PointOfInterestRepository poiRepository = mock(PointOfInterestRepository.class);
        RoutePoiService service = new RoutePoiService(
                routePoiRepository,
                poiRepository,
                mock(PointOfInterestService.class)
        );
        Route route = new Route();
        route.setId(42L);

        assertThrows(InvalidPoiDataException.class, () -> service.replace(route, List.of(7L, 7L)));

        verifyNoInteractions(poiRepository);
        verify(routePoiRepository, never()).deleteByRouteId(anyLong());
    }
}
