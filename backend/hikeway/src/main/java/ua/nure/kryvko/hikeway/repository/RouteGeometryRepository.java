package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.Route;
import ua.nure.kryvko.hikeway.model.RouteGeometry;

import java.util.Optional;

public interface RouteGeometryRepository extends JpaRepository<RouteGeometry, Long> {
    Optional<RouteGeometry> findByRoute(Route route);
}
