package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.RoutePoi;

import java.util.List;

public interface RoutePoiRepository extends JpaRepository<RoutePoi, Long> {
    List<RoutePoi> findByRouteIdOrderByPositionAsc(Long routeId);

    void deleteByRouteId(Long routeId);

    void deleteByPoiId(Long poiId);

    boolean existsByPoiId(Long poiId);
}
