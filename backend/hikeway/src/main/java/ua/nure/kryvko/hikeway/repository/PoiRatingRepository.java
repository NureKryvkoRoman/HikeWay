package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ua.nure.kryvko.hikeway.model.PoiRating;

import java.util.List;
import java.util.Optional;

public interface PoiRatingRepository extends JpaRepository<PoiRating, Long> {
    Optional<PoiRating> findByPoiIdAndUserId(Long poiId, String userId);

    void deleteByPoiId(Long poiId);

    @Query("select coalesce(avg(r.score), 0), count(r) from PoiRating r where r.poi.id = :poiId")
    Object[] aggregateForPoi(@Param("poiId") Long poiId);

    List<PoiRating> findByPoiIdInAndUserId(List<Long> poiIds, String userId);

    @Query("""
            select r.poi.id, coalesce(avg(r.score), 0), count(r)
            from PoiRating r where r.poi.id in :poiIds group by r.poi.id
            """)
    List<Object[]> aggregateForPois(@Param("poiIds") List<Long> poiIds);
}
