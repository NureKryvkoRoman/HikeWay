package ua.nure.kryvko.hikeway.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.nure.kryvko.hikeway.model.geojson.GeoJsonLineString;
import ua.nure.kryvko.hikeway.model.response.RouteResponse;
import ua.nure.kryvko.hikeway.service.RouteService;

/**
 * Public route controller for accessing route info.
 */
@RestController
@RequestMapping("/route")
public class RouteController {

    private final RouteService routeService;

    @Autowired
    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable long id) {
        var route = routeService.getRoute(id);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/{routeId}/geometry")
    public ResponseEntity<GeoJsonLineString> getRouteGeometry(@PathVariable long routeId) {
        var geometry = routeService.getRouteGeometry(routeId);
        return ResponseEntity.ok(geometry);
    }

}
