package ua.nure.kryvko.hikeway.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ua.nure.kryvko.hikeway.model.request.PoiRequests;
import ua.nure.kryvko.hikeway.model.response.PageResponse;
import ua.nure.kryvko.hikeway.model.response.PoiResponses;
import ua.nure.kryvko.hikeway.service.PointOfInterestService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PointOfInterestControllerTest {
    private PointOfInterestService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(PointOfInterestService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PointOfInterestController(service)).build();
    }

    @Test
    void createsPoiAndReturnsCreated() throws Exception {
        when(service.create(eq(new PoiRequests.Create("Spring", "Fresh water", 24.1, 49.8))))
                .thenReturn(detail());

        mockMvc.perform(post("/pois")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Spring",
                                  "description": "Fresh water",
                                  "longitude": 24.1,
                                  "latitude": 49.8
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Spring"));
    }

    @Test
    void returnsNearbyPoisWithDefaultPagination() throws Exception {
        PoiResponses.NearbySummary summary = new PoiResponses.NearbySummary(
                7L,
                "Spring",
                "Fresh water",
                24.1,
                49.8,
                "Hiker",
                true,
                125.5
        );
        when(service.nearby(24.1, 49.8, 5000.0, 0, 50))
                .thenReturn(new PageResponse<>(List.of(summary), 0, 50, 1, 1));

        mockMvc.perform(get("/pois/nearby")
                        .param("longitude", "24.1")
                        .param("latitude", "49.8")
                        .param("radiusMeters", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].distanceMeters").value(125.5))
                .andExpect(jsonPath("$.items[0].averageRating").doesNotExist())
                .andExpect(jsonPath("$.items[0].photos").doesNotExist())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50));

        verify(service).nearby(24.1, 49.8, 5000.0, 0, 50);
    }

    @Test
    void addsCommentAndReturnsCreated() throws Exception {
        PoiResponses.Comment response = new PoiResponses.Comment(
                3L,
                "user-1",
                "Hiker",
                true,
                "Useful place",
                Instant.parse("2026-06-25T10:00:00Z"),
                Instant.parse("2026-06-25T10:00:00Z")
        );
        when(service.addComment(7L, new PoiRequests.Comment("Useful place"))).thenReturn(response);

        mockMvc.perform(post("/pois/7/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"Useful place\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.text").value("Useful place"));
    }

    @Test
    void updatesPhotoCaption() throws Exception {
        when(service.updatePhoto(7L, 9L, new PoiRequests.PhotoUpdate("Summit view")))
                .thenReturn(photo("Summit view"));

        mockMvc.perform(patch("/pois/7/photos/9")
                        .contentType("application/json")
                        .content("{\"caption\":\"Summit view\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.caption").value("Summit view"));

        verify(service).updatePhoto(7L, 9L, new PoiRequests.PhotoUpdate("Summit view"));
    }

    @Test
    void finalizesPhotoAndReturnsCreated() throws Exception {
        when(service.finalizePhoto(7L, new PoiRequests.PhotoFinalize(9L, "Summit view")))
                .thenReturn(photo("Summit view"));

        mockMvc.perform(post("/pois/7/photos")
                        .contentType("application/json")
                        .content("{\"photoId\":9,\"caption\":\"Summit view\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9));
    }

    @Test
    void deletesResourcesWithNoContent() throws Exception {
        mockMvc.perform(delete("/pois/7"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete("/pois/7/comments/3"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(delete("/pois/7/photos/9"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete(7L);
        verify(service).deleteComment(7L, 3L);
        verify(service).deletePhoto(7L, 9L);
    }

    private PoiResponses.Detail detail() {
        Instant timestamp = Instant.parse("2026-06-25T10:00:00Z");
        return new PoiResponses.Detail(
                7L,
                "Spring",
                "Fresh water",
                24.1,
                49.8,
                "user-1",
                "Hiker",
                true,
                0.0,
                0L,
                null,
                List.of(),
                timestamp,
                timestamp
        );
    }

    private PoiResponses.Photo photo(String caption) {
        return new PoiResponses.Photo(
                9L,
                "user-1",
                "Hiker",
                true,
                "http://storage/pois/7/photo.jpg",
                "image/jpeg",
                1024L,
                caption,
                Instant.parse("2026-06-25T10:00:00Z")
        );
    }
}
