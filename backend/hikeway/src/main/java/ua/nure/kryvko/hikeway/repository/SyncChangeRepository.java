package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.SyncChange;

import java.util.List;

public interface SyncChangeRepository extends JpaRepository<SyncChange, Long> {
    List<SyncChange> findByOwnerIdAndIdGreaterThanOrderByIdAsc(
            String ownerId,
            long id,
            Pageable pageable
    );
}
