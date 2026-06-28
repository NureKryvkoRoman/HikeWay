package ua.nure.kryvko.hikeway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;
import ua.nure.kryvko.hikeway.model.request.RouteSearchRequest;
import ua.nure.kryvko.hikeway.model.response.PageResponse;
import ua.nure.kryvko.hikeway.model.response.RouteSearchResponse;
import ua.nure.kryvko.hikeway.service.RouteService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoutesControllerTest {
    private RouteService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(RouteService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RoutesController(service)).build();
    }

    @Test
    void searchesRoutesWithDefaultPagination() throws Exception {
        var request = new RouteSearchRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                50
        );
        when(service.searchRoutes(eq(request)))
                .thenReturn(new PageResponse<>(List.of(response()), 0, 50, 1, 1));

        mockMvc.perform(get("/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].terrain").value("FOREST"))
                .andExpect(jsonPath("$.items[0].geometry.type").value("LineString"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));

        verify(service).searchRoutes(request);
    }

    @Test
    void parsesAllFilters() throws Exception {
        var request = new RouteSearchRequest(
                5.0,
                12.0,
                60,
                180,
                Set.of(Difficulty.EASY, Difficulty.HARD),
                Set.of(Terrain.FOREST, Terrain.MIXED),
                24.1,
                49.8,
                15.0,
                2,
                25
        );
        when(service.searchRoutes(eq(request)))
                .thenReturn(new PageResponse<>(List.of(), 2, 25, 0, 0));

        mockMvc.perform(get("/routes")
                        .param("minDistanceKm", "5")
                        .param("maxDistanceKm", "12")
                        .param("minEstimatedTimeMinutes", "60")
                        .param("maxEstimatedTimeMinutes", "180")
                        .param("difficulties", "EASY", "HARD")
                        .param("terrains", "FOREST", "MIXED")
                        .param("longitude", "24.1")
                        .param("latitude", "49.8")
                        .param("maxProximityKm", "15")
                        .param("page", "2")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(25));

        verify(service).searchRoutes(request);
    }

    private RouteSearchResponse response() {
        return new RouteSearchResponse(
                7L,
                "Forest Trail",
                "Public route",
                8.5,
                120,
                Difficulty.EASY,
                180,
                Terrain.FOREST,
                Instant.parse("2026-06-25T10:00:00Z"),
                "user-1",
                new GeoJsonLineString(
                        "LineString",
                        List.of(List.of(24.1, 49.8), List.of(24.2, 49.9))
                )
        );
    }
}
