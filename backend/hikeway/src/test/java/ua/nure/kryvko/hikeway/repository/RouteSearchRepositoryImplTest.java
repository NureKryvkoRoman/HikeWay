package ua.nure.kryvko.hikeway.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;
import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.RouteVisibility;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.model.request.RouteSearchRequest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteSearchRepositoryImplTest {
    @Test
    void buildsPublishedRouteSearchWithStartPointProximity() {
        EntityManager entityManager = mock(EntityManager.class);
        Query resultQuery = query();
        Query countQuery = query();
        when(entityManager.createNativeQuery(anyString(), eq(RouteGeometry.class))).thenReturn(resultQuery);
        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery);
        when(resultQuery.getResultList()).thenReturn(List.of());
        when(countQuery.getSingleResult()).thenReturn(0L);

        var repository = new RouteSearchRepositoryImpl(entityManager);
        var request = new RouteSearchRequest(
                5.0,
                12.0,
                60,
                180,
                Set.of(Difficulty.HARD),
                Set.of(Terrain.ROCKY),
                24.1,
                49.8,
                15.0,
                1,
                25
        );

        var page = repository.searchPublishedRoutes(request, PageRequest.of(1, 25));

        assertEquals(0, page.getTotalElements());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture(), eq(RouteGeometry.class));
        assertTrue(sql.getValue().contains("r.visibility = :visibility"));
        assertTrue(sql.getValue().contains("r.deleted = false"));
        assertTrue(sql.getValue().contains("ST_StartPoint(rg.line)"));
        assertTrue(sql.getValue().contains("ORDER BY r.published_at DESC NULLS LAST, r.id DESC"));

        verify(resultQuery).setParameter("visibility", RouteVisibility.PUBLIC.name());
        verify(resultQuery).setParameter("minDistanceKm", 5.0);
        verify(resultQuery).setParameter("maxDistanceKm", 12.0);
        verify(resultQuery).setParameter("minEstimatedTimeMinutes", 60);
        verify(resultQuery).setParameter("maxEstimatedTimeMinutes", 180);
        verify(resultQuery).setParameter("difficulties", List.of("HARD"));
        verify(resultQuery).setParameter("terrains", List.of("ROCKY"));
        verify(resultQuery).setParameter("longitude", 24.1);
        verify(resultQuery).setParameter("latitude", 49.8);
        verify(resultQuery).setParameter("maxProximityMeters", 15_000.0);
        verify(resultQuery).setFirstResult(25);
        verify(resultQuery).setMaxResults(25);
    }

    private Query query() {
        Query query = mock(Query.class);
        when(query.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(query);
        when(query.setFirstResult(org.mockito.ArgumentMatchers.anyInt())).thenReturn(query);
        when(query.setMaxResults(org.mockito.ArgumentMatchers.anyInt())).thenReturn(query);
        return query;
    }
}
