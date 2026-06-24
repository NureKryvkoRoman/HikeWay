package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.PoiPhoto;
import ua.nure.kryvko.hikeway.model.PoiPhotoStatus;

import java.util.List;
import java.util.Optional;

public interface PoiPhotoRepository extends JpaRepository<PoiPhoto, Long> {
    List<PoiPhoto> findByPoiId(Long poiId);

    List<PoiPhoto> findByPoiIdAndStatusOrderByCreatedAtAsc(Long poiId, PoiPhotoStatus status);

    List<PoiPhoto> findByPoiIdInAndStatusOrderByCreatedAtAsc(List<Long> poiIds, PoiPhotoStatus status);

    Optional<PoiPhoto> findByIdAndPoiId(Long id, Long poiId);
}
