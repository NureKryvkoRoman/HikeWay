package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.nure.kryvko.hikeway.model.PointOfInterest;

import java.util.List;
import java.util.Optional;

public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, Long> {
    Optional<PointOfInterest> findByIdAndDeletedFalse(Long id);

    List<PointOfInterest> findAllByIdInAndDeletedFalse(List<Long> ids);

    Page<PointOfInterest> findByDeletedFalse(Pageable pageable);

    @Query(
            value = """
                    SELECT * FROM point_of_interest p
                    WHERE p.deleted = false
                      AND ST_X(p.location) BETWEEN :minLongitude AND :maxLongitude
                      AND ST_Y(p.location) BETWEEN :minLatitude AND :maxLatitude
                    """,
            countQuery = """
                    SELECT count(*) FROM point_of_interest p
                    WHERE p.deleted = false
                      AND ST_X(p.location) BETWEEN :minLongitude AND :maxLongitude
                      AND ST_Y(p.location) BETWEEN :minLatitude AND :maxLatitude
                    """,
            nativeQuery = true
    )
    Page<PointOfInterest> findWithinBounds(
            @Param("minLongitude") double minLongitude,
            @Param("minLatitude") double minLatitude,
            @Param("maxLongitude") double maxLongitude,
            @Param("maxLatitude") double maxLatitude,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT p.id AS id,
                           p.name AS name,
                           p.description AS description,
                           ST_X(p.location) AS longitude,
                           ST_Y(p.location) AS latitude,
                           p.owner_id AS "ownerId",
                           p.owner_display_name AS "ownerDisplayName",
                           ST_Distance(
                               CAST(p.location AS geography),
                               CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
                           ) AS "distanceMeters"
                    FROM point_of_interest p
                    WHERE p.deleted = false
                      AND ST_DWithin(
                          CAST(p.location AS geography),
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                          :radiusMeters
                      )
                    ORDER BY "distanceMeters" ASC, p.id ASC
                    """,
            countQuery = """
                    SELECT count(*)
                    FROM point_of_interest p
                    WHERE p.deleted = false
                      AND ST_DWithin(
                          CAST(p.location AS geography),
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                          :radiusMeters
                      )
                    """,
            nativeQuery = true
    )
    Page<NearbyPoiProjection> findNearby(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("radiusMeters") double radiusMeters,
            Pageable pageable
    );
}
