package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.request.RouteSearchRequest;

public interface RouteSearchRepository {
    Page<RouteGeometry> searchPublishedRoutes(RouteSearchRequest request, Pageable pageable);
}
