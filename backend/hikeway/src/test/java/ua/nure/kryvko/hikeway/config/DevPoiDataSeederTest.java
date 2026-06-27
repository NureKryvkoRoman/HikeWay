package ua.nure.kryvko.hikeway.config;

import org.junit.jupiter.api.Test;
import ua.nure.kryvko.hikeway.model.PointOfInterest;
import ua.nure.kryvko.hikeway.repository.PointOfInterestRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevPoiDataSeederTest {
    @Test
    void seedsPoisWhenRepositoryIsEmpty() throws Exception {
        PointOfInterestRepository repository = mock(PointOfInterestRepository.class);
        when(repository.count()).thenReturn(0L);
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        new DevPoiDataSeeder().seedDevPois(repository).run(null);

        verify(repository).saveAll(org.mockito.ArgumentMatchers.argThat(pois -> {
            List<PointOfInterest> items = (List<PointOfInterest>) pois;
            assertEquals(4, items.size());
            assertFalse(items.get(0).isDeleted());
            assertEquals("seed-poi-owner", items.get(0).getOwnerId());
            return true;
        }));
    }

    @Test
    void skipsSeedingWhenRepositoryAlreadyHasPois() throws Exception {
        PointOfInterestRepository repository = mock(PointOfInterestRepository.class);
        when(repository.count()).thenReturn(1L);

        new DevPoiDataSeeder().seedDevPois(repository).run(null);

        verify(repository, never()).saveAll(anyList());
    }
}
