package ua.nure.kryvko.hikeway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.nure.kryvko.hikeway.model.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {
}
