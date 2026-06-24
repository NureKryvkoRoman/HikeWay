package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.PoiComment;

import java.util.Optional;
import java.util.List;

public interface PoiCommentRepository extends JpaRepository<PoiComment, Long> {
    Page<PoiComment> findByPoiIdAndDeletedFalse(Long poiId, Pageable pageable);

    List<PoiComment> findByPoiIdAndDeletedFalse(Long poiId);

    Optional<PoiComment> findByIdAndPoiIdAndDeletedFalse(Long id, Long poiId);
}
