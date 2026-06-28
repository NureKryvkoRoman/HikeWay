package ua.nure.kryvko.hikeway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ua.nure.kryvko.hikeway.model.Difficulty;
import ua.nure.kryvko.hikeway.model.Terrain;
import ua.nure.kryvko.hikeway.model.request.RouteSearchRequest;
import ua.nure.kryvko.hikeway.model.response.PageResponse;
import ua.nure.kryvko.hikeway.model.response.RouteSearchResponse;
import ua.nure.kryvko.hikeway.service.RouteService;

import java.util.Set;

@RestController
public class RoutesController {
    private final RouteService routeService;

    public RoutesController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/routes")
    public PageResponse<RouteSearchResponse> search(
            @RequestParam(required = false) Double minDistanceKm,
            @RequestParam(required = false) Double maxDistanceKm,
            @RequestParam(required = false) Integer minEstimatedTimeMinutes,
            @RequestParam(required = false) Integer maxEstimatedTimeMinutes,
            @RequestParam(required = false) Set<Difficulty> difficulties,
            @RequestParam(required = false) Set<Terrain> terrains,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double maxProximityKm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return routeService.searchRoutes(new RouteSearchRequest(
                minDistanceKm,
                maxDistanceKm,
                minEstimatedTimeMinutes,
                maxEstimatedTimeMinutes,
                difficulties,
                terrains,
                longitude,
                latitude,
                maxProximityKm,
                page,
                size
        ));
    }
}
