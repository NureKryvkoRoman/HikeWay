package ua.nure.kryvko.hikeway.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.nure.kryvko.hikeway.model.request.CreateRouteRequest;
import ua.nure.kryvko.hikeway.model.request.UpdateRouteRequest;
import ua.nure.kryvko.hikeway.model.response.RouteResponse;
import ua.nure.kryvko.hikeway.service.RouteService;

@Slf4j
@RestController
@RequestMapping("/admin/route")
public class AdminRouteController {
    private final RouteService routeService;

    @Autowired
    public AdminRouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@RequestBody CreateRouteRequest request) {
        RouteResponse route = routeService.createRoute(request);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable Long id) {
        RouteResponse route = routeService.getRoute(id);
        return ResponseEntity.ok(route);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RouteResponse> updateRoute(@PathVariable Long id, @RequestBody UpdateRouteRequest request) {
        RouteResponse route = routeService.updateRoute(id, request);
        return ResponseEntity.ok(route);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
