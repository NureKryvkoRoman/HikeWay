package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.Route;

import java.util.Optional;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByCreatedByAndClientId(String createdBy, UUID clientId);
}
