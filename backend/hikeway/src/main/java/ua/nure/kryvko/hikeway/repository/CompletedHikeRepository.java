package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.CompletedHike;

import java.util.Optional;
import java.util.UUID;

public interface CompletedHikeRepository extends JpaRepository<CompletedHike, Long> {
    Optional<CompletedHike> findByOwnerIdAndClientId(String ownerId, UUID clientId);
}
