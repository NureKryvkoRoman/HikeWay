package ua.nure.kryvko.hikeway.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import ua.nure.kryvko.hikeway.model.*;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;
import ua.nure.kryvko.hikeway.model.request.SyncRequest;
import ua.nure.kryvko.hikeway.repository.CompletedHikeRepository;
import ua.nure.kryvko.hikeway.repository.RouteGeometryRepository;
import ua.nure.kryvko.hikeway.repository.RouteRepository;
import ua.nure.kryvko.hikeway.repository.SyncChangeRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataSyncServiceTest {
    private RouteRepository routeRepository;
    private RouteGeometryRepository geometryRepository;
    private CompletedHikeRepository hikeRepository;
    private SyncChangeRepository changeRepository;
    private DataSyncService service;

    @BeforeEach
    void setUp() {
        routeRepository = mock(RouteRepository.class);
        geometryRepository = mock(RouteGeometryRepository.class);
        hikeRepository = mock(CompletedHikeRepository.class);
        changeRepository = mock(SyncChangeRepository.class);
        service = new DataSyncService(
                routeRepository,
                geometryRepository,
                hikeRepository,
                changeRepository,
                new GeoJsonGeometryMapper()
        );
        Jwt jwt = new Jwt(
                "token",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Map.of("alg", "none"),
                Map.of("sub", "user-1")
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, null)
        );
        when(changeRepository.findByOwnerIdAndIdGreaterThanOrderByIdAsc(
                anyString(),
                anyLong(),
                any(Pageable.class)
        )).thenReturn(List.of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createsRouteUsingClientId() {
        UUID clientId = UUID.randomUUID();
        when(routeRepository.findByCreatedByAndClientId("user-1", clientId))
                .thenReturn(Optional.empty());
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(42L);
            return route;
        });
        when(geometryRepository.findByRoute(any(Route.class))).thenReturn(Optional.empty());
        SyncRequest request = new SyncRequest(
                null,
                UUID.randomUUID(),
                List.of(new SyncRequest.Mutation<>(
                        SyncRequest.Operation.UPSERT,
                        clientId,
                        0,
                        routePayload()
                )),
                List.of()
        );

        var response = service.synchronize(request);

        assertEquals(1, response.accepted().size());
        assertEquals(42L, response.accepted().getFirst().serverId());
        assertEquals(1L, response.accepted().getFirst().version());
        verify(routeRepository).save(any(Route.class));
        verify(geometryRepository).save(any(RouteGeometry.class));
        verify(changeRepository).save(any(SyncChange.class));
    }

    @Test
    void staleRouteMutationReturnsConflictWithoutOverwrite() {
        UUID clientId = UUID.randomUUID();
        Route existing = new Route();
        existing.setId(42L);
        existing.setClientId(clientId);
        existing.setCreatedBy("user-1");
        existing.setSyncVersion(3);
        existing.setName("Server route");
        existing.setDescription("Server description");
        existing.setDifficulty(Difficulty.EASY);
        existing.setTerrain(Terrain.FOREST);
        existing.setVisibility(RouteVisibility.PRIVATE);
        existing.setCreatedAt(Instant.now());
        existing.setUpdatedAt(Instant.now());
        when(routeRepository.findByCreatedByAndClientId("user-1", clientId))
                .thenReturn(Optional.of(existing));
        when(geometryRepository.findByRoute(existing)).thenReturn(Optional.empty());
        SyncRequest request = new SyncRequest(
                null,
                UUID.randomUUID(),
                List.of(new SyncRequest.Mutation<>(
                        SyncRequest.Operation.UPSERT,
                        clientId,
                        2,
                        routePayload()
                )),
                List.of()
        );

        var response = service.synchronize(request);

        assertTrue(response.accepted().isEmpty());
        assertEquals(1, response.conflicts().size());
        assertEquals(3, response.conflicts().getFirst().serverVersion());
        verify(routeRepository, never()).save(any(Route.class));
    }

    private SyncRequest.RoutePayload routePayload() {
        return new SyncRequest.RoutePayload(
                "Forest Loop",
                "Description",
                8.4,
                175,
                Difficulty.MEDIUM,
                460,
                Terrain.FOREST,
                new GeoJsonLineString(
                        "LineString",
                        List.of(List.of(24.0, 49.0), List.of(24.1, 49.1))
                )
        );
    }
}
