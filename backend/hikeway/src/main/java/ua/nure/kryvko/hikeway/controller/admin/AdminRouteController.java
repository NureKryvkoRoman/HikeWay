package ua.nure.kryvko.hikeway.controller.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.nure.kryvko.hikeway.model.request.CreateRouteRequest;
import ua.nure.kryvko.hikeway.model.request.UpdateRouteRequest;
import ua.nure.kryvko.hikeway.model.response.FullRouteResponse;
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
    public ResponseEntity<FullRouteResponse> createRoute(@RequestBody CreateRouteRequest request) {
        FullRouteResponse route = routeService.createRoute(request);
        return ResponseEntity.ok(route);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FullRouteResponse> getRoute(@PathVariable Long id) {
        FullRouteResponse route = routeService.getRouteWithGeometry(id);
        return ResponseEntity.ok(route);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FullRouteResponse> updateRoute(@PathVariable Long id, @RequestBody UpdateRouteRequest request) {
        FullRouteResponse route = routeService.updateRoute(id, request);
        return ResponseEntity.ok(route);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
