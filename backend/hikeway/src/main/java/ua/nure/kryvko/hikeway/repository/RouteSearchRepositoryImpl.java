package ua.nure.kryvko.hikeway.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ua.nure.kryvko.hikeway.model.RouteGeometry;
import ua.nure.kryvko.hikeway.model.RouteVisibility;
import ua.nure.kryvko.hikeway.model.request.RouteSearchRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteSearchRepositoryImpl implements RouteSearchRepository {
    private final EntityManager entityManager;

    public RouteSearchRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<RouteGeometry> searchPublishedRoutes(RouteSearchRequest request, Pageable pageable) {
        StringBuilder fromAndWhere = new StringBuilder("""
                FROM route_geometry rg
                JOIN route r ON r.id = rg.route_id
                WHERE r.deleted = false
                  AND r.visibility = :visibility
                  AND rg.line IS NOT NULL
                """);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("visibility", RouteVisibility.PUBLIC.name());

        addFilters(request, fromAndWhere, parameters);

        Query resultQuery = entityManager.createNativeQuery("""
                SELECT rg.*
                """ + fromAndWhere + """
                ORDER BY r.published_at DESC NULLS LAST, r.id DESC
                """, RouteGeometry.class);
        parameters.forEach(resultQuery::setParameter);
        resultQuery.setFirstResult((int) pageable.getOffset());
        resultQuery.setMaxResults(pageable.getPageSize());

        Query countQuery = entityManager.createNativeQuery("SELECT COUNT(*) " + fromAndWhere);
        parameters.forEach(countQuery::setParameter);

        @SuppressWarnings("unchecked")
        List<RouteGeometry> items = resultQuery.getResultList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(items, pageable, total);
    }

    private void addFilters(
            RouteSearchRequest request,
            StringBuilder where,
            Map<String, Object> parameters
    ) {
        if (request.minDistanceKm() != null) {
            where.append(" AND r.distance_km >= :minDistanceKm\n");
            parameters.put("minDistanceKm", request.minDistanceKm());
        }
        if (request.maxDistanceKm() != null) {
            where.append(" AND r.distance_km <= :maxDistanceKm\n");
            parameters.put("maxDistanceKm", request.maxDistanceKm());
        }
        if (request.minEstimatedTimeMinutes() != null) {
            where.append(" AND r.estimated_time_minutes >= :minEstimatedTimeMinutes\n");
            parameters.put("minEstimatedTimeMinutes", request.minEstimatedTimeMinutes());
        }
        if (request.maxEstimatedTimeMinutes() != null) {
            where.append(" AND r.estimated_time_minutes <= :maxEstimatedTimeMinutes\n");
            parameters.put("maxEstimatedTimeMinutes", request.maxEstimatedTimeMinutes());
        }
        if (request.difficulties() != null && !request.difficulties().isEmpty()) {
            where.append(" AND r.difficulty IN (:difficulties)\n");
            parameters.put(
                    "difficulties",
                    request.difficulties().stream().map(Enum::name).toList()
            );
        }
        if (request.terrains() != null && !request.terrains().isEmpty()) {
            where.append(" AND r.terrain IN (:terrains)\n");
            parameters.put(
                    "terrains",
                    request.terrains().stream().map(Enum::name).toList()
            );
        }
        if (request.maxProximityKm() != null) {
            where.append("""
                     AND ST_DWithin(
                          CAST(ST_StartPoint(rg.line) AS geography),
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                          :maxProximityMeters
                     )
                    """);
            parameters.put("longitude", request.longitude());
            parameters.put("latitude", request.latitude());
            parameters.put("maxProximityMeters", request.maxProximityKm() * 1_000.0);
        }
    }
}
